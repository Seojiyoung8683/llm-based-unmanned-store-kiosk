package com.kiosk.jarvis.model.pricing

// 가격 정책 종류
enum class PricingPolicyType { TIME_BASED, STOCK_BASED, CUSTOMER_TIER, LOCATION_BASED }

// 가격 조정 방식
enum class PriceAdjustmentType { PERCENTAGE, FIXED_AMOUNT }

// 단일 규칙 (예: "06:00-09:00", "재고 < 10")
data class PricingRule(
    val ruleId: String,
    val condition: String,
    val adjustmentType: PriceAdjustmentType,
    val value: Double
)

// 가격 정책
data class PricingPolicy(
    val policyId: String,
    val policyName: String,
    val productId: String,
    val productName: String,
    val basePrice: Double,
    val policyType: PricingPolicyType,
    val rules: List<PricingRule>,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

enum class PromotionType { SEASONAL, BUY_ONE_GET_ONE, FLASH_SALE, COUPON }

data class Promotion(
    val promotionId: String,
    val title: String,
    val description: String,
    val type: PromotionType,
    val adjustmentType: PriceAdjustmentType,
    val value: Double,
    val targetProductIds: List<String>,
    val startTime: Long,
    val endTime: Long,
    val isActive: Boolean,
    val stockLimit: Int? = null,
    val usedCount: Int = 0
)

// 가격 변경 이력
data class PriceHistory(
    val historyId: String,
    val productId: String,
    val productName: String,
    val oldPrice: Double,
    val newPrice: Double,
    val reason: String,
    val changedBy: String, // 변경자
    val timestamp: Long
)
