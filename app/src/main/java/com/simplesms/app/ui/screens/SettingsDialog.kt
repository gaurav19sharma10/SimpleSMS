package com.simplesms.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplesms.app.data.model.SimCardInfo
import com.simplesms.app.ui.theme.BrandBlue
import com.simplesms.app.ui.theme.BrandBlueDark
import com.simplesms.app.ui.theme.BrandBlueLight
import com.simplesms.app.ui.theme.BrandBorder
import com.simplesms.app.ui.theme.BrandError
import com.simplesms.app.ui.theme.BrandSuccess
import com.simplesms.app.ui.theme.BrandSurface
import com.simplesms.app.ui.theme.BrandSurfaceSecondary
import com.simplesms.app.ui.theme.BrandTextPrimary
import com.simplesms.app.ui.theme.BrandTextSecondary
import kotlin.math.roundToLong

@Composable
fun SettingsDialog(
    simCards: List<SimCardInfo>,
    currentDefaultSim: SimCardInfo?,
    onSelectDefaultSim: (SimCardInfo) -> Unit,
    throttleDelayMs: Long,
    onThrottleDelayChange: (Long) -> Unit,
    onRequestPermissions: () -> Unit,
    hasTelephonyPermissions: Boolean,
    hasContactsPermission: Boolean,
    hasNotificationPermission: Boolean,
    onDismiss: () -> Unit
) {
    var delaySliderValue by remember(throttleDelayMs) {
        mutableFloatStateOf(throttleDelayMs / 1000f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = BrandSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandBlueLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Application Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                    Text(
                        text = "Telephony & dispatch configuration",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Default Outbound SIM
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DEFAULT OUTBOUND SIM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "All carrier SMS transmissions are routed through this SIM card.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandTextSecondary
                    )

                    if (simCards.isEmpty()) {
                        Text(
                            text = "No active SIM cards detected on device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandError
                        )
                    } else {
                        simCards.forEach { sim ->
                            val isSelected = currentDefaultSim?.subscriptionId == sim.subscriptionId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDefaultSim(sim) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) BrandBlue else BrandBorder
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BrandBlueLight.copy(alpha = 0.5f) else BrandSurface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) BrandBlue else BrandSurfaceSecondary,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.SimCard,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else BrandTextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "SIM ${sim.slotIndex + 1}: ${sim.carrierName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandTextPrimary
                                            )
                                            if (!sim.phoneNumber.isNullOrBlank()) {
                                                Text(
                                                    text = sim.phoneNumber,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BrandTextSecondary
                                                )
                                            }
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectDefaultSim(sim) },
                                        colors = RadioButtonDefaults.colors(selectedColor = BrandBlue)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BrandBorder)

                // Section 2: Sequential Dispatch Throttle
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DISPATCH THROTTLE PACING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandBlueLight
                        ) {
                            Text(
                                text = "${String.format("%.1f", delaySliderValue)}s / SMS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Interval delay between successive carrier SMS transmissions to prevent telecom network rate-limiting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandTextSecondary
                    )

                    Slider(
                        value = delaySliderValue,
                        onValueChange = {
                            delaySliderValue = it
                            onThrottleDelayChange((it * 1000).roundToLong())
                        },
                        valueRange = 0.5f..5.0f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandBlue,
                            activeTrackColor = BrandBlue,
                            inactiveTrackColor = BrandBorder
                        )
                    )
                }

                HorizontalDivider(color = BrandBorder)

                // Section 3: Permissions Status
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PERMISSIONS STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        letterSpacing = 0.5.sp
                    )

                    PermissionStatusRow("Send SMS via Physical SIM", hasTelephonyPermissions)
                    PermissionStatusRow("Read Phone Contacts", hasContactsPermission)
                    PermissionStatusRow("Background Service Notification", hasNotificationPermission)

                    if (!hasTelephonyPermissions || !hasContactsPermission || !hasNotificationPermission) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onRequestPermissions,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BrandBorder)
                        ) {
                            Text("Grant Missing Permissions", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PermissionStatusRow(label: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) BrandSuccess else BrandError,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isGranted) BrandTextPrimary else BrandError
        )
    }
}
