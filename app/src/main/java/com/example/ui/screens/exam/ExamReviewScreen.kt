package com.example.ui.screens.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.components.SetScreenSecure
import com.example.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamReviewScreen(viewModel: MedBankViewModel) {
    // Requirements 8 & 11: FLAG_SECURE protection
    SetScreenSecure(isSecure = true)

    val reviewData by viewModel.reviewData.collectAsState()

    // Back handler to dismiss review mode
    BackHandler {
        viewModel.exitExam()
    }

    if (reviewData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Review not available.")
        }
        return
    }

    val rev = reviewData!!
    
    // Change 2: Fetch explanations fresh
    var explanations by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    val examId = rev.exam.id

    LaunchedEffect(examId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("examPrivate").document(examId)
            .collection("answers")
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { snapshot ->
                val fetchedExplanations = snapshot.documents.associate { doc ->
                    doc.id to doc.getString("explanation")
                }
                explanations = fetchedExplanations
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MEDBANK_REVIEW", "Failed to fetch explanations: ${e.message}")
            }
    }

    val studentAnswersMap = remember(rev.result.answers) {
        rev.result.answers.associateBy { it.qid }
    }

    var filterType by remember { mutableStateOf("all") } // "all", "wrong", "correct", "skipped"

    val filteredQuestions = remember(filterType, rev.questions) {
        rev.questions.filter { q ->
            val sa = studentAnswersMap[q.id]
            val ca = rev.answers[q.id]?.correctIndex ?: 0
            when (filterType) {
                "wrong" -> sa != null && sa.selectedIndex != -1 && sa.selectedIndex != ca
                "correct" -> sa != null && sa.selectedIndex == ca
                "skipped" -> sa == null || sa.selectedIndex == -1
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Exam Review & Explanations",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = rev.exam.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.exitExam() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Review",
                            tint = MaterialTheme.colorScheme.onPrimary
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
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { viewModel.exitExam() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("review_back_to_banks_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back to Exam Banks", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "all",
                    onClick = { filterType = "all" },
                    label = { Text("All (${rev.questions.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = filterType == "wrong",
                    onClick = { filterType = "wrong" },
                    label = { Text("Incorrect (${rev.result.wrongCount})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.error
                    )
                )
                FilterChip(
                    selected = filterType == "correct",
                    onClick = { filterType = "correct" },
                    label = { Text("Correct (${rev.result.correctCount})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = filterType == "skipped",
                    onClick = { filterType = "skipped" },
                    label = { Text("Skipped (${rev.result.skippedCount})", fontSize = 12.sp) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(filteredQuestions) { _, q ->
                    val sa = studentAnswersMap[q.id]
                    val privateAns = rev.answers[q.id]
                    val correctIdx = privateAns?.correctIndex ?: 0
                    val studentPick = sa?.selectedIndex ?: -1
                    val isCorrect = studentPick == correctIdx
                    val isSkipped = studentPick == -1

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_card_${q.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Header: Status Badge + Q number
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Question ${q.order}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                StatusBadge(
                                    text = when {
                                        isSkipped -> "SKIPPED"
                                        isCorrect -> "CORRECT"
                                        else -> "INCORRECT"
                                    },
                                    color = when {
                                        isSkipped -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        isCorrect -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                    icon = when {
                                        isSkipped -> Icons.Default.Remove
                                        isCorrect -> Icons.Default.CheckCircle
                                        else -> Icons.Default.Cancel
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Question Stem
                            Text(
                                text = q.text,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Options Review Breakdown
                            val optionLabels = listOf("A", "B", "C", "D")
                            q.options.forEachIndexed { optIdx, optText ->
                                val isOptionCorrect = optIdx == correctIdx
                                val isOptionChosenByStudent = optIdx == studentPick

                                val containerColor = when {
                                    isOptionCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    isOptionChosenByStudent && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }

                                val borderColor = when {
                                    isOptionCorrect -> MaterialTheme.colorScheme.primary
                                    isOptionChosenByStudent && !isCorrect -> MaterialTheme.colorScheme.error
                                    else -> Color.Transparent
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(containerColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isOptionCorrect -> MaterialTheme.colorScheme.primary
                                                    isOptionChosenByStudent -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = optionLabels.getOrElse(optIdx) { "$optIdx" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOptionCorrect || isOptionChosenByStudent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = optText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isOptionCorrect) {
                                        Text(
                                            text = "Correct",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    } else if (isOptionChosenByStudent) {
                                        Text(
                                            text = "Your Pick",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // High-Yield Clinical Explanation Box
                            if (!privateAns?.explanation.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Clinical Explanation",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = privateAns.explanation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
