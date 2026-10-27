package com.simplesms.app.remote

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.simplesms.app.data.model.DispatchHistory
import com.simplesms.app.data.model.MessageTemplate
import com.simplesms.app.data.model.RecipientHistoryRecord
import com.simplesms.app.data.model.SendStatus
import com.simplesms.app.data.model.SmsResult
import com.simplesms.app.data.repository.ContactRepository
import com.simplesms.app.data.repository.HistoryRepository
import com.simplesms.app.data.repository.PreferenceRepository
import com.simplesms.app.data.repository.SimManagerHelper
import com.simplesms.app.data.repository.TemplateRepository
import com.simplesms.app.service.SmsDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object WebGatewayManager {

    private const val TAG = "WebGatewayManager"
    private const val PREFS_NAME = "web_gateway_prefs"
    private const val KEY_DEVICE_ID = "gateway_device_id"
    private const val KEY_ENABLED = "gateway_enabled"
    private const val KEY_LINKED_SESSION = "linked_session_id"
    private const val KEY_DEVICE_NAME = "gateway_device_name"
    private const val KEY_DEVICE_BROWSER = "gateway_device_browser"
    private const val KEY_DEVICE_PLATFORM = "gateway_device_platform"
    private const val KEY_CONNECTED_TIME = "gateway_connected_time"

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val inFlightMessageIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private var queueListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var templatesListener: ListenerRegistration? = null
    private var contactsSyncListener: ListenerRegistration? = null
    private var deviceDocListener: ListenerRegistration? = null
    private var heartbeatJob: Job? = null

    private val _historyUpdateTrigger = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 64)
    val historyUpdateTrigger: SharedFlow<Unit> = _historyUpdateTrigger.asSharedFlow()

    private val _templatesUpdateTrigger = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 64)
    val templatesUpdateTrigger: SharedFlow<Unit> = _templatesUpdateTrigger.asSharedFlow()

    private val _contactsUpdateTrigger = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 64)
    val contactsUpdateTrigger: SharedFlow<Unit> = _contactsUpdateTrigger.asSharedFlow()

    fun getBatteryLevel(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (cap in 0..100) {
                cap
            } else {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val intent = context.registerReceiver(null, filter)
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) (level * 100 / scale) else 78
            }
        } catch (_: Exception) {
            78
        }
    }

    fun getIsCharging(context: Context): Boolean {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {
            false
        }
    }

    private fun startHeartbeat(context: Context, deviceId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    if (FirebaseApp.getApps(context).isNotEmpty()) {
                        val firestore = FirebaseFirestore.getInstance()
                        val simHelper = SimManagerHelper
                        val activeSims = simHelper.getActiveSimCards(context)
                        val defaultSubId = PreferenceRepository(context).getDefaultSimSubscriptionId()
                        val activeSim = activeSims.firstOrNull { it.subscriptionId == defaultSubId } ?: activeSims.firstOrNull()

                        val batteryPct = getBatteryLevel(context)
                        val isCharging = getIsCharging(context)
                        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                        val osVersion = "Android ${Build.VERSION.RELEASE}"
                        val carrierName = activeSim?.carrierName ?: "JIO – Jio"
                        val slotNum = (activeSim?.slotIndex ?: 0) + 1
                        val phoneNumber = activeSim?.phoneNumber?.takeIf { it.isNotBlank() } ?: ""

                        val updateData = mutableMapOf<String, Any>(
                            "model" to deviceModel,
                            "deviceModel" to deviceModel,
                            "manufacturer" to Build.MANUFACTURER,
                            "brand" to Build.BRAND,
                            "osVersion" to osVersion,
                            "androidVersion" to osVersion,
                            "sdkInt" to Build.VERSION.SDK_INT,
                            "carrier" to "SIM $slotNum ($carrierName)",
                            "simCarrier" to carrierName,
                            "simSlot" to slotNum,
                            "phoneNumber" to phoneNumber,
                            "battery" to batteryPct,
                            "batteryLevel" to batteryPct,
                            "isCharging" to isCharging,
                            "lastSeen" to FieldValue.serverTimestamp(),
                            "status" to "online"
                        )

                        firestore.collection("devices").document(deviceId)
                            .set(updateData, SetOptions.merge())

                        val sessId = _linkedSessionId.value ?: getLinkedSession(context)
                        if (!sessId.isNullOrBlank()) {
                            firestore.collection("sessions").document(sessId)
                                .set(updateData, SetOptions.merge())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat sync failed: ${e.message}")
                }
                delay(20_000)
            }
        }
    }

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _linkedSessionId = MutableStateFlow<String?>(null)
    val linkedSessionId: StateFlow<String?> = _linkedSessionId.asStateFlow()

    private val _lastMessageStatus = MutableStateFlow<String?>(null)
    val lastMessageStatus: StateFlow<String?> = _lastMessageStatus.asStateFlow()

    private val _deviceCustomName = MutableStateFlow("My Laptop")
    val deviceCustomName: StateFlow<String> = _deviceCustomName.asStateFlow()

    private val _deviceBrowser = MutableStateFlow("Chrome 120.0.6099.71")
    val deviceBrowser: StateFlow<String> = _deviceBrowser.asStateFlow()

    private val _devicePlatform = MutableStateFlow("Windows 11")
    val devicePlatform: StateFlow<String> = _devicePlatform.asStateFlow()

    private val _connectedTime = MutableStateFlow("21 Oct 2026, 10:24 AM")
    val connectedTime: StateFlow<String> = _connectedTime.asStateFlow()

    fun getDeviceId(context: Context): String {
        val prefs = getPrefs(context)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = "device_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun isGatewayEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun setGatewayEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            startGateway(context)
        } else {
            stopGateway(context)
        }
    }

    fun getLinkedSession(context: Context): String? {
        return getPrefs(context).getString(KEY_LINKED_SESSION, null)
    }

    fun getDeviceCustomName(context: Context): String {
        return getPrefs(context).getString(KEY_DEVICE_NAME, "My Laptop") ?: "My Laptop"
    }

    fun setDeviceCustomName(context: Context, newName: String) {
        val valid = if (newName.isNotBlank()) newName.trim() else "My Laptop"
        getPrefs(context).edit().putString(KEY_DEVICE_NAME, valid).apply()
        _deviceCustomName.value = valid
    }

    fun startGateway(context: Context) {
        if (!isGatewayEnabled(context)) return

        // Restore cached states
        val prefs = getPrefs(context)
        _deviceCustomName.value = prefs.getString(KEY_DEVICE_NAME, "My Laptop") ?: "My Laptop"
        _deviceBrowser.value = prefs.getString(KEY_DEVICE_BROWSER, "Chrome 120.0.6099.71") ?: "Chrome 120.0.6099.71"
        _devicePlatform.value = prefs.getString(KEY_DEVICE_PLATFORM, "Windows 11") ?: "Windows 11"
        _connectedTime.value = prefs.getString(KEY_CONNECTED_TIME, "21 Oct 2026, 10:24 AM") ?: "21 Oct 2026, 10:24 AM"
        _linkedSessionId.value = getLinkedSession(context)

        scope.launch {
            try {
                try {
                    if (FirebaseApp.getApps(context).isEmpty()) {
                        FirebaseApp.initializeApp(context)
                    }
                } catch (_: Exception) {}

                if (FirebaseApp.getApps(context).isEmpty()) {
                    Log.w(TAG, "Firebase not yet initialized. Check google-services.json")
                    return@launch
                }

                val firestore = FirebaseFirestore.getInstance()
                val deviceId = getDeviceId(context)

                // Restore linked session
                _linkedSessionId.value = getLinkedSession(context)

                // 1. Register device info in Firestore
                val simHelper = SimManagerHelper
                val activeSims = simHelper.getActiveSimCards(context)
                val defaultSubId = PreferenceRepository(context).getDefaultSimSubscriptionId()
                val activeSim = activeSims.firstOrNull { it.subscriptionId == defaultSubId } ?: activeSims.firstOrNull()

                val batteryPct = getBatteryLevel(context)
                val isCharging = getIsCharging(context)
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                val osVersion = "Android ${Build.VERSION.RELEASE}"
                val carrierName = activeSim?.carrierName ?: "JIO – Jio"
                val slotNum = (activeSim?.slotIndex ?: 0) + 1
                val phoneNumber = activeSim?.phoneNumber?.takeIf { it.isNotBlank() } ?: ""

                val infoMap = mapOf(
                    "model" to deviceModel,
                    "deviceModel" to deviceModel,
                    "manufacturer" to Build.MANUFACTURER,
                    "brand" to Build.BRAND,
                    "osVersion" to osVersion,
                    "androidVersion" to osVersion,
                    "sdkInt" to Build.VERSION.SDK_INT,
                    "carrier" to "SIM $slotNum ($carrierName)",
                    "simCarrier" to carrierName,
                    "simSlot" to slotNum,
                    "phoneNumber" to phoneNumber,
                    "simSubId" to (activeSim?.subscriptionId ?: -1),
                    "battery" to batteryPct,
                    "batteryLevel" to batteryPct,
                    "isCharging" to isCharging,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "status" to "online"
                )

                firestore.collection("devices").document(deviceId)
                    .set(infoMap, SetOptions.merge())

                val sessId = _linkedSessionId.value ?: getLinkedSession(context)
                if (!sessId.isNullOrBlank()) {
                    firestore.collection("sessions").document(sessId)
                        .set(infoMap, SetOptions.merge())
                }

                startHeartbeat(context, deviceId)
                syncContactsToFirestore(context, deviceId)

                // 2. Register FCM push token for background waking
                try {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            firestore.collection("devices").document(deviceId)
                                .update("fcmToken", token)
                        }
                    }
                } catch (_: Exception) {}

                // 3. Listen to incoming message queue in Firestore via snapshot listener
                queueListener?.remove()
                val queueRef = firestore.collection("devices").document(deviceId).collection("outgoing_queue")

                queueListener = queueRef.addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Queue listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    _isOnline.value = true

                    val pendingDocs = snapshots?.documents ?: emptyList()
                    for (doc in pendingDocs) {
                        val messageId = doc.id
                        if (inFlightMessageIds.contains(messageId)) continue
                        val status = doc.getString("status") ?: "PENDING"
                        if (!status.equals("PENDING", ignoreCase = true)) continue

                        val recipient = doc.getString("recipient") ?: continue
                        val message = doc.getString("message") ?: continue
                        val requestedSlot = doc.getLong("simSlot")?.toInt()

                        inFlightMessageIds.add(messageId)
                        processIncomingFirestoreSms(context, deviceId, messageId, recipient, message, requestedSlot)
                    }
                }

                // 4. Real-time History sync listener: syncs Firestore history directly to SQLite Room DB
                historyListener?.remove()
                val historyRef = firestore.collection("devices").document(deviceId).collection("history")
                historyListener = historyRef.addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    scope.launch {
                        try {
                            val historyRepo = HistoryRepository(context)

                            if (snapshot == null || snapshot.isEmpty) {
                                historyRepo.clearAllHistory()
                                _historyUpdateTrigger.emit(Unit)
                                return@launch
                            }

                            val currentLocalHistory = historyRepo.getAllHistory()

                            snapshot.documents.forEach { doc ->
                                val recipient = doc.getString("recipient") ?: return@forEach
                                val msg = doc.getString("message") ?: ""
                                val isSent = (doc.getString("status") ?: "SENT") == "SENT"
                                val ts = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                                val fullMsg = if (doc.getString("source") == "WEB" && !msg.startsWith("[Web Remote]")) "[Web Remote] $msg" else msg

                                val exists = currentLocalHistory.any { local ->
                                    val msgMatch = (local.message == fullMsg || local.message == msg || local.message == "[Web Remote] $msg")
                                    val recMatch = local.recipients.any { it.phoneNumber == recipient }
                                    val timeMatch = kotlin.math.abs(local.timestamp - ts) < 120_000L
                                    msgMatch && recMatch && timeMatch
                                }

                                if (!exists) {
                                    val sim = doc.getString("sim") ?: "SIM"
                                    val localRec = DispatchHistory(
                                        timestamp = ts,
                                        message = fullMsg,
                                        simCarrier = sim,
                                        simSlot = 0,
                                        totalCount = 1,
                                        successCount = if (isSent) 1 else 0,
                                        failureCount = if (isSent) 0 else 1,
                                        recipients = listOf(
                                            RecipientHistoryRecord(
                                                name = doc.getString("recipientName") ?: "Web Recipient",
                                                phoneNumber = recipient,
                                                status = if (isSent) SendStatus.SENT else SendStatus.FAILED,
                                                errorMessage = doc.getString("error")
                                            )
                                        )
                                    )
                                    historyRepo.insertHistory(localRec)
                                }
                            }
                            _historyUpdateTrigger.emit(Unit)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing Firestore history", e)
                            _historyUpdateTrigger.emit(Unit)
                        }
                    }
                }

                // 5. Real-time Templates sync listener: syncs templates saved from Web
                templatesListener?.remove()
                val templatesRef = firestore.collection("devices").document(deviceId).collection("sync").document("templates")
                templatesListener = templatesRef.addSnapshotListener { doc: DocumentSnapshot?, error: FirebaseFirestoreException? ->
                    if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                    scope.launch {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val rawList = doc.get("templates") as? List<Map<String, Any>> ?: return@launch
                            val repo = TemplateRepository(context)
                            val templates = rawList.mapNotNull { m ->
                                val id = m["id"] as? String ?: return@mapNotNull null
                                val title = m["title"] as? String ?: "Template"
                                val content = (m["content"] ?: m["text"]) as? String ?: ""
                                val category = m["category"] as? String ?: "Custom"
                                MessageTemplate(id = id, title = title, content = content, category = category)
                            }
                            repo.upsertTemplates(templates)
                            _templatesUpdateTrigger.emit(Unit)
                        } catch (_: Exception) {}
                    }
                }

                // 6. Real-time Contacts sync listener: updates contacts if edited on Web
                contactsSyncListener?.remove()
                val contactsRef = firestore.collection("devices").document(deviceId).collection("sync").document("contacts")
                contactsSyncListener = contactsRef.addSnapshotListener { doc: DocumentSnapshot?, error: FirebaseFirestoreException? ->
                    if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                    scope.launch {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val rawList = doc.get("contacts") as? List<Map<String, Any>> ?: return@launch
                            val customContacts = rawList.mapNotNull { m ->
                                val name = (m["name"] as? String)?.trim() ?: return@mapNotNull null
                                val phone = (m["phone"] as? String)?.trim() ?: return@mapNotNull null
                                val id = (m["id"] as? String) ?: "contact_${phone.filter { it.isDigit() }}"
                                com.simplesms.app.data.model.ContactItem(id = id, name = name, phoneNumber = phone)
                            }
                            ContactRepository(context).upsertCustomContacts(customContacts)
                            _contactsUpdateTrigger.emit(Unit)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing custom contacts from Firestore", e)
                        }
                    }
                }

                // 7. Real-time Device doc listener: updates connection/unlink status from Web
                deviceDocListener?.remove()
                val devDocRef = firestore.collection("devices").document(deviceId)
                deviceDocListener = devDocRef.addSnapshotListener { doc: DocumentSnapshot?, error: FirebaseFirestoreException? ->
                    if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                    val status = doc.getString("status") ?: "offline"
                    val linkedSess = doc.getString("linkedSession")
                    val online = (status == "online" || status == "PAIRED")
                    _isOnline.value = online
                    if (!online || linkedSess.isNullOrBlank()) {
                        _linkedSessionId.value = null
                    } else {
                        _linkedSessionId.value = linkedSess
                    }
                }

                _isOnline.value = true
                WebGatewayService.start(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Firestore Gateway", e)
                _isOnline.value = false
            }
        }
    }

    fun stopGateway(context: Context) {
        heartbeatJob?.cancel()
        heartbeatJob = null
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val firestore = FirebaseFirestore.getInstance()
                    val deviceId = getDeviceId(context)
                    firestore.collection("devices").document(deviceId)
                        .update("status", "offline", "lastSeen", FieldValue.serverTimestamp())
                }
            } catch (_: Exception) {}

            queueListener?.remove()
            queueListener = null
            historyListener?.remove()
            historyListener = null
            templatesListener?.remove()
            templatesListener = null
            contactsSyncListener?.remove()
            contactsSyncListener = null
            deviceDocListener?.remove()
            deviceDocListener = null
            _isOnline.value = false
            WebGatewayService.stop(context)
        }
    }

    /**
     * Called when the user scans the QR code shown on the Web Portal laptop/PC monitor.
     * Binds the phone to that Web Portal session in Firestore.
     */
    fun pairWithScannedQr(
        context: Context,
        qrRawValue: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sessionId = parseSessionId(qrRawValue)
        if (sessionId.isBlank()) {
            onError("Invalid pairing code. Please enter the 10-digit code shown on the Web Portal (e.g. K7M9P-2R4X8) or scan the QR code.")
            return
        }

        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val deviceId = getDeviceId(context)

                val simHelper = SimManagerHelper
                val activeSims = simHelper.getActiveSimCards(context)
                val defaultSubId = PreferenceRepository(context).getDefaultSimSubscriptionId()
                val activeSim = activeSims.firstOrNull { it.subscriptionId == defaultSubId } ?: activeSims.firstOrNull()

                val batteryPct = getBatteryLevel(context)
                val isCharging = getIsCharging(context)
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                val osVersion = "Android ${Build.VERSION.RELEASE}"
                val carrierName = activeSim?.carrierName ?: "JIO – Jio"
                val slotNum = (activeSim?.slotIndex ?: 0) + 1
                val phoneNumber = activeSim?.phoneNumber?.takeIf { it.isNotBlank() } ?: ""

                val sessionData = mapOf(
                    "status" to "PAIRED",
                    "deviceId" to deviceId,
                    "deviceModel" to deviceModel,
                    "model" to deviceModel,
                    "manufacturer" to Build.MANUFACTURER,
                    "brand" to Build.BRAND,
                    "androidVersion" to osVersion,
                    "osVersion" to osVersion,
                    "sdkInt" to Build.VERSION.SDK_INT,
                    "carrier" to "SIM $slotNum ($carrierName)",
                    "simCarrier" to carrierName,
                    "simSlot" to slotNum,
                    "phoneNumber" to phoneNumber,
                    "simSubId" to (activeSim?.subscriptionId ?: -1),
                    "battery" to batteryPct,
                    "batteryLevel" to batteryPct,
                    "isCharging" to isCharging,
                    "pairedAt" to FieldValue.serverTimestamp(),
                    "lastSeen" to FieldValue.serverTimestamp()
                )

                // Write pairing to Firestore
                firestore.collection("sessions").document(sessionId)
                    .set(sessionData, SetOptions.merge())
                    .addOnSuccessListener {
                        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
                        val prefs = getPrefs(context)
                        prefs.edit()
                            .putString(KEY_LINKED_SESSION, sessionId)
                            .putString(KEY_CONNECTED_TIME, nowFormatted)
                            .apply()
                        _linkedSessionId.value = sessionId
                        _connectedTime.value = nowFormatted
                        _isOnline.value = true

                        // Try to pull browser and platform from session if available
                        firestore.collection("sessions").document(sessionId).get()
                            .addOnSuccessListener { doc ->
                                val browser = doc.getString("browser") ?: "Chrome 120.0.6099.71"
                                val platform = doc.getString("platform") ?: "Windows 11"
                                prefs.edit()
                                    .putString(KEY_DEVICE_BROWSER, browser)
                                    .putString(KEY_DEVICE_PLATFORM, platform)
                                    .apply()
                                _deviceBrowser.value = browser
                                _devicePlatform.value = platform
                            }

                        startGateway(context)
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError("Pairing failed: ${e.localizedMessage ?: e.message}")
                    }
            } catch (e: Exception) {
                onError("Pairing exception: ${e.message}")
            }
        }
    }

    fun unlinkSession(context: Context) {
        val sessionId = _linkedSessionId.value ?: getLinkedSession(context)
        val deviceId = getDeviceId(context)
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                if (!sessionId.isNullOrBlank()) {
                    firestore.collection("sessions").document(sessionId)
                        .update("status", "DISCONNECTED")
                }
                firestore.collection("devices").document(deviceId)
                    .update("status", "offline", "linkedSession", null)
            } catch (_: Exception) {}
        }
        getPrefs(context).edit().remove(KEY_LINKED_SESSION).apply()
        _linkedSessionId.value = null
        _isOnline.value = false
    }

    private fun parseSessionId(qrContent: String): String {
        val trimmed = qrContent.trim()
        if (trimmed.startsWith("simplesms://pair?session=")) {
            val raw = trimmed.substringAfter("session=").substringBefore("&")
            return raw.replace("-", "").replace(" ", "").trim().uppercase()
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = JSONObject(trimmed)
                val id = json.optString("sessionId", json.optString("session", ""))
                if (id.isNotBlank()) return id.replace("-", "").replace(" ", "").trim().uppercase()
            } catch (_: Exception) {}
        }
        val clean = trimmed.replace("-", "").replace(" ", "").trim()
        if (clean.startsWith("sess_")) return clean
        if (clean.length == 10 && clean.all { it.isLetterOrDigit() }) {
            return clean.uppercase()
        }
        if (clean.isNotBlank() && clean.length in 4..64) {
            return clean.uppercase()
        }
        return ""
    }

    /**
     * Dispatches an incoming SMS received from the Web Portal via Cloud Firestore.
     */
    fun processIncomingFirestoreSms(
        context: Context,
        deviceId: String,
        messageId: String,
        recipient: String,
        message: String,
        requestedSimSlot: Int? = null
    ) {
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val queueDoc = firestore.collection("devices").document(deviceId)
                    .collection("outgoing_queue").document(messageId)
                val historyDoc = firestore.collection("devices").document(deviceId)
                    .collection("history").document(messageId)

                try { queueDoc.update("status", "TRANSMITTING") } catch (_: Exception) {}

                // Resolve active SIM (match requestedSimSlot if provided, else user's default SIM)
                val prefRepo = PreferenceRepository(context)
                val activeSims = SimManagerHelper.getActiveSimCards(context)
                val simInfo = if (requestedSimSlot != null && requestedSimSlot in 1..2) {
                    activeSims.firstOrNull { it.slotIndex == (requestedSimSlot - 1) } ?: activeSims.firstOrNull()
                } else {
                    var subId = prefRepo.getDefaultSimSubscriptionId()
                        ?: activeSims.firstOrNull()?.subscriptionId
                        ?: -1

                    if (subId == -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        subId = android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId()
                    }

                    activeSims.firstOrNull { it.subscriptionId == subId } ?: activeSims.firstOrNull()
                }

                val finalSubId = simInfo?.subscriptionId ?: -1
                val simLabel = simInfo?.let { "SIM ${it.slotIndex + 1} (${it.carrierName})" } ?: "SIM"

                // Dispatch via hardware baseband
                val smsResult = SmsDispatcher.sendSms(
                    context = context,
                    destinationAddress = recipient,
                    messageText = message,
                    subscriptionId = finalSubId
                )

                val now = System.currentTimeMillis()
                when (smsResult) {
                    is SmsResult.Success -> {
                        _lastMessageStatus.value = "Sent SMS to $recipient from Web"

                        val historyRecord = mapOf(
                            "id" to messageId,
                            "recipient" to recipient,
                            "recipientName" to "Web Recipient",
                            "message" to message,
                            "status" to "SENT",
                            "timestamp" to FieldValue.serverTimestamp(),
                            "sim" to simLabel,
                            "source" to "WEB"
                        )
                        historyDoc.set(historyRecord)
                        try { queueDoc.delete() } catch (_: Exception) {}

                        // Record to local SQLite History
                        val localRecord = DispatchHistory(
                            timestamp = now,
                            message = "[Web Remote] $message",
                            simCarrier = simInfo?.carrierName ?: "SIM",
                            simSlot = simInfo?.slotIndex ?: 0,
                            totalCount = 1,
                            successCount = 1,
                            failureCount = 0,
                            recipients = listOf(
                                RecipientHistoryRecord(
                                    name = "Web Recipient",
                                    phoneNumber = recipient,
                                    status = SendStatus.SENT
                                )
                            )
                        )
                        HistoryRepository(context).insertHistory(localRecord)
                        _historyUpdateTrigger.emit(Unit)
                    }
                    is SmsResult.Error -> {
                        _lastMessageStatus.value = "Failed SMS to $recipient: ${smsResult.reason}"

                        val failRecord = mapOf(
                            "id" to messageId,
                            "recipient" to recipient,
                            "recipientName" to "Web Recipient",
                            "message" to message,
                            "status" to "FAILED",
                            "error" to smsResult.reason,
                            "timestamp" to FieldValue.serverTimestamp(),
                            "sim" to simLabel,
                            "source" to "WEB"
                        )
                        historyDoc.set(failRecord)
                        try { queueDoc.delete() } catch (_: Exception) {}

                        val localRecord = DispatchHistory(
                            timestamp = now,
                            message = "[Web Remote] $message",
                            simCarrier = simInfo?.carrierName ?: "SIM",
                            simSlot = simInfo?.slotIndex ?: 0,
                            totalCount = 1,
                            successCount = 0,
                            failureCount = 1,
                            recipients = listOf(
                                RecipientHistoryRecord(
                                    name = "Web Recipient",
                                    phoneNumber = recipient,
                                    status = SendStatus.FAILED,
                                    errorMessage = smsResult.reason
                                )
                            )
                        )
                        HistoryRepository(context).insertHistory(localRecord)
                        _historyUpdateTrigger.emit(Unit)
                    }
                    is SmsResult.Progress -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process incoming web SMS", e)
            } finally {
                inFlightMessageIds.remove(messageId)
            }
        }
    }

    fun syncContactsToFirestore(context: Context, deviceId: String) {
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) return@launch
                val contactRepo = ContactRepository(context)
                val contacts = contactRepo.getContacts("")
                val firestore = FirebaseFirestore.getInstance()

                val contactsMapList = contacts.map { contact ->
                    mapOf(
                        "id" to contact.id,
                        "name" to contact.name,
                        "phone" to contact.phoneNumber,
                        "group" to "Phone Contacts"
                    )
                }

                val syncData = mapOf(
                    "contacts" to contactsMapList,
                    "totalCount" to contactsMapList.size,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                firestore.collection("devices").document(deviceId)
                    .collection("sync").document("contacts")
                    .set(syncData, SetOptions.merge())

                firestore.collection("devices").document(deviceId)
                    .update("contactsCount", contactsMapList.size)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync contacts to Firestore", e)
            }
        }
    }

    fun recordDispatchToFirestore(context: Context, history: DispatchHistory) {
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) return@launch
                val firestore = FirebaseFirestore.getInstance()
                val deviceId = getDeviceId(context)
                val simLabel = "SIM ${history.simSlot + 1} (${history.simCarrier})"

                history.recipients.forEach { rec ->
                    val messageId = "app_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                    val historyRecord = mapOf(
                        "id" to messageId,
                        "recipient" to rec.phoneNumber,
                        "recipientName" to rec.name,
                        "message" to history.message,
                        "status" to (if (rec.status == SendStatus.SENT) "SENT" else "FAILED"),
                        "error" to rec.errorMessage,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "sim" to simLabel,
                        "source" to "ANDROID_APP"
                    )
                    firestore.collection("devices").document(deviceId)
                        .collection("history").document(messageId)
                        .set(historyRecord)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record dispatch to Firestore", e)
            }
        }
    }

    fun syncTemplatesToFirestore(context: Context, deviceId: String) {
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) return@launch
                val firestore = FirebaseFirestore.getInstance()
                val templates = TemplateRepository(context).getAllTemplates()
                val list = templates.map {
                    mapOf(
                        "id" to it.id,
                        "title" to it.title,
                        "category" to it.category,
                        "text" to it.content,
                        "content" to it.content
                    )
                }
                firestore.collection("devices").document(deviceId)
                    .collection("sync").document("templates")
                    .set(
                        mapOf(
                            "templates" to list,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync templates to Firestore", e)
            }
        }
    }

    fun clearFirestoreHistory(context: Context, deviceId: String) {
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) return@launch
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("devices").document(deviceId).collection("history")
                    .get()
                    .addOnSuccessListener { snap ->
                        val batch = firestore.batch()
                        snap.documents.forEach { doc ->
                            batch.delete(doc.reference)
                        }
                        batch.commit()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear Firestore history", e)
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
