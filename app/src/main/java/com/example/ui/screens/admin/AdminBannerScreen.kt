package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MedBankViewModel
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankPillButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBannerScreen(viewModel: MedBankViewModel) {
    val currentBanner by viewModel.banner.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isActive by remember { mutableStateOf(currentBanner.isActive) }
    var message by remember { mutableStateOf(currentBanner.message) }
    var buttonText by remember { mutableStateOf(currentBanner.buttonText) }
    var linkUrl by remember { mutableStateOf(currentBanner.linkUrl) }
    var isSaving by remember { mutableStateOf(false) }

    // Sync state when currentBanner updates from Firestore
    LaunchedEffect(currentBanner) {
        isActive = currentBanner.isActive
        message = currentBanner.message
        buttonText = currentBanner.buttonText.ifBlank { "Update Now" }
        linkUrl = currentBanner.linkUrl
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Banner Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
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
            MedBankCard(
                padding = 20.dp,
                modifier = Modifier.testTag("admin_banner_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Enable Banner",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Show announcement on student home",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBlue),
                        modifier = Modifier.testTag("banner_enable_switch")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 150) message = it },
                    label = { Text("Message (Max 150 chars)") },
                    placeholder = { Text("A new version of Medical Bank is available!") },
                    maxLines = 3,
                    shape = InputFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_message_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = buttonText,
                    onValueChange = { buttonText = it },
                    label = { Text("Button Text") },
                    placeholder = { Text("Update Now") },
                    singleLine = true,
                    shape = InputFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_button_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text("Link URL") },
                    placeholder = { Text("https://clowd.host/a/5617beqg93") },
                    singleLine = true,
                    shape = InputFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_link_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                MedBankPillButton(
                    text = "Save Banner Changes",
                    onClick = {
                        if (message.isBlank() || linkUrl.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Message and Link URL cannot be empty.") }
                            return@MedBankPillButton
                        }
                        isSaving = true
                        viewModel.saveBanner(
                            isActive = isActive,
                            message = message.trim(),
                            buttonText = buttonText.ifBlank { "Update Now" }.trim(),
                            linkUrl = linkUrl.trim(),
                            onSuccess = {
                                isSaving = false
                                scope.launch { snackbarHostState.showSnackbar("Banner updated successfully!") }
                            },
                            onError = { err ->
                                isSaving = false
                                scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                            }
                        )
                    },
                    isLoading = isSaving,
                    modifier = Modifier.testTag("save_banner_button")
                )
            }
        }
    }
}
