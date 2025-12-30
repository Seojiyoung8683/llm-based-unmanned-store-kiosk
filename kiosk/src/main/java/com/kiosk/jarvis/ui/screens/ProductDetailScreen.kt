// C:/kioskfinal/kiosk/src/main/java/com/kiosk/jarvis/ui/screens/ProductDetailScreen.kt
package com.kiosk.jarvis.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiosk.jarvis.model.Product
import com.kiosk.jarvis.repository.ProductRepository
import com.kiosk.jarvis.ui.util.ImageRes
import com.kiosk.jarvis.viewmodel.CartViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    productId: Int,
    language: String,
    onBackClick: () -> Unit,
    onRecommendedProductClick: (Int) -> Unit,
    cartViewModel: CartViewModel
) = ProductDetailScreen(
    productId = productId.toString(),
    language = language,
    onBackClick = onBackClick,
    onRecommendedProductClick = { idStr ->
        onRecommendedProductClick(idStr.toIntOrNull() ?: -1)
    },
    cartViewModel = cartViewModel
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    language: String,
    onBackClick: () -> Unit,
    onRecommendedProductClick: (String) -> Unit,
    cartViewModel: CartViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            Log.d("ProductDetailScreen", "ensureSeeded() 호출")
            ProductRepository.ensureSeeded(context)
        }
    }

    val inventoryList by remember {
        ProductRepository.observeInventory(context)
    }.collectAsState(initial = emptyList())

    val current = remember(inventoryList, productId) {
        Log.d(
            "ProductDetailScreen",
            "inventoryList size=${inventoryList.size}, 찾는 productId=$productId, ids=${
                inventoryList.joinToString { it.product.id.toString() }
            }"
        )
        inventoryList.firstOrNull { it.product.id.toString() == productId }
    }

    val product: Product? = current?.product
    val stock: Int = current?.stock ?: 0

    val outOfStock = stock <= 0
    val lowStock = stock in 1..5

    val recommendedProducts: List<Product> = remember(inventoryList, productId) {
        inventoryList
            .map { it.product }
            .filter { it.id.toString() != productId }
            .take(3)
    }

    LaunchedEffect(current?.product?.id) {
        if (current != null) {
            Log.d(
                "ProductDetailScreen",
                "상품 찾음(id=${current.product.id}), 5초 후 자동 뒤로가기 시작"
            )
            delay(5000)
            Log.d("ProductDetailScreen", "5초 경과 → onBackClick() 호출")
            onBackClick()
        } else {
            Log.d("ProductDetailScreen", "current == null → 자동 뒤로가기는 실행 안 함")
        }
    }

    // ================== 로딩 / 에러 상태 처리 ==================
    when {
        inventoryList.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "상품 정보를 불러오는 중입니다...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            return
        }

        product == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "상품 정보를 찾을 수 없습니다.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "productId = $productId",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            return
        }
    }

    // ================== 실제 상세 화면 ==================
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (language == "ko") product.nameKo else product.nameEn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF5F7FA), Color.White)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // 이미지 카드
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val imageRes = ImageRes.forProductId(context, product.id.toString())
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = if (language == "ko") product.nameKo else product.nameEn,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 상품 정보 카드
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (language == "ko") product.nameKo else product.nameEn,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${product.price}원",
                                fontSize = 32.sp,
                                color = Color(0xFF6366F1),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Divider(color = Color(0xFFE5E7EB))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = when {
                                        outOfStock -> Color(0xFFF44336)
                                        lowStock   -> Color(0xFFFFA726)
                                        else       -> Color(0xFF4CAF50)
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (language == "ko") "재고" else "Stock",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    outOfStock -> Color(0xFFF44336).copy(alpha = 0.15f)
                                    lowStock   -> Color(0xFFFFA726).copy(alpha = 0.15f)
                                    else       -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = "${stock}${if (language == "ko") "개" else " units"}",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontSize = 16.sp,
                                    color = when {
                                        outOfStock -> Color(0xFFF44336)
                                        lowStock   -> Color(0xFFFFA726)
                                        else       -> Color(0xFF4CAF50)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 위치 카드
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF6366F1).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (language == "ko") "위치" else "Location",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = product.location,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 추천 상품
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Recommend,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (language == "ko") "추천 상품" else "Recommended Products",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }

                Spacer(Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(recommendedProducts) { rp ->
                        RecommendedProductCard(
                            product = rp,
                            language = language,
                            onClick = { onRecommendedProductClick(rp.id.toString()) }
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 장바구니
                Button(
                    onClick = {
                        Log.d(
                            "CartDebug",
                            "상세에서 담기 클릭: id=${product.id}, name=${product.nameKo}, stock=$stock"
                        )
                        cartViewModel.addToCart(product, stock)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (outOfStock) Color(0xFF9CA3AF) else Color(0xFF6366F1),
                        disabledContainerColor = Color(0xFF9CA3AF)
                    ),
                    enabled = !outOfStock,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (outOfStock) Icons.Default.Block else Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (language == "ko") {
                                if (outOfStock) "품절" else "장바구니에 담기"
                            } else {
                                if (outOfStock) "Sold Out" else "Add to Cart"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RecommendedProductCard(
    product: Product,
    language: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(160.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                val img = ImageRes.forProductId(context, product.id.toString())
                Image(
                    painter = painterResource(id = img),
                    contentDescription = if (language == "ko") product.nameKo else product.nameEn,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = if (language == "ko") product.nameKo else product.nameEn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${product.price}원",
                    fontSize = 14.sp,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
