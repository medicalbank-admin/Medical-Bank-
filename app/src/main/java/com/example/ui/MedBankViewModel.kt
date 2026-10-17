package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MedBankDatabase
import com.example.data.models.*
import com.example.data.repository.MedBankRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.UUID

enum class ScreenTab {
    EXPLORER,
    MY_RESULTS,
    ADMIN,
    PROFILE
}

data class ExamPlayerState(
    val exam: Exam? = null,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, Int> = emptyMap(), // qid -> selectedIndex (0..3)
    val markedForReview: Set<String> = emptySet(), // qid set
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)

data class ReviewData(
    val result: ExamResult,
    val exam: Exam,
    val questions: List<Question>,
    val answers: Map<String, PrivateAnswer> // qid -> PrivateAnswer
)

data class AccessSheetData(
    val exam: Exam,
    val plan: MembershipPlan?,
    val isPending: Boolean = false
)

class MedBankViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MedBankDatabase.getInstance(application)
    val repository = MedBankRepository(application, db.dao())

    // Stable Device ID
    val deviceId: String
    val deviceModel: String

    init {
        val prefs = application.getSharedPreferences("medbank_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("stable_device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("stable_device_id", id).apply()
        }
        deviceId = id
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    // App Settings & Auth State
    val appSettings: StateFlow<AppSettings> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Navigation & Tabs
    private val _activeTab = MutableStateFlow(ScreenTab.EXPLORER)
    val activeTab: StateFlow<ScreenTab> = _activeTab.asStateFlow()

    // Explorer State
    private val _selectedProgramType = MutableStateFlow("mbbs") // "mbbs" or "licensing"
    val selectedProgramType: StateFlow<String> = _selectedProgramType.asStateFlow()

    private val _selectedParentId = MutableStateFlow<String?>(null) // e.g. "year_1" or "usmle"
    val selectedParentId: StateFlow<String?> = _selectedParentId.asStateFlow()

    private val _selectedParentName = MutableStateFlow<String>("")
    val selectedParentName: StateFlow<String> = _selectedParentName.asStateFlow()

    private val _selectedSubject = MutableStateFlow<Subject?>(null)
    val selectedSubject: StateFlow<Subject?> = _selectedSubject.asStateFlow()

    // Exam Player & Timer
    private val _playerState = MutableStateFlow(ExamPlayerState())
    val playerState: StateFlow<ExamPlayerState> = _playerState.asStateFlow()

    private var timerJob: Job? = null
    private var examStartedTimestamp: Long = 0L

    // Latest Exam Result
    private val _latestResult = MutableStateFlow<ExamResult?>(null)
    val latestResult: StateFlow<ExamResult?> = _latestResult.asStateFlow()

    // Review Mode State
    private val _reviewData = MutableStateFlow<ReviewData?>(null)
    val reviewData: StateFlow<ReviewData?> = _reviewData.asStateFlow()

    // Access Request Sheet State
    private val _accessSheet = MutableStateFlow<AccessSheetData?>(null)
    val accessSheet: StateFlow<AccessSheetData?> = _accessSheet.asStateFlow()

    private val _accessRequestStatusMsg = MutableStateFlow<String?>(null)
    val accessRequestStatusMsg: StateFlow<String?> = _accessRequestStatusMsg.asStateFlow()

    private val _pendingAccessRequests = MutableStateFlow<Set<String>>(emptySet())
    val pendingAccessRequests: StateFlow<Set<String>> = _pendingAccessRequests.asStateFlow()

    // Admin State
    private val _adminStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val adminStats: StateFlow<Map<String, Int>> = _adminStats.asStateFlow()

    init {
        // Observe user changes to update currentUser reactively
        viewModelScope.launch {
            currentUser.filterNotNull().collectLatest { user ->
                // Observe user record for entitlements (SnapshotListener)
                launch {
                    repository.observeUser(user.uid).collect { updated ->
                        if (updated != null) {
                            _currentUser.value = updated
                        }
                    }
                }

                // Observe pending access requests
                launch {
                    repository.observePendingRequestsForUser(user.uid).collect { requests ->
                        _pendingAccessRequests.value = requests.map { it.scopeId }.toSet()
                    }
                }
            }
        }
    }

    fun setActiveTab(tab: ScreenTab) {
        _activeTab.value = tab
        if (tab == ScreenTab.ADMIN) {
            refreshAdminStats()
        }
    }

    fun clearAuthMessages() {
        _authError.value = null
        _authSuccessMessage.value = null
    }

    // --- Authentication ---
    fun register(
        fullName: String,
        cnic: String,
        email: String,
        phone: String,
        country: String,
        programType: String,
        mbbsYear: Int?,
        licensingPath: String?,
        password: String
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _authSuccessMessage.value = null

            val result = repository.registerUser(
                fullName = fullName,
                cnic = cnic,
                email = email,
                phone = phone,
                country = country,
                photoBase64 = "",
                programType = programType,
                mbbsYear = mbbsYear,
                licensingPath = licensingPath,
                password = password,
                deviceId = deviceId,
                deviceModel = deviceModel
            )

            _isAuthLoading.value = false
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authSuccessMessage.value = "Welcome to MedBank, ${user.fullName}!"
                    if (user.role == "admin") {
                        // Scaffold standard folder tree structure on initial admin bootstrap
                        repository.scaffoldDefaultStructureIfRequested()
                    }
                },
                onFailure = { err ->
                    _authError.value = err.message ?: "Registration failed."
                }
            )
        }
    }

    fun login(cnic: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _authSuccessMessage.value = null

            val result = repository.login(cnic, pass, deviceId, deviceModel)
            _isAuthLoading.value = false
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authSuccessMessage.value = "Signed in successfully."
                },
                onFailure = { err ->
                    _authError.value = err.message ?: "Login failed."
                }
            )
        }
    }

    fun forgotPassword(cnic: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val res = repository.forgotPassword(cnic)
            _isAuthLoading.value = false
            res.fold(
                onSuccess = { msg ->
                    _authSuccessMessage.value = msg
                },
                onFailure = { err ->
                    _authError.value = err.message ?: "Failed to resolve CNIC."
                }
            )
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeTab.value = ScreenTab.EXPLORER
        _selectedParentId.value = null
        _selectedSubject.value = null
        _playerState.value = ExamPlayerState()
        _reviewData.value = null
    }

    // --- Content Navigation ---
    fun selectProgramType(type: String) {
        _selectedProgramType.value = type
        _selectedParentId.value = null
        _selectedSubject.value = null
    }

    fun selectParent(id: String, name: String) {
        _selectedParentId.value = id
        _selectedParentName.value = name
        _selectedSubject.value = null
    }

    fun selectSubject(subject: Subject) {
        _selectedSubject.value = subject
    }

    fun navigateBackExplorer(): Boolean {
        if (_selectedSubject.value != null) {
            _selectedSubject.value = null
            return true
        }
        if (_selectedParentId.value != null) {
            _selectedParentId.value = null
            return true
        }
        return false
    }

    // --- Entitlement & Locking Check ---
    fun isExamUnlocked(exam: Exam): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role == "admin") return true // Admin has full access

        val scopeId = "${exam.parentType}_${exam.parentId}"
        val ent = user.entitlements[scopeId]
        
        return ent != null && ent.status == "active" && ent.expiresAt > System.currentTimeMillis()
    }

    fun openAccessRequestSheet(exam: Exam) {
        viewModelScope.launch {
            val planId = exam.accessPlanIds.firstOrNull()
            val plan = if (planId != null) db.dao().getPlanById(planId)?.let {
                MembershipPlan(it.id, it.name, it.price, it.currency, it.durationDays, it.scopeType)
            } else null
            _accessSheet.value = AccessSheetData(exam = exam, plan = plan)
        }
    }

    fun closeAccessRequestSheet() {
        _accessSheet.value = null
        _accessRequestStatusMsg.value = null
    }

    fun submitAccessRequest(paymentProofSimulated: String, launchWhatsApp: Boolean, context: Context) {
        val sheet = _accessSheet.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            val plan = sheet.plan
            val planName = plan?.name ?: "Full Access Pass"
            val planId = plan?.id ?: "standard_plan"

            val scopeId = "${sheet.exam.parentType}_${sheet.exam.parentId}"

            repository.createAccessRequest(
                uid = user.uid,
                studentName = user.fullName,
                cnic = user.cnic,
                scopeType = sheet.exam.parentType,
                scopeId = scopeId,
                planId = planId,
                paymentProofBase64 = paymentProofSimulated,
                whatsappSent = launchWhatsApp
            )

            if (launchWhatsApp) {
                val settings = repository.getSettings()
                val adminPhone = settings.adminWhatsapp.replace("+", "").replace(" ", "").trim()
                val message = "Hello MedBank Admin,\nI have submitted an access request.\nName: ${user.fullName}\nCNIC: ${user.cnic}\nExam: ${sheet.exam.title}\nPlan: $planName"
                val encodedMsg = URLEncoder.encode(message, "UTF-8")
                val waUrl = "https://wa.me/$adminPhone?text=$encodedMsg"

                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }

            _accessRequestStatusMsg.value = "Access request submitted! Admin will verify and activate."
            // NOTE: UI layer must observe accessRequestStatusMsg and show Snackbar
            delay(1800)
            closeAccessRequestSheet()
        }
    }

    // --- Exam Player ---
    fun startExam(exam: Exam) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val pastResults = repository.getPastResultsForExam(user.uid, exam.id)
            if (pastResults.size >= exam.maxAttempts) {
                _playerState.value = ExamPlayerState(
                    error = "Maximum attempts reached (${pastResults.size}/${exam.maxAttempts})."
                )
                return@launch
            }

            val questions = repository.getQuestionsForExam(exam.id)
            if (questions.isEmpty()) {
                _playerState.value = ExamPlayerState(
                    error = "This exam currently has no questions."
                )
                return@launch
            }

            examStartedTimestamp = System.currentTimeMillis()
            _playerState.value = ExamPlayerState(
                exam = exam,
                questions = questions,
                currentIndex = 0,
                answers = emptyMap(),
                markedForReview = emptySet(),
                remainingSeconds = exam.durationMin * 60,
                isRunning = true,
                isSubmitting = false,
                isCompleted = false,
                error = null
            )

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_playerState.value.remainingSeconds > 0 && _playerState.value.isRunning) {
                delay(1000)
                val newSec = _playerState.value.remainingSeconds - 1
                _playerState.value = _playerState.value.copy(remainingSeconds = newSec)
                if (newSec <= 0) {
                    // Auto submit on time elapsed
                    submitActiveExam()
                    break
                }
            }
        }
    }

    fun selectAnswer(qid: String, optionIndex: Int) {
        val cur = _playerState.value
        val updated = cur.answers.toMutableMap()
        if (updated[qid] == optionIndex) {
            updated.remove(qid) // toggle unselect
        } else {
            updated[qid] = optionIndex
        }
        _playerState.value = cur.copy(answers = updated)
    }

    fun toggleMarkForReview(qid: String) {
        val cur = _playerState.value
        val set = cur.markedForReview.toMutableSet()
        if (set.contains(qid)) {
            set.remove(qid)
        } else {
            set.add(qid)
        }
        _playerState.value = cur.copy(markedForReview = set)
    }

    fun jumpToQuestion(index: Int) {
        val cur = _playerState.value
        if (index in cur.questions.indices) {
            _playerState.value = cur.copy(currentIndex = index)
        }
    }

    fun nextQuestion() {
        jumpToQuestion(_playerState.value.currentIndex + 1)
    }

    fun previousQuestion() {
        jumpToQuestion(_playerState.value.currentIndex - 1)
    }

    fun submitActiveExam() {
        val cur = _playerState.value
        val exam = cur.exam ?: return
        val user = _currentUser.value ?: return

        timerJob?.cancel()
        _playerState.value = cur.copy(isRunning = false, isSubmitting = true)

        viewModelScope.launch {
            val studentAnswersList = cur.questions.map { q ->
                StudentAnswer(qid = q.id, selectedIndex = cur.answers[q.id] ?: -1)
            }

            val result = repository.submitExam(
                uid = user.uid,
                examId = exam.id,
                studentAnswers = studentAnswersList,
                startedAt = examStartedTimestamp,
                deviceId = deviceId
            )

            result.fold(
                onSuccess = { res ->
                    _latestResult.value = res
                    _playerState.value = cur.copy(isSubmitting = false, isCompleted = true)
                    // Prepare review data
                    loadReview(res.id)
                },
                onFailure = { err ->
                    _playerState.value = cur.copy(isSubmitting = false, error = err.message)
                }
            )
        }
    }

    fun exitExam() {
        timerJob?.cancel()
        _playerState.value = ExamPlayerState()
        _reviewData.value = null
    }

    fun resetExplorerNavigation() {
        _selectedParentId.value = null
        _selectedSubject.value = null
    }

    // --- Review Mode ---
    fun loadReview(resultId: String) {
        viewModelScope.launch {
            val result = repository.getResultById(resultId) ?: return@launch
            val exam = repository.getExamById(result.examId) ?: return@launch
            val questions = repository.getQuestionsForExam(exam.id)
            val answers = repository.getAnswersForExam(exam.id).associateBy { it.qid }

            _reviewData.value = ReviewData(
                result = result,
                exam = exam,
                questions = questions,
                answers = answers
            )
        }
    }

    fun clearReview() {
        _reviewData.value = null
    }

    // --- Admin Operations ---
    fun refreshAdminStats() {
        viewModelScope.launch {
            _adminStats.value = repository.getAdminDashboardStats()
        }
    }

    fun approveRequest(req: AccessRequest, adminNote: String) {
        viewModelScope.launch {
            repository.updateRequestStatus(req, isApproved = true, adminNote = adminNote)
            refreshAdminStats()
        }
    }

    fun rejectRequest(req: AccessRequest, adminNote: String) {
        viewModelScope.launch {
            repository.updateRequestStatus(req, isApproved = false, adminNote = adminNote)
            refreshAdminStats()
        }
    }

    fun resetDevice(uid: String) {
        viewModelScope.launch {
            repository.resetUserDevice(uid)
        }
    }

    fun promoteUser(uid: String) {
        viewModelScope.launch {
            repository.promoteUserToAdmin(uid)
        }
    }
}
