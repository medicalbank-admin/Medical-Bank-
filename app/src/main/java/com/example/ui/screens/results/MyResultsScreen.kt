package com.example.ui.screens.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankChip
import com.example.ui.components.MedBankPillButton
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyResultsScreen(viewModel: MedBankViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val uid = currentUser?.uid ?: ""
    val results by viewModel.repository.observeResultsByUser(uid).collectAsState(initial = emptyList())

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Exam Attempts",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
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
                Box(modifier = Modifier.padding(16.dp)) {
                    MedBankPillButton(
                        text = "Back to Exam Banks",
                        onClick = {
                            viewModel.setActiveTab(ScreenTab.EXPLORER)
                            viewModel.resetExplorerNavigation()
                        },
                        icon = Icons.Default.Home,
                        modifier = Modifier.testTag("results_back_to_banks_button")
                    )
                }
            }
        }
    ) { padding ->
        if (results.isEmpty()) {
            EmptyPlaceholder(
                icon = Icons.Default.HistoryEdu,
                title = "No Past Attempts",
                subtitle = "Take an exam from the question bank to track your performance history here.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(results) { res ->
                    val percentage = if (res.totalMarks > 0) ((res.score / res.totalMarks) * 100).toInt() else 0

                    // MedBankChip for the score (Green >70%, Amber 50-70%, Red <50%)
                    val (chipColor, chipBg) = when {
                        percentage >= 70 -> Pair(BrandSuccess, BrandSuccessPastel)
                        percentage >= 50 -> Pair(AmberWarning, AmberWarningPastel)
                        else -> Pair(BrandRed, BrandRedPastel)
                    }

                    // MedBankCard showing Exam Title, Attempt #, Date, and score chip
                    MedBankCard(
                        modifier = Modifier.testTag("result_card_${res.id}"),
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = res.examTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Attempt #${res.attemptNumber} • ${dateFormat.format(Date(res.submittedAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            MedBankChip(
                                text = "$percentage%",
                                color = chipColor,
                                bgColor = chipBg,
                                icon = if (percentage >= 50) Icons.Default.CheckCircle else Icons.Default.Warning
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "${res.score.toInt()}/${res.totalMarks.toInt()} Marks",
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "• ${res.correctCount} Correct",
                                    color = BrandSuccess,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "• ${res.wrongCount} Wrong",
                                    color = BrandRed,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }

                            TextButton(
                                onClick = { viewModel.loadReview(res.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = BrandBlue),
                                modifier = Modifier.testTag("review_button_${res.id}")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Review", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
