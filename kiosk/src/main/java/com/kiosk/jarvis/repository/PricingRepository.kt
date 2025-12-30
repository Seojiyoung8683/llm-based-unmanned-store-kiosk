package com.kiosk.jarvis.repository

import com.kiosk.jarvis.model.PriceAdjustmentType
import com.kiosk.jarvis.model.PriceHistory
import com.kiosk.jarvis.model.PricingPolicy
import com.kiosk.jarvis.model.PricingPolicyType
import com.kiosk.jarvis.model.PricingRule
import com.kiosk.jarvis.model.Promotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.kiosk.jarvis.model.PromotionType

class PricingRepository {

    fun getAllPricingPolicies(): Flow<List<PricingPolicy>> = flow {
        delay(500)
        val now = System.currentTimeMillis()
        emit(
            listOf(
                PricingPolicy(
                    policyId = "POL001",
                    policyName = "홈런볼 시간대별 가격",
                    productId = "P001",
                    productName = "홈런볼",
                    basePrice = 1500.0,
                    policyType = PricingPolicyType.TIME_BASED,
                    rules = listOf(
                        PricingRule("R001", "06:00-09:00", PriceAdjustmentType.PERCENTAGE, -10.0),
                        PricingRule("R002", "14:00-16:00", PriceAdjustmentType.FIXED_AMOUNT, -500.0)
                    ),
                    isActive = true,
                    createdAt = now - 2_592_000_000, // 30일 전
                    updatedAt = now
                ),
                PricingPolicy(
                    policyId = "POL002",
                    policyName = "새우깡 재고 기반 가격",
                    productId = "P003",
                    productName = "새우깡",
                    basePrice = 1200.0,
                    policyType = PricingPolicyType.STOCK_BASED,
                    rules = listOf(
                        PricingRule("R003", "재고 < 10", PriceAdjustmentType.PERCENTAGE, 10.0),
                        PricingRule("R004", "재고 > 30", PriceAdjustmentType.PERCENTAGE, -15.0)
                    ),
                    isActive = true,
                    createdAt = now - 1_296_000_000, // 15일 전
                    updatedAt = now
                )
            )
        )
    }

    fun getAllPromotions(): Flow<List<Promotion>> = flow {
        delay(500)
        val now = System.currentTimeMillis()
        emit(
            listOf(
                Promotion(
                    promotionId = "PROMO001",
                    title = "신년 특별 할인",
                    description = "모든 음료 20% 할인",
                    type = PromotionType.SEASONAL,
                    adjustmentType = PriceAdjustmentType.PERCENTAGE,
                    value = 20.0,
                    targetProductIds = listOf("P001", "P002", "P004"),
                    startTime = now - 86_400_000,       // 1일 전
                    endTime = now + 604_800_000,        // 7일 후
                    isActive = true,
                    stockLimit = null,
                    usedCount = 156
                ),
                Promotion(
                    promotionId = "PROMO002",
                    title = "새우깡 1+1",
                    description = "새우깡 구매 시 1개 추가 증정",
                    type = PromotionType.BUY_ONE_GET_ONE,
                    adjustmentType = PriceAdjustmentType.PERCENTAGE,
                    value = 50.0, // 1+1 정가 절반 효과
                    targetProductIds = listOf("P003"),
                    startTime = now - 172_800_000,      // 2일 전
                    endTime = now + 432_000_000,        // 5일 후
                    isActive = true,
                    stockLimit = 100,
                    usedCount = 45
                ),
                Promotion(
                    promotionId = "PROMO003",
                    title = "점심 특가",
                    description = "12시-14시 식품 15% 할인",
                    type = PromotionType.FLASH_SALE,
                    adjustmentType = PriceAdjustmentType.PERCENTAGE,
                    value = 15.0,
                    targetProductIds = listOf("P003", "P006"),
                    startTime = now - 259_200_000,      // 3일 전
                    endTime = now + 259_200_000,        // 3일 후
                    isActive = false,
                    stockLimit = null,
                    usedCount = 0
                )
            )
        )
    }

    fun getPriceHistory(productId: String? = null): Flow<List<PriceHistory>> = flow {
        delay(500)
        val now = System.currentTimeMillis()
        val history = listOf(
            PriceHistory("H001", "P001", "홈런볼", 1500.0, 1500.0, "원가 상승", "관리자", now - 2_592_000_000),
            PriceHistory("H002", "P002", "새우깡", 1200.0, 1200.0, "원가 상승", "관리자", now - 2_592_000_000),
            PriceHistory("H003", "P003", "꼬북칩", 1800.0, 1800.0, "재료비 인상", "관리자", now - 1_296_000_000)
        )
        emit(productId?.let { pid -> history.filter { it.productId == pid } } ?: history)
    }

    suspend fun savePricingPolicy(policy: PricingPolicy): Result<String> {
        delay(500)
        return Result.success("가격 정책이 저장되었습니다.")
    }

    suspend fun savePromotion(promotion: Promotion): Result<String> {
        delay(500)
        return Result.success("프로모션이 저장되었습니다.")
    }

    suspend fun togglePromotion(promotionId: String, isActive: Boolean): Result<String> {
        delay(300)
        return Result.success(if (isActive) "프로모션이 활성화되었습니다." else "프로모션이 비활성화되었습니다.")
    }
}
