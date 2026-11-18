package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiModelTier
import com.example.data.ai.AudioRecorderHelper
import com.example.data.ai.GeminiAiRepository
import com.example.data.ai.SmartTriageSuggestion
import com.example.data.ai.VoiceComplaintExtraction
import com.example.data.firebase.CitizenFeedback
import com.example.data.firebase.Complaint
import com.example.data.firebase.ComplaintHistoryEntry
import com.example.data.firebase.ComplaintStatus
import com.example.data.firebase.FundRequest
import com.example.data.firebase.GovernmentScheme
import com.example.data.firebase.GramConnectRepository
import com.example.data.firebase.NotificationItem
import com.example.data.firebase.OfficialTelanganaCatalog
import com.example.data.firebase.PriorityLevel
import com.example.data.firebase.ProblemDepartment
import com.example.data.firebase.PublicProject
import com.example.data.firebase.RationRecord
import com.example.data.firebase.RationSummary
import com.example.data.firebase.RoleRequest
import com.example.data.firebase.SchemeApplication
import com.example.data.firebase.StakeholderRole
import com.example.data.firebase.UserProfile
import com.example.data.firebase.WorkflowStage
import com.example.data.local.DistrictEntity
import com.example.data.local.LocalGeoRepository
import com.example.data.local.MandalEntity
import com.example.data.local.VillageEntity
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "GramConnectVM"

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class ChatMessage(
  val id: String,
  val isFromUser: Boolean,
  val text: String,
  val isStreaming: Boolean = false,
)

class GramConnectViewModel(
  private val repo: GramConnectRepository,
  private val geoRepo: LocalGeoRepository,
  private val aiRepo: GeminiAiRepository,
  val currentUserId: String,
  private val currentUserName: String,
  private val currentUserEmail: String,
) : ViewModel() {

  private val isBootstrappedAdmin: Boolean = currentUserEmail == "varunbanoth14@gmail.com"
  private val audioRecorder = AudioRecorderHelper()
  private var recordingJob: Job? = null

  private val _selectedDistrictId = MutableStateFlow("warangal")
  val selectedDistrictId: StateFlow<String> = _selectedDistrictId.asStateFlow()

  private val _selectedMandalId = MutableStateFlow("wardhannapet")
  val selectedMandalId: StateFlow<String> = _selectedMandalId.asStateFlow()

  private val _isTelugu = MutableStateFlow(false)
  val isTelugu: StateFlow<Boolean> = _isTelugu.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _isSubmittingComplaint = MutableStateFlow(false)
  val isSubmittingComplaint: StateFlow<Boolean> = _isSubmittingComplaint.asStateFlow()

  private val _isRecordingVoice = MutableStateFlow(false)
  val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

  private val _isTranscribingVoice = MutableStateFlow(false)
  val isTranscribingVoice: StateFlow<Boolean> = _isTranscribingVoice.asStateFlow()

  private val _voiceResult = MutableStateFlow<VoiceComplaintExtraction?>(null)
  val voiceResult: StateFlow<VoiceComplaintExtraction?> = _voiceResult.asStateFlow()

  private val _triageSuggestion = MutableStateFlow<SmartTriageSuggestion?>(null)
  val triageSuggestion: StateFlow<SmartTriageSuggestion?> = _triageSuggestion.asStateFlow()

  private val _isTriaging = MutableStateFlow(false)
  val isTriaging: StateFlow<Boolean> = _isTriaging.asStateFlow()

  private val _aiTier = MutableStateFlow(AiModelTier.STANDARD)
  val aiTier: StateFlow<AiModelTier> = _aiTier.asStateFlow()

  private val _chatMessages =
    MutableStateFlow(
      listOf(
        ChatMessage(
          id = "welcome",
          isFromUser = false,
          text =
            "Namaskaram! I am GramSahayak, your Telangana GramConnect governance assistant. Ask me about village grievance escalation, Rythu Bharosa / Gruha Jyothi / Mahalakshmi schemes, or PDS Ration entitlements in English or Telugu.",
        )
      )
    )
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  private val _isAiReplying = MutableStateFlow(false)
  val isAiReplying: StateFlow<Boolean> = _isAiReplying.asStateFlow()

  private val _aiError = MutableStateFlow<String?>(null)
  val aiError: StateFlow<String?> = _aiError.asStateFlow()

  // Local Room Telangana 33-district hierarchy
  val districts: StateFlow<List<DistrictEntity>> =
    geoRepo.districts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val mandals: StateFlow<List<MandalEntity>> =
    _selectedDistrictId
      .flatMapLatest { geoRepo.mandalsFor(it) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val villages: StateFlow<List<VillageEntity>> =
    _selectedMandalId
      .flatMapLatest { geoRepo.villagesFor(it) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load $label"))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val userProfile: StateFlow<UiState<UserProfile?>> =
    repo.observeUserProfile(currentUserId).toUiState("profile")

  val complaints: StateFlow<UiState<List<Complaint>>> =
    repo.observeComplaintsForUser(currentUserId, isBootstrappedAdmin).toUiState("complaints")

  val mySubmittedGrievances: StateFlow<UiState<List<Complaint>>> =
    repo.observeMySubmittedGrievances(currentUserId).toUiState("my grievances")

  val fieldTasks: StateFlow<UiState<List<Complaint>>> =
    repo.observeFieldWorkerTasks(currentUserId).toUiState("field tasks")

  val fundRequests: StateFlow<UiState<List<FundRequest>>> =
    repo.observeFundRequests(currentUserId, isBootstrappedAdmin).toUiState("fund requests")

  val publicProjects: StateFlow<UiState<List<PublicProject>>> =
    repo
      .observePublicProjects(currentUserId)
      .map { list ->
        if (list.isEmpty()) OfficialTelanganaCatalog.publicProjects("official").map { it.second } else list
      }
      .toUiState("public projects")

  val schemes: StateFlow<UiState<List<GovernmentScheme>>> =
    repo
      .observeSchemes(currentUserId)
      .map { list ->
        if (list.isEmpty()) OfficialTelanganaCatalog.schemes.map { it.second } else list
      }
      .toUiState("schemes")

  val rationSummaries: StateFlow<UiState<List<RationSummary>>> =
    repo
      .observeRationSummaries(currentUserId)
      .map { list ->
        if (list.isEmpty()) OfficialTelanganaCatalog.rationSummaries.map { it.second } else list
      }
      .toUiState("ration summaries")

  val schemeApplications: StateFlow<UiState<List<SchemeApplication>>> =
    repo.observeUserSchemeApplications(currentUserId).toUiState("scheme applications")

  val rationRecords: StateFlow<UiState<List<RationRecord>>> =
    repo.observeUserRationRecords(currentUserId).toUiState("ration records")

  val notifications: StateFlow<UiState<List<NotificationItem>>> =
    repo.observeNotifications(currentUserId).toUiState("notifications")

  val feedbackList: StateFlow<UiState<List<CitizenFeedback>>> =
    repo.observePublishedFeedback(currentUserId).toUiState("feedback")

  val roleRequests: StateFlow<UiState<List<RoleRequest>>> =
    repo.observeRoleRequests(currentUserId, isBootstrappedAdmin).toUiState("role requests")

  init {
    viewModelScope.launch {
      geoRepo.ensureSeeded()
      repo.ensureUserProfile(currentUserId, currentUserName, currentUserEmail).onFailure {
        _statusMessage.value = it.localizedMessage ?: "Failed to initialize profile."
      }
      if (isBootstrappedAdmin) {
        repo.seedInitialOfficialCatalogIfAdmin()
      }
    }
  }

  fun selectDistrict(districtId: String) {
    _selectedDistrictId.value = districtId
    viewModelScope.launch {
      geoRepo.mandalsFor(districtId).collect { list ->
        if (list.isNotEmpty() && list.none { it.mandalId == _selectedMandalId.value }) {
          _selectedMandalId.value = list.first().mandalId
        }
        return@collect
      }
    }
  }

  fun selectMandal(mandalId: String) {
    _selectedMandalId.value = mandalId
  }

  fun toggleLanguage() {
    _isTelugu.value = !_isTelugu.value
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  fun showMessage(msg: String) {
    _statusMessage.value = msg
  }

  fun observeHistory(complaintId: String): Flow<List<ComplaintHistoryEntry>> =
    repo.observeComplaintHistory(complaintId, currentUserId)

  fun submitComplaint(
    title: String,
    description: String,
    category: ProblemDepartment,
    subCategory: String,
    priority: PriorityLevel,
    district: DistrictEntity,
    mandal: MandalEntity,
    village: VillageEntity,
    wardId: String,
    latitude: Double?,
    longitude: Double?,
    photoUriString: String?,
    onSuccess: (String) -> Unit,
  ) {
    if (_isSubmittingComplaint.value) return
    viewModelScope.launch {
      _isSubmittingComplaint.value = true
      try {
        val profile = (userProfile.value as? UiState.Success)?.data
        val citizenName = profile?.displayName ?: currentUserName.ifBlank { "Citizen" }
        val result =
          repo.submitComplaint(
            citizenName = citizenName,
            title = title,
            description = description,
            category = category,
            subCategory = subCategory,
            priority = priority,
            districtId = district.districtId,
            districtName = district.name,
            mandalId = mandal.mandalId,
            mandalName = mandal.name,
            villageId = village.villageId,
            villageName = village.name,
            wardId = wardId,
            latitude = latitude,
            longitude = longitude,
            photoUriString = photoUriString,
            aiSummary = _triageSuggestion.value?.summary,
          )
        result
          .onSuccess { trackingCode ->
            _triageSuggestion.value = null
            _voiceResult.value = null
            _statusMessage.value = "Grievance registered: $trackingCode"
            onSuccess(trackingCode)
          }
          .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to submit complaint." }
      } finally {
        _isSubmittingComplaint.value = false
      }
    }
  }

  fun transitionComplaint(
    complaint: Complaint,
    actorRole: StakeholderRole,
    nextStatus: ComplaintStatus,
    nextStage: WorkflowStage,
    remarks: String,
    assignedWorkerId: String? = complaint.assignedWorkerId,
    assignedWorkerName: String? = complaint.assignedWorkerName,
    estimatedCostInr: Long = complaint.estimatedCostInr,
    fundsRequired: Boolean = complaint.fundsRequired,
  ) {
    viewModelScope.launch {
      val profile = (userProfile.value as? UiState.Success)?.data
      val actorName = profile?.displayName ?: currentUserName
      repo
        .transitionComplaintWorkflow(
          complaint = complaint,
          actorName = actorName,
          actorRole = actorRole,
          nextStatus = nextStatus,
          nextStage = nextStage,
          remarks = remarks,
          assignedWorkerId = assignedWorkerId,
          assignedWorkerName = assignedWorkerName,
          estimatedCostInr = estimatedCostInr,
          fundsRequired = fundsRequired,
        )
        .onSuccess { _statusMessage.value = "Updated ${complaint.trackingCode} -> ${nextStatus.label}" }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Action not permitted for current role." }
    }
  }

  fun createFundRequest(
    complaint: Complaint,
    projectTitle: String,
    fundSource: String,
    estimatedCostInr: Long,
    requestedAmountInr: Long,
    remarks: String,
  ) {
    viewModelScope.launch {
      val profile = (userProfile.value as? UiState.Success)?.data
      val name = profile?.displayName ?: currentUserName
      repo
        .createFundRequest(
          complaint = complaint,
          requesterName = name,
          projectTitle = projectTitle,
          fundSource = fundSource,
          estimatedCostInr = estimatedCostInr,
          requestedAmountInr = requestedAmountInr,
          remarks = remarks,
        )
        .onSuccess { _statusMessage.value = "Fund request submitted to Mandal/District queue." }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Fund request requires verified officer/admin." }
    }
  }

  fun advanceFundRequest(
    fundRequest: FundRequest,
    nextStage: String,
    nextStatus: String,
    sanctionedAmountInr: Long,
    expenditureInr: Long,
    remarks: String,
  ) {
    viewModelScope.launch {
      repo
        .advanceFundRequest(
          fundRequest = fundRequest,
          nextStage = nextStage,
          nextStatus = nextStatus,
          sanctionedAmountInr = sanctionedAmountInr,
          expenditureInr = expenditureInr,
          remarks = remarks,
        )
        .onSuccess { _statusMessage.value = "Fund status updated to $nextStatus." }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Unable to update fund status." }
    }
  }

  fun applyForSchemeTracking(scheme: GovernmentScheme, villageName: String, refNo: String) {
    viewModelScope.launch {
      repo
        .submitSchemeApplicationTracking(scheme, villageName, refNo, "Submitted via GramConnect")
        .onSuccess { _statusMessage.value = "Application reference added for ${scheme.title}." }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not save application." }
    }
  }

  fun addRationRecord(
    cardReference: String,
    shopCode: String,
    monthLabel: String,
    entitledKg: Long,
    receivedKg: Long,
  ) {
    viewModelScope.launch {
      repo
        .addPersonalRationRecord(cardReference, shopCode, monthLabel, entitledKg, receivedKg)
        .onSuccess { _statusMessage.value = "Ration distribution record saved." }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not save ration record." }
    }
  }

  fun submitFeedback(
    targetId: String,
    targetTitle: String,
    targetType: String,
    districtName: String,
    villageName: String,
    rating: Int,
    comment: String,
  ) {
    viewModelScope.launch {
      repo
        .submitFeedback(targetId, targetTitle, targetType, districtName, villageName, rating, comment)
        .onSuccess { _statusMessage.value = "Verified citizen feedback published." }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to submit feedback." }
    }
  }

  fun submitRoleRequest(
    requestedRole: StakeholderRole,
    workId: String,
    department: ProblemDepartment,
    districtId: String,
    mandalId: String,
    villageId: String,
    wardId: String,
  ) {
    viewModelScope.launch {
      val profile = (userProfile.value as? UiState.Success)?.data
      repo
        .submitRoleRequest(
          applicantName = profile?.displayName ?: currentUserName,
          requestedRole = requestedRole,
          officialWorkId = workId,
          department = department,
          districtId = districtId,
          mandalId = mandalId,
          villageId = villageId,
          wardId = wardId,
        )
        .onSuccess { _statusMessage.value = "Official verification request submitted to Central Admin." }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to submit role request." }
    }
  }

  fun reviewRoleRequest(req: RoleRequest, approve: Boolean, notes: String) {
    viewModelScope.launch {
      repo
        .reviewRoleRequest(req, approve, notes)
        .onSuccess {
          _statusMessage.value = if (approve) "Approved ${req.applicantName}" else "Rejected request"
        }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Admin privileges required." }
    }
  }

  fun updateJurisdiction(
    displayName: String,
    districtId: String,
    mandalId: String,
    villageId: String,
    wardId: String,
  ) {
    viewModelScope.launch {
      repo
        .updateUserJurisdiction(
          uid = currentUserId,
          displayName = displayName,
          districtId = districtId,
          mandalId = mandalId,
          villageId = villageId,
          wardId = wardId,
          preferredLang = if (_isTelugu.value) "TE" else "EN",
        )
        .onSuccess { _statusMessage.value = "Jurisdiction profile saved." }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update profile." }
    }
  }

  fun addCustomVillageToLocalGeo(districtId: String, mandalName: String, villageName: String) {
    viewModelScope.launch {
      geoRepo.addCustomMandalOrVillage(districtId, mandalName, villageName)
      _statusMessage.value = "Added $villageName ($mandalName) to Telangana geo directory."
    }
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch { repo.markNotificationRead(currentUserId, notificationId) }
  }

  // --- Gemini AI Logic & Voice Transcription ---

  fun startVoiceRecording() {
    if (_isRecordingVoice.value) return
    _isRecordingVoice.value = true
    recordingJob =
      viewModelScope.launch {
        try {
          audioRecorder.startRecordingLoop()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          _isRecordingVoice.value = false
          _statusMessage.value = e.localizedMessage ?: "Microphone unavailable."
        }
      }
  }

  fun stopVoiceRecordingAndTranscribe(forChatOnly: Boolean = false) {
    if (!_isRecordingVoice.value) return
    _isRecordingVoice.value = false
    val wavBytes = audioRecorder.stopAndBuildWav()
    recordingJob?.cancel()
    recordingJob = null
    viewModelScope.launch {
      _isTranscribingVoice.value = true
      aiRepo
        .transcribeVoiceComplaint(wavBytes)
        .onSuccess { extracted ->
          if (forChatOnly) {
            if (extracted.transcript.isNotBlank()) {
              sendChatMessage(extracted.transcript)
            }
          } else {
            _voiceResult.value = extracted
            _statusMessage.value = "Voice transcribed with Gemini AI."
          }
        }
        .onFailure { err ->
          _statusMessage.value = err.localizedMessage ?: "Audio transcription failed."
        }
      _isTranscribingVoice.value = false
    }
  }

  fun clearVoiceResult() {
    _voiceResult.value = null
  }

  fun runFastTriage(title: String, description: String) {
    if (title.isBlank() && description.isBlank()) {
      _statusMessage.value = "Enter a problem title or description first."
      return
    }
    viewModelScope.launch {
      _isTriaging.value = true
      aiRepo
        .triageComplaintText(title, description)
        .onSuccess { _triageSuggestion.value = it }
        .onFailure { _statusMessage.value = it.localizedMessage ?: "AI triage failed." }
      _isTriaging.value = false
    }
  }

  fun setAiTier(tier: AiModelTier) {
    _aiTier.value = tier
  }

  fun sendChatMessage(prompt: String) {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isBlank() || _isAiReplying.value) return

    val currentHistory: List<Content> =
      _chatMessages.value.drop(1).map { msg ->
        content(if (msg.isFromUser) "user" else "model") { text(msg.text) }
      }

    val userMsg = ChatMessage("u_${System.currentTimeMillis()}", isFromUser = true, text = cleanPrompt)
    val replyId = "m_${System.currentTimeMillis()}"
    val placeholderMsg = ChatMessage(replyId, isFromUser = false, text = "", isStreaming = true)

    _chatMessages.value = _chatMessages.value + userMsg + placeholderMsg
    _isAiReplying.value = true
    _aiError.value = null

    viewModelScope.launch {
      try {
        aiRepo.streamChatReply(_aiTier.value, currentHistory, cleanPrompt).collect { partial ->
          _chatMessages.value =
            _chatMessages.value.map {
              if (it.id == replyId) it.copy(text = partial, isStreaming = true) else it
            }
        }
        _chatMessages.value =
          _chatMessages.value.map {
            if (it.id == replyId) it.copy(isStreaming = false) else it
          }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val errMsg = e.localizedMessage ?: "Gemini request failed."
        _aiError.value = errMsg
        _chatMessages.value =
          _chatMessages.value.map {
            if (it.id == replyId) {
              it.copy(text = "Error: $errMsg", isStreaming = false)
            } else it
          }
      } finally {
        _isAiReplying.value = false
      }
    }
  }
}
