package com.simplesms.app.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.simplesms.app.R
import com.simplesms.app.remote.EmbeddedQrScannerBox
import com.simplesms.app.remote.WebGatewayManager
import com.simplesms.app.ui.navigation.AppTab
import com.simplesms.app.ui.viewmodel.MainViewModel

private const val WEB_PORTAL_URL = "https://sms.rvsclasses.com"

enum class GatewayScreenState {
    LANDING,
    PAIRING,
    MANAGEMENT
}

@Composable
fun WebGatewayTab(viewModel: MainViewModel) {
    val context = LocalContext.current

    val linkedSessionId by WebGatewayManager.linkedSessionId.collectAsState()
    val isOnline by WebGatewayManager.isOnline.collectAsState()
    val deviceName by WebGatewayManager.deviceCustomName.collectAsState()
    val deviceBrowser by WebGatewayManager.deviceBrowser.collectAsState()
    val devicePlatform by WebGatewayManager.devicePlatform.collectAsState()
    val connectedTime by WebGatewayManager.connectedTime.collectAsState()

    var isGatewayActive by remember { mutableStateOf(WebGatewayManager.isGatewayEnabled(context)) }

    // Navigation state: starts on MANAGEMENT if linked, else LANDING
    var currentScreenState by remember {
        mutableStateOf(if (linkedSessionId != null) GatewayScreenState.MANAGEMENT else GatewayScreenState.LANDING)
    }

    LaunchedEffect(linkedSessionId) {
        if (linkedSessionId != null) {
            currentScreenState = GatewayScreenState.MANAGEMENT
        } else if (currentScreenState == GatewayScreenState.MANAGEMENT) {
            currentScreenState = GatewayScreenState.LANDING
        }
    }

    // Interactive dialog states
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var showManualCodeDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Pairing progress & steps
    var pairingStep by remember { mutableIntStateOf(1) } // 1: Scan QR, 2: Connecting, 3: Done
    var isPairingInProgress by remember { mutableStateOf(false) }

    // Camera permission check & request
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission needed to scan pairing QR code", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(label: String, text: String, toastMsg: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }

    fun openWebPortal() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_PORTAL_URL))
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard("Web Portal", WEB_PORTAL_URL, "URL copied: $WEB_PORTAL_URL")
        }
    }

    fun handleQrScanned(rawContent: String) {
        if (isPairingInProgress) return
        isPairingInProgress = true
        pairingStep = 2

        WebGatewayManager.pairWithScannedQr(
            context = context,
            qrRawValue = rawContent,
            onSuccess = {
                isPairingInProgress = false
                pairingStep = 3
                Toast.makeText(context, "Successfully paired with Web Portal!", Toast.LENGTH_SHORT).show()
                currentScreenState = GatewayScreenState.MANAGEMENT
            },
            onError = { error ->
                isPairingInProgress = false
                pairingStep = 1
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))
    ) {
        // TOP APP BAR: Identical design language across all 3 states
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (currentScreenState == GatewayScreenState.PAIRING) {
                                currentScreenState = if (linkedSessionId != null) {
                                    GatewayScreenState.MANAGEMENT
                                } else {
                                    GatewayScreenState.LANDING
                                }
                            } else {
                                viewModel.switchTab(AppTab.HOME)
                            }
                        }
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

                // Center Icon + Title & Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gateway_monitor),
                        contentDescription = "Web Remote Gateway",
                        modifier = Modifier.size(36.dp)
                    )

                    Column {
                        Text(
                            text = "Web Remote Gateway",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.5.sp,
                                letterSpacing = (-0.2).sp,
                                color = Color(0xFF172033)
                            )
                        )
                        val subtitle = when (currentScreenState) {
                            GatewayScreenState.LANDING -> "Send SMS from your computer"
                            GatewayScreenState.PAIRING -> "Pair a computer"
                            GatewayScreenState.MANAGEMENT -> "Manage connected devices"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                color = Color(0xFF667085),
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }

                // Help Button (?)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showHelpDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = Color(0xFF0057FF),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // CONTENT SECTION: Animated transitions between the 3 screen states
        AnimatedContent(
            targetState = currentScreenState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "gatewayScreen"
        ) { state ->
            when (state) {
                GatewayScreenState.LANDING -> {
                    GatewayLandingView(
                        onConnectClick = {
                            currentScreenState = GatewayScreenState.PAIRING
                        },
                        onViewPortalClick = {
                            openWebPortal()
                        }
                    )
                }

                GatewayScreenState.PAIRING -> {
                    GatewayPairingView(
                        pairingStep = pairingStep,
                        isPairingInProgress = isPairingInProgress,
                        hasCameraPermission = hasCameraPermission,
                        onRequestCameraPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onQrScanned = { raw ->
                            handleQrScanned(raw)
                        },
                        onCopyUrl = {
                            copyToClipboard("Web Portal URL", WEB_PORTAL_URL, "Portal URL copied to clipboard!")
                        },
                        onEnterCodeManually = {
                            showManualCodeDialog = true
                        }
                    )
                }

                GatewayScreenState.MANAGEMENT -> {
                    GatewayManagementView(
                        isGatewayActive = isGatewayActive,
                        deviceName = deviceName,
                        deviceBrowser = deviceBrowser,
                        devicePlatform = devicePlatform,
                        connectedTime = connectedTime,
                        sessionId = linkedSessionId ?: "sess_offline",
                        onToggleGateway = {
                            val target = !isGatewayActive
                            isGatewayActive = target
                            WebGatewayManager.setGatewayEnabled(context, target)
                        },
                        onAddDeviceClick = {
                            currentScreenState = GatewayScreenState.PAIRING
                        },
                        onRenameClick = {
                            showRenameDialog = true
                        },
                        onDisconnectClick = {
                            showDisconnectConfirmDialog = true
                        },
                        onCopySessionId = { fullId ->
                            copyToClipboard("Session ID", fullId, "Session ID copied to clipboard!")
                        },
                        onOpenWebPortal = {
                            openWebPortal()
                        },
                        onShowInstructions = {
                            showHelpDialog = true
                        }
                    )
                }
            }
        }
    }

    // DIALOG 1: Rename Device Dialog
    if (showRenameDialog) {
        var inputName by remember { mutableStateOf(deviceName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Rename Device",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF172033)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Assign a friendly name for this computer (e.g. My Laptop, Work PC, MacBook).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085)
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        singleLine = true,
                        placeholder = { Text("Device name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0057FF),
                            unfocusedBorderColor = Color(0xFFE5E9F0)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        WebGatewayManager.setDeviceCustomName(context, inputName)
                        showRenameDialog = false
                        Toast.makeText(context, "Device renamed to $inputName", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color(0xFF667085))
                }
            }
        )
    }

    // DIALOG 2: Disconnect Device Confirmation Dialog
    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Disconnect Device?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF172033)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to disconnect \"$deviceName\"? The desktop browser will no longer be able to send SMS until paired again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF667085),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        WebGatewayManager.unlinkSession(context)
                        showDisconnectConfirmDialog = false
                        currentScreenState = GatewayScreenState.LANDING
                        Toast.makeText(context, "Device disconnected", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Disconnect", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF667085))
                }
            }
        )
    }

    // DIALOG 3: Manual Pairing Code Dialog
    if (showManualCodeDialog) {
        var manualCodeInput by remember { mutableStateOf("") }
        var isManualPairing by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isManualPairing) showManualCodeDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Enter 10-Digit Pairing Code",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF172033)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the 10-digit code displayed on the Web Portal (e.g. K7M9P-2R4X8). Hyphens and spaces are optional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085)
                    )
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { input ->
                            // Auto-uppercase and filter allowed chars
                            val filtered = input.uppercase().filter { it.isLetterOrDigit() || it == '-' || it == ' ' }.take(14)
                            manualCodeInput = filtered
                        },
                        singleLine = true,
                        placeholder = { Text("e.g. K7M9P - 2R4X8") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0057FF),
                            unfocusedBorderColor = Color(0xFFE5E9F0)
                        )
                    )
                    if (isManualPairing) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF0057FF))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sanitized = manualCodeInput.replace("-", "").replace(" ", "").trim().uppercase()
                        if (sanitized.isBlank()) {
                            Toast.makeText(context, "Please enter a valid 10-digit pairing code", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isManualPairing = true
                        WebGatewayManager.pairWithScannedQr(
                            context = context,
                            qrRawValue = sanitized,
                            onSuccess = {
                                isManualPairing = false
                                showManualCodeDialog = false
                                currentScreenState = GatewayScreenState.MANAGEMENT
                                Toast.makeText(context, "Successfully paired with Web Portal!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isManualPairing = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isManualPairing && manualCodeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Pair Device", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualCodeDialog = false },
                    enabled = !isManualPairing
                ) {
                    Text("Cancel", color = Color(0xFF667085))
                }
            }
        )
    }

    // DIALOG 4: Step-by-Step Instructions & Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF0057FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Web Remote Gateway Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF172033)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GuideStepItem("1", "Open Portal", "Open $WEB_PORTAL_URL on any computer or laptop browser.")
                    GuideStepItem("2", "Display QR Code", "The web portal generates a unique secure session QR code.")
                    GuideStepItem("3", "Scan with App", "Tap 'Connect a Computer' and point your phone camera at the QR code.")
                    GuideStepItem("4", "Keep SIM Active", "Ensure this phone remains connected to the internet with your SIM card inserted.")
                    GuideStepItem("5", "Send SMS Remotely", "Type messages and upload CSV files on your desktop to send SMS through this device!")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// =====================================================================================
// SCREEN 1: LANDING STATE ("Send SMS from your computer")
// =====================================================================================
@Composable
private fun GatewayLandingView(
    onConnectClick: () -> Unit,
    onViewPortalClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Graphic Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFF7EEFD),
                                    Color(0xFFE2F1FD),
                                    Color(0xFFECFDF5)
                                )
                            )
                        )
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gateway_hero),
                        contentDescription = "Laptop and smartphone linked",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                    )
                }
            }
        }

        // Headline & Explanatory Subtitle
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Send SMS from your computer",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.5.sp,
                        letterSpacing = (-0.3).sp,
                        color = Color(0xFF172033)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Connect your phone and send SMS directly from the web portal. Keep your SIM in this phone.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF667085),
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4 Feature Advantage Rows
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
                    FeatureRow(
                        icon = Icons.Default.Bolt,
                        iconColor = Color(0xFFF59E0B),
                        iconBg = Color(0xFFFFF7ED),
                        title = "Fast & Easy",
                        subtitle = "Pair once and start sending SMS"
                    )
                    FeatureRow(
                        icon = Icons.Default.Security,
                        iconColor = Color(0xFF10B981),
                        iconBg = Color(0xFFECFDF5),
                        title = "Secure Connection",
                        subtitle = "Your data stays on your device"
                    )
                    FeatureRow(
                        icon = Icons.Default.Sync,
                        iconColor = Color(0xFF0057FF),
                        iconBg = Color(0xFFEFF6FF),
                        title = "Real-time Sync",
                        subtitle = "Messages, contacts and templates"
                    )
                    FeatureRow(
                        icon = Icons.Default.Laptop,
                        iconColor = Color(0xFF8B5CF6),
                        iconBg = Color(0xFFF5F3FF),
                        title = "Multi-Device Support",
                        subtitle = "Use on multiple computers"
                    )
                }
            }
        }

        // Bottom Action Buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action Button: Connect a Computer
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Connect a Computer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                // Secondary Action Button: View Web Portal
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onViewPortalClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFF0057FF),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View Web Portal",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0057FF)
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================================
// SCREEN 2: PAIRING STATE ("Pair a computer")
// =====================================================================================
@Composable
private fun GatewayPairingView(
    pairingStep: Int,
    isPairingInProgress: Boolean,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onQrScanned: (String) -> Unit,
    onCopyUrl: () -> Unit,
    onEnterCodeManually: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Step Breadcrumb Stepper: (1) Scan QR Code  ──  (2) Connecting  ──  (3) Done
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Step 1
                    StepPill(
                        stepNum = "1",
                        label = "Scan QR Code",
                        isActive = pairingStep >= 1,
                        isDone = pairingStep > 1
                    )

                    // Connecting Line 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .height(1.5.dp)
                            .background(if (pairingStep >= 2) Color(0xFF0057FF) else Color(0xFFE5E9F0))
                    )

                    // Step 2
                    StepPill(
                        stepNum = "2",
                        label = "Connecting",
                        isActive = pairingStep >= 2,
                        isDone = pairingStep > 2
                    )

                    // Connecting Line 2
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .height(1.5.dp)
                            .background(if (pairingStep >= 3) Color(0xFF16A36A) else Color(0xFFE5E9F0))
                    )

                    // Step 3
                    StepPill(
                        stepNum = "3",
                        label = "Done",
                        isActive = pairingStep >= 3,
                        isDone = pairingStep >= 3
                    )
                }
            }
        }

        // STEP 1 CONTAINER: Open Web Portal
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "1",
                                    color = Color(0xFF0057FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Open the web portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color(0xFF172033)
                            )
                            Text(
                                text = "On your computer, open this URL:",
                                fontSize = 12.sp,
                                color = Color(0xFF667085)
                            )
                        }
                    }

                    // URL Box with Copy Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F4FA),
                        border = BorderStroke(1.dp, Color(0xFFDCE4F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = WEB_PORTAL_URL,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0057FF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFDCE4F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onCopyUrl() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color(0xFF0057FF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Copy",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF0057FF)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // STEP 2 CONTAINER: Scan the QR code
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "2",
                                    color = Color(0xFF0057FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Scan the QR code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color(0xFF172033)
                            )
                            Text(
                                text = "Scan the QR code shown on the website using your phone.",
                                fontSize = 12.sp,
                                color = Color(0xFF667085)
                            )
                        }
                    }

                    // Live Camera QR Viewport matching the reference mockup
                    EmbeddedQrScannerBox(
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = onRequestCameraPermission,
                        onQrScanned = onQrScanned
                    )
                }
            }
        }

        // OR DIVIDER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFE5E9F0))
                )
                Text(
                    text = "OR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF98A2B3),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFE5E9F0))
                )
            }
        }

        // Enter Pairing Code Manually Button
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onEnterCodeManually() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = Color(0xFF0057FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter Pairing Code Manually",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0057FF)
                    )
                }
            }
        }
    }
}

// =====================================================================================
// SCREEN 3: MANAGEMENT STATE ("Manage connected devices")
// =====================================================================================
@Composable
private fun GatewayManagementView(
    isGatewayActive: Boolean,
    deviceName: String,
    deviceBrowser: String,
    devicePlatform: String,
    connectedTime: String,
    sessionId: String,
    onToggleGateway: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onCopySessionId: (String) -> Unit,
    onOpenWebPortal: () -> Unit,
    onShowInstructions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Card: Gateway Active / Inactive
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (isGatewayActive) Color(0xFFBBF7D0) else Color(0xFFFED7AA)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGatewayActive) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Green pulsing status indicator dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isGatewayActive) Color(0xFF16A36A) else Color(0xFFF59E0B))
                        )
                        Column {
                            Text(
                                text = if (isGatewayActive) "Gateway Active" else "Gateway Paused",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isGatewayActive) Color(0xFF166534) else Color(0xFF92400E)
                            )
                            Text(
                                text = if (isGatewayActive) {
                                    "Your phone is connected and ready to send SMS from the web portal."
                                } else {
                                    "Remote SMS dispatch is paused. Turn on to resume sending."
                                },
                                fontSize = 11.5.sp,
                                color = if (isGatewayActive) Color(0xFF15803D) else Color(0xFFB45309),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Turn Off / Turn On button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFDCE4F0)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleGateway() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (isGatewayActive) Color(0xFF0057FF) else Color(0xFF16A36A),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isGatewayActive) "Turn Off" else "Turn On",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isGatewayActive) Color(0xFF0057FF) else Color(0xFF16A36A)
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Connected Devices (1) + Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Connected Devices (1)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF172033)
                )

                // Plus Add Device Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAddDeviceClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add computer",
                            tint = Color(0xFF0057FF),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // DEVICE CARD
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
                    // Header Row: Laptop Icon + Device Name (Editable) + Chevron
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_laptop_device),
                                contentDescription = "Laptop",
                                modifier = Modifier.size(44.dp)
                            )

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = deviceName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF172033)
                                    )

                                    // Edit Pencil Icon
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename Device",
                                        tint = Color(0xFF98A2B3),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRenameClick() }
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF16A36A))
                                    )
                                    Text(
                                        text = "Active now",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF16A36A)
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFF98A2B3),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F4FA), thickness = 1.dp)

                    // 5 Metadata Key-Value Rows
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeviceMetadataRow(
                            icon = Icons.Default.Public,
                            label = "Browser",
                            value = deviceBrowser
                        )
                        DeviceMetadataRow(
                            icon = Icons.Default.Laptop,
                            label = "Platform",
                            value = devicePlatform
                        )
                        DeviceMetadataRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Connected On",
                            value = connectedTime
                        )
                        DeviceMetadataRow(
                            icon = Icons.Default.AccessTime,
                            label = "Last Active",
                            value = "2 minutes ago"
                        )
                        DeviceMetadataRow(
                            icon = Icons.Default.VpnKey,
                            label = "Session ID",
                            value = if (sessionId.length > 10) "${sessionId.take(4)}...${sessionId.takeLast(4)}" else sessionId,
                            onCopy = { onCopySessionId(sessionId) }
                        )
                    }

                    // Disconnect Device Action Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF0F0),
                        border = BorderStroke(1.dp, Color(0xFFFED7D7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDisconnectClick() }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFE5484D),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Disconnect Device",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFFE5484D)
                            )
                        }
                    }
                }
            }
        }

        // Action Tile 1: Open Web Portal
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenWebPortal() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = Color(0xFF0057FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Open Web Portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF172033)
                            )
                            Text(
                                text = "Use this link on your computer to access the SMS panel.",
                                fontSize = 11.5.sp,
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

        // Action Tile 2: Show Pairing Instructions
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E9F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onShowInstructions() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF5F3FF),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Show Pairing Instructions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF8B5CF6)
                            )
                            Text(
                                text = "Step by step guide to connect a computer.",
                                fontSize = 11.5.sp,
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
    }
}

// =====================================================================================
// REUSABLE MICRO-COMPONENTS
// =====================================================================================
@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconBg,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
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
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF667085)
            )
        }
    }
}

@Composable
private fun StepPill(
    stepNum: String,
    label: String,
    isActive: Boolean,
    isDone: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isDone -> Color(0xFF16A36A)
                isActive -> Color(0xFF0057FF)
                else -> Color(0xFFE5E9F0)
            },
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNum,
                    color = if (isActive || isDone) Color.White else Color(0xFF667085),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp
                )
            }
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isDone -> Color(0xFF16A36A)
                isActive -> Color(0xFF0057FF)
                else -> Color(0xFF98A2B3)
            }
        )
    }
}

@Composable
private fun DeviceMetadataRow(
    icon: ImageVector,
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF98A2B3),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.5.sp,
                color = Color(0xFF667085)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF172033)
            )

            if (onCopy != null) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color(0xFF0057FF),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onCopy() }
                )
            }
        }
    }
}

@Composable
private fun GuideStepItem(
    number: String,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFEFF6FF),
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF0057FF)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF172033)
            )
            Text(
                text = desc,
                fontSize = 11.5.sp,
                color = Color(0xFF667085),
                lineHeight = 16.sp
            )
        }
    }
}
