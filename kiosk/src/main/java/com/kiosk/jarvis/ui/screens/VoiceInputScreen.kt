// C:/kioskfinal/kiosk/src/main/java/com/kiosk/jarvis/ui/screens/VoiceInputScreen.kt
package com.kiosk.jarvis.ui.screens

import android.Manifest
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.kiosk.jarvis.service.ConversationService
import com.kiosk.jarvis.service.ConversationService.Event
import com.kiosk.jarvis.service.ConversationService.State

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceInputScreen(
    conversationService: ConversationService,
    language: String,
    onNavigateBack: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onOpenStore3D: (String) -> Unit,
    onOpenProductDetail: (String) -> Unit,
) {
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val csState by conversationService.state.collectAsState()
    val logs by conversationService.initLogs.collectAsState()

    LaunchedEffect(Unit) {
        conversationService.initializeApp()
    }

    var errorMessage by remember { mutableStateOf("") }

    val isInitializing = csState is State.STATE_INIT_NOW

    val recognizedText: String = remember(logs) {
        logs.lastOrNull { it.startsWith("STT: ") }?.let { line ->
            val main = line.removePrefix("STT: ").trim()
            val idx = main.lastIndexOf(" (")
            if (idx > 0) main.substring(0, idx) else main
        } ?: ""
    }

    val llmRaw: String = remember(logs) {
        logs.lastOrNull { it.startsWith("LLM: ") }?.let { line ->
            val main = line.removePrefix("LLM: ").trim()
            val idx = main.lastIndexOf(" (")
            if (idx > 0) main.substring(0, idx) else main
        } ?: ""
    }

    val jarvisResponse: String = remember(logs) {
        logs.lastOrNull { it.startsWith("TTS: ") }?.let { line ->
            val main = line.removePrefix("TTS: ").trim()
            val idx = main.lastIndexOf(" (")
            if (idx > 0) main.substring(0, idx) else main
        } ?: ""
    }

    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotBlank()) {
            onVoiceResult(recognizedText)
        }
    }

    var lastHandledLlm by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(llmRaw) {
        if (llmRaw.isBlank()) return@LaunchedEffect

        val trimmed = llmRaw.trim()
        if (trimmed == lastHandledLlm) return@LaunchedEffect
        lastHandledLlm = trimmed

        val lower = trimmed.lowercase()
        Log.d("VoiceInputScreen", "llmRaw for routing: $trimmed")

        val isDoorToken = "<jarvis_1>" in lower      // 문
        val isLightToken = "<jarvis_0>" in lower     // 조명
        val isAirconToken = "<jarvis_3>" in lower    // 에어컨
        val isMusicToken = "<jarvis_7>" in lower     // 음악

        val isOffIntent =
            "enable=false" in lower ||
                    "off=true" in lower ||
                    "light_off" in lower ||
                    "turn_off_light" in lower

        val isOnIntent =
            "enable=true" in lower ||
                    "on=true" in lower ||
                    "light_on" in lower ||
                    "turn_on_light" in lower

        // -------------------------
        // 1) 문/조명/에어컨/음악 → 3D
        // -------------------------
        val command3d: String? = when {
            // 문
            isDoorToken && isOffIntent -> "closeDoor"
            isDoorToken && isOnIntent  -> "openDoor"
            isDoorToken                -> "openDoor"

            // 조명
            isLightToken && isOffIntent -> "lightOff"
            isLightToken && isOnIntent  -> "lightOn"
            isLightToken                -> "lightOn"

            // 에어컨
            isAirconToken && isOffIntent -> "acOff"
            isAirconToken && isOnIntent  -> "acOn"
            isAirconToken                -> "acOn"

            // 음악
            isMusicToken && isOffIntent -> "musicOff"
            isMusicToken && isOnIntent  -> "musicOn"
            isMusicToken                -> "musicOn"

            else -> null
        }

        if (command3d != null) {
            Log.d("VoiceInputScreen", ">>> 3D 매장 이동 command=$command3d")
            onOpenStore3D(command3d)
            conversationService.clearLogs()
            return@LaunchedEffect
        }

        // 2) 제품 위치 안내 → 상세 페이지
        //    LLM RAW: <jarvis_4>(product=1)
        if ("<jarvis_4>" in lower) {

            val rawProductId: String? =
                Regex("""product\s*=\s*([0-9]+)""")
                    .find(trimmed)
                    ?.groupValues
                    ?.get(1)

            Log.d("VoiceInputScreen", ">>> jarvis_4 detected. trimmed=$trimmed, parsedId=$rawProductId")

            val mappedId: String? = when (rawProductId) {
                "1" -> "P001"   // 홈런볼
                "2" -> "P002"   // 새우깡
                "3" -> "P003"   // 꼬북칩
                "4" -> "P004"   // 빼빼로
                "5" -> "P005"   // 초코파이
                "6" -> "P006"   // 고래밥
                "7" -> "P007"   // 콜라
                "8" -> "P008"   // 사이다
                "9" -> "P009"   // 오렌지주스
                "10" -> "P010"  // 초코우유
                else -> null
            }

            if (mappedId != null) {
                Log.d("VoiceInputScreen", ">>> 상품 상세 이동 mappedId=$mappedId (raw=$rawProductId)")
                onOpenProductDetail(mappedId)
                conversationService.clearLogs()
            } else {
                Log.w("VoiceInputScreen", "jarvis_4 있지만 매핑 실패: rawId=$rawProductId")
            }

            return@LaunchedEffect
        }


    }

    // ================== 상태 계산 ==================
    val isListening: Boolean = remember(csState) {
        when (csState) {
            is State.STATE_RECORDING,
            is State.STATE_STT_RUNNING,
            is State.STATE_LLM_RUNNING,
            is State.STATE_TTS_RUNNING,
            is State.STATE_PLAYING -> true
            else -> false
        }
    }

    val isProcessingLLM: Boolean = csState is State.STATE_LLM_RUNNING

    fun toggleRecording() {
        Log.d(
            "VoiceInputScreen",
            "Mic button clicked. state=$csState, granted=${micPermissionState.status.isGranted}"
        )

        if (!micPermissionState.status.isGranted) {
            Log.d("VoiceInputScreen", "Mic permission not granted. Requesting...")
            micPermissionState.launchPermissionRequest()
            return
        }

        when (csState) {
            is State.STATE_IDLE,
            is State.STATE_ERROR,
            is State.STATE_INTERRUPTED -> {
                Log.d("VoiceInputScreen", "Sending Event.MicPressed to ConversationService")
                errorMessage = ""
                conversationService.handleEvent(Event.MicPressed)
            }

            is State.STATE_RECORDING,
            is State.STATE_STT_RUNNING,
            is State.STATE_LLM_RUNNING,
            is State.STATE_TTS_RUNNING,
            is State.STATE_PLAYING -> {
                Log.d("VoiceInputScreen", "Sending Event.MicReleased to ConversationService")
                conversationService.handleEvent(Event.MicReleased)
            }

            else -> Unit
        }
    }

    // ================== UI ==================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFEC4899)
                    )
                )
            )
    ) {
        // 상단 뒤로가기
        IconButton(
            onClick = { onNavigateBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 제목
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = if (language == "ko") "음성 입력" else "Voice Input",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                if (isInitializing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language == "ko")
                            "초기화 중입니다... 잠시만 기다려 주세요."
                        else
                            "Initializing... Please wait a moment.",
                        fontSize = 14.sp,
                        color = Color(0xFFE5E7EB),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 안내 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (language == "ko")
                            "마이크 버튼을 눌러 말씀하면,\n오프라인 STT/LLM/TTS로 처리합니다."
                        else
                            "Tap the mic button to speak.\nOffline STT/LLM/TTS will handle your request.",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 🎙 마이크 버튼
            VoiceRecordButton(
                isListening = isListening,
                language = language,
                enabled = micPermissionState.status.isGranted && !isInitializing,
                onClick = { toggleRecording() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 👤 사용자 발화
            if (recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color(0xFF6366F1).copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = if (language == "ko") "사용자 입력" else "Your Input",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                        }
                        Divider(color = Color(0xFFE5E7EB))
                        Text(
                            text = recognizedText,
                            fontSize = 18.sp,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // LLM 처리 중
            if (isProcessingLLM) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF6366F1)
                        )
                        Text(
                            text = if (language == "ko") "응답 생성 중..." else "Generating response...",
                            fontSize = 16.sp,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // JARVIS 응답
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color(0xFF6366F1).copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (language == "ko") "JARVIS 응답" else "JARVIS Response",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                    Divider(color = Color(0xFFE5E7EB))

                    if (jarvisResponse.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (language == "ko")
                                    "AI 응답이 이곳에 표시됩니다"
                                else
                                    "AI response will be displayed here",
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Text(
                                text = if (language == "ko")
                                    "마이크 버튼을 눌러 질문해주세요"
                                else
                                    "Press the microphone button to ask a question",
                                fontSize = 12.sp,
                                color = Color(0xFFD1D5DB),
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = jarvisResponse,
                            fontSize = 16.sp,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // 에러 메시지
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 마이크 권한 경고
            if (!micPermissionState.status.isGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFA726).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (language == "ko")
                                "마이크 권한이 필요합니다"
                            else
                                "Microphone permission required",
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceRecordButton(
    isListening: Boolean,
    language: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (isListening && enabled) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(scale)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(scale * 0.9f)
                        .background(
                            Color.White.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }

            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier
                    .size(140.dp)
                    .shadow(16.dp, CircleShape),
                containerColor = Color.White,
                contentColor = if (isListening) Color(0xFFFF6B6B) else Color(0xFF6366F1)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    modifier = Modifier.size(70.dp)
                )
            }
        }

        Card(
            modifier = Modifier.shadow(8.dp, RoundedCornerShape(999.dp)),
            shape = RoundedCornerShape(999.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    !enabled -> Color.LightGray.copy(alpha = 0.7f)
                    isListening -> Color(0xFFFF6B6B).copy(alpha = 0.9f)
                    else -> Color.White.copy(alpha = 0.2f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isListening && enabled) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = when {
                        !enabled -> {
                            if (language == "ko") "초기화 중..." else "Initializing..."
                        }

                        isListening -> {
                            if (language == "ko") "듣는 중..." else "Listening..."
                        }

                        else -> {
                            if (language == "ko") "탭하여 시작" else "Tap to start"
                        }
                    },
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
