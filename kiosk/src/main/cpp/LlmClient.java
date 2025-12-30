package com.kiosk.jarvis.engine;

import android.util.Log;

public class LlmClient {
    static {
        try {
            System.loadLibrary("Llama2Genie");
            Log.d("LlmClient", "✅ Llama2Genie native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e("LlmClient", "❌ Failed to load Llama2Genie", e);
        }
    }

    public native int Init(String modelPath);
    public native String Infer(String prompt);
    public native void Release();

    public static boolean isNativeAvailable() {
        try {
            System.loadLibrary("Llama2Genie");
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}
