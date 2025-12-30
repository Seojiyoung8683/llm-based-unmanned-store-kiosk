// com.kiosk.jarvis.model.Product (UI 모델)은 그대로 사용

// 새 파일: com/kiosk/jarvis/bootstrap/mappers.kt
package com.kiosk.jarvis.bootstrap

import com.kiosk.jarvis.data.local.InventoryEntity
import com.kiosk.jarvis.data.local.ProductEntity
import com.kiosk.jarvis.model.Product

fun Product.toProductEntity(): ProductEntity =
    ProductEntity(
        id = this.id.toString(),
        name = this.nameKo,
        price = this.price,
        category = this.category.name,
        barcode = this.barcode,
        imageUrl = null
    )

fun Product.toInventoryEntity(defaultStock: Int = 0): InventoryEntity =
    InventoryEntity(
        productId = this.id.toString(),
        stock = defaultStock,
        location = this.location
    )
