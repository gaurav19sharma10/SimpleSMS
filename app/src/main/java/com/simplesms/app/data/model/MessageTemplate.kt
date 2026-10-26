package com.simplesms.app.data.model

data class MessageTemplate(
    val id: String,
    val title: String,
    val category: String, // e.g. "Reminders", "Greetings", "Business", "Custom"
    val content: String
)
