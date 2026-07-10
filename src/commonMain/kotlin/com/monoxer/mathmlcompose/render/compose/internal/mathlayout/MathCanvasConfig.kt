package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.monoxer.mathmlcompose.render.compose.MxhtmlColors

internal data class MathCanvasConfig(
    val scale: Float,
    val baseHeight: Float,
    val textColor: Color,
    val colors: MxhtmlColors,
    val fontFamily: FontFamily = FontFamily.Default,
    val cursorAlpha: Float = 1f,
    val editable: Boolean = false,
) {
    companion object {
        private const val MIN_HEIGHT_SP = 12f
    }

    val baseHSpace: Float = 2f * scale
    val baseVSpace: Float = 2f * scale

    // Wide horizontal margin for operator-like elements (mo, mtext): extra breathing room
    // around symbols such as +, =, etc.
    val wideHMargin: Float = baseHSpace / 2f

    // Minimum font height (≈12sp). Note: if baseHeight is already close to minHeight, nested
    // modifier elements will render at nearly the same size as their parent, reducing readability.
    val minHeight: Float = MIN_HEIGHT_SP * scale
}

internal fun MathCanvasConfig.getSmaller(level: Int = 1): MathCanvasConfig =
    copy(baseHeight = maxOf(baseHeight - (2 * level * scale), minHeight))
