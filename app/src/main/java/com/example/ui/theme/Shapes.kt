package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), // Card radius: 16dp
    large = RoundedCornerShape(24.dp),
    extraLarge = CircleShape // Button radius: Pill
)

val CardShape = RoundedCornerShape(16.dp)
val ButtonPillShape = CircleShape
val InputFieldShape = RoundedCornerShape(12.dp)
val ChipPillShape = CircleShape
