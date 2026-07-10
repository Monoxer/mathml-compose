package com.monoxer.mathmlcompose.input.hint

import com.monoxer.mathmlcompose.math.MathElement

/**
 * Immutable state for hint-based formula input.
 *
 * The user fills [slotCount] blank slots one at a time. Each slot either holds a
 * [MathElement] answer or is null (empty). [activeIndex] tracks which slot currently
 * has focus.
 *
 * Rendering: call [toSlotElement] to get the [MathElement] to display for each slot,
 * then compose those into the full hint formula for [MathCanvas].
 */
data class FormulaHintInputState(
    val filledBlanks: List<MathElement?>,
    val activeIndex: Int = 0,
) {
    val slotCount: Int get() = filledBlanks.size

    /** Returns the display element for slot [index] (cursor, filled slot, or placeholder). */
    fun toSlotElement(index: Int): MathElement = hintInputSlot(filledBlanks, activeIndex, index)
}

/** Fills the active slot with [elem] and advances the cursor to the next slot. */
fun FormulaHintInputState.fillSlot(elem: MathElement): FormulaHintInputState {
    val newBlanks = filledBlanks.toMutableList()
    newBlanks[activeIndex] = elem
    val nextIndex = if (activeIndex < slotCount - 1) activeIndex + 1 else activeIndex
    return copy(filledBlanks = newBlanks, activeIndex = nextIndex)
}

/** Clears the active slot. */
fun FormulaHintInputState.clearSlot(): FormulaHintInputState {
    val newBlanks = filledBlanks.toMutableList()
    newBlanks[activeIndex] = null
    return copy(filledBlanks = newBlanks)
}

fun FormulaHintInputState.movePrev(): FormulaHintInputState =
    if (activeIndex >
        0
    ) {
        copy(activeIndex = activeIndex - 1)
    } else {
        this
    }

fun FormulaHintInputState.moveNext(): FormulaHintInputState =
    if (activeIndex <
        slotCount - 1
    ) {
        copy(activeIndex = activeIndex + 1)
    } else {
        this
    }
