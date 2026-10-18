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
                // Secondary level TopAppBar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when {
                                    selectedSubject != null -> selectedSubject!!.name
                                    else -> selectedParentName
                                },
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CardSurface
                    )
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
                Surface(
                    color = BrandBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin Content Test Mode",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Top row: User greeting (Name + CNIC) and circular notification bell
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MedBankIconBadge(
                                icon = Icons.Default.Person,
                                badgeColor = BrandBlue,
                                backgroundColor = BrandBluePastel,
                                size = 46.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Hello, ${currentUser?.fullName?.ifBlank { "Doctor" } ?: "Doctor"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "CNIC: ${currentUser?.cnic?.ifBlank { "Not Registered" } ?: "Not Registered"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Circular notification bell
                        Surface(
                            onClick = { viewModel.openProfileScreen("notifications_inbox") },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("home_notification_bell"),
                            shape = CircleShape,
                            color = CardSurface,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                // Red unread badge dot
                                if (hasUnread) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(BrandRed)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-10).dp, y = (10).dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val banner by viewModel.banner.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    if (banner.isActive && banner.message.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dynamic_announcement_banner"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(BrandBlue, BrandDarkerBlue)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Announcement",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = banner.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (banner.linkUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(banner.linkUrl))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White,
                                                contentColor = BrandBlue
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("banner_action_button")
                                        ) {
                                            Text(
                                                text = banner.buttonText.ifBlank { "Update Now" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // MedBankBanner carousel showing announcements
                    val announcements = remember(appSettings.announcementBanner) {
                        if (appSettings.announcementBanner.isNotBlank()) {
                            listOf(
                                appSettings.announcementBanner,
                                "Full clinical explanations enabled after exam submission.",
                                "Unlock your next term pass with instant WhatsApp admin verification."
                            )
                        } else {
                            listOf(
                                "Welcome to Medical Bank! Practice high-yield clinical MCQs anytime.",
                                "New Mock Mode with negative marking now active.",
                                "All-Pakistan MBBS & Licensing standardized question banks."
                            )
                        }
                    }
                    MedBankBanner(announcements = announcements)

                    Spacer(modifier = Modifier.height(14.dp))

                    // MedBankSegmentedTabs for MBBS vs Licensing
                    MedBankSegmentedTabs(
                        tabs = listOf("MBBS Years 1–6", "Licensing Pathways"),
                        selectedIndex = if (selectedProgramType == "mbbs") 0 else 1,
                        onTabSelected = { idx ->
                            viewModel.selectProgramType(if (idx == 0) "mbbs" else "licensing")
                        },
                        modifier = Modifier.testTag("home_segmented_tabs")
                    )
                }
            }

            // CONTENT AREA BASED ON NAVIGATION DEPTH
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // LEVEL 3: EXAM LIST UNDER A SUBJECT
                    selectedSubject != null -> {
                        ExamListContent(
                            subject = selectedSubject!!,
                            viewModel = viewModel
                        )
                    }

                    // LEVEL 2: SUBJECTS LIST UNDER A YEAR OR PATHWAY
                    selectedParentId != null -> {
                        SubjectListContent(
                            parentType = selectedProgramType,
                            parentId = selectedParentId!!,
                            parentName = selectedParentName,
                            viewModel = viewModel
                        )
                    }

                    // LEVEL 1: ROOT DIRECTORY (MBBS Years or Licensing Pathways)
                    selectedProgramType == "mbbs" -> {
                        MbbsYearsGrid(viewModel = viewModel)
                    }

                    else -> {
                        LicensingPathwaysList(viewModel = viewModel)
                    }
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
                    Text(
                        appSettings.announcementBanner.ifBlank {
                            "Welcome to Medical Bank! Study questions are curated by medical specialists."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        "FCM Push Notifications are configured. You will receive updates on access approvals and new exam papers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationDialog = false },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(activeYears) { year ->
                MedBankCard(
                    onClick = { viewModel.selectParent(year.id, year.name) },
                    modifier = Modifier.testTag("year_card_${year.yearNumber}")
                ) {
                    MedBankIconBadge(
                        icon = Icons.Default.School,
                        badgeColor = BrandBlue,
                        backgroundColor = BrandBluePastel,
                        size = 44.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = year.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Year ${year.yearNumber} Question Banks",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View Subjects",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(16.dp)
                        )
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
        EmptyPlaceholder(
            icon = Icons.Default.VerifiedUser,
            title = "No Licensing Pathways Configured",
            subtitle = "Licensing examination tracks (NRE, FCPS, USMLE, PLAB, DHA, SMLE) can be configured by Admin."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activePathways) { path ->
                MedBankCard(
                    onClick = { viewModel.selectParent(path.id, path.name) },
                    modifier = Modifier.testTag("pathway_card_${path.code}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MedBankIconBadge(
                            icon = Icons.Default.VerifiedUser,
                            badgeColor = BrandRed,
                            backgroundColor = BrandRedPastel,
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = path.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                MedBankChip(
                                    text = path.code,
                                    color = BrandRed,
                                    bgColor = BrandRedPastel
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Standardized licensing exam question bank",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectListContent(
    parentType: String,
    parentId: String,
    parentName: String,
    viewModel: MedBankViewModel
) {
    val subjects by viewModel.repository.observeSubjectsByParent(parentType, parentId).collectAsState(initial = emptyList())

    if (subjects.isEmpty()) {
        EmptyPlaceholder(
            icon = Icons.Default.MenuBook,
            title = "No Subjects in $parentName",
            subtitle = "Subjects and question papers have not been published for this section yet."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subjects) { subject ->
                MedBankCard(
                    onClick = { viewModel.selectSubject(subject) },
                    modifier = Modifier.testTag("subject_card_${subject.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MedBankIconBadge(
                            icon = Icons.Default.MenuBook,
                            badgeColor = BrandBlue,
                            backgroundColor = BrandBluePastel
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "High-yield clinical MCQs and mocks",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExamListContent(
    subject: Subject,
    viewModel: MedBankViewModel
) {
    val exams by viewModel.repository.observePublishedExamsBySubject(subject.id).collectAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.collectAsState()
    val startingExamId by viewModel.isStartingExam.collectAsState()

    // Pre-fetch data in LaunchedEffect to eliminate button lag
    LaunchedEffect(subject.id) {
        viewModel.preloadExamsForSubject(subject)
    }

    if (exams.isEmpty()) {
        EmptyPlaceholder(
            icon = Icons.Default.Assignment,
            title = "No Exams Available",
            subtitle = "There are currently no published exams for ${subject.name}."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(exams) { exam ->
                val scopeId = "${exam.parentType}_${exam.parentId}"
                val ent = currentUser?.entitlements?.get(scopeId)
                val isPending = viewModel.pendingAccessRequests.collectAsState().value.contains(scopeId)
                
                val isUnlocked = ent != null && ent.status == "active" && ent.expiresAt > System.currentTimeMillis()
                val isExpired = ent != null && (ent.status == "expired" || ent.expiresAt <= System.currentTimeMillis())

                val state = when {
                    isUnlocked -> 3 // Approved/Active
                    isExpired -> 4 // Expired
                    isPending -> 2 // Pending
                    else -> 1 // Not Requested (Locked)
                }

                val isStartingThisExam = startingExamId == exam.id

                // Content list: Use MedBankCard for each exam. Show lock status using MedBankChip
                MedBankCard(
                    onClick = {
                        when (state) {
                            3 -> viewModel.startExam(exam)
                            1, 4 -> viewModel.openAccessRequestSheet(exam)
                            else -> {}
                        }
                    },
                    modifier = Modifier.testTag("exam_card_${exam.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Show lock status using MedBankChip (Red for LOCKED)
                        when (state) {
                            3 -> MedBankChip(text = "ACTIVE", color = BrandSuccess, icon = Icons.Default.CheckCircle)
                            2 -> MedBankChip(text = "PENDING", color = AmberWarning, icon = Icons.Default.Schedule)
                            4 -> MedBankChip(text = "EXPIRED", color = BrandRed, icon = Icons.Default.Warning)
                            else -> MedBankChip(text = "LOCKED", color = BrandRed, icon = Icons.Default.Lock)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${exam.questionCount} Qs", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${exam.durationMin} mins", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${exam.totalMarks.toInt()} Marks", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        if (exam.negativeMarking > 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = BrandRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("-${exam.negativeMarking}", style = MaterialTheme.typography.bodySmall, color = BrandRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state == 3) {
                            Text(
                                text = "Unlocked until ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(ent!!.expiresAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { viewModel.startExam(exam) },
                                enabled = !isStartingThisExam,
                                shape = CircleShape, // Pill
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandBlue,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("start_exam_button_${exam.id}")
                            ) {
                                if (isStartingThisExam) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Starting...", fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Exam", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (state == 2) {
                            Text(
                                text = "Payment proof under review",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning,
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verification Pending", fontSize = 12.sp)
                            }
                        } else {
                            Text(
                                text = if (state == 4) "Pass expired" else "Subscription required",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandRed,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = { viewModel.openAccessRequestSheet(exam) },
                                shape = CircleShape, // Pill
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandRed,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("unlock_exam_button_${exam.id}")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unlock Bank", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
