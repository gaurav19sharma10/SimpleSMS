package com.simplesms.app.data.model

enum class SendStatus {
    IDLE,
    QUEUED,
    SENDING,
    SENT,
    FAILED
}

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isSelected: Boolean = false,
    val sendStatus: SendStatus = SendStatus.IDLE,
    val errorMessage: String? = null
) {
    val initials: String
        get() {
            val parts = name.trim().split("\\s+".toRegex())
            return when {
                parts.isEmpty() || parts[0].isEmpty() -> "?"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts[0].first()}${parts[1].first()}".uppercase()
            }
        }
}
