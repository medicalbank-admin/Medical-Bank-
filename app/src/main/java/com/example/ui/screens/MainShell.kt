package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MedBankViewModel
import com.example.ui.ScreenTab
import com.example.ui.screens.access.RequestAccessBottomSheet
import com.example.ui.screens.admin.AdminPortalScreen
import com.example.ui.screens.exam.ExamPlayerScreen
import com.example.ui.screens.exam.ExamResultScreen
import com.example.ui.screens.exam.ExamReviewScreen
import com.example.ui.screens.explorer.FolderTreeScreen
import com.example.ui.screens.profile.*
import com.example.ui.screens.results.MyResultsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(viewModel: MedBankViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    val playerState by viewModel.playerState.collectAsState()
    val reviewData by viewModel.reviewData.collectAsState()
    val latestResult by viewModel.latestResult.collectAsState()
    val activeLegalViewer by viewModel.activeLegalViewer.collectAsState()
    val activeProfileScreen by viewModel.activeProfileScreen.collectAsState()

    // Full screen overlays for Exam taking, Review, Legal Text & Profile Sub-Screens
    when {
        activeLegalViewer != null -> {
            com.example.ui.screens.legal.LegalTextViewerScreen(
                type = activeLegalViewer!!,
                viewModel = viewModel,
                onBack = { viewModel.closeLegalViewer() }
            )
            return
        }
        activeProfileScreen != null -> {
            when (activeProfileScreen) {
                "account" -> AccountDetailsScreen(viewModel = viewModel, onBack = { viewModel.openProfileScreen(null) })
                "help" -> HelpSupportScreen(viewModel = viewModel, onBack = { viewModel.openProfileScreen(null) })
                "tutorial" -> AppTutorialScreen(viewModel = viewModel, onBack = { viewModel.openProfileScreen(null) })
                "notifications" -> NotificationPreferencesScreen(viewModel = viewModel, onBack = { viewModel.openProfileScreen(null) })
                "device" -> DeviceBindingScreen(viewModel = viewModel, onBack = { viewModel.openProfileScreen(null) })
                "analytics" -> StudentAnalyticsScreen(viewModel = viewModel, onBack = { viewModel.openProfileScreen(null) })
                "membership" -> MembershipScreen(viewModel = viewModel, onBack = { viewModel.openProfileScreen(null) })
                "refer_earn" -> ReferEarnScreen(viewModel = viewModel, onBack = { viewModel.openProfileScreen(null) })
                else -> ProfileScreen(viewModel = viewModel)
            }
            return
        }
        reviewData != null -> {
            ExamReviewScreen(viewModel = viewModel)
            return
        }
        playerState.isRunning || playerState.isSubmitting -> {
            ExamPlayerScreen(viewModel = viewModel)
            return
        }
        playerState.isCompleted && latestResult != null -> {
            ExamResultScreen(viewModel = viewModel)
            return
        }
    }

    val isAdmin = currentUser?.role == "admin"

    Scaffold(
        bottomBar = {
            val navItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets.navigationBars
            ) {
                // Tab 0: Explorer
                NavigationBarItem(
                    selected = activeTab == ScreenTab.EXPLORER,
                    onClick = { 
                        viewModel.setActiveTab(ScreenTab.EXPLORER)
                        viewModel.resetExplorerNavigation()
                    },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Question Banks") },
                    label = { Text("Banks", fontWeight = if (activeTab == ScreenTab.EXPLORER) FontWeight.Bold else FontWeight.Normal) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_explorer")
                )

                // Tab 1: My Results
                NavigationBarItem(
                    selected = activeTab == ScreenTab.MY_RESULTS,
                    onClick = { viewModel.setActiveTab(ScreenTab.MY_RESULTS) },
                    icon = { Icon(Icons.Default.HistoryEdu, contentDescription = "My Results") },
                    label = { Text("Results", fontWeight = if (activeTab == ScreenTab.MY_RESULTS) FontWeight.Bold else FontWeight.Normal) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_results")
                )

                // Tab 2 (ADMIN ONLY): Admin Portal
                if (isAdmin) {
                    NavigationBarItem(
                        selected = activeTab == ScreenTab.ADMIN,
                        onClick = { viewModel.setActiveTab(ScreenTab.ADMIN) },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Portal") },
                        label = { Text("Admin", fontWeight = if (activeTab == ScreenTab.ADMIN) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_admin")
                    )
                }

                // Tab 3: Profile
                NavigationBarItem(
                    selected = activeTab == ScreenTab.PROFILE,
                    onClick = { viewModel.setActiveTab(ScreenTab.PROFILE) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontWeight = if (activeTab == ScreenTab.PROFILE) FontWeight.Bold else FontWeight.Normal) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (activeTab) {
                ScreenTab.EXPLORER -> FolderTreeScreen(viewModel = viewModel)
                ScreenTab.MY_RESULTS -> MyResultsScreen(viewModel = viewModel)
                ScreenTab.ADMIN -> {
                    if (isAdmin) {
                        AdminPortalScreen(viewModel = viewModel)
                    } else {
                        FolderTreeScreen(viewModel = viewModel)
                    }
                }
                ScreenTab.PROFILE -> ProfileScreen(viewModel = viewModel)
            }
        }

        // Bottom sheet for access request
        RequestAccessBottomSheet(viewModel = viewModel)
    }
}
