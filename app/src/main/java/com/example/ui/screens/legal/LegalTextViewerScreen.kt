package com.example.ui.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankIconBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalTextViewerScreen(
    type: String, // "privacy" or "terms"
    viewModel: MedBankViewModel,
    onBack: () -> Unit
) {
    val legalContent by viewModel.legalContent.collectAsState()
    val isPrivacy = type.equals("privacy", ignoreCase = true)
    val title = if (isPrivacy) "Privacy Policy" else "Terms & Conditions"

    LaunchedEffect(type) {
        viewModel.loadLegalContent()
    }

    val rawText = if (isPrivacy) {
        legalContent?.privacyPolicy
    } else {
        legalContent?.termsAndConditions
    }

    val displayText = if (rawText.isNullOrBlank()) "Content not available yet." else rawText

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("legal_viewer_back_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            MedBankCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MedBankIconBadge(
                        icon = if (isPrivacy) Icons.Default.Security else Icons.Default.Description,
                        badgeColor = if (isPrivacy) BrandBlue else BrandRed,
                        backgroundColor = if (isPrivacy) BrandBluePastel else BrandRedPastel
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val dateFormatted = legalContent?.updatedAt?.takeIf { it > 0 }?.let {
                            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Current Version"
                        Text(
                            text = "Last updated: $dateFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Document Content in a scrollable MedBankCard
            MedBankCard(
                modifier = Modifier.testTag("legal_content_card")
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (displayText == "Content not available yet.") TextSecondary else TextPrimary,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
