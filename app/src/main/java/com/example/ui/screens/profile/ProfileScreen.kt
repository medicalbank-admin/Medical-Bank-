package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankChip
import com.example.ui.components.MedBankListItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MedBankViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser

    if (user == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("No user signed in.", color = TextSecondary)
        }
        return
    }

    var showLogoutConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Card: Avatar, Name, CNIC, and Role chip
            MedBankCard(
                padding = 20.dp,
                modifier = Modifier.testTag("profile_user_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
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
                            fontSize = 22.sp
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
                            text = if (user.role == "admin") "ADMINISTRATOR" else "CANDIDATE",
                            color = if (user.role == "admin") BrandBlue else BrandSuccess,
                            bgColor = if (user.role == "admin") BrandBluePastel else BrandSuccessPastel,
                            icon = if (user.role == "admin") Icons.Default.Shield else Icons.Default.School
                        )
                    }
                }
            }

            // Menu List using MedBankListItem inside a MedBankCard
            MedBankCard(
                padding = 10.dp
            ) {
                // Account Management
                MedBankListItem(
                    title = "Account Management",
                    subtitle = "View full profile & personal information",
                    icon = Icons.Default.Person,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    onClick = { viewModel.openProfileScreen("account") },
                    modifier = Modifier.testTag("menu_account")
                )

                // Active Device
                MedBankListItem(
                    title = "Active Device",
                    subtitle = "${viewModel.deviceModel} (1/1 Bound)",
                    icon = Icons.Default.PhoneAndroid,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    trailingText = "Active",
                    onClick = { viewModel.openProfileScreen("device") },
                    modifier = Modifier.testTag("menu_device")
                )

                // Notification Preferences
                MedBankListItem(
                    title = "Notification Preferences",
                    subtitle = "FCM announcements & approval alerts",
                    icon = Icons.Default.Notifications,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    onClick = { viewModel.openProfileScreen("notifications") },
                    modifier = Modifier.testTag("menu_notifications")
                )

                // App Tutorial
                MedBankListItem(
                    title = "App Tutorial",
                    subtitle = "How to practice question banks & mock exams",
                    icon = Icons.Default.HelpCenter,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    onClick = { viewModel.openProfileScreen("tutorial") },
                    modifier = Modifier.testTag("menu_tutorial")
                )

                // My Performance
                MedBankListItem(
                    title = "My Performance",
                    subtitle = "Clinical analytics, scores & subject breakdown",
                    icon = Icons.Default.BarChart,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    onClick = { viewModel.openProfileScreen("analytics") },
                    modifier = Modifier.testTag("menu_analytics")
                )

                // My Subscriptions
                MedBankListItem(
                    title = "My Subscriptions",
                    subtitle = "Active passes, renewals & upgrade plans",
                    icon = Icons.Default.CardMembership,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    onClick = { viewModel.openProfileScreen("membership") },
                    modifier = Modifier.testTag("menu_membership")
                )

                // Refer & Earn
                MedBankListItem(
                    title = "Refer & Earn",
                    subtitle = "Earn 20% royalty for referring colleagues",
                    icon = Icons.Default.CardGiftcard,
                    iconColor = BrandSuccess,
                    iconBgColor = BrandSuccessPastel,
                    trailingText = "20% Royalty",
                    onClick = { viewModel.openProfileScreen("refer_earn") },
                    modifier = Modifier.testTag("menu_refer_earn")
                )

                // Help Center
                MedBankListItem(
                    title = "Help Center",
                    subtitle = "Admin support, verification, & queries",
                    icon = Icons.Default.SupportAgent,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    onClick = { viewModel.openProfileScreen("help") },
                    modifier = Modifier.testTag("menu_help")
                )


                // Privacy Policy
                MedBankListItem(
                    title = "Privacy Policy",
                    subtitle = "Institution data safety and privacy rules",
                    icon = Icons.Default.Security,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    onClick = { viewModel.openLegalViewer("privacy") },
                    modifier = Modifier.testTag("menu_privacy_policy")
                )

                // Terms & Conditions
                MedBankListItem(
                    title = "Terms & Conditions",
                    subtitle = "Single-device security & subscription terms",
                    icon = Icons.Default.Description,
                    iconColor = BrandBlue,
                    iconBgColor = BrandBluePastel,
                    onClick = { viewModel.openLegalViewer("terms") },
                    modifier = Modifier.testTag("menu_terms")
                )

                // Logout (Red icon badge)
                MedBankListItem(
                    title = "Sign Out",
                    subtitle = "Log out from this session",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconColor = BrandRed,
                    iconBgColor = BrandRedPastel,
                    showArrow = false,
                    showDivider = false,
                    onClick = { showLogoutConfirmation = true },
                    modifier = Modifier.testTag("menu_logout")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Medical Bank Candidate App • v1.0.4",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            Text(
                text = "Protected by Hardware Device Binding & FLAG_SECURE",
                fontSize = 11.sp,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Sign Out?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to sign out? Your device binding and exam records remain safely stored in the cloud.", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        viewModel.logout()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
