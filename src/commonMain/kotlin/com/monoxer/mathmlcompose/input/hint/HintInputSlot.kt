package com.monoxer.mathmlcompose.input.hint

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathFilledSlot
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathPlaceholder

/**
 * Returns the [MathElement] to display for a single hint input slot.
 *
 * - Active + filled  -> [MathMrow] with [MathCursor] on the left of [child].
 * - Active + empty   -> [MathMrow] with only a [MathCursor].
 * - Inactive + filled -> [MathFilledSlot] wrapping [child].
 * - Inactive + empty  -> [MathPlaceholder].
 */
fun hintInputSlot(
    filledBlanks: List<MathElement?>,
    activeBlankIndex: Int,
    index: Int,
): MathElement {
    val child = filledBlanks[index]
    return when {
        child != null && index == activeBlankIndex -> MathMrow(listOf(MathCursor, child))
        child != null -> MathFilledSlot(child)
        index == activeBlankIndex -> MathMrow(listOf(MathCursor))
        else -> MathPlaceholder
    }
}
