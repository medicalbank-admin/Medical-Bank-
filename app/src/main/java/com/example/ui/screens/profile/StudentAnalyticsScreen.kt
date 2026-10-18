package com.example.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankChip
import com.example.ui.components.MedBankIconBadge
import com.example.ui.components.MedBankProgressRing
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAnalyticsScreen(
    viewModel: MedBankViewModel,
    onBack: () -> Unit
) {
    val results by viewModel.userResults.collectAsState()

    // Calculate analytics
    val totalExams = results.size
    val averagePercentage = if (totalExams > 0) {
        results.map { res ->
            if (res.totalMarks > 0) ((res.score / res.totalMarks) * 100).coerceIn(0.0, 100.0) else 0.0
        }.average()
    } else {
        0.0
    }

    // Group by examTitle
    val subjectGroups = results.groupBy { it.examTitle.ifBlank { "General Practice Exam" } }
        .map { (title, resList) ->
            val avg = resList.map { r ->
                if (r.totalMarks > 0) ((r.score / r.totalMarks) * 100).coerceIn(0.0, 100.0) else 0.0
            }.average()
            title to Pair(resList.size, avg)
        }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("analytics_back_button")
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
        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MedBankIconBadge(
                        icon = Icons.Default.BarChart,
                        badgeColor = BrandBlue,
                        backgroundColor = BrandBluePastel,
                        size = 64.dp
                    )
                    Text(
                        text = "No exams taken yet. Start practicing!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Complete your first mock exam or question bank session to track your clinical performance metrics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.setActiveTab(ScreenTab.EXPLORER)
                            viewModel.openProfileScreen(null)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("Browse Question Banks")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Section: Overall Score Card with MedBankProgressRing
                item {
                    MedBankCard(
                        padding = 20.dp,
                        modifier = Modifier.testTag("analytics_overview_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                MedBankProgressRing(
                                    percentage = (averagePercentage / 100.0).toFloat().coerceIn(0f, 1f),
                                    size = 90.dp,
                                    strokeWidth = 10.dp,
                                    label = "Average"
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Overall Performance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total Exams Taken: $totalExams",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                MedBankChip(
                                    text = if (averagePercentage >= 70.0) "EXCELLENT" else if (averagePercentage >= 50.0) "GOOD STANDING" else "NEEDS PRACTICE",
                                    color = if (averagePercentage >= 70.0) BrandSuccess else if (averagePercentage >= 50.0) AmberWarning else BrandRed,
                                    bgColor = if (averagePercentage >= 70.0) BrandSuccessPastel else if (averagePercentage >= 50.0) AmberWarningPastel else BrandRedPastel
                                )
                            }
                        }
                    }
                }

                // Middle Section: Subject Breakdown Header
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subject & Exam Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // List of Subject Breakdown Cards
                items(subjectGroups) { (examTitle, data) ->
                    val (count, avgScore) = data
                    MedBankCard(
                        padding = 16.dp,
                        modifier = Modifier.testTag("subject_breakdown_item_${examTitle.hashCode()}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = examTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$count attempts recorded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${avgScore.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { (avgScore / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = BrandBlue,
                            trackColor = BrandBluePastel,
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
