package com.example.ui.screens.access

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.MembershipPlan
import com.example.ui.MedBankViewModel
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestAccessBottomSheet(viewModel: MedBankViewModel) {
    val sheetData by viewModel.accessSheet.collectAsState()
    val trackPlans by viewModel.trackPlans.collectAsState()
    val isSubmitting by viewModel.isSubmittingRequest.collectAsState()
    val statusMsg by viewModel.accessRequestStatusMsg.collectAsState()
    val context = LocalContext.current

    if (sheetData != null) {
        val exam = sheetData!!.exam

        LaunchedEffect(exam.id, exam.parentType, exam.parentId) {
            viewModel.reloadPlansForExam(exam)
        }

        var selectedPlan by remember(trackPlans) {
            mutableStateOf<MembershipPlan?>(trackPlans.firstOrNull())
        }

        var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
        var selectedFileName by remember { mutableStateOf("") }
        var whatsappRequested by remember { mutableStateOf(true) }

        // Bug 5: ActivityResultContracts.GetContent() file picker for image/* and application/pdf
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedFileUri = uri
                val name = try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) cursor.getString(nameIndex) else null
                        } else null
                    }
                } catch (_: Exception) {
                    null
                }
                selectedFileName = name ?: uri.lastPathSegment ?: "receipt_file"
            }
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeAccessRequestSheet() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unlock Question Bank",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Edge case: Already unlocked check
                val currentUser by viewModel.currentUser.collectAsState()
                val activeEnt = viewModel.getActiveEntitlementForExam(exam)
                val hasYearUnlocked = currentUser?.let { u ->
                    val now = System.currentTimeMillis()
                    val parentKeys = listOf(
                        exam.parentId,
                        "${exam.parentType}_${exam.parentId}",
                        if (exam.parentId.startsWith("year_")) "mbbs_${exam.parentId}" else "",
                        if (exam.parentType == "mbbs" && !exam.parentId.startsWith("mbbs_")) "mbbs_${exam.parentId}" else "",
                        if (exam.parentType == "licensing" && !exam.parentId.startsWith("licensing_")) "licensing_${exam.parentId}" else ""
                    ).filter { it.isNotBlank() }
                    parentKeys.any { k ->
                        val ent = u.entitlements[k]
                        ent != null && ent.status == "active" && (ent.expiresAt == 0L || ent.expiresAt > now)
                    }
                } ?: false

                if (activeEnt != null) {
                    val messageText = if (hasYearUnlocked) {
                        "You already have access to this exam via your Year plan."
                    } else {
                        "You already have access to this exam via your ${activeEnt.planName.ifBlank { "active" }} plan."
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("already_unlocked_message")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Access Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = messageText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Clickable button to view payment accounts
                Surface(
                    onClick = {
                        viewModel.closeAccessRequestSheet()
                        viewModel.openProfileScreen("billing_accounts")
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_view_payment_accounts")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "💳",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Payment Accounts",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Selectable Membership Plans
                Text(
                    text = "Select Membership Plan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (trackPlans.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("no_plans_found_message")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No active plans found for this track. Please contact support.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    val settings = viewModel.appSettings.value
                                    val adminPhone = settings.adminWhatsapp.replace("+", "").replace(" ", "").trim()
                                    val message = "Hello MedBank Support,\nI would like to request access for: ${exam.title} (${exam.parentType} / ${exam.parentId}), but no active membership plan was found for this track."
                                    val encodedMsg = URLEncoder.encode(message, "UTF-8")
                                    val waUrl = "https://wa.me/$adminPhone?text=$encodedMsg"
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.testTag("contact_support_button")
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Contact Support")
                            }
                        }
                    }
                } else {
                    trackPlans.forEach { plan ->
                        val isSelected = selectedPlan?.id == plan.id
                        ElevatedCard(
                            onClick = { selectedPlan = plan },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("plan_card_${plan.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                        else Modifier
                                    )
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPlan = plan },
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${plan.currency} ${plan.price}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val scopeDisplay = if (plan.scopeName.isNotBlank()) {
                                        "Includes: ${plan.scopeName}"
                                    } else {
                                        when (plan.scopeType.lowercase()) {
                                            "all_access" -> "Includes: All Access"
                                            "track" -> "Includes: Entire ${plan.scopeId.removePrefix("track_").uppercase()} Track"
                                            "year" -> "Includes: ${plan.scopeId.replace("_", " ").uppercase()}"
                                            "exam" -> "Includes: Single Exam"
                                            else -> "Includes: ${plan.name}"
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = scopeDisplay,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Duration: ${plan.durationDays} Days",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    val featuresToShow = if (plan.features.isNotEmpty()) plan.features else listOf(
                                        "Full high-yield clinical explanations",
                                        "Timed mock mode with negative marking"
                                    )

                                    featuresToShow.forEach { feature ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = feature,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Real File Picker (Image/PDF)
                    Text(
                        text = "Payment Proof",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedCard(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_payment_proof_card"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (selectedFileUri != null) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (selectedFileUri != null) selectedFileName else "Attach receipt (Image/PDF)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedFileUri != null) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (selectedFileUri != null) "File attached" else "Supports JPG, PNG, PDF receipts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Text(
                                text = if (selectedFileUri != null) "Change" else "Browse",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Verification Channel (WhatsApp)
                    Text(
                        text = "Verification Channel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Open WhatsApp with prefilled details", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Sends Name, CNIC, and selected Plan to admin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        Switch(
                            checked = whatsappRequested,
                            onCheckedChange = { whatsappRequested = it },
                            modifier = Modifier.testTag("whatsapp_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!statusMsg.isNullOrBlank()) {
                        Text(
                            text = statusMsg ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    // Submit Button: "Upload Payment Proof & Request"
                    Button(
                        onClick = {
                            val planToSubmit = selectedPlan ?: trackPlans.firstOrNull() ?: return@Button
                            viewModel.submitAccessRequestWithFile(
                                fileUri = selectedFileUri,
                                selectedPlan = planToSubmit,
                                launchWhatsApp = whatsappRequested,
                                context = context
                            )
                        },
                        enabled = !isSubmitting && (selectedPlan != null || trackPlans.isNotEmpty()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_access_request_button"),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading & Submitting...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Payment Proof & Request", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
