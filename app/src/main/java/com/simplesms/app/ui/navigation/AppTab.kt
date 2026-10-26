package com.simplesms.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    COMPOSE("Send", Icons.AutoMirrored.Filled.Send),
    TEMPLATES("Templates", Icons.Default.Description),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings),
    GATEWAY("Web Remote", Icons.Default.Laptop)
}
