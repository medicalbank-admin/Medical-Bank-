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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MedBankViewModel by viewModels()
    private val TAG = "MainActivity"
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

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

        // CRITICAL: Check Remember Me on app launch. If logged in via Firebase but Remember Me is false, force sign out.
        if (FirebaseAuth.getInstance().currentUser != null) {
            val rememberMe = com.example.util.PreferenceManager.getRememberMe(this)
            if (!rememberMe) {
                FirebaseAuth.getInstance().signOut()
                com.example.util.PreferenceManager.clearRememberMe(this)
            } else {
                setupFCM()
            }
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Setup AuthStateListener to save FCM token to Firestore and subscribe to all_students topic when the user successfully logs in
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                setupFCM()
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
                                setupFCM()
                                delay(1000)
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

    override fun onStart() {
        super.onStart()
        authStateListener?.let {
            FirebaseAuth.getInstance().addAuthStateListener(it)
        }
        if (FirebaseAuth.getInstance().currentUser != null) {
            setupFCM()
        }
    }

    override fun onStop() {
        super.onStop()
        authStateListener?.let {
            FirebaseAuth.getInstance().removeAuthStateListener(it)
        }
    }

    fun setupFCM() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid

        // 1. Get Token and Save to Firestore
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("FCM_DEBUG", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_DEBUG", "Token: $token")

            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d("FCM_DEBUG", "Token saved to Firestore") }
                .addOnFailureListener { e -> Log.e("FCM_DEBUG", "Failed to save token", e) }

            try {
                val dbId = getString(R.string.firestore_database_id)
                if (dbId.isNotBlank()) {
                    FirebaseFirestore.getInstance(dbId).collection("users").document(uid)
                        .update("fcmToken", token)
                        .addOnSuccessListener { Log.d("FCM_DEBUG", "Token saved to custom Firestore") }
                        .addOnFailureListener { e -> Log.e("FCM_DEBUG", "Failed to save token to custom Firestore", e) }
                }
            } catch (_: Exception) {}
        }

        // 2. Subscribe to the global broadcast topic
        FirebaseMessaging.getInstance().subscribeToTopic("all_students")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_DEBUG", "Subscribed to all_students topic")
                } else {
                    Log.e("FCM_DEBUG", "Failed to subscribe to topic", task.exception)
                }
            }
    }

    private fun registerFcmTokenAndTopics(uid: String, programType: String, mbbsYear: Int?, licensingPath: String?, notificationOptIn: Boolean) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful && FirebaseAuth.getInstance().currentUser != null) {
                val token = task.result
                val currentUid = FirebaseAuth.getInstance().currentUser!!.uid
                FirebaseFirestore.getInstance().collection("users").document(currentUid)
                    .update("fcmToken", token)
                    .addOnSuccessListener { Log.d("FCM_TOKEN", "Token saved to Firestore: $token") }
                    .addOnFailureListener { e -> Log.e("FCM_TOKEN", "Failed to save token", e) }

                try {
                    val dbId = getString(R.string.firestore_database_id)
                    if (dbId.isNotBlank()) {
                        FirebaseFirestore.getInstance(dbId).collection("users").document(currentUid)
                            .update("fcmToken", token)
                            .addOnSuccessListener { Log.d("FCM_TOKEN", "Token saved to Firestore: $token") }
                            .addOnFailureListener { e -> Log.e("FCM_TOKEN", "Failed to save token", e) }
                    }
                } catch (_: Exception) {}
            } else if (!task.isSuccessful) {
                Log.e("FCM_TOKEN", "Failed to fetch FCM token", task.exception)
            }
        }

        TopicManager.subscribeUserTopics(uid, programType, mbbsYear, licensingPath, notificationOptIn)
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
