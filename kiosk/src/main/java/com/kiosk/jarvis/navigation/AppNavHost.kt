// C:/kioskfinal/kiosk/src/main/java/com/kiosk/jarvis/navigation/AppNavHost.kt
package com.kiosk.jarvis.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kiosk.jarvis.service.ConversationService
import com.kiosk.jarvis.ui.screens.MainScreen
import com.kiosk.jarvis.ui.screens.Store3DScreen
import com.kiosk.jarvis.ui.screens.VoiceInputScreen
import com.kiosk.jarvis.ui.screens.admin.DashboardScreen
import com.kiosk.jarvis.ui.screens.ProductDetailScreen
import com.kiosk.jarvis.viewmodel.CartViewModel

object Routes {
    const val Main = "main"
    const val Dashboard = "dashboard"
    const val Voice = "voice_input"

    const val Store3D = "store3d"

    const val ProductDetail = "product_detail"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    var language by rememberSaveable { mutableStateOf("한국어") }

    NavHost(
        navController = navController,
        startDestination = Routes.Main
    ) {
        composable(Routes.Main) {
            MainScreen(
                language = language,
                onLanguageChange = { newLang ->
                    language = newLang
                },
                onNavigateToPayment = {
                },
                onNavigateToProducts = {
                },
                onNavigateToVoiceInput = {
                    navController.navigate(Routes.Voice)
                },
                onNavigateToWeather = {
                },
                onNavigateToDashboard = {
                    navController.navigate(Routes.Dashboard)
                }
            )
        }

        // 음성 입력 화면
        composable(Routes.Voice) {
            val context = LocalContext.current

            val conversationService = remember {
                ConversationService(context)
            }

            val voiceLang = if (language == "한국어") "ko" else "en"

            VoiceInputScreen(
                conversationService = conversationService,
                language = voiceLang,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onVoiceResult = { resultText ->
                    Log.d("AppNavHost", "Voice result: $resultText")
                },
                onOpenStore3D = { command ->
                    Log.d("AppNavHost", "Navigate to Store3D with command=$command")
                    navController.navigate("${Routes.Store3D}/$command")
                },
                onOpenProductDetail = { productId ->
                    Log.d("AppNavHost", "onOpenProductDetail called with id=$productId")
                    navController.navigate("${Routes.ProductDetail}/$productId")
                }
            )
        }

        composable(Routes.Dashboard) {
            DashboardScreen(
                navController = navController,
                onNavigateToKiosk = {
                    val popped = navController.popBackStack(Routes.Main, inclusive = false)
                    if (!popped) {
                        navController.navigate(Routes.Main) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = "${Routes.Store3D}/{command}",
            arguments = listOf(
                navArgument("command") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val command = backStackEntry.arguments?.getString("command")
            Store3DScreen(
                initialCommand = command,
                onClose = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "${Routes.ProductDetail}/{productId}",
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable

            val cartViewModel: CartViewModel = viewModel()

            val detailLang = if (language == "한국어") "ko" else "en"

            ProductDetailScreen(
                productId = productId,
                language = detailLang,
                onBackClick = {
                    navController.popBackStack()
                },
                onRecommendedProductClick = { recommendedId ->
                    navController.navigate("${Routes.ProductDetail}/$recommendedId")
                },
                cartViewModel = cartViewModel
            )
        }
    }
}
