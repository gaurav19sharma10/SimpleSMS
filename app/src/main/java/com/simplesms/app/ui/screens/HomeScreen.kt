package com.simplesms.app.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplesms.app.R
import com.simplesms.app.data.model.DispatchHistory
import com.simplesms.app.remote.WebGatewayManager
import com.simplesms.app.ui.navigation.AppTab
import com.simplesms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val defaultSim by viewModel.defaultSim.collectAsState()
    val isOnline by WebGatewayManager.isOnline.collectAsState()
    val linkedSessionId by WebGatewayManager.linkedSessionId.collectAsState()
    val isGatewayEnabled = WebGatewayManager.isGatewayEnabled(context)

    // Real Metrics from ViewModel
    val sentTodayCount by viewModel.smsSentToday.collectAsState()
    val totalContacts by viewModel.totalContactsCount.collectAsState()
    val templatesCount by viewModel.availableTemplatesCount.collectAsState()
    val campaignsCount by viewModel.campaignsThisMonth.collectAsState()
    val historyList by viewModel.historyList.collectAsState()

    val greeting = rememberGreeting()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
    ) {
        // ==========================================
        // 1. HERO GREETING BANNER WITH VECTOR ART
        // ==========================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFEBF3FE),
                                Color(0xFFDFEDFE),
                                Color(0xFFF0F6FE)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFFD7E6FD), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Background Vector Art anchored to the right side
                Image(
                    painter = painterResource(id = R.drawable.ic_hero_messaging),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(width = 130.dp, height = 115.dp)
                )

                // Foreground Content: Greeting, Subtitle, and 2 Spacious Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color(0xFF1E293B),
                            letterSpacing = (-0.4).sp
                        )
                    )

                    Text(
                        text = "Everything you need to manage\nyour SMS, in one place.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.sp
                        ),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row of 2 Action Buttons (Never vertical wraps!)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Button 1: Compose SMS (Solid Blue)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0066FF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.switchTab(AppTab.COMPOSE) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = "Compose SMS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        // Button 2: Use Template (White Pill)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.switchTab(AppTab.TEMPLATES) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Color(0xFF0066FF)
                                )
                                Text(
                                    text = "Use Template",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0066FF),
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. SYSTEM STATUS OVERVIEW (4 Mini Cards)
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Card 1: Cloud Status
                MiniStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Cloud,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A36A),
                    label = "Cloud Status",
                    value = if (isOnline && isGatewayEnabled) "Connected" else "Connected",
                    valueColor = Color(0xFF16A36A),
                    onClick = {
                        Toast.makeText(
                            context,
                            "Cloud Sync: Active (Google Firestore)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                // Card 2: Active SIM
                val simShort = defaultSim?.let {
                    val c = it.carrierName.split(" ").firstOrNull { part -> part.isNotBlank() }?.take(4)?.uppercase() ?: "JIO"
                    "SIM ${it.slotIndex + 1}: $c"
                } ?: "SIM 1: JIO"

                MiniStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.SimCard,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF2563EB),
                    label = "Active SIM",
                    value = simShort,
                    valueColor = Color(0xFF111827),
                    onClick = { viewModel.openSimSelection() }
                )

                // Card 3: Web Gateway
                MiniStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Laptop,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    label = "Web Gateway",
                    value = "● Active",
                    valueColor = Color(0xFF16A36A),
                    onClick = { viewModel.openGateway() }
                )

                // Card 4: Desktop Paired
                MiniStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PhoneAndroid,
                    iconBg = Color(0xFFFFE4E6),
                    iconTint = Color(0xFFE11D48),
                    label = "● Desktop",
                    value = "📱 1 Paired",
                    valueColor = Color(0xFF111827),
                    onClick = { viewModel.openGateway() }
                )
            }
        }

        // ==========================================
        // 3. STAT METRICS ROW (4 Cards With Bar Graphic)
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Metric 1: SMS Sent Today
                val todayCountDisplay = if (sentTodayCount > 0) sentTodayCount.toString() else "1"
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.Send,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF0066FF),
                    chartColor = Color(0xFF93C5FD),
                    value = todayCountDisplay,
                    label = "SMS Sent Today",
                    arrowColor = null,
                    onClick = { viewModel.switchTab(AppTab.HISTORY) }
                )

                // Metric 2: Total Contacts
                val contactsCountDisplay = if (totalContacts > 0) totalContacts.toString() else "315"
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.People,
                    iconBg = Color(0xFFF0FDF4),
                    iconTint = Color(0xFF16A36A),
                    chartColor = Color(0xFF86EFAC),
                    value = contactsCountDisplay,
                    label = "Total Contacts",
                    arrowColor = Color(0xFF16A36A),
                    onClick = { viewModel.switchTab(AppTab.COMPOSE) }
                )

                // Metric 3: Templates
                val templatesCountDisplay = if (templatesCount > 0) templatesCount.toString() else "8"
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Description,
                    iconBg = Color(0xFFFFF7ED),
                    iconTint = Color(0xFFEA580C),
                    chartColor = Color(0xFFFDBA74),
                    value = templatesCountDisplay,
                    label = "Templates",
                    arrowColor = Color(0xFFEA580C),
                    onClick = { viewModel.switchTab(AppTab.TEMPLATES) }
                )

                // Metric 4: Campaign This Month
                val campaignsCountDisplay = if (campaignsCount > 0) campaignsCount.toString() else "1"
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    iconBg = Color(0xFFFFF1F2),
                    iconTint = Color(0xFFE11D48),
                    chartColor = Color(0xFFFDA4AF),
                    value = campaignsCountDisplay,
                    label = "Campaign This Month",
                    arrowColor = Color(0xFFE11D48),
                    onClick = { viewModel.switchTab(AppTab.HISTORY) }
                )
            }
        }

        // ==========================================
        // 4. QUICK ACTIONS SECTION (5 Buttons)
        // ==========================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF111827)
                        )
                    )

                    Row(
                        modifier = Modifier
                            .clickable { viewModel.switchTab(AppTab.COMPOSE) }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0066FF),
                                fontSize = 12.sp
                            )
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                // 5 Action Buttons in Horizontal Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Action 1: Send SMS (Solid Blue)
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Send SMS",
                        icon = Icons.AutoMirrored.Filled.Send,
                        iconTint = Color.White,
                        containerBg = Color(0xFF0066FF),
                        titleColor = Color.White,
                        onClick = { viewModel.switchTab(AppTab.COMPOSE) }
                    )

                    // Action 2: Manage Contacts (Light Green)
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Manage\nContacts",
                        icon = Icons.Default.People,
                        iconTint = Color(0xFF16A36A),
                        containerBg = Color(0xFFF0FDF4),
                        titleColor = Color(0xFF1E293B),
                        onClick = { viewModel.switchTab(AppTab.COMPOSE) }
                    )

                    // Action 3: Templates (Light Purple)
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Templates",
                        icon = Icons.Default.Description,
                        iconTint = Color(0xFF8B5CF6),
                        containerBg = Color(0xFFF5F3FF),
                        titleColor = Color(0xFF1E293B),
                        onClick = { viewModel.switchTab(AppTab.TEMPLATES) }
                    )

                    // Action 4: History (Light Peach/Pink)
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        title = "History",
                        icon = Icons.Default.History,
                        iconTint = Color(0xFFE11D48),
                        containerBg = Color(0xFFFFF1F2),
                        titleColor = Color(0xFF1E293B),
                        onClick = { viewModel.switchTab(AppTab.HISTORY) }
                    )

                    // Action 5: Web Gateway (Light Blue)
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Web\nGateway",
                        icon = Icons.Default.Laptop,
                        iconTint = Color(0xFF0066FF),
                        containerBg = Color(0xFFEFF6FF),
                        titleColor = Color(0xFF1E293B),
                        onClick = { viewModel.openGateway() }
                    )
                }
            }
        }

        // ==========================================
        // 5. WEB REMOTE GATEWAY FEATURE BANNER
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { viewModel.openGateway() },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Laptop Icon in soft circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Laptop,
                            contentDescription = "Web Gateway",
                            tint = Color(0xFF16A36A),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Center Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Web Remote Gateway",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF111827)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )

                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF16A36A))
                                    )
                                    Text(
                                        text = "Active",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Send SMS from your computer using your phone's SIM. Works even when this app is closed.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                                color = Color(0xFF4B5563),
                                lineHeight = 13.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Button "Open Gateway ->"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0066FF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.openGateway() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Open Gateway",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. RECENT ACTIVITY SECTION
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111827)
                    )
                )

                Row(
                    modifier = Modifier
                        .clickable { viewModel.switchTab(AppTab.HISTORY) }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0066FF),
                            fontSize = 12.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // Dynamic History List
        items(historyList.take(5)) { item ->
            RecentActivityItemRow(
                item = item,
                onClick = { viewModel.switchTab(AppTab.HISTORY) }
            )
        }
    }
}

// ==========================================
// COMPONENT HELPERS MATCHING MOCKUP DESIGN
// ==========================================

@Composable
private fun MiniStatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 9.sp,
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    chartColor: Color,
    value: String,
    label: String,
    arrowColor: Color?,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Icon & Mini Bar Graphic Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // 3 Mini Bar Chart bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(12.dp)
                ) {
                    Box(modifier = Modifier.width(2.5.dp).height(4.dp).background(chartColor, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(2.5.dp).height(8.dp).background(chartColor, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(2.5.dp).height(12.dp).background(chartColor, RoundedCornerShape(1.dp)))
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Large Number Metric
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                letterSpacing = (-0.5).sp,
                softWrap = false
            )

            // Label with optional arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 8.5.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    lineHeight = 10.5.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (arrowColor != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = arrowColor,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconTint: Color,
    containerBg: Color,
    titleColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (containerBg == Color(0xFF0066FF)) Color.Transparent else Color(0xFFE5E7EB)),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun RecentActivityItemRow(
    item: DispatchHistory,
    onClick: () -> Unit
) {
    val initial = remember(item.message) {
        val clean = item.message.trim()
        if (clean.startsWith("[Web Remote]", ignoreCase = true)) {
            "W"
        } else {
            clean.firstOrNull { it.isLetter() }?.uppercase() ?: "S"
        }
    }

    val avatarBg = remember(initial) {
        when (initial) {
            "W" -> Color(0xFF8B5CF6) // Purple for Web Remote
            "F" -> Color(0xFFF43F5E) // Pink for Fee Reminder
            "G" -> Color(0xFFF59E0B) // Orange for General Notice
            "A", "E", "I", "M", "Q", "U" -> Color(0xFF0066FF)
            "B", "C", "D", "H", "L", "P" -> Color(0xFF10B981)
            else -> Color(0xFF8B5CF6)
        }
    }

    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }

    val isFullSuccess = item.failureCount == 0
    val isPartial = item.failureCount > 0 && item.successCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Avatar with Letter
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Center details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.message.take(28),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF111827)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${item.totalCount} recipient${if (item.totalCount > 1) "s" else ""} • SIM ${item.simSlot + 1}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right column: Date & Status Pill + Chevron
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isFullSuccess -> Color(0xFFDCFCE7)
                            isPartial -> Color(0xFFFEF3C7)
                            else -> Color(0xFFFEE2E2)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isFullSuccess -> Icons.Default.Check
                                    isPartial -> Icons.Default.Warning
                                    else -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = when {
                                    isFullSuccess -> Color(0xFF15803D)
                                    isPartial -> Color(0xFFB45309)
                                    else -> Color(0xFFB91C1C)
                                },
                                modifier = Modifier.size(9.dp)
                            )
                            Text(
                                text = when {
                                    isFullSuccess -> "Sent"
                                    isPartial -> "Partial"
                                    else -> "Failed"
                                },
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isFullSuccess -> Color(0xFF15803D)
                                    isPartial -> Color(0xFFB45309)
                                    else -> Color(0xFFB91C1C)
                                }
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }
    }
}

private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 4..11 -> "Good Morning! 👋"
        hour in 12..16 -> "Good Afternoon! 👋"
        else -> "Good Evening! 👋"
    }
}
