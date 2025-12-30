#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "Llama2Genie"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

//   public native int Init(String modelPath);
//   public native String Infer(String prompt);
//   public native void Release();

// Init(modelPath: String): Int
extern "C"
JNIEXPORT jint JNICALL
Java_com_kiosk_jarvis_engine_LlmClient_Init(
        JNIEnv* env,
        jobject thiz,          // 인스턴스 메서드 → jobject
        jstring jModelPath
) {
    const char* modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    LOGI("Initializing LLM with model path: %s", modelPath ? modelPath : "(null)");

    // TODO: 여기서 실제 Genie / QNN 초기화 로직 수행
    // int rc = ...;

    env->ReleaseStringUTFChars(jModelPath, modelPath);

    // 일단 테스트용으로 0 리턴 (성공)
    return 0;
}

// Infer(prompt: String): String
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kiosk_jarvis_engine_LlmClient_Infer(
        JNIEnv* env,
        jobject thiz,
        jstring jPrompt
) {
    const char* prompt = env->GetStringUTFChars(jPrompt, nullptr);
    std::string promptStr = prompt ? prompt : "";
    LOGI("Infer called with prompt: %s", prompt ? prompt : "(null)");

    std::string reply;  // 최종적으로 Kotlin으로 돌려줄 문자열

    // ------------------------------------------------------------------
    // ⚙️ 1. 규칙 기반 매핑
    //    ConversationService 에서 넘어오는 prompt 예:
    //    "Below is the query ... Query: 문 열어줘 Response:"
    //    → 여기서는 한국어 문장 키워드 기준으로 분기
    // ------------------------------------------------------------------

    // 1) 출입문 제어: <jarvis_1>(enable=True/False)
    if (promptStr.find("문 열") != std::string::npos ||
        promptStr.find("문 좀 열") != std::string::npos) {
        // 문 열기
        reply = "<jarvis_1>(enable=True)";
    }
    else if (promptStr.find("문 잠궈") != std::string::npos ||
             promptStr.find("문 잠가") != std::string::npos ||
             promptStr.find("문 잠가줘") != std::string::npos ||
             promptStr.find("문 닫") != std::string::npos) {
        // 문 잠그기 / 닫기
        reply = "<jarvis_1>(enable=False)";
    }

        // 2) 조명 제어: <jarvis_0>(enable=True/False)
    else if (promptStr.find("불 켜") != std::string::npos ||
             promptStr.find("조명 켜") != std::string::npos ||
             promptStr.find("불 좀 켜") != std::string::npos) {
        reply = "<jarvis_0>(enable=True)";
    }
    else if (promptStr.find("불 꺼") != std::string::npos ||
             promptStr.find("조명 꺼") != std::string::npos ||
             promptStr.find("불 좀 꺼") != std::string::npos) {
        reply = "<jarvis_0>(enable=False)";
    }

        // 3) 에어컨 제어: <jarvis_2>(enable=True/False)
    else if (promptStr.find("에어컨 켜") != std::string::npos ||
             promptStr.find("에어컨 좀 켜") != std::string::npos) {
        reply = "<jarvis_2>(enable=True)";
    }
    else if (promptStr.find("에어컨 꺼") != std::string::npos ||
             promptStr.find("에어컨 좀 꺼") != std::string::npos) {
        reply = "<jarvis_2>(enable=False)";
    }

        // 4) 블라인드 제어: <jarvis_3>(enable=True/False)
    else if (promptStr.find("블라인드 올려") != std::string::npos ||
             promptStr.find("블라인드 좀 올려") != std::string::npos) {
        reply = "<jarvis_3>(enable=True)";
    }
    else if (promptStr.find("블라인드 내려") != std::string::npos ||
             promptStr.find("블라인드 좀 내려") != std::string::npos) {
        reply = "<jarvis_3>(enable=False)";
    }

        // 5) 제품 위치 탐색: <jarvis_4>(product="1"~"10")
    else if (promptStr.find("홈런볼") != std::string::npos) {
        reply = "<jarvis_4>(product=1)";
    }
    else if (promptStr.find("새우깡") != std::string::npos) {
        reply = "<jarvis_4>(product=2)";
    }
    else if (promptStr.find("꼬북칩") != std::string::npos) {
        reply = "<jarvis_4>(product=3)";
    }
    else if (promptStr.find("빼빼로") != std::string::npos) {
        reply = "<jarvis_4>(product=4)";
    }
    else if (promptStr.find("초코파이") != std::string::npos) {
        reply = "<jarvis_4>(product=5)";
    }
    else if (promptStr.find("고래밥") != std::string::npos) {
        reply = "<jarvis_4>(product=6)";
    }
    else if (promptStr.find("콜라") != std::string::npos) {
        reply = "<jarvis_4>(product=7)";
    }
    else if (promptStr.find("사이다") != std::string::npos) {
        reply = "<jarvis_4>(product=8)";
    }
    else if (promptStr.find("오렌지주스") != std::string::npos) {
        reply = "<jarvis_4>(product=9)";
    }
    else if (promptStr.find("초코우유") != std::string::npos) {
        reply = "<jarvis_4>(product=10)";
    }

        // 6) 구역/냉장고 안내: <jarvis_5>(section="1"~"4")
    else if (promptStr.find("A구역") != std::string::npos ||
             promptStr.find("에이구역") != std::string::npos) {
        reply = "<jarvis_5>(section=1)";
    }
    else if (promptStr.find("B구역") != std::string::npos ||
             promptStr.find("비구역") != std::string::npos) {
        reply = "<jarvis_5>(section=2)";
    }
    else if (promptStr.find("냉장고1") != std::string::npos ||
             promptStr.find("냉장고 1") != std::string::npos) {
        reply = "<jarvis_5>(section=3)";
    }
    else if (promptStr.find("냉장고2") != std::string::npos ||
             promptStr.find("냉장고 2") != std::string::npos) {
        reply = "<jarvis_5>(section=4)";
    }

        // 7) 음악 제어: <jarvis_6>(enable=True/False)
    else if (promptStr.find("음악 켜") != std::string::npos ||
             promptStr.find("노래 틀어") != std::string::npos ||
             promptStr.find("노래 좀 틀어") != std::string::npos) {
        reply = "<jarvis_6>(enable=True)";
    }
    else if (promptStr.find("음악 꺼") != std::string::npos ||
             promptStr.find("노래 꺼") != std::string::npos) {
        reply = "<jarvis_6>(enable=False)";
    }

        // 8) 가습기 제어: <jarvis_7>(enable=True/False)
    else if (promptStr.find("가습기 켜") != std::string::npos ||
             promptStr.find("가습기 좀 켜") != std::string::npos) {
        reply = "<jarvis_7>(enable=True)";
    }
    else if (promptStr.find("가습기 꺼") != std::string::npos ||
             promptStr.find("가습기 좀 꺼") != std::string::npos) {
        reply = "<jarvis_7>(enable=False)";
    }

        // ------------------------------------------------------------------
        // ⚙️ 2. 어떤 규칙에도 안 걸리면 → reply = "" (토큰 없음)
        //    Kotlin 쪽에서 파싱 실패하면 STT 원문을 그대로 읽어주는
        //    fallback 로직이 이미 들어가 있어서, 여기서는 비워둠.
        // ------------------------------------------------------------------
    else {
        LOGI("No rule matched. Returning empty reply (will fallback to STT text).");
        reply = "";
    }

    env->ReleaseStringUTFChars(jPrompt, prompt);

    return env->NewStringUTF(reply.c_str());
}

// Release(): void
extern "C"
JNIEXPORT void JNICALL
Java_com_kiosk_jarvis_engine_LlmClient_Release(
        JNIEnv* env,
jobject thiz
) {
LOGI("Releasing LLM resources");

// TODO: 여기서 실제 리소스 해제 로직 수행
}
