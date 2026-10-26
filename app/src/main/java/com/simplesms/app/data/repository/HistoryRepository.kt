package com.simplesms.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.simplesms.app.data.model.DispatchHistory
import com.simplesms.app.data.model.RecipientHistoryRecord
import com.simplesms.app.data.model.SendStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    suspend fun getAllHistory(): List<DispatchHistory> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DispatchHistory>()
        val db = dbHelper.writableDatabase

        val cursor = db.query(
            TABLE_HISTORY,
            null, null, null, null, null,
            "$COLUMN_TIMESTAMP DESC"
        )

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(COLUMN_ID)
            val timeCol = it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val msgCol = it.getColumnIndexOrThrow(COLUMN_MESSAGE)
            val carrierCol = it.getColumnIndexOrThrow(COLUMN_SIM_CARRIER)
            val slotCol = it.getColumnIndexOrThrow(COLUMN_SIM_SLOT)
            val totalCol = it.getColumnIndexOrThrow(COLUMN_TOTAL)
            val successCol = it.getColumnIndexOrThrow(COLUMN_SUCCESS)
            val failCol = it.getColumnIndexOrThrow(COLUMN_FAILURE)
            val recJsonCol = it.getColumnIndexOrThrow(COLUMN_RECIPIENTS_JSON)

            while (it.moveToNext()) {
                val jsonString = it.getString(recJsonCol) ?: "[]"
                val recipients = parseRecipients(jsonString)

                list.add(
                    DispatchHistory(
                        id = it.getLong(idCol),
                        timestamp = it.getLong(timeCol),
                        message = it.getString(msgCol) ?: "",
                        simCarrier = it.getString(carrierCol) ?: "Unknown",
                        simSlot = it.getInt(slotCol),
                        totalCount = it.getInt(totalCol),
                        successCount = it.getInt(successCol),
                        failureCount = it.getInt(failCol),
                        recipients = recipients
                    )
                )
            }
        }
        list
    }

    suspend fun insertHistory(history: DispatchHistory): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, history.timestamp)
            put(COLUMN_MESSAGE, history.message)
            put(COLUMN_SIM_CARRIER, history.simCarrier)
            put(COLUMN_SIM_SLOT, history.simSlot)
            put(COLUMN_TOTAL, history.totalCount)
            put(COLUMN_SUCCESS, history.successCount)
            put(COLUMN_FAILURE, history.failureCount)
            put(COLUMN_RECIPIENTS_JSON, serializeRecipients(history.recipients))
        }
        db.insert(TABLE_HISTORY, null, values)
    }

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(TABLE_HISTORY, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(TABLE_HISTORY, null, null)
    }

    private fun serializeRecipients(list: List<RecipientHistoryRecord>): String {
        val array = JSONArray()
        list.forEach { rec ->
            val obj = JSONObject().apply {
                put("name", rec.name)
                put("phone", rec.phoneNumber)
                put("status", rec.status.name)
                put("error", rec.errorMessage ?: "")
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseRecipients(json: String): List<RecipientHistoryRecord> {
        val list = mutableListOf<RecipientHistoryRecord>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val statusName = obj.optString("status", "SENT")
                val status = try { SendStatus.valueOf(statusName) } catch (_: Exception) { SendStatus.SENT }
                val error = obj.optString("error", "").ifBlank { null }

                list.add(
                    RecipientHistoryRecord(
                        name = obj.optString("name", "Unknown"),
                        phoneNumber = obj.optString("phone", ""),
                        status = status,
                        errorMessage = error
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_HISTORY (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_TIMESTAMP INTEGER NOT NULL,
                    $COLUMN_MESSAGE TEXT NOT NULL,
                    $COLUMN_SIM_CARRIER TEXT NOT NULL,
                    $COLUMN_SIM_SLOT INTEGER NOT NULL,
                    $COLUMN_TOTAL INTEGER NOT NULL,
                    $COLUMN_SUCCESS INTEGER NOT NULL,
                    $COLUMN_FAILURE INTEGER NOT NULL,
                    $COLUMN_RECIPIENTS_JSON TEXT NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
            onCreate(db)
        }
    }

    companion object {
        private const val DATABASE_NAME = "simple_sms_history.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_HISTORY = "history"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_SIM_CARRIER = "sim_carrier"
        private const val COLUMN_SIM_SLOT = "sim_slot"
        private const val COLUMN_TOTAL = "total_count"
        private const val COLUMN_SUCCESS = "success_count"
        private const val COLUMN_FAILURE = "failure_count"
        private const val COLUMN_RECIPIENTS_JSON = "recipients_json"
    }
}
