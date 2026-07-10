package com.monoxer.mathmlcompose.input.formula

import com.monoxer.mathmlcompose.input.formula.internal.childrenOf
import com.monoxer.mathmlcompose.input.formula.internal.findCursorPath
import com.monoxer.mathmlcompose.input.formula.internal.hasEditableSlots
import com.monoxer.mathmlcompose.input.formula.internal.isEditableSlot
import com.monoxer.mathmlcompose.input.formula.internal.nodeAtPath
import com.monoxer.mathmlcompose.input.formula.internal.replaceAtPath
import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathPlaceholder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormulaInputStateTreeHelpersTest {
    @Test
    fun childrenOf_mathMrow_returnsChildrenInOrder() {
        val children = listOf(MathMn("1"), MathMi("x"), MathCursor)
        val row = MathMrow(children)
        assertEquals(children, childrenOf(row))
    }

    @Test
    fun childrenOf_mathMfrac_returnsDenominatorThenNumerator() {
        val den = MathMrow(listOf(MathCursor))
        val num = MathPlaceholder
        val frac = MathMfrac(numerator = num, denominator = den)
        // childrenOf uses slot order: [denominator(0), numerator(1)]
        assertEquals(listOf(den, num), childrenOf(frac))
    }

    @Test
    fun childrenOf_leaf_returnsEmpty() {
        assertTrue(childrenOf(MathMn("3")).isEmpty())
        assertTrue(childrenOf(MathMi("x")).isEmpty())
        assertTrue(childrenOf(MathPlaceholder).isEmpty())
    }

    @Test
    fun findCursorPath_cursorAtRoot_returnsEmptyList() {
        assertEquals(emptyList(), findCursorPath(MathCursor))
    }

    @Test
    fun findCursorPath_cursorInNestedMrow_returnsCorrectPath() {
        // MathMrow([MathMn("1"), MathCursor]) → cursor is at index 1
        val row = MathMrow(listOf(MathMn("1"), MathCursor))
        assertEquals(listOf(1), findCursorPath(row))
    }

    @Test
    fun findCursorPath_cursorNotPresent_returnsNull() {
        val row = MathMrow(listOf(MathMn("1"), MathMi("x")))
        assertNull(findCursorPath(row))
    }

    @Test
    fun nodeAtPath_emptyPath_returnsRoot() {
        val root = MathMrow(listOf(MathCursor))
        assertEquals(root, nodeAtPath(root, emptyList()))
    }

    @Test
    fun nodeAtPath_singleIndex_returnsDirectChild() {
        val child = MathMn("5")
        val root = MathMrow(listOf(child, MathCursor))
        assertEquals(child, nodeAtPath(root, listOf(0)))
    }

    @Test
    fun replaceAtPath_emptyPath_replacesRoot() {
        val root = MathMrow(listOf(MathCursor))
        val replacement = MathMn("9")
        assertEquals(replacement, replaceAtPath(root, emptyList(), replacement))
    }

    @Test
    fun replaceAtPath_singleIndex_replacesChild() {
        val root = MathMrow(listOf(MathMn("1"), MathCursor))
        val replacement = MathMi("y")
        val result = assertIs<MathMrow>(replaceAtPath(root, listOf(0), replacement))
        assertEquals(replacement, result.children[0])
        assertIs<MathCursor>(result.children[1])
    }

    @Test
    fun isEditableSlot_placeholderAndMrow_returnTrue() {
        assertTrue(isEditableSlot(MathPlaceholder))
        assertTrue(isEditableSlot(MathMrow(emptyList())))
    }

    @Test
    fun hasEditableSlots_fracWithMrowSlots_returnsTrue() {
        val frac =
            MathMfrac(
                numerator = MathMrow(listOf(MathCursor)),
                denominator = MathPlaceholder,
            )
        assertTrue(hasEditableSlots(frac))
    }
}
