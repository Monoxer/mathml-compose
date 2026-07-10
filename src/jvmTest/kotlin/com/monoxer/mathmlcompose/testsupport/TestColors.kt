package com.monoxer.mathmlcompose.testsupport

import androidx.compose.ui.graphics.Color
import com.monoxer.mathmlcompose.render.compose.MxhtmlColors

/** Fixed color set for layout/hit-test unit tests — values are arbitrary, never asserted on. */
internal val TEST_COLORS =
    MxhtmlColors(
        accentColor = Color.Magenta,
        focusedBackground = Color.Transparent,
        focusedBorder = Color.Gray,
    )
