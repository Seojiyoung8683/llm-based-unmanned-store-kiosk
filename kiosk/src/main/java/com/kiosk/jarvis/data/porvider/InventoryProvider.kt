package com.kiosk.jarvis.data.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first            // ✅ 추가
import com.kiosk.jarvis.data.local.JarvisDatabase
import com.kiosk.jarvis.data.local.ProductEntity

class InventoryProvider : ContentProvider() {

    companion object {
        private const val AUTH = "com.kiosk.jarvis.inventory"
        private const val PATH_PRODUCTS = "products"
        private const val CODE_PRODUCTS = 1

        val CONTENT_URI_PRODUCTS: Uri =
            Uri.parse("content://$AUTH/$PATH_PRODUCTS")
    }

    private lateinit var db: JarvisDatabase

    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTH, PATH_PRODUCTS, CODE_PRODUCTS)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("insert not supported")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("update not supported")
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("delete not supported")
    }


    override fun onCreate(): Boolean {
        db = JarvisDatabase.get(requireNotNull(context))
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (matcher.match(uri)) {
            CODE_PRODUCTS -> {
                val list: List<ProductEntity> = runBlocking {
                    db.productDao().observeAll().first()
                }
                toCursor(list)
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = when (matcher.match(uri)) {
        CODE_PRODUCTS -> "vnd.android.cursor.dir/vnd.$AUTH.$PATH_PRODUCTS"
        else -> null
    }

    private fun toCursor(list: List<ProductEntity>): Cursor {
        val columns = arrayOf("_id", "id", "name", "price", "category", "barcode", "imageUrl", "updatedAt", "deleted")
        val c = MatrixCursor(columns)
        var idx = 0L
        list.forEach { p ->
            c.addRow(arrayOf(
                idx++,
                p.id,
                p.name,
                p.price,
                p.category,
                p.barcode,
                p.imageUrl,
                p.updatedAt,
                if (p.deleted) 1 else 0
            ))
        }
        return c
    }

}
