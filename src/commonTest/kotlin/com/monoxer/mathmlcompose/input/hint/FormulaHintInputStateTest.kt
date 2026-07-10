package com.monoxer.mathmlcompose.input.hint

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathFilledSlot
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathPlaceholder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class FormulaHintInputStateTest {
    private val elem = MathMi("cos")

    private fun emptyState(count: Int = 3) = FormulaHintInputState(List(count) { null })

    @Test
    fun fillSlot_storesElementInActiveSlot() {
        val state = emptyState().fillSlot(elem)
        assertEquals(elem, state.filledBlanks[0])
    }

    @Test
    fun fillSlot_advancesActiveIndexToNextSlot() {
        val state = emptyState().fillSlot(elem)
        assertEquals(1, state.activeIndex)
    }

    @Test
    fun fillSlot_doesNotAdvancePastLastSlot() {
        val state = emptyState(count = 2).fillSlot(elem).fillSlot(elem)
        assertEquals(1, state.activeIndex)
    }

    @Test
    fun fillSlot_overwritesExistingValue() {
        val other = MathMi("sin")
        val state = emptyState().fillSlot(elem).movePrev().fillSlot(other)
        assertEquals(other, state.filledBlanks[0])
    }

    @Test
    fun clearSlot_setsActiveSlotToNull() {
        val state = emptyState().fillSlot(elem).movePrev().clearSlot()
        assertNull(state.filledBlanks[0])
    }

    @Test
    fun clearSlot_doesNotMoveActiveIndex() {
        val state = emptyState().fillSlot(elem).movePrev()
        assertEquals(0, state.clearSlot().activeIndex)
    }

    @Test
    fun clearSlot_onAlreadyEmptySlot_isNoOp() {
        val state = emptyState()
        assertNull(state.clearSlot().filledBlanks[0])
    }

    @Test
    fun movePrev_decrementsActiveIndex() {
        val state = emptyState().fillSlot(elem)
        assertEquals(0, state.movePrev().activeIndex)
    }

    @Test
    fun movePrev_atFirstSlot_isNoOp() {
        val state = emptyState()
        assertSame(state, state.movePrev())
    }

    @Test
    fun moveNext_incrementsActiveIndex() {
        val state = emptyState()
        assertEquals(1, state.moveNext().activeIndex)
    }

    @Test
    fun moveNext_atLastSlot_isNoOp() {
        val state = emptyState(count = 2).moveNext()
        assertSame(state, state.moveNext())
    }

    @Test
    fun toSlotElement_emptyActive_returnsMathMrowWithCursor() {
        val state = emptyState()
        val result = state.toSlotElement(0)
        val row = assertIs<MathMrow>(result)
        assertIs<MathCursor>(row.children[0])
    }

    @Test
    fun toSlotElement_emptyInactive_returnsPlaceholder() {
        val state = emptyState()
        assertIs<MathPlaceholder>(state.toSlotElement(1))
    }

    @Test
    fun toSlotElement_filledInactive_returnsMathFilledSlot() {
        val state = emptyState().fillSlot(elem) // activeIndex moves to 1
        val result = state.toSlotElement(0)
        assertEquals(elem, assertIs<MathFilledSlot>(result).child)
    }

    @Test
    fun toSlotElement_filledActive_returnsMathMrowWithCursorThenChild() {
        val state = emptyState().fillSlot(elem).movePrev() // back to index 0
        val row = assertIs<MathMrow>(state.toSlotElement(0))
        assertIs<MathCursor>(row.children[0])
        assertEquals(elem, row.children[1])
    }
}
