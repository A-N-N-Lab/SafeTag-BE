from fastapi import FastAPI, HTTPException, Header, UploadFile, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from typing import Literal, List, Optional
from openai import OpenAI
from pathlib import Path

import httpx, json, os, io, uuid, datetime, tempfile
from pdf2image import convert_from_bytes

from PIL import Image, ImageDraw, ImageFont, UnidentifiedImageError
import pytesseract

# 0) 환경설정 & OpenAI
try:
    from dotenv import load_dotenv
    load_dotenv()
except Exception:
    pass

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    raise RuntimeError("OPENAI_API_KEY is not set.")
oai = OpenAI(api_key=OPENAI_API_KEY)

# 1) FastAPI 앱 & CORS
app = FastAPI(title="SafeTag Chatbot API (Function Calling)")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 2) 정적 파일 서빙 (/static)
BASE_DIR = os.path.dirname(__file__)
STATIC_DIR = os.path.join(BASE_DIR, "static")
TEMPLATE_DIR = os.path.join(STATIC_DIR, "templates")
STICKER_OUT_DIR = os.path.join(STATIC_DIR, "stickers")
os.makedirs(STICKER_OUT_DIR, exist_ok=True)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


def _load_font(size: int = 40) -> ImageFont.FreeTypeFont:
    try:
        # 프로젝트 내부 폰트 우선
        base_dir = Path(__file__).resolve().parent
        project_font = base_dir / "static" / "fonts" / "NotoSansKR-Regular.otf"
        if project_font.is_file():
            return ImageFont.truetype(str(project_font), size=size)

        # OS별 기본 폰트 폴백
        candidates = [
            Path("/System/Library/Fonts/AppleSDGothicNeo.ttc"),  # macOS
            Path("C:/Windows/Fonts/malgun.ttf"),                 # Windows
            Path("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"),  # Linux
            Path("/usr/share/fonts/truetype/unfonts-core/UnDotum.ttf"),
        ]
        for p in candidates:
            if p.is_file():
                return ImageFont.truetype(str(p), size=size)

        # 폴백 (영문 전용)
        return ImageFont.load_default()

    except Exception:
        return ImageFont.load_default()


TEMPLATE_BY_TYPE = {
    "pregnant": "pregnant.png",
    "disabled": "disabled.png",
    "resident": "resident.png",
}

def create_sticker_image(
    sticker_type: str,
    issue_no: str,
    vehicle_no: str,
    expires_at: str,
) -> str:
    """
    템플릿 위에 발급번호/차량번호/유효기간 텍스트를 합성하여 PNG 저장.
    반환: /static/stickers/xxx.png 형태의 URL 경로
    """
    template_filename = TEMPLATE_BY_TYPE.get(sticker_type, "pregnant.png")
    template_path = os.path.join(TEMPLATE_DIR, template_filename)
    if not os.path.exists(template_path):
        raise RuntimeError(f"Template not found: {template_path}")

    img = Image.open(template_path).convert("RGBA")
    draw = ImageDraw.Draw(img)
    font_body = _load_font(50)

    # 템플릿에 맞춰 좌표값 조정 (**좌표 수정 필요)
    issue_xy   = (220, 420)  # 발급번호 값
    vehicle_xy = (220, 540)  # 차량번호 값
    expire_xy  = (220, 660)  # 유효기간 값

    fill = (0, 0, 0, 255)
    draw.text(issue_xy,   f"{issue_no}",   font=font_body, fill=fill)
    draw.text(vehicle_xy, f"{vehicle_no}", font=font_body, fill=fill)
    draw.text(expire_xy,  f"{expires_at}", font=font_body, fill=fill)

    out_name = f"{uuid.uuid4().hex}.png"
    out_path = os.path.join(STICKER_OUT_DIR, out_name)
    img.save(out_path, format="PNG")

    return f"/static/stickers/{out_name}"

# 3) 시스템 프롬프트
SYSTEM_PROMPT = (
    "너는 Safe Tag 앱의 AI 챗봇 '세이피'야. "
    "앱 사용법/정책/기능을 간결하게 답하고, "
    "스티커 발급·인증(거주/임산부/장애인)·OTP/통화중계는 "
    "백엔드 API로 처리된다고 가정하고 절차를 안내해. "
    "한국어로 단계가 필요하면 1,2,3으로 정리해. "
    "앱과 무관하거나 위험한 요청은 정중히 거절해."
)

# 4) DTO (요청/응답)
class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str

class ChatRequest(BaseModel):
    messages: List[ChatMessage]

class ChatResponse(BaseModel):
    content: str

# 5) Spring 프록시 유틸
SPRING_BASE_URL = os.getenv("SPRING_BASE_URL", "http://localhost:8080")

async def call_spring(method: str, path: str, auth: Optional[str] = None, json_body: dict | None = None):
    headers = {"Content-Type": "application/json"}
    if auth:
        headers["Authorization"] = auth
    url = f"{SPRING_BASE_URL}{path}"
    async with httpx.AsyncClient(timeout=20.0) as client:
        r = await client.request(method, url, headers=headers, json=json_body)
        r.raise_for_status()
        return r.json()

async def start_verification(type_: str, file_id: str, auth: Optional[str] = None):
    return await call_spring("POST", "/api/verify/start", auth, {"type": type_, "fileId": file_id})

async def get_verification_status(verify_id: str, auth: Optional[str] = None):
    return await call_spring("GET", f"/api/verify/{verify_id}", auth)

async def issue_sticker(vehicle_number: str, sticker_type: str, valid_days: int | None = None, auth: Optional[str] = None):
    body = {"vehicleNumber": vehicle_number, "stickerType": sticker_type}
    if valid_days is not None:
        body["validDays"] = valid_days
    return await call_spring("POST", "/api/sticker/issue", auth, body)

async def issue_otp(user_id: int, auth: Optional[str] = None):
    return await call_spring("POST", "/api/otp/issue", auth, {"userId": user_id})

# 6) OpenAI Tools (Function Calling)
TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "start_verification",
            "description": "서류 인증 시작(아파트 거주/임산부/장애인). 업로드된 fileId 필요.",
            "parameters": {
                "type": "object",
                "properties": {
                    "type": {"type": "string", "enum": ["resident", "pregnant", "disabled"]},
                    "fileId": {"type": "string"}
                },
                "required": ["type", "fileId"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_verification_status",
            "description": "인증 상태 조회.",
            "parameters": {
                "type": "object",
                "properties": { "verifyId": {"type": "string"} },
                "required": ["verifyId"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "issue_sticker",
            "description": "디지털 스티커 발급.",
            "parameters": {
                "type": "object",
                "properties": {
                    "vehicleNumber": {"type": "string"},
                    "stickerType": {"type": "string", "enum": ["resident", "pregnant", "disabled"]},
                    "validDays": {"type": "integer"}
                },
                "required": ["vehicleNumber", "stickerType"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "issue_otp",
            "description": "통화 중계용 OTP 발급.",
            "parameters": {
                "type": "object",
                "properties": { "userId": {"type": "integer"} },
                "required": ["userId"]
            }
        }
    }
]

# 7) 유틸: 타입 정규화 / OCR 타입 추정
def normalize_type(raw: str) -> Optional[str]:
    if not raw:
        return None
    t = raw.strip().lower()
    if t in ("임산부", "임신", "preg", "pregnant"):
        return "pregnant"
    if t in ("장애", "장애인", "disabled", "disability"):
        return "disabled"
    if t in ("거주", "거주자", "아파트", "resident", "residence", "apart"):
        return "resident"
    return None

def guess_type_from_text(text: str) -> Optional[str]:
    if not text:
        return None
    # 한글 우선
    if "임산부" in text or "임신" in text:
        return "pregnant"
    if "장애인" in text or "장애" in text:
        return "disabled"
    if "아파트" in text or "거주" in text or "입주" in text:
        return "resident"
    # 영어 백업
    t = text.lower()
    if "preg" in t: return "pregnant"
    if "disab" in t: return "disabled"
    if "resi" in t or "apart" in t: return "resident"
    return None

# 8) /chat : Function Calling
@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, authorization: str | None = Header(default=None)):
    try:
        msgs = [{"role": "system", "content": SYSTEM_PROMPT}]
        msgs += [m.model_dump() for m in req.messages]

        comp = oai.chat.completions.create(
            model="gpt-4o-mini",
            messages=msgs,
            tools=TOOLS,
            temperature=0.4,
            max_tokens=700,
        )
        choice = comp.choices[0]
        message = choice.message

        # 도구 호출 없음 → 일반 Q&A
        if not getattr(message, "tool_calls", None):
            return ChatResponse(content=message.content or "")

        # 도구 호출 처리
        tool_results = []
        for tc in message.tool_calls:
            name = tc.function.name
            args = json.loads(tc.function.arguments or "{}")

            if name == "start_verification":
                result = await start_verification(args["type"], args["fileId"], authorization)
            elif name == "get_verification_status":
                result = await get_verification_status(args["verifyId"], authorization)
            elif name == "issue_sticker":
                result = await issue_sticker(args["vehicleNumber"], args["stickerType"], args.get("validDays"), authorization)
            elif name == "issue_otp":
                result = await issue_otp(args["userId"], authorization)
            else:
                result = {"error": f"Unknown tool: {name}"}

            tool_results.append({
                "role": "tool",
                "tool_call_id": tc.id,
                "name": name,
                "content": json.dumps(result, ensure_ascii=False)
            })

        msgs.append(message.model_dump(exclude_none=True))
        msgs += tool_results

        comp2 = oai.chat.completions.create(
            model="gpt-4o-mini",
            messages=msgs,
            temperature=0.3,
            max_tokens=700,
        )
        content = comp2.choices[0].message.content or ""
        return ChatResponse(content=content)

    except httpx.HTTPStatusError as he:
        raise HTTPException(status_code=he.response.status_code, detail={"error": f"SPRING_DOWN: {str(he)}"})
    except Exception as e:
        raise HTTPException(status_code=500, detail={"error": str(e)})

# 9) OCR (PNG/JPG/PDF)
@app.post("/ocr")
async def ocr(file: UploadFile):
    try:
        raw = await file.read()
        if not raw:
            raise HTTPException(status_code=422, detail="업로드된 파일이 비어 있습니다.")

        images = []

        # PDF → 이미지 변환
        if (file.content_type and "pdf" in file.content_type.lower()) or file.filename.lower().endswith(".pdf"):
            images = convert_from_bytes(raw, dpi=300, fmt="png")

        # 이미지 파일은 임시 파일로 저장 후 열기(헤더/포인터 문제 회피)
        else:
            suffix = os.path.splitext(file.filename)[1] or ".bin"
            with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
                tmp.write(raw)
                tmp_path = tmp.name
            try:
                img = Image.open(tmp_path)
                img.load()
                images = [img]
            except UnidentifiedImageError:
                raise HTTPException(
                    status_code=415,
                    detail=f"이미지 형식을 인식할 수 없습니다. filename={file.filename}, content_type={file.content_type}"
                )
            finally:
                try:
                    os.remove(tmp_path)
                except Exception:
                    pass

        # OCR (간단 전처리)
        texts = []
        for img in images:
            g = img.convert("L")
            texts.append(pytesseract.image_to_string(g, lang="kor+eng"))

        return {"text": "\n".join(texts)}

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR 실패: {e}")


# 10) OCR → 검증 → 스티커 발급(+이미지 생성)
@app.post("/ocr/sticker")
async def ocr_and_issue(
    file: UploadFile,
    authorization: str | None = Header(default=None),
    vehicleNumber: str | None = Form(default="12가3456"),
    validDays: int | None = Form(default=30),
):
    """
    파일 업로드 → OCR → 임산부/장애인/거주 단어 검증 → 조건 충족 시 스티커 발급
    성공 시 스티커 이미지 PNG를 생성하고 URL 반환
    """
    try:
        raw = await file.read()
        if not raw:
            raise HTTPException(status_code=422, detail="업로드된 파일이 비어 있습니다.")

        # PDF or IMAGE 로딩
        if (file.content_type and "pdf" in file.content_type.lower()) or file.filename.lower().endswith(".pdf"):
            pages = convert_from_bytes(raw, dpi=300, fmt="png")
        else:
            suffix = os.path.splitext(file.filename)[1] or ".bin"
            with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
                tmp.write(raw)
                tmp_path = tmp.name
            try:
                img = Image.open(tmp_path); img.load()
                pages = [img]
            except UnidentifiedImageError:
                raise HTTPException(status_code=415, detail="이미지 형식을 인식할 수 없습니다.")
            finally:
                try: os.remove(tmp_path)
                except Exception: pass

        # OCR
        ocr_texts = []
        for img in pages:
            g = img.convert("L")
            ocr_texts.append(pytesseract.image_to_string(g, lang="kor+eng").strip())
        text = "\n".join(ocr_texts)

        # 유형 판별 → 실패 시 종료
        sticker_type = guess_type_from_text(text)
        if not sticker_type:
            return {"status": "FAILED", "reason": "서류에서 인증 단어를 찾지 못했습니다.", "ocrText": text}

        # 발급 + 이미지 생성
        issue_res = await issue_sticker(vehicleNumber or "12가3456", sticker_type, validDays or 30, authorization)
        issue_no = str(issue_res.get("stickerId", "")) or uuid.uuid4().hex[:8]
        expires  = issue_res.get("expiresAt") or (datetime.date.today() + datetime.timedelta(days=validDays or 30)).isoformat()
        sticker_img_url = create_sticker_image(sticker_type, issue_no, vehicleNumber or "12가3456", expires)

        return {
            "status": "SUCCESS",
            "stickerType": sticker_type,
            "ocrText": text,
            "result": issue_res,
            "stickerImageUrl": sticker_img_url
        }


    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR+스티커 발급 실패: {e}")
