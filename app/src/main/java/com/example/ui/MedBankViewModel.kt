package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
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
import com.example.ui.screens.admin.AdminSection
import com.example.notifications.NotificationHelper

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

    private val _userResults = MutableStateFlow<List<ExamResult>>(emptyList())
    val userResults: StateFlow<List<ExamResult>> = _userResults.asStateFlow()


    // Review Mode State
    private val _reviewData = MutableStateFlow<ReviewData?>(null)
    val reviewData: StateFlow<ReviewData?> = _reviewData.asStateFlow()

    // Access Request Sheet State
    private val _accessSheet = MutableStateFlow<AccessSheetData?>(null)
    val accessSheet: StateFlow<AccessSheetData?> = _accessSheet.asStateFlow()

    private val _accessRequestStatusMsg = MutableStateFlow<String?>(null)
    val accessRequestStatusMsg: StateFlow<String?> = _accessRequestStatusMsg.asStateFlow()

    private val _trackPlans = MutableStateFlow<List<MembershipPlan>>(emptyList())
    val trackPlans: StateFlow<List<MembershipPlan>> = _trackPlans.asStateFlow()

    private val _isSubmittingRequest = MutableStateFlow(false)
    val isSubmittingRequest: StateFlow<Boolean> = _isSubmittingRequest.asStateFlow()

    private val _isStartingExam = MutableStateFlow<String?>(null)
    val isStartingExam: StateFlow<String?> = _isStartingExam.asStateFlow()

    private val questionsCache = mutableMapOf<String, List<Question>>()

    private val _pendingAccessRequests = MutableStateFlow<Set<String>>(emptySet())
    val pendingAccessRequests: StateFlow<Set<String>> = _pendingAccessRequests.asStateFlow()

    // Admin State
    private val _adminStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val adminStats: StateFlow<Map<String, Int>> = _adminStats.asStateFlow()

    private val _activeAdminSection = MutableStateFlow(AdminSection.DASHBOARD)
    val activeAdminSection: StateFlow<AdminSection> = _activeAdminSection.asStateFlow()

    fun setActiveAdminSection(section: AdminSection) {
        _activeAdminSection.value = section
    }

    fun navigateToAdminSection(section: AdminSection) {
        _activeAdminSection.value = section
        _activeTab.value = ScreenTab.ADMIN
    }

    // Legal Content State
    private val _legalContent = MutableStateFlow<LegalContent?>(null)
    val legalContent: StateFlow<LegalContent?> = _legalContent.asStateFlow()

    private val _isSavingLegal = MutableStateFlow(false)
    val isSavingLegal: StateFlow<Boolean> = _isSavingLegal.asStateFlow()

    private val _legalSaveSuccess = MutableStateFlow<Boolean?>(null)
    val legalSaveSuccess: StateFlow<Boolean?> = _legalSaveSuccess.asStateFlow()

    private val _activeLegalViewer = MutableStateFlow<String?>(null) // "privacy" or "terms"
    val activeLegalViewer: StateFlow<String?> = _activeLegalViewer.asStateFlow()

    private val _activeProfileScreen = MutableStateFlow<String?>(null) // null, "account", "help", "tutorial", "notifications", "device"
    val activeProfileScreen: StateFlow<String?> = _activeProfileScreen.asStateFlow()

    fun openProfileScreen(screen: String?) {
        _activeProfileScreen.value = screen
    }

    private val _adminBrowsingContent = MutableStateFlow(false)
    val adminBrowsingContent: StateFlow<Boolean> = _adminBrowsingContent.asStateFlow()

    fun setAdminBrowsingContent(browsing: Boolean) {
        _adminBrowsingContent.value = browsing
    }

    fun observeReferralTransactionsForUser(referrerUid: String): Flow<List<ReferralTransaction>> {
        return repository.observeReferralTransactionsForUser(referrerUid)
    }

    fun observePaidReferralTransactions(): Flow<List<ReferralTransaction>> {
        return repository.observePaidReferralTransactions()
    }

    fun observeApprovedAccessRequestsForUser(uid: String): Flow<List<AccessRequest>> {
        return repository.observeApprovedAccessRequestsForUser(uid)
    }

    fun observeNotifications(uid: String): Flow<List<NotificationItem>> {
        return repository.observeNotifications(uid)
    }

    fun markNotificationRead(uid: String, notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(uid, notificationId)
        }
    }

    private val _banner = MutableStateFlow(com.example.data.models.AppBanner())
    val banner: StateFlow<com.example.data.models.AppBanner> = _banner.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeBanner().collect { b ->
                _banner.value = b
            }
        }
    }

    fun saveBanner(isActive: Boolean, message: String, buttonText: String, linkUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.saveBanner(isActive, message, buttonText, linkUrl)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save banner")
            }
        }
    }




    init {
    // Observe user changes to update currentUser reactively
    viewModelScope.launch {
        currentUser.filterNotNull().collectLatest { user ->
            // REMOVED: The observeUser snapshot listener that was causing the session overwrite.

            // Observe pending access requests
            launch {
                repository.observePendingRequestsForUser(user.uid).collect { requests ->
                    _pendingAccessRequests.value = requests.map { it.scopeId }.toSet()
                }
            }

            // Observe user exam results
            launch {
                repository.observeResultsByUser(user.uid).collect { list ->
                    _userResults.value = list.sortedByDescending { it.submittedAt }.take(200)
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
        password: String,
        referralCode: String? = null
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
                deviceModel = deviceModel,
                enteredReferralCode = referralCode
            )

            _isAuthLoading.value = false
            result.fold(
                onSuccess = { outcome ->
                    _currentUser.value = outcome.user
                    if (outcome.invalidReferralCodeEntered) {
                        _authSuccessMessage.value = "Invalid referral code. Proceeding without referral."
                    } else {
                        _authSuccessMessage.value = "Welcome to MedBank, ${outcome.user.fullName}!"
                    }
                    if (outcome.user.role == "admin") {
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
fun refreshCurrentUserProfile() {
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
    viewModelScope.launch {
        val updated = repository.getUserByUid(uid)
        if (updated != null && updated.uid == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid) {
            _currentUser.value = updated
        }
    }
}
    fun logout() {
    _currentUser.value = null
    _activeTab.value = ScreenTab.EXPLORER
    _selectedParentId.value = null
    _selectedSubject.value = null
    _playerState.value = ExamPlayerState()
    _reviewData.value = null
    
    // Add these two lines to prevent data leaking between user sessions:
    _pendingAccessRequests.value = emptySet()
    _userResults.value = emptyList()
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

    // --- Multi-Level Entitlement & Locking Check ---
    fun getActiveEntitlementForExam(exam: Exam): Entitlement? {
        val user = _currentUser.value ?: return null
        if (user.role == "admin") {
            return Entitlement(planId = "admin_full_access", planName = "Admin Access", expiresAt = Long.MAX_VALUE, status = "active")
        }
        val now = System.currentTimeMillis()
        fun isActive(ent: Entitlement?): Boolean =
            ent != null && ent.status == "active" && (ent.expiresAt == 0L || ent.expiresAt > now)

        // 1. Specific exam unlock: exam_{exam.id} or {exam.id}
        val examKeys = listOf("exam_${exam.id}", exam.id)
        for (k in examKeys) {
            val ent = user.entitlements[k]
            if (isActive(ent)) return ent
        }

        // 2. Year or Pathway unlock: {exam.parentId} (e.g., "mbbs_year_1" or "licensing_nre")
        val parentKeys = listOf(
            exam.parentId,
            "${exam.parentType}_${exam.parentId}",
            if (exam.parentId.startsWith("year_")) "mbbs_${exam.parentId}" else "",
            if (exam.parentType == "mbbs" && !exam.parentId.startsWith("mbbs_")) "mbbs_${exam.parentId}" else "",
            if (exam.parentType == "licensing" && !exam.parentId.startsWith("licensing_")) "licensing_${exam.parentId}" else ""
        ).filter { it.isNotBlank() }
        for (k in parentKeys) {
            val ent = user.entitlements[k]
            if (isActive(ent)) return ent
        }

        // 3. Track unlock: track_{exam.parentType} (e.g., "track_mbbs" or "track_licensing")
        val trackKeys = listOf(
            "track_${exam.parentType}",
            exam.parentType,
            "track_${exam.parentType.removePrefix("track_")}"
        )
        for (k in trackKeys) {
            val ent = user.entitlements[k]
            if (isActive(ent)) return ent
        }

        // 4. all_access (Unlocks everything)
        val allEnt = user.entitlements["all_access"]
        if (isActive(allEnt)) return allEnt

        // 5. Check if any active entitlement has planId matching exam.accessPlanIds
        if (exam.accessPlanIds.isNotEmpty()) {
            val ent = user.entitlements.values.firstOrNull { isActive(it) && it.planId in exam.accessPlanIds }
            if (ent != null) return ent
        }

        return null
    }

    fun isExamUnlocked(exam: Exam): Boolean {
        return getActiveEntitlementForExam(exam) != null
    }

    fun isExamExpired(exam: Exam): Boolean {
        val user = _currentUser.value ?: return false
        if (isExamUnlocked(exam)) return false
        val now = System.currentTimeMillis()
        val allMatchingKeys = listOf(
            "exam_${exam.id}",
            exam.id,
            exam.parentId,
            "${exam.parentType}_${exam.parentId}",
            "track_${exam.parentType}",
            exam.parentType,
            "all_access"
        )
        return allMatchingKeys.any { k ->
            val ent = user.entitlements[k]
            ent != null && (ent.status == "expired" || (ent.expiresAt in 1..now))
        }
    }

    fun isExamPending(exam: Exam): Boolean {
        val pending = _pendingAccessRequests.value
        val keys = listOf(
            "exam_${exam.id}",
            exam.id,
            exam.parentId,
            "${exam.parentType}_${exam.parentId}",
            "track_${exam.parentType}",
            "all_access"
        )
        return keys.any { pending.contains(it) }
    }

    fun openAccessRequestSheet(exam: Exam) {
        viewModelScope.launch {
            _trackPlans.value = emptyList()
            val plans = repository.getPlansForExam(exam)
            _trackPlans.value = plans
            val defaultPlan = plans.firstOrNull()
            _accessSheet.value = AccessSheetData(exam = exam, plan = defaultPlan)
        }
    }

    fun reloadPlansForExam(exam: Exam) {
        viewModelScope.launch {
            val plans = repository.getPlansForExam(exam)
            _trackPlans.value = plans
            if (_accessSheet.value?.plan == null || plans.none { it.id == _accessSheet.value?.plan?.id }) {
                _accessSheet.value = _accessSheet.value?.copy(plan = plans.firstOrNull())
            }
        }
    }

    fun closeAccessRequestSheet() {
        _accessSheet.value = null
        _accessRequestStatusMsg.value = null
        _isSubmittingRequest.value = false
    }

    fun submitAccessRequest(paymentProofSimulated: String, launchWhatsApp: Boolean, context: Context) {
        val sheet = _accessSheet.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            _isSubmittingRequest.value = true
            val plan = sheet.plan
            val planName = plan?.name ?: "Full Access Pass"
            val planId = plan?.id ?: "standard_plan"
            val scopeType = plan?.scopeType?.ifBlank { "exam" } ?: "exam"
            val scopeId = plan?.scopeId?.ifBlank { "exam_${sheet.exam.id}" } ?: "exam_${sheet.exam.id}"

            repository.createAccessRequest(
                uid = user.uid,
                studentName = user.fullName,
                cnic = user.cnic,
                scopeType = scopeType,
                scopeId = scopeId,
                planId = planId,
                planName = planName,
                paymentProofBase64 = paymentProofSimulated,
                paymentProofUrl = paymentProofSimulated,
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

            _isSubmittingRequest.value = false
            _accessRequestStatusMsg.value = "Access request submitted! Admin will verify and activate."
            delay(1800)
            closeAccessRequestSheet()
        }
    }

    fun submitAccessRequestWithFile(
        fileUri: android.net.Uri?,
        selectedPlan: MembershipPlan,
        launchWhatsApp: Boolean,
        context: Context
    ) {
        val sheet = _accessSheet.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            _isSubmittingRequest.value = true
            val planName = selectedPlan.name
            val planId = selectedPlan.id
            val scopeType = selectedPlan.scopeType.ifBlank { "exam" }
            val scopeId = selectedPlan.scopeId.ifBlank { "exam_${sheet.exam.id}" }

            repository.uploadPaymentProofAndCreateRequest(
                uid = user.uid,
                studentName = user.fullName,
                cnic = user.cnic,
                scopeType = scopeType,
                scopeId = scopeId,
                planId = planId,
                planName = planName,
                fileUri = fileUri,
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

            _isSubmittingRequest.value = false
            _accessRequestStatusMsg.value = "Access request submitted! Admin will verify and activate."
            delay(1800)
            closeAccessRequestSheet()
        }
    }

    // --- Pre-fetch and Fast Start Exam ---
    fun preloadExamsForSubject(subject: Subject?) {
        if (subject == null) return
        viewModelScope.launch {
            val exams = repository.getExamsBySubject(subject.id)
            exams.forEach { ex ->
                if (!questionsCache.containsKey(ex.id)) {
                    val qs = repository.getQuestionsForExam(ex.id)
                    questionsCache[ex.id] = qs
                }
            }
        }
    }

    fun startExam(exam: Exam) {
        val user = _currentUser.value ?: return
        _isStartingExam.value = exam.id

        viewModelScope.launch {
            val pastResults = repository.getPastResultsForExam(user.uid, exam.id)
            if (pastResults.size >= exam.maxAttempts) {
                _isStartingExam.value = null
                _playerState.value = ExamPlayerState(
                    error = "Maximum attempts reached (${pastResults.size}/${exam.maxAttempts})."
                )
                return@launch
            }

            val questions = questionsCache[exam.id] ?: repository.getQuestionsForExam(exam.id).also {
                questionsCache[exam.id] = it
            }

            _isStartingExam.value = null

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
                deviceId = deviceId,
                cachedExam = exam,
                cachedQuestions = cur.questions
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

    fun approveRequest(req: AccessRequest, adminNote: String, overridePlan: MembershipPlan? = null) {
        viewModelScope.launch {
            repository.updateRequestStatus(req, isApproved = true, adminNote = adminNote, overridePlan = overridePlan)
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

    // --- Legal Content Operations ---
    fun loadLegalContent() {
        viewModelScope.launch {
            _legalContent.value = repository.getLegalContent()
        }
    }

    fun saveLegalContent(privacyPolicy: String, termsAndConditions: String) {
        viewModelScope.launch {
            _isSavingLegal.value = true
            val success = repository.saveLegalContent(privacyPolicy, termsAndConditions)
            if (success) {
                _legalContent.value = LegalContent(
                    privacyPolicy = privacyPolicy,
                    termsAndConditions = termsAndConditions,
                    updatedAt = System.currentTimeMillis()
                )
            }
            _isSavingLegal.value = false
            _legalSaveSuccess.value = success
        }
    }

    fun clearLegalSaveStatus() {
        _legalSaveSuccess.value = null
    }

    fun openLegalViewer(type: String) {
        _activeLegalViewer.value = type
        loadLegalContent()
    }

    fun closeLegalViewer() {
        _activeLegalViewer.value = null
    }

    // --- Referral & Payout Operations ---
    fun observeReferralsForCurrentUser(): Flow<List<User>> {
        val uid = _currentUser.value?.uid ?: return flowOf(emptyList())
        return repository.observeReferralsForUser(uid)
    }

    fun observePendingReferralTransactions(): Flow<List<ReferralTransaction>> {
        return repository.observePendingReferralTransactions()
    }

    fun markPayoutPaid(transactionId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.markReferralTransactionPaid(transactionId)
                refreshAdminStats()
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun observeRecentAccessRequests(limit: Int = 5): Flow<List<AccessRequest>> {
        return repository.observeAllAccessRequests().map { list ->
            list.sortedByDescending { it.createdAt }.take(limit)
        }
    }

    fun broadcastAlert(title: String, message: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.broadcastNotification(title, message)
                NotificationHelper.showNotification(
                    context = getApplication(),
                    channelId = NotificationHelper.CHANNEL_DEFAULT,
                    title = "Broadcast Alert: $title",
                    body = message
                )
                refreshAdminStats()
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun observePaymentAccounts(activeOnly: Boolean = false): Flow<List<com.example.data.models.PaymentAccount>> {
        return repository.observePaymentAccounts(activeOnly)
    }

    fun savePaymentAccount(account: com.example.data.models.PaymentAccount, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.savePaymentAccount(account)
                onDone(true)
            } catch (e: Exception) {
                Log.e("MedBankViewModel", "Failed to save payment account: ${e.message}")
                onDone(false)
            }
        }
    }

    fun deletePaymentAccount(accountId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePaymentAccount(accountId)
                onDone(true)
            } catch (e: Exception) {
                Log.e("MedBankViewModel", "Failed to delete payment account: ${e.message}")
                onDone(false)
            }
        }
    }
}

