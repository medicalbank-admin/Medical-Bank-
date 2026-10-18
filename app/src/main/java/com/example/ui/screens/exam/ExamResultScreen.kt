package com.example.ui.screens.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(viewModel: MedBankViewModel) {
    SetScreenSecure(isSecure = true)

    val result by viewModel.latestResult.collectAsState()

    BackHandler {
        viewModel.exitExam()
    }

    if (result == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("No result found.", color = TextSecondary)
        }
        return
    }

    val res = result!!
    val percentageFloat = if (res.totalMarks > 0) (res.score / res.totalMarks).toFloat().coerceIn(0f, 1f) else 0f
    val percentageInt = (percentageFloat * 100).toInt()
    val isPassed = percentageInt >= 50

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Exam Results",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.exitExam() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = CardSurface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // [ Review Answers ] (Outlined Pill Button)
                    MedBankPillButton(
                        text = "Review Answers & Explanations",
                        onClick = { viewModel.loadReview(res.id) },
                        isPrimary = false,
                        icon = Icons.Default.FindInPage,
                        modifier = Modifier.testTag("review_exam_button")
                    )

                    // [ Back to Banks ] (Filled Blue Pill Button)
                    MedBankPillButton(
                        text = "Back to Exam Banks",
                        onClick = { viewModel.exitExam() },
                        isPrimary = true,
                        icon = Icons.Default.Home,
                        modifier = Modifier.testTag("back_to_banks_button")
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Main Performance Card
            MedBankCard(
                padding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = res.examTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Attempt #${res.attemptNumber} • ${res.score.toInt()} of ${res.totalMarks.toInt()} Marks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Large MedBankProgressRing showing the score
                    MedBankProgressRing(
                        percentage = percentageFloat,
                        size = 150.dp,
                        strokeWidth = 14.dp,
                        progressColor = if (isPassed) BrandSuccess else BrandRed,
                        trackColor = Color(0xFFE5E7EB),
                        label = if (isPassed) "PASSED" else "NEEDS REVISION"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    MedBankChip(
                        text = if (isPassed) "PASSING GRADE ACHIEVED" else "ADDITIONAL REVISION RECOMMENDED",
                        color = if (isPassed) BrandSuccess else BrandRed,
                        bgColor = if (isPassed) BrandSuccessPastel else BrandRedPastel,
                        icon = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Warning
                    )
                }
            }

            // Below: 3 MedBankStatPills (Correct, Wrong, Skipped)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MedBankStatPill(
                    title = "Correct",
                    value = "${res.correctCount}",
                    color = BrandSuccess,
                    bgColor = BrandSuccessPastel,
                    modifier = Modifier.weight(1f)
                )

                MedBankStatPill(
                    title = "Wrong",
                    value = "${res.wrongCount}",
                    color = BrandRed,
                    bgColor = BrandRedPastel,
                    modifier = Modifier.weight(1f)
                )

                MedBankStatPill(
                    title = "Skipped",
                    value = "${res.skippedCount}",
                    color = TextSecondary,
                    bgColor = Color(0xFFE5E7EB).copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
            }

            // Summary tips card
            MedBankCard(
                padding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedBankIconBadge(
                        icon = Icons.Default.Lightbulb,
                        badgeColor = AmberWarning,
                        backgroundColor = AmberWarningPastel,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "High-Yield Clinical Review",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap 'Review Answers' below to study detailed clinical explanations for all questions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
