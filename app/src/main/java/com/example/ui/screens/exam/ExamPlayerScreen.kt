package com.example.ui.screens.exam

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPlayerScreen(viewModel: MedBankViewModel) {
    // Enforce Screen Security (FLAG_SECURE)
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
        containerColor = AppBackground,
        topBar = {
            // Top bar: Back button, title, and timer pill (Blue background, white text)
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "Question ${currentQIndex + 1} of $totalQ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showSubmitConfirmation = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Exam",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Timer pill (Blue background, white text)
                    Surface(
                        color = if (isTimeLow) BrandRed else BrandBlue,
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timerText,
                                color = Color.White,
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
                            tint = BrandBlue
                        )
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
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Bottom: Question palette with 40dp circle chips
                    Text(
                        text = "Question Palette",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playerState.questions) { idx, q ->
                            val isAnswered = playerState.answers.containsKey(q.id)
                            val isFlagged = playerState.markedForReview.contains(q.id)
                            val isCurrent = idx == currentQIndex

                            val chipBgColor = when {
                                isCurrent -> BrandBlue
                                isFlagged -> BrandRed
                                isAnswered -> BrandSuccess
                                else -> BrandBluePastel.copy(alpha = 0.5f)
                            }
                            val chipTextColor = when {
                                isCurrent || isFlagged || isAnswered -> Color.White
                                else -> TextPrimary
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp) // 40dp circle chip
                                    .clip(CircleShape)
                                    .background(chipBgColor)
                                    .then(
                                        if (isCurrent) Modifier.border(2.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { viewModel.jumpToQuestion(idx) }
                                    .testTag("palette_chip_${idx + 1}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipTextColor
                                )
                            }
                        }
                    }

                    // Navigation Actions (Previous, Flag, Next / Submit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        OutlinedButton(
                            onClick = { viewModel.previousQuestion() },
                            enabled = currentQIndex > 0,
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (currentQIndex > 0) BrandBlue else BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue),
                            modifier = Modifier.testTag("exam_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontWeight = FontWeight.Bold)
                        }

                        // Mark for Review Toggle Button
                        OutlinedIconButton(
                            onClick = {
                                if (currentQ != null) {
                                    viewModel.toggleMarkForReview(currentQ.id)
                                }
                            },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isMarkedForReview) BrandRed else BorderLight),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                containerColor = if (isMarkedForReview) BrandRedPastel else Color.Transparent
                            ),
                            modifier = Modifier.testTag("exam_mark_review_button")
                        ) {
                            Icon(
                                imageVector = if (isMarkedForReview) Icons.Default.Flag else Icons.Outlined.Flag,
                                contentDescription = "Mark for Review",
                                tint = if (isMarkedForReview) BrandRed else TextSecondary
                            )
                        }

                        // Next / Submit Button
                        if (currentQIndex < totalQ - 1) {
                            Button(
                                onClick = { viewModel.nextQuestion() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandBlue,
                                    contentColor = Color.White
                                ),
                                shape = CircleShape,
                                modifier = Modifier.testTag("exam_next_button")
                            ) {
                                Text("Next", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Button(
                                onClick = { showSubmitConfirmation = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandRed,
                                    contentColor = Color.White
                                ),
                                shape = CircleShape,
                                modifier = Modifier.testTag("exam_finish_submit_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Submit", fontWeight = FontWeight.Bold)
                            }
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
                .padding(16.dp)
        ) {
            // Flag indicator badge
            if (isMarkedForReview) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MedBankChip(
                        text = "FLAGGED FOR REVIEW",
                        color = BrandRed,
                        bgColor = BrandRedPastel,
                        icon = Icons.Default.Flag
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Question Card: White card
            MedBankCard(
                modifier = Modifier.testTag("exam_question_card")
            ) {
                Text(
                    text = "QUESTION ${currentQIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentQ?.text ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Select one answer:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Multiple Choice Options (A, B, C, D)
            // Options are rounded rectangles (12dp). Selected: Blue border, pastel blue background.
            val optionLabels = listOf("A", "B", "C", "D")
            currentQ?.options?.forEachIndexed { index, optionText ->
                val isSelected = selectedOptionIndex == index

                Surface(
                    onClick = {
                        if (currentQ != null) {
                            viewModel.selectAnswer(currentQ.id, index)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("option_${optionLabels.getOrNull(index)}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) BrandBluePastel else CardSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) BrandBlue else BorderLight
                    ),
                    shadowElevation = if (isSelected) 2.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) BrandBlue else BrandBluePastel.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optionLabels.getOrElse(index) { "$index" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else BrandBlue
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Question Palette Grid Dialog
    if (showPaletteDialog) {
        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Question Palette", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BrandSuccess))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Answered", fontSize = 11.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BrandRed))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flagged", fontSize = 11.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BrandBluePastel))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unanswered", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        itemsIndexed(playerState.questions) { idx, q ->
                            val isAnswered = playerState.answers.containsKey(q.id)
                            val isFlagged = playerState.markedForReview.contains(q.id)
                            val isCurrent = idx == currentQIndex

                            val boxColor = when {
                                isCurrent -> BrandBlue
                                isFlagged -> BrandRed
                                isAnswered -> BrandSuccess
                                else -> BrandBluePastel.copy(alpha = 0.5f)
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(boxColor)
                                    .then(if (isCurrent) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                    .clickable {
                                        viewModel.jumpToQuestion(idx)
                                        showPaletteDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isAnswered || isFlagged || isCurrent) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPaletteDialog = false },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
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
            icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = BrandBlue) },
            title = { Text("Submit Examination?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Are you sure you want to finish and submit your answers?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    MedBankCard(
                        padding = 12.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Answered Questions:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$answeredCount", color = BrandSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Skipped Questions:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$skippedCount", color = BrandRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Flagged for Review:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$flaggedCount", color = AmberWarning, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Confirm & Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmation = false }) {
                    Text("Keep Answering", color = TextSecondary)
                }
            }
        )
    }

    // Immediate UI feedback on submit
    if (playerState.isSubmitting) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardSurface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = BrandBlue
                    )
                    Text(
                        "Submitting answers & recording attempt...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
