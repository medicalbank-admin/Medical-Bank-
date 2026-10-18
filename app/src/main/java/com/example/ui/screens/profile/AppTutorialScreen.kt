package com.example.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankIconBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTutorialScreen(
    viewModel: MedBankViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Tutorial & Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("app_tutorial_back_button")
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
            MedBankCard(padding = 20.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MedBankIconBadge(
                        icon = Icons.Default.School,
                        badgeColor = BrandBlue,
                        backgroundColor = BrandBluePastel,
                        size = 50.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Candidate Onboarding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Learn how to navigate question banks, take mock exams, and review clinical explanations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            TutorialStepCard(
                stepNumber = "01",
                title = "Browse Question Banks",
                description = "Navigate to the Banks tab from the bottom navigation. Filter question banks by MBBS year or Medical Licensing pathways (USMLE, PLAB, FCPS).",
                icon = Icons.Default.Folder
            )

            TutorialStepCard(
                stepNumber = "02",
                title = "Unlock & Membership Pass",
                description = "If a subject or exam bank is locked, tap 'Unlock Bank'. Choose your preferred membership duration plan, upload your payment receipt screenshot, and submit your request for fast approval.",
                icon = Icons.Default.CardMembership
            )

            TutorialStepCard(
                stepNumber = "03",
                title = "Timed Mock Exams",
                description = "Start timed exam sessions featuring realistic exam conditions. Use the question palette to jump between questions, flag doubts, and submit when ready.",
                icon = Icons.Default.Timer
            )

            TutorialStepCard(
                stepNumber = "04",
                title = "Review Clinical Explanations",
                description = "Immediately upon submission, view your detailed score breakdown and access comprehensive clinical explanations for every option to deepen your medical mastery.",
                icon = Icons.Default.MenuBook
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TutorialStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    MedBankCard(padding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            MedBankIconBadge(
                icon = icon,
                badgeColor = BrandBlue,
                backgroundColor = BrandBluePastel,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "STEP $stepNumber",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = BrandBlue
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
