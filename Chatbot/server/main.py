from fastapi import FastAPI, HTTPException, Header, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Literal, List, Optional
from openai import OpenAI
import httpx, json
from PIL import Image
import pytesseract
import io
import os

try:
    from dotenv import load_dotenv  # pip install python-dotenv
    load_dotenv()
except Exception:
    pass

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    raise RuntimeError("OPENAI_API_KEY is not set in environment variables.")

# === Added: OpenAI client 생성 ===
oai = OpenAI(api_key=OPENAI_API_KEY)

app = FastAPI(title="SafeTag Chatbot API (Function Calling)")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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

# 한글 → 영문 enum 정규화
KOR_TO_ENUM = {
    "임산부": "pregnant",
    "임신": "pregnant",
    "거주자": "resident",
    "아파트": "resident",
    "장애인": "disabled",
    "장애": "disabled",
}
def normalize_type(raw: str) -> str:
    if not raw:
        raise ValueError("type is empty")
    s = str(raw).strip().lower()
    if s in ("pregnant", "resident", "disabled"):
        return s
    for k, v in KOR_TO_ENUM.items():
        if k in raw:
            return v
    if "preg" in s: return "pregnant"
    if "resi" in s or "apart" in s: return "resident"
    if "disab" in s: return "disabled"
    raise ValueError(f"unsupported type: {raw}")

# 요청/응답 모델
class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str

class ChatRequest(BaseModel):
    messages: List[ChatMessage]

class ChatResponse(BaseModel):
    content: str

# Spring 프록시 유틸
SPRING_BASE_URL = "http://localhost:8081"

async def call_spring(method: str, path: str, auth: Optional[str] = None, json_body: dict | None = None):
    headers = {"Content-Type": "application/json"}
    if auth:
        headers["Authorization"] = auth
    async with httpx.AsyncClient(timeout=15.0) as client:
        r = await client.request(method, f"{SPRING_BASE_URL}{path}", headers=headers, json=json_body)
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

# 정규화 적용 래퍼(원본은 보존)
_start_verification_raw = start_verification
async def start_verification(type_: str, file_id: str, auth: Optional[str] = None):
    t = normalize_type(type_)
    return await _start_verification_raw(t, file_id, auth)

_issue_sticker_raw = issue_sticker
async def issue_sticker(vehicle_number: str, sticker_type: str, valid_days: int | None = None, auth: Optional[str] = None):
    st = normalize_type(sticker_type)
    return await _issue_sticker_raw(vehicle_number, st, valid_days, auth)

# OpenAI Tools(Function) 스키마
TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "start_verification",
            "description": "서류 인증 시작(아파트 거주/임산부/장애인). 업로드된 파일 ID 필요.",
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

# /chat : Function Calling 적용
@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, authorization: str | None = Header(default=None)):
    try:
        # 1) 사용자 대화 + 시스템 프롬프트 구성
        msgs = [{"role": "system", "content": SYSTEM_PROMPT}]
        msgs += [m.model_dump() for m in req.messages]

        # 2) 1차 호출: 답변 or 도구 호출 결정
        comp = oai.chat.completions.create(
            model="gpt-4o-mini",
            messages=msgs,
            tools=TOOLS,
            temperature=0.4,
            max_tokens=700,
        )
        choice = comp.choices[0]
        message = choice.message

        # 3) 도구 호출이 없는 일반 Q&A라면 바로 반환
        if not message.tool_calls:
            return ChatResponse(content=message.content or "")

        # 4) 도구 호출이 하나 이상이면 모두 실행
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

        # 5) 도구 결과를 모델에 전달하여 자연어 응답 생성
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
        # Spring 에러를 그대로 전달(상태코드 유지)
        raise HTTPException(status_code=he.response.status_code, detail={"error": f"SPRING_DOWN: {str(he)}"})
    except Exception as e:
        raise HTTPException(status_code=500, detail={"error": str(e)})

    
# OCR 엔드포인트
@app.post("/ocr")
async def ocr(file: UploadFile):
    try:
        img = Image.open(io.BytesIO(await file.read()))
        # 전처리 없이 가동. 필요 시 그레이스케일/이진화 추가 가능
        text = pytesseract.image_to_string(img, lang="kor+eng")
        return {"text": text}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR 실패: {e}")


# OCR + 자동 스티커 발급 엔드포인트
@app.post("/ocr/sticker")
async def ocr_and_issue(file: UploadFile, authorization: str | None = Header(default=None)):
    try:
        # 1) OCR 처리
        img = Image.open(io.BytesIO(await file.read()))
        text = pytesseract.image_to_string(img, lang="kor+eng")
        text = text.strip()
        
        # 2) 유형 판별
        sticker_type = None
        if "임산부" in text:
            sticker_type = "pregnant"
        elif "장애인" in text:
            sticker_type = "disabled"
        elif "아파트" in text or "거주" in text:
            sticker_type = "resident"

        # 3) 조건 충족 여부 확인
        if not sticker_type:
            return {"status": "FAILED", "reason": "인증에 실패하였습니다. 다시 시도해 주세요.", "ocrText": text}
        
        # 4) 스티커 발급 (임시 차량번호: 'TEST-1234')
        result = await issue_sticker("TEST-1234", sticker_type, 30, authorization)

        return {
            "status": "SUCCESS",
            "stickerType": sticker_type,
            "ocrText": text,
            "result": result
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR+스티커 발급 실패: {e}")
