package com.simplesms.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecipientHistoryRecord(
    val name: String,
    val phoneNumber: String,
    val status: SendStatus,
    val errorMessage: String? = null
)

data class DispatchHistory(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val simCarrier: String,
    val simSlot: Int,
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val recipients: List<RecipientHistoryRecord>
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
