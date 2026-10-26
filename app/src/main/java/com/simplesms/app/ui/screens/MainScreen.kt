package com.simplesms.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplesms.app.R
import com.simplesms.app.ui.navigation.AppTab
import com.simplesms.app.ui.theme.BrandBackground
import com.simplesms.app.ui.theme.BrandBlue
import com.simplesms.app.ui.theme.BrandBlueLight
import com.simplesms.app.ui.theme.BrandBorder
import com.simplesms.app.ui.theme.BrandError
import com.simplesms.app.ui.theme.BrandErrorBg
import com.simplesms.app.ui.theme.BrandSurface
import com.simplesms.app.ui.theme.BrandTextPrimary
import com.simplesms.app.ui.theme.BrandTextSecondary
import com.simplesms.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    hasAllPermissions: Boolean
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val sendSmsStep by viewModel.sendSmsStep.collectAsState()
    val showProcessingScreen by viewModel.showProcessingScreen.collectAsState()
    val showSimSelectDialog by viewModel.showSimSelectDialog.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val defaultSim by viewModel.defaultSim.collectAsState()
    val activeSimCards by viewModel.activeSimCards.collectAsState()
    val throttleDelayMs by viewModel.throttleDelayMs.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showNotificationsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Dedicated Processing Screen for active SMS dispatching
    if (showProcessingScreen) {
        ProcessingScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeProcessingScreen() }
        )
        return
    }

    // 5 Primary Navigation Destinations as specified in Master Prompt
    val primaryTabs = remember {
        listOf(
            AppTab.HOME,
            AppTab.COMPOSE,
            AppTab.TEMPLATES,
            AppTab.HISTORY,
            AppTab.SETTINGS
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentTab != AppTab.GATEWAY && currentTab != AppTab.SETTINGS) {
                Surface(
                    color = Color.White,
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentTab == AppTab.COMPOSE) {
                            // Back Arrow Button
                            IconButton(
                                onClick = { viewModel.prevSendSmsStep() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Logo Squircle & Title (Will never wrap)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo_plane),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            val titleText = if (currentTab == AppTab.COMPOSE) "Send SMS" else "Simple SMS"
                            val subtitleText = if (currentTab == AppTab.COMPOSE) {
                                when (sendSmsStep) {
                                    1 -> "Send to contacts or numbers"
                                    2 -> "Create your message"
                                    else -> "Review and send your campaign"
                                }
                            } else {
                                "Fast • Simple • Reliable"
                            }

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        letterSpacing = (-0.3).sp,
                                        color = Color(0xFF111827)
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF6B7280),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Right Actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Compact Active SIM Status Pill
                            val carrierClean = defaultSim?.carrierName?.split(" ")?.firstOrNull { it.isNotBlank() }?.take(5)?.uppercase() ?: "JIO"
                            val simCarrierText = "SIM ${(defaultSim?.slotIndex ?: 0) + 1}: $carrierClean"

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { viewModel.openSimSelection() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF0066FF)
                                    )
                                    Text(
                                        text = simCarrierText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF0066FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch SIM",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF0066FF)
                                    )
                                }
                            }

                            if (currentTab != AppTab.COMPOSE) {
                                // Bell Notification Button with Red Badge Dot
                                Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                    IconButton(
                                        onClick = { showNotificationsDialog = true },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Notifications",
                                            tint = Color(0xFF374151),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Red badge dot
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 5.dp, end = 5.dp)
                                            .size(6.5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                }

                                // Settings Button
                                IconButton(
                                    onClick = { viewModel.openSettings() },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color(0xFF374151),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                primaryTabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            viewModel.switchTab(tab)
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0066FF),
                            selectedTextColor = Color(0xFF0066FF),
                            indicatorColor = Color(0xFFEBF2FE),
                            unselectedIconColor = Color(0xFF6B7280),
                            unselectedTextColor = Color(0xFF6B7280)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Alert Banner if missing
            if (!hasAllPermissions) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandErrorBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Carrier SMS & Contacts permissions required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandError,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = onRequestPermissions,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandError),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab Content with Animated Transition
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    AppTab.HOME -> HomeScreen(viewModel = viewModel)
                    AppTab.COMPOSE -> ComposeTab(
                        viewModel = viewModel,
                        onRequestPermissions = onRequestPermissions,
                        hasAllPermissions = hasAllPermissions
                    )
                    AppTab.GATEWAY -> WebGatewayTab(viewModel = viewModel)
                    AppTab.TEMPLATES -> TemplatesTab(viewModel = viewModel)
                    AppTab.HISTORY -> HistoryTab(viewModel = viewModel)
                    AppTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onRequestPermissions = onRequestPermissions,
                            hasAllPermissions = hasAllPermissions
                        )
                    }
                }
            }
        }
    }

    // Modal SIM Selection Dialog
    if (showSimSelectDialog) {
        SimSelectionDialog(
            simCards = activeSimCards,
            currentDefaultSim = defaultSim,
            onSelectSim = { selectedSim ->
                viewModel.selectDefaultSim(selectedSim)
            },
            onDismiss = { viewModel.closeSimSelection() },
            isFirstLaunch = defaultSim == null
        )
    }

    // Modal Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            simCards = activeSimCards,
            currentDefaultSim = defaultSim,
            onSelectDefaultSim = { selectedSim ->
                viewModel.selectDefaultSim(selectedSim)
            },
            throttleDelayMs = throttleDelayMs,
            onThrottleDelayChange = { newDelay ->
                viewModel.setThrottleDelay(newDelay)
            },
            onRequestPermissions = onRequestPermissions,
            hasTelephonyPermissions = hasAllPermissions,
            hasContactsPermission = hasAllPermissions,
            hasNotificationPermission = true,
            onDismiss = { viewModel.closeSettings() }
        )
    }

    // System Notifications Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "System Status & Alerts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF111827)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A36A))
                            )
                            Column {
                                Text(
                                    "Cloud Synchronization Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    "Google Firestore real-time listener is online.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0066FF))
                            )
                            Column {
                                Text(
                                    "Carrier SIM Radio Initialized",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1D4ED8)
                                )
                                Text(
                                    defaultSim?.let { "SIM ${it.slotIndex + 1} (${it.carrierName}) ready." } ?: "Dual SIM active.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF5F3FF),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6))
                            )
                            Column {
                                Text(
                                    "Desktop Remote Gateway",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6D28D9)
                                )
                                Text(
                                    "Ready to pair with your computer browser.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
