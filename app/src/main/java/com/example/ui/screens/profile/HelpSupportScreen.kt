package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
fun HelpSupportScreen(
    viewModel: MedBankViewModel,
    onBack: () -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & Support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("help_support_back_button")
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MedBankIconBadge(
                        icon = Icons.Default.SupportAgent,
                        badgeColor = BrandSuccess,
                        backgroundColor = BrandSuccessPastel,
                        size = 54.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Candidate Support Desk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Instant WhatsApp assistance for payments, activation, and technical queries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                MedBankCard(
                    padding = 12.dp,
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = appSettings.adminWhatsapp.ifBlank { "+92 300 1234567" },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = BrandBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val adminPhone = appSettings.adminWhatsapp.replace("+", "").replace(" ", "").trim()
                        val waUrl = "https://wa.me/$adminPhone?text=Hello%20MedBank%20Support,%20I%20need%20assistance%20with%20my%20account."
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_open_whatsapp"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess)
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open WhatsApp Chat", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            MedBankCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedBankIconBadge(
                        icon = Icons.Default.Schedule,
                        badgeColor = BrandBlue,
                        backgroundColor = BrandBluePastel,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Admin Operating Hours",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Mon - Sat: 9:00 AM - 10:00 PM PKT",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Payment proof reviews typically cleared within 1-3 hours.",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            }

            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            FaqCard(
                question = "How do I unlock question banks?",
                answer = "Navigate to the Banks tab, tap 'Unlock Bank' on any locked subject, select your preferred membership plan, and upload your payment transaction screenshot or receipt. Our admin team will verify and activate your pass."
            )

            FaqCard(
                question = "Can I access MedBank on multiple devices?",
                answer = "Medical Bank strictly enforces a 1-Device Policy via hardware device binding. If you replace or upgrade your phone, please reach out to admin support on WhatsApp to reset your bound hardware ID."
            )

            FaqCard(
                question = "What if my payment is taking long to approve?",
                answer = "Normal approval turnaround is 1 to 3 hours. If you need urgent access for an upcoming exam, tap 'Open WhatsApp Chat' above and send your registered CNIC."
            )

            FaqCard(
                question = "How do mock exams and negative marking work?",
                answer = "Mock exams feature timed countdowns with real Pakistani and International medical council exam formats. Incorrect answers apply negative score penalties as configured per exam."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FaqCard(
    question: String,
    answer: String
) {
    MedBankCard(padding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            MedBankIconBadge(
                icon = Icons.Default.HelpOutline,
                badgeColor = BrandBlue,
                backgroundColor = BrandBluePastel,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = answer,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
