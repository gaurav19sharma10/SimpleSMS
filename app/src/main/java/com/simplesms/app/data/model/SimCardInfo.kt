package com.simplesms.app.data.model

data class SimCardInfo(
    val slotIndex: Int,          // 0 for SIM 1, 1 for SIM 2
    val subscriptionId: Int,     // Android internal subId
    val displayName: String,     // e.g. "Jio 4G", "Airtel"
    val carrierName: String,     // e.g. "Jio", "Bharti Airtel"
    val phoneNumber: String? = null,
    val isDefault: Boolean = false
) {
    val userFacingLabel: String
        get() = "SIM ${slotIndex + 1} ($carrierName)"
}
