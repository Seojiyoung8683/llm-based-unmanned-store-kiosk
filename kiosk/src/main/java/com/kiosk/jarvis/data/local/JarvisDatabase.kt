package com.kiosk.jarvis.data.local

import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        com.kiosk.jarvis.data.local.OrderEntity::class,
        com.kiosk.jarvis.data.local.ProductEntity::class,
        com.kiosk.jarvis.data.local.InventoryEntity::class,
        com.kiosk.jarvis.data.local.OrderItemEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(com.kiosk.jarvis.data.local.Converters::class)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun orderDao(): com.kiosk.jarvis.data.local.OrderDao
    abstract fun productDao(): com.kiosk.jarvis.data.local.ProductDao
    abstract fun inventoryDao(): com.kiosk.jarvis.data.local.InventoryDao
    abstract fun orderItemDao(): com.kiosk.jarvis.data.local.OrderItemDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean {
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(name)
            ).use { c ->
                return c.moveToFirst()
            }
        }

        private fun columnExists(
            db: SupportSQLiteDatabase,
            table: String,
            column: String
        ): Boolean {
            db.query("PRAGMA table_info($table)").use { c: Cursor ->
                val nameIdx = c.getColumnIndex("name")
                while (c.moveToNext()) {
                    if (c.getString(nameIdx).equals(column, ignoreCase = true)) return true
                }
            }
            return false
        }

        // 1 → 2 : products / inventory / orders 방어적 마이그레이션
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // === products ===
                if (!tableExists(db, "products")) {
                    db.execSQL(
                        """
                        CREATE TABLE products (
                            id TEXT NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            price INTEGER NOT NULL,
                            category TEXT NOT NULL,
                            barcode TEXT,
                            imageUrl TEXT,
                            stock INTEGER NOT NULL DEFAULT 0,
                            updatedAt INTEGER NOT NULL DEFAULT 0,
                            deleted INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                } else {
                    if (!columnExists(db, "products", "barcode")) {
                        db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT")
                    }
                    if (!columnExists(db, "products", "imageUrl")) {
                        db.execSQL("ALTER TABLE products ADD COLUMN imageUrl TEXT")
                    }
                    if (!columnExists(db, "products", "stock")) {
                        db.execSQL(
                            "ALTER TABLE products ADD COLUMN stock INTEGER NOT NULL DEFAULT 0"
                        )
                    }
                    if (!columnExists(db, "products", "updatedAt")) {
                        db.execSQL(
                            "ALTER TABLE products ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
                        )
                    }
                    if (!columnExists(db, "products", "deleted")) {
                        db.execSQL(
                            "ALTER TABLE products ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0"
                        )
                    }
                }

                // === inventory ===
                if (!tableExists(db, "inventory")) {
                    db.execSQL(
                        """
                        CREATE TABLE inventory (
                            productId TEXT NOT NULL PRIMARY KEY,
                            stock INTEGER NOT NULL,
                            minThreshold INTEGER NOT NULL DEFAULT 0,
                            location TEXT,
                            updatedAt INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                } else {
                    if (!columnExists(db, "inventory", "minThreshold")) {
                        db.execSQL(
                            "ALTER TABLE inventory ADD COLUMN minThreshold INTEGER NOT NULL DEFAULT 0"
                        )
                    }
                    if (!columnExists(db, "inventory", "location")) {
                        db.execSQL("ALTER TABLE inventory ADD COLUMN location TEXT")
                    }
                    if (!columnExists(db, "inventory", "updatedAt")) {
                        db.execSQL(
                            "ALTER TABLE inventory ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
                        )
                    }
                }

                // === orders ===
                if (!tableExists(db, "orders")) {
                    db.execSQL(
                        """
                        CREATE TABLE orders (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            orderNumber TEXT NOT NULL,
                            orderedAtMillis INTEGER NOT NULL,
                            totalPrice INTEGER NOT NULL,
                            paymentMethod TEXT NOT NULL,
                            status TEXT NOT NULL,
                            items TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                } else {
                    val hasItems = columnExists(db, "orders", "items")
                    val hasItemsJson = columnExists(db, "orders", "itemsJson")

                    if (!hasItems && hasItemsJson) {
                        db.execSQL(
                            """
                            CREATE TABLE orders_new (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                orderNumber TEXT NOT NULL,
                                orderedAtMillis INTEGER NOT NULL,
                                totalPrice INTEGER NOT NULL,
                                paymentMethod TEXT NOT NULL,
                                status TEXT NOT NULL,
                                items TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO orders_new (id, orderNumber, orderedAtMillis, totalPrice, paymentMethod, status, items)
                            SELECT id, orderNumber, orderedAtMillis, totalPrice, paymentMethod, status, itemsJson
                            FROM orders
                            """.trimIndent()
                        )
                        db.execSQL("DROP TABLE orders")
                        db.execSQL("ALTER TABLE orders_new RENAME TO orders")
                    } else if (!hasItems && !hasItemsJson) {
                        db.execSQL("DROP TABLE orders")
                        db.execSQL(
                            """
                            CREATE TABLE orders (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                orderNumber TEXT NOT NULL,
                                orderedAtMillis INTEGER NOT NULL,
                                totalPrice INTEGER NOT NULL,
                                paymentMethod TEXT NOT NULL,
                                status TEXT NOT NULL,
                                items TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                    }
                }
            }
        }

        // 2 → 3: order_items 생성
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderId INTEGER NOT NULL,
                        productId TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice INTEGER NOT NULL,
                        orderedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_items_order   ON order_items(orderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_items_date    ON order_items(orderedAtMillis)")
            }
        }

        // 3 → 4 : 추가 방어용 마이그레이션 (stock 누락 대비)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "products", "stock")) {
                    db.execSQL(
                        "ALTER TABLE products ADD COLUMN stock INTEGER NOT NULL DEFAULT 0"
                    )
                }
                // 혹시 order_items 테이블이 없는 DB가 있을 경우 방어적으로 생성
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderId INTEGER NOT NULL,
                        productId TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice INTEGER NOT NULL,
                        orderedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_items_order   ON order_items(orderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_items_date    ON order_items(orderedAtMillis)")
            }
        }

        fun get(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "kiosk.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
