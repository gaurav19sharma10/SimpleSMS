package com.simplesms.app.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.simplesms.app.data.model.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ContactRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCustomContacts(): List<ContactItem> {
        val json = prefs.getString(KEY_CUSTOM_CONTACTS, "[]") ?: "[]"
        val list = mutableListOf<ContactItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ContactItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        phoneNumber = obj.getString("phoneNumber")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveCustomContact(name: String, phone: String): ContactItem {
        val cleanPhone = phone.replace("[\\s\\-\\(\\)]".toRegex(), "")
        val id = "contact_${cleanPhone}_${System.currentTimeMillis()}"
        val contact = ContactItem(
            id = id,
            name = name.trim().ifBlank { cleanPhone },
            phoneNumber = cleanPhone
        )
        val existing = getCustomContacts().filter { it.phoneNumber != cleanPhone }.toMutableList()
        existing.add(0, contact)
        saveCustomContactsList(existing)
        return contact
    }

    fun upsertCustomContacts(contacts: List<ContactItem>) {
        val existing = getCustomContacts().toMutableList()
        val existingPhones = existing.map { it.phoneNumber }.toMutableSet()
        var changed = false
        contacts.forEach { c ->
            val cleanPhone = c.phoneNumber.replace("[\\s\\-\\(\\)]".toRegex(), "")
            if (cleanPhone.isNotBlank() && !existingPhones.contains(cleanPhone)) {
                existing.add(c.copy(phoneNumber = cleanPhone))
                existingPhones.add(cleanPhone)
                changed = true
            }
        }
        if (changed) {
            saveCustomContactsList(existing)
        }
    }

    fun deleteCustomContact(idOrPhone: String) {
        val clean = idOrPhone.replace("[\\s\\-\\(\\)]".toRegex(), "")
        val filtered = getCustomContacts().filter { it.id != idOrPhone && it.phoneNumber != clean }
        saveCustomContactsList(filtered)
    }

    private fun saveCustomContactsList(contacts: List<ContactItem>) {
        val arr = JSONArray()
        contacts.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phoneNumber", c.phoneNumber)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_CONTACTS, arr.toString()).apply()
    }

    suspend fun getContacts(searchQuery: String = ""): List<ContactItem> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<ContactItem>()
        val seenNumbers = mutableSetOf<String>()

        // 1. Include custom / Web-synced contacts
        val customContacts = getCustomContacts()
        customContacts.forEach {
            val cleanNumber = it.phoneNumber.replace("[\\s\\-\\(\\)]".toRegex(), "")
            if (cleanNumber.isNotBlank() && seenNumbers.add(cleanNumber)) {
                contactsList.add(it.copy(phoneNumber = cleanNumber))
            }
        }

        // 2. Include native Android contacts from ContentResolver
        if (SimManagerHelper.hasContactsPermission(context)) {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            val selection = if (searchQuery.isNotBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            } else null

            val selectionArgs = if (searchQuery.isNotBlank()) {
                val queryWildcard = "%$searchQuery%"
                arrayOf(queryWildcard, queryWildcard)
            } else null

            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"

            try {
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                    val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                    val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (it.moveToNext()) {
                        val id = if (idIdx != -1) it.getString(idIdx) else ""
                        val name = if (nameIdx != -1) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                        val rawNumber = if (numberIdx != -1) it.getString(numberIdx) ?: "" else ""

                        val cleanNumber = rawNumber.replace("[\\s\\-\\(\\)]".toRegex(), "")
                        if (cleanNumber.isNotBlank() && seenNumbers.add(cleanNumber)) {
                            contactsList.add(
                                ContactItem(
                                    id = id,
                                    name = name.trim(),
                                    phoneNumber = cleanNumber
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Graceful fallback on security exception or cursor error
            }
        }

        // 3. Search and ordering
        val result = if (searchQuery.isNotBlank()) {
            val queryLower = searchQuery.lowercase()
            contactsList.filter {
                it.name.lowercase().contains(queryLower) || it.phoneNumber.contains(queryLower)
            }
        } else {
            contactsList
        }

        result.sortedBy { it.name.lowercase() }
    }

    companion object {
        private const val PREFS_NAME = "simple_sms_contacts"
        private const val KEY_CUSTOM_CONTACTS = "custom_contacts"
    }
}
