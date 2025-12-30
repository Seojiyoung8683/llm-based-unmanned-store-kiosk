package com.kiosk.jarvis.ui.screens.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kiosk.jarvis.model.*
import com.kiosk.jarvis.navigation.AppNavigationDrawer
import com.kiosk.jarvis.repository.ProductRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    viewModel: InventoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current   // ✅ Context

    // ✅ 화면 진입 시 한 번 로컬 DB에서 재고 로드
    LaunchedEffect(Unit) {
        viewModel.loadInventoryData(context)
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
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
                        Text(
                            "재고 관리",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "메뉴")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.loadInventoryData(context)
                            }
                        ) {
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
                            colors = listOf(
                                Color(0xFFF5F7FA),
                                Color(0xFFE8EAF6)
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(padding)) {
                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFF6366F1),
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                                color = Color(0xFF6366F1),
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            text = {
                                Text(
                                    "재고 현황",
                                    fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            text = {
                                Text(
                                    "리필 작업",
                                    fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            text = {
                                Text(
                                    "입고 예정",
                                    fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF6366F1),
                                strokeWidth = 4.dp
                            )
                        }
                    } else {
                        when (uiState.selectedTab) {
                            0 -> InventoryTab(uiState.inventory)
                            // ✅ 리필 작업 탭: 재고 추가 주문 화면으로 변경
                            1 -> RefillTasksTab(viewModel)
                            2 -> DeliveriesTab(uiState.deliveries)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryTab(inventory: List<InventoryItem>) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(inventory) { item ->
            InventoryCard(item)
        }
    }
}

@Composable
fun InventoryCard(item: InventoryItem) {
    val (statusColor, statusText, statusIcon) = when (item.status) {
        InventoryStatus.NORMAL -> Triple(Color(0xFF4CAF50), "정상", Icons.Default.CheckCircle)
        InventoryStatus.LOW_STOCK -> Triple(Color(0xFFFFA726), "부족", Icons.Default.Warning)
        InventoryStatus.OUT_OF_STOCK -> Triple(Color(0xFFF44336), "품절", Icons.Default.Error)
        InventoryStatus.EXPIRING_SOON -> Triple(Color(0xFFFF9800), "유통기한 임박", Icons.Default.Schedule)
        InventoryStatus.EXPIRED -> Triple(Color(0xFF9E9E9E), "유통기한 만료", Icons.Default.Block)
    }

    val stockPercentage =
        (item.currentStock.toFloat() / item.maxCapacity.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Color(0xFF6366F1).copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF6366F1)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.storeName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F7FA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "현재 재고",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${item.currentStock}개",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 28.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "최소: ${item.minThreshold}개",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "최대: ${item.maxCapacity}개",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "재고 수준",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${(stockPercentage * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE0E0E0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(stockPercentage)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                statusColor,
                                                statusColor.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ──────────────────────────────────────────────
   🔹 리필 작업 탭: 재고 추가 주문 화면 (DB 연동)
   - 왼쪽: 상품 이미지
   - 오른쪽: 현재 재고 + +1 / +5 / +10 버튼
   - 버튼 클릭 시 Room(로컬 DB) stock 증가
   ────────────────────────────────────────────── */

@Composable
fun RefillTasksTab(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 최초 1회: DB 비어 있으면 10개 상품 시드
    LaunchedEffect(Unit) {
        ProductRepository.ensureSeeded(context)
    }

    // Product + 재고 정보 관찰
    val inventoryList by remember {
        ProductRepository.observeInventory(context)
    }.collectAsState(initial = emptyList())

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(inventoryList) { item ->
            RefillOrderCard(
                item = item,
                onIncrease = { delta ->
                    scope.launch {
                        ProductRepository.increaseStock(
                            context = context,
                            productId = item.product.id,
                            delta = delta
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun RefillOrderCard(
    item: ProductRepository.ProductInventory,
    onIncrease: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 🔹 왼쪽: 제품 이미지 (또는 기본 아이콘)
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(start = 12.dp, top = 20.dp, bottom = 20.dp)
                    .background(Color(0xFFF5F7FA), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.product.imageRes != 0) {
                    Image(
                        painter = painterResource(id = item.product.imageRes),
                        contentDescription = item.product.nameKo,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 상단: 상품명 + 카테고리 뱃지
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.product.nameKo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "무인매장 1호점",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF6366F1),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (item.product.category == ProductCategory.DRINK) "음료" else "과자",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 중간: 현재 재고 + 재고 변화 가이드
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F7FA)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "현재 재고",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${item.stock}개",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "추가 주문 후 예시",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${item.stock + 10}개 ( +10 기준 )",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 하단: 수량 증가 버튼들 (+1 / +5 / +10)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RefillAmountButton(label = "+1", onClick = { onIncrease(1) })
                    RefillAmountButton(label = "+5", onClick = { onIncrease(5) })
                    RefillAmountButton(label = "+10", onClick = { onIncrease(10) })
                }
            }
        }
    }
}

@Composable
private fun RefillAmountButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ───────────────────────────────
   나머지 입고 예정 탭은 그대로
   ─────────────────────────────── */

@Composable
fun DeliveriesTab(deliveries: List<VendorDelivery>) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(deliveries) { delivery ->
            DeliveryCard(delivery)
        }
    }
}

@Composable
fun DeliveryCard(delivery: VendorDelivery) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    val (statusColor, statusText, statusIcon) =
        when (delivery.status) {
            DeliveryStatus.SCHEDULED -> Triple(Color(0xFF2196F3), "예정", Icons.Default.Schedule)
            DeliveryStatus.IN_TRANSIT -> Triple(Color(0xFFFFA726), "배송중", Icons.Default.LocalShipping)
            DeliveryStatus.DELIVERED -> Triple(Color(0xFF4CAF50), "도착", Icons.Default.CheckCircle)
            DeliveryStatus.INSPECTED -> Triple(Color(0xFF9C27B0), "검수완료", Icons.Default.FactCheck)
            DeliveryStatus.COMPLETED -> Triple(Color(0xFF4CAF50), "완료", Icons.Default.Done)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                statusColor.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = statusColor
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = delivery.vendorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = delivery.storeName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(statusColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F7FA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "입고 예정 시간",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = dateFormat.format(Date(delivery.expectedDate)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Divider(color = Color(0xFFE0E0E0))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "입고 품목 (${delivery.items.size}개)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                delivery.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F7FA), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF6366F1), CircleShape)
                            )
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "${item.expectedQuantity}개",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }
                }
            }
        }
    }
}
