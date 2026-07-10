package com.monoxer.mathmlcompose.render.compose

import androidx.compose.ui.graphics.Color

/**
 * Colors the host app must supply so this library carries no dependency on the host's
 * design system. [accentColor] tints the blinking input cursor; [focusedBackground] and
 * [focusedBorder] highlight the active editable slot while [MathCanvas.editable] is true.
 */
data class MxhtmlColors(
    val accentColor: Color,
    val focusedBackground: Color,
    val focusedBorder: Color,
)
