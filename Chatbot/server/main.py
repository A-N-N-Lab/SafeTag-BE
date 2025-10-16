from fastapi import FastAPI, HTTPException, Header, UploadFile, Form, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from typing import Literal, List, Optional
from openai import OpenAI
from pathlib import Path
import httpx, json, os, uuid, datetime, tempfile
from pdf2image import convert_from_bytes
from PIL import Image, UnidentifiedImageError
import pytesseract

# 1) 기본 설정
STICKER_ENUM_MAP = {
    "pregnant": "PREGNANT",
    "disabled": "DISABLED",
    "resident": "RESIDENT",
}

TEMPLATE_BY_TYPE = {
    "pregnant": "pregnant.png",
    "disabled": "disabled.png",
    "resident": "resident.png",
}

try:
    from dotenv import load_dotenv
    load_dotenv()
except Exception:
    pass

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    raise RuntimeError("OPENAI_API_KEY is not set.")
oai = OpenAI(api_key=OPENAI_API_KEY)

bearer_scheme = HTTPBearer(auto_error=False)

app = FastAPI(
    title="SafeTag Chatbot API (Function Calling)",
    openapi_tags=[{"name": "Chatbot", "description": "OCR 및 스티커 발급"}],
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 2) 정적 폴더 (템플릿 이미지만 사용)
BASE_DIR = os.path.dirname(__file__)
STATIC_DIR = os.path.join(BASE_DIR, "static")
TEMPLATE_DIR = os.path.join(STATIC_DIR, "templates")
os.makedirs(TEMPLATE_DIR, exist_ok=True)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

def get_template_url(sticker_type: str) -> str:
    filename = TEMPLATE_BY_TYPE.get(sticker_type, "pregnant.png")
    return f"/static/templates/{filename}"

def _date_only(v) -> str:
    import datetime as _dt
    if isinstance(v, _dt.datetime):
        return v.date().isoformat()
    if isinstance(v, _dt.date):
        return v.isoformat()
    if isinstance(v, str):
        if "T" in v:
            return v.split("T", 1)[0]
        try:
            return _dt.datetime.fromisoformat(v).date().isoformat()
        except Exception:
            return v
    return str(v)

# 3) 시스템 프롬프트
SYSTEM_PROMPT = (
    "너는 Safe Tag 앱의 AI 챗봇 '세이피'야. "
    "앱 사용법/정책/기능에 대해 간결하게 답하고, "
    "디지털 스티커 발급·인증(아파트 거주/임산부/장애인)·OTP/통화중계는 "
    "백엔드 API로 처리된다고 가정하고 절차를 안내해. 필요할 때는 제공된 도구를 호출해.\n\n"
    "## 아파트 거주자 인증 절차\n"
    "1. Safe Tag 앱에 로그인합니다.\n"
    "2. 인증 메뉴에서 '아파트 거주자 인증'을 선택합니다.\n"
    "3. 거주 증명서(예: 주민등록등본, 아파트 계약서 등)를 앱 내에서 업로드합니다.\n"
    "4. 제출한 서류가 심사됩니다. 결과는 앱 알림을 통해 확인할 수 있습니다.\n\n"
    "## 임산부 인증 절차\n"
    "1. Safe Tag 앱에 로그인합니다.\n"
    "2. 인증 메뉴에서 '임산부 인증'을 선택합니다.\n"
    "3. 산모 수첩, 진단서 등 임신을 증명할 수 있는 서류를 앱 내에서 업로드합니다.\n"
    "4. 제출한 서류가 심사됩니다. 결과는 앱 알림을 통해 확인할 수 있습니다.\n\n"
    "## 장애인 인증 절차\n"
    "1. Safe Tag 앱에 로그인합니다.\n"
    "2. 인증 메뉴에서 '장애인 인증'을 선택합니다.\n"
    "3. 장애인 등록증 또는 관련 증명서를 앱 내에서 업로드합니다.\n"
    "4. 제출한 서류가 심사됩니다. 결과는 앱 알림을 통해 확인할 수 있습니다.\n\n"
    "## 응답 규칙\n"
    "- 한국어로 간결하게. 단계가 필요하면 1,2,3 순서로.\n"
    "- 실제 인증/발급/OTP 생성 등은 도구 호출로 처리하고, 결과를 요약해 알려줘.\n"
    "- 앱과 무관하거나 불법/위험한 요청은 정중히 거절해.\n"
)

# 4) Spring 연결 함수
SPRING_BASE_URL = os.getenv("SPRING_BASE_URL", "http://localhost:8080")
USER_ME_PATH    = os.getenv("USER_ME_PATH", "/api/user/me")
MYPAGE_GET_PATH = os.getenv("MYPAGE_GET_PATH", "/api/mypage")

async def call_spring(method: str, path: str, auth: Optional[str] = None, json_body: dict | None = None):
    headers = {"Content-Type": "application/json"}
    if auth:
        headers["Authorization"] = auth
    url = f"{SPRING_BASE_URL}{path}"
    async with httpx.AsyncClient(timeout=20.0) as client:
        r = await client.request(method, url, headers=headers, json=json_body)
        r.raise_for_status()
        return r.json()

async def issue_sticker(car_number: str, sticker_type: str, valid_days: int | None = None, auth: Optional[str] = None):
    body = {"carNumber": car_number, "stickerType": sticker_type}
    if valid_days is not None:
        body["validDays"] = valid_days
    return await call_spring("POST", "/api/sticker/issue", auth, body)

async def get_my_car_number(auth: Optional[str]) -> Optional[str]:
    # 1) /api/user/me
    try:
        me = await call_spring("GET", USER_ME_PATH, auth)
        car = (
            me.get("carNumber")
            or (me.get("data") or {}).get("carNumber")
        )
        if car:
            return car
    except httpx.HTTPStatusError:
        pass
    # 2) /api/mypage
    try:
        mp = await call_spring("GET", MYPAGE_GET_PATH, auth)
        car = (
            mp.get("carNumber")
            or (mp.get("data") or {}).get("carNumber")
        )
        if car:
            return car
    except httpx.HTTPStatusError:
        pass
    return None

# 5) OCR 인증
def guess_type_from_text(text: str) -> Optional[str]:
    if not text:
        return None
    if "임산부" in text or "임신" in text:
        return "pregnant"
    if "장애인" in text or "장애" in text:
        return "disabled"
    if "아파트" in text or "거주" in text or "입주" in text:
        return "resident"
    t = text.lower()
    if "preg" in t: return "pregnant"
    if "disab" in t: return "disabled"
    if "resi" in t or "apart" in t: return "resident"
    return None

@app.post("/ocr")
async def ocr(file: UploadFile):
    """OCR만 수행"""
    try:
        raw = await file.read()
        if not raw:
            raise HTTPException(status_code=422, detail="업로드된 파일이 비어 있습니다.")
        images = []
        if "pdf" in (file.content_type or "").lower() or file.filename.lower().endswith(".pdf"):
            images = convert_from_bytes(raw, dpi=300, fmt="png")
        else:
            with tempfile.NamedTemporaryFile(delete=False) as tmp:
                tmp.write(raw)
                tmp_path = tmp.name
            try:
                img = Image.open(tmp_path)
                images = [img]
            except UnidentifiedImageError:
                raise HTTPException(status_code=415, detail="이미지 형식을 인식할 수 없습니다.")
            finally:
                os.remove(tmp_path)
        texts = []
        for img in images:
            texts.append(pytesseract.image_to_string(img.convert("L"), lang="kor+eng"))
        return {"text": "\n".join(texts)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR 실패: {e}")

# 6) OCR → 스티커 발급
@app.post("/ocr/sticker")
async def ocr_and_issue(
    file: UploadFile,
    creds: HTTPAuthorizationCredentials = Security(bearer_scheme),
    carNumber: str | None = Form(default=None),
    validDays: int | None = Form(default=None),
):
    """서류 OCR → 스티커 발급"""
    if not creds:
        raise HTTPException(status_code=401, detail="Authorization 헤더가 필요합니다.")
    authorization = f"{creds.scheme} {creds.credentials}"

    try:
        if not validDays or str(validDays).strip() in ("", "0", "null"):
            days = 730
        else:
            days = int(validDays)
    except ValueError:
        days = 730

    car_no = carNumber or await get_my_car_number(authorization)
    if not car_no:
        return {"status": "FAILED", "reason": "차량번호를 찾을 수 없습니다."}

    try:
        # 파일 → OCR
        raw = await file.read()
        if not raw:
            raise HTTPException(status_code=422, detail="파일이 비어 있습니다.")
        if "pdf" in (file.content_type or "").lower() or file.filename.lower().endswith(".pdf"):
            pages = convert_from_bytes(raw, dpi=300, fmt="png")
        else:
            with tempfile.NamedTemporaryFile(delete=False) as tmp:
                tmp.write(raw)
                tmp_path = tmp.name
            try:
                img = Image.open(tmp_path)
                pages = [img]
            except UnidentifiedImageError:
                raise HTTPException(status_code=415, detail="이미지 형식을 인식할 수 없습니다.")
            finally:
                os.remove(tmp_path)

        texts = [pytesseract.image_to_string(p.convert("L"), lang="kor+eng").strip() for p in pages]
        text = "\n".join(texts)
        sticker_type = guess_type_from_text(text)
        if not sticker_type:
            return {"status": "FAILED", "reason": "서류에서 인증 단어를 찾지 못했습니다.", "ocrText": text}

        enum_type = STICKER_ENUM_MAP.get(sticker_type)
        issue_res = await issue_sticker(car_no, enum_type, days, authorization)

        # 메타데이터 구성
        today = datetime.date.today()
        issue_no   = str(issue_res.get("stickerId", "")) or uuid.uuid4().hex[:8]
        issued_at  = _date_only(issue_res.get("issuedAt")  or today.isoformat())
        expires_at = _date_only(issue_res.get("expiresAt") or (today + datetime.timedelta(days=days)).isoformat())
        issuer     = issue_res.get("issuer") or "SAFETAG"

        template_url = get_template_url(sticker_type)

        return {
            "status": "SUCCESS",
            "stickerType": sticker_type,
            "ocrText": text,
            "sticker": {
                "stickerId": issue_no,
                "type": enum_type,
                "imageUrl": template_url,
                "carNumber": car_no,
                "issuedAt": issued_at,
                "expiresAt": expires_at,
                "issuer": issuer
            }
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR+스티커 발급 실패: {e}")
