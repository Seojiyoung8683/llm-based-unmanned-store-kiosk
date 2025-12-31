# JARVIS
## **On-Device AI 기반 무인 판매점 키오스크 시스템**

---

## 📌 Project Overview

**JARVIS**는 Qualcomm **QCS6490** 기반의 **온디바이스 AI**를 활용한  
무인 판매점용 지능형 키오스크 시스템이다.

본 프로젝트는 클라우드 서버에 의존하지 않고  
디바이스 내부에서 **음성 인식(STT), 대규모 언어 모델(LLM), 음성 합성(TTS), 이상행동 감지**를 수행함으로써  
**보안성, 응답 속도, 전력 효율**을 동시에 확보하는 것을 목표로 한다.

특히 Qualcomm **Hexagon DSP(NPU)** 가속과 **JNI 기반 구조**를 통해  
실제 임베디드 환경에서 동작 가능한 온디바이스 AI 파이프라인을 구현하였다.

---

## 🎯 Key Features

- 🎤 **음성 기반 고객 응대**
  - VAD → STT → LLM → TTS 파이프라인
- 🧠 **온디바이스 LLM 추론**
  - Qualcomm QNN + Hexagon DSP 가속
- 🏪 **매장 제어 기능**
  - 음성 명령을 통한 문 열기, 조명 제어, 음악 재생
- 🔒 **완전 로컬 처리**
  - 영상·음성 데이터 외부 서버 전송 없음

---

## 🏗 System Architecture

본 시스템은 **3계층 온디바이스 AI 아키텍처**로 구성된다.

[Kotlin / Java Layer]
└─ ConversationService
└─ LlmClient (JNI Interface)
└─ Native C++ Inference Engine
└─ QNN Runtime (HTP / Hexagon DSP)

yaml
코드 복사

- Android(Java/Kotlin) 환경에서는 NPU 직접 호출이 불가능하므로 **JNI 필수**
- C++ Native Layer에서 QNN Runtime 및 Genie Dialog를 통해 추론 수행

---

## 🔄 Data Flow

사용자 음성 입력
→ VAD (Voice Activity Detection)
→ STT (Sherpa-ONNX)
→ ConversationService
→ LLM 추론 (JNI → QNN → DSP)
→ Command Token 생성
→ 매장 제어 / TTS 음성 출력

yaml
코드 복사

---

## 🛠 Tech Stack

### 🔹 Hardware
- Qualcomm **QCS6490 Turnkey Board**
- Hexagon DSP (HTP Backend)
- USB 오디오 인터페이스 (마이크 / 스피커)

### 🔹 Software
- Android OS
- Kotlin + Jetpack Compose
- JNI / C++
- Qualcomm AI Engine Direct (**QNN**)
- Genie Dialog Framework
- **Sherpa-ONNX**
  - STT (SenseVoice)
  - TTS (VITS)
  - VAD (Silero)
- three.js (매장 시뮬레이션)

---

## 📦 Model Design & Optimization

### 🔹 Model
- Transformer 기반 **경량 LLM (Decoder-only)**
- Function Calling 중심 설계

### 🔹 Optimization
- **Post-Training Quantization (INT8)**
- KV Cache 활용으로 추론 지연 최소화
- Hexagon DSP(HTP Backend) 타겟 최적화

---

## 🔌 API & Interface

### 🔹 LLM Inference API

```kotlin
LlmClient.runInference(prompt: String): LlmResult
🔹 Response Example
json
코드 복사
{
  "token": "TURN_ON_LIGHT",
  "parameters": {
    "room": "entrance"
  },
  "rawText": "입구의 불을 켤게요."
}
🔹 Supported Command Tokens
Token	Description
OPEN_DOOR	문 열기
CLOSE_DOOR	문 닫기
TURN_ON_LIGHT	조명 켜기
PLAY_MUSIC	음악 재생
STOP_MUSIC	음악 정지

🚀 Deployment
🔹 Model & Runtime Installation
모델 파일 경로

swift
코드 복사
/data/local/tmp/model/
모델 파일 전송

bash
코드 복사
adb push model.bin /data/local/tmp/model/
QNN Runtime

.so 파일 → jniLibs/arm64-v8a 디렉토리에 포함

🔹 Requirements
Android 10 이상

RAM 4GB 이상 권장

DSP 가속 필수

🎬 Demo
1️⃣ 메인 페이지
<img width="365" src="https://github.com/user-attachments/assets/a5f23771-c63c-4d2f-b5a0-acfa8f4ac083" />
키오스크 초기 진입 화면
상품 탐색, 음성 입력, 결제 등 주요 기능의 진입점

2️⃣ 음성 입력 기반 매장 상호작용
<img width="367" src="https://github.com/user-attachments/assets/dabc30bf-4fb6-477e-b09a-4b2c28ef1f60" />
“문 열어줘” → 3D 매장 입구 개방

“새우깡 어디에 있어?” →
“새우깡은 A구역에 있습니다” 음성 응답과 함께 상품 디테일 정보 표시

“A구역에 뭐가 있어?” →
해당 구역에 위치한 상품 목록 안내

3️⃣ 제품 목록 페이지
<img width="366" src="https://github.com/user-attachments/assets/39bbe17d-d030-473a-9842-94eac30a8616" />
매장 내 전체 상품을 카테고리별로 확인 가능

4️⃣ 제품 디테일 페이지
<img width="365" src="https://github.com/user-attachments/assets/aec9053a-82a0-43bf-b03d-cd38b5d2e24e" />
상품의 가격, 설명, 이미지 등 상세 정보 제공

5️⃣ 결제 페이지
<img width="362" src="https://github.com/user-attachments/assets/22340a7f-edfe-4647-81a2-5e770fd3b5fb" />
무인 키오스크 환경에 최적화된 결제 UI 제공

6️⃣ 관리자 모드 진입
<img width="369" src="https://github.com/user-attachments/assets/9fc5579f-70fc-462d-8d39-11d4ebd61792" />
메인 페이지에서 아이콘 4회 클릭 시
관리자 비밀번호 입력 화면 표시

7️⃣ 관리자 대시보드
<img width="369" src="https://github.com/user-attachments/assets/8620051f-c13d-4862-8ed3-ecc2102c3b86" />
금일 매출

거래 건수

평균 거래 금액

일별 매출

많이 팔린 상품 TOP 5

8️⃣ 매장 제어 페이지
<img width="363" src="https://github.com/user-attachments/assets/8aff4f10-5997-4fd4-9ff5-0dd79ac4a3ab" />
관리자 권한으로
문, 조명, 에어컨, 블라인드 제어

9️⃣ 상품 및 재고 관리
<img width="361" src="https://github.com/user-attachments/assets/0aedc6b5-e8ea-4835-a094-08ac2e0d8729" />
상품 생성, 수정, 삭제 기능 제공
재고 현황 확인 및 재고 리필 관리

🧠 Retrospective
JNI 기반 온디바이스 AI 구조에 대한 실전 이해

NPU/DSP 환경에서의 모델 최적화 경험

로그가 제한적인 하드웨어 환경에서의 디버깅 경험

단순 기능 구현을 넘어 시스템 전체 흐름 설계의 중요성을 체감

yaml
코드 복사
