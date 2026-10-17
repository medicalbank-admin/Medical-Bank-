package com.example.ui.screens.exam

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Flag
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
fun ExamPlayerScreen(viewModel: MedBankViewModel) {
    // ENFORCE SCREEN SECURITY (FLAG_SECURE - Screenshot and screen recording blocking)
    SetScreenSecure(isSecure = true)

    val playerState by viewModel.playerState.collectAsState()
    val exam = playerState.exam ?: return

    var showSubmitConfirmation by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }

    // Intercept back button during active exam
    BackHandler {
        showSubmitConfirmation = true
    }

    val currentQIndex = playerState.currentIndex
    val totalQ = playerState.questions.size
    val currentQ = playerState.questions.getOrNull(currentQIndex)
    val selectedOptionIndex = currentQ?.let { playerState.answers[it.id] }
    val isMarkedForReview = currentQ?.let { playerState.markedForReview.contains(it.id) } ?: false

    // Format remaining seconds
    val minutes = playerState.remainingSeconds / 60
    val seconds = playerState.remainingSeconds % 60
    val timerText = "%02d:%02d".format(minutes, seconds)
    val isTimeLow = playerState.remainingSeconds < 300 // under 5 minutes

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "Question ${currentQIndex + 1} of $totalQ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    // Timer Display (Warning uses colorScheme.error)
                    Surface(
                        color = if (isTimeLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isTimeLow) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timerText,
                                color = if (isTimeLow) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Question Palette Trigger
                    IconButton(
                        onClick = { showPaletteDialog = true },
                        modifier = Modifier.testTag("exam_palette_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Palette",
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
            // Action Navigation Bar (Previous, Mark for Review, Save & Next, Submit)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    OutlinedButton(
                        onClick = { viewModel.previousQuestion() },
                        enabled = currentQIndex > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("exam_prev_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }

                    // Mark for Review Toggle Button
                    OutlinedIconButton(
                        onClick = {
                            if (currentQ != null) {
                                viewModel.toggleMarkForReview(currentQ.id)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = if (isMarkedForReview) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        modifier = Modifier.testTag("exam_mark_review_button")
                    ) {
                        Icon(
                            imageVector = if (isMarkedForReview) Icons.Default.Flag else Icons.Outlined.Flag,
                            contentDescription = "Mark for Review",
                            tint = if (isMarkedForReview) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Save & Next or Finish Button
                    if (currentQIndex < totalQ - 1) {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("exam_next_button")
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { showSubmitConfirmation = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("exam_finish_submit_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Submit")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            // Meta Row (Difficulty, Tags, Flag state)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = currentQ?.difficulty?.uppercase() ?: "MEDIUM",
                        color = when (currentQ?.difficulty?.lowercase()) {
                            "easy" -> MaterialTheme.colorScheme.primary
                            "hard" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                    currentQ?.tags?.firstOrNull()?.let { tag ->
                        StatusBadge(
                            text = tag,
                            color = MaterialTheme.colorScheme.onSurface,
                            bgColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                if (isMarkedForReview) {
                    StatusBadge(
                        text = "FLAGGED FOR REVIEW",
                        color = MaterialTheme.colorScheme.secondary,
                        icon = Icons.Default.Flag
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Question Stem
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = currentQ?.text ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Multiple Choice Options (A, B, C, D)
            Text(
                text = "Select one answer:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            val optionLabels = listOf("A", "B", "C", "D")
            currentQ?.options?.forEachIndexed { index, optionText ->
                val isSelected = selectedOptionIndex == index

                ElevatedCard(
                    onClick = {
                        if (currentQ != null) {
                            viewModel.selectAnswer(currentQ.id, index)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("option_${optionLabels.getOrNull(index)}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optionLabels.getOrElse(index) { "$index" },
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Question Palette Dialog
    if (showPaletteDialog) {
        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Question Palette")
                }
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Answered", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flagged", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Skipped", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        itemsIndexed(playerState.questions) { idx, q ->
                            val isAnswered = playerState.answers.containsKey(q.id)
                            val isFlagged = playerState.markedForReview.contains(q.id)
                            val isCurrent = idx == currentQIndex

                            val boxColor = when {
                                isFlagged -> MaterialTheme.colorScheme.secondary
                                isAnswered -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(boxColor)
                                    .then(if (isCurrent) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)) else Modifier)
                                    .clickable {
                                        viewModel.jumpToQuestion(idx)
                                        showPaletteDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAnswered || isFlagged) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaletteDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Submit Confirmation Dialog
    if (showSubmitConfirmation) {
        val answeredCount = playerState.answers.size
        val flaggedCount = playerState.markedForReview.size
        val skippedCount = totalQ - answeredCount

        AlertDialog(
            onDismissRequest = { showSubmitConfirmation = false },
            icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Submit Examination?") },
            text = {
                Column {
                    Text(
                        "Are you sure you want to finish and submit your answers?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Answered Questions:", fontWeight = FontWeight.SemiBold)
                                Text("$answeredCount", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Skipped Questions:", fontWeight = FontWeight.SemiBold)
                                Text("$skippedCount", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Flagged for Review:", fontWeight = FontWeight.SemiBold)
                                Text("$flaggedCount", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmation = false
                        viewModel.submitActiveExam()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Confirm & Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmation = false }) {
                    Text("Keep Answering")
                }
            }
        )
    }
}
