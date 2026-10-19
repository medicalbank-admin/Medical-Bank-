package com.example.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.AccessRequest
import com.example.ui.MedBankViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: MedBankViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val stats by viewModel.adminStats.collectAsState()
    val recentRequests by viewModel.observeRecentAccessRequests(5).collectAsState(initial = emptyList())

    val adminName = currentUser?.fullName?.ifBlank { "Administrator" } ?: "Administrator"
    val snackbarHostState = remember { SnackbarHostState() }

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var isBroadcasting by remember { mutableStateOf(false) }

    val pendingCount = stats["pendingRequests"] ?: 0
    val totalStudents = stats["totalStudents"] ?: stats["users"] ?: 0
    val publishedExams = stats["publishedExams"] ?: 0
    val activeSubscriptions = stats["activeSubscriptions"] ?: stats["activePlans"] ?: 0

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.refreshAdminStats()
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Administrator Portal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                MedBankChip(
                                    text = "SUPER ADMIN",
                                    color = BrandRed,
                                    bgColor = BrandRedPastel,
                                    icon = Icons.Default.Shield
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Welcome, $adminName",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        // Notification Bell Icon with unread indicator
                        Surface(
                            onClick = { showNotificationDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("admin_dashboard_bell"),
                            shape = CircleShape,
                            color = CardSurface,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Admin Notifications",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (pendingCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(BrandRed)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-10).dp, y = (10).dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // =================================================================
            // 2. QUICK STATS (2x2 Grid of MedBankCards)
            // =================================================================
            Text(
                text = "Operational Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Row 1: Total Students & Pending Requests
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Total Students (Query Firestore users count)
                MedBankCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_stat_total_students"),
                    onClick = { viewModel.navigateToAdminSection(AdminSection.USERS) },
                    padding = 16.dp
                ) {
                    MedBankIconBadge(
                        icon = Icons.Default.People,
                        badgeColor = BrandBlue,
                        backgroundColor = BrandBluePastel,
                        size = 42.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$totalStudents",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Total Students",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // Card 2: Pending Requests (Query accessRequests where status == pending)
                MedBankCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_stat_pending_requests"),
                    onClick = { viewModel.navigateToAdminSection(AdminSection.REQUESTS) },
                    padding = 16.dp
                ) {
                    MedBankIconBadge(
                        icon = Icons.Default.PendingActions,
                        badgeColor = if (pendingCount > 0) BrandRed else BrandBlue,
                        backgroundColor = if (pendingCount > 0) BrandRedPastel else BrandBluePastel,
                        size = 42.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$pendingCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (pendingCount > 0) BrandRed else TextPrimary
                    )
                    Text(
                        text = "Pending Requests",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            // Row 2: Published Exams & Active Subscriptions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 3: Published Exams (Query exams where isPublished == true)
                MedBankCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_stat_published_exams"),
                    onClick = { viewModel.navigateToAdminSection(AdminSection.CONTENT) },
                    padding = 16.dp
                ) {
                    MedBankIconBadge(
                        icon = Icons.Default.Assignment,
                        badgeColor = BrandSuccess,
                        backgroundColor = BrandSuccessPastel,
                        size = 42.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$publishedExams",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Published Exams",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // Card 4: Active Subscriptions (Users with active entitlements/passes)
                MedBankCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_stat_active_subscriptions"),
                    onClick = { viewModel.navigateToAdminSection(AdminSection.PLANS) },
                    padding = 16.dp
                ) {
                    MedBankIconBadge(
                        icon = Icons.Default.CardMembership,
                        badgeColor = AmberWarning,
                        backgroundColor = AmberWarningPastel,
                        size = 42.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$activeSubscriptions",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Active Subscriptions",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            // =================================================================
            // 3. QUICK ACTIONS SECTION
            // =================================================================
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            MedBankCard(
                padding = 14.dp,
                modifier = Modifier.testTag("admin_quick_actions_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Action 1: Review Requests
                    Surface(
                        onClick = { viewModel.navigateToAdminSection(AdminSection.REQUESTS) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (pendingCount > 0) BrandRedPastel.copy(alpha = 0.4f) else BrandBluePastel.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_action_review_requests")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MedBankIconBadge(
                                icon = Icons.Default.Verified,
                                badgeColor = if (pendingCount > 0) BrandRed else BrandBlue,
                                backgroundColor = CardSurface,
                                size = 36.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Review Requests",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (pendingCount > 0) "$pendingCount pending approval" else "Verify payment receipts & grant passes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    // Action: Test Exams / Content Browser
                    Surface(
                        onClick = { viewModel.setAdminBrowsingContent(true) },
                        shape = RoundedCornerShape(12.dp),
                        color = BrandBluePastel.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_action_test_exams")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MedBankIconBadge(
                                icon = Icons.Default.School,
                                badgeColor = BrandBlue,
                                backgroundColor = CardSurface,
                                size = 36.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Test Exams / Content Browser",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Browse MBBS & Licensing trees, test exam player as admin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = BrandBlue
                            )
                        }
                    }

                    // Action 2: Bulk Import
                    Surface(
                        onClick = { viewModel.navigateToAdminSection(AdminSection.BULK_IMPORT) },
                        shape = RoundedCornerShape(12.dp),
                        color = BrandBluePastel.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_action_bulk_import")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MedBankIconBadge(
                                icon = Icons.Default.CloudUpload,
                                badgeColor = BrandBlue,
                                backgroundColor = CardSurface,
                                size = 36.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bulk Import",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Upload questions & explanations via pipe format",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    // Action 3: Broadcast Alert (FCM dialog)
                    Surface(
                        onClick = { showBroadcastDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = BrandSuccessPastel.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_action_broadcast_alert")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MedBankIconBadge(
                                icon = Icons.Default.Campaign,
                                badgeColor = BrandSuccess,
                                backgroundColor = CardSurface,
                                size = 36.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Broadcast Alert",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Send FCM push alert & update announcement banner",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    // Action 4: Billing Accounts
                    Surface(
                        onClick = { viewModel.navigateToAdminSection(AdminSection.BILLING_ACCOUNTS) },
                        shape = RoundedCornerShape(12.dp),
                        color = BrandBluePastel.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_action_billing_accounts")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MedBankIconBadge(
                                icon = Icons.Default.AccountBalance,
                                badgeColor = BrandBlue,
                                backgroundColor = CardSurface,
                                size = 36.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Billing Accounts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Manage bank accounts, JazzCash & Easypaisa payment details",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 4. RECENT ACTIVITY LIST (5 Most Recent accessRequests)
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Access Requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = { viewModel.navigateToAdminSection(AdminSection.REQUESTS) },
                    modifier = Modifier.testTag("admin_recent_view_all")
                ) {
                    Text(
                        text = "View All",
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (recentRequests.isEmpty()) {
                MedBankCard(
                    padding = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recent access requests",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "When students request locked exams, they will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                MedBankCard(
                    padding = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_recent_requests_list")
                ) {
                    recentRequests.forEachIndexed { index, req ->
                        val dateText = try {
                            dateFormat.format(Date(req.createdAt))
                        } catch (_: Exception) {
                            "Recent"
                        }

                        val statusColor = when (req.status) {
                            "approved" -> BrandSuccess
                            "rejected" -> BrandRed
                            else -> AmberWarning
                        }

                        val statusBg = when (req.status) {
                            "approved" -> BrandSuccessPastel
                            "rejected" -> BrandRedPastel
                            else -> AmberWarningPastel
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateToAdminSection(AdminSection.REQUESTS) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MedBankIconBadge(
                                icon = when (req.status) {
                                    "approved" -> Icons.Default.CheckCircle
                                    "rejected" -> Icons.Default.Cancel
                                    else -> Icons.Default.HourglassTop
                                },
                                badgeColor = statusColor,
                                backgroundColor = statusBg,
                                size = 36.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.studentName.ifBlank { "Candidate" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${req.planName.ifBlank { req.planId }} • $dateText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            MedBankChip(
                                text = req.status.uppercase(),
                                color = statusColor,
                                bgColor = statusBg
                            )
                        }

                        if (index < recentRequests.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // =====================================================================
    // BROADCAST ALERT DIALOG (FCM notification to students)
    // =====================================================================
    if (showBroadcastDialog) {
        var alertTitle by remember { mutableStateOf("") }
        var alertMessage by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { if (!isBroadcasting) showBroadcastDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardSurface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_broadcast_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedBankIconBadge(
                            icon = Icons.Default.Campaign,
                            badgeColor = BrandBlue,
                            backgroundColor = BrandBluePastel,
                            size = 40.dp,
                            iconSize = 22.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Broadcast Alert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Send FCM push alert to all students",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = alertTitle,
                        onValueChange = { alertTitle = it },
                        label = { Text("Alert Title") },
                        placeholder = { Text("e.g. New Mock Exam Published") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_broadcast_title_input")
                    )

                    OutlinedTextField(
                        value = alertMessage,
                        onValueChange = { alertMessage = it },
                        label = { Text("Message / Announcement") },
                        placeholder = { Text("e.g. Surgery Final Year Mock Exam is now live.") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_broadcast_message_input")
                    )

                    Text(
                        text = "• Sends FCM notification to topic: all_students\n• Automatically displays in student home screen carousel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showBroadcastDialog = false },
                            enabled = !isBroadcasting
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (alertTitle.isNotBlank() && alertMessage.isNotBlank()) {
                                    isBroadcasting = true
                                    viewModel.broadcastAlert(alertTitle.trim(), alertMessage.trim()) { success ->
                                        isBroadcasting = false
                                        showBroadcastDialog = false
                                    }
                                }
                            },
                            enabled = alertTitle.isNotBlank() && alertMessage.isNotBlank() && !isBroadcasting,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_broadcast_submit_btn")
                        ) {
                            if (isBroadcasting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Alert")
                            }
                        }
                    }
                }
            }
        }
    }

    // =====================================================================
    // NOTIFICATION BELL SUMMARY DIALOG
    // =====================================================================
    if (showNotificationDialog) {
        Dialog(onDismissRequest = { showNotificationDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardSurface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_bell_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BrandBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin System Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        IconButton(onClick = { showNotificationDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    // Alert Item 1: Pending Requests
                    MedBankCard(
                        padding = 12.dp,
                        onClick = {
                            showNotificationDialog = false
                            viewModel.navigateToAdminSection(AdminSection.REQUESTS)
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MedBankIconBadge(
                                icon = Icons.Default.PendingActions,
                                badgeColor = if (pendingCount > 0) BrandRed else BrandSuccess,
                                backgroundColor = if (pendingCount > 0) BrandRedPastel else BrandSuccessPastel,
                                size = 34.dp,
                                iconSize = 18.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Access Requests",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (pendingCount > 0) "$pendingCount pending verification" else "All caught up! No pending requests.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            MedBankChip(
                                text = if (pendingCount > 0) "$pendingCount PENDING" else "CLEAR",
                                color = if (pendingCount > 0) BrandRed else BrandSuccess,
                                bgColor = if (pendingCount > 0) BrandRedPastel else BrandSuccessPastel
                            )
                        }
                    }

                    // Alert Item 2: Pending Referral Payouts
                    val pendingPayouts = stats["pendingPayouts"] ?: 0
                    MedBankCard(
                        padding = 12.dp,
                        onClick = {
                            showNotificationDialog = false
                            viewModel.navigateToAdminSection(AdminSection.PAYOUTS)
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MedBankIconBadge(
                                icon = Icons.Default.Paid,
                                badgeColor = if (pendingPayouts > 0) BrandRed else BrandSuccess,
                                backgroundColor = if (pendingPayouts > 0) BrandRedPastel else BrandSuccessPastel,
                                size = 34.dp,
                                iconSize = 18.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Referral Payouts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (pendingPayouts > 0) "$pendingPayouts student commissions awaiting transfer" else "No pending payout requests.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            MedBankChip(
                                text = if (pendingPayouts > 0) "$pendingPayouts PENDING" else "CLEAR",
                                color = if (pendingPayouts > 0) BrandRed else BrandSuccess,
                                bgColor = if (pendingPayouts > 0) BrandRedPastel else BrandSuccessPastel
                            )
                        }
                    }
                }
            }
        }
    }
}
