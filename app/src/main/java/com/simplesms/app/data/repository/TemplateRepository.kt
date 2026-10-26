package com.simplesms.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.simplesms.app.data.model.MessageTemplate
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TemplateRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val defaultTemplates = listOf(
        MessageTemplate(
            id = "default_1",
            title = "Friendly Greeting",
            category = "Greetings",
            content = "Hello {name}, hope you are having a wonderful day!"
        ),
        MessageTemplate(
            id = "default_2",
            title = "Meeting Reminder",
            category = "Reminders",
            content = "Hi {name}, this is a quick reminder for our scheduled meeting today. Looking forward to speaking with you!"
        ),
        MessageTemplate(
            id = "default_3",
            title = "Payment Follow-up",
            category = "Business",
            content = "Dear {name}, friendly reminder that payment is pending. Please complete the transfer at your convenience."
        ),
        MessageTemplate(
            id = "default_4",
            title = "Event Invitation",
            category = "Events",
            content = "Hi {name}, you are cordially invited to our upcoming celebration! We would love to have you with us."
        ),
        MessageTemplate(
            id = "default_5",
            title = "Urgent Notice",
            category = "Alerts",
            content = "Important alert for {name}: Please review the urgent documents sent to your email as soon as possible."
        ),
        MessageTemplate(
            id = "default_6",
            title = "Fee Reminder",
            category = "Finance",
            content = "Dear {name}, friendly reminder that your payment installment is due this week. Thank you."
        ),
        MessageTemplate(
            id = "default_7",
            title = "General Notice",
            category = "Announcements",
            content = "Notice for {name}: Please be informed about our scheduled maintenance window this weekend."
        ),
        MessageTemplate(
            id = "default_8",
            title = "Order Confirmation",
            category = "Updates",
            content = "Hi {name}, your order has been received and confirmed. We appreciate your business!"
        )
    )

    fun getAllTemplates(): List<MessageTemplate> {
        val list = mutableListOf<MessageTemplate>()
        list.addAll(defaultTemplates)

        val customJson = prefs.getString(KEY_CUSTOM_TEMPLATES, "[]") ?: "[]"
        try {
            val array = JSONArray(customJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MessageTemplate(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        category = obj.optString("category", "Custom"),
                        content = obj.getString("content")
                    )
                )
            }
        } catch (_: Exception) {}

        return list
    }

    fun addCustomTemplate(title: String, category: String, content: String): MessageTemplate {
        val template = MessageTemplate(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category.ifBlank { "Custom" },
            content = content
        )

        val existing = getCustomTemplates().toMutableList()
        existing.add(template)
        saveCustomTemplates(existing)
        return template
    }

    fun upsertTemplates(templates: List<MessageTemplate>) {
        val existing = getCustomTemplates().toMutableList()
        val existingIds = existing.map { it.id }.toSet()
        var changed = false
        templates.forEach { t ->
            if (!existingIds.contains(t.id)) {
                existing.add(t)
                changed = true
            }
        }
        if (changed) {
            saveCustomTemplates(existing)
        }
    }

    fun deleteTemplate(id: String) {
        val filtered = getCustomTemplates().filter { it.id != id }
        saveCustomTemplates(filtered)
    }

    private fun getCustomTemplates(): List<MessageTemplate> {
        val list = mutableListOf<MessageTemplate>()
        val customJson = prefs.getString(KEY_CUSTOM_TEMPLATES, "[]") ?: "[]"
        try {
            val array = JSONArray(customJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MessageTemplate(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        category = obj.optString("category", "Custom"),
                        content = obj.getString("content")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveCustomTemplates(list: List<MessageTemplate>) {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("category", it.category)
                put("content", it.content)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_TEMPLATES, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "simple_sms_templates"
        private const val KEY_CUSTOM_TEMPLATES = "custom_templates"
    }
}
