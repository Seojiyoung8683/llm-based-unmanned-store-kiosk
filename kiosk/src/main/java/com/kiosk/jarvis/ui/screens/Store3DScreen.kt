package com.kiosk.jarvis.ui.screens

import android.graphics.Color
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun Store3DScreen(
    initialCommand: String? = null,   // "openDoor", "closeDoor", "musicOn", ...
    onClose: () -> Unit               // 5초 후 화면 닫기용 콜백
) {
    // 5초 후 자동으로 닫기
    LaunchedEffect(initialCommand) {
        if (initialCommand != null) {
            delay(5000L)
            onClose()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setBackgroundColor(Color.BLACK)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadsImagesAutomatically = true
                    cacheMode = WebSettings.LOAD_NO_CACHE

                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true

                    javaScriptCanOpenWindowsAutomatically = true

                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                }

                webChromeClient = WebChromeClient()

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        initialCommand?.let { command ->
                            val js = """
                                (function() {
                                  if (window.handleKioskCommand) {
                                    window.handleKioskCommand("$command");
                                  } else {
                                    console.warn("handleKioskCommand is not defined");
                                  }
                                })();
                            """.trimIndent()

                            view?.evaluateJavascript(js, null)
                        }
                    }
                }

                // JS → 안드로이드 콜백
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onCommandFromJs(command: String) {

                    }
                }, "AndroidBridge")

                // 3D 매장 HTML 로드
                loadUrl("file:///android_asset/index.html")
            }
        },
        update = { webView ->

        }
    )
}
