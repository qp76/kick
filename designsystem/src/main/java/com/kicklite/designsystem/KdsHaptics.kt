package com.kicklite.designsystem

import android.view.HapticFeedbackConstants
import android.view.View

/** Optional tactile feedback for deliberate UI actions. */
fun View.performKdsClickHaptic() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}
