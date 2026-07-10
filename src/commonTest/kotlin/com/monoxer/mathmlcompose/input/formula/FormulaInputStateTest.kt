package com.monoxer.mathmlcompose.input.formula

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMo
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathMsqrt
import com.monoxer.mathmlcompose.math.MathMsup
import com.monoxer.mathmlcompose.math.MathPlaceholder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FormulaInputStateTest {
    private fun FormulaInputState.rootRow(): MathMrow = assertIs(root)

    private fun FormulaInputState.rootChildren(): List<MathElement> = rootRow().children

    @Test
    fun insertLeaf_consecutiveMathMn_storesAsSeparateNodes() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
                .insertLeaf(MathMn("2"))

        val children = state.rootChildren()
        // Expected: [MathMn("4"), MathMn("2"), MathCursor]
        assertEquals(3, children.size)
        assertEquals("4", (children[0] as MathMn).text)
        assertEquals("2", (children[1] as MathMn).text)
        assertIs<MathCursor>(children[2])
    }

    @Test
    fun insertLeaf_mnFollowedByMi_doesNotMerge() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
                .insertLeaf(MathMi("x"))

        val children = state.rootChildren()
        // Expected: [MathMn("4"), MathMi("x"), MathCursor]
        assertEquals(3, children.size)
        assertIs<MathMn>(children[0])
        assertIs<MathMi>(children[1])
        assertIs<MathCursor>(children[2])
    }

    @Test
    fun backspace_onEmptySupExponent_restoresBaseToParentRow() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("x"))
                .insertSup()

        val result = state.backspace()
        val children = result.rootChildren()

        // Expected: [MathMn("x"), MathCursor]
        assertEquals(2, children.size, "Base should be restored to parent row after backspace on empty exponent")
        assertIs<MathMn>(children[0])
        assertEquals("x", (children[0] as MathMn).text)
        assertIs<MathCursor>(children[1])
    }

    @Test
    fun backspace_onEmptySubSubscript_restoresBaseToParentRow() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMi("a"))
                .insertSub()

        val result = state.backspace()
        val children = result.rootChildren()

        // Expected: [MathMi("a"), MathCursor]
        assertEquals(2, children.size, "Base should be restored to parent row after backspace on empty subscript")
        assertIs<MathMi>(children[0])
        assertEquals("a", (children[0] as MathMi).text)
        assertIs<MathCursor>(children[1])
    }

    @Test
    fun backspace_onNonEmptySupExponent_deletesLastCharInExponent() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("x"))
                .insertSup()
                .insertLeaf(MathMn("2"))

        val result = state.backspace()
        val msup = assertIs<MathMsup>(result.rootChildren()[0])
        val supRow = assertIs<MathMrow>(msup.sup)
        assertEquals(1, supRow.children.size)
        assertIs<MathCursor>(supRow.children[0])
    }

    @Test
    fun backspace_onMathMn_deletesWholeNode() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
                .insertLeaf(MathMn("2"))
        // [MathMn("4"), MathMn("2"), MathCursor]

        val result = state.backspace()
        val children = result.rootChildren()
        // Expected: [MathMn("4"), MathCursor]
        assertEquals(2, children.size)
        assertEquals("4", (children[0] as MathMn).text)
        assertIs<MathCursor>(children[1])
    }

    @Test
    fun backspace_atStartOfNonEmptySlot_justMovesLeft() {
        val state =
            FormulaInputState()
                .insertFrac()
                .insertLeaf(MathMn("3"))
                .moveLeft()
        val result = state.backspace()
        val fracElem = result.rootChildren().firstOrNull { it is MathMfrac }
        assertTrue(fracElem != null, "Fraction should still be present after backspace at slot start")
    }

    @Test
    fun backspace_atVeryStart_isNoop() {
        val state = FormulaInputState()
        val result = state.backspace()
        assertEquals(state.root, result.root)
    }

    @Test
    fun insertFrac_cursorGoesToDenominator() {
        val state = FormulaInputState().insertFrac()
        val root = state.rootRow()
        assertEquals(1, root.children.size)
        val frac = assertIs<MathMfrac>(root.children[0])
        val denominator = assertIs<MathMrow>(frac.denominator)
        assertIs<MathCursor>(denominator.children[0])
        assertIs<MathPlaceholder>(frac.numerator)
    }

    @Test
    fun insertFrac_moveRight_entersNumerator() {
        val state =
            FormulaInputState()
                .insertFrac()
                .moveRight()

        val root = state.rootRow()
        val frac = assertIs<MathMfrac>(root.children[0])
        val numerator = assertIs<MathMrow>(frac.numerator)
        assertIs<MathCursor>(numerator.children[0])
    }

    @Test
    fun insertFrac_moveRight_twice_exitsFrac() {
        val state =
            FormulaInputState()
                .insertFrac()
                .moveRight() // denominator → numerator
                .moveRight() // numerator → after frac in root

        val children = state.rootChildren()
        // [MathFrac(...), MathCursor]
        assertEquals(2, children.size)
        assertIs<MathMfrac>(children[0])
        assertIs<MathCursor>(children[1])
    }

    @Test
    fun insertSup_withNoPrecedingElement_createsPlaceholderBase() {
        val state = FormulaInputState().insertSup()
        val root = state.rootRow()
        val msup = assertIs<MathMsup>(root.children[0])
        assertIs<MathPlaceholder>(msup.main)
        val supRow = assertIs<MathMrow>(msup.sup)
        assertIs<MathCursor>(supRow.children[0])
    }

    @Test
    fun insertSqrt_cursorGoesInsideRadicand() {
        val state = FormulaInputState().insertSqrt()
        val root = state.rootRow()
        val sqrt = assertIs<MathMsqrt>(root.children[0])
        val inner = assertIs<MathMrow>(sqrt.main)
        assertIs<MathCursor>(inner.children[0])
    }

    @Test
    fun toResultElement_emptyState_returnsPlaceholder() {
        val result = FormulaInputState().toResultElement()
        assertIs<MathPlaceholder>(result)
    }

    @Test
    fun toResultElement_withContent_removesOnlyCursor() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMi("x"))
                .insertLeaf(MathMo("+"))
                .insertLeaf(MathMn("1"))

        val result = assertIs<MathMrow>(state.toResultElement())
        assertEquals(3, result.children.size)
        assertEquals("x", (result.children[0] as MathMi).text)
        assertEquals("+", (result.children[1] as MathMo).text)
        assertEquals("1", (result.children[2] as MathMn).text)
    }

    @Test
    fun toResultElement_removesOnlyCursorLeavingDigitsSeparate() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
                .insertLeaf(MathMn("2"))
                .moveLeft()
        // root = MathMrow([MathMn("4"), MathCursor, MathMn("2")])

        val result = state.toResultElement()
        val row = assertIs<MathMrow>(result)
        // Digits remain as separate nodes — no merging
        assertEquals(2, row.children.size)
        assertEquals("4", (row.children[0] as MathMn).text)
        assertEquals("2", (row.children[1] as MathMn).text)
    }

    @Test
    fun moveCursorTo_rightHalfOfLeaf_placesCursorAfter() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMi("x"))
                .insertLeaf(MathMi("y"))

        val result = state.moveCursorTo(listOf(0), onRight = true)
        val children = result.rootChildren()
        // [MathMi("x"), MathCursor, MathMi("y")]
        assertEquals(3, children.size)
        assertIs<MathMi>(children[0])
        assertIs<MathCursor>(children[1])
        assertIs<MathMi>(children[2])
    }

    @Test
    fun moveCursorTo_leftHalfOfLeaf_placesCursorBefore() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMi("x"))
                .insertLeaf(MathMi("y"))

        val result = state.moveCursorTo(listOf(1), onRight = false)
        val children = result.rootChildren()
        // [MathMi("x"), MathCursor, MathMi("y")]
        assertEquals(3, children.size)
        assertIs<MathMi>(children[0])
        assertIs<MathCursor>(children[1])
        assertIs<MathMi>(children[2])
    }
}
