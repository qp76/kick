package com.kicklite.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object KdsShape {
    val control = RoundedCornerShape(8.dp)
    val card = RoundedCornerShape(16.dp)
    val panel = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50)

    val material = Shapes(
        small = control,
        medium = card,
        large = panel
    )
}
