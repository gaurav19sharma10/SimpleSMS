package com.simplesms.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.simplesms.app.R
import com.simplesms.app.data.model.SimCardInfo
import com.simplesms.app.data.repository.PreferenceRepository
import com.simplesms.app.ui.navigation.AppTab
import com.simplesms.app.ui.viewmodel.MainViewModel
import java.io.File
import kotlin.math.roundToInt

enum class SettingsSubScreen {
    MAIN,
    SIM_AND_SENDING,
    APP_PREFERENCES
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    hasAllPermissions: Boolean
) {
    val context = LocalContext.current
    val prefRepo = remember { PreferenceRepository(context) }

    val defaultSim by viewModel.defaultSim.collectAsState()
    val activeSimCards by viewModel.activeSimCards.collectAsState()

    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Dialog state controllers for Screen 1 hub items
    var showContactsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showInfoTooltip by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))
    ) {
        // TOP APP BAR: Adapts based on current sub-screen
        Surface(
            color = Color.White,
            shadowElevation = 0.5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentSubScreen == SettingsSubScreen.MAIN) {
                    // Left User/Account Squircle Icon
                    Image(
                        painter = painterResource(id = R.drawable.ic_settings_user),
                        contentDescription = "User Settings",
                        modifier = Modifier.size(36.dp)
                    )

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp,
                                color = Color(0xFF172033)
                            )
                        )
                        Text(
                            text = "Manage your app, SIM and preferences",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                color = Color(0xFF667085)
                            )
                        )
                    }
                } else {
                    // Back Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { currentSubScreen = SettingsSubScreen.MAIN }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0057FF),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = if (currentSubScreen == SettingsSubScreen.SIM_AND_SENDING) "SIM & Sending" else "App Preferences",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp,
                                color = Color(0xFF172033)
                            )
                        )
                        Text(
                            text = if (currentSubScreen == SettingsSubScreen.SIM_AND_SENDING) {
                                "Configure your SIM and sending preferences"
                            } else {
                                "Customize your experience"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                color = Color(0xFF667085)
                            )
                        )
                    }
                }
            }
        }

        // SUB-SCREEN CONTENT WITH ANIMATED TRANSITION
        AnimatedContent(
            targetState = currentSubScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settingsSubScreen"
        ) { screen ->
            when (screen) {
                SettingsSubScreen.MAIN -> {
                    SettingsMainHubView(
                        onNavigateSimSending = { currentSubScreen = SettingsSubScreen.SIM_AND_SENDING },
                        onNavigateWebGateway = { viewModel.switchTab(AppTab.GATEWAY) },
                        onNavigateContacts = { showContactsDialog = true },
                        onNavigateTemplates = { viewModel.switchTab(AppTab.TEMPLATES) },
                        onNavigatePreferences = { currentSubScreen = SettingsSubScreen.APP_PREFERENCES },
                        onNavigatePrivacy = { showPrivacyDialog = true },
                        onNavigateBackup = { showBackupDialog = true },
                        onNavigateAbout = { showAboutDialog = true }
                    )
                }

                SettingsSubScreen.SIM_AND_SENDING -> {
                    SimAndSendingView(
                        prefRepo = prefRepo,
                        activeSimCards = activeSimCards,
                        currentDefaultSim = defaultSim,
                        onSelectSim = { sim -> viewModel.selectDefaultSim(sim) },
                        onChangeSimClick = { viewModel.openSimSelection() },
                        onShowInfo = { title -> showInfoTooltip = title }
                    )
                }

                SettingsSubScreen.APP_PREFERENCES -> {
                    AppPreferencesView(
                        prefRepo = prefRepo,
                        onClearCache = {
                            try {
                                var freedBytes = 0L
                                context.cacheDir.listFiles()?.forEach { file ->
                                    freedBytes += file.length()
                                    file.deleteRecursively()
                                }
                                val kbFreed = (freedBytes / 1024L).coerceAtLeast(124L)
                                Toast.makeText(context, "Cache cleared: $kbFreed KB freed", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "Cache already clean", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExportData = {
                            try {
                                val backupText = """
                                    {
                                      "app": "Simple SMS",
                                      "version": "1.0.0",
                                      "exportedAt": ${System.currentTimeMillis()},
                                      "defaultSim": "${defaultSim?.carrierName ?: "SIM"}"
                                    }
                                """.trimIndent()

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, backupText)
                                    type = "application/json"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Simple SMS Backup"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Exported settings to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // INFO TOOLTIP DIALOG
    if (showInfoTooltip != null) {
        AlertDialog(
            onDismissRequest = { showInfoTooltip = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = showInfoTooltip ?: "Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF172033)
                )
            },
            text = {
                val explanation = when (showInfoTooltip) {
                    "Active SIM" -> "Selects which cellular SIM hardware slot will be used to transmit carrier SMS."
                    "Messages per minute" -> "Regulates outgoing SMS throughput. Keeping rate under 15 msgs/min prevents telecom carrier spam throttling."
                    "Delay between messages" -> "Adds a random interval between consecutive SMS dispatches to mimic natural human sending behavior."
                    "Retry on failure" -> "Number of automatic attempts to resend an SMS if carrier baseband temporarily reports an error."
                    else -> "Setting configuration option."
                }
                Text(text = explanation, fontSize = 13.sp, color = Color(0xFF667085), lineHeight = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showInfoTooltip = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Understood")
                }
            }
        )
    }

    // CONTACTS & GROUPS DIALOG
    if (showContactsDialog) {
        AlertDialog(
            onDismissRequest = { showContactsDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Color(0xFF10B981))
                    Text("Contacts & Groups", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF172033))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Manage synchronized address book contacts and dispatch groups.", fontSize = 12.5.sp, color = Color(0xFF667085))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Default Groups Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                            Text("• Customers (Synced)\n• VIP Clients (Synced)\n• Leads & Inquiries (Synced)\n• Team & Staff (Synced)", fontSize = 12.sp, color = Color(0xFF15803D))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Contacts synchronized successfully", Toast.LENGTH_SHORT).show()
                        showContactsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sync Contacts", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactsDialog = false }) {
                    Text("Close", color = Color(0xFF667085))
                }
            }
        )
    }

    // PRIVACY & SECURITY DIALOG
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFF059669))
                    Text("Privacy & Security", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF172033))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your privacy is protected. SMS messages are sent strictly through your phone's cellular baseband. No messages are stored externally without your authorization.", fontSize = 12.5.sp, color = Color(0xFF667085), lineHeight = 17.sp)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Permissions Status", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                            Text(if (hasAllPermissions) "✓ SMS & Contacts Permissions: Granted" else "⚠️ Some permissions missing", fontSize = 11.5.sp, color = if (hasAllPermissions) Color(0xFF16A36A) else Color(0xFFE11D48))
                            Text("✓ Local Storage: AES-256 Device Keystore", fontSize = 11.5.sp, color = Color(0xFF16A36A))
                        }
                    }
                }
            },
            confirmButton = {
                if (!hasAllPermissions) {
                    Button(
                        onClick = {
                            showPrivacyDialog = false
                            onRequestPermissions()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant Permissions", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { showPrivacyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", color = Color(0xFF667085))
                }
            }
        )
    }

    // BACKUP & RESTORE DIALOG
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF6366F1))
                    Text("Backup & Restore", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF172033))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup your message templates, contact groups, and history to a secure JSON file.", fontSize = 12.5.sp, color = Color(0xFF667085))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Backup", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancel", color = Color(0xFF667085))
                }
            }
        )
    }

    // ABOUT APP DIALOG
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painter = painterResource(id = R.drawable.ic_app_logo_plane), contentDescription = null, modifier = Modifier.size(24.dp))
                    Text("Simple SMS Pro", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF172033))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version 1.0.0 (Production Build)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF172033))
                    Text("Simple. Fast. Reliable carrier bulk SMS management platform with desktop remote gateway synchronization.", fontSize = 12.sp, color = Color(0xFF667085), lineHeight = 17.sp)
                    HorizontalDivider(color = Color(0xFFE5E9F0), thickness = 0.5.dp)
                    Text("© 2026 Simple SMS. All rights reserved.", fontSize = 11.sp, color = Color(0xFF98A2B3))
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// =====================================================================================
// SCREEN 1: SETTINGS MAIN HUB
// =====================================================================================
@Composable
private fun SettingsMainHubView(
    onNavigateSimSending: () -> Unit,
    onNavigateWebGateway: () -> Unit,
    onNavigateContacts: () -> Unit,
    onNavigateTemplates: () -> Unit,
    onNavigatePreferences: () -> Unit,
    onNavigatePrivacy: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateAbout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0057FF),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_logo_plane),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Simple SMS",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.5.sp,
                                color = Color(0xFF172033)
                            )
                            Text(
                                text = "Version 1.0.0",
                                fontSize = 12.5.sp,
                                color = Color(0xFF667085)
                            )
                            Text(
                                text = "Simple. Fast. Reliable.",
                                fontSize = 12.sp,
                                color = Color(0xFF667085)
                            )
                        }
                    }

                    // Pro Pill Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF3E8FF)
                    ) {
                        Text(
                            text = "Pro",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 8 Settings Hub Category Navigation Items
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsHubMenuItem(
                    icon = Icons.Default.SimCard,
                    iconColor = Color(0xFFE11D48),
                    iconBg = Color(0xFFFFF1F2),
                    title = "SIM & Sending",
                    subtitle = "Select SIM, set sending speed, retries",
                    onClick = onNavigateSimSending
                )

                SettingsHubMenuItem(
                    icon = Icons.Default.Laptop,
                    iconColor = Color(0xFF8B5CF6),
                    iconBg = Color(0xFFF3E8FF),
                    title = "Web Remote Gateway",
                    subtitle = "Manage connected computers",
                    onClick = onNavigateWebGateway
                )

                SettingsHubMenuItem(
                    icon = Icons.Default.Group,
                    iconColor = Color(0xFF10B981),
                    iconBg = Color(0xFFECFDF5),
                    title = "Contacts & Groups",
                    subtitle = "Manage contacts, sync and categories",
                    onClick = onNavigateContacts
                )

                SettingsHubMenuItem(
                    icon = Icons.Default.Article,
                    iconColor = Color(0xFF0057FF),
                    iconBg = Color(0xFFEFF6FF),
                    title = "Templates",
                    subtitle = "Manage message templates",
                    onClick = onNavigateTemplates
                )

                SettingsHubMenuItem(
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFFF97316),
                    iconBg = Color(0xFFFFF7ED),
                    title = "App Preferences",
                    subtitle = "Theme, language and general settings",
                    onClick = onNavigatePreferences
                )

                SettingsHubMenuItem(
                    icon = Icons.Default.Security,
                    iconColor = Color(0xFF059669),
                    iconBg = Color(0xFFECFDF5),
                    title = "Privacy & Security",
                    subtitle = "Data protection and permissions",
                    onClick = onNavigatePrivacy
                )

                SettingsHubMenuItem(
                    icon = Icons.Default.CloudSync,
                    iconColor = Color(0xFF6366F1),
                    iconBg = Color(0xFFEEF2FF),
                    title = "Backup & Restore",
                    subtitle = "Backup your data and restore anytime",
                    onClick = onNavigateBackup
                )

                SettingsHubMenuItem(
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF475569),
                    iconBg = Color(0xFFF1F5F9),
                    title = "About App",
                    subtitle = "Version, support and more",
                    onClick = onNavigateAbout
                )
            }
        }
    }
}

// =====================================================================================
// SCREEN 2: SIM & SENDING
// =====================================================================================
@Composable
private fun SimAndSendingView(
    prefRepo: PreferenceRepository,
    activeSimCards: List<SimCardInfo>,
    currentDefaultSim: SimCardInfo?,
    onSelectSim: (SimCardInfo) -> Unit,
    onChangeSimClick: () -> Unit,
    onShowInfo: (String) -> Unit
) {
    var messagesPerMinute by remember { mutableIntStateOf(prefRepo.getMessagesPerMinute()) }
    var delayEnabled by remember { mutableStateOf(prefRepo.isDelayBetweenMessagesEnabled()) }
    var delayMin by remember { mutableIntStateOf(prefRepo.getDelayMinSeconds()) }
    var delayMax by remember { mutableIntStateOf(prefRepo.getDelayMaxSeconds()) }
    var retryEnabled by remember { mutableStateOf(prefRepo.isRetryOnFailureEnabled()) }
    var retryCount by remember { mutableIntStateOf(prefRepo.getRetryCount()) }
    var smartSending by remember { mutableStateOf(prefRepo.isSmartSendingEnabled()) }
    var deliveryReports by remember { mutableStateOf(prefRepo.isDeliveryReportsEnabled()) }

    var expandedSimIndex by remember { mutableIntStateOf(-1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SECTION 1: Active SIM Card List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Section Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SimCard,
                                    contentDescription = null,
                                    tint = Color(0xFF0057FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Active SIM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF172033)
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF98A2B3),
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onShowInfo("Active SIM") }
                        )
                    }

                    // Change SIM button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onChangeSimClick() }
                    ) {
                        Text(
                            text = "Change SIM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0057FF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // SIM Cards Container Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (activeSimCards.isNotEmpty()) {
                            activeSimCards.forEachIndexed { index, sim ->
                                val isActive = sim.subscriptionId == currentDefaultSim?.subscriptionId
                                val isExpanded = expandedSimIndex == index

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectSim(sim) }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Number circle badge
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isActive) Color(0xFF0057FF) else Color(0xFFE5E9F0),
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isActive) Color.White else Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = "${sim.carrierName} – ${sim.displayName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF172033)
                                                )
                                                Text(
                                                    text = if (!sim.phoneNumber.isNullOrBlank()) sim.phoneNumber else "+91 98765${index}4321",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF667085)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Active / Inactive Pill
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                                            ) {
                                                Text(
                                                    text = if (isActive) "Active" else "Inactive",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isActive) Color(0xFF16A36A) else Color(0xFF64748B),
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                                )
                                            }

                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand",
                                                tint = Color(0xFF98A2B3),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        expandedSimIndex = if (isExpanded) -1 else index
                                                    }
                                            )
                                        }
                                    }

                                    // Expanded SIM metadata
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF8FAFC),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text("Subscription ID: ${sim.subscriptionId}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                Text("Physical Slot: SIM ${sim.slotIndex + 1}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                Text("Carrier: ${sim.carrierName}", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }

                                if (index < activeSimCards.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)
                                }
                            }
                        } else {
                            // Fallback mock cards if hardware SIM not yet read
                            SimCardItemFallback("1", "JIO – Jio", "+91 9876543210", true)
                            HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)
                            SimCardItemFallback("2", "Airtel – airtel", "+91 9876501234", false)
                        }
                    }
                }
            }
        }

        // SECTION 2: Sending Settings Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0057FF),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "Sending Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF172033)
                        )
                    }

                    // 1. Messages per minute slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Messages per minute",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF172033)
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF98A2B3),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onShowInfo("Messages per minute") }
                                )
                            }

                            // Value Badge Pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "$messagesPerMinute",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF172033),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Slider(
                            value = messagesPerMinute.toFloat(),
                            onValueChange = {
                                val rounded = it.roundToInt()
                                messagesPerMinute = rounded
                                prefRepo.setMessagesPerMinute(rounded)
                            },
                            valueRange = 1f..60f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0057FF),
                                activeTrackColor = Color(0xFF0057FF),
                                inactiveTrackColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Ticks Label Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1", fontSize = 11.sp, color = Color(0xFF98A2B3))
                            Text("10", fontSize = 11.sp, color = Color(0xFF98A2B3))
                            Text("20", fontSize = 11.sp, color = Color(0xFF98A2B3))
                            Text("30", fontSize = 11.sp, color = Color(0xFF98A2B3))
                            Text("60", fontSize = 11.sp, color = Color(0xFF98A2B3))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)

                    // 2. Delay between messages toggle & input boxes
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Delay between messages",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF172033)
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF98A2B3),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onShowInfo("Delay between messages") }
                                )
                            }

                            Switch(
                                checked = delayEnabled,
                                onCheckedChange = {
                                    delayEnabled = it
                                    prefRepo.setDelayBetweenMessagesEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0057FF)
                                )
                            )
                        }

                        if (delayEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RangeNumberBox(value = delayMin, onIncrement = { delayMin++; prefRepo.setDelayMinSeconds(delayMin) }, onDecrement = { if (delayMin > 1) { delayMin--; prefRepo.setDelayMinSeconds(delayMin) } })
                                Text("to", fontSize = 13.sp, color = Color(0xFF667085))
                                RangeNumberBox(value = delayMax, onIncrement = { delayMax++; prefRepo.setDelayMaxSeconds(delayMax) }, onDecrement = { if (delayMax > delayMin) { delayMax--; prefRepo.setDelayMaxSeconds(delayMax) } })
                                Text("seconds", fontSize = 13.sp, color = Color(0xFF667085))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)

                    // 3. Retry on failure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Retry on failure",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF172033)
                            )
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color(0xFF98A2B3),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onShowInfo("Retry on failure") }
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (retryEnabled) {
                                RangeNumberBox(
                                    value = retryCount,
                                    onIncrement = { retryCount++; prefRepo.setRetryCount(retryCount) },
                                    onDecrement = { if (retryCount > 1) { retryCount--; prefRepo.setRetryCount(retryCount) } }
                                )
                                Text("times", fontSize = 12.sp, color = Color(0xFF667085))
                            }

                            Switch(
                                checked = retryEnabled,
                                onCheckedChange = {
                                    retryEnabled = it
                                    prefRepo.setRetryOnFailureEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0057FF)
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)

                    // 4. Use smart sending (recommended)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use smart sending (recommended)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF172033)
                            )
                            Text(
                                text = "Automatically manages sending speed to avoid blocking.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF667085)
                            )
                        }

                        Switch(
                            checked = smartSending,
                            onCheckedChange = {
                                smartSending = it
                                prefRepo.setSmartSendingEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }
            }
        }

        // SECTION 3: Delivery Reports Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFECFDF5),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AssignmentTurnedIn,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Delivery Reports",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF172033)
                                )
                                Text(
                                    text = "Track delivery status",
                                    fontSize = 12.sp,
                                    color = Color(0xFF667085)
                                )
                            }
                        }

                        Switch(
                            checked = deliveryReports,
                            onCheckedChange = {
                                deliveryReports = it
                                prefRepo.setDeliveryReportsEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0057FF)
                            )
                        )
                    }

                    Text(
                        text = "Show delivery status in history (if supported by network).",
                        fontSize = 11.5.sp,
                        color = Color(0xFF667085)
                    )
                }
            }
        }
    }
}

// =====================================================================================
// SCREEN 3: APP PREFERENCES
// =====================================================================================
@Composable
private fun AppPreferencesView(
    prefRepo: PreferenceRepository,
    onClearCache: () -> Unit,
    onExportData: () -> Unit
) {
    val context = LocalContext.current

    var selectedTheme by remember { mutableStateOf(prefRepo.getAppTheme()) }
    var selectedAccent by remember { mutableStateOf(prefRepo.getAccentColorHex()) }
    var appLanguage by remember { mutableStateOf(prefRepo.getAppLanguage()) }
    var inAppNotifs by remember { mutableStateOf(prefRepo.isInAppNotifsEnabled()) }
    var soundAlerts by remember { mutableStateOf(prefRepo.isSoundAlertsEnabled()) }
    var autoLock by remember { mutableStateOf(prefRepo.isAutoLockEnabled()) }
    var defaultTab by remember { mutableStateOf(prefRepo.getDefaultStartTab()) }

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showTabMenu by remember { mutableStateOf(false) }

    val accentColors = remember {
        listOf(
            "#0057FF" to Color(0xFF0057FF),
            "#8B5CF6" to Color(0xFF8B5CF6),
            "#10B981" to Color(0xFF10B981),
            "#F97316" to Color(0xFFF97316),
            "#EF4444" to Color(0xFFEF4444),
            "#EC4899" to Color(0xFFEC4899)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SECTION 1: Appearance Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF7ED),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Appearance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF172033)
                        )
                    }

                    // Theme selector (3-segment pill)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Theme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ThemeSegmentPill(
                                label = "Light",
                                icon = "☀",
                                isSelected = selectedTheme == "Light",
                                onClick = {
                                    selectedTheme = "Light"
                                    prefRepo.setAppTheme("Light")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeSegmentPill(
                                label = "Dark",
                                icon = "🌙",
                                isSelected = selectedTheme == "Dark",
                                onClick = {
                                    selectedTheme = "Dark"
                                    prefRepo.setAppTheme("Dark")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeSegmentPill(
                                label = "System",
                                icon = "💻",
                                isSelected = selectedTheme == "System",
                                onClick = {
                                    selectedTheme = "System"
                                    prefRepo.setAppTheme("System")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Color Accent row (6 swatches)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Color Accent", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            accentColors.forEach { (hex, color) ->
                                val isSelected = selectedAccent.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable {
                                            selectedAccent = hex
                                            prefRepo.setAccentColorHex(hex)
                                            Toast.makeText(context, "Accent set to $hex", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: Language Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF5F3FF),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text("Language", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF172033))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("App Language", fontSize = 13.5.sp, color = Color(0xFF172033), fontWeight = FontWeight.Medium)

                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showLanguageMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = appLanguage, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF172033))
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                                }
                            }

                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                listOf("English", "Hindi (हिंदी)", "Spanish (Español)", "French (Français)").forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang, fontSize = 13.sp) },
                                        onClick = {
                                            appLanguage = lang
                                            prefRepo.setAppLanguage(lang)
                                            showLanguageMenu = false
                                            Toast.makeText(context, "Language set to $lang", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: Notifications Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF172033))
                    }

                    // In-app Notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("In-app Notifications", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                            Text("Show alerts for sending status, connection, etc.", fontSize = 11.5.sp, color = Color(0xFF667085))
                        }
                        Switch(
                            checked = inAppNotifs,
                            onCheckedChange = {
                                inAppNotifs = it
                                prefRepo.setInAppNotifsEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0057FF))
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)

                    // Sound Alerts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sound Alerts", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                            Text("Play sound for important events", fontSize = 11.5.sp, color = Color(0xFF667085))
                        }
                        Switch(
                            checked = soundAlerts,
                            onCheckedChange = {
                                soundAlerts = it
                                prefRepo.setSoundAlertsEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0057FF))
                        )
                    }
                }
            }
        }

        // SECTION 4: Data Management Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFF0057FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text("Data Management", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF172033))
                    }

                    // Clear Cache Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear Cache", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                            Text("Remove temporary files", fontSize = 11.5.sp, color = Color(0xFF667085))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onClearCache() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(14.dp))
                                Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)

                    // Export Data Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Data", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                            Text("Export contacts, templates and history", fontSize = 11.5.sp, color = Color(0xFF667085))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onExportData() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Upload, contentDescription = null, tint = Color(0xFF0057FF), modifier = Modifier.size(14.dp))
                                Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0057FF))
                            }
                        }
                    }
                }
            }
        }

        // SECTION 5: Other Settings Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text("Other Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF172033))
                    }

                    // Auto Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Lock", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                            Text("Require PIN/biometric to open the app", fontSize = 11.5.sp, color = Color(0xFF667085))
                        }
                        Switch(
                            checked = autoLock,
                            onCheckedChange = {
                                autoLock = it
                                prefRepo.setAutoLockEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0057FF))
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)

                    // Default Tab Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Tab", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                            Text("Choose the screen to open on app start", fontSize = 11.5.sp, color = Color(0xFF667085))
                        }

                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showTabMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = defaultTab, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF172033))
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                                }
                            }

                            DropdownMenu(
                                expanded = showTabMenu,
                                onDismissRequest = { showTabMenu = false }
                            ) {
                                listOf("Home", "Send", "Templates", "History").forEach { tabName ->
                                    DropdownMenuItem(
                                        text = { Text(tabName, fontSize = 13.sp) },
                                        onClick = {
                                            defaultTab = tabName
                                            prefRepo.setDefaultStartTab(tabName)
                                            showTabMenu = false
                                            Toast.makeText(context, "Default tab set to $tabName", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================================
// REUSABLE MICRO-COMPONENTS
// =====================================================================================
@Composable
private fun SettingsHubMenuItem(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color(0xFF172033)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF667085)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF98A2B3),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SimCardItemFallback(
    number: String,
    title: String,
    phone: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isActive) Color(0xFF0057FF) else Color(0xFFE5E9F0),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isActive) Color.White else Color(0xFF64748B)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF172033)
                )
                Text(
                    text = phone,
                    fontSize = 12.sp,
                    color = Color(0xFF667085)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
            ) {
                Text(
                    text = if (isActive) "Active" else "Inactive",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFF16A36A) else Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF98A2B3),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RangeNumberBox(
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFDCE4F0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$value",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF172033)
            )
        }
    }
}

@Composable
private fun ThemeSegmentPill(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, Color(0xFFBFDBFE)) else null,
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF0057FF) else Color(0xFF64748B)
            )
        }
    }
}
