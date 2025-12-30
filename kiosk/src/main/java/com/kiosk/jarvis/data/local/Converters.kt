package com.kiosk.jarvis.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kiosk.jarvis.model.OrderItem

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromOrderItemList(list: List<OrderItem>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toOrderItemList(json: String): List<OrderItem> {
        val type = object : TypeToken<List<OrderItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
