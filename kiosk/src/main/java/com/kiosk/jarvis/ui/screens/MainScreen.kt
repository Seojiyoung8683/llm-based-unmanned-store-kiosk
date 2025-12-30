package com.kiosk.jarvis.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiosk.jarvis.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToPayment: () -> Unit,
    onNavigateToVoiceInput: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    // ── 관리자 진입 관련 상태값
    val scope = rememberCoroutineScope()
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun registerSecretTap() {
        val now = System.currentTimeMillis()
        tapCount = if (now - lastTapAt <= 2000L) tapCount + 1 else 1 // 2초 내 연속탭만 인정
        lastTapAt = now
        if (tapCount >= 6) {
            tapCount = 0
            showPasswordDialog = true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFF7C3AED)
                    )
                )
            )
    ) {
        Surface(
            onClick = { showLanguageDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .shadow(8.dp, CircleShape),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Language",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 스크롤 + 하단 패딩 유지
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(top = 32.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .shadow(24.dp, CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    )
                    .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.robot),
                        contentDescription = "Jarvis Robot",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .clickable { registerSecretTap() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "JARVIS",
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(20.dp))
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (language == "ko") "무인매장 도우미" else "Unmanned Store Assistant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp * pulseScale)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                )

                FloatingActionButton(
                    onClick = onNavigateToVoiceInput,
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(16.dp, CircleShape),
                    containerColor = Color.White,
                    contentColor = Color(0xFF6366F1),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Input",
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == "ko") "음성" else "Voice",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            MainMenuButton(
                text = if (language == "ko") "제품 목록" else "Products",
                icon = Icons.Filled.ShoppingBag,
                onClick = onNavigateToProducts
            )

            Spacer(modifier = Modifier.height(16.dp))

            MainMenuButton(
                text = if (language == "ko") "바로 결제하기" else "Direct Payment",
                icon = Icons.Filled.Payment,
                onClick = onNavigateToPayment
            )
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF6366F1)
                    )
                }
            },
            title = {
                Text(
                    "언어 선택 / Select Language",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = {
                            onLanguageChange("ko")
                            showLanguageDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = if (language == "ko") Color(0xFF6366F1) else Color(0xFFF3F4F6)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🇰🇷", fontSize = 28.sp)
                            Text(
                                text = "한국어",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (language == "ko") Color.White else Color(0xFF374151)
                            )
                            if (language == "ko") {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            onLanguageChange("en")
                            showLanguageDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = if (language == "en") Color(0xFF6366F1) else Color(0xFFF3F4F6)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🇺🇸", fontSize = 28.sp)
                            Text(
                                text = "English",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (language == "en") Color.White else Color(0xFF374151)
                            )
                            if (language == "en") {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF6366F1)
                    )
                ) {
                    Text("닫기 / Close", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                passwordInput = ""
                passwordError = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF6366F1))
                }
            },
            title = { Text("관리자 로그인", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        passwordError = null
                    },
                    label = { Text("비밀번호") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = {
                        if (passwordError != null) {
                            Text(passwordError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput == "1234") {
                            showPasswordDialog = false
                            passwordInput = ""
                            passwordError = null
                            onNavigateToDashboard()
                        } else {
                            passwordError = "비밀번호가 올바르지 않습니다."
                            scope.launch {
                                delay(150)
                                passwordInput = ""
                            }
                        }
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPasswordDialog = false
                        passwordInput = ""
                        passwordError = null
                    }
                ) { Text("취소") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun MainMenuButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(320.dp)
            .height(76.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF6366F1)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF6366F1).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF6366F1)
                )
            }
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
