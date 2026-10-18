package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MedBankViewModel
import com.example.ui.components.MedBankCard
import com.example.ui.components.MedBankPillButton
import com.example.ui.components.MedBankSegmentedTabs
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: MedBankViewModel) {
    var selectedAuthTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register

    // Login Fields
    var loginCnic by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Register Fields
    var regFullName by remember { mutableStateOf("") }
    var regCnic by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regCountry by remember { mutableStateOf("") }
    var regProgramType by remember { mutableStateOf("mbbs") } // "mbbs" or "licensing"
    var regMbbsYear by remember { mutableIntStateOf(1) } // 1..6
    var regLicensingPath by remember { mutableStateOf("USMLE") } // NRE, FCPS, USMLE, PLAB, DHA, SMLE
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regReferralCode by remember { mutableStateOf("") }

    // Forgot Password Dialog
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotCnic by remember { mutableStateOf("") }

    val authError by viewModel.authError.collectAsState()
    val authSuccessMsg by viewModel.authSuccessMessage.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    val licensingOptions = listOf("USMLE", "NRE", "FCPS", "PLAB", "DHA", "SMLE")

    Scaffold(
        containerColor = CardSurface // White background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // Centered App Brand Logo
            Image(
                painter = painterResource(id = com.example.R.drawable.logo),
                contentDescription = "Medical Bank Logo",
                modifier = Modifier
                    .width(220.dp)
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (selectedAuthTab == 0) "Sign in to access your question banks" else "Join Medical Bank candidate portal",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Segmented tabs: [ Sign In ] [ Register ]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                MedBankSegmentedTabs(
                    tabs = listOf("Sign In", "Register"),
                    selectedIndex = selectedAuthTab,
                    onTabSelected = { idx ->
                        selectedAuthTab = idx
                        viewModel.clearAuthMessages()
                    },
                    modifier = Modifier.testTag("auth_segmented_tabs")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status / Error alerts
            if (!authError.isNullOrBlank()) {
                Surface(
                    color = BrandRedPastel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = BrandRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = authError ?: "",
                            color = BrandRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!authSuccessMsg.isNullOrBlank()) {
                Surface(
                    color = BrandSuccessPastel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BrandSuccess
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = authSuccessMsg ?: "",
                            color = BrandSuccess,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // FORM CONTAINER inside MedBankCard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                MedBankCard(
                    padding = 22.dp
                ) {
                    if (selectedAuthTab == 0) {
                        // --- LOGIN FORM ---
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Log in with your 13-digit CNIC and password",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // CNIC input (12dp corners, soft grey border)
                        OutlinedTextField(
                            value = loginCnic,
                            onValueChange = {
                                if (it.length <= 13 && it.all { ch -> ch.isDigit() }) {
                                    loginCnic = it
                                }
                            },
                            label = { Text("13-Digit CNIC (No dashes)") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = BrandBlue) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_cnic_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password input
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandBlue) },
                            trailingIcon = {
                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                    Icon(
                                        imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val context = androidx.compose.ui.platform.LocalContext.current
                        var rememberMe by remember {
                            mutableStateOf(com.example.util.PreferenceManager.getRememberMe(context))
                        }

                        // Pre-fill loginCnic if rememberMe was true on initial composition
                        LaunchedEffect(Unit) {
                            if (rememberMe) {
                                val savedCnic = com.example.util.PreferenceManager.getSavedCnic(context)
                                if (savedCnic.length == 13 && loginCnic.isBlank()) {
                                    loginCnic = savedCnic
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
                                    modifier = Modifier.testTag("remember_me_checkbox")
                                )
                                Text("Remember Me", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }

                            TextButton(
                                onClick = { showForgotPasswordDialog = true },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text("Forgot Password?", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Pill Button (Blue)
                        MedBankPillButton(
                            text = "Sign In",
                            onClick = {
                                com.example.util.PreferenceManager.saveRememberMe(context, rememberMe, loginCnic)
                                viewModel.login(loginCnic, loginPassword)
                            },
                            enabled = loginCnic.length == 13 && loginPassword.isNotBlank(),
                            isLoading = isAuthLoading,
                            modifier = Modifier.testTag("login_submit_button")
                        )

                    } else {
                        // --- REGISTER FORM ---
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Register for exam preparation & progress tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Full Name
                        OutlinedTextField(
                            value = regFullName,
                            onValueChange = { regFullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_fullname_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // CNIC (13 digits)
                        OutlinedTextField(
                            value = regCnic,
                            onValueChange = {
                                if (it.length <= 13 && it.all { ch -> ch.isDigit() }) {
                                    regCnic = it
                                }
                            },
                            label = { Text("13-Digit CNIC (Without dashes)") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = BrandBlue) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_cnic_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandBlue) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_email_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Phone
                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            label = { Text("Phone / WhatsApp (Optional)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BrandBlue) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_phone_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Country
                        OutlinedTextField(
                            value = regCountry,
                            onValueChange = { regCountry = it },
                            label = { Text("Country of Residence") },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = BrandBlue) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_country_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Track / Academic Focus",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = regProgramType == "mbbs",
                                onClick = { regProgramType = "mbbs" },
                                label = { Text("MBBS (Years 1–6)") },
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = regProgramType == "licensing",
                                onClick = { regProgramType = "licensing" },
                                label = { Text("Licensing Pathways") },
                                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (regProgramType == "mbbs") {
                            Text(
                                text = "Current MBBS Year",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..6).forEach { yr ->
                                    val isSel = regMbbsYear == yr
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSel) BrandBlue else BrandBluePastel.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .testTag("reg_year_$yr")
                                    ) {
                                        IconButton(onClick = { regMbbsYear = yr }) {
                                            Text(
                                                "Y$yr",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else BrandBlue
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Target Licensing Examination",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                licensingOptions.take(3).forEach { path ->
                                    FilterChip(
                                        selected = regLicensingPath == path,
                                        onClick = { regLicensingPath = path },
                                        label = { Text(path, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                licensingOptions.drop(3).forEach { path ->
                                    FilterChip(
                                        selected = regLicensingPath == path,
                                        onClick = { regLicensingPath = path },
                                        label = { Text(path, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Password (min 8 characters)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandBlue) },
                            trailingIcon = {
                                IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                    Icon(
                                        imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_password_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Referral Code (Optional)
                        OutlinedTextField(
                            value = regReferralCode,
                            onValueChange = { regReferralCode = it.uppercase() },
                            label = { Text("Referral Code (Optional)") },
                            placeholder = { Text("e.g. MB1234") },
                            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = BrandBlue) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_referral_code_input"),
                            shape = InputFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Pill Button
                        MedBankPillButton(
                            text = "Complete Registration",
                            onClick = {
                                viewModel.register(
                                    fullName = regFullName,
                                    cnic = regCnic,
                                    email = regEmail,
                                    phone = regPhone,
                                    country = regCountry,
                                    programType = regProgramType,
                                    mbbsYear = if (regProgramType == "mbbs") regMbbsYear else null,
                                    licensingPath = if (regProgramType == "licensing") regLicensingPath else null,
                                    password = regPassword,
                                    referralCode = regReferralCode.ifBlank { null }
                                )
                            },
                            enabled = regFullName.isNotBlank() && regCnic.length == 13 && regEmail.isNotBlank() && regPassword.length >= 8,
                            isLoading = isAuthLoading,
                            modifier = Modifier.testTag("reg_submit_button")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Account Recovery", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter your registered CNIC. An administrator or automated recovery email will be triggered.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = forgotCnic,
                        onValueChange = {
                            if (it.length <= 13 && it.all { ch -> ch.isDigit() }) {
                                forgotCnic = it
                            }
                        },
                        label = { Text("13-Digit CNIC") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_cnic_input"),
                        shape = InputFieldShape
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.forgotPassword(forgotCnic)
                        showForgotPasswordDialog = false
                    },
                    enabled = forgotCnic.length == 13,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Request Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
