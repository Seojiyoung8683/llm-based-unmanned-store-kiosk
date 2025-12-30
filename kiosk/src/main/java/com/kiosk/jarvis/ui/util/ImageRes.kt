package com.kiosk.jarvis.ui.util

import android.content.Context
import androidx.annotation.DrawableRes
import com.kiosk.jarvis.R

object ImageRes {

    /**
     * 제품 ID → drawable 이미지 매핑
     * 1) 수동 매핑
     * 2) 파일명 추정(getIdentifier)
     * 3) 기본 플레이스홀더
     */
    @DrawableRes
    fun forProductId(context: Context, productId: String): Int {
        // 1) 제품 ID와 실제 이미지 파일 이름 매핑
        val manual: Map<String, Int> = mapOf(
            "P001" to R.drawable.homerunball,       // 초코우유
            "P002" to R.drawable.shrimp,    // 초코파이
            "P003" to R.drawable.turtle,      // 꼬북칩
            "P004" to R.drawable.pepero,       // 사이다
            "P005" to R.drawable.chocopie,        // 콜라
            "P006" to R.drawable.gorae,       // 고래밥
            "P007" to R.drawable.coke, // 홈런볼
            "P008" to R.drawable.cider,      // 오렌지주스
            "P009" to R.drawable.orange,      // 빼빼로
            "P010" to R.drawable.choco       // 새우깡
        )

        manual[productId]?.let { return it }

        // 2) productId가 파일명과 같을 경우 자동 탐색
        val candidates = listOf(
            productId,
            productId.lowercase(),
            productId.lowercase().replace(Regex("[^a-z0-9_]+"), "_")
        )

        for (name in candidates) {
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id != 0) return id
        }

        // 3) 기본 아이콘 (res/drawable/placeholder.xml 사용)
        return R.drawable.placeholder
    }
}
