package com.example.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Enforces FLAG_SECURE on the current Activity window.
 * Requirement 11: Blocks screenshots, screen recording, and recent apps preview
 * on sensitive screens (Exam Player, Review, Result, Admin Bulk Import).
 */
@Composable
fun SetScreenSecure(isSecure: Boolean = true) {
    val context = LocalContext.current
    DisposableEffect(isSecure) {
        val activity = context as? Activity
        if (activity != null && isSecure) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        onDispose {
            val act = context as? Activity
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
