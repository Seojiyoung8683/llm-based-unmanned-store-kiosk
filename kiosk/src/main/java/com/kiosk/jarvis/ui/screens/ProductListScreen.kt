package com.kiosk.jarvis.ui.screens

import android.util.Log
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiosk.jarvis.model.Product
import com.kiosk.jarvis.model.ProductCategory
import com.kiosk.jarvis.repository.ProductRepository
import com.kiosk.jarvis.ui.util.ImageRes
import com.kiosk.jarvis.viewmodel.CartViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    language: String,
    onProductClick: (String) -> Unit,     // String으로 통일
    onBackClick: () -> Unit,
    cartViewModel: CartViewModel
) {
    val popularIds = remember { setOf("1") }   // 예시
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            ProductRepository.ensureSeeded(context)
        }
    }

    val inventoryList by remember {
        ProductRepository.observeInventory(context)
    }.collectAsState(initial = emptyList())

    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var query by remember { mutableStateOf("") }

    // 카테고리 필터
    val base = if (selectedCategory != null) {
        inventoryList.filter { it.product.category == selectedCategory }
    } else {
        inventoryList
    }

    // 검색 필터
    val displayItems = remember(base, query, language) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) base
        else base.filter { pi ->
            val p = pi.product
            p.nameKo.lowercase().contains(q) || p.nameEn.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (language == "ko") "제품 목록" else "Products",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
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
                        colors = listOf(
                            Color(0xFFF5F7FA),
                            Color(0xFFE8EAF6)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
            ) {
                // 검색창
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF6366F1)
                            )
                        },
                        placeholder = {
                            Text(
                                if (language == "ko") "제품명을 검색하세요" else "Search products…",
                                color = Color.Gray
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 카테고리 필터
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryChip(
                        text = if (language == "ko") "전체" else "All",
                        icon = Icons.Default.Apps,
                        isSelected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                    CategoryChip(
                        text = if (language == "ko") "과자" else "Snacks",
                        icon = Icons.Default.Fastfood,
                        isSelected = selectedCategory == ProductCategory.SNACK,
                        onClick = { selectedCategory = ProductCategory.SNACK }
                    )
                    CategoryChip(
                        text = if (language == "ko") "음료" else "Drinks",
                        icon = Icons.Default.LocalDrink,
                        isSelected = selectedCategory == ProductCategory.DRINK,
                        onClick = { selectedCategory = ProductCategory.DRINK }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 결과 개수
                if (displayItems.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (language == "ko")
                                "${displayItems.size}개의 제품"
                            else
                                "${displayItems.size} products",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 제품 그리드
                if (displayItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        Color(0xFF6366F1).copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = Color(0xFF6366F1).copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = if (language == "ko") "검색 결과가 없습니다" else "No results found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(displayItems) { pi ->
                            val product = pi.product
                            val stock = pi.stock
                            ProductCard(
                                product = product,
                                stock = stock,
                                language = language,
                                isPopular = popularIds.contains(product.id.toString()),
                                onClick = { onProductClick(product.id.toString()) },
                                onAddToCart = {
                                    Log.d("CartDebug", "담기 클릭: id=${product.id}, name=${product.nameKo}, stock=$stock")
                                    cartViewModel.addToCart(product, stock)
                                }                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF6366F1) else Color.White,
        modifier = Modifier
            .height(48.dp)
            .shadow(if (isSelected) 6.dp else 3.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF6366F1),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFF111827),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    stock: Int,
    language: String,
    isPopular: Boolean,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    val outOfStock = stock <= 0
    val lowStock = stock in 1..5

    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val cardShape = RoundedCornerShape(16.dp)
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, cardShape)
            .then(
                if (isPopular) {
                    Modifier.border(
                        width = 3.dp,
                        color = Color(0xFFFF6B6B).copy(alpha = 0.7f * pulseAlpha),
                        shape = cardShape
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6))
            ) {
                val imageRes = ImageRes.forProductId(context, product.id.toString())
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = (if (language == "ko") product.nameKo else product.nameEn),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isPopular) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .shadow(4.dp, RoundedCornerShape(999.dp)),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFFF6B6B)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🔥", fontSize = 14.sp)
                            Text(
                                text = if (language == "ko") "인기" else "HOT",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (outOfStock || lowStock) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .shadow(4.dp, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        color = if (outOfStock) Color(0xFFF44336) else Color(0xFFFFA726)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (outOfStock) Icons.Default.Block else Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (language == "ko") {
                                    if (outOfStock) "품절" else "재고↓"
                                } else {
                                    if (outOfStock) "Out" else "Low"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (language == "ko") product.nameKo else product.nameEn,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${product.price}원",
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = when {
                        outOfStock -> Color(0xFFF44336)
                        lowStock -> Color(0xFFFFA726)
                        else -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (language == "ko") "재고: ${stock}개" else "Stock: $stock",
                    fontSize = 13.sp,
                    color = when {
                        outOfStock -> Color(0xFFF44336)
                        lowStock -> Color(0xFFFFA726)
                        else -> Color.Gray
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAddToCart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (outOfStock) Color(0xFF9CA3AF) else Color(0xFF6366F1),
                    disabledContainerColor = Color(0xFF9CA3AF)
                ),
                enabled = !outOfStock,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (outOfStock) Icons.Default.Block else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (language == "ko") {
                            if (outOfStock) "품절" else "담기"
                        } else {
                            if (outOfStock) "Sold Out" else "Add"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
