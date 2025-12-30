// C:/kioskfinal/kiosk/src/main/java/com/kiosk/jarvis/MainActivity.kt
package com.kiosk.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.kiosk.jarvis.data.InventoryManager
import com.kiosk.jarvis.service.ConversationService
import com.kiosk.jarvis.ui.theme.JarvisKioskTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var conversationService: ConversationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InventoryManager.init(applicationContext)

        conversationService = ConversationService(applicationContext)

        lifecycleScope.launch {
            conversationService.initializeApp()
        }

        setContent {
            JarvisKioskTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KioskApp(conversationService = conversationService)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::conversationService.isInitialized) {
            conversationService.cleanup()
        }
    }
}
