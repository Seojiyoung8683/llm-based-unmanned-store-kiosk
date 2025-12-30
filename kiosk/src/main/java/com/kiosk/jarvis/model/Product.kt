package com.kiosk.jarvis.model

import androidx.room.Entity
import com.kiosk.jarvis.R

@Entity(tableName = "products")
data class Product(
    val id: String,
    val nameKo: String,
    val nameEn: String,
    val category: ProductCategory,
    val price: Int,
    val location: String,
    val imageRes: Int,
    val description: String = "",
    val barcode: String? = null
)

enum class ProductCategory { SNACK, DRINK }

data class CartItem(
    val product: Product,
    val quantity: Int
)

object ProductData {
    val products = listOf(
        Product("P001", "홈런볼", "Homerun Ball", ProductCategory.SNACK, 1500, "A", R.drawable.homerunball, barcode = "880000000001"),
        Product("P002", "새우깡", "Shrimp Cracker", ProductCategory.SNACK, 1200, "A", R.drawable.shrimp,       barcode = "880000000002"),
        Product("P003", "꼬북칩", "Turtle Chip",   ProductCategory.SNACK, 1800, "B", R.drawable.turtle,       barcode = "880000000003"),
        Product("P004", "빼빼로", "Pepero",        ProductCategory.SNACK, 1300, "B", R.drawable.pepero,       barcode = "880000000004"),
        Product("P005", "초코파이","Choco Pie",    ProductCategory.SNACK, 2000, "A", R.drawable.chocopie,     barcode = "880000000005"),
        Product("P006", "고래밥",  "Whale Snack",  ProductCategory.SNACK, 1000, "B", R.drawable.gorae,        barcode = "880000000006"),
        Product("P007", "콜라",    "Cola",         ProductCategory.DRINK, 1500, "냉장고1", R.drawable.coke,   barcode = "880000000007"),
        Product("P008", "사이다",  "Sprite",       ProductCategory.DRINK, 1500, "냉장고1", R.drawable.cider,  barcode = "880000000008"),
        Product("P009", "오렌지주스","Orange Juice",ProductCategory.DRINK, 2500, "냉장고2", R.drawable.orange,barcode = "880000000009"),
        Product("P010", "초코우유","Chocolate Milk",ProductCategory.DRINK, 2000, "냉장고2", R.drawable.choco, barcode = "880000000010")
    )

    fun getProductById(id: String): Product? = products.find { it.id == id }

    fun getProductsByCategory(category: ProductCategory): List<Product> =
        products.filter { it.category == category }
}
