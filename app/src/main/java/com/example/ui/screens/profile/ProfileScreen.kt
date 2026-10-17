package com.example.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MedBankViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No user signed in.")
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.logo),
                            contentDescription = "Medical Bank Logo",
                            modifier = Modifier.height(28.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Candidate Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.logo),
                contentDescription = "Medical Bank Logo",
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            StatusBadge(
                text = user.role.uppercase(),
                color = if (user.role == "admin") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                icon = if (user.role == "admin") Icons.Default.Shield else Icons.Default.School
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Details Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Candidate Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    ProfileRow(label = "CNIC / National ID", value = user.cnic, icon = Icons.Default.Badge)
                    ProfileRow(label = "Email", value = user.email, icon = Icons.Default.Email)
                    ProfileRow(label = "Phone", value = user.phone, icon = Icons.Default.Phone)
                    ProfileRow(label = "Country", value = user.country, icon = Icons.Default.Public)
                    ProfileRow(
                        label = "Medical Track",
                        value = if (user.programType == "mbbs") "MBBS (Year ${user.mbbsYear ?: 1})" else "Licensing (${user.licensingPath ?: "USMLE"})",
                        icon = Icons.Default.MedicalInformation
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Device Binding Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Device (1/1 Limit)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        StatusBadge(text = "BOUND", color = MaterialTheme.colorScheme.primary, icon = Icons.Default.Lock)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Device Model: ${viewModel.deviceModel}", style = MaterialTheme.typography.bodyMedium)
                    Text("Hardware ID: ${viewModel.deviceId.take(12)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Preferences Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notification Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    val subscribed = user.subscribedTopics
                    val optIn = user.notificationOptIn

                    // Access Approval Alerts switch
                    val hasApprovals = subscribed.contains("medbank_approvals") || optIn
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Access approval alerts", fontWeight = FontWeight.Medium)
                            Text("Get notified when access requests are approved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = hasApprovals,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    com.example.notifications.TopicManager.subscribeTopic(user.uid, "medbank_approvals", subscribed)
                                } else {
                                    com.example.notifications.TopicManager.unsubscribeTopic(user.uid, "medbank_approvals", subscribed)
                                }
                            }
                        )
                    }

                    // New exam announcements switch
                    val hasExams = subscribed.contains("medbank_new_exams") || optIn
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("New exam announcements", fontWeight = FontWeight.Medium)
                            Text("Alerts when new exams are published in your track", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = hasExams,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    com.example.notifications.TopicManager.subscribeTopic(user.uid, "medbank_new_exams", subscribed)
                                } else {
                                    com.example.notifications.TopicManager.unsubscribeTopic(user.uid, "medbank_new_exams", subscribed)
                                }
                            }
                        )
                    }

                    // General announcements switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("General announcements", fontWeight = FontWeight.Medium)
                            Text("System updates and important news", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = optIn,
                            onCheckedChange = { checked ->
                                com.example.notifications.TopicManager.subscribeUserTopics(
                                    uid = user.uid,
                                    programType = user.programType,
                                    mbbsYear = user.mbbsYear,
                                    licensingPath = user.licensingPath,
                                    notificationOptIn = checked
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sign Out Button
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
