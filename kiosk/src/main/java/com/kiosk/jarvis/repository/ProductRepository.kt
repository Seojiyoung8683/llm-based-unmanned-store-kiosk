// ProductRepository.kt
package com.kiosk.jarvis.repository

import android.content.Context
import com.kiosk.jarvis.R
import com.kiosk.jarvis.data.local.JarvisDatabase
import com.kiosk.jarvis.data.local.ProductEntity
import com.kiosk.jarvis.model.Product
import com.kiosk.jarvis.model.ProductCategory
import com.kiosk.jarvis.model.ProductData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.abs

object ProductRepository {

    private fun dao(context: Context) = JarvisDatabase.get(context).productDao()

    suspend fun ensureSeeded(context: Context) = withContext(Dispatchers.IO) {
        val d = dao(context)
        if (d.count() == 0) {
            val seeds = ProductData.products.map { p ->
                ProductEntity(
                    id        = p.id,
                    name      = p.nameKo,
                    price     = p.price,
                    category  = when (p.category) {
                        ProductCategory.DRINK -> "DRINK"
                        ProductCategory.SNACK -> "SNACK"
                    },
                    barcode   = p.barcode,
                    imageUrl  = null,
                    stock     = 20,
                    updatedAt = System.currentTimeMillis(),
                    deleted   = false
                )
            }
            d.upsertAll(seeds)
        }
    }

    fun observeAll(context: Context): Flow<List<Product>> =
        dao(context).observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getById(context: Context, id: String): Product? =
        withContext(Dispatchers.IO) { dao(context).getById(id)?.toModel() }

    suspend fun upsert(context: Context, product: Product): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val d = dao(context)
                val existing = d.getById(product.id)
                val entity = product.toEntity(existing?.stock ?: 0)
                d.upsert(entity)
            }
        }

    suspend fun delete(context: Context, productId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { dao(context).hardDelete(productId) }
        }

    // ───────────── 재고 관리용 모델/함수 추가 ─────────────

    data class ProductInventory(
        val product: Product,
        val stock: Int
    )

    fun observeInventory(context: Context): Flow<List<ProductInventory>> =
        dao(context).observeAll().map { list ->
            list.map { entity ->
                ProductInventory(
                    product = entity.toModel(),
                    stock   = entity.stock
                )
            }
        }

    // ───────────────────────── Mapper ─────────────────────────
    private fun ProductEntity.toModel(): Product =
        Product(
            id          = id,
            nameKo      = name,
            nameEn      = name,
            category    = when (category.uppercase()) {
                "DRINK", "BEVERAGE" -> ProductCategory.DRINK
                else                 -> ProductCategory.SNACK
            },
            price       = price,
            location    = "A",
            imageRes    = imageResFromName(name),
            description = "",
            barcode     = barcode
        )

    private fun Product.toEntity(currentStock: Int = 0): ProductEntity =
        ProductEntity(
            id        = id,
            name      = nameKo,
            price     = price,
            category  = when (category) {
                ProductCategory.DRINK -> "DRINK"
                ProductCategory.SNACK -> "SNACK"
            },
            barcode   = barcode,
            imageUrl  = null,
            stock     = currentStock,
            updatedAt = System.currentTimeMillis(),
            deleted   = false
        )

    private fun imageResFromName(name: String): Int {
        val n = name.lowercase()
        return when {
            // 스낵류
            "홈런볼" in n || "homerun" in n     -> R.drawable.homerunball
            "새우깡" in n || "shrimp" in n      -> R.drawable.shrimp
            "꼬북칩" in n || "turtle" in n      -> R.drawable.turtle
            "빼빼로" in n || "pepero" in n      -> R.drawable.pepero
            "초코파이" in n || "choco pie" in n -> R.drawable.chocopie
            "고래밥" in n || "whale" in n       -> R.drawable.gorae

            // 음료류
            "콜라" in n || "cola" in n || "coke" in n   -> R.drawable.coke
            "사이다" in n || "sprite" in n              -> R.drawable.cider
            "오렌지" in n || "orange" in n              -> R.drawable.orange
            "초코우유" in n || "chocolate milk" in n    -> R.drawable.choco

            else -> 0
        }
    }

    suspend fun increaseStock(context: Context, productId: String, delta: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                dao(context).increaseStock(
                    id = productId,
                    delta = delta,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }

    suspend fun decreaseStock(context: Context, productId: String, quantity: Int): Result<Unit> =
        increaseStock(context, productId, -abs(quantity))
}
