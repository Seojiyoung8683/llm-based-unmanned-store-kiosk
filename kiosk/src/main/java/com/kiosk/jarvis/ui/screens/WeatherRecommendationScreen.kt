package com.kiosk.jarvis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiosk.jarvis.R
import com.kiosk.jarvis.model.ProductData
import com.kiosk.jarvis.ui.util.ImageRes
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext

// ---- 날씨 타입 & 테마 정의 ----
private enum class WeatherType { Sunny, Rainy, Cloudy }

private data class WeatherTheme(
    val background: Brush,
    val appBar: Color,
    val accent: Color,
    val emoji: String,
    val labelKo: String,
    val labelEn: String
)

private fun themeFor(conditionText: String, language: String): WeatherTheme {
    val text = conditionText.lowercase(Locale.getDefault())
    val type = when {
        "비" in text || "rain" in text -> WeatherType.Rainy
        "흐" in text || "cloud" in text || "overcast" in text -> WeatherType.Cloudy
        else -> WeatherType.Sunny
    }
    return when (type) {
        WeatherType.Sunny -> WeatherTheme(
            background = Brush.verticalGradient(
                listOf(Color(0xFFFFF7AE), Color(0xFFFFE08A), Color(0xFFFFC46B))
            ),
            appBar = Color(0xFFFFB020),
            accent = Color(0xFFFB8C00),
            emoji = "☀️",
            labelKo = "맑음",
            labelEn = "Sunny"
        )
        WeatherType.Rainy -> WeatherTheme(
            background = Brush.verticalGradient(
                listOf(Color(0xFFB3C7E6), Color(0xFF8DA6C7), Color(0xFF6C88AB))
            ),
            appBar = Color(0xFF527AA2),
            accent = Color(0xFF3F6B93),
            emoji = "🌧️",
            labelKo = "비",
            labelEn = "Rainy"
        )
        WeatherType.Cloudy -> WeatherTheme(
            background = Brush.verticalGradient(
                listOf(Color(0xFFD9DEE5), Color(0xFFBCC3CC), Color(0xFFA7B0BA))
            ),
            appBar = Color(0xFF8E98A3),
            accent = Color(0xFF7B8692),
            emoji = "☁️",
            labelKo = "흐림",
            labelEn = "Cloudy"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherRecommendationScreen(
    language: String,
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    // ---- 날짜 ----
    val currentDate = remember(language) {
        SimpleDateFormat(
            if (language == "ko") "yyyy년 MM월 dd일" else "MMM dd, yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    // ---- 모의 날씨 데이터 ----
    val temperature = 22
    val weatherConditionText = if (language == "ko") "맑음" else "Sunny"

    // ---- 테마 계산 ----
    val theme = remember(weatherConditionText, language) {
        themeFor(weatherConditionText, language)
    }

    // ---- 추천 상품 ----
    val recommendedProducts = remember(theme) {
        ProductData.products.take(4)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (language == "ko") "오늘의 날씨 & 추천" else "Weather & Recommendations",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.appBar
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    // Weather Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.accent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentDate,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = theme.emoji, fontSize = 80.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "$temperature°C",
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (language == "ko") theme.labelKo else theme.labelEn,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (language == "ko") "오늘의 추천 상품" else "Today's Recommendations",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(recommendedProducts) { product ->
                    WeatherRecommendationCard(
                        product = product,
                        language = language,
                        onClick = { onProductClick(product.id.toString()) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun WeatherRecommendationCard(
    product: com.kiosk.jarvis.model.Product,
    language: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageRes = ImageRes.forProductId(context, product.id.toString())

            Image(
                painter = painterResource(id = imageRes),
                contentDescription = if (language == "ko") product.nameKo else product.nameEn,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == "ko") product.nameKo else product.nameEn,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${product.price}원",
                    fontSize = 16.sp,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${if (language == "ko") "위치" else "Location"}: ${product.location}",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}
