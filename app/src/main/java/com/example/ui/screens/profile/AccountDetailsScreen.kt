package com.example.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankChip
import com.example.ui.components.MedBankIconBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    viewModel: MedBankViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("account_details_back_button")
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
        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No user information available.", color = TextSecondary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Identity Header Card
            MedBankCard(padding = 20.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(BrandBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2)
                                .joinToString("")
                                .uppercase()
                                .ifBlank { "MB" },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "CNIC: ${user.cnic}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MedBankChip(
                            text = if (user.role == "admin") "ADMINISTRATOR" else "ACTIVE CANDIDATE",
                            color = if (user.role == "admin") BrandBlue else BrandSuccess,
                            bgColor = if (user.role == "admin") BrandBluePastel else BrandSuccessPastel,
                            icon = if (user.role == "admin") Icons.Default.Shield else Icons.Default.School
                        )
                    }
                }
            }

            // Personal Information Card
            MedBankCard(padding = 16.dp) {
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                AccountInfoRow(
                    icon = Icons.Default.Person,
                    label = "Full Name",
                    value = user.fullName
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppBackground)

                AccountInfoRow(
                    icon = Icons.Default.Badge,
                    label = "CNIC / Identity No.",
                    value = user.cnic
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppBackground)

                AccountInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email Address",
                    value = user.email.ifBlank { "Not provided" }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppBackground)

                AccountInfoRow(
                    icon = Icons.Default.Phone,
                    label = "Phone Number",
                    value = user.phone.ifBlank { "Not provided" }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppBackground)

                AccountInfoRow(
                    icon = Icons.Default.Public,
                    label = "Country",
                    value = user.country.ifBlank { "Pakistan" }
                )
            }

            // Academic Track & Curriculum Card
            MedBankCard(padding = 16.dp) {
                Text(
                    text = "Academic Program & Track",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                val programName = if (user.programType == "mbbs") "MBBS Curriculum" else "Medical Licensing Exams"
                val trackDetail = if (user.programType == "mbbs") {
                    "Year ${user.mbbsYear ?: 1} Professional"
                } else {
                    user.licensingPath ?: "USMLE / PLAB"
                }

                AccountInfoRow(
                    icon = Icons.Default.School,
                    label = "Program",
                    value = programName
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppBackground)

                AccountInfoRow(
                    icon = Icons.Default.MenuBook,
                    label = "Track / Level",
                    value = trackDetail
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppBackground)

                AccountInfoRow(
                    icon = Icons.Default.CardMembership,
                    label = "Active Entitlements",
                    value = "${user.entitlements.size} Subjects Unlocked"
                )
            }

            // Security & Hardware Binding Card
            MedBankCard(padding = 16.dp) {
                Text(
                    text = "Security & Hardware Token",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                AccountInfoRow(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Registered Device",
                    value = viewModel.deviceModel
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppBackground)

                AccountInfoRow(
                    icon = Icons.Default.VpnKey,
                    label = "Hardware Binding",
                    value = "Single-Device Active (1/1)"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppBackground)

                AccountInfoRow(
                    icon = Icons.Default.Lock,
                    label = "Integrity Protection",
                    value = "FLAG_SECURE Enabled"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccountInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MedBankIconBadge(
            icon = icon,
            badgeColor = BrandBlue,
            backgroundColor = BrandBluePastel,
            size = 36.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
