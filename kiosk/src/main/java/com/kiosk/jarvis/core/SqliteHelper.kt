package com.kiosk.jarvis.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.apply
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.text.trimIndent
import kotlin.to

class SqliteHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object{
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "SUDA_API_MAPPING.db"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE API_CALL (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT" +
                ", token_header TEXT" +
                ", api_method TEXT" +
                ", answer_kr TEXT" +
                ", answer_en TEXT" +
                ")")

        db?.execSQL("CREATE TABLE API_PARAM (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT " +
                ", api_call_id INTEGER " +
                ", param TEXT" +
                ", value TEXT" +
                ", FOREIGN KEY (api_call_id) REFERENCES API_CALL (id) ON DELETE CASCADE)")

        db?.execSQL("CREATE TABLE LLM_PARAM (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT" +
                ", api_call_id INTEGER" +
                ", param TEXT" +
                ", value TEXT" +
                ", FOREIGN KEY (api_call_id) REFERENCES API_CALL (id) ON DELETE CASCADE)")

        db?.execSQL("CREATE TABLE PARALLEL_ANSWER (" +
                "llm_response TEXT PRIMARY KEY" +
                ", answer_kr TEXT" +
                ", answer_en TEXT)")

        db?.execSQL("CREATE TABLE MULTITURN_ANSWER (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT" +
                ", token_header TEXT" +
                ", multiturn_end INTEGER" +
                ", answer_order INTEGER" +
                ", answer_kr TEXT" +
                ", answer_en TEXT)")

        db?.execSQL("CREATE TABLE MULTITURN_LLM_PARAM (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT " +
                ", multiturn_answer_id INTEGER " +
                ", param TEXT" +
                ", value TEXT" +
                ", FOREIGN KEY (multiturn_answer_id) REFERENCES API_CALL (id) ON DELETE CASCADE)")

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS API_CALL")
        db?.execSQL("DROP TABLE IF EXISTS API_PARAM")
        db?.execSQL("DROP TABLE IF EXISTS LLM_PARAM")
        db?.execSQL("DROP TABLE IF EXISTS PARALLEL_ANSWER")
        db?.execSQL("DROP TABLE IF EXISTS MULTITURN_ANSWER")
        db?.execSQL("DROP TABLE IF EXISTS MULTITURN_LLM_PARAM")
        onCreate(db)
    }

    fun insert(tokenHeader: String, apiMethod: String, answerKr: String, answerEn: String, llmParams: Map<String, String>, apiParams: Map<String, String>) {
        val check = selectByMultiParam(tokenHeader, llmParams)

        if (check != null) {
            return
        }

        val db = writableDatabase
        db.beginTransaction()

        try {
            val apiCall = ContentValues().apply {
                put("token_header", tokenHeader)
                put("api_method", apiMethod)
                put("answer_kr", answerKr)
                put("answer_en", answerEn)
            }
            val apiCallId = db.insert("API_CALL", null, apiCall)

            llmParams.forEach { (param, value) ->
                val llmParam = ContentValues().apply {
                    put("api_call_id", apiCallId)
                    put("param", param)
                    put("value", value)
                }
                db.insert("LLM_PARAM", null, llmParam)
            }

            apiParams.forEach { (param, value) ->
                val apiParam = ContentValues().apply {
                    put("api_call_id", apiCallId)
                    put("param", param)
                    put("value", value)
                }
                db.insert("API_PARAM", null, apiParam)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertMultiturnAnswer(tokenHeader: String, answerOrder: Int, isMultiturnEnd: Boolean, answerKr: String, answerEn: String, params: Map<String, String>) {
        val check = selectMultiturnAnswerByParam(tokenHeader, answerOrder, params)

        if (check != null) {
            return
        }

        val db = writableDatabase
        db.beginTransaction()

        try {
            val multiturnAnswer = ContentValues().apply {
                put("token_header", tokenHeader)
                put("answer_order", answerOrder)
                put("answer_kr", answerKr)
                put("answer_en", answerEn)
                put("multiturn_end", if (isMultiturnEnd) 1 else 0)
            }
            val id = db.insert("MULTITURN_ANSWER", null, multiturnAnswer)

            params.forEach { (param, value) ->
                val param = ContentValues().apply {
                    put("multiturn_answer_id", id)
                    put("param", param)
                    put("value", value)
                }
                db.insert("MULTITURN_LLM_PARAM", null, param)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun selectMultiturnAnswerByParam(tokenHeader: String, multiturnCount: Int, params: Map<String, String>): MultiturnAnswer? {
        var result: MultiturnAnswer? = null
        val db = this.readableDatabase

        var query = "SELECT ma.answer_order, ma.multiturn_end, ma.answer_kr, ma.answer_en from MULTITURN_ANSWER ma "

        params.keys.forEachIndexed { index, _ ->
            query += "left join MULTITURN_LLM_PARAM lp$index on ma.id = lp$index.multiturn_answer_id "
        }

        query += "where ma.token_header = ? AND ma.answer_order = ? "

        params.keys.forEachIndexed { index, _ ->
            query += "AND lp$index.param = ? AND lp$index.value = ?"
        }

        var queryArgs: Array<String> = arrayOf(tokenHeader, multiturnCount.toString())

        params.forEach { (key, value) ->
            queryArgs = queryArgs.plus(key).plus(value)
        }

        val cursor = db.rawQuery(query, queryArgs)

        if (cursor.moveToFirst()) {
            val answerOrder = cursor.getInt(cursor.getColumnIndexOrThrow("answer_order"))
            val answerKR = cursor.getString(cursor.getColumnIndexOrThrow("answer_kr"))
            val answerEN = cursor.getString(cursor.getColumnIndexOrThrow("answer_en"))
            val multiturnEnd = cursor.getInt(cursor.getColumnIndexOrThrow("multiturn_end"))

            result = MultiturnAnswer(if (multiturnEnd == 0) false else true, answerOrder, answerKR, answerEN)
        }
        cursor.close()

        return result
    }

    fun selectByMultiParam(tokenHeader: String, params: Map<String, String>): ApiCallParam? {
        var result: ApiCallParam? = null
        val db = this.readableDatabase

        var query = "SELECT ac.api_method, ac.answer_kr, ac.answer_en, ap.param AS api_param, ap.value AS api_value " +
                "FROM API_CALL ac " +
                "LEFT JOIN API_PARAM ap ON ac.id = ap.api_call_id "

        params.keys.forEachIndexed { index, _ ->
            query += "LEFT JOIN LLM_PARAM lp$index ON ac.id = lp$index.api_call_id "
        }

        query += "WHERE ac.token_header = ? "

        params.keys.forEachIndexed { index, _ ->
            query += "AND lp$index.param = ? AND lp$index.value = ? "
        }

        var queryArgs = arrayOf(tokenHeader)

        params.forEach { (key, value) ->
            queryArgs = queryArgs.plus(key).plus(value)
        }

        val cursor = db.rawQuery(query, queryArgs)

        if (cursor.moveToFirst()) {
            val apiMethod = cursor.getString(cursor.getColumnIndexOrThrow("api_method"))
            val answerKR = cursor.getString(cursor.getColumnIndexOrThrow("answer_kr"))
            val answerEN = cursor.getString(cursor.getColumnIndexOrThrow("answer_en"))

            val apiParams = kotlin.collections.HashMap<String, String>()

            do {
                val apiParam = cursor.getString(cursor.getColumnIndexOrThrow("api_param"))
                val apiValue = cursor.getString(cursor.getColumnIndexOrThrow("api_value"))

                if (apiParam != null && apiValue != null) {
                    apiParams[apiParam] = apiValue                     }
            } while (cursor.moveToNext())

            result = ApiCallParam(apiMethod, answerKR, answerEN, apiParams)
        }
        cursor.close()

        return result
    }


    fun selectParallelAnswer(llmResponse: String): ParallelAnswer? {
        var result: ParallelAnswer? = null
        val db = this.readableDatabase

        val cursor = db.rawQuery("""
            SELECT answer_kr, answer_en
            FROM PARALLEL_ANSWER
            WHERE llm_response = ?
        """.trimIndent(), arrayOf(llmResponse)
        )

        if (cursor.moveToFirst()) {
            val answerKR = cursor.getString(cursor.getColumnIndexOrThrow("answer_kr"))
            val answerEN = cursor.getString(cursor.getColumnIndexOrThrow("answer_en"))

            result = ParallelAnswer(answerKR, answerEN)
        }
        cursor.close()

        return result
    }

    fun insertParallelAnswer(llmResponse: String, answerKr: String, answerEn: String) {
        val check = selectParallelAnswer(llmResponse)

        if (check != null) {
            return
        }

        val db = writableDatabase
        db.beginTransaction()

        try {
            val parallelAnswer = ContentValues().apply {
                put("llm_response", llmResponse)
                put("answer_en", answerEn)
                put("answer_kr", answerKr)
            }
            db.insert("PARALLEL_ANSWER", null, parallelAnswer)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun selectByApiParam(tokenHeader: String, params: Map<String, String>): ApiCallParam? {
        var result: ApiCallParam? = null
        val db = this.readableDatabase

        var query = "SELECT ac.api_method, ac.answer_kr, ac.answer_en, lp.param, lp.value " +
                "from API_CALL ac " +
                "left join LLM_PARAM lp on ac.id = lp.api_call_id "

        params.keys.forEachIndexed { index, _ ->
            query += "left join API_PARAM ap$index on ac.id = ap$index.api_call_id "
        }

        query += "where ac.token_header = ? "

        params.keys.forEachIndexed { index, _ ->
            query += "AND ap$index.param = ? AND ap$index.value = ?"
        }

        var queryArgs = arrayOf(tokenHeader)

        params.forEach { (key, value) ->
            queryArgs = queryArgs.plus(key).plus(value)
        }

        val cursor = db.rawQuery(query, queryArgs)

        if (cursor.moveToFirst()) {
            val apiMethod = cursor.getString(cursor.getColumnIndexOrThrow("api_method"))
            val answerKR = cursor.getString(cursor.getColumnIndexOrThrow("answer_kr"))
            val answerEN = cursor.getString(cursor.getColumnIndexOrThrow("answer_en"))

            val apiParams = kotlin.collections.HashMap<String, String>()

            do {
                val apiParam = cursor.getString(cursor.getColumnIndexOrThrow("param"))
                val apiValue = cursor.getString(cursor.getColumnIndexOrThrow("value"))

                apiParams[apiParam] = apiValue
            } while (cursor.moveToNext())

            result = ApiCallParam(apiMethod, answerKR, answerEN, apiParams)
        }
        cursor.close()

        return result
    }

    data class ApiCallParam (
        val apiMethod: String,
        val answerKr: String,
        val answerEn: String,
        val params: Map<String, String>
    )

    data class ParallelAnswer (
        val answerKr: String,
        val answerEn: String
    )

    data class MultiturnAnswer (
        val isMultiturnEnd: Boolean,
        val answerOrder: Int,
        val answerKr: String,
        val answerEn: String
    )
    fun initApiParam() {
        // ---------------------------
        // 1. 조명 제어: <jarvis_0>(enable=True/False)
        // ---------------------------
        insert(
            "<jarvis_0>",
            "control_manager",
            "조명을 켰습니다.",
            "The lights have been turned on.",
            mapOf("enable" to "True"),
            mapOf()
        )

        insert(
            "<jarvis_0>",
            "control_manager",
            "조명을 껐습니다.",
            "The lights have been turned off.",
            mapOf("enable" to "False"),
            mapOf()
        )

        // ---------------------------
        // 2. 출입문 제어: <jarvis_1>(enable=True/False)
        // ---------------------------
        insert(
            "<jarvis_1>",
            "control_door",
            "문을 열었습니다.",
            "The door has been opened.",
            mapOf("enable" to "True"),
            mapOf()
        )

        insert(
            "<jarvis_1>",
            "control_door",
            "문을 잠궜습니다.",
            "The door has been locked.",
            mapOf("enable" to "False"),
            mapOf()
        )

        // ---------------------------
        // 3. 에어컨 제어: <jarvis_2>(enable=True/False)
        // ---------------------------
        insert(
            "<jarvis_2>",
            "control_air_conditioner",
            "에어컨을 켰습니다.",
            "The air conditioner has been turned on.",
            mapOf("enable" to "True"),
            mapOf()
        )

        insert(
            "<jarvis_2>",
            "control_air_conditioner",
            "에어컨을 껐습니다.",
            "The air conditioner has been turned off.",
            mapOf("enable" to "False"),
            mapOf()
        )

        // ---------------------------
        // 4. 블라인드 제어: <jarvis_3>(enable=True/False)
        // ---------------------------
        insert(
            "<jarvis_3>",
            "control_blind",
            "블라인드를 올렸습니다.",
            "The blinds have been raised.",
            mapOf("enable" to "True"),
            mapOf()
        )

        insert(
            "<jarvis_3>",
            "control_blind",
            "블라인드를 내렸습니다.",
            "The blinds have been lowered.",
            mapOf("enable" to "False"),
            mapOf()
        )

        // ---------------------------
        // 5. 제품 위치 탐색: <jarvis_4>(product="1"~"10")
        // ---------------------------
        insert(
            "<jarvis_4>",
            "product_location_query",
            "홈런볼은 A구역에 있습니다.",
            "Home Run Ball is located in section A.",
            mapOf("product" to "1"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "새우깡은 A구역에 있습니다.",
            "Saewookkang is located in section A.",
            mapOf("product" to "2"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "꼬북칩은 A구역에 있습니다.",
            "Kkobukchip is located in section A.",
            mapOf("product" to "3"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "빼빼로는 B구역에 있습니다.",
            "Pepero is located in section B.",
            mapOf("product" to "4"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "초코파이는 B구역에 있습니다.",
            "Choco Pie is located in section B.",
            mapOf("product" to "5"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "고래밥은 B구역에 있습니다.",
            "Goraebap is located in section B.",
            mapOf("product" to "6"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "콜라는 냉장고1에 있습니다.",
            "Cola is in refrigerator 1.",
            mapOf("product" to "7"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "사이다는 냉장고2에 있습니다.",
            "Cider is in refrigerator 2.",
            mapOf("product" to "8"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "오렌지주스는 냉장고1에 있습니다.",
            "Orange juice is in refrigerator 1.",
            mapOf("product" to "9"),
            mapOf()
        )

        insert(
            "<jarvis_4>",
            "product_location_query",
            "초코우유는 냉장고2에 있습니다.",
            "Chocolate milk is in refrigerator 2.",
            mapOf("product" to "10"),
            mapOf()
        )

        // ---------------------------
        // 6. 구역별/냉장고별 상품 안내: <jarvis_5>(section="1"~"4")
        // ---------------------------
        insert(
            "<jarvis_5>",
            "section_query",
            "A구역에는 홈런볼, 새우깡, 꼬북칩이 있습니다.",
            "In section A, there are Home Run Ball, Saewookkang, and Kkobukchip.",
            mapOf("section" to "1"),
            mapOf()
        )

        insert(
            "<jarvis_5>",
            "section_query",
            "B구역에는 빼빼로, 초코파이, 새우깡이 있습니다.",
            "In section B, there are Pepero, Choco Pie, and Saewookkang.",
            mapOf("section" to "2"),
            mapOf()
        )

        insert(
            "<jarvis_5>",
            "section_query",
            "냉장고1에는 콜라, 오렌지주스가 있습니다.",
            "In refrigerator 1, there are Cola and Orange juice.",
            mapOf("section" to "3"),
            mapOf()
        )

        insert(
            "<jarvis_5>",
            "section_query",
            "냉장고2에는 사이다, 초코우유가 있습니다.",
            "In refrigerator 2, there are Cider and Chocolate milk.",
            mapOf("section" to "4"),
            mapOf()
        )

        // ---------------------------
        // 7. 음악 제어: <jarvis_6>(enable=True/False)
        // ---------------------------
        insert(
            "<jarvis_6>",
            "control_music",
            "음악을 켰습니다.",
            "The music has been turned on.",
            mapOf("enable" to "True"),
            mapOf()
        )

        insert(
            "<jarvis_6>",
            "control_music",
            "음악을 껐습니다.",
            "The music has been turned off.",
            mapOf("enable" to "False"),
            mapOf()
        )

        // ---------------------------
        // 8. 가습기 제어: <jarvis_7>(enable=True/False)
        // ---------------------------
        insert(
            "<jarvis_7>",
            "control_humidifier",
            "가습기를 켰습니다.",
            "The humidifier has been turned on.",
            mapOf("enable" to "True"),
            mapOf()
        )

        insert(
            "<jarvis_7>",
            "control_humidifier",
            "가습기를 껐습니다.",
            "The humidifier has been turned off.",
            mapOf("enable" to "False"),
            mapOf()
        )
    }

}