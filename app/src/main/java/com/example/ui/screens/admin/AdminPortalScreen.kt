package com.example.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.data.models.*
import com.example.ui.MedBankViewModel
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.SetScreenSecure
import com.example.ui.components.StatTile
import com.example.ui.components.StatusBadge
import kotlinx.coroutines.launch
import java.util.UUID
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.screens.legal.LegalContentScreen

enum class AdminSection {
    DASHBOARD,
    REQUESTS,
    PAYOUTS,
    USERS,
    CONTENT,
    BULK_IMPORT,
    PLANS,
    BILLING_ACCOUNTS,
    BANNER_CONTROL,
    LEGAL,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(viewModel: MedBankViewModel) {
    val activeSection by viewModel.activeAdminSection.collectAsState()

    // Requirement 11: Bulk Import has FLAG_SECURE
    if (activeSection == AdminSection.BULK_IMPORT) {
        SetScreenSecure(isSecure = true)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAdminStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.logo),
                        contentDescription = "Medical Bank Logo",
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Admin Portal",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CardSurface
            )
        )

        // Sub-Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminSection.entries.forEach { section ->
                val isSel = activeSection == section
                FilterChip(
                    selected = isSel,
                    onClick = { viewModel.setActiveAdminSection(section) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlue,
                        selectedLabelColor = Color.White,
                        containerColor = CardSurface,
                        labelColor = TextSecondary
                    ),
                    label = {
                        Text(
                            text = when (section) {
                                AdminSection.DASHBOARD -> "Dashboard"
                                AdminSection.REQUESTS -> "Access Requests"
                                AdminSection.PAYOUTS -> "Referral Payouts"
                                AdminSection.USERS -> "Users & Devices"
                                AdminSection.CONTENT -> "Content Hierarchy"
                                AdminSection.BULK_IMPORT -> "Bulk Import"
                                AdminSection.PLANS -> "Plans & Passes"
                                AdminSection.BILLING_ACCOUNTS -> "Billing Accounts"
                                AdminSection.BANNER_CONTROL -> "Banner Control"
                                AdminSection.LEGAL -> "Legal Content"
                                AdminSection.SETTINGS -> "App Config"
                            },
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Selected Sub-View
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeSection) {
                AdminSection.DASHBOARD -> AdminDashboardView(viewModel = viewModel, onNavigate = { viewModel.setActiveAdminSection(it) })
                AdminSection.REQUESTS -> AdminRequestsView(viewModel = viewModel)
                AdminSection.PAYOUTS -> AdminPayoutsScreen(viewModel = viewModel)
                AdminSection.USERS -> AdminUsersView(viewModel = viewModel)
                AdminSection.CONTENT -> AdminContentView(viewModel = viewModel)
                AdminSection.BULK_IMPORT -> AdminBulkImportView(viewModel = viewModel)
                AdminSection.PLANS -> AdminPlansView(viewModel = viewModel)
                AdminSection.BILLING_ACCOUNTS -> AdminBillingScreen(viewModel = viewModel, onBack = { viewModel.setActiveAdminSection(AdminSection.DASHBOARD) })
                AdminSection.BANNER_CONTROL -> AdminBannerScreen(viewModel = viewModel)
                AdminSection.LEGAL -> LegalContentScreen(viewModel = viewModel, onBack = { viewModel.setActiveAdminSection(AdminSection.DASHBOARD) })
                AdminSection.SETTINGS -> AdminSettingsView(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AdminDashboardView(viewModel: MedBankViewModel, onNavigate: (AdminSection) -> Unit) {
    val stats by viewModel.adminStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Overview Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

        // 2x2 grid of Stat Cards (Users, Pending Requests, Exams, Plans)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MedBankCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_stat_users"),
                onClick = { onNavigate(AdminSection.USERS) },
                padding = 16.dp
            ) {
                MedBankIconBadge(
                    icon = Icons.Default.People,
                    badgeColor = BrandBlue,
                    backgroundColor = BrandBluePastel,
                    size = 42.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${stats["users"] ?: 0}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(text = "Total Users", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            MedBankCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_stat_requests"),
                onClick = { onNavigate(AdminSection.REQUESTS) },
                padding = 16.dp
            ) {
                MedBankIconBadge(
                    icon = Icons.Default.PendingActions,
                    badgeColor = BrandRed,
                    backgroundColor = BrandRedPastel,
                    size = 42.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${stats["pendingRequests"] ?: 0}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandRed
                )
                Text(text = "Pending Requests", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MedBankCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_stat_exams"),
                onClick = { onNavigate(AdminSection.CONTENT) },
                padding = 16.dp
            ) {
                MedBankIconBadge(
                    icon = Icons.Default.Assignment,
                    badgeColor = BrandSuccess,
                    backgroundColor = BrandSuccessPastel,
                    size = 42.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${stats["publishedExams"] ?: 0}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(text = "Published Exams", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            MedBankCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_stat_plans"),
                onClick = { onNavigate(AdminSection.PLANS) },
                padding = 16.dp
            ) {
                MedBankIconBadge(
                    icon = Icons.Default.CardMembership,
                    badgeColor = AmberWarning,
                    backgroundColor = AmberWarningPastel,
                    size = 42.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${stats["activePlans"] ?: 0}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(text = "Active Plans", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Text("Portal Navigation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Menu List using MedBankListItem inside a MedBankCard
        MedBankCard(
            padding = 8.dp
        ) {
            MedBankListItem(
                title = "Content Manager",
                subtitle = "MBBS years, licensing pathways, subjects & exams",
                icon = Icons.Default.FolderOpen,
                iconColor = BrandBlue,
                iconBgColor = BrandBluePastel,
                onClick = { onNavigate(AdminSection.CONTENT) },
                modifier = Modifier.testTag("admin_nav_content")
            )
            MedBankListItem(
                title = "Membership Plans",
                subtitle = "Manage pass durations, prices, and track scopes",
                icon = Icons.Default.CardMembership,
                iconColor = BrandBlue,
                iconBgColor = BrandBluePastel,
                onClick = { onNavigate(AdminSection.PLANS) },
                modifier = Modifier.testTag("admin_nav_plans")
            )
            MedBankListItem(
                title = "Access Requests",
                subtitle = "Verify payment receipts and activate student access",
                icon = Icons.Default.Verified,
                iconColor = BrandBlue,
                iconBgColor = BrandBluePastel,
                onClick = { onNavigate(AdminSection.REQUESTS) },
                modifier = Modifier.testTag("admin_nav_requests")
            )
            MedBankListItem(
                title = "Referral Payouts",
                subtitle = "Manage student commissions & mark bank transfers as paid",
                icon = Icons.Default.Paid,
                iconColor = BrandSuccess,
                iconBgColor = BrandSuccessPastel,
                trailingText = "${stats["pendingPayouts"] ?: 0} Pending",
                onClick = { onNavigate(AdminSection.PAYOUTS) },
                modifier = Modifier.testTag("admin_nav_payouts")
            )
            MedBankListItem(
                title = "Bulk Import",
                subtitle = "Upload exam questions & explanations via pipe format",
                icon = Icons.Default.CloudUpload,
                iconColor = BrandBlue,
                iconBgColor = BrandBluePastel,
                onClick = { onNavigate(AdminSection.BULK_IMPORT) },
                modifier = Modifier.testTag("admin_nav_bulk_import")
            )
            MedBankListItem(
                title = "Billing Accounts",
                subtitle = "Manage bank accounts, JazzCash, Easypaisa & payment details",
                icon = Icons.Default.AccountBalance,
                iconColor = BrandBlue,
                iconBgColor = BrandBluePastel,
                onClick = { onNavigate(AdminSection.BILLING_ACCOUNTS) },
                modifier = Modifier.testTag("admin_nav_billing")
            )
            MedBankListItem(
                title = "Legal Content",
                subtitle = "Edit Privacy Policy & Terms and Conditions",
                icon = Icons.Default.Gavel,
                iconColor = BrandBlue,
                iconBgColor = BrandBluePastel,
                onClick = { onNavigate(AdminSection.LEGAL) },
                modifier = Modifier.testTag("admin_nav_legal")
            )
            MedBankListItem(
                title = "Settings",
                subtitle = "Configure WhatsApp contact & announcements banner",
                icon = Icons.Default.Settings,
                iconColor = BrandBlue,
                iconBgColor = BrandBluePastel,
                showDivider = false,
                onClick = { onNavigate(AdminSection.SETTINGS) },
                modifier = Modifier.testTag("admin_nav_settings")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRequestsView(viewModel: MedBankViewModel) {
    val requests by viewModel.repository.observeAllAccessRequests().collectAsState(initial = emptyList())
    val allPlans by viewModel.repository.observeAllPlans().collectAsState(initial = emptyList())
    val context = LocalContext.current

    var selectedReq by remember { mutableStateOf<AccessRequest?>(null) }
    var adminNote by remember { mutableStateOf("") }
    var overridePlan by remember { mutableStateOf<MembershipPlan?>(null) }
    var planDropdownExpanded by remember { mutableStateOf(false) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    if (requests.isEmpty()) {
        EmptyPlaceholder(
            icon = Icons.Default.DoneAll,
            title = "No Access Requests",
            subtitle = "When students request locked exams and submit payment proof, they will show up here."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests) { req ->
                val isPdf = req.paymentProofUrl.contains(".pdf", ignoreCase = true) ||
                        req.paymentProofUrl.contains("application%2Fpdf", ignoreCase = true)
                val hasProof = req.paymentProofUrl.isNotBlank() && req.paymentProofUrl != "payment_receipt.jpg"

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("admin_request_card_${req.id}"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(req.studentName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            StatusBadge(
                                text = req.status.uppercase(),
                                color = when (req.status) {
                                    "approved" -> MaterialTheme.colorScheme.primary
                                    "rejected" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CNIC: ${req.cnic} • Plan: ${req.planName.ifBlank { req.planId }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Scope: ${req.scopeType.uppercase()} (${req.scopeId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        // Bug 5: Payment proof display on request card
                        if (hasProof) {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (isPdf) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(req.paymentProofUrl)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.util.Log.e("MEDBANK_PDF", "Cannot open PDF: ${e.message}")
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open PDF Receipt", fontSize = 12.sp)
                                }
                            } else {
                                AsyncImage(
                                    model = req.paymentProofUrl,
                                    contentDescription = "Payment Receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { fullScreenImageUrl = req.paymentProofUrl },
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    text = "Tap to enlarge receipt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else if (req.paymentProofUrl.isNotBlank()) {
                            Text("Payment Proof: ${req.paymentProofUrl}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }

                        if (req.adminNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Note: ${req.adminNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }

                        if (req.status == "pending") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        selectedReq = req
                                        overridePlan = allPlans.find { it.id == req.planId }
                                        adminNote = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Approve")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.rejectRequest(req, "Invalid payment proof.")
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reject", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail & Approval Dialog with Plan Override and Payment Proof Viewer
    if (selectedReq != null) {
        val req = selectedReq!!
        val isPdf = req.paymentProofUrl.contains(".pdf", ignoreCase = true) ||
                req.paymentProofUrl.contains("application%2Fpdf", ignoreCase = true)
        val hasProof = req.paymentProofUrl.isNotBlank() && req.paymentProofUrl != "payment_receipt.jpg"

        AlertDialog(
            onDismissRequest = { selectedReq = null },
            title = { Text("Approve Access for ${req.studentName}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("CNIC: ${req.cnic}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Requested Plan: ${req.planName.ifBlank { req.planId }}", style = MaterialTheme.typography.bodyMedium)

                    // Bug 3: Admin Plan Override Dropdown
                    Text("Override Plan (Optional):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    val activePlansList = allPlans.filter { it.isActive }
                    val currentSelectedPlanName = overridePlan?.name ?: if (activePlansList.any { it.id == req.planId }) {
                        activePlansList.first { it.id == req.planId }.name
                    } else {
                        req.planName.ifBlank { "Select Plan to Override" }
                    }

                    ExposedDropdownMenuBox(
                        expanded = planDropdownExpanded,
                        onExpandedChange = { planDropdownExpanded = !planDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentSelectedPlanName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Plan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = planDropdownExpanded,
                            onDismissRequest = { planDropdownExpanded = false }
                        ) {
                            activePlansList.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.durationDays} days • ${p.currency} ${p.price})") },
                                    onClick = {
                                        overridePlan = p
                                        planDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    val effectiveDuration = overridePlan?.durationDays ?: 30
                    Text(
                        text = "Granting $effectiveDuration days entitlement under ${overridePlan?.name ?: req.planName.ifBlank { "Requested Pass" }}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Bug 5: Payment Proof Preview in dialog
                    if (hasProof) {
                        Text("Payment Proof Receipt:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        if (isPdf) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(req.paymentProofUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MEDBANK_PDF", "Cannot open PDF: ${e.message}")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open PDF Proof")
                            }
                        } else {
                            AsyncImage(
                                model = req.paymentProofUrl,
                                contentDescription = "Payment Receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { fullScreenImageUrl = req.paymentProofUrl },
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = "Tap image to view full screen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = { Text("Admin Note (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                // Bug 1: Approval writes to Firestore only, does NOT sign out or switch auth session
                Button(
                    onClick = {
                        val requestToApprove = selectedReq!!
                        val planToGrant = overridePlan
                        viewModel.approveRequest(requestToApprove, adminNote, overridePlan = planToGrant)
                        selectedReq = null
                        adminNote = ""
                        overridePlan = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Confirm Approval")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedReq = null
                    overridePlan = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bug 5: Full-screen Image Viewer on tap
    if (fullScreenImageUrl != null) {
        Dialog(
            onDismissRequest = { fullScreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { fullScreenImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fullScreenImageUrl,
                    contentDescription = "Full Screen Payment Receipt",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullScreenImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Full Screen",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminUsersView(viewModel: MedBankViewModel) {
    val users by viewModel.repository.observeAllUsers().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForDeviceReset by remember { mutableStateOf<User?>(null) }
    var selectedUserForPromotion by remember { mutableStateOf<User?>(null) }

    val filtered = users.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) || it.cnic.contains(searchQuery)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or CNIC...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { u ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(u.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            StatusBadge(
                                text = u.role.uppercase(),
                                color = if (u.role == "admin") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text("CNIC: ${u.cnic} • Email: ${u.email}", style = MaterialTheme.typography.bodySmall)
                        Text("Track: ${u.programType.uppercase()} ${u.mbbsYear?.let { "• Y$it" } ?: ""} ${u.licensingPath ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Reset Device
                            OutlinedButton(
                                onClick = { selectedUserForDeviceReset = u },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Device", fontSize = 12.sp)
                            }

                            // Promote to admin if student
                            if (u.role != "admin") {
                                Button(
                                    onClick = { selectedUserForPromotion = u },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Make Admin", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedUserForDeviceReset != null) {
        AlertDialog(
            onDismissRequest = { selectedUserForDeviceReset = null },
            title = { Text("Reset User Device Binding") },
            text = { Text("This will revoke active device tokens for ${selectedUserForDeviceReset!!.fullName}. They will be able to log in on a new device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDevice(selectedUserForDeviceReset!!.uid)
                        selectedUserForDeviceReset = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Confirm Reset") }
            },
            dismissButton = { TextButton(onClick = { selectedUserForDeviceReset = null }) { Text("Cancel") } }
        )
    }

    if (selectedUserForPromotion != null) {
        AlertDialog(
            onDismissRequest = { selectedUserForPromotion = null },
            title = { Text("Promote User to Admin") },
            text = { Text("Are you sure you want to promote ${selectedUserForPromotion!!.fullName} to Admin? They will receive full access to the Admin Portal.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.promoteUser(selectedUserForPromotion!!.uid)
                        selectedUserForPromotion = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Confirm Promotion") }
            },
            dismissButton = { TextButton(onClick = { selectedUserForPromotion = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminContentView(viewModel: MedBankViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var scaffoldSuccess by remember { mutableStateOf(false) }

    val allYears by viewModel.repository.observeAllYears().collectAsState(initial = emptyList())
    val allPathways by viewModel.repository.observeAllPathways().collectAsState(initial = emptyList())
    val allSubjects by viewModel.repository.observeAllSubjects().collectAsState(initial = emptyList())
    val allExams by viewModel.repository.observeAllExams().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Scaffold Standard Medical Structure
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scaffold Standard Folders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "One-click generate standard folders for MBBS (Years 1 to 6) and Licensing Pathways (NRE, FCPS, USMLE, PLAB, DHA, SMLE).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.scaffoldDefaultStructureIfRequested()
                            scaffoldSuccess = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (scaffoldSuccess) "Standard Folders Ready!" else "Scaffold Standard Structure")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Current Question Bank Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Text("• MBBS Years Configured: ${allYears.size}", style = MaterialTheme.typography.bodyMedium)
        Text("• Licensing Pathways Configured: ${allPathways.size}", style = MaterialTheme.typography.bodyMedium)
        Text("• Total Subjects Created: ${allSubjects.size}", style = MaterialTheme.typography.bodyMedium)
        Text("• Total Exams Published: ${allExams.size}", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(20.dp))

        Text("Published Exams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        if (allExams.isEmpty()) {
            Text("No exams published yet. Use Bulk Import to upload exams.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        } else {
            allExams.forEach { ex ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(ex.title, fontWeight = FontWeight.Bold)
                            Text("${ex.questionCount} Questions • ${ex.durationMin} mins • Scope: ${ex.parentType}", style = MaterialTheme.typography.bodySmall)
                        }

                        IconButton(onClick = {
                            coroutineScope.launch { viewModel.repository.deleteExam(ex.id) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBulkImportView(viewModel: MedBankViewModel) {
    val coroutineScope = rememberCoroutineScope()

    var selectedProgram by remember { mutableStateOf("mbbs") } // "mbbs" or "licensing"
    var selectedParentId by remember { mutableStateOf("year_1") }

    val mbbsYears = listOf(
        "year_1" to "Year 1",
        "year_2" to "Year 2",
        "year_3" to "Year 3",
        "year_4" to "Year 4",
        "year_5" to "Year 5",
        "year_6" to "Year 6"
    )

    val licensingPathways = listOf(
        "nre" to "NRE",
        "fcps" to "FCPS",
        "usmle" to "USMLE",
        "plab" to "PLAB",
        "dha" to "DHA",
        "smle" to "SMLE"
    )

    var subjectsList by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    var examsList by remember { mutableStateOf<List<Exam>>(emptyList()) }
    var selectedExamId by remember { mutableStateOf<String?>(null) }

    var importMode by remember { mutableStateOf("append") } // "append" or "replace"
    var advancedMode by remember { mutableStateOf(false) }

    val sampleQTemplate = remember {
        "Q|What is the primary treatment for acute anaphylaxis?|Epinephrine intramuscular|Intravenous diphenhydramine|Albuterol nebulizer|Methylprednisolone intravenous|A|Epinephrine is the first-line and life-saving treatment for anaphylaxis.|high|emergency;allergy"
    }

    var importText by remember { mutableStateOf(sampleQTemplate) }
    var parseResult by remember { mutableStateOf<BulkImportResult?>(null) }
    var successSnackbar by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showCreateSubjDialog by remember { mutableStateOf(false) }
    var newSubjName by remember { mutableStateOf("") }

    var showCreateExamDialog by remember { mutableStateOf(false) }
    var newExamTitle by remember { mutableStateOf("") }
    var newExamDuration by remember { mutableStateOf("60") }
    var newExamMarks by remember { mutableStateOf("100") }
    var newExamNegative by remember { mutableStateOf("0.25") }
    var newExamAttempts by remember { mutableStateOf("1") }
    var newExamPublished by remember { mutableStateOf(true) }
    var allPlans by remember { mutableStateOf<List<MembershipPlan>>(emptyList()) }
    var selectedPlanIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Load subjects when program or parent changes
    LaunchedEffect(selectedProgram, selectedParentId) {
        val subs = viewModel.repository.getSubjectsByParent(selectedProgram, selectedParentId)
        subjectsList = subs
        selectedSubjectId = subs.firstOrNull()?.id
    }

    // Load exams when subject changes
    LaunchedEffect(selectedSubjectId) {
        if (selectedSubjectId != null) {
            val exms = viewModel.repository.getExamsBySubject(selectedSubjectId!!)
            examsList = exms
            selectedExamId = exms.firstOrNull()?.id
        } else {
            examsList = emptyList()
            selectedExamId = null
        }
    }

    // Load plans
    LaunchedEffect(Unit) {
        allPlans = viewModel.repository.getAllPlans()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Bulk Question Import", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // STEP 1: Program Segmented Control / Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Step 1: Select Program", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = selectedProgram == "mbbs",
                        onClick = {
                            selectedProgram = "mbbs"
                            selectedParentId = "year_1"
                            parseResult = null
                        },
                        label = { Text("MBBS") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedProgram == "licensing",
                        onClick = {
                            selectedProgram = "licensing"
                            selectedParentId = "usmle"
                            parseResult = null
                        },
                        label = { Text("Licensing Exams") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // STEP 2: Year or Pathway Dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (selectedProgram == "mbbs") "Step 2: Select Academic Year" else "Step 2: Select Licensing Pathway",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val options = if (selectedProgram == "mbbs") mbbsYears else licensingPathways
                var expandedParent by remember { mutableStateOf(false) }
                val currentParentName = options.find { it.first == selectedParentId }?.second ?: selectedParentId

                ExposedDropdownMenuBox(
                    expanded = expandedParent,
                    onExpandedChange = { expandedParent = !expandedParent },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentParentName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedParent) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedParent,
                        onDismissRequest = { expandedParent = false }
                    ) {
                        options.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedParentId = id
                                    expandedParent = false
                                    parseResult = null
                                }
                            )
                        }
                    }
                }
            }
        }

        // STEP 3: Subject Dropdown & + Create new subject
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Step 3: Select Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                var expandedSubj by remember { mutableStateOf(false) }
                val currentSubjName = subjectsList.find { it.id == selectedSubjectId }?.name ?: if (subjectsList.isEmpty()) "No subjects found" else "Select subject"

                ExposedDropdownMenuBox(
                    expanded = expandedSubj,
                    onExpandedChange = { expandedSubj = !expandedSubj },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentSubjName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubj) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSubj,
                        onDismissRequest = { expandedSubj = false }
                    ) {
                        subjectsList.forEach { subj ->
                            DropdownMenuItem(
                                text = { Text(subj.name) },
                                onClick = {
                                    selectedSubjectId = subj.id
                                    expandedSubj = false
                                    parseResult = null
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Create new subject", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                expandedSubj = false
                                newSubjName = ""
                                showCreateSubjDialog = true
                            }
                        )
                    }
                }
            }
        }

        // STEP 4: Exam Dropdown & + Create new exam
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Step 4: Select Target Exam", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                var expandedExam by remember { mutableStateOf(false) }
                val currentExamName = examsList.find { it.id == selectedExamId }?.title ?: if (examsList.isEmpty()) "No exams found in subject" else "Select exam"

                ExposedDropdownMenuBox(
                    expanded = expandedExam,
                    onExpandedChange = { expandedExam = !expandedExam },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentExamName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExam) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedExam,
                        onDismissRequest = { expandedExam = false }
                    ) {
                        examsList.forEach { ex ->
                            DropdownMenuItem(
                                text = { Text("${ex.title} (${ex.questionCount} Qs)") },
                                onClick = {
                                    selectedExamId = ex.id
                                    expandedExam = false
                                    parseResult = null
                                }
                            )
                        }
                        if (selectedSubjectId != null) {
                            DropdownMenuItem(
                                text = { Text("+ Create new exam", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    expandedExam = false
                                    newExamTitle = ""
                                    newExamDuration = "60"
                                    newExamMarks = "100"
                                    newExamNegative = "0.25"
                                    newExamAttempts = "1"
                                    newExamPublished = true
                                    selectedPlanIds = emptySet()
                                    showCreateExamDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // STEP 5: Import Mode
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Step 5: Import Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { importMode = "append" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = importMode == "append", onClick = { importMode = "append" })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Append to existing questions")
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { importMode = "replace" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = importMode == "replace", onClick = { importMode = "replace" })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Replace all questions in this exam")
                }
            }
        }

        // Empty state prompt if needed
        if (subjectsList.isEmpty() || examsList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Prompt: Create a subject and exam first, or use the Content Manager.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // STEP 6: Paste Area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Step 6: Paste Questions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        importText = if (advancedMode) {
                            "EXAM|Mock Exam|Cardiology|mbbs|year_3|60|100|0.25||published\n$sampleQTemplate"
                        } else {
                            sampleQTemplate
                        }
                        parseResult = null
                    }) {
                        Text("Load Sample", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    text = if (advancedMode)
                        "Advanced Mode: Line 1: EXAM|title|subject|parentType|parentId|duration|marks|negative|plan|status\nFollowing lines: Q|text|opt1|opt2|opt3|opt4|correct|explanation|difficulty|tags"
                    else
                        "Format: Q|text|opt1|opt2|opt3|opt4|correctIndex (A/B/C/D or 0/3)|explanation|difficulty|tags",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = importText,
                    onValueChange = {
                        importText = it
                        parseResult = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("bulk_import_textfield"),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text(sampleQTemplate) }
                )
            }
        }

        // Advanced Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth().clickable { advancedMode = !advancedMode },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = advancedMode, onCheckedChange = { advancedMode = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("Advanced mode — paste full EXAM metadata line", style = MaterialTheme.typography.bodySmall)
        }

        // STEP 7: Actions (Preview & Commit)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (advancedMode) {
                        parseResult = viewModel.repository.parseBulkImport(importText)
                    } else {
                        val examId = selectedExamId ?: "temp_exam"
                        parseResult = viewModel.repository.parseContextBulkImport(importText, examId)
                    }
                },
                enabled = selectedExamId != null || advancedMode,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).testTag("validate_import_button")
            ) {
                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview Import")
            }

            val canCommit = parseResult != null && parseResult!!.success && (selectedExamId != null || (advancedMode && parseResult!!.exam != null))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val res = parseResult!!
                        if (advancedMode) {
                            viewModel.repository.commitBulkImport(
                                exam = res.exam!!,
                                subjectName = "General",
                                questions = res.questions,
                                answers = res.answers
                            )
                        } else {
                            viewModel.repository.commitContextBulkImport(
                                examId = selectedExamId!!,
                                questions = res.questions,
                                answers = res.answers,
                                replaceMode = importMode == "replace"
                            )
                        }
                        val examTitle = if (advancedMode) res.exam?.title else examsList.find { it.id == selectedExamId }?.title ?: "Exam"
                        successSnackbar = "Successfully Imported ${res.questions.size} questions into $examTitle"
                        parseResult = null
                        importText = ""
                        viewModel.refreshAdminStats()
                    }
                },
                enabled = canCommit,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f).testTag("commit_import_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Commit Import")
            }
        }

        // Success Feedback
        if (successSnackbar != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = successSnackbar!!,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Preview Table & Validation Errors
        if (parseResult != null) {
            val res = parseResult!!
            if (!res.success) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Validation Errors Found:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        res.errors.forEach { err ->
                            Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Validation Passed! Preview Table (${res.questions.size} questions):", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        res.questions.take(15).forEachIndexed { index, q ->
                            val ans = res.answers.getOrNull(index)
                            val correctChar = when (ans?.correctIndex) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                3 -> "D"
                                else -> "?"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("#${q.order}", fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                                Text(q.text.take(50) + "...", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text(" Ans: $correctChar ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                StatusBadge(text = "✓", color = MaterialTheme.colorScheme.primary)
                            }
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create New Subject
    if (showCreateSubjDialog) {
        AlertDialog(
            onDismissRequest = { showCreateSubjDialog = false },
            title = { Text("Create New Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newSubjName,
                        onValueChange = { newSubjName = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjName.isNotBlank()) {
                            coroutineScope.launch {
                                val newId = "subj_${newSubjName.lowercase().replace(" ", "_")}_${java.util.UUID.randomUUID().toString().take(4)}"
                                val subject = Subject(
                                    id = newId,
                                    parentType = selectedProgram,
                                    parentId = selectedParentId,
                                    name = newSubjName.trim(),
                                    order = subjectsList.size + 1,
                                    isActive = true
                                )
                                viewModel.repository.saveSubject(subject)
                                val subs = viewModel.repository.getSubjectsByParent(selectedProgram, selectedParentId)
                                subjectsList = subs
                                selectedSubjectId = newId
                                showCreateSubjDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSubjDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Create New Exam
    if (showCreateExamDialog) {
        AlertDialog(
            onDismissRequest = { showCreateExamDialog = false },
            title = { Text("Create New Exam") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newExamTitle,
                        onValueChange = { newExamTitle = it },
                        label = { Text("Exam Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newExamDuration,
                        onValueChange = { newExamDuration = it },
                        label = { Text("Duration (minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newExamMarks,
                        onValueChange = { newExamMarks = it },
                        label = { Text("Total Marks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newExamNegative,
                        onValueChange = { newExamNegative = it },
                        label = { Text("Negative Marking (e.g. 0.25)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newExamAttempts,
                        onValueChange = { newExamAttempts = it },
                        label = { Text("Max Attempts (1, 2, or 3)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Published")
                        Switch(checked = newExamPublished, onCheckedChange = { newExamPublished = it })
                    }
                    Text("Access Plans (Optional):", style = MaterialTheme.typography.labelMedium)
                    allPlans.forEach { plan ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedPlanIds = if (selectedPlanIds.contains(plan.id)) {
                                    selectedPlanIds - plan.id
                                } else {
                                    selectedPlanIds + plan.id
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedPlanIds.contains(plan.id),
                                onCheckedChange = { checked ->
                                    selectedPlanIds = if (checked) selectedPlanIds + plan.id else selectedPlanIds - plan.id
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(plan.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newExamTitle.isNotBlank() && selectedSubjectId != null) {
                            coroutineScope.launch {
                                val examId = "exam_${java.util.UUID.randomUUID().toString().take(8)}"
                                val exam = Exam(
                                    id = examId,
                                    title = newExamTitle.trim(),
                                    subjectId = selectedSubjectId!!,
                                    parentType = selectedProgram,
                                    parentId = selectedParentId,
                                    durationMin = newExamDuration.toIntOrNull() ?: 60,
                                    totalMarks = newExamMarks.toDoubleOrNull() ?: 100.0,
                                    negativeMarking = newExamNegative.toDoubleOrNull() ?: 0.25,
                                    maxAttempts = newExamAttempts.toIntOrNull() ?: 1,
                                    offlineEnabled = true,
                                    isPublished = newExamPublished,
                                    accessPlanIds = selectedPlanIds.toList(),
                                    questionCount = 0
                                )
                                viewModel.repository.saveExam(exam)
                                val exms = viewModel.repository.getExamsBySubject(selectedSubjectId!!)
                                examsList = exms
                                selectedExamId = examId
                                showCreateExamDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateExamDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPlansView(viewModel: MedBankViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val plans by viewModel.repository.observeAllPlans().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<MembershipPlan?>(null) }

    var planName by remember { mutableStateOf("") }
    var planPrice by remember { mutableStateOf("49.99") }
    var planDuration by remember { mutableStateOf("30") }
    var planCurrency by remember { mutableStateOf("USD") }

    // Granular Scope: [ All Access ] [ Entire Track ] [ Specific Year / Pathway ] [ Specific Exam ]
    var planScope by remember { mutableStateOf("all_access") } // "all_access", "track", "year", "exam"
    var selectedTargetId by remember { mutableStateOf("all_access") }
    var scopeName by remember { mutableStateOf("All Access") }

    val trackOptions = listOf(
        "track_mbbs" to "MBBS",
        "track_licensing" to "Licensing",
        "track_past_papers" to "Past Papers",
        "track_mock_exams" to "Mock Exams"
    )

    val yearPathwayOptions = listOf(
        "mbbs_year_1" to "MBBS Year 1",
        "mbbs_year_2" to "MBBS Year 2",
        "mbbs_year_3" to "MBBS Year 3",
        "mbbs_year_4" to "MBBS Year 4",
        "mbbs_year_5" to "MBBS Year 5",
        "mbbs_year_6" to "MBBS Year 6",
        "licensing_nre" to "NRE",
        "licensing_fcps" to "FCPS",
        "licensing_usmle" to "USMLE",
        "licensing_plab" to "PLAB",
        "licensing_dha" to "DHA",
        "licensing_smle" to "SMLE"
    )

    val allExamsList by viewModel.repository.observeAllExams().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Membership Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Configure granular access plans (All Access, Track, Year, Exam)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Button(
                onClick = {
                    planToEdit = null
                    planName = "All Access Pass"
                    planPrice = "49.99"
                    planDuration = "30"
                    planCurrency = "USD"
                    planScope = "all_access"
                    selectedTargetId = "all_access"
                    scopeName = "All Access"
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_add_plan_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Plan")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (plans.isEmpty()) {
            EmptyPlaceholder(
                icon = Icons.Default.CardMembership,
                title = "No Membership Plans",
                subtitle = "Create access plans assigned to All Access, Tracks, Academic Years, or Specific Exams.",
                actionLabel = "Create Default All Access Plan",
                onActionClick = {
                    coroutineScope.launch {
                        viewModel.repository.savePlan(
                            MembershipPlan(
                                id = "plan_all_access_standard",
                                name = "All Access Full Pass",
                                price = 49.99,
                                currency = "USD",
                                durationDays = 30,
                                scopeType = "all_access",
                                scopeId = "all_access",
                                scopeName = "All Access",
                                features = listOf("All MBBS Years & Pathways", "All Past Papers & Mocks", "Full Explanations")
                            )
                        )
                    }
                }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(plans) { p ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().testTag("plan_item_${p.id}"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${p.currency} ${p.price} • ${p.durationDays} Days",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val scopeBadgeText = when (p.scopeType.lowercase()) {
                                    "all_access" -> "All Access (${p.scopeId})"
                                    "track" -> "Track: ${p.scopeName.ifBlank { p.scopeId }}"
                                    "year" -> "Year/Pathway: ${p.scopeName.ifBlank { p.scopeId }}"
                                    "exam" -> "Exam: ${p.scopeName.ifBlank { p.scopeId }}"
                                    else -> "Scope: ${p.scopeName.ifBlank { p.scopeId }}"
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = scopeBadgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        planToEdit = p
                                        planName = p.name
                                        planPrice = p.price.toString()
                                        planCurrency = p.currency
                                        planDuration = p.durationDays.toString()
                                        planScope = p.scopeType
                                        selectedTargetId = p.scopeId
                                        scopeName = p.scopeName.ifBlank { p.name }
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.testTag("edit_plan_${p.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Plan", tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch { viewModel.repository.deletePlan(p.id) }
                                    },
                                    modifier = Modifier.testTag("delete_plan_${p.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Plan", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var scopeDropdownExpanded by remember { mutableStateOf(false) }
        var targetDropdownExpanded by remember { mutableStateOf(false) }

        val scopeOptions = listOf(
            "all_access" to "All Access",
            "track" to "Entire Track",
            "year" to "Specific Year / Pathway",
            "exam" to "Specific Exam"
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (planToEdit != null) "Edit Membership Plan" else "Create Membership Plan",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Plan Scope Dropdown
                    Text("Plan Scope *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    val currentScopeLabel = scopeOptions.find { it.first == planScope }?.second ?: "All Access"
                    ExposedDropdownMenuBox(
                        expanded = scopeDropdownExpanded,
                        onExpandedChange = { scopeDropdownExpanded = !scopeDropdownExpanded },
                        modifier = Modifier.fillMaxWidth().testTag("plan_scope_dropdown")
                    ) {
                        OutlinedTextField(
                            value = currentScopeLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Scope") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scopeDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = scopeDropdownExpanded,
                            onDismissRequest = { scopeDropdownExpanded = false }
                        ) {
                            scopeOptions.forEach { (scKey, scLabel) ->
                                DropdownMenuItem(
                                    text = { Text(scLabel) },
                                    onClick = {
                                        planScope = scKey
                                        scopeDropdownExpanded = false
                                        when (scKey) {
                                            "all_access" -> {
                                                selectedTargetId = "all_access"
                                                scopeName = "All Access"
                                                if (planName.isBlank() || planName.contains("Pass")) planName = "All Access Pass"
                                            }
                                            "track" -> {
                                                selectedTargetId = "track_mbbs"
                                                scopeName = "MBBS Track"
                                                if (planName.isBlank() || planName.contains("Pass")) planName = "MBBS Track Pass"
                                            }
                                            "year" -> {
                                                selectedTargetId = "mbbs_year_1"
                                                scopeName = "MBBS Year 1"
                                                if (planName.isBlank() || planName.contains("Pass")) planName = "MBBS Year 1 Pass"
                                            }
                                            "exam" -> {
                                                val firstEx = allExamsList.firstOrNull()
                                                val exId = if (firstEx != null) {
                                                    if (firstEx.id.startsWith("exam_")) firstEx.id else "exam_${firstEx.id}"
                                                } else "exam_custom"
                                                selectedTargetId = exId
                                                scopeName = firstEx?.title ?: "Single Exam"
                                                if (planName.isBlank() || planName.contains("Pass")) planName = "${scopeName} Pass"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 2. Dynamic Target Dropdown
                    when (planScope) {
                        "track" -> {
                            Text("Dynamic Target: Select Track *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            val currentTrackName = trackOptions.find { it.first == selectedTargetId }?.second ?: "MBBS"
                            ExposedDropdownMenuBox(
                                expanded = targetDropdownExpanded,
                                onExpandedChange = { targetDropdownExpanded = !targetDropdownExpanded },
                                modifier = Modifier.fillMaxWidth().testTag("plan_track_dropdown")
                            ) {
                                OutlinedTextField(
                                    value = currentTrackName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target Track") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = targetDropdownExpanded,
                                    onDismissRequest = { targetDropdownExpanded = false }
                                ) {
                                    trackOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt.second) },
                                            onClick = {
                                                selectedTargetId = opt.first
                                                scopeName = "${opt.second} Track"
                                                if (planName.isBlank() || planName.contains("Pass")) planName = "${opt.second} Track Pass"
                                                targetDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "year" -> {
                            Text("Dynamic Target: Select Academic Year / Pathway *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            val currentYearName = yearPathwayOptions.find { it.first == selectedTargetId }?.second ?: "MBBS Year 1"
                            ExposedDropdownMenuBox(
                                expanded = targetDropdownExpanded,
                                onExpandedChange = { targetDropdownExpanded = !targetDropdownExpanded },
                                modifier = Modifier.fillMaxWidth().testTag("plan_year_pathway_dropdown")
                            ) {
                                OutlinedTextField(
                                    value = currentYearName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target Year / Pathway") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = targetDropdownExpanded,
                                    onDismissRequest = { targetDropdownExpanded = false }
                                ) {
                                    yearPathwayOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt.second) },
                                            onClick = {
                                                selectedTargetId = opt.first
                                                scopeName = opt.second
                                                if (planName.isBlank() || planName.contains("Pass")) planName = "${opt.second} Pass"
                                                targetDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "exam" -> {
                            Text("Dynamic Target: Select Specific Exam *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            val currentExamTitle = allExamsList.find {
                                val normalizedId = if (it.id.startsWith("exam_")) it.id else "exam_${it.id}"
                                normalizedId == selectedTargetId || it.id == selectedTargetId
                            }?.title ?: scopeName.ifBlank { "Select Exam" }

                            ExposedDropdownMenuBox(
                                expanded = targetDropdownExpanded,
                                onExpandedChange = { targetDropdownExpanded = !targetDropdownExpanded },
                                modifier = Modifier.fillMaxWidth().testTag("plan_exam_target_dropdown")
                            ) {
                                OutlinedTextField(
                                    value = currentExamTitle,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target Exam") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = targetDropdownExpanded,
                                    onDismissRequest = { targetDropdownExpanded = false }
                                ) {
                                    if (allExamsList.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No exams found in Firestore") },
                                            onClick = { targetDropdownExpanded = false }
                                        )
                                    } else {
                                        allExamsList.forEach { ex ->
                                            DropdownMenuItem(
                                                text = { Text(ex.title) },
                                                onClick = {
                                                    val normId = if (ex.id.startsWith("exam_")) ex.id else "exam_${ex.id}"
                                                    selectedTargetId = normId
                                                    scopeName = ex.title
                                                    if (planName.isBlank() || planName.contains("Pass")) planName = "${ex.title} Pass"
                                                    targetDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            // "all_access" has no dynamic target dropdown needed
                        }
                    }

                    // 3. Auto-filled Scope Name & Scope ID preview
                    OutlinedTextField(
                        value = scopeName,
                        onValueChange = { scopeName = it },
                        label = { Text("Scope Name (Display Label) *") },
                        placeholder = { Text("e.g. MBBS Year 1, NRE Complete, Past Paper 2023") },
                        modifier = Modifier.fillMaxWidth().testTag("plan_input_scope_name"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Unlocks ID: $selectedTargetId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Plan Name
                    OutlinedTextField(
                        value = planName,
                        onValueChange = { planName = it },
                        label = { Text("Plan Name *") },
                        placeholder = { Text("e.g. MBBS Year 1 Full Access") },
                        modifier = Modifier.fillMaxWidth().testTag("plan_input_name"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = planPrice,
                            onValueChange = { planPrice = it },
                            label = { Text("Price") },
                            modifier = Modifier.weight(1f).testTag("plan_input_price"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = planCurrency,
                            onValueChange = { planCurrency = it },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f).testTag("plan_input_currency"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = planDuration,
                        onValueChange = { planDuration = it },
                        label = { Text("Duration (Days)") },
                        modifier = Modifier.fillMaxWidth().testTag("plan_input_duration"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (planName.isNotBlank()) {
                            coroutineScope.launch {
                                val planId = planToEdit?.id ?: "plan_${UUID.randomUUID().toString().take(6)}"
                                val plan = MembershipPlan(
                                    id = planId,
                                    name = planName.trim(),
                                    price = planPrice.toDoubleOrNull() ?: 29.99,
                                    currency = planCurrency.trim().ifBlank { "USD" },
                                    durationDays = planDuration.toIntOrNull() ?: 30,
                                    scopeType = planScope,
                                    scopeId = selectedTargetId,
                                    scopeName = scopeName.trim().ifBlank { planName.trim() },
                                    features = planToEdit?.features?.ifEmpty { null } ?: listOf(
                                        "Full explanations & clinical rationales",
                                        "Timed mock exams with negative marking",
                                        "Unlimited practice attempts"
                                    ),
                                    isActive = true
                                )
                                viewModel.repository.savePlan(plan)
                                showAddDialog = false
                                planToEdit = null
                                planName = ""
                            }
                        }
                    },
                    enabled = planName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("plan_save_button")
                ) {
                    Text(if (planToEdit != null) "Update Plan" else "Save Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    planToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminSettingsView(viewModel: MedBankViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.appSettings.collectAsState()

    var whatsappNumber by remember(settings) { mutableStateOf(settings.adminWhatsapp) }
    var bannerText by remember(settings) { mutableStateOf(settings.announcementBanner) }
    var saveSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Application Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = whatsappNumber,
            onValueChange = { whatsappNumber = it },
            label = { Text("Admin WhatsApp (with country code)") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bannerText,
            onValueChange = { bannerText = it },
            label = { Text("Announcement Banner Message") },
            leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Device Enforcement Policy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text("• Maximum Devices Allowed Per User: 1 (Strictly Enforced)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text("• Offline Pack Expiry: 7 Days (Device-Bound)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.repository.updateSettings(whatsappNumber, bannerText)
                    saveSuccess = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(if (saveSuccess) "Settings Saved!" else "Update Settings")
        }
    }
}
