package com.monoxer.mathmlcompose.input.hint

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathFilledSlot
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathPlaceholder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HintInputSlotTest {
    private val elem = MathMi("cos")

    @Test
    fun emptyInactive_returnsPlaceholder() {
        val result = hintInputSlot(listOf(null, null), activeBlankIndex = 1, index = 0)
        assertIs<MathPlaceholder>(result)
    }

    @Test
    fun emptyActive_returnsMathMrowWithCursorOnly() {
        val result = hintInputSlot(listOf(null, null), activeBlankIndex = 0, index = 0)
        val row = assertIs<MathMrow>(result)
        assertEquals(1, row.children.size, "Expected 1 child, got ${row.children.size}")
        assertIs<MathCursor>(row.children[0])
    }

    @Test
    fun filledInactive_returnsMathFilledSlot() {
        val result = hintInputSlot(listOf(elem, null), activeBlankIndex = 1, index = 0)
        val slot = assertIs<MathFilledSlot>(result)
        assertEquals(elem, slot.child)
    }

    @Test
    fun filledActive_returnsMathMrowWithCursorThenChild() {
        val result = hintInputSlot(listOf(elem, null), activeBlankIndex = 0, index = 0)
        val row = assertIs<MathMrow>(result)
        assertEquals(2, row.children.size, "Expected 2 children, got ${row.children.size}")
        assertIs<MathCursor>(row.children[0])
        assertEquals(elem, row.children[1])
    }
}
