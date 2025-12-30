package com.kiosk.jarvis.ui.screens.admin

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kiosk.jarvis.navigation.AppNavigationDrawer
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// 모델: PricingModels.kt (com.kiosk.jarvis.model)
import com.kiosk.jarvis.model.PriceAdjustmentType
import com.kiosk.jarvis.model.PriceHistory
import com.kiosk.jarvis.model.Promotion
import com.kiosk.jarvis.model.PromotionType
import com.kiosk.jarvis.model.PricingPolicy
import com.kiosk.jarvis.model.PricingPolicyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    navController: NavController,
    viewModel: PricingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

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
                    title = { Text("가격/프로모션", fontSize = 22.sp) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "메뉴")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: 추가 */ }) {
                            Icon(Icons.Default.Add, contentDescription = "추가")
                        }
                        IconButton(onClick = { viewModel.loadPricingData() }) {
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
                            text = { Text("가격 정책") },
                            icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            text = { Text("프로모션") },
                            icon = { Icon(Icons.Default.LocalOffer, contentDescription = null) }
                        )
                        Tab(
                            selected = uiState.selectedTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            text = { Text("가격 이력") },
                            icon = { Icon(Icons.Default.History, contentDescription = null) }
                        )
                    }

                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = Color(0xFF6366F1),
                                strokeWidth = 4.dp
                            )
                        }
                    } else {
                        when (uiState.selectedTab) {
                            0 -> PricingPoliciesTab(uiState.pricingPolicies)
                            1 -> PromotionsTab(uiState.promotions, viewModel)
                            2 -> PriceHistoryTab(uiState.priceHistory)
                        }
                    }
                }
            }
        }
    }
}

/* -------------------- 가격 정책 탭 -------------------- */

@Composable
fun PricingPoliciesTab(policies: List<PricingPolicy>) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(policies) { policy ->
            PricingPolicyCard(policy)
        }
    }
}

@Composable
private fun policyVisual(policyType: PricingPolicyType): Pair<Color, androidx.compose.ui.graphics.vector.ImageVector> {
    return when (policyType) {
        PricingPolicyType.TIME_BASED   -> Color(0xFF2196F3) to Icons.Default.Schedule
        PricingPolicyType.STOCK_BASED  -> Color(0xFFFFA726) to Icons.Default.Inventory
        PricingPolicyType.DEMAND_BASED -> Color(0xFF4CAF50) to Icons.Default.TrendingUp
        PricingPolicyType.MANUAL       -> Color(0xFF9E9E9E) to Icons.Default.Tune
    }
}

@Composable
fun PricingPolicyCard(policy: PricingPolicy) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA)
    val (policyColor, policyIcon) = policyVisual(policy.policyType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

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
                            .background(policyColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(policyIcon, contentDescription = null, tint = policyColor, modifier = Modifier.size(32.dp))
                    }

                    Column {
                        Text(text = policy.policyName, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Text(text = policy.productName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }

                // 활성/비활성 배지
                Box(
                    modifier = Modifier
                        .background(if (policy.isActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(if (policy.isActive) Icons.Default.CheckCircle else Icons.Default.Block, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(text = if (policy.isActive) "활성" else "비활성", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // 기본 가격
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "기본 가격", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = currencyFormat.format(policy.basePrice),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF6366F1)
                        )
                    }
                    // 정책 유형 배지
                    val typeLabel = when (policy.policyType) {
                        PricingPolicyType.TIME_BASED   -> "시간대별"
                        PricingPolicyType.STOCK_BASED  -> "재고 기반"
                        PricingPolicyType.DEMAND_BASED -> "수요 기반"
                        PricingPolicyType.MANUAL       -> "수동"
                    }
                    Box(
                        modifier = Modifier
                            .background(policyColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text(typeLabel, color = policyColor) }
                }
            }

            if (policy.rules.isNotEmpty()) {
                Divider(color = Color(0xFFE0E0E0))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Rule, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                        Text(text = "가격 규칙 (${policy.rules.size}개)", style = MaterialTheme.typography.titleSmall)
                    }
                    policy.rules.forEach { rule ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)), shape = RoundedCornerShape(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).background(policyColor, CircleShape))
                                    Text(text = rule.condition, style = MaterialTheme.typography.bodyMedium)
                                }
                                val isMinus = rule.adjustmentValue < 0
                                val badgeBg = if (isMinus) Color(0xFFF44336).copy(alpha = 0.15f) else Color(0xFF4CAF50).copy(alpha = 0.15f)
                                val badgeFg = if (isMinus) Color(0xFFF44336) else Color(0xFF4CAF50)
                                Box(
                                    modifier = Modifier
                                        .background(badgeBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    val label = when (rule.adjustmentType) {
                                        PriceAdjustmentType.PERCENTAGE   -> "${rule.adjustmentValue}%"
                                        PriceAdjustmentType.FIXED_AMOUNT -> NumberFormat.getNumberInstance().format(rule.adjustmentValue) + "원"
                                    }
                                    Text(text = label, color = badgeFg, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* -------------------- 프로모션 탭 -------------------- */

@Composable
fun PromotionsTab(promotions: List<Promotion>, viewModel: PricingViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(promotions) { promotion ->
            PromotionCard(promotion, viewModel)
        }
    }
}

@Composable
private fun promotionVisual(type: PromotionType): Pair<Color, androidx.compose.ui.graphics.vector.ImageVector> {
    return when (type) {
        PromotionType.SEASONAL         -> Color(0xFFFFA726) to Icons.Default.CalendarMonth
        PromotionType.BUY_ONE_GET_ONE  -> Color(0xFF4CAF50)  to Icons.Default.AddShoppingCart
        PromotionType.FLASH_SALE       -> Color(0xFFFF5722)  to Icons.Default.Bolt
        PromotionType.COUPON           -> Color(0xFF6366F1)  to Icons.Default.Redeem
        PromotionType.MEMBER_ONLY      -> Color(0xFF9C27B0)  to Icons.Default.People
    }
}

@Composable
fun PromotionCard(promotion: Promotion, viewModel: PricingViewModel) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val (promotionColor, promotionIcon) = promotionVisual(promotion.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

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
                            .background(promotionColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(promotionIcon, contentDescription = null, tint = promotionColor, modifier = Modifier.size(32.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = promotion.title, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                        promotion.description?.let {
                            Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }

                Switch(
                    checked = promotion.isActive,
                    onCheckedChange = { viewModel.togglePromotion(promotion.promotionId, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4CAF50),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF9E9E9E)
                    )
                )
            }

            Card(colors = CardDefaults.cardColors(containerColor = promotionColor.copy(alpha = 0.08f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 유형 배지
                        val typeLabel = when (promotion.type) {
                            PromotionType.SEASONAL        -> "시즌"
                            PromotionType.BUY_ONE_GET_ONE -> "1+1"
                            PromotionType.FLASH_SALE      -> "특가"
                            PromotionType.COUPON          -> "쿠폰"
                            PromotionType.MEMBER_ONLY     -> "회원전용"
                        }
                        Box(
                            modifier = Modifier
                                .background(promotionColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(typeLabel, color = Color.White, style = MaterialTheme.typography.labelSmall) }

                        // 할인/증정 값 배지
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF6366F1), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            val valueLabel = when (promotion.adjustmentType) {
                                PriceAdjustmentType.PERCENTAGE   -> "${promotion.value}% 할인"
                                PriceAdjustmentType.FIXED_AMOUNT -> "${promotion.value.toInt()}원 할인"
                            }
                            Text(valueLabel, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Divider(color = promotionColor.copy(alpha = 0.2f))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = promotionColor, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${dateFormat.format(Date(promotion.startTime))} ~ ${dateFormat.format(Date(promotion.endTime))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    promotion.stockLimit?.let { limit ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = promotionColor, modifier = Modifier.size(18.dp))
                                Text("사용 현황", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text("${promotion.usedCount} / $limit", style = MaterialTheme.typography.bodyMedium, color = promotionColor)
                        }

                        val usagePct = (promotion.usedCount.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE0E0E0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(usagePct)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(promotionColor, promotionColor.copy(alpha = 0.7f))
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

/* -------------------- 가격 이력 탭 -------------------- */

@Composable
fun PriceHistoryTab(history: List<PriceHistory>) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(history) { item ->
            PriceHistoryCard(item)
        }
    }
}

@Composable
fun PriceHistoryCard(history: PriceHistory) {
    val currency = NumberFormat.getCurrencyInstance(Locale.KOREA)
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    val isUp = history.newPrice > history.oldPrice
    val changeColor = if (isUp) Color(0xFFF44336) else Color(0xFF4CAF50)
    val changeIcon = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(6.dp).height(IntrinsicSize.Min).background(changeColor))

            Column(
                modifier = Modifier.weight(1f).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(changeColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(changeIcon, contentDescription = null, tint = changeColor, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(text = history.productName, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Text(text = history.changedBy ?: "-", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }

                    val changePct = if (history.oldPrice != 0.0)
                        ((history.newPrice - history.oldPrice) / history.oldPrice * 100.0)
                    else 0.0
                    Box(
                        modifier = Modifier
                            .background(changeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val pctLabel = (if (changePct >= 0) "+" else "") + String.format(Locale.getDefault(), "%.1f%%", changePct)
                        Text(pctLabel, color = changeColor, fontSize = 12.sp)
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(40.dp).background(Color(0xFF9E9E9E).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFF9E9E9E))
                            }
                            Text("변경 전", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                            Text(currency.format(history.oldPrice), style = MaterialTheme.typography.titleMedium)
                        }

                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(40.dp).background(changeColor.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(changeIcon, contentDescription = null, tint = changeColor)
                            }
                            Text("변경 후", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                            Text(currency.format(history.newPrice), style = MaterialTheme.typography.titleMedium, color = changeColor)
                        }
                    }
                }

                Divider(color = Color(0xFFE0E0E0))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                        Text("변경 사유", style = MaterialTheme.typography.labelMedium)
                    }
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F7FA), RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text(text = history.reason ?: "-", color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(dateFormat.format(Date(history.changedAt)), style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
