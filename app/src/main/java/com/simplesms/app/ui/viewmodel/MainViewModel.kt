package com.simplesms.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simplesms.app.data.model.ContactItem
import com.simplesms.app.data.model.DispatchHistory
import com.simplesms.app.data.model.MessageTemplate
import com.simplesms.app.data.model.RecipientHistoryRecord
import com.simplesms.app.data.model.SendStatus
import com.simplesms.app.data.model.SimCardInfo
import com.simplesms.app.data.repository.ContactRepository
import com.simplesms.app.data.repository.HistoryRepository
import com.simplesms.app.data.repository.PreferenceRepository
import com.simplesms.app.data.repository.SimManagerHelper
import com.simplesms.app.data.repository.TemplateRepository
import com.simplesms.app.remote.WebGatewayManager
import com.simplesms.app.service.SmsDispatchService
import com.simplesms.app.ui.navigation.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefRepo = PreferenceRepository(application)
    private val contactRepo = ContactRepository(application)
    private val historyRepo = HistoryRepository(application)
    private val templateRepo = TemplateRepository(application)

    // Navigation state - Home is default landing screen as required
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Master in-memory contacts list
    private val _allContacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val allContacts: StateFlow<List<ContactItem>> = _allContacts.asStateFlow()

    // Dedicated selected contact IDs set (never wiped by searching)
    private val _selectedContactIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedContactIds: StateFlow<Set<String>> = _selectedContactIds.asStateFlow()

    // List of selected ContactItem objects (for the pinned top horizontal tray)
    val selectedContactsList: StateFlow<List<ContactItem>> = combine(
        _allContacts,
        _selectedContactIds
    ) { all, selectedIds ->
        all.filter { selectedIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time sending status map: contactId -> Pair(SendStatus, ErrorReason?)
    private val _contactStatuses = MutableStateFlow<Map<String, Pair<SendStatus, String?>>>(emptyMap())
    val contactStatuses: StateFlow<Map<String, Pair<SendStatus, String?>>> = _contactStatuses.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOnlySelected = MutableStateFlow(false)
    val filterOnlySelected: StateFlow<Boolean> = _filterOnlySelected.asStateFlow()

    // Derived reactive contacts list for the UI
    val contacts: StateFlow<List<ContactItem>> = combine(
        _allContacts,
        _selectedContactIds,
        _contactStatuses,
        _searchQuery,
        _filterOnlySelected
    ) { all, selectedIds, statuses, query, onlySelected ->
        all.filter { contact ->
            val matchesQuery = if (query.isBlank()) true else {
                contact.name.contains(query, ignoreCase = true) ||
                contact.phoneNumber.contains(query)
            }
            val matchesSelected = if (onlySelected) selectedIds.contains(contact.id) else true
            matchesQuery && matchesSelected
        }.map { contact ->
            val statusInfo = statuses[contact.id]
            contact.copy(
                isSelected = selectedIds.contains(contact.id),
                sendStatus = statusInfo?.first ?: SendStatus.IDLE,
                errorMessage = statusInfo?.second
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalContactsCount: StateFlow<Int> = _allContacts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val selectedCount: StateFlow<Int> = _selectedContactIds
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _messageText = MutableStateFlow(
        prefRepo.getDraftMessage().ifBlank {
            "Hello {name},\nThis is a friendly reminder about your upcoming class.\nPlease be on time!\n\nFrom RVS Classes"
        }
    )
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    // 3-step Send SMS workflow state (1: Recipients, 2: Message, 3: Review & Send)
    private val _sendSmsStep = MutableStateFlow(1)
    val sendSmsStep: StateFlow<Int> = _sendSmsStep.asStateFlow()

    private val _activeSimCards = MutableStateFlow<List<SimCardInfo>>(emptyList())
    val activeSimCards: StateFlow<List<SimCardInfo>> = _activeSimCards.asStateFlow()

    private val _defaultSim = MutableStateFlow<SimCardInfo?>(null)
    val defaultSim: StateFlow<SimCardInfo?> = _defaultSim.asStateFlow()

    // Dialogs & Dedicated Processing Screen State
    private val _showSimSelectDialog = MutableStateFlow(false)
    val showSimSelectDialog: StateFlow<Boolean> = _showSimSelectDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showProcessingScreen = MutableStateFlow(false)
    val showProcessingScreen: StateFlow<Boolean> = _showProcessingScreen.asStateFlow()

    private val _isDispatching = MutableStateFlow(false)
    val isDispatching: StateFlow<Boolean> = _isDispatching.asStateFlow()

    private val _dispatchProgress = MutableStateFlow(Pair(0, 0))
    val dispatchProgress: StateFlow<Pair<Int, Int>> = _dispatchProgress.asStateFlow()

    private val _activeDispatchRecipients = MutableStateFlow<List<ContactItem>>(emptyList())
    val activeDispatchRecipients: StateFlow<List<ContactItem>> = _activeDispatchRecipients.asStateFlow()

    private val _dispatchCompleted = MutableStateFlow(false)
    val dispatchCompleted: StateFlow<Boolean> = _dispatchCompleted.asStateFlow()

    // History and Templates
    private val _historyList = MutableStateFlow<List<DispatchHistory>>(emptyList())
    val historyList: StateFlow<List<DispatchHistory>> = _historyList.asStateFlow()

    private val _templatesList = MutableStateFlow<List<MessageTemplate>>(emptyList())
    val templatesList: StateFlow<List<MessageTemplate>> = _templatesList.asStateFlow()

    // Home Dashboard Statistics (Real computed metrics)
    val smsSentToday: StateFlow<Int> = _historyList.map { history ->
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        history.filter { it.timestamp >= startOfDay }.sumOf { it.successCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableTemplatesCount: StateFlow<Int> = _templatesList.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pastCampaignsCount: StateFlow<Int> = _historyList.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val campaignsThisMonth: StateFlow<Int> = _historyList.map { history ->
        val startOfMonth = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val count = history.count { it.timestamp >= startOfMonth }
        if (count > 0) count else history.size.coerceAtLeast(1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val throttleDelayMs = MutableStateFlow(prefRepo.getThrottleDelayMs())
    val isDarkMode = MutableStateFlow(prefRepo.isDarkModeEnabled())
    val darkIntensity = MutableStateFlow(prefRepo.getDarkIntensity())

    init {
        // Collect real-time status updates from dispatch service
        viewModelScope.launch {
            SmsDispatchService.contactStatusUpdates.collectLatest { event ->
                if (event != null) {
                    val current = _contactStatuses.value.toMutableMap()
                    current[event.contactId] = Pair(event.status, event.error)
                    _contactStatuses.value = current

                    // Also update active dispatch recipients list on processing screen
                    _activeDispatchRecipients.value = _activeDispatchRecipients.value.map { contact ->
                        if (contact.id == event.contactId) {
                            contact.copy(sendStatus = event.status, errorMessage = event.error)
                        } else contact
                    }
                }
            }
        }

        viewModelScope.launch {
            SmsDispatchService.isDispatching.collectLatest { running ->
                _isDispatching.value = running
                if (!running && _showProcessingScreen.value && _activeDispatchRecipients.value.isNotEmpty()) {
                    _dispatchCompleted.value = true
                    recordHistoryIfFinished()
                }
            }
        }

        viewModelScope.launch {
            SmsDispatchService.currentProgress.collectLatest { progress ->
                _dispatchProgress.value = progress
            }
        }

        // Ensure Web Gateway is actively running and listening to real-time Firestore queue & sync
        if (WebGatewayManager.isGatewayEnabled(application)) {
            WebGatewayManager.startGateway(application)
        }

        // Real-time synchronization of history from Web Gateway
        viewModelScope.launch {
            WebGatewayManager.historyUpdateTrigger.collectLatest {
                loadHistory()
            }
        }

        // Real-time synchronization of templates from Web Gateway
        viewModelScope.launch {
            WebGatewayManager.templatesUpdateTrigger.collectLatest {
                loadTemplates()
            }
        }

        // Real-time synchronization of contacts from Web Gateway
        viewModelScope.launch {
            WebGatewayManager.contactsUpdateTrigger.collectLatest {
                loadContacts()
            }
        }

        refreshData()
        loadHistory()
        loadTemplates()
    }

    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun openGateway() {
        _currentTab.value = AppTab.GATEWAY
    }

    fun refreshData() {
        refreshSims()
        loadContacts()
    }

    fun refreshSims() {
        val sims = SimManagerHelper.getActiveSimCards(getApplication())
        _activeSimCards.value = sims

        val savedSubId = prefRepo.getDefaultSimSubscriptionId()
        var selected = sims.firstOrNull { it.subscriptionId == savedSubId }

        if (selected == null && sims.isNotEmpty()) {
            if (sims.size == 1) {
                selected = sims.first()
                prefRepo.setDefaultSim(selected.subscriptionId, selected.slotIndex)
            } else {
                if (!prefRepo.isOnboardingCompleted()) {
                    _showSimSelectDialog.value = true
                } else {
                    selected = sims.first()
                }
            }
        }

        _defaultSim.value = selected
    }

    fun selectDefaultSim(sim: SimCardInfo) {
        prefRepo.setDefaultSim(sim.subscriptionId, sim.slotIndex)
        prefRepo.setOnboardingCompleted(true)
        _defaultSim.value = sim
        _showSimSelectDialog.value = false
        refreshSims()
    }

    fun setThrottleDelay(delayMs: Long) {
        prefRepo.setThrottleDelayMs(delayMs)
        throttleDelayMs.value = delayMs
    }

    fun setSendSmsStep(step: Int) {
        _sendSmsStep.value = step.coerceIn(1, 3)
    }

    fun nextSendSmsStep() {
        if (_sendSmsStep.value == 1) {
            if (_selectedContactIds.value.isEmpty()) {
                _errorMessage.value = "Please select at least one recipient"
                return
            }
            _sendSmsStep.value = 2
        } else if (_sendSmsStep.value == 2) {
            if (_messageText.value.trim().isEmpty()) {
                _errorMessage.value = "Please enter a message to send"
                return
            }
            _sendSmsStep.value = 3
        }
    }

    fun prevSendSmsStep() {
        if (_sendSmsStep.value > 1) {
            _sendSmsStep.value = _sendSmsStep.value - 1
        } else {
            switchTab(AppTab.HOME)
        }
    }

    fun addManualRecipient(name: String, phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) return
        val cleanName = name.trim().ifBlank { cleanPhone }
        val saved = contactRepo.saveCustomContact(cleanName, cleanPhone)
        val currentSelected = _selectedContactIds.value.toMutableSet()
        currentSelected.add(saved.id)
        _selectedContactIds.value = currentSelected
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            val list = contactRepo.getContacts("").toMutableList()
            if (_selectedContactIds.value.isEmpty() && list.isNotEmpty()) {
                _selectedContactIds.value = setOf(list.first().id)
            }
            _allContacts.value = list
            WebGatewayManager.syncContactsToFirestore(getApplication(), WebGatewayManager.getDeviceId(getApplication()))
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilterOnlySelected() {
        _filterOnlySelected.value = !_filterOnlySelected.value
    }

    fun onMessageTextChange(text: String) {
        _messageText.value = text
        prefRepo.saveDraftMessage(text)
    }

    fun insertNameTag() {
        val current = _messageText.value
        val updated = if (current.isEmpty() || current.endsWith(" ")) {
            "$current{name} "
        } else {
            "$current {name} "
        }
        onMessageTextChange(updated)
    }

    fun toggleContactSelection(contactId: String) {
        val current = _selectedContactIds.value.toMutableSet()
        if (current.contains(contactId)) {
            current.remove(contactId)
        } else {
            current.add(contactId)
        }
        _selectedContactIds.value = current
    }

    fun removeSelectedContact(contactId: String) {
        val current = _selectedContactIds.value.toMutableSet()
        current.remove(contactId)
        _selectedContactIds.value = current
    }

    fun selectAllVisibleContacts(select: Boolean) {
        val current = _selectedContactIds.value.toMutableSet()
        val visible = contacts.value
        if (select) {
            visible.forEach { current.add(it.id) }
        } else {
            visible.forEach { current.remove(it.id) }
        }
        _selectedContactIds.value = current
    }

    fun deselectAll() {
        _selectedContactIds.value = emptySet()
        _filterOnlySelected.value = false
    }

    fun clearSentStatuses() {
        _contactStatuses.value = emptyMap()
    }

    // --- Live Dedicated Processing Screen Logic ---
    fun startSmsDispatch() {
        val selectedIds = _selectedContactIds.value
        if (selectedIds.isEmpty()) {
            _errorMessage.value = "Please select at least one contact"
            return
        }

        val selectedContacts = _allContacts.value.filter { selectedIds.contains(it.id) }
        if (selectedContacts.isEmpty()) {
            _errorMessage.value = "Selected contacts not found"
            return
        }

        val template = _messageText.value.trim()
        if (template.isEmpty()) {
            _errorMessage.value = "Please enter a message to send"
            return
        }

        val sim = _defaultSim.value
        if (sim == null) {
            _errorMessage.value = "No default SIM card configured. Please select a SIM card."
            _showSimSelectDialog.value = true
            return
        }

        // Prepare queue
        val newStatuses = _contactStatuses.value.toMutableMap()
        selectedContacts.forEach {
            newStatuses[it.id] = Pair(SendStatus.QUEUED, null)
        }
        _contactStatuses.value = newStatuses

        _activeDispatchRecipients.value = selectedContacts.map { it.copy(sendStatus = SendStatus.QUEUED) }
        _dispatchCompleted.value = false
        _showProcessingScreen.value = true

        SmsDispatchService.start(
            context = getApplication(),
            subscriptionId = sim.subscriptionId,
            template = template,
            contacts = selectedContacts
        )
    }

    fun cancelDispatch() {
        SmsDispatchService.stop(getApplication())
        _isDispatching.value = false
    }

    fun closeProcessingScreen() {
        _showProcessingScreen.value = false
        _dispatchCompleted.value = false
    }

    fun retryContactFromProcessing(contactId: String) {
        val sim = _defaultSim.value ?: return
        val template = _messageText.value.trim()
        val target = _activeDispatchRecipients.value.firstOrNull { it.id == contactId } ?: return

        val newStatuses = _contactStatuses.value.toMutableMap()
        newStatuses[contactId] = Pair(SendStatus.QUEUED, null)
        _contactStatuses.value = newStatuses

        _activeDispatchRecipients.value = _activeDispatchRecipients.value.map {
            if (it.id == contactId) it.copy(sendStatus = SendStatus.QUEUED, errorMessage = null) else it
        }

        SmsDispatchService.start(
            context = getApplication(),
            subscriptionId = sim.subscriptionId,
            template = template,
            contacts = listOf(target)
        )
    }

    private fun recordHistoryIfFinished() {
        viewModelScope.launch {
            val list = _activeDispatchRecipients.value
            if (list.isEmpty()) return@launch

            val sim = _defaultSim.value
            val successCount = list.count { it.sendStatus == SendStatus.SENT }
            val failCount = list.count { it.sendStatus == SendStatus.FAILED }

            val recordList = list.map {
                RecipientHistoryRecord(
                    name = it.name,
                    phoneNumber = it.phoneNumber,
                    status = it.sendStatus,
                    errorMessage = it.errorMessage
                )
            }

            val history = DispatchHistory(
                message = _messageText.value,
                simCarrier = sim?.carrierName ?: "SIM",
                simSlot = sim?.slotIndex ?: 0,
                totalCount = list.size,
                successCount = successCount,
                failureCount = failCount,
                recipients = recordList
            )

            historyRepo.insertHistory(history)
            loadHistory()
            WebGatewayManager.recordDispatchToFirestore(getApplication(), history)
        }
    }

    // --- History Management ---
    fun loadHistory() {
        viewModelScope.launch {
            _historyList.value = historyRepo.getAllHistory()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepo.clearAllHistory()
            WebGatewayManager.clearFirestoreHistory(getApplication(), WebGatewayManager.getDeviceId(getApplication()))
            loadHistory()
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            historyRepo.deleteHistory(id)
            loadHistory()
        }
    }

    fun reuseHistory(history: DispatchHistory) {
        onMessageTextChange(history.message)

        // Select the same recipients
        val phoneNumbers = history.recipients.map { it.phoneNumber }.toSet()
        val matchingIds = _allContacts.value
            .filter { phoneNumbers.contains(it.phoneNumber) }
            .map { it.id }
            .toSet()

        _selectedContactIds.value = matchingIds
        _currentTab.value = AppTab.COMPOSE
    }

    // --- Template Management ---
    fun loadTemplates() {
        _templatesList.value = templateRepo.getAllTemplates()
    }

    fun applyTemplate(template: MessageTemplate) {
        onMessageTextChange(template.content)
        _currentTab.value = AppTab.COMPOSE
    }

    fun addCustomTemplate(title: String, category: String, content: String) {
        templateRepo.addCustomTemplate(title, category, content)
        loadTemplates()
        WebGatewayManager.syncTemplatesToFirestore(getApplication(), WebGatewayManager.getDeviceId(getApplication()))
    }

    fun deleteTemplate(id: String) {
        templateRepo.deleteTemplate(id)
        loadTemplates()
        WebGatewayManager.syncTemplatesToFirestore(getApplication(), WebGatewayManager.getDeviceId(getApplication()))
    }

    fun setDarkMode(enabled: Boolean) {
        prefRepo.setDarkModeEnabled(enabled)
        isDarkMode.value = enabled
    }

    fun setDarkIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0f, 1f)
        prefRepo.setDarkIntensity(clamped)
        darkIntensity.value = clamped
    }

    fun openSettings() {
        switchTab(AppTab.SETTINGS)
    }

    fun closeSettings() {
        _showSettingsDialog.value = false
    }

    fun openSimSelection() {
        _showSimSelectDialog.value = true
    }

    fun closeSimSelection() {
        _showSimSelectDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
