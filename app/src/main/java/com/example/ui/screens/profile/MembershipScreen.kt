package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.MembershipPlan
import com.example.ui.MedBankViewModel
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankChip
import com.example.ui.components.MedBankIconBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(
    viewModel: MedBankViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allPlans by viewModel.repository.observeAllPlans().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val user = currentUser
    val activePlans = allPlans.filter { it.isActive }

    var selectedPlanForUpgrade by remember { mutableStateOf<MembershipPlan?>(null) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileBase64 by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitSuccessMsg by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            selectedFileName = it.getString(nameIndex)
                        }
                    }
                }
                if (selectedFileName.isBlank()) {
                    selectedFileName = uri.lastPathSegment ?: "payment_receipt"
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    selectedFileBase64 = "data:$mimeType;base64,$base64Str"
                }
            } catch (e: Exception) {
                selectedFileName = "attachment_receipt"
            }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Subscriptions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("membership_back_button")
                    ) {
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Active Plans
            item {
                Text(
                    text = "Active Subscriptions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (user == null || user.entitlements.isEmpty()) {
                item {
                    MedBankCard(padding = 20.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MedBankIconBadge(
                                icon = Icons.Default.CardMembership,
                                badgeColor = AmberWarning,
                                backgroundColor = AmberWarningPastel,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No Active Subscriptions",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Choose an upgrade plan below to unlock full question banks and explanations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                items(user.entitlements.entries.toList()) { (scopeId, entitlement) ->
                    val dateFormatted = try {
                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(entitlement.expiresAt))
                    } catch (_: Exception) {
                        "Active"
                    }

                    MedBankCard(
                        padding = 16.dp,
                        modifier = Modifier.testTag("active_entitlement_$scopeId")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                MedBankIconBadge(
                                    icon = Icons.Default.Verified,
                                    badgeColor = BrandSuccess,
                                    backgroundColor = BrandSuccessPastel,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Scope Pass: $scopeId",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Expires: $dateFormatted",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            MedBankChip(text = "ACTIVE", color = BrandSuccess, bgColor = BrandSuccessPastel)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val matchingPlan = activePlans.firstOrNull() ?: MembershipPlan(id = "renew_pass", name = "Renewal Pass", price = 29.99, durationDays = 30)
                                selectedPlanForUpgrade = matchingPlan
                                showUpgradeDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Text("Renew Now")
                        }
                    }
                }
            }

            // Section 2: Available Upgrades
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Available Upgrades & Passes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (activePlans.isEmpty()) {
                item {
                    Text(
                        text = "No membership plans configured by admin yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                items(activePlans) { plan ->
                    MedBankCard(
                        padding = 16.dp,
                        modifier = Modifier.testTag("upgrade_plan_card_${plan.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plan.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${plan.currency} ${plan.price} • ${plan.durationDays} Days Duration",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val scopeBadge = if (plan.scopeName.isNotBlank()) {
                                    "Includes: ${plan.scopeName}"
                                } else if (plan.scopeType.equals("all_access", ignoreCase = true)) {
                                    "Includes: All Access"
                                } else if (plan.scopeType.equals("track", ignoreCase = true)) {
                                    "Includes: Entire ${plan.scopeId.removePrefix("track_").uppercase()} Track"
                                } else if (plan.scopeType.equals("year", ignoreCase = true)) {
                                    "Includes: ${plan.scopeId.replace("_", " ").uppercase()}"
                                } else if (plan.scopeType.equals("exam", ignoreCase = true)) {
                                    "Includes: Specific Exam"
                                } else null
                                if (scopeBadge != null) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = scopeBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            MedBankChip(text = "${plan.durationDays} DAYS", color = BrandBlue, bgColor = BrandBluePastel)
                        }

                        if (plan.features.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            plan.features.forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = BrandSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = feature,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                selectedPlanForUpgrade = plan
                                selectedFileUri = null
                                selectedFileName = ""
                                selectedFileBase64 = ""
                                submitSuccessMsg = null
                                showUpgradeDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Text("Upgrade Now")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Upgrade & Payment Proof Upload Dialog
    if (showUpgradeDialog && selectedPlanForUpgrade != null) {
        val plan = selectedPlanForUpgrade!!
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = CardSurface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp),
            title = {
                Text(
                    text = "Request Plan: ${plan.name}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Price: ${plan.currency} ${plan.price} for ${plan.durationDays} days access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (!submitSuccessMsg.isNullOrBlank()) {
                        Surface(
                            color = BrandSuccessPastel,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = submitSuccessMsg!!,
                                color = BrandSuccess,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Text(
                        text = "Attach Payment Proof (Screenshot / Receipt)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedCard(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = CardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (selectedFileUri != null) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (selectedFileUri != null) selectedFileName else "Attach receipt (Image/PDF)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        fontWeight = if (selectedFileUri != null) FontWeight.SemiBold else FontWeight.Normal,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (selectedFileUri != null) "File attached" else "Supports JPG, PNG, PDF",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedFileUri != null) "Change" else "Browse",
                                color = BrandBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (user != null && selectedFileBase64.isNotBlank()) {
                            isSubmitting = true
                            coroutineScope.launch {
                                try {
                                    viewModel.repository.createAccessRequest(
                                        uid = user.uid,
                                        studentName = user.fullName,
                                        cnic = user.cnic,
                                        scopeType = plan.scopeType,
                                        scopeId = plan.scopeIds.firstOrNull() ?: "pass_${plan.id}",
                                        planId = plan.id,
                                        paymentProofBase64 = selectedFileBase64,
                                        whatsappSent = false,
                                        planName = plan.name
                                    )
                                    submitSuccessMsg = "Request submitted successfully! Awaiting admin approval."
                                    isSubmitting = false
                                } catch (e: Exception) {
                                    isSubmitting = false
                                    submitSuccessMsg = "Error submitting request: ${e.message}"
                                }
                            }
                        } else {
                            submitSuccessMsg = "Please attach a payment proof receipt first."
                        }
                    },
                    enabled = !isSubmitting && selectedFileBase64.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Submit Payment Proof & Request", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}
