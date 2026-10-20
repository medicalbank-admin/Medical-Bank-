package com.example.ui.screens.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Exam
import com.example.data.models.Subject
import com.example.ui.MedBankViewModel
import com.example.ui.components.*
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderTreeScreen(viewModel: MedBankViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val adminBrowsing by viewModel.adminBrowsingContent.collectAsState()

    // If user is an Admin and NOT browsing content, render the dedicated Admin Dashboard
    if (currentUser?.role == "admin" && !adminBrowsing) {
        AdminDashboardScreen(viewModel = viewModel)
        return
    }

    val notifications by viewModel.observeNotifications(currentUser?.uid ?: "").collectAsState(initial = emptyList())
    val hasUnread = notifications.any { !it.isRead }

    val selectedProgramType by viewModel.selectedProgramType.collectAsState()
    val selectedParentId by viewModel.selectedParentId.collectAsState()
    val selectedParentName by viewModel.selectedParentName.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    var showNotificationDialog by remember { mutableStateOf(false) }
    var showBanners by remember { mutableStateOf(true) } // State to dismiss banners

    // Handle Android system back button when drilled down
    BackHandler(enabled = selectedParentId != null || selectedSubject != null) {
        viewModel.navigateBackExplorer()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val msg by viewModel.accessRequestStatusMsg.collectAsState()
    LaunchedEffect(msg) {
        msg?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    Scaffold(
    containerColor = AppBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
        if (selectedParentId != null || selectedSubject != null) {
            // Secondary level TopAppBar (Drilled down)
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedSubject != null) selectedSubject!!.name else selectedParentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (selectedSubject != null) selectedParentName else "Select a subject to view exams",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBackExplorer() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        } else {
            // NEW: Root level TopAppBar with Track Dropdown and Icons
            TopAppBar(
                title = {
                    var trackDropdownExpanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier.clickable { trackDropdownExpanded = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when(selectedProgramType) {
                                    "licensing" -> "Licensing Exams"
                                    "past_papers" -> "Past Papers"
                                    "mock_exams" -> "Mock Exams"
                                    else -> "MBBS"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Track",
                                tint = BrandBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = trackDropdownExpanded,
                            onDismissRequest = { trackDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("MBBS") }, onClick = { viewModel.selectProgramType("mbbs"); trackDropdownExpanded = false })
                            DropdownMenuItem(text = { Text("Licensing Exams") }, onClick = { viewModel.selectProgramType("licensing"); trackDropdownExpanded = false })
                            DropdownMenuItem(text = { Text("Past Papers") }, onClick = { viewModel.selectProgramType("past_papers"); trackDropdownExpanded = false })
                            DropdownMenuItem(text = { Text("Mock Exams") }, onClick = { viewModel.selectProgramType("mock_exams"); trackDropdownExpanded = false })
                        }
                    }
                },
                actions = {
                    // Help Desk Icon
                    IconButton(onClick = { viewModel.openProfileScreen("help") }) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Help Desk", tint = BrandBlue)
                    }
                    // Notification Bell
                    Box {
                        IconButton(onClick = { viewModel.openProfileScreen("notifications_inbox") }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                        }
                        if (hasUnread) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BrandRed)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    }
) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
            if (currentUser?.role == "admin" && adminBrowsing) {
                Surface(color = BrandBlue, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Admin Content Test Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Button(
                            onClick = { viewModel.setAdminBrowsingContent(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlue),
                            modifier = Modifier.testTag("exit_admin_browse_button")
                        ) {
                            Text("Exit to Dashboard", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ROOT LEVEL HOME HEADER
            if (selectedParentId == null && selectedSubject == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // User greeting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MedBankIconBadge(icon = Icons.Default.Person, badgeColor = BrandBlue, backgroundColor = BrandBluePastel, size = 46.dp, iconSize = 22.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Hello, ${currentUser?.fullName?.ifBlank { "Doctor" } ?: "Doctor"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("CNIC: ${currentUser?.cnic?.ifBlank { "Not Registered" } ?: "Not Registered"}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val banner by viewModel.banner.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    // DISMISSIBLE BANNERS
                    AnimatedVisibility(visible = showBanners, exit = fadeOut() + shrinkVertically()) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    if (banner.isActive && banner.message.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().testTag("dynamic_announcement_banner"),
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.Transparent,
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().background(
                                                    brush = Brush.horizontalGradient(colors = listOf(BrandBlue, BrandDarkerBlue)),
                                                    shape = RoundedCornerShape(16.dp)
                                                ).padding(16.dp)
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text("Announcement", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(banner.message, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                                    
                                                    if (banner.linkUrl.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Button(
                                                            onClick = {
                                                                try {
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(banner.linkUrl))
                                                                    context.startActivity(intent)
                                                                } catch (_: Exception) {}
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlue),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.testTag("banner_action_button")
                                                        ) {
                                                            Text(banner.buttonText.ifBlank { "Update Now" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }

                                    val announcements = remember(appSettings.announcementBanner) {
                                        if (appSettings.announcementBanner.isNotBlank()) {
                                            listOf(appSettings.announcementBanner, "Full clinical explanations enabled after exam submission.", "Unlock your next term pass with instant WhatsApp admin verification.")
                                        } else {
                                            listOf("Welcome to Medical Bank! Practice high-yield clinical MCQs anytime.", "New Mock Mode with negative marking now active.", "All-Pakistan MBBS & Licensing standardized question banks.")
                                        }
                                    }
                                    MedBankBanner(announcements = announcements)
                                }

                                // Dismiss Button for Banners
                                IconButton(
                                    onClick = { showBanners = false },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(28.dp).background(Color.Black.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss Banners", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }

            // CONTENT AREA BASED ON NAVIGATION DEPTH
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    selectedSubject != null -> { ExamListContent(subject = selectedSubject!!, viewModel = viewModel) }
                    selectedParentId != null -> { SubjectListContent(parentType = selectedProgramType, parentId = selectedParentId!!, parentName = selectedParentName, viewModel = viewModel) }
                    selectedProgramType == "mbbs" -> { MbbsYearsGrid(viewModel = viewModel) }
                    selectedProgramType == "licensing" -> { LicensingPathwaysList(viewModel = viewModel) }
                    selectedProgramType == "past_papers" -> { PastPapersFolderList(viewModel = viewModel) }
                    selectedProgramType == "mock_exams" -> { MockExamsFolderList(viewModel = viewModel) }
                    else -> { MbbsYearsGrid(viewModel = viewModel) }
                }
            }
        }
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = BrandBlue) },
            title = { Text("Announcements", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appSettings.announcementBanner.ifBlank { "Welcome to Medical Bank! Study questions are curated by medical specialists." }, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text("FCM Push Notifications are configured. You will receive updates on access approvals and new exam papers.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(onClick = { showNotificationDialog = false }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) {
                    Text("Close")
                }
            }
        )
    }
}
@Composable
fun MbbsYearsGrid(viewModel: MedBankViewModel) {
    val activeYears by viewModel.repository.observeActiveYears().collectAsState(initial = emptyList())

    if (activeYears.isEmpty()) {
        EmptyPlaceholder(
            icon = Icons.Default.School,
            title = "No MBBS Years Configured",
            subtitle = "The question bank structure is empty. Admin can configure years in Admin > Content Manager."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(activeYears) { year ->
                MedBankCard(
                    onClick = { viewModel.selectParent(year.id, year.name) },
                    modifier = Modifier.testTag("year_card_${year.yearNumber}")
                ) {
                    MedBankIconBadge(icon = Icons.Default.School, badgeColor = BrandBlue, backgroundColor = BrandBluePastel, size = 44.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(year.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Year ${year.yearNumber} Question Banks", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("View Subjects", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LicensingPathwaysList(viewModel: MedBankViewModel) {
    val activePathways by viewModel.repository.observeActivePathways().collectAsState(initial = emptyList())

    if (activePathways.isEmpty()) {
        EmptyPlaceholder(icon = Icons.Default.VerifiedUser, title = "No Licensing Pathways Configured", subtitle = "Licensing examination tracks (NRE, FCPS, USMLE, PLAB, DHA, SMLE) can be configured by Admin.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activePathways) { path ->
                MedBankCard(onClick = { viewModel.selectParent(path.id, path.name) }, modifier = Modifier.testTag("pathway_card_${path.code}")) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        MedBankIconBadge(icon = Icons.Default.VerifiedUser, badgeColor = BrandRed, backgroundColor = BrandRedPastel, size = 46.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(path.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                MedBankChip(text = path.code, color = BrandRed, bgColor = BrandRedPastel)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Standardized licensing exam question bank", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectListContent(parentType: String, parentId: String, parentName: String, viewModel: MedBankViewModel) {
    val subjects by viewModel.repository.observeSubjectsByParent(parentType, parentId).collectAsState(initial = emptyList())

    if (subjects.isEmpty()) {
        EmptyPlaceholder(icon = Icons.Default.MenuBook, title = "No Subjects in $parentName", subtitle = "Subjects and question papers have not been published for this section yet.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(subjects) { subject ->
                MedBankCard(onClick = { viewModel.selectSubject(subject) }, modifier = Modifier.testTag("subject_card_${subject.id}")) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        MedBankIconBadge(icon = Icons.Default.MenuBook, badgeColor = BrandBlue, backgroundColor = BrandBluePastel)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("High-yield clinical MCQs and mocks", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExamListContent(subject: Subject, viewModel: MedBankViewModel) {
    val exams by viewModel.repository.observeExamsForSubjectOrParent(subject).collectAsState(initial = emptyList())
    val startingExamId by viewModel.isStartingExam.collectAsState()

    LaunchedEffect(subject.id) { viewModel.preloadExamsForSubject(subject) }

    if (exams.isEmpty()) {
        EmptyPlaceholder(icon = Icons.Default.Assignment, title = "No Exams Available", subtitle = "There are currently no published exams for ${subject.name}.")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(exams) { exam ->
                val activeEnt = viewModel.getActiveEntitlementForExam(exam)
                val isUnlocked = activeEnt != null
                val isPending = viewModel.isExamPending(exam)
                val isExpired = if (!isUnlocked) viewModel.isExamExpired(exam) else false

                val state = when { isUnlocked -> 3; isPending -> 2; isExpired -> 4; else -> 1 }
                val isStartingThisExam = startingExamId == exam.id

                MedBankCard(
                    onClick = { when (state) { 3 -> viewModel.startExam(exam); 1, 4 -> viewModel.openAccessRequestSheet(exam) } },
                    modifier = Modifier.testTag("exam_card_${exam.id}")
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(exam.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                        when (state) {
                            3 -> MedBankChip(text = "ACTIVE", color = BrandSuccess, icon = Icons.Default.CheckCircle)
                            2 -> MedBankChip(text = "PENDING", color = AmberWarning, icon = Icons.Default.Schedule)
                            4 -> MedBankChip(text = "EXPIRED", color = BrandRed, icon = Icons.Default.Warning)
                            else -> MedBankChip(text = "LOCKED", color = BrandRed, icon = Icons.Default.Lock)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary); Spacer(modifier = Modifier.width(4.dp)); Text("${exam.questionCount} Qs", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary); Spacer(modifier = Modifier.width(4.dp)); Text("${exam.durationMin} mins", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary); Spacer(modifier = Modifier.width(4.dp)); Text("${exam.totalMarks.toInt()} Marks", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                        if (exam.negativeMarking > 0.0) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = BrandRed); Spacer(modifier = Modifier.width(4.dp)); Text("-${exam.negativeMarking}", style = MaterialTheme.typography.bodySmall, color = BrandRed) } }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (state == 3) {
                            val expiryText = if (activeEnt != null && activeEnt.expiresAt > 0L && activeEnt.expiresAt < Long.MAX_VALUE) "Unlocked until ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(activeEnt.expiresAt))}" else "Unlocked"
                            Text(expiryText, style = MaterialTheme.typography.labelSmall, color = BrandSuccess, fontWeight = FontWeight.SemiBold)
                            Button(onClick = { viewModel.startExam(exam) }, enabled = !isStartingThisExam, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White), modifier = Modifier.testTag("start_exam_button_${exam.id}")) {
                                if (isStartingThisExam) { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White); Spacer(modifier = Modifier.width(6.dp)); Text("Starting...", fontSize = 13.sp) } else { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Start Exam", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            }
                        } else if (state == 2) {
                            Text("Payment proof under review", style = MaterialTheme.typography.labelSmall, color = AmberWarning, fontWeight = FontWeight.Medium)
                            OutlinedButton(onClick = {}, enabled = false, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning), colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning)) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Verification Pending", fontSize = 12.sp)
                            }
                        } else {
                            Text(if (state == 4) "Pass expired" else "Subscription required", style = MaterialTheme.typography.labelSmall, color = BrandRed, fontWeight = FontWeight.Medium)
                            Button(onClick = { viewModel.openAccessRequestSheet(exam) }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = Color.White), modifier = Modifier.testTag("unlock_exam_button_${exam.id}")) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(if (state == 4) "Renew Access" else "Unlock Bank", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PastPapersFolderList(viewModel: MedBankViewModel) {
    val dbSubjects by viewModel.repository.observeSubjectsByParentType("past_papers").collectAsState(initial = emptyList())
    val defaultFolders = remember { listOf(Subject(id = "pp_mbbs", parentType = "past_papers", parentId = "pp_mbbs", name = "MBBS Professional Past Papers", order = 1, isActive = true), Subject(id = "pp_licensing", parentType = "past_papers", parentId = "pp_licensing", name = "Licensing & FCPS Past Papers", order = 2, isActive = true), Subject(id = "pp_annual", parentType = "past_papers", parentId = "pp_annual", name = "Annual & Supplementary Papers", order = 3, isActive = true), Subject(id = "pp_specialty", parentType = "past_papers", parentId = "pp_specialty", name = "Specialty Clinical Papers", order = 4, isActive = true)) }
    val allFolders = remember(dbSubjects) { val list = mutableListOf<Subject>(); list.addAll(dbSubjects); defaultFolders.forEach { def -> if (list.none { it.id == def.id }) list.add(def) }; list.sortedBy { it.order } }

    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(allFolders) { folder ->
            MedBankCard(onClick = { viewModel.selectSubject(folder) }, modifier = Modifier.testTag("past_paper_card_${folder.id}")) {
                MedBankIconBadge(icon = Icons.Default.HistoryEdu, badgeColor = BrandRed, backgroundColor = BrandRedPastel, size = 44.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Past Exam Papers", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("View Exams", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = BrandRed)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = BrandRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun MockExamsFolderList(viewModel: MedBankViewModel) {
    val dbSubjects by viewModel.repository.observeSubjectsByParentType("mock_exams").collectAsState(initial = emptyList())
    val defaultFolders = remember { listOf(Subject(id = "mock_grand", parentType = "mock_exams", parentId = "mock_grand", name = "Full-Length Grand Mocks", order = 1, isActive = true), Subject(id = "mock_clinical", parentType = "mock_exams", parentId = "mock_clinical", name = "Clinical Vignette Mocks", order = 2, isActive = true), Subject(id = "mock_timed", parentType = "mock_exams", parentId = "mock_timed", name = "High-Yield Timed Mocks", order = 3, isActive = true), Subject(id = "mock_rapid", parentType = "mock_exams", parentId = "mock_rapid", name = "Rapid Revision Mocks", order = 4, isActive = true)) }
    val allFolders = remember(dbSubjects) { val list = mutableListOf<Subject>(); list.addAll(dbSubjects); defaultFolders.forEach { def -> if (list.none { it.id == def.id }) list.add(def) }; list.sortedBy { it.order } }

    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(allFolders) { folder ->
            MedBankCard(onClick = { viewModel.selectSubject(folder) }, modifier = Modifier.testTag("mock_exam_card_${folder.id}")) {
                MedBankIconBadge(icon = Icons.Default.Quiz, badgeColor = AmberWarning, backgroundColor = AmberWarningPastel, size = 44.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Standardized Mock Tests", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("View Exams", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = AmberWarning)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}


        
