package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.models.ReferralTransaction
import com.example.ui.MedBankViewModel
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankChip
import com.example.ui.components.MedBankIconBadge
import com.example.ui.components.MedBankPillButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPayoutsScreen(viewModel: MedBankViewModel) {
    val pendingTransactions by viewModel.observePendingReferralTransactions().collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    var selectedTransactionToPay by remember { mutableStateOf<ReferralTransaction?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Stats Banner
            MedBankCard(
                padding = 16.dp,
                modifier = Modifier.testTag("admin_payouts_header_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Referral Commission Payouts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manual bank / Easypaisa transfers to student referrers",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    MedBankChip(
                        text = "${pendingTransactions.size} PENDING",
                        color = if (pendingTransactions.isNotEmpty()) BrandRed else BrandSuccess,
                        bgColor = if (pendingTransactions.isNotEmpty()) BrandRedPastel else BrandSuccessPastel
                    )
                }

                if (pendingTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val totalPendingPkr = pendingTransactions.sumOf { it.commissionAmount }
                    val totalFormatted = if (totalPendingPkr % 1.0 == 0.0) totalPendingPkr.toInt().toString() else String.format("%.2f", totalPendingPkr)
                    Surface(
                        color = AmberWarningPastel,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Total Pending Payout: PKR $totalFormatted across ${pendingTransactions.size} transactions",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (pendingTransactions.isEmpty()) {
                EmptyPlaceholder(
                    icon = Icons.Default.Paid,
                    title = "No Pending Payouts",
                    subtitle = "All referral commissions have been disbursed. When students refer peers who purchase exam subscriptions, payout requests will appear here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(pendingTransactions, key = { it.id }) { tx ->
                        MedBankCard(
                            padding = 16.dp,
                            modifier = Modifier.testTag("payout_card_${tx.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MedBankIconBadge(
                                        icon = Icons.Default.Person,
                                        badgeColor = BrandBlue,
                                        backgroundColor = BrandBluePastel,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tx.referrerName.ifBlank { "Referrer (${tx.referrerUid.take(6)})" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "UID: ${tx.referrerUid}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary
                                        )
                                    }
                                }

                                val commStr = if (tx.commissionAmount % 1.0 == 0.0) tx.commissionAmount.toInt().toString() else String.format("%.2f", tx.commissionAmount)
                                Text(
                                    text = "PKR $commStr",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandSuccess
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DividerColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Transaction details
                            Text(
                                text = "Referred User: ${tx.referredName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Subscription Plan: ${tx.planName} • Total Paid: PKR ${tx.amountPaid.toInt()} (20% Royalty)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Date: ${dateFormat.format(Date(tx.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Button: [ Mark as Paid ]
                            MedBankPillButton(
                                text = "Mark as Paid",
                                icon = Icons.Default.CheckCircle,
                                isPrimary = true,
                                containerColor = BrandSuccess,
                                contentColor = Color.White,
                                onClick = { selectedTransactionToPay = tx },
                                modifier = Modifier.testTag("btn_mark_paid_${tx.id}")
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Confirmation Dialog
    if (selectedTransactionToPay != null) {
        val tx = selectedTransactionToPay!!
        val commStr = if (tx.commissionAmount % 1.0 == 0.0) tx.commissionAmount.toInt().toString() else String.format("%.2f", tx.commissionAmount)

        AlertDialog(
            onDismissRequest = { if (!isProcessing) selectedTransactionToPay = null },
            title = {
                Text(
                    text = "Confirm Referral Payout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Have you transferred PKR $commStr to ${tx.referrerName}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Marking this transaction as paid will record the manual payout timestamp and clear the pending status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        viewModel.markPayoutPaid(tx.id) { success ->
                            isProcessing = false
                            selectedTransactionToPay = null
                            scope.launch {
                                if (success) {
                                    snackbarHostState.showSnackbar("Marked as paid! Transfer confirmed for ${tx.referrerName}.")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to update transaction.")
                                }
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm Paid")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedTransactionToPay = null },
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
