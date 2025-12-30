// app/src/main/java/com/kiosk/jarvis/AdminActivity.kt
package com.kiosk.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.kiosk.jarvis.navigation.AppNavigation

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    // Admin 전용 네비게이션 (대시보드가 startDestination)
                    AppNavigation(
                        onNavigateToKiosk = {
                            // 관리자 전용 Activity에서 홈 아이콘 누르면 이 액티비티 종료
                            finish()
                        }
                    )                }
            }
        }
    }
}
