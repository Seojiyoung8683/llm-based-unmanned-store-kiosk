// C:/kioskfinal/kiosk/src/main/java/com/kiosk/jarvis/KioskApp.kt
package com.kiosk.jarvis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.kiosk.jarvis.model.OrderItem
import com.kiosk.jarvis.navigation.AppNavigation
import com.kiosk.jarvis.service.ConversationService
import com.kiosk.jarvis.ui.screens.*
import com.kiosk.jarvis.viewmodel.CartViewModel
import com.kiosk.jarvis.viewmodel.OrderViewModel
import com.kiosk.jarvis.viewmodel.OrderViewModelFactory

@Composable
fun KioskApp(
    conversationService: ConversationService
) {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()
    var currentLanguage by remember { mutableStateOf("ko") }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "dashboard") {
                CartBottomBar(
                    cartViewModel = cartViewModel,
                    language = currentLanguage,
                    onCheckout = { navController.navigate("payment") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = "main"
            ) {
                // 메인 화면
                composable("main") {
                    MainScreen(
                        language = currentLanguage,
                        onLanguageChange = { currentLanguage = it },
                        onNavigateToProducts = { navController.navigate("products") },
                        onNavigateToWeather = { navController.navigate("weather") },
                        onNavigateToPayment = { navController.navigate("payment") },
                        onNavigateToVoiceInput = { navController.navigate("voice_input") },
                        onNavigateToDashboard = {
                            navController.navigate("dashboard") {
                                launchSingleTop = true
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    )
                }

                // 관리자 대시보드
                composable("dashboard") {
                    AppNavigation(
                        onNavigateToKiosk = {
                            navController.navigate("main") {
                                popUpTo("dashboard") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // 음성 입력 화면
                composable("voice_input") {
                    VoiceInputScreen(
                        conversationService = conversationService,
                        language = currentLanguage,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onVoiceResult = { /* 필요하면 나중에 처리 */ },
                        onOpenStore3D = { command ->
                            navController.navigate("store3d/$command")
                        },
                        onOpenProductDetail = { productId ->
                            navController.navigate("product_detail/$productId")
                        }
                    )
                }


                //  3D 매장 화면
                composable(
                    route = "store3d/{command}",
                    arguments = listOf(
                        navArgument("command") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val command = backStackEntry.arguments?.getString("command")
                    Store3DScreen(
                        initialCommand = command,
                        onClose = {
                            // 5초 후 문 닫기까지 끝나면 이전 화면으로 돌아감
                            navController.popBackStack()
                        }
                    )
                }

                // 주문 내역
                composable("order_history") {
                    val contextLocal = LocalContext.current
                    val orderVM: OrderViewModel = viewModel(
                        factory = OrderViewModelFactory(contextLocal.applicationContext as android.app.Application)
                    )

                    OrderHistoryScreen(
                        orderViewModel = orderVM,
                        onBack = { navController.popBackStack() },
                        onOpenOrderDetail = { id -> navController.navigate("order_detail/$id") }
                    )
                }

                // 주문 상세
                composable(
                    route = "order_detail/{orderId}",
                    arguments = listOf(navArgument("orderId") { type = NavType.LongType })
                ) { entry ->
                    val orderId = entry.arguments?.getLong("orderId") ?: 0L

                    val contextLocal = LocalContext.current
                    val orderVM: OrderViewModel = viewModel(
                        factory = OrderViewModelFactory(contextLocal.applicationContext as android.app.Application)
                    )

                    OrderDetailScreen(
                        orderId = orderId,
                        orderViewModel = orderVM,
                        onBack = { navController.popBackStack() },
                        onReorder = { /* TODO */ }
                    )
                }

                // 상품 목록
                composable("products") {
                    ProductListScreen(
                        language = currentLanguage,
                        onProductClick = { productId ->
                            navController.navigate("product_detail/$productId")
                        },
                        onBackClick = { navController.popBackStack() },
                        cartViewModel = cartViewModel
                    )
                }

                // 상품 상세
                composable(
                    route = "product_detail/{productId}",
                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId") ?: ""

                    ProductDetailScreen(
                        productId = productId,
                        language = currentLanguage,
                        onBackClick = { navController.popBackStack() },
                        onRecommendedProductClick = { id ->
                            navController.navigate("product_detail/$id")
                        },
                        cartViewModel = cartViewModel
                    )
                }

                // 결제 결과
                composable("payment_result") {
                    PaymentResultScreen(
                        onGoHome = {
                            navController.navigate("main") {
                                popUpTo("main") { inclusive = true }
                            }
                        },
                        onGoOrderHistory = {
                            navController.navigate("order_history")
                        }
                    )
                }

                // 날씨 추천
                composable("weather") {
                    WeatherRecommendationScreen(
                        language = currentLanguage,
                        onBackClick = { navController.popBackStack() },
                        onProductClick = { productId ->
                            navController.navigate("product_detail/$productId")
                        }
                    )
                }

                // 결제
                composable("payment") {
                    val contextLocal = LocalContext.current
                    val orderViewModel: OrderViewModel = viewModel(
                        factory = OrderViewModelFactory(contextLocal.applicationContext as android.app.Application)
                    )

                    PaymentScreen(
                        language = currentLanguage,
                        cartViewModel = cartViewModel,
                        onBackClick = { navController.popBackStack() },
                        onPaymentComplete = {
                            val snapshot: List<OrderItem> = cartViewModel.cartItems.map { ci ->
                                val p = ci.product
                                OrderItem(
                                    productId = p.id,
                                    name = p.nameKo,
                                    unitPrice = p.price,
                                    quantity = ci.quantity,
                                    imageResId = p.imageRes,
                                )
                            }

                            orderViewModel.placeOrder(
                                items = snapshot,
                                paymentMethod = "카드",
                                status = "결제 완료"
                            )

                            cartViewModel.clearCart()

                            navController.navigate("payment_result") {
                                popUpTo("payment") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
