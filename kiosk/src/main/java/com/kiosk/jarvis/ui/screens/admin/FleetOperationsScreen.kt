// app/src/main/java/com/kiosk/jarvis/ui/screens/admin/FleetOperationsScreen.kt
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kiosk.jarvis.model.*
import com.kiosk.jarvis.navigation.AppNavigationDrawer
import com.kiosk.jarvis.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetOperationsScreen(
    navController: NavController,
    viewModel: FleetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    // 메시지 스낵바
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
                    title = {
                        Text(
                            "매장 운영",
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
                        IconButton(onClick = { viewModel.loadFleetData() }) {
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
                                    "매장 환경",
                                    fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
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
                                    "운영 스케줄",
                                    fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
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
                                    "알림",
                                    fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
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
                            0 -> StoreEnvironmentTab(
                                navController = navController,
                                viewModel = viewModel
                            )
                            1 -> ScheduleTab(uiState.schedules)
                            2 -> AlertsTab(uiState.alerts, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreEnvironmentTab(
    navController: NavController,
    viewModel: FleetViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "매장 환경 제어",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 조명
            item {
                EnvironmentControlCard(
                    title = "조명",
                    description = "매장 전체 조명 전원 제어",
                    icon = Icons.Default.Lightbulb,
                    primaryLabel = "켜기",
                    secondaryLabel = "끄기",
                    onPrimary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_LIGHTING",
                                action = ControlAction.TURN_ON
                            )
                        )
                        // ✅ 3D 매장 조명 켜기
                        navController.navigate("${Screen.Store3D.route}/lightsOn")
                    },
                    onSecondary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_LIGHTING",
                                action = ControlAction.TURN_OFF
                            )
                        )
                        // ✅ 3D 매장 조명 끄기
                        navController.navigate("${Screen.Store3D.route}/lightsOff")
                    }
                )
            }

            // 문
            item {
                EnvironmentControlCard(
                    title = "출입문",
                    description = "매장 출입문 개폐 제어",
                    icon = Icons.Default.MeetingRoom,
                    primaryLabel = "열기",
                    secondaryLabel = "닫기",
                    onPrimary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_DOOR",
                                action = ControlAction.OPEN
                            )
                        )
                        // ✅ 3D 매장 문 열기
                        navController.navigate("${Screen.Store3D.route}/openDoor")
                    },
                    onSecondary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_DOOR",
                                action = ControlAction.CLOSE
                            )
                        )
                        // ✅ 3D 매장 문 닫기
                        navController.navigate("${Screen.Store3D.route}/closeDoor")
                    }
                )
            }

            // 에어컨
            item {
                EnvironmentControlCard(
                    title = "에어컨",
                    description = "에어컨 전원 제어",
                    icon = Icons.Default.AcUnit,
                    primaryLabel = "켜기",
                    secondaryLabel = "끄기",
                    onPrimary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_AC",
                                action = ControlAction.TURN_ON
                            )
                        )
                        // ✅ 3D 매장 에어컨 ON
                        navController.navigate("${Screen.Store3D.route}/acOn")
                    },
                    onSecondary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_AC",
                                action = ControlAction.TURN_OFF
                            )
                        )
                        // ✅ 3D 매장 에어컨 OFF
                        navController.navigate("${Screen.Store3D.route}/acOff")
                    }
                )
            }

            // 블라인드 (3D 연동은 나중에 필요하면 추가)
            item {
                EnvironmentControlCard(
                    title = "블라인드",
                    description = "블라인드 올림 / 내림 제어",
                    icon = Icons.Default.Window,
                    primaryLabel = "열기",
                    secondaryLabel = "닫기",
                    onPrimary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_BLIND",
                                action = ControlAction.OPEN
                            )
                        )
                    },
                    onSecondary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_BLIND",
                                action = ControlAction.CLOSE
                            )
                        )
                    }
                )
            }

            // 스피커
            item {
                EnvironmentControlCard(
                    title = "스피커",
                    description = "배경 음악 전원 제어",
                    icon = Icons.Default.VolumeUp,
                    primaryLabel = "켜기",
                    secondaryLabel = "끄기",
                    onPrimary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_SPEAKER",
                                action = ControlAction.TURN_ON
                            )
                        )
                        // ✅ 3D 매장 음악 ON
                        navController.navigate("${Screen.Store3D.route}/musicOn")
                    },
                    onSecondary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_SPEAKER",
                                action = ControlAction.TURN_OFF
                            )
                        )
                        // ✅ 3D 매장 음악 OFF
                        navController.navigate("${Screen.Store3D.route}/musicOff")
                    }
                )
            }

            // 가습기 (3D 연동은 나중에)
            item {
                EnvironmentControlCard(
                    title = "가습기",
                    description = "가습기 전원 제어",
                    icon = Icons.Default.WaterDrop,
                    primaryLabel = "켜기",
                    secondaryLabel = "끄기",
                    onPrimary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_HUMIDIFIER",
                                action = ControlAction.TURN_ON
                            )
                        )
                    },
                    onSecondary = {
                        viewModel.executeControl(
                            RemoteControlAction(
                                deviceId = "ENV_HUMIDIFIER",
                                action = ControlAction.TURN_OFF
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun EnvironmentControlCard(
    title: String,
    description: String,
    icon: ImageVector,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
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
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = primaryLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = secondaryLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceStatusTab(devices: List<Device>, viewModel: FleetViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(devices) { device ->
            DeviceCard(device, viewModel)
        }
    }
}

@Composable
fun DeviceCard(device: Device, viewModel: FleetViewModel) {
    var showControlDialog by remember { mutableStateOf(false) }

    val (statusColor, statusText, statusBgColor) = when (device.status) {
        DeviceStatus.ONLINE -> Triple(Color(0xFF4CAF50), "정상", Color(0xFF4CAF50).copy(alpha = 0.1f))
        DeviceStatus.OFFLINE -> Triple(Color(0xFF9E9E9E), "오프라인", Color(0xFF9E9E9E).copy(alpha = 0.1f))
        DeviceStatus.WARNING -> Triple(Color(0xFFFFA726), "경고", Color(0xFFFFA726).copy(alpha = 0.1f))
        DeviceStatus.ERROR -> Triple(Color(0xFFF44336), "오류", Color(0xFFF44336).copy(alpha = 0.1f))
    }

    val deviceIcon = when (device.deviceType) {
        DeviceType.KIOSK -> Icons.Default.Computer
        DeviceType.LIGHTING -> Icons.Default.Lightbulb
        DeviceType.BLIND -> Icons.Default.Window
        DeviceType.DOOR -> Icons.Default.MeetingRoom
        DeviceType.CAMERA -> Icons.Default.Videocam
        DeviceType.SCANNER -> Icons.Default.QrCodeScanner
        DeviceType.PAYMENT_TERMINAL -> Icons.Default.Payment
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                            imageVector = deviceIcon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF6366F1)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = device.deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = device.storeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(statusBgColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
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

            if (device.temperature != null || device.powerUsage != null || device.networkLatency != null) {
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
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        device.temperature?.let {
                            DeviceMetric("온도", "${it}°C", Icons.Default.Thermostat, Color(0xFFFF6B6B))
                        }
                        device.powerUsage?.let {
                            DeviceMetric("전력", "${it}W", Icons.Default.BatteryChargingFull, Color(0xFF4CAF50))
                        }
                        device.networkLatency?.let {
                            DeviceMetric("지연", "${it}ms", Icons.Default.NetworkCheck, Color(0xFF2196F3))
                        }
                    }
                }
            }

            if (device.status != DeviceStatus.OFFLINE) {
                Button(
                    onClick = { showControlDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "원격 제어",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showControlDialog) {
        DeviceControlDialog(
            device = device,
            onDismiss = { showControlDialog = false },
            onControl = { action ->
                viewModel.executeControl(action)
                showControlDialog = false
            }
        )
    }
}

@Composable
fun DeviceMetric(label: String, value: String, icon: ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun DeviceControlDialog(
    device: Device,
    onDismiss: () -> Unit,
    onControl: (RemoteControlAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "${device.deviceName} 제어",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (device.deviceType) {
                    DeviceType.LIGHTING -> {
                        ControlButton(
                            text = "켜기",
                            icon = Icons.Default.LightMode,
                            color = Color(0xFFFFA726),
                            onClick = { onControl(RemoteControlAction(device.deviceId, ControlAction.TURN_ON)) }
                        )
                        ControlButton(
                            text = "끄기",
                            icon = Icons.Default.DarkMode,
                            color = Color(0xFF9E9E9E),
                            onClick = { onControl(RemoteControlAction(device.deviceId, ControlAction.TURN_OFF)) }
                        )
                    }
                    DeviceType.BLIND, DeviceType.DOOR -> {
                        ControlButton(
                            text = "열기",
                            icon = Icons.Default.OpenInFull,
                            color = Color(0xFF4CAF50),
                            onClick = { onControl(RemoteControlAction(device.deviceId, ControlAction.OPEN)) }
                        )
                        ControlButton(
                            text = "닫기",
                            icon = Icons.Default.CloseFullscreen,
                            color = Color(0xFFF44336),
                            onClick = { onControl(RemoteControlAction(device.deviceId, ControlAction.CLOSE)) }
                        )
                    }
                    else -> {
                        ControlButton(
                            text = "재시작",
                            icon = Icons.Default.RestartAlt,
                            color = Color(0xFF6366F1),
                            onClick = { onControl(RemoteControlAction(device.deviceId, ControlAction.RESTART)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF6366F1), fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ControlButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ScheduleTab(schedules: List<StoreSchedule>) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(schedules) { schedule ->
            ScheduleCard(schedule)
        }
    }
}

@Composable
fun ScheduleCard(schedule: StoreSchedule) {
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
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = schedule.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
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
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "개점 시간",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = schedule.openTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(80.dp)
                            .background(Color(0xFFE0E0E0))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "마감 시간",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = schedule.closeTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Divider(color = Color(0xFFE0E0E0))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "자동화 설정",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )

                ScheduleOption("야간 무인 모드", schedule.nightMode, Icons.Default.DarkMode)
                ScheduleOption("전력 세이브", schedule.powerSaveMode, Icons.Default.PowerSettingsNew)
                ScheduleOption("자동 셧다운", schedule.autoShutdown, Icons.Default.PowerOff)
            }
        }
    }
}

@Composable
fun ScheduleOption(label: String, enabled: Boolean, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (enabled) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (enabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AlertsTab(alerts: List<Alert>, viewModel: FleetViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = Color(0xFFE0E0E0),
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "알림이 없습니다",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "모든 시스템이 정상 작동 중입니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                AlertCard(alert, viewModel)
            }
        }
    }
}

@Composable
fun AlertCard(alert: Alert, viewModel: FleetViewModel) {
    val (severityColor, severityText, severityIcon) = when (alert.severity) {
        AlertSeverity.INFO -> Triple(Color(0xFF2196F3), "정보", Icons.Default.Info)
        AlertSeverity.WARNING -> Triple(Color(0xFFFFA726), "경고", Icons.Default.Warning)
        AlertSeverity.CRITICAL -> Triple(Color(0xFFF44336), "긴급", Icons.Default.Error)
    }

    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 왼쪽 색상 바
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(IntrinsicSize.Min)
                    .background(severityColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(severityColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = severityIcon,
                                contentDescription = null,
                                tint = severityColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Box(
                                modifier = Modifier
                                    .background(severityColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = severityText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = alert.storeName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = dateFormat.format(Date(alert.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                alert.deviceName?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                if (!alert.isResolved) {
                    Button(
                        onClick = { viewModel.resolveAlert(alert.alertId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = severityColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "해결 처리",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
