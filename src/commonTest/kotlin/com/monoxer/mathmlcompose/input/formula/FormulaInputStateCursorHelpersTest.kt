package com.monoxer.mathmlcompose.input.formula

import com.monoxer.mathmlcompose.input.formula.internal.withCursorAtFirst
import com.monoxer.mathmlcompose.input.formula.internal.withCursorAtLast
import com.monoxer.mathmlcompose.input.formula.internal.withCursorRemoved
import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMroot
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathMsqrt
import com.monoxer.mathmlcompose.math.MathMsup
import com.monoxer.mathmlcompose.math.MathPlaceholder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FormulaInputStateCursorHelpersTest {
    @Test
    fun withCursorAtFirst_placeholder_replaceWithCursorRow() {
        val result = assertIs<MathMrow>(withCursorAtFirst(MathPlaceholder))
        assertIs<MathCursor>(result.children.single())
    }

    @Test
    fun withCursorAtFirst_mrow_insertsCursorBeforeFirstElement() {
        val row = MathMrow(listOf(MathMn("1"), MathMi("x")))
        val result = assertIs<MathMrow>(withCursorAtFirst(row))
        assertIs<MathCursor>(result.children[0])
        assertEquals(MathMn("1"), result.children[1])
    }

    @Test
    fun withCursorAtFirst_attributedMrow_preservesAttributes() {
        val attributes = mapOf("data-meta" to "value")
        val row = MathMrow(listOf(MathMn("1")), attributes)

        val result = assertIs<MathMrow>(withCursorAtFirst(row))

        assertEquals(attributes, result.attributes)
        assertEquals(listOf(MathCursor, MathMn("1")), result.children)
    }

    @Test
    fun withCursorAtFirst_mrowWithLeadingComplexElement_entersItInstead() {
        // Cursor should skip into the leading sup rather than stopping before it
        val sup = MathMsup(main = MathPlaceholder, sup = MathMrow(listOf(MathPlaceholder)))
        val row = MathMrow(listOf(sup))
        val result = assertIs<MathMrow>(withCursorAtFirst(row))
        // Cursor ended up inside the sup's first slot, not before the sup in the outer row
        assertIs<MathMsup>(result.children[0])
    }

    @Test
    fun withCursorAtFirst_mrowWithLeadingSqrt_stopsBefore() {
        // sqrt has a radical sign (√) to the LEFT of its content, so "before √x" and
        // "inside √x at start" are visually distinct — cursor must stop before sqrt, not skip in.
        val sqrt = MathMsqrt(main = MathMrow(listOf(MathPlaceholder)))
        val row = MathMrow(listOf(sqrt))
        val result = assertIs<MathMrow>(withCursorAtFirst(row))
        assertIs<MathCursor>(result.children[0])
        assertIs<MathMsqrt>(result.children[1])
    }

    @Test
    fun withCursorAtFirst_mrowWithLeadingMroot_stopsBefore() {
        val mroot = MathMroot(main = MathMrow(listOf(MathPlaceholder)), root = MathMrow(listOf(MathPlaceholder)))
        val row = MathMrow(listOf(mroot))
        val result = assertIs<MathMrow>(withCursorAtFirst(row))
        assertIs<MathCursor>(result.children[0])
        assertIs<MathMroot>(result.children[1])
    }

    @Test
    fun withCursorAtLast_placeholder_replaceWithCursorRow() {
        val result = assertIs<MathMrow>(withCursorAtLast(MathPlaceholder))
        assertIs<MathCursor>(result.children.single())
    }

    @Test
    fun withCursorAtLast_mrow_appendsCursorAfterLastElement() {
        val row = MathMrow(listOf(MathMn("1"), MathMi("x")))
        val result = assertIs<MathMrow>(withCursorAtLast(row))
        assertIs<MathCursor>(result.children.last())
        assertEquals(3, result.children.size)
    }

    @Test
    fun withCursorRemoved_cursorNode_returnsPlaceholder() {
        assertIs<MathPlaceholder>(withCursorRemoved(MathCursor))
    }

    @Test
    fun withCursorRemoved_mrowWithOnlyCursor_returnsPlaceholder() {
        val row = MathMrow(listOf(MathCursor))
        assertIs<MathPlaceholder>(withCursorRemoved(row))
    }

    @Test
    fun withCursorRemoved_attributedMrowWithOnlyCursor_preservesEmptyRowAndAttributes() {
        val attributes = mapOf("data-meta" to "value")
        val row = MathMrow(listOf(MathCursor), attributes)

        val result = assertIs<MathMrow>(withCursorRemoved(row))

        assertTrue(result.children.isEmpty())
        assertEquals(attributes, result.attributes)
    }

    @Test
    fun withCursorRemoved_mrowWithCursorAndContent_removesCursorOnly() {
        val row = MathMrow(listOf(MathMn("1"), MathCursor, MathMi("x")))
        val result = assertIs<MathMrow>(withCursorRemoved(row))
        assertEquals(2, result.children.size)
        assertEquals(MathMn("1"), result.children[0])
        assertEquals(MathMi("x"), result.children[1])
    }
}
