package com.kiosk.jarvis.ui.screens.admin

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kiosk.jarvis.navigation.AppNavigationDrawer
import com.kiosk.jarvis.ui.dashboard.DashboardUiState
import com.kiosk.jarvis.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Locale.ROOT

/* ─────────────────────────────────────────────────────────────
   ✅ 로컬 DB(Room) 기반: model.dashboard 타입 그대로 사용
   ───────────────────────────────────────────────────────────── */
typealias SalesData    = com.kiosk.jarvis.model.dashboard.SalesData
typealias SalesMetrics = com.kiosk.jarvis.model.dashboard.SalesMetrics
typealias StoreSummary = com.kiosk.jarvis.model.dashboard.StoreSummary
typealias TopProduct   = com.kiosk.jarvis.model.dashboard.TopProduct

/* ─────────────────────────────────────────────────────────────
   리플렉션 기반 안전 접근 헬퍼
   ───────────────────────────────────────────────────────────── */
private fun Any?.readFieldRaw(vararg names: String): Any? {
    if (this == null) return null
    val cls = this.javaClass
    // 1) 필드 직접 접근
    for (n in names) {
        runCatching {
            val f = cls.getDeclaredField(n)
            f.isAccessible = true
            return f.get(this)
        }
    }
    // 2) 게터 메서드 (getXxx / isXxx)
    for (n in names) {
        val pascal =
            n.replaceFirstChar { if (it.isLowerCase()) it.titlecase(ROOT) else it.toString() }
        val candidates = arrayOf("get$pascal", "is$pascal")
        for (m in candidates) {
            val v = runCatching {
                val method: Method = cls.getMethod(m)
                method.isAccessible = true
                method.invoke(this)
            }.getOrNull()
            if (v != null) return v
        }
    }
    return null
}

private fun Any?.readDouble(vararg names: String, default: Double = 0.0): Double =
    (readFieldRaw(*names) as? Number)?.toDouble() ?: default

private fun Any?.readInt(vararg names: String, default: Int = 0): Int =
    (readFieldRaw(*names) as? Number)?.toInt() ?: default

private fun Any?.readString(vararg names: String, default: String = ""): String =
    readFieldRaw(*names)?.toString() ?: default

/* status가 Enum/문자열 어떤 형태든 안정적으로 읽기 */
private fun Any?.readStatusString(vararg names: String, default: String = "OFFLINE"): String {
    val raw = readFieldRaw(*names) ?: return default
    return when (raw) {
        is Enum<*> -> raw.name
        else       -> raw.toString()
    }
}

/* ─────────────────────────────────────────────────────────────
   Dashboard
   ───────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(),
    // ✅ 키오스크 메인으로 돌아가기 콜백 (AppNavHost 경로에서만 전달)
    onNavigateToKiosk: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val context = LocalContext.current

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
                            "대시보드",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "메뉴"
                            )
                        }
                    },
                    actions = {
                        // 🏠 홈 아이콘: 항상 표시
                        IconButton(
                            onClick = {
                                Log.d(
                                    "DashboardScreen",
                                    "Home clicked, hasCallback=${onNavigateToKiosk != null}"
                                )
                                if (onNavigateToKiosk != null) {
                                    // ✅ 키오스크 NavHost 경로: AppNavHost 에서 넘겨준 콜백으로 메인 이동
                                    onNavigateToKiosk()
                                } else {
                                    // ✅ 콜백이 없으면 그냥 NavController 기준으로 뒤로 가기만 수행
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "키오스크 메인으로"
                            )
                        }

                        // 🔄 새로고침 버튼
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "새로고침"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF6366F1),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
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
                when {
                    uiState.isLoading -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF6366F1),
                                strokeWidth = 4.dp
                            )
                        }
                    }

                    uiState.error != null -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    uiState.error ?: "오류 발생",
                                    color = Color(0xFFF44336),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = { viewModel.refreshData() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    )
                                ) {
                                    Text("다시 시도")
                                }
                            }
                        }
                    }

                    else -> {
                        DashboardContent(
                            uiState = uiState,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Content
   ───────────────────────────────────────────────────────────── */
@Composable
fun DashboardContent(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 매출 지표 카드
        item {
            uiState.salesMetrics?.let { m -> SalesMetricsCard(m) }
        }

        // 매장 현황
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "매장 현황",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
        }

        itemsIndexed(uiState.storeSummaries) { _, s ->
            StoreSummaryCard(s)
        }

        // 일별 매출 추이
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "일별 매출 추이",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
        }
        item {
            DailySalesChart(uiState.dailySales)
        }

        // 인기 상품
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "인기 상품 TOP 5",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
        }
        itemsIndexed(uiState.topProducts.take(5)) { index, p ->
            TopProductCard(p, rank = index + 1)
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Cards
   ───────────────────────────────────────────────────────────── */
@Composable
fun SalesMetricsCard(metrics: SalesMetrics) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.KOREA) }

    val totalRevenue = metrics.readDouble("totalRevenue", "revenue", "todayRevenue")
    val totalTx      = metrics.readInt(
        "totalTransactions",
        "transactions",
        "txCount",
        "totalOrders"
    )
    val avgTx        = metrics.readDouble(
        "averageTransaction",
        "avgTransaction",
        "avgTx",
        "avgOrderValue",
        default = if (totalTx > 0) totalRevenue / totalTx else 0.0
    )
    val revChange    = metrics.readDouble(
        "revenueChange",
        "revenueChangePct",
        "revenueDeltaPct",
        "changeRevenuePct",
        default = Double.NaN
    )
    val txChange     = metrics.readDouble(
        "transactionChange",
        "transactionsChangePct",
        "txChangePct",
        "changeOrdersPct",
        default = Double.NaN
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        "오늘의 매출",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        icon = Icons.Default.TrendingUp,
                        label = "총 매출",
                        value = currencyFormat.format(totalRevenue),
                        change = revChange.takeIf { !it.isNaN() },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    MetricItem(
                        icon = Icons.Default.Receipt,
                        label = "거래 건수",
                        value = "${totalTx}건",
                        change = txChange.takeIf { !it.isNaN() },
                        modifier = Modifier.weight(1f)
                    )
                }

                MetricItem(
                    icon = Icons.Default.Calculate,
                    label = "평균 거래액",
                    value = currencyFormat.format(avgTx),
                    change = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    change: Double?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )

            if (change != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val up = change >= 0
                    Icon(
                        if (up) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        null,
                        tint = if (up) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (up) "+" else ""}%.1f%%".format(change),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (up) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StoreSummaryCard(store: StoreSummary) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.KOREA) }

    val storeName         = store.readString("storeName", "name")
    val todayRevenue      = store.readDouble("todayRevenue", "revenue", "sales")
    val todayTransactions = store.readInt(
        "todayTransactions",
        "transactions",
        "txCount",
        "todayOrders"
    )
    val statusStr         = store.readStatusString("status", "state", "storeStatus")

    val (statusColor, statusText, statusIcon) = when (statusStr.uppercase(ROOT)) {
        "ONLINE", "RUNNING", "ACTIVE"      -> Triple(Color(0xFF4CAF50), "운영중", Icons.Default.CheckCircle)
        "OFFLINE", "INACTIVE"              -> Triple(Color(0xFF9E9E9E), "오프라인", Icons.Default.Cancel)
        "MAINTENANCE", "MAINTAINING"       -> Triple(Color(0xFFFFA726), "점검중", Icons.Default.Build)
        "ERROR", "FAIL", "ALERT"           -> Triple(Color(0xFFF44336), "오류", Icons.Default.Error)
        else                               -> Triple(Color(0xFF9E9E9E), "미정", Icons.Default.Help)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "오늘 매출",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            currencyFormat.format(todayRevenue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1),
                            fontSize = 18.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "거래 건수",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            "${todayTransactions}건",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   ✅ 일별 매출 추이 (orders 기반, 날짜·매출만)
   ───────────────────────────────────────────────────────────── */
@Composable
fun DailySalesChart(salesData: List<SalesData>) {
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val maxRevenue =
                salesData.maxOfOrNull {
                    it.readDouble("revenue", "amount", "sales", default = 0.0)
                } ?: 1.0

            salesData.forEach { data ->
                // ✅ dateMillis 기준으로 날짜 라벨 생성
                val millis = (data.readFieldRaw("dateMillis") as? Number)?.toLong() ?: 0L
                val dateLabel =
                    if (millis > 0L) dateFormat.format(Date(millis))
                    else data.readString("date", "day", "label") // 혹시 모를 예전 데이터 대비

                val revenue =
                    data.readDouble("revenue", "amount", "sales", default = 0.0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(60.dp),
                        fontSize = 13.sp
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(
                                    fraction = (
                                            if (maxRevenue > 0) (revenue / maxRevenue) else 0.0
                                            ).toFloat()
                                )
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFF8B5CF6)
                                        )
                                    )
                                )
                        )
                    }

                    Text(
                        // 원 단위 그대로 표시 (필요하면 /1000 해서 "천원" 단위로 바꿔도 됨)
                        NumberFormat.getCurrencyInstance(Locale.KOREA).format(revenue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.width(90.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TopProductCard(product: TopProduct, rank: Int) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.KOREA) }

    val name    = product.readString("productName", "name", "title")
    val count   = product.readInt("salesCount", "count", "qty", "quantity")
    val revenue = product.readDouble("revenue", "amount", "sales")

    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFF6366F1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(rankColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$rank",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${count}개 판매",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    currencyFormat.format(revenue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1),
                    fontSize = 18.sp
                )
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF4CAF50).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "인기",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}
