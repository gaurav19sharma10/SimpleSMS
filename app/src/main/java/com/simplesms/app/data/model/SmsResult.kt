package com.simplesms.app.data.model

sealed interface SmsResult {
    data class Success(val message: String, val partsCount: Int) : SmsResult
    data class Error(val reason: String, val errorCode: Int? = null) : SmsResult
    data class Progress(val status: String) : SmsResult
}
