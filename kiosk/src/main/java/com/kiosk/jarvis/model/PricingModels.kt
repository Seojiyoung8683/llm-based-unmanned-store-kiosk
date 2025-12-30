package com.kiosk.jarvis.model

/* ───────────── 공통 ───────────── */
enum class PriceAdjustmentType { PERCENTAGE, FIXED_AMOUNT }

/* ───────────── 가격 정책 ───────────── */
enum class PricingPolicyType { TIME_BASED, STOCK_BASED, DEMAND_BASED, MANUAL }

data class PricingRule(
    val ruleId: String,
    /** 조건 표현 (예: "06:00-09:00", "재고 < 10") */
    val condition: String,
    val adjustmentType: PriceAdjustmentType,
    /** 조정 값 (PERCENTAGE면 %, FIXED_AMOUNT면 금액) */
    val adjustmentValue: Double
)

data class PricingPolicy(
    val policyId: String,
    val policyName: String,
    val productId: String,
    val productName: String,
    val basePrice: Double,
    val policyType: PricingPolicyType,
    val rules: List<PricingRule> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/* ───────────── 프로모션 ───────────── */
enum class PromotionType { SEASONAL, BUY_ONE_GET_ONE, FLASH_SALE, COUPON, MEMBER_ONLY }

data class Promotion(
    val promotionId: String,
    /** UI 타이틀 */
    val title: String,
    /** 상세 설명 */
    val description: String? = null,
    /** 프로모션 종류 */
    val type: PromotionType,
    /** 할인/증정 방식 */
    val adjustmentType: PriceAdjustmentType,
    /** 할인 값 (PERCENTAGE면 %, FIXED_AMOUNT면 금액) */
    val value: Double,
    /** 대상 상품 ID들 */
    val targetProductIds: List<String> = emptyList(),
    /** 시작/종료 시간(epoch millis) */
    val startTime: Long,
    val endTime: Long,
    /** 현재 활성 여부 */
    val isActive: Boolean = true,
    /** 재고 한도(있으면 수량 제한) */
    val stockLimit: Int? = null,
    /** 사용/적용된 횟수 등 */
    val usedCount: Int = 0
)

/* ───────────── 가격 변경 이력 ───────────── */
data class PriceHistory(
    val historyId: String,
    val productId: String,
    val productName: String,
    val oldPrice: Double,
    val newPrice: Double,
    val reason: String? = null,
    val changedBy: String? = null,
    val changedAt: Long = System.currentTimeMillis()
)
