package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.User
import com.example.ui.MedBankViewModel
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankChip
import com.example.ui.components.MedBankIconBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferEarnScreen(
    viewModel: MedBankViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser
    val referrals by viewModel.observeReferralsForCurrentUser().collectAsState(initial = emptyList())
    val payoutTransactions by viewModel.observeReferralTransactionsForUser(currentUser?.uid ?: "").collectAsState(initial = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val referralCode = user?.referralCode?.ifBlank { "MB2026" } ?: "MB2026"
    val walletBalance = user?.walletBalance ?: 0.0
    val totalEarned = user?.totalEarned ?: 0.0

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Refer & Earn",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("refer_earn_back_button")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP MEDBANKCARD: Invite Friends, Earn 20% Royalty
            item {
                MedBankCard(
                    padding = 20.dp,
                    modifier = Modifier.testTag("refer_earn_invite_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MedBankIconBadge(
                            icon = Icons.Default.CardGiftcard,
                            badgeColor = BrandBlue,
                            backgroundColor = BrandBluePastel,
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Invite Friends, Earn 20% Royalty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            MedBankChip(
                                text = "20% INSTANT CASHBACK",
                                color = BrandBlue,
                                bgColor = BrandBluePastel
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Share your code. When your friend buys a subscription, you earn 20% of their payment in your wallet!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Large Referral Code Display Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, BrandBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        color = BrandBluePastel.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "YOUR REFERRAL CODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = referralCode,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandBlue,
                                letterSpacing = 3.sp,
                                modifier = Modifier.testTag("referral_code_text")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Two Buttons side-by-side: [ Copy Code ] and [ Share ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Copy Button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("MedBank Referral Code", referralCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                            },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_copy_referral_code")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = BrandBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copy Code",
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }

                        // Share Button
                        Button(
                            onClick = {
                                val shareMessage = "Join Medical Bank using my referral code *$referralCode* to get high-yield clinical MCQs and mock exams for MBBS & Licensing pathways!\n\nDownload app and enter my code during registration."
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Referral Code via")
                                context.startActivity(shareIntent)
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_share_referral_code")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. MIDDLE MEDBANKCARD — "My Wallet"
            item {
                MedBankCard(
                    padding = 20.dp,
                    modifier = Modifier.testTag("refer_earn_wallet_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MedBankIconBadge(
                                icon = Icons.Default.AccountBalanceWallet,
                                badgeColor = BrandSuccess,
                                backgroundColor = BrandSuccessPastel,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "My Wallet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        MedBankChip(
                            text = "AVAILABLE",
                            color = BrandSuccess,
                            bgColor = BrandSuccessPastel
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val formattedBalance = if (walletBalance % 1.0 == 0.0) {
                        walletBalance.toInt().toString()
                    } else {
                        String.format("%.2f", walletBalance)
                    }

                    val formattedEarned = if (totalEarned % 1.0 == 0.0) {
                        totalEarned.toInt().toString()
                    } else {
                        String.format("%.2f", totalEarned)
                    }

                    // Wallet Balance: large green text
                    Text(
                        text = "PKR $formattedBalance",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF16A34A), // Rich green
                        modifier = Modifier.testTag("wallet_balance_text")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Total Earned: smaller grey text
                    Text(
                        text = "Total Earned: PKR $formattedEarned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.testTag("total_earned_text")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Admin automatically transfers your earned commission to your bank account / Easypaisa upon payout approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        lineHeight = 16.sp
                    )
                }
            }

            // 3. BOTTOM SECTION — "My Referrals"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "My Referrals (${referrals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (referrals.isNotEmpty()) {
                        MedBankChip(
                            text = "${referrals.size} Candidates",
                            color = BrandBlue,
                            bgColor = BrandBluePastel
                        )
                    }
                }
            }

            if (referrals.isEmpty()) {
                item {
                    MedBankCard(
                        padding = 24.dp,
                        modifier = Modifier.testTag("empty_referrals_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MedBankIconBadge(
                                icon = Icons.Default.GroupAdd,
                                badgeColor = TextSecondary,
                                backgroundColor = AppBackground,
                                size = 52.dp,
                                iconSize = 26.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "You haven't referred anyone yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Share your code above with peers to start earning 20% royalties.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
             } else {
                items(referrals, key = { it.uid }) { referredUser ->
                    ReferredUserCard(referredUser = referredUser, viewModel = viewModel, dateFormat = dateFormat)
                }
            }

            // 4. BOTTOM SECTION — "Payout History"
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Payout History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (payoutTransactions.isEmpty()) {
                item {
                    MedBankCard(
                        padding = 16.dp,
                        modifier = Modifier.testTag("empty_payout_history")
                    ) {
                        Text(
                            text = "No payout history yet. Commissions from referred subscriptions will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(payoutTransactions, key = { it.id }) { tx ->
                    val isPaid = tx.status == "paid"
                    MedBankCard(
                        padding = 14.dp,
                        modifier = Modifier.testTag("payout_tx_${tx.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Referred: ${tx.referredName.ifBlank { "Student" }} (${tx.planName})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateFormat.format(Date(tx.createdAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "PKR ${tx.commissionAmount.toInt()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandSuccess
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                MedBankChip(
                                    text = if (isPaid) "PAID" else "PENDING",
                                    color = if (isPaid) BrandSuccess else AmberWarning,
                                    bgColor = if (isPaid) BrandSuccessPastel else AmberWarningPastel
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ReferredUserCard(referredUser: User, viewModel: MedBankViewModel, dateFormat: SimpleDateFormat) {
    val approvedRequests by viewModel.observeApprovedAccessRequestsForUser(referredUser.uid).collectAsState(initial = emptyList())
    val hasPaid = approvedRequests.isNotEmpty()
    val planName = approvedRequests.firstOrNull()?.planName ?: ""

    MedBankCard(
        padding = 14.dp,
        modifier = Modifier.testTag("referred_user_${referredUser.uid}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BrandBluePastel),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = referredUser.fullName.firstOrNull()?.toString()?.uppercase() ?: "U",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlue
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = referredUser.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (hasPaid) "Purchased: $planName" else "Joined ${dateFormat.format(Date(referredUser.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasPaid) BrandSuccess else TextSecondary,
                    fontWeight = if (hasPaid) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            if (hasPaid) {
                MedBankChip(
                    text = "PAID",
                    color = BrandSuccess,
                    bgColor = BrandSuccessPastel
                )
            } else {
                MedBankChip(
                    text = "REGISTERED (UNPAID)",
                    color = TextSecondary,
                    bgColor = AppBackground
                )
            }
        }
    }
}

