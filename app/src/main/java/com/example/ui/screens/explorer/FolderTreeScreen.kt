package com.example.ui.screens.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Exam
import com.example.data.models.Subject
import com.example.ui.MedBankViewModel
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderTreeScreen(viewModel: MedBankViewModel) {
    val selectedProgramType by viewModel.selectedProgramType.collectAsState()
    val selectedParentId by viewModel.selectedParentId.collectAsState()
    val selectedParentName by viewModel.selectedParentName.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Handle Android system back button when drilled down
    BackHandler(enabled = selectedParentId != null || selectedSubject != null) {
        viewModel.navigateBackExplorer()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header / Navigation Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.logo),
                        contentDescription = "Medical Bank Logo",
                        modifier = Modifier.height(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when {
                                selectedSubject != null -> selectedSubject!!.name
                                selectedParentId != null -> selectedParentName
                                selectedProgramType == "mbbs" -> "MBBS Question Banks"
                                else -> "Licensing Exam Banks"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (selectedParentId != null || selectedSubject != null) {
                            Text(
                                text = if (selectedSubject != null) selectedParentName else "Subjects & Examinations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (selectedParentId != null || selectedSubject != null) {
                    IconButton(onClick = { viewModel.navigateBackExplorer() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Top-Level Track Toggle (Only shown at root level)
        if (selectedParentId == null && selectedSubject == null) {
            TabRow(
                selectedTabIndex = if (selectedProgramType == "mbbs") 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                indicator = {}
            ) {
                Tab(
                    selected = selectedProgramType == "mbbs",
                    onClick = { viewModel.selectProgramType("mbbs") },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MBBS Years 1–6", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedProgramType == "licensing",
                    onClick = { viewModel.selectProgramType("licensing") },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Licensing Pathways", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        }

        // CONTENT AREA BASED ON NAVIGATION DEPTH
        val snackbarHostState = remember { SnackbarHostState() }
        val msg by viewModel.accessRequestStatusMsg.collectAsState()
        LaunchedEffect(msg) {
            msg?.let {
                snackbarHostState.showSnackbar(it)
            }
        }
        
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(activeYears) { year ->
                ElevatedCard(
                    onClick = { viewModel.selectParent(year.id, year.name) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("year_card_${year.yearNumber}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Y${year.yearNumber}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }

                        Column {
                            Text(
                                text = year.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to view subjects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activePathways) { path ->
                ElevatedCard(
                    onClick = { viewModel.selectParent(path.id, path.name) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pathway_card_${path.code}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = path.code.take(4),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = path.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Standardized licensing question bank",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
                ElevatedCard(
                    onClick = { viewModel.selectSubject(subject) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_card_${subject.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to explore available exams",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
                    else -> 1 // Not Requested
                }

                ElevatedCard(
                    onClick = {
                        when (state) {
                            3 -> viewModel.startExam(exam)
                            1, 4 -> viewModel.openAccessRequestSheet(exam)
                            else -> {} // Do nothing
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exam_card_${exam.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (state == 3) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (state == 3) 2.dp else 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
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
                                modifier = Modifier.weight(1f)
                            )

                            when (state) {
                                2 -> StatusBadge(text = "PENDING", color = Color(0xFFFFA000), icon = Icons.Default.Schedule)
                                3 -> StatusBadge(text = "UNLOCKED", color = Color(0xFF388E3C), icon = Icons.Default.LockOpen)
                                4 -> StatusBadge(text = "EXPIRED", color = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning)
                                else -> {}
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${exam.questionCount} Qs", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${exam.durationMin} mins", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${exam.totalMarks.toInt()} Marks", style = MaterialTheme.typography.bodySmall)
                            }
                            if (exam.negativeMarking > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("-${exam.negativeMarking}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                                    text = "Unlocked until ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(ent!!.expiresAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FilledTonalButton(
                                    onClick = { viewModel.startExam(exam) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Exam")
                                }
                            } else if (state == 2) {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Requested — Pending Approval")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.openAccessRequestSheet(exam) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (state == 4) "Renew Access" else "Request Access", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
