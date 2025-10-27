# -*- coding: utf-8 -*-
"""
SafeTag Chatbot Server - FULL main.py

포함 기능:
- /           : 루트/헬스/정적
- /ocr        : OCR 텍스트만 추출
- /ocr/sticker: OCR→유형판별→유효기간 계산→Spring 발급→스티커 PNG 생성
- /chat       : 간단 챗봇(OPENAI_API_KEY 있으면 모델 호출, 없으면 로컬 에코)

정책:
- 임산부 : 분만/출산 예정일(+ 별칭) OCR 또는 폼 dueDate → +180일(6개월), 실패시 기본 2년
- 장애인 : 기본 5년(1825일). 문서에서 만료일 보이면 그 날짜 적용(60일 미만이면 5년)
- 거주민 : 기본 2년. 문서에서 계약만료 등 찾으면 해당 날짜까지.
- 거주민 주소→단지명: static/config/resident_map.json
- 텍스트 좌표: .env(STICKER_X, STICKER_Y_ISSUE, _VEHICLE, _EXPIRE, _AP
T)
"""

from fastapi import FastAPI, HTTPException, UploadFile, File, Form, Security, Request
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse, Response
from pydantic import BaseModel
from typing import Optional, List, Dict, Any, Literal

from pathlib import Path
import os, io, re, json, uuid, datetime, httpx
from pdf2image import convert_from_bytes
from PIL import Image, ImageDraw, ImageFont, UnidentifiedImageError
import pytesseract

# =========================
# 0) 환경변수 / 기본 설정
# =========================
try:
    from dotenv import load_dotenv
    load_dotenv()
except Exception:
    pass

DEV_FAKE_SPRING = os.getenv("DEV_FAKE_SPRING", "0") == "1"

# 경로
BASE_DIR      = Path(__file__).resolve().parent
STATIC_DIR    = BASE_DIR / "static"
TEMPLATE_DIR  = STATIC_DIR / "templates"
GENERATED_DIR = STATIC_DIR / "generated"
WEB_DIR       = BASE_DIR / "webui"
CONFIG_DIR    = STATIC_DIR / "config"

for d in (STATIC_DIR, TEMPLATE_DIR, GENERATED_DIR, WEB_DIR, CONFIG_DIR):
    d.mkdir(parents=True, exist_ok=True)

# 스티커 템플릿 파일명
TEMPLATE_BY_TYPE = {
    "pregnant": "pregnant.png",
    "disabled": "disabled.png",
    "resident": "resident.png",
}
# Enum 매핑
STICKER_ENUM_MAP = {
    "pregnant": "PREGNANT",
    "disabled": "DISABLED",
    "resident": "RESIDENT",
}

# 좌표/기간 기본값(.env로 조절)
def _env_int(name: str, default: int) -> int:
    try:
        v = os.getenv(name)
        return int(v) if v not in (None, "", "null") else default
    except Exception:
        return default

# 텍스트 좌표(템플릿 700px 기준)
X_VALUE_START = _env_int("STICKER_X", 340)
Y_ISSUE       = _env_int("STICKER_Y_ISSUE", 350)
Y_VEHICLE     = _env_int("STICKER_Y_VEHICLE", 410)
Y_EXPIRE      = _env_int("STICKER_Y_EXPIRE", 470)
APT_Y         = _env_int("STICKER_Y_APT", 300)

# 정책 기본일수
PREGNANT_FALLBACK_DAYS = _env_int("PREGNANT_FALLBACK_DAYS", 730)   # 2년
DISABLED_DEFAULT_DAYS  = _env_int("DISABLED_DEFAULT_DAYS", 1825)   # 5년
RESIDENT_DEFAULT_DAYS  = _env_int("RESIDENT_DEFAULT_DAYS", 730)    # 2년

# 거주민 매핑 파일
RESIDENT_MAP_PATH = os.getenv("RESIDENT_MAP_PATH", str(CONFIG_DIR / "resident_map.json"))
_resident_cache: Dict[str, Any] = {"mtime": None, "data": []}

# Windows 폰트 위치
WIN_FONTS = Path("C:/Windows/Fonts")


# =========================
# 1) FastAPI 앱
# =========================
app = FastAPI(title="SafeTag Chatbot API")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"]
)
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")
app.mount("/web", StaticFiles(directory=str(WEB_DIR), html=True), name="web")

bearer_scheme = HTTPBearer(auto_error=False)

@app.get("/", response_class=HTMLResponse)
def root():
    return "<h3>SafeTag Chatbot API</h3><ul><li><a href='/healthz'>/healthz</a></li></ul>"

@app.get("/healthz")
def healthz():
    return {"ok": True}

@app.get("/favicon.ico")
def favicon():
    return Response(status_code=204)


# =========================
# 2) OCR & 전처리 유틸
# =========================
def _ocr_texts_from_images(images: List[Image.Image]) -> List[str]:
    texts: List[str] = []
    for img in images:
        g = img.convert("L")
        # 두 번 추출(일반/이진화) → 합치기
        t1 = pytesseract.image_to_string(g, lang="kor+eng", config="--psm 6")
        bw = g.point(lambda x: 255 if x > 180 else 0, mode="1")
        t2 = pytesseract.image_to_string(bw, lang="kor+eng", config="--psm 6 -c tessedit_char_blacklist=|[]{}")
        texts.append((t1 + "\n" + t2).strip())
    return texts

def guess_type_from_text(text: str) -> Optional[str]:
    if not text:
        return None
    t_lower = text.lower()

    # 임산부 먼저
    if any(k in text for k in ["임산부", "임신", "분만", "출산"]) or \
       any(k in t_lower for k in ["pregnant", "birth", "due"]):
        return "pregnant"

    # 장애인은 임산부 관련 단어가 없을 때만
    if ("장애인" in text or "장애" in text) and ("임산부" not in text and "임신" not in text):
        return "disabled"

    # 거주민
    if any(k in text for k in ["아파트", "거주", "입주", "세대주", "주민등록증"]) or \
       any(k in t_lower for k in ["resident", "apartment", "resi"]):
        return "resident"

    # 영어 백업
    if "preg" in t_lower:
        return "pregnant"
    if "disab" in t_lower:
        return "disabled"
    if "resi" in t_lower or "apart" in t_lower:
        return "resident"

    return None


# =========================
# 3) 날짜 파싱(보강)
# =========================
DATE_RE      = re.compile(r"(\d{2,4})[\s.\-/:년]*([01]?\d)[\s.\-/:월]*([0-3]?\d)")
RE_COMPACT8  = re.compile(r"(?<!\d)(\d{8})(?!\d)")

def _norm_year(y: int) -> int:
    # OCR 오인식: '2026'을 '3026'으로 읽는 경우 보정
    if 3000 <= y <= 3999:
        return y - 1000
    # 00–49 → 2000~2049, 50–99 → 1950~1999
    if y < 100:
        return 2000 + y if y <= 49 else 1900 + y
    return y

def _sanitize_for_dates(t: str) -> str:
    # OCR 오인식 교정(O→0, l/I→1, S→5, B→8, 긴 대시→-)
    trans = {
        "\u004f": "0",  # O
        "\u006f": "0",  # o
        "\u006c": "1",  # l
        "\u0049": "1",  # I
        "\u0053": "5",  # S
        "\u0042": "8",  # B
        "\u2014": "-",  # —
        "\u2013": "-",  # –
        "\u2015": "-",  # ―
    }
    t = t.translate(str.maketrans(trans))
    t = re.sub(r"\s+", " ", t)
    return t

def _find_date_near(text: str, keywords: List[str], window: int = 50) -> Optional[datetime.datetime]:
    t_flat = _sanitize_for_dates(text).replace("\n", " ")
    t_nospace = re.sub(r"\s+", "", t_flat)
    hit = any(kw.replace(" ", "") in t_nospace for kw in keywords)

    for kw in keywords:
        for m in re.finditer(re.escape(kw), t_flat):
            start = max(0, m.start() - window)
            end   = min(len(t_flat), m.end() + window)
            sub = t_flat[start:end]

            m2 = DATE_RE.search(sub)
            if m2:
                y, mo, d = map(int, m2.groups()); y = _norm_year(y)
                try: return datetime.datetime(y, mo, d)
                except: pass

            m3 = RE_COMPACT8.search(sub)
            if m3:
                ymd = m3.group(1)
                y, mo, d = int(ymd[:4]), int(ymd[4:6]), int(ymd[6:8])
                try: return datetime.datetime(y, mo, d)
                except: pass

    if hit:
        m2 = DATE_RE.search(t_flat)
        if m2:
            y, mo, d = map(int, m2.groups()); y = _norm_year(y)
            try: return datetime.datetime(y, mo, d)
            except: pass
        m3 = RE_COMPACT8.search(t_flat)
        if m3:
            ymd = m3.group(1)
            y, mo, d = int(ymd[:4]), int(ymd[4:6]), int(ymd[6:8])
            try: return datetime.datetime(y, mo, d)
            except: pass
    return None

def _parse_any_date(text: str) -> Optional[datetime.datetime]:
    t = _sanitize_for_dates(text).replace("\n", " ")
    m = DATE_RE.search(t)
    if m:
        y, mo, d = map(int, m.groups()); y = _norm_year(y)
        try: return datetime.datetime(y, mo, d)
        except: pass
    m2 = RE_COMPACT8.search(t)
    if m2:
        ymd = m2.group(1)
        y, mo, d = int(ymd[:4]), int(ymd[4:6]), int(ymd[6:8])
        try: return datetime.datetime(y, mo, d)
        except: pass
    return None

def _find_due_date(text: str) -> Optional[datetime.datetime]:
    """
    임산부 서류에서 '분만예정일/출산예정일/예정일'을 탐지하고
    그 근처의 날짜를 찾아 datetime으로 반환.
    """
    t = _sanitize_for_dates(text)
    t_flat = re.sub(r"\s+", " ", t)
    t_nospace = re.sub(r"\s+", "", t)

    # 느슨한 키워드 매칭 (띄어쓰기/오탈자 허용)
    kw_regexes = [
        r"분\s*만\s*예\s*정\s*일",
        r"출\s*산\s*예\s*정\s*일",
        r"예\s*정\s*일",
    ]

    for kw in kw_regexes:
        for m in re.finditer(kw, t_flat):
            start = max(0, m.start() - 80)
            end   = min(len(t_flat), m.end() + 80)
            sub = t_flat[start:end]

            m2 = DATE_RE.search(sub)
            if m2:
                y, mo, d = map(int, m2.groups())
                y = _norm_year(y)
                try:
                    return datetime.datetime(y, mo, d)
                except Exception:
                    pass

            m3 = RE_COMPACT8.search(sub)
            if m3:
                ymd = m3.group(1)
                y, mo, d = int(ymd[:4]), int(ymd[4:6]), int(ymd[6:8])
                try:
                    return datetime.datetime(y, mo, d)
                except Exception:
                    pass

    # 예비 탐색: “분만예정”, “출산예정”, “예정일” 등이 있을 때
    if any(x in t_nospace for x in ["분만예정", "출산예정", "예정일", "예정ll", "예정Il", "예정l"]):
        pick = _pick_future_date(t_flat, max_days=540)  # 18개월 이내 미래 날짜
        if pick:
            return pick

    return None


def _pick_future_date(text: str, max_days: int = 540) -> Optional[datetime.datetime]:
    t = _sanitize_for_dates(text).replace("\n", " ")
    today = datetime.date.today()
    best = None
    for m in DATE_RE.finditer(t):
        y, mo, d = map(int, m.groups()); y = _norm_year(y)
        try: dt = datetime.date(y, mo, d)
        except: continue
        delta = (dt - today).days
        if 0 <= delta <= max_days and ((best is None) or dt < best):
            best = dt
    for m in RE_COMPACT8.finditer(t):
        ymd = m.group(1)
        y, mo, d = int(ymd[:4]), int(ymd[4:6]), int(ymd[6:8])
        try: dt = datetime.date(y, mo, d)
        except: continue
        delta = (dt - today).days
        if 0 <= delta <= max_days and ((best is None) or dt < best):
            best = dt
    return datetime.datetime.combine(best, datetime.time()) if best else None

# 폼으로 받은 dueDate 문자열 파서 (최우선 사용)
def _parse_due_date_str(s: Optional[str]) -> Optional[datetime.datetime]:
    if not s:
        return None
    s = s.strip().replace("년", ".").replace("월", ".").replace("일", "")
    for fmt in ("%Y.%m.%d", "%Y-%m-%d", "%Y/%m/%d", "%y.%m.%d"):
        try:
            return datetime.datetime.strptime(s, fmt)
        except Exception:
            continue
    return None


# =========================
# 4) 거주민 주소 → 단지명 매핑
# =========================
def _normalize_addr(s: str) -> str:
    s = s.strip()
    s = re.sub(r"\s+", "", s)
    s = s.replace("-", "").replace(",", "")
    s = s.replace("서울시", "서울특별시")
    return s

def _load_resident_map() -> List[Dict[str, Any]]:
    path = Path(RESIDENT_MAP_PATH)
    if not path.exists():
        return []
    mtime = path.stat().st_mtime
    if _resident_cache["mtime"] != mtime:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            for row in data:
                row["patterns_norm"] = [_normalize_addr(p) for p in row.get("patterns", [])]
            _resident_cache["data"] = data
            _resident_cache["mtime"] = mtime
        except Exception:
            _resident_cache["data"] = []
    return _resident_cache["data"]

def find_apartment_from_text(ocr_text: str) -> Optional[str]:
    data = _load_resident_map()
    if not data:
        return None
    t_norm = _normalize_addr(ocr_text.replace("\n", " "))
    for row in data:
        for pat in row.get("patterns_norm", []):
            if pat and pat in t_norm:
                return row.get("apartment")
    return None


# =========================
# 5) 폰트/그리기 유틸
# =========================
def _pick_first_existing(paths) -> Optional[str]:
    for p in paths:
        try:
            pp = Path(p)
            if pp.exists():
                return str(pp)
        except Exception:
            pass
    return None

def _load_font(px: int) -> ImageFont.FreeTypeFont:
    candidates: List[Any] = []
    candidates += [
        STATIC_DIR / "fonts" / "Pretendard-Bold.ttf",
        STATIC_DIR / "fonts" / "Pretendard.ttf",
        STATIC_DIR / "fonts" / "PretendardVariable.ttf",
        STATIC_DIR / "fonts" / "NotoSansKR-Bold.ttf",
    ]
    try:
        if WIN_FONTS.exists():
            candidates += list(WIN_FONTS.glob("*Pretendard*Bold*.ttf"))
            candidates += list(WIN_FONTS.glob("*Pretendard*.ttf"))
            candidates += [WIN_FONTS / "malgunbd.ttf", WIN_FONTS / "malgun.ttf"]
    except Exception:
        pass
    candidates += [
        "/usr/share/fonts/truetype/noto/NotoSansKR-Bold.ttf",
        "/usr/share/fonts/truetype/noto/NotoSansKR-Regular.ttf",
    ]
    path = _pick_first_existing(candidates)
    if path:
        try: return ImageFont.truetype(path, px)
        except Exception: pass
    return ImageFont.load_default()

def _rel(W: int, H: int, x: int, y: int, base: int = 700):
    scale = W / float(base) if base else 1.0
    return (int(x * scale), int(y * scale))

def _fit_font(draw: ImageDraw.ImageDraw, text: str, max_px: int, max_width: int) -> ImageFont.FreeTypeFont:
    size = max_px
    while size >= 12:
        font = _load_font(size)
        w = draw.textbbox((0, 0), text, font=font)[2]
        if w <= max_width:
            return font
        size -= 1
    return _load_font(12)

def _pretty_carno(car: str) -> str:
    car = car.replace("-", " ").strip()
    if " " in car:
        return car
    return car[:-4] + " " + car[-4:] if len(car) >= 5 else car

def create_sticker_image(
    sticker_type: str,
    issue_no: str,
    car_no: str,
    issued_at: str,
    expires_at: str,
    request: Request,
    apt_name: Optional[str] = None,
) -> str:
    template_file = TEMPLATE_BY_TYPE.get(sticker_type, "pregnant.png")
    template_path = TEMPLATE_DIR / template_file
    if not template_path.exists():
        raise FileNotFoundError(f"템플릿 없음: {template_path}")

    base = Image.open(template_path).convert("RGBA")
    W, H = base.size
    draw = ImageDraw.Draw(base)
    color = (34, 34, 34, 255)

    x_val    = _rel(W, H, X_VALUE_START, 0)[0]
    y_issue  = _rel(W, H, 0, Y_ISSUE)[1]
    y_veh    = _rel(W, H, 0, Y_VEHICLE)[1]
    y_exp    = _rel(W, H, 0, Y_EXPIRE)[1]
    max_width = int(W * 0.52)
    max_px = max(22, int(W * 0.05))

    car_text = _pretty_carno(car_no)
    font_issue   = _fit_font(draw, issue_no, max_px, max_width)
    font_vehicle = _fit_font(draw, car_text, max_px, max_width)
    font_expire  = _fit_font(draw, expires_at[:10], max_px, max_width)

    # resident 단지명(옵션)
    if sticker_type == "resident" and apt_name:
        apt_y = _rel(W, H, 0, APT_Y)[1]
        apt_x = _rel(W, H, 130, 0)[0]
        apt_font = _fit_font(draw, f"단지: {apt_name}", max(18, int(W * 0.035)), int(W * 0.7))
        draw.text((apt_x, apt_y), f"단지: {apt_name}", fill=color, font=apt_font)

    draw.text((x_val, y_issue), issue_no,        fill=color, font=font_issue)
    draw.text((x_val, y_veh),   car_text,        fill=color, font=font_vehicle)
    draw.text((x_val, y_exp),   expires_at[:10], fill=color, font=font_expire)

    out_name = f"{sticker_type}_{uuid.uuid4().hex[:10]}.png"
    out_path = GENERATED_DIR / out_name
    base.save(out_path)

    base_url = str(request.base_url).rstrip("/")
    return f"{base_url}/static/generated/{out_name}"


# =========================
# 6) Spring 연동 (DEV 스텁)
# =========================
SPRING_BASE_URL = os.getenv("SPRING_BASE_URL", "http://localhost:8080")
USER_ME_PATH    = os.getenv("USER_ME_PATH", "/api/user/me")
MYPAGE_GET_PATH = os.getenv("MYPAGE_GET_PATH", "/api/mypage")

async def call_spring(method: str, path: str, auth: Optional[str] = None, json_body: Optional[Dict[str, Any]] = None):
    if DEV_FAKE_SPRING:
        # 간단 스텁
        if path.startswith("/api/sticker/issue") and method.upper() == "POST":
            from datetime import date, timedelta
            days = (json_body or {}).get("validDays", 730)
            return {
                "stickerId": "DEV-STICKER-001",
                "issuedAt": date.today().isoformat(),
                "expiresAt": (date.today() + timedelta(days=days)).isoformat(),
                "issuer": "DEV",
            }
        if path == USER_ME_PATH and method.upper() == "GET":
            return {"carNumber": "12가3456"}
        if path == MYPAGE_GET_PATH and method.upper() == "GET":
            return {"data": {"carNumber": "12가3456"}}

    headers = {"Content-Type": "application/json"}
    if auth:
        headers["Authorization"] = auth
    url = f"{SPRING_BASE_URL}{path}"

    async with httpx.AsyncClient(timeout=20.0) as client:
        r = await client.request(method, url, headers=headers, json=json_body)
        try: js = r.json()
        except Exception: js = None
        if r.status_code >= 400:
            raise httpx.HTTPStatusError(f"{r.status_code} for {url}", request=r.request, response=r)
        return js if js is not None else {"raw": r.text, "status": r.status_code}

async def get_my_car_number(auth: Optional[str]) -> Optional[str]:
    try:
        me = await call_spring("GET", USER_ME_PATH, auth)
        car = (me or {}).get("carNumber") or (me.get("data") or {}).get("carNumber")
        if car: return car
    except httpx.HTTPStatusError:
        pass
    try:
        mp = await call_spring("GET", MYPAGE_GET_PATH, auth)
        car = (mp or {}).get("carNumber") or (mp.get("data") or {}).get("carNumber")
        if car: return car
    except httpx.HTTPStatusError:
        pass
    return None

async def spring_issue_sticker(
    car_number: str,
    enum_type: str,
    valid_days: Optional[int],
    auth: Optional[str],
):

    body: Dict[str, Any] = {
        "carNumber": car_number,
        "stickerType": enum_type,
    }
    if valid_days is not None:
        body["validDays"] = int(valid_days)

    return await call_spring("POST", "/api/sticker/issue", auth, body)


# =========================
# 7) /ocr (텍스트만)
# =========================
@app.post("/ocr")
async def ocr(file: UploadFile = File(...)):
    try:
        raw = await file.read()
        await file.close()
        if not raw:
            raise HTTPException(status_code=422, detail="업로드된 파일이 비어 있습니다.")
        if "pdf" in (file.content_type or "").lower() or (file.filename or "").lower().endswith(".pdf"):
            images = convert_from_bytes(raw, dpi=300, fmt="png")
        else:
            try:
                img = Image.open(io.BytesIO(raw)).convert("RGB"); img.load()
                images = [img]
            except UnidentifiedImageError:
                raise HTTPException(status_code=415, detail="이미지 형식을 인식할 수 없습니다.")
        texts = _ocr_texts_from_images(images)
        return {"text": "\n".join(texts)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR 실패: {e}")


# =========================
# 8) /ocr/sticker (핵심)
# =========================
@app.post("/ocr/sticker")
async def ocr_and_issue(
    request: Request,
    file: UploadFile = File(...),
    creds: HTTPAuthorizationCredentials = Security(bearer_scheme),
    carNumber: Optional[str] = Form(None),
    dueDate: Optional[str] = Form(None),        # ★ 추가: 분만예정일 문자열(최우선 사용)
    validDays: Optional[int] = Form(None),
):
    # 인증 체크
    if not creds:
        raise HTTPException(status_code=401, detail="Authorization 헤더가 필요합니다.")
    authorization = f"{creds.scheme} {creds.credentials}"

    # 차량번호 확보
    car_no = carNumber or await get_my_car_number(authorization)
    if not car_no:
        return {"status": "FAILED", "reason": "차량번호를 찾을 수 없습니다."}

    # 파일 로드
    try:
        raw = await file.read()
        await file.close()
        if not raw:
            raise HTTPException(status_code=422, detail="파일이 비어 있습니다.")

        if "pdf" in (file.content_type or "").lower() or (file.filename or "").lower().endswith(".pdf"):
            pages = convert_from_bytes(raw, dpi=300, fmt="png")
        else:
            try:
                img = Image.open(io.BytesIO(raw)).convert("RGB")
                img.load()
                pages = [img]
            except UnidentifiedImageError:
                raise HTTPException(status_code=415, detail="이미지 형식을 인식할 수 없습니다.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"파일 처리 실패: {e}")

    # OCR
    texts = _ocr_texts_from_images(pages)
    text = "\n".join(texts)

    # 유형 판별
    sticker_type = guess_type_from_text(text)
    if not sticker_type:
        return {"status": "FAILED", "reason": "서류에서 인증 단어를 찾지 못했습니다.", "ocrText": text}

    # 거주민 단지명 검색(옵션)
    apt_name_for_print = None
    has_target_addr = False
    if sticker_type == "resident":
        apt = find_apartment_from_text(text)
        if apt:
            apt_name_for_print = apt
            has_target_addr = True

    # 유효기간 계산
    today = datetime.date.today()
    calc_valid_days: int

    # 1순위: 프론트에서 validDays를 주면 그대로 사용
    if validDays is not None and str(validDays).strip() != "":
        try:
            calc_valid_days = int(validDays)
        except Exception:
            calc_valid_days = RESIDENT_DEFAULT_DAYS
    else:
        # 2순위: 임산부는 dueDate(폼) → OCR 탐지 순
        if sticker_type == "pregnant":
            due = _parse_due_date_str(dueDate) \
                  or _find_due_date(text) \
                  or _find_date_near(text, ["분만예정일", "분만 예정일", "출산예정일", "출산 예정일", "예정일"]) \
                  or _pick_future_date(text, max_days=540)

            if due:
                # 18개월 이내가 아니면 (예: 3026 같은 오인식) 1000년 보정 한 번 더 시도
                if (due.date() - today).days > 540 and 3000 <= due.year <= 3999:
                    due = due.replace(year=due.year - 1000)

                expire = (due + datetime.timedelta(days=180)).date()  # +6개월
                delta = (expire - today).days
                calc_valid_days = max(30, min(delta, 730))             # 최소 30일, 최대 2년 캡
            else:
                calc_valid_days = PREGNANT_FALLBACK_DAYS

        elif sticker_type == "disabled":
            end = _find_date_near(text, ["유효기간", "만료일", "종료일", "기간"]) or _parse_any_date(text)
            if end:
                delta = (end.date() - today).days
                calc_valid_days = DISABLED_DEFAULT_DAYS if delta < 60 else max(30, delta)
            else:
                calc_valid_days = DISABLED_DEFAULT_DAYS

        else:  # resident
            lease_end = _find_date_near(text, ["계약만료", "만료일", "종료일", "계약기간"]) or _parse_any_date(text)
            if lease_end:
                delta = (lease_end.date() - today).days
                calc_valid_days = max(30, delta)
            else:
                calc_valid_days = RESIDENT_DEFAULT_DAYS

    # Spring 발급 호출
    enum_type = STICKER_ENUM_MAP.get(sticker_type, sticker_type.upper())
    try:
        issue_res = await spring_issue_sticker(car_no, enum_type, calc_valid_days, authorization)
    except httpx.HTTPStatusError as he:
        raise HTTPException(status_code=he.response.status_code if he.response else 502,
                            detail=he.response.text if he.response else "SPRING error")

    issue_no   = str(issue_res.get("stickerId", "")) or uuid.uuid4().hex[:8]
    issued_at  = (issue_res.get("issuedAt")  or today.isoformat())
    expires_at = (issue_res.get("expiresAt") or (today + datetime.timedelta(days=calc_valid_days)).isoformat())
    issuer     = issue_res.get("issuer") or "SAFETAG"


    # 스티커 PNG 생성
    image_url = create_sticker_image(
        sticker_type, issue_no, car_no, issued_at, expires_at, request,
        apt_name=apt_name_for_print
    )

    debug_info = {
        "detectedType": sticker_type,
        "dueDateFromForm": dueDate or None,  # ← 어떤 경로를 탔는지 확인용
        "pregDueFromKeywords": str(_find_date_near(text, ["분만예정일","분만 예정일","출산예정일","출산 예정일","예정일"])),
        "pickedFutureDate": str(_pick_future_date(text, max_days=540)),
        "finalValidDays": calc_valid_days,
        "issuedAt": issued_at,
        "expiresAt": expires_at,
    }

    return {
        "status": "SUCCESS",
        "stickerType": sticker_type,
        "ocrText": text,
        "sticker": {
            "stickerId": issue_no,
            "type": enum_type,
            "imageUrl": image_url,
            "carNumber": car_no,
            "issuedAt": issued_at,
            "expiresAt": expires_at,
            "issuer": issuer,
            "apartmentName": apt_name_for_print if sticker_type == "resident" else None,
            "residentAddressMatched": has_target_addr if sticker_type == "resident" else None,
        },
        "debug": debug_info,
    }


# =========================
# 9) /chat (프론트 호환)
# =========================
class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str

class ChatResponse(BaseModel):
    content: str

SYSTEM_PROMPT = (
    "너는 Safe Tag 앱의 AI 챗봇 '세이피'야. "
    "앱 사용법/정책/기능에 대해 간결하게 답하고, "
    "디지털 스티커 발급·인증(아파트 거주/임산부/장애인)·OTP/통화중계는 "
    "백엔드 API로 처리된다고 가정하고 절차를 안내해."
)

# OpenAI(옵션)
try:
    from openai import OpenAI
    _OPENAI_KEY = os.getenv("OPENAI_API_KEY")
    oai = OpenAI(api_key=_OPENAI_KEY) if _OPENAI_KEY else None
except Exception:
    oai = None

@app.post("/chat", response_model=ChatResponse)
async def chat(req: Dict[str, Any]):
    """
    프론트의 postChat(messages)와 호환되는 최소 구현.
    OPENAI_API_KEY가 있으면 모델 호출, 없으면 로컬 에코.
    """
    try:
        messages = req.get("messages") or []
        msgs = [{"role": "system", "content": SYSTEM_PROMPT}]
        for m in messages:
            role = m.get("role") if m.get("role") in ("system","user","assistant") else "user"
            msgs.append({"role": role, "content": str(m.get("content") or "")})

        if oai:
            comp = oai.chat.completions.create(
                model="gpt-4o-mini",
                messages=msgs,
                temperature=0.4,
                max_tokens=600,
            )
            content = comp.choices[0].message.content or ""
            return ChatResponse(content=content)

        last_user = next((m["content"] for m in reversed(msgs) if m["role"]=="user"), "")
        return ChatResponse(content=f"(로컬 모드) 요청을 받았어요: {last_user[:200]}")
    except Exception as e:
        return ChatResponse(content=f"오류가 발생했습니다: {e}")
