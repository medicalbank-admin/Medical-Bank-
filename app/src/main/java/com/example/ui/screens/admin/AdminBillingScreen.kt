package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import com.example.data.models.PaymentAccount
import com.example.ui.MedBankViewModel
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankChip
import com.example.ui.components.MedBankIconBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBillingScreen(
    viewModel: MedBankViewModel,
    onBack: () -> Unit
) {
    val accounts by viewModel.observePaymentAccounts(activeOnly = false).collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<PaymentAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<PaymentAccount?>(null) }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Billing Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_billing_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    accountToEdit = PaymentAccount(order = (accounts.size + 1))
                    showEditDialog = true
                },
                containerColor = BrandBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("admin_add_payment_account_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Billing Account")
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MedBankIconBadge(
                        icon = Icons.Default.AccountBalance,
                        badgeColor = BrandBlue,
                        backgroundColor = BrandBluePastel,
                        size = 56.dp,
                        iconSize = 30.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Billing Accounts Configured",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the + button below to add your bank accounts, JazzCash, or Easypaisa details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(accounts, key = { it.id.ifBlank { it.accountNumber } }) { account ->
                    MedBankCard(
                        padding = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_account_card_${account.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bank Logo or Default Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(0.5.dp, BorderLight, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (account.logoUrl.isNotBlank()) {
                                    SubcomposeAsyncImage(
                                        model = account.logoUrl,
                                        contentDescription = account.bankName,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp),
                                        contentScale = ContentScale.Fit,
                                        loading = {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        error = {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = BrandBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = account.bankName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    MedBankChip(
                                        text = if (account.isActive) "ACTIVE" else "INACTIVE",
                                        color = if (account.isActive) BrandSuccess else TextSecondary,
                                        bgColor = if (account.isActive) BrandSuccessPastel else BorderLight.copy(alpha = 0.5f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Title: ${account.accountTitle.ifBlank { "N/A" }}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "A/C: ${account.accountNumber}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBlue
                                )

                                if (account.iban.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "IBAN: ${account.iban}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Actions
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        accountToEdit = account
                                        showEditDialog = true
                                    },
                                    modifier = Modifier.testTag("edit_account_${account.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Account",
                                        tint = BrandBlue
                                    )
                                }
                                IconButton(
                                    onClick = { accountToDelete = account },
                                    modifier = Modifier.testTag("delete_account_${account.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Account",
                                        tint = BrandRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit / Add Dialog
    if (showEditDialog && accountToEdit != null) {
        val editing = accountToEdit!!
        var bankName by remember { mutableStateOf(editing.bankName) }
        var accountTitle by remember { mutableStateOf(editing.accountTitle) }
        var accountNumber by remember { mutableStateOf(editing.accountNumber) }
        var iban by remember { mutableStateOf(editing.iban) }
        var logoUrl by remember { mutableStateOf(editing.logoUrl) }
        var orderStr by remember { mutableStateOf(editing.order.toString()) }
        var isActive by remember { mutableStateOf(editing.isActive) }
        var isSaving by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { if (!isSaving) showEditDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardSurface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedBankIconBadge(
                            icon = Icons.Default.AccountBalance,
                            badgeColor = BrandBlue,
                            backgroundColor = BrandBluePastel,
                            size = 38.dp,
                            iconSize = 22.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (editing.id.isBlank()) "Add Payment Account" else "Edit Payment Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = BrandRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it; errorMessage = null },
                        label = { Text("Bank / Wallet Name *") },
                        placeholder = { Text("e.g. Meezan Bank, JazzCash, Easypaisa") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_billing_bank_name_input")
                    )

                    OutlinedTextField(
                        value = accountTitle,
                        onValueChange = { accountTitle = it },
                        label = { Text("Account Title *") },
                        placeholder = { Text("e.g. Medical Bank Official") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_billing_account_title_input")
                    )

                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it; errorMessage = null },
                        label = { Text("Account / Mobile Number *") },
                        placeholder = { Text("e.g. 010203040506 or 03001234567") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_billing_account_number_input")
                    )

                    OutlinedTextField(
                        value = iban,
                        onValueChange = { iban = it },
                        label = { Text("IBAN (Optional)") },
                        placeholder = { Text("e.g. PK00MEZN0000000102030405") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_billing_iban_input")
                    )

                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { logoUrl = it },
                        label = { Text("Bank Logo URL (Optional)") },
                        placeholder = { Text("https://example.com/logo.png") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_billing_logo_url_input")
                    )

                    OutlinedTextField(
                        value = orderStr,
                        onValueChange = { orderStr = it },
                        label = { Text("Display Order") },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active for Students",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandBlue
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showEditDialog = false },
                            enabled = !isSaving
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (bankName.trim().isBlank() || accountNumber.trim().isBlank()) {
                                    errorMessage = "Bank Name and Account Number are required."
                                    return@Button
                                }
                                val orderNum = orderStr.toIntOrNull() ?: 1
                                val updatedAccount = editing.copy(
                                    bankName = bankName.trim(),
                                    accountTitle = accountTitle.trim(),
                                    accountNumber = accountNumber.trim(),
                                    iban = iban.trim(),
                                    logoUrl = logoUrl.trim(),
                                    order = orderNum,
                                    isActive = isActive
                                )
                                isSaving = true
                                viewModel.savePaymentAccount(updatedAccount) { success ->
                                    isSaving = false
                                    if (success) {
                                        showEditDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Payment account saved successfully")
                                        }
                                    } else {
                                        errorMessage = "Failed to save account. Please try again."
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            modifier = Modifier.testTag("admin_billing_save_btn")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (editing.id.isBlank()) "Create Account" else "Save Changes",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (accountToDelete != null) {
        val deleting = accountToDelete!!
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Text(text = "Delete Account?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete ${deleting.bankName} (${deleting.accountNumber})?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = deleting.id
                        accountToDelete = null
                        viewModel.deletePaymentAccount(id) { success ->
                            scope.launch {
                                if (success) {
                                    snackbarHostState.showSnackbar("Account deleted successfully")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to delete account")
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = BrandRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
