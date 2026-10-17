package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.notifications.NotificationHelper
import com.example.notifications.TopicManager
import com.example.ui.MedBankViewModel
import com.example.ui.screens.MainShell
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.theme.MedBankTheme
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MedBankViewModel by viewModels()
    private val TAG = "MainActivity"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted.")
        } else {
            Log.d(TAG, "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannels(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MedBankTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(1500)
                        showSplash = false
                    }

                    if (showSplash) {
                        SplashScreen()
                    } else {
                        val currentUser by viewModel.currentUser.collectAsState()

                        // Register FCM token and subscribe to topics upon login
                        LaunchedEffect(currentUser?.uid) {
                            val user = currentUser
                            if (user != null) {
                                delay(4000) // Allow more time for Firebase to initialize
                                registerFcmTokenAndTopics(user.uid, user.programType, user.mbbsYear, user.licensingPath, user.notificationOptIn)
                            }
                        }

                        if (currentUser == null) {
                            AuthScreen(viewModel = viewModel)
                        } else {
                            MainShell(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun registerFcmTokenAndTopics(uid: String, programType: String, mbbsYear: Int?, licensingPath: String?, notificationOptIn: Boolean) {
        Log.d("MEDBANK_FCM", "GPS=${com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)}")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val e = task.exception
            if (!task.isSuccessful) {
                Log.e("MEDBANK_FCM", "FAIL class=${e?.javaClass?.simpleName} msg=${e?.message}")
            } else {
                Log.d("MEDBANK_FCM", "OK token=${task.result}")
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .update("fcmToken", task.result)
                        .addOnSuccessListener { Log.d("MEDBANK_FCM", "Firestore write OK for $uid") }
                        .addOnFailureListener { Log.e("MEDBANK_FCM", "Firestore write FAIL: ${it.message}") }
                } else {
                    Log.e("MEDBANK_FCM", "User is null when writing token")
                }
            }
        }

        TopicManager.subscribeUserTopics(uid, programType, mbbsYear, licensingPath, notificationOptIn)
    }

    private fun updateTokenWithRetry(uid: String, token: String, retries: Int) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "fcmToken" to token,
                    "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Log.d(TAG, "FCM token saved successfully.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving FCM token: ${e.message}")
                if (retries > 0) {
                    updateTokenWithRetry(uid, token, retries - 1)
                }
            }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Medical Bank Logo",
                modifier = Modifier
                    .width(240.dp)
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Practice with Confidence",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
