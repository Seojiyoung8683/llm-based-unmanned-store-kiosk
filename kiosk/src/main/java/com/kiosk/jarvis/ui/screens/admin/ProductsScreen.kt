package com.kiosk.jarvis.ui.screens.admin


import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kiosk.jarvis.model.Product
import com.kiosk.jarvis.model.ProductCategory
import com.kiosk.jarvis.navigation.AppNavigationDrawer
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    navController: NavController
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ProductsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProductsViewModel(application) as T
            }
        }
    )

    val uiState by viewModel.ui.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    // 상품 추가/수정 다이얼로그 상태
    var isEditorOpen by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(uiState.error, uiState.toast) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
        uiState.toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(
                navController = navController,
                currentRoute = currentRoute,
                onItemClick = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("상품 관리", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "메뉴")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                // 상품 추가 모드
                                editingProduct = null
                                isEditorOpen = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "상품 추가")
                        }
                        IconButton(onClick = { viewModel.load() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF6366F1),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFF5F7FA), Color(0xFFE8EAF6))
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(padding)) {
                    CategoryFilter(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.filterBy(it) }
                    )

                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = Color(0xFF6366F1),
                                strokeWidth = 4.dp
                            )
                        }
                    } else {
                        val filteredProducts = uiState.selectedCategory?.let { cat ->
                            uiState.products.filter { it.category == cat }
                        } ?: uiState.products

                        ProductList(
                            products = filteredProducts,
                            onDeleteProduct = { id -> viewModel.delete(id) },
                            onEditProduct = { product ->
                                editingProduct = product
                                isEditorOpen = true
                            }
                        )
                    }
                }

                // 상품 추가/수정 다이얼로그
                if (isEditorOpen) {
                    ProductEditorDialog(
                        initialProduct = editingProduct,
                        onDismiss = { isEditorOpen = false },
                        onSave = { nameKo, nameEn, category, priceText, location, description, barcode ->
                            val price = priceText.toIntOrNull() ?: 0
                            viewModel.saveProduct(
                                existingId = editingProduct?.id,
                                nameKo = nameKo,
                                nameEn = nameEn,
                                category = category,
                                price = price,
                                location = location,
                                description = description,
                                barcode = barcode.ifBlank { null }
                            )
                            isEditorOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryFilter(
    selectedCategory: ProductCategory?,
    onCategorySelected: (ProductCategory?) -> Unit
) {
    val categoryData: Map<ProductCategory?, Triple<String, ImageVector, Color>> = mapOf(
        null to Triple("전체", Icons.Default.Apps, Color(0xFF6366F1)),
        ProductCategory.DRINK to Triple("음료", Icons.Default.LocalDrink, Color(0xFF2196F3)),
        ProductCategory.SNACK to Triple("스낵", Icons.Default.Cookie, Color(0xFFFFA726))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val (label, icon, color) = categoryData[null]!!
                CategoryChip(
                    label = label, icon = icon, color = color,
                    isSelected = selectedCategory == null,
                    onClick = { onCategorySelected(null) }
                )
            }
            items(ProductCategory.values()) { category ->
                val (label, icon, color) = categoryData[category]!!
                CategoryChip(
                    label = label, icon = icon, color = color,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(
    label: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.shadow(if (isSelected) 4.dp else 2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color else Color(0xFFF5F7FA),
        border = BorderStroke(1.dp, if (isSelected) color else color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (isSelected) Color.White else color)
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isSelected) Color.White else Color.Gray
            )
        }
    }
}

@Composable
fun ProductList(
    products: List<Product>,
    onDeleteProduct: (String) -> Unit,
    onEditProduct: (Product) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFF6366F1).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF6366F1)
                            )
                        }
                        Text(
                            text = "상품이 없습니다",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "새로운 상품을 추가해보세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(products) { product ->
                ProductCard(
                    product = product,
                    onDelete = onDeleteProduct,
                    onEdit = onEditProduct
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onDelete: (String) -> Unit,
    onEdit: (Product) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA)
    val categoryColor = when (product.category) {
        ProductCategory.DRINK -> Color(0xFF2196F3)
        ProductCategory.SNACK -> Color(0xFFFFA726)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(IntrinsicSize.Min)
                    .background(categoryColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProductThumbnail(
                            imageRes = product.imageRes,
                            fallbackIcon = when (product.category) {
                                ProductCategory.DRINK -> Icons.Default.LocalDrink
                                ProductCategory.SNACK -> Icons.Default.Cookie
                            },
                            tint = categoryColor
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = product.nameKo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (product.nameEn.isNotBlank()) {
                                Text(
                                    text = product.nameEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "판매 가격",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = currencyFormat.format(product.price),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1),
                                    fontSize = 24.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFF6366F1).copy(alpha = 0.3f)
                            )
                        }
                    }

                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    if (product.location.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = categoryColor
                                )
                                Text(
                                    text = "위치: ${product.location}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onEdit(product) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF6366F1).copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "수정",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFF44336).copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFF44336).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFF44336)
                    )
                }
            },
            title = { Text("상품 삭제", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Text(
                    "${product.nameKo}을(를) 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.",
                    fontSize = 15.sp, color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = { onDelete(product.id); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("삭제", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }, shape = RoundedCornerShape(8.dp)) {
                    Text("취소", fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ProductThumbnail(
    imageRes: Int,
    fallbackIcon: ImageVector,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != 0) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(fallbackIcon, null, tint = tint)
        }
    }
}

/** 상품 추가/수정용 다이얼로그 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditorDialog(
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onSave: (
        nameKo: String,
        nameEn: String,
        category: ProductCategory,
        price: String,
        location: String,
        description: String,
        barcode: String
    ) -> Unit
) {
    var nameKo by remember(initialProduct) { mutableStateOf(initialProduct?.nameKo ?: "") }
    var nameEn by remember(initialProduct) { mutableStateOf(initialProduct?.nameEn ?: "") }
    var category by remember(initialProduct) { mutableStateOf(initialProduct?.category ?: ProductCategory.SNACK) }
    var price by remember(initialProduct) { mutableStateOf(initialProduct?.price?.toString() ?: "") }
    var location by remember(initialProduct) { mutableStateOf(initialProduct?.location ?: "") }
    var description by remember(initialProduct) { mutableStateOf(initialProduct?.description ?: "") }
    var barcode by remember(initialProduct) { mutableStateOf(initialProduct?.barcode ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onSave(nameKo, nameEn, category, price, location, description, barcode)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (initialProduct == null) "추가" else "수정")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("취소")
            }
        },
        title = {
            Text(
                text = if (initialProduct == null) "상품 추가" else "상품 수정",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nameKo,
                    onValueChange = { nameKo = it },
                    label = { Text("상품명(한글)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("상품명(영문)") },
                    singleLine = true
                )

                Text("카테고리", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = category == ProductCategory.SNACK,
                            onClick = { category = ProductCategory.SNACK }
                        )
                        Text("스낵")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = category == ProductCategory.DRINK,
                            onClick = { category = ProductCategory.DRINK }
                        )
                        Text("음료")
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { ch -> ch.isDigit() } },
                    label = { Text("가격(원)") },
                    singleLine = true,

                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("위치(예: A, 냉장고1)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
                    maxLines = 3
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("바코드 (선택)") },
                    singleLine = true
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
