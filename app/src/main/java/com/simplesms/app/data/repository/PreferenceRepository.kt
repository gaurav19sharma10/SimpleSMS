package com.simplesms.app.data.repository

import android.content.Context
import android.content.SharedPreferences

class PreferenceRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultSimSubscriptionId(): Int? {
        val subId = prefs.getInt(KEY_DEFAULT_SIM_SUB_ID, -1)
        return if (subId != -1) subId else null
    }

    fun setDefaultSim(subscriptionId: Int, slotIndex: Int) {
        prefs.edit()
            .putInt(KEY_DEFAULT_SIM_SUB_ID, subscriptionId)
            .putInt(KEY_DEFAULT_SIM_SLOT, slotIndex)
            .apply()
    }

    fun getDefaultSimSlot(): Int? {
        val slot = prefs.getInt(KEY_DEFAULT_SIM_SLOT, -1)
        return if (slot != -1) slot else null
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getThrottleDelayMs(): Long {
        return prefs.getLong(KEY_THROTTLE_DELAY_MS, 1500L)
    }

    fun setThrottleDelayMs(delayMs: Long) {
        prefs.edit().putLong(KEY_THROTTLE_DELAY_MS, delayMs).apply()
    }

    fun getDraftMessage(): String {
        return prefs.getString(KEY_DRAFT_MESSAGE, "") ?: ""
    }

    fun saveDraftMessage(message: String) {
        prefs.edit().putString(KEY_DRAFT_MESSAGE, message).apply()
    }

    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun getDarkIntensity(): Float {
        return prefs.getFloat(KEY_DARK_INTENSITY, 0.85f)
    }

    fun setDarkIntensity(intensity: Float) {
        prefs.edit().putFloat(KEY_DARK_INTENSITY, intensity.coerceIn(0f, 1f)).apply()
    }

    // --- SIM & Sending Configurations ---
    fun getMessagesPerMinute(): Int = prefs.getInt(KEY_MESSAGES_PER_MIN, 12)
    fun setMessagesPerMinute(speed: Int) {
        prefs.edit().putInt(KEY_MESSAGES_PER_MIN, speed).apply()
        val delay = if (speed > 0) (60_000L / speed) else 3000L
        setThrottleDelayMs(delay)
    }

    fun isDelayBetweenMessagesEnabled(): Boolean = prefs.getBoolean(KEY_DELAY_ENABLED, true)
    fun setDelayBetweenMessagesEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_DELAY_ENABLED, enabled).apply()

    fun getDelayMinSeconds(): Int = prefs.getInt(KEY_DELAY_MIN_SEC, 3)
    fun setDelayMinSeconds(sec: Int) = prefs.edit().putInt(KEY_DELAY_MIN_SEC, sec).apply()

    fun getDelayMaxSeconds(): Int = prefs.getInt(KEY_DELAY_MAX_SEC, 6)
    fun setDelayMaxSeconds(sec: Int) = prefs.edit().putInt(KEY_DELAY_MAX_SEC, sec).apply()

    fun isRetryOnFailureEnabled(): Boolean = prefs.getBoolean(KEY_RETRY_ENABLED, true)
    fun setRetryOnFailureEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_RETRY_ENABLED, enabled).apply()

    fun getRetryCount(): Int = prefs.getInt(KEY_RETRY_COUNT, 3)
    fun setRetryCount(count: Int) = prefs.edit().putInt(KEY_RETRY_COUNT, count).apply()

    fun isSmartSendingEnabled(): Boolean = prefs.getBoolean(KEY_SMART_SENDING, true)
    fun setSmartSendingEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SMART_SENDING, enabled).apply()

    fun isDeliveryReportsEnabled(): Boolean = prefs.getBoolean(KEY_DELIVERY_REPORTS, true)
    fun setDeliveryReportsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_DELIVERY_REPORTS, enabled).apply()

    // --- App Preferences Configurations ---
    fun getAppTheme(): String = prefs.getString(KEY_APP_THEME, "Light") ?: "Light"
    fun setAppTheme(theme: String) = prefs.edit().putString(KEY_APP_THEME, theme).apply()

    fun getAccentColorHex(): String = prefs.getString(KEY_ACCENT_COLOR, "#0057FF") ?: "#0057FF"
    fun setAccentColorHex(hex: String) = prefs.edit().putString(KEY_ACCENT_COLOR, hex).apply()

    fun getAppLanguage(): String = prefs.getString(KEY_APP_LANGUAGE, "English") ?: "English"
    fun setAppLanguage(lang: String) = prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()

    fun isInAppNotifsEnabled(): Boolean = prefs.getBoolean(KEY_IN_APP_NOTIFS, true)
    fun setInAppNotifsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_IN_APP_NOTIFS, enabled).apply()

    fun isSoundAlertsEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ALERTS, true)
    fun setSoundAlertsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SOUND_ALERTS, enabled).apply()

    fun isAutoLockEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LOCK, false)
    fun setAutoLockEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_LOCK, enabled).apply()

    fun getDefaultStartTab(): String = prefs.getString(KEY_DEFAULT_TAB, "Home") ?: "Home"
    fun setDefaultStartTab(tab: String) = prefs.edit().putString(KEY_DEFAULT_TAB, tab).apply()

    companion object {
        private const val PREFS_NAME = "simple_sms_prefs"
        private const val KEY_DEFAULT_SIM_SUB_ID = "default_sim_sub_id"
        private const val KEY_DEFAULT_SIM_SLOT = "default_sim_slot"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_THROTTLE_DELAY_MS = "throttle_delay_ms"
        private const val KEY_DRAFT_MESSAGE = "draft_message"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
        private const val KEY_DARK_INTENSITY = "dark_intensity"

        private const val KEY_MESSAGES_PER_MIN = "settings_messages_per_min"
        private const val KEY_DELAY_ENABLED = "settings_delay_enabled"
        private const val KEY_DELAY_MIN_SEC = "settings_delay_min_sec"
        private const val KEY_DELAY_MAX_SEC = "settings_delay_max_sec"
        private const val KEY_RETRY_ENABLED = "settings_retry_enabled"
        private const val KEY_RETRY_COUNT = "settings_retry_count"
        private const val KEY_SMART_SENDING = "settings_smart_sending"
        private const val KEY_DELIVERY_REPORTS = "settings_delivery_reports"

        private const val KEY_APP_THEME = "settings_app_theme"
        private const val KEY_ACCENT_COLOR = "settings_accent_color"
        private const val KEY_APP_LANGUAGE = "settings_app_language"
        private const val KEY_IN_APP_NOTIFS = "settings_in_app_notifs"
        private const val KEY_SOUND_ALERTS = "settings_sound_alerts"
        private const val KEY_AUTO_LOCK = "settings_auto_lock"
        private const val KEY_DEFAULT_TAB = "settings_default_tab"
    }
}
