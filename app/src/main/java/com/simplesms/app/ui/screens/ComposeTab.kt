package com.simplesms.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplesms.app.R
import com.simplesms.app.data.model.ContactItem
import com.simplesms.app.data.model.MessageTemplate
import com.simplesms.app.ui.navigation.AppTab
import com.simplesms.app.ui.viewmodel.MainViewModel

@Composable
fun ComposeTab(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    hasAllPermissions: Boolean
) {
    val context = LocalContext.current
    val currentStep by viewModel.sendSmsStep.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val selectedContacts by viewModel.selectedContactsList.collectAsState()
    val selectedContactIds by viewModel.selectedContactIds.collectAsState()
    val selectedCount by viewModel.selectedCount.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val defaultSim by viewModel.defaultSim.collectAsState()
    val templatesList by viewModel.templatesList.collectAsState()
    val throttleDelayMs by viewModel.throttleDelayMs.collectAsState()

    // Modals state
    var showContactPicker by remember { mutableStateOf(false) }
    var showManualNumberDialog by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showEmojiDialog by remember { mutableStateOf(false) }

    // GSM-7 SMS part calculation
    val charCount = messageText.length
    val smsParts = remember(charCount) {
        if (charCount <= 160) 1 else ((charCount - 1) / 153) + 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // ==========================================
        // 1. STEP PROGRESS BREADCRUMB INDICATOR
        // ==========================================
        StepProgressBar(currentStep = currentStep)

        // ==========================================
        // 2. ACTIVE STEP VIEW CONTENT
        // ==========================================
        when (currentStep) {
            1 -> {
                StepSelectRecipientsView(
                    viewModel = viewModel,
                    selectedContacts = selectedContacts,
                    selectedCount = selectedCount,
                    onOpenContacts = {
                        if (!hasAllPermissions) onRequestPermissions()
                        showContactPicker = true
                    },
                    onOpenManualEntry = { showManualNumberDialog = true },
                    onNext = { viewModel.nextSendSmsStep() }
                )
            }
            2 -> {
                StepCreateMessageView(
                    viewModel = viewModel,
                    messageText = messageText,
                    charCount = charCount,
                    smsParts = smsParts,
                    selectedContacts = selectedContacts,
                    templatesList = templatesList,
                    onOpenTemplates = { showTemplateSheet = true },
                    onOpenSaveTemplate = { showSaveTemplateDialog = true },
                    onOpenEmoji = { showEmojiDialog = true },
                    onBack = { viewModel.prevSendSmsStep() },
                    onNext = { viewModel.nextSendSmsStep() }
                )
            }
            3 -> {
                StepReviewAndSendView(
                    viewModel = viewModel,
                    selectedContacts = selectedContacts,
                    selectedCount = selectedCount,
                    messageText = messageText,
                    charCount = charCount,
                    smsParts = smsParts,
                    defaultSimCarrier = defaultSim?.let { "SIM ${it.slotIndex + 1}: ${it.carrierName.take(12)}" } ?: "SIM 1: JIO – Jio",
                    throttleDelayMs = throttleDelayMs,
                    onEditRecipients = { viewModel.setSendSmsStep(1) },
                    onEditMessage = { viewModel.setSendSmsStep(2) },
                    onChangeSim = { viewModel.openSimSelection() },
                    onChangeDispatch = { viewModel.openSettings() },
                    onSendDispatch = { viewModel.startSmsDispatch() }
                )
            }
        }
    }

    // ==========================================
    // MODAL DIALOGS
    // ==========================================

    // Contact Picker Modal
    if (showContactPicker) {
        ContactPickerModal(
            allContacts = allContacts,
            selectedContactIds = selectedContactIds,
            onToggleContact = { id -> viewModel.toggleContactSelection(id) },
            onSelectAll = { select -> viewModel.selectAllVisibleContacts(select) },
            onDismiss = { showContactPicker = false }
        )
    }

    // Manual Phone Number Entry Dialog
    if (showManualNumberDialog) {
        ManualNumberDialog(
            onAddNumber = { name, phone ->
                viewModel.addManualRecipient(name, phone)
                showManualNumberDialog = false
            },
            onDismiss = { showManualNumberDialog = false }
        )
    }

    // Template Picker Sheet
    if (showTemplateSheet) {
        TemplatePickerSheet(
            templates = templatesList,
            onSelectTemplate = { t ->
                viewModel.onMessageTextChange(t.content)
                showTemplateSheet = false
            },
            onDismiss = { showTemplateSheet = false }
        )
    }

    // Save as Template Dialog
    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            currentMessage = messageText,
            onSave = { title, cat ->
                viewModel.addCustomTemplate(title, cat, messageText)
                Toast.makeText(context, "Template saved!", Toast.LENGTH_SHORT).show()
                showSaveTemplateDialog = false
            },
            onDismiss = { showSaveTemplateDialog = false }
        )
    }

    // Emoji Picker Dialog
    if (showEmojiDialog) {
        EmojiPickerDialog(
            onSelectEmoji = { emoji ->
                viewModel.onMessageTextChange(messageText + emoji)
                showEmojiDialog = false
            },
            onDismiss = { showEmojiDialog = false }
        )
    }
}

// ==========================================
// 1. STEP PROGRESS BAR COMPONENT
// ==========================================

@Composable
private fun StepProgressBar(currentStep: Int) {
    Surface(
        color = Color.White,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Step 1: Recipients
            StepIndicatorItem(
                stepNumber = 1,
                label = "Recipients",
                isActive = currentStep == 1,
                isCompleted = currentStep > 1
            )

            // Line 1 -> 2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .padding(horizontal = 8.dp)
                    .background(if (currentStep > 1) Color(0xFF0066FF) else Color(0xFFE5E7EB))
            )

            // Step 2: Message
            StepIndicatorItem(
                stepNumber = 2,
                label = "Message",
                isActive = currentStep == 2,
                isCompleted = currentStep > 2
            )

            // Line 2 -> 3
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .padding(horizontal = 8.dp)
                    .background(if (currentStep > 2) Color(0xFF0066FF) else Color(0xFFE5E7EB))
            )

            // Step 3: Review & Send
            StepIndicatorItem(
                stepNumber = 3,
                label = "Review & Send",
                isActive = currentStep == 3,
                isCompleted = false
            )
        }
    }
}

@Composable
private fun StepIndicatorItem(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val circleColor = when {
            isCompleted || isActive -> Color(0xFF0066FF)
            else -> Color(0xFFD1D5DB)
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive || isCompleted) Color(0xFF0066FF) else Color(0xFF6B7280)
        )
    }
}

// ==========================================
// 2. STEP 1: SELECT RECIPIENTS VIEW
// ==========================================

@Composable
private fun StepSelectRecipientsView(
    viewModel: MainViewModel,
    selectedContacts: List<ContactItem>,
    selectedCount: Int,
    onOpenContacts: () -> Unit,
    onOpenManualEntry: () -> Unit,
    onNext: () -> Unit
) {
    var selectedMode by remember { mutableIntStateOf(1) } // 1: Contacts, 2: Manual

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Section Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Select Recipients",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = "Choose from your contacts or enter numbers manually.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }

        // Two Mode Selector Cards (Side-by-Side)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mode 1: Choose from Contacts
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            selectedMode = 1
                            onOpenContacts()
                        },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (selectedMode == 1) 1.5.dp else 1.dp,
                        color = if (selectedMode == 1) Color(0xFF0066FF) else Color(0xFFE5E7EB)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == 1) Color(0xFFF8FAFF) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (selectedMode == 1) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0066FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Choose from\nContacts",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "Select from saved contacts, groups or categories.",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 15.sp
                        )
                    }
                }

                // Mode 2: Enter Numbers Manually
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            selectedMode = 2
                            onOpenManualEntry()
                        },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (selectedMode == 2) 1.5.dp else 1.dp,
                        color = if (selectedMode == 2) Color(0xFF10B981) else Color(0xFFE5E7EB)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == 2) Color(0xFFF9FDFB) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0FDF4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_dialpad_dots),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (selectedMode == 2) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Enter Numbers\nManually",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "Add one or multiple mobile numbers.",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Selected Recipients Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Selected Recipients",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF111827)
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF)
                    ) {
                        Text(
                            text = selectedCount.toString(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (selectedCount > 0) {
                    Text(
                        text = "Clear All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066FF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.deselectAll() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // List of Selected Recipients
        items(selectedContacts) { contact ->
            RecipientItemRow(
                contact = contact,
                onRemove = { viewModel.removeSelectedContact(contact.id) }
            )
        }

        // "+ Add More Recipients" Button
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenContacts() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add More Recipients",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066FF)
                    )
                }
            }
        }

        // Bottom "Next ->" Button
        item {
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Next",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RecipientItemRow(
    contact: ContactItem,
    onRemove: () -> Unit
) {
    val initials = remember(contact.name) {
        val parts = contact.name.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        } else {
            contact.name.take(2).uppercase()
        }
    }

    val avatarColor = remember(contact.name) {
        when {
            contact.name.contains("Arjun", ignoreCase = true) -> Color(0xFF8B5CF6) // Purple
            contact.name.contains("Suresh", ignoreCase = true) -> Color(0xFF10B981) // Green
            contact.name.contains("Neha", ignoreCase = true) -> Color(0xFF3B82F6) // Blue
            else -> {
                val colors = listOf(Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFFEC4899))
                colors[Math.abs(contact.name.hashCode()) % colors.size]
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contact Name & Phone
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF111827)
                )
                Text(
                    text = contact.phoneNumber,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }

            // Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. STEP 2: CREATE MESSAGE VIEW
// ==========================================

@Composable
private fun StepCreateMessageView(
    viewModel: MainViewModel,
    messageText: String,
    charCount: Int,
    smsParts: Int,
    selectedContacts: List<ContactItem>,
    templatesList: List<MessageTemplate>,
    onOpenTemplates: () -> Unit,
    onOpenSaveTemplate: () -> Unit,
    onOpenEmoji: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var insertFieldMenuExpanded by remember { mutableStateOf(false) }
    var selectedPreviewContactIndex by remember { mutableIntStateOf(0) }
    var previewMenuExpanded by remember { mutableStateOf(false) }

    val activePreviewContact = remember(selectedContacts, selectedPreviewContactIndex) {
        selectedContacts.getOrNull(selectedPreviewContactIndex)
            ?: selectedContacts.firstOrNull()
            ?: ContactItem("demo", "Arjun Mahawar", "8426073133")
    }

    val renderedPreviewText = remember(messageText, activePreviewContact) {
        messageText.replace("{name}", activePreviewContact.name)
            .replace("{phone}", activePreviewContact.phoneNumber)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Title & Description
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Create Message",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = "Type your message or use a template.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }

        // Template Filter Chips Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary "Templates" button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenTemplates() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Templates",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF)
                        )
                    }
                }

                // Category Chips
                listOf("Greetings", "Reminders", "Business").forEach { cat ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val match = templatesList.firstOrNull { it.category.equals(cat, ignoreCase = true) }
                                if (match != null) {
                                    viewModel.onMessageTextChange(match.content)
                                }
                            }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Message Input Box with Toolbar inside
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Text Input Area
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { viewModel.onMessageTextChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            placeholder = {
                                Text(
                                    text = "Hello {name},\nType your message here...",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 14.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = Color(0xFF111827),
                                lineHeight = 20.sp
                            )
                        )

                        // Smiley Emoji Button in bottom right
                        IconButton(
                            onClick = onOpenEmoji,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfied,
                                contentDescription = "Insert Emoji",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Action Toolbar Row inside card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. "+ {name}" Tag Insertion Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.insertNameTag() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "{name}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0066FF)
                                )
                            }
                        }

                        // 2. Insert Field Dropdown Chip
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { insertFieldMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Insert Field",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF374151)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = insertFieldMenuExpanded,
                                onDismissRequest = { insertFieldMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("{name} — Contact Name") },
                                    onClick = {
                                        viewModel.insertNameTag()
                                        insertFieldMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("{phone} — Phone Number") },
                                    onClick = {
                                        viewModel.onMessageTextChange("$messageText {phone} ")
                                        insertFieldMenuExpanded = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // 3. Save as Template Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenSaveTemplate() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Save as Template",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0066FF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Counters Row: Characters & GSM-7 SMS parts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = "$charCount characters",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "$smsParts SMS (GSM-7)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Live Preview (First Recipient) Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Preview Header with Recipient Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Preview (First Recipient)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF111827)
                            )
                        }

                        // Recipient Switcher Dropdown
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF3F4F6),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (selectedContacts.isNotEmpty()) {
                                            previewMenuExpanded = true
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = activePreviewContact.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF374151),
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = previewMenuExpanded,
                                onDismissRequest = { previewMenuExpanded = false }
                            ) {
                                selectedContacts.forEachIndexed { idx, c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            selectedPreviewContactIndex = idx
                                            previewMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Rendered Message Content Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = renderedPreviewText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B),
                                lineHeight = 19.sp
                            )
                        )
                    }
                }
            }
        }

        // Bottom Navigation Buttons: "Back" & "Next ->"
        item {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Back Button
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier
                        .weight(0.7f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066FF)
                    )
                }

                // Next Button
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Next",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. STEP 3: REVIEW & SEND VIEW
// ==========================================

@Composable
private fun StepReviewAndSendView(
    viewModel: MainViewModel,
    selectedContacts: List<ContactItem>,
    selectedCount: Int,
    messageText: String,
    charCount: Int,
    smsParts: Int,
    defaultSimCarrier: String,
    throttleDelayMs: Long,
    onEditRecipients: () -> Unit,
    onEditMessage: () -> Unit,
    onChangeSim: () -> Unit,
    onChangeDispatch: () -> Unit,
    onSendDispatch: () -> Unit
) {
    var recipientsExpanded by remember { mutableStateOf(true) }
    var messageExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Title & Description
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Review & Send",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = "Please check the details before sending.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }

        // Review Card 1: Recipients
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Recipients",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                                Text(
                                    text = "$selectedCount recipients",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF111827)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEditRecipients() }
                            ) {
                                Text(
                                    text = "Edit",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0066FF),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { recipientsExpanded = !recipientsExpanded },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (recipientsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Expandable Recipient Rows
                    AnimatedVisibility(visible = recipientsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedContacts.forEach { contact ->
                                val initials = remember(contact.name) {
                                    val parts = contact.name.trim().split(" ")
                                    if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                                    else contact.name.take(2).uppercase()
                                }

                                val avatarColor = remember(contact.name) {
                                    when {
                                        contact.name.contains("Arjun", ignoreCase = true) -> Color(0xFF8B5CF6)
                                        contact.name.contains("Suresh", ignoreCase = true) -> Color(0xFF10B981)
                                        contact.name.contains("Neha", ignoreCase = true) -> Color(0xFF3B82F6)
                                        else -> Color(0xFF8B5CF6)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = contact.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF111827)
                                        )
                                        Text(
                                            text = contact.phoneNumber,
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Review Card 2: Message
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Message",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                                Text(
                                    text = "$charCount characters • $smsParts SMS (GSM-7)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEditMessage() }
                            ) {
                                Text(
                                    text = "Edit",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0066FF),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { messageExpanded = !messageExpanded },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (messageExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = messageExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = messageText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Review Card 3: Sending via (SIM)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = null,
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Sending via",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = defaultSimCarrier,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF111827)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onChangeSim() }
                    ) {
                        Text(
                            text = "Change",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Review Card 4: Dispatch Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Dispatch Settings",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            val delaySec = throttleDelayMs / 1000f
                            Text(
                                text = if (delaySec > 0) "Send now • ${delaySec}s pacing" else "Send now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF111827)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onChangeDispatch() }
                    ) {
                        Text(
                            text = "Change",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Bottom Action Button: Solid Green "Send SMS to X Recipients"
        item {
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSendDispatch,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send SMS to $selectedCount Recipients",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Messages will be sent using your phone's SIM. Standard carrier charges may apply.",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==========================================
// MODAL DIALOG IMPLEMENTATIONS
// ==========================================

@Composable
private fun ContactPickerModal(
    allContacts: List<ContactItem>,
    selectedContactIds: Set<String>,
    onToggleContact: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(allContacts, query) {
        if (query.isBlank()) allContacts
        else allContacts.filter {
            it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Contacts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF111827)
                )

                // Search Box
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search name or number...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0066FF),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )

                // Select All / Clear All Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onSelectAll(true) }) {
                        Text("Select All", fontSize = 12.sp, color = Color(0xFF0066FF), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { onSelectAll(false) }) {
                        Text("Deselect All", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { contact ->
                    val isChecked = selectedContactIds.contains(contact.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleContact(contact.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleContact(contact.id) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0066FF))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF111827))
                            Text(contact.phoneNumber, fontSize = 11.5.sp, color = Color(0xFF6B7280))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
            ) {
                Text("Done (${selectedContactIds.size} selected)", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ManualNumberDialog(
    onAddNumber: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enter Number Manually",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF111827)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number(s)") },
                    placeholder = { Text("e.g. 9812345678, 9876543210") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name (Optional)") },
                    placeholder = { Text("e.g. Student / Client") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phoneInput.isNotBlank()) {
                        // Supports comma or space separated numbers
                        val numbers = phoneInput.split(",", "\n", " ").map { it.trim() }.filter { it.isNotBlank() }
                        numbers.forEach { num ->
                            onAddNumber(nameInput, num)
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
            ) {
                Text("Add Recipient", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TemplatePickerSheet(
    templates: List<MessageTemplate>,
    onSelectTemplate: (MessageTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Message Template",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF111827)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTemplate(template) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = template.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = template.category,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF0066FF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = template.content,
                                fontSize = 11.5.sp,
                                color = Color(0xFF4B5563),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SaveTemplateDialog(
    currentMessage: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Custom") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save as New Template",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF111827)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Template Title") },
                    placeholder = { Text("e.g. Class Reminder") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g. Reminders / Business") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, category)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
            ) {
                Text("Save Template", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmojiPickerDialog(
    onSelectEmoji: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf("👋", "😊", "📅", "📢", "⏰", "📚", "🔔", "👍", "🎉", "🔥", "✅", "💡", "🚀", "✨", "🙏", "⚠️")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quick Emojis",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                emojis.take(8).forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectEmoji(emoji) }
                            .padding(4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
