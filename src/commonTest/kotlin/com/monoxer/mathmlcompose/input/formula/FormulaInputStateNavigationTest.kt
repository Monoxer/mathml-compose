package com.monoxer.mathmlcompose.input.formula

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMroot
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathMsqrt
import com.monoxer.mathmlcompose.math.MathMsub
import com.monoxer.mathmlcompose.math.MathMsubsup
import com.monoxer.mathmlcompose.math.MathMsup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class FormulaInputStateNavigationTest {
    private fun FormulaInputState.rootRow(): MathMrow = assertIs(root)

    private fun FormulaInputState.rootChildren(): List<MathElement> = rootRow().children

    @Test
    fun moveLeft_threeDigits_movesNodeByNode() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("1"))
                .insertLeaf(MathMn("2"))
                .insertLeaf(MathMn("3"))
        // state.root = MathMrow([MathMn("1"), MathMn("2"), MathMn("3"), MathCursor])

        val moved1 = state.moveLeft()
        // Expected: [MathMn("1"), MathMn("2"), MathCursor, MathMn("3")]
        val c1 = moved1.rootChildren()
        assertEquals(4, c1.size)
        assertEquals("2", (c1[1] as MathMn).text)
        assertIs<MathCursor>(c1[2])
        assertEquals("3", (c1[3] as MathMn).text)

        val moved2 = moved1.moveLeft()
        // Expected: [MathMn("1"), MathCursor, MathMn("2"), MathMn("3")]
        val c2 = moved2.rootChildren()
        assertEquals(4, c2.size)
        assertEquals("1", (c2[0] as MathMn).text)
        assertIs<MathCursor>(c2[1])
        assertEquals("2", (c2[2] as MathMn).text)
    }

    @Test
    fun moveLeft_onMathMn_movesBeforeNode() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
                .insertLeaf(MathMn("2"))
        // [MathMn("4"), MathMn("2"), MathCursor]

        val moved = state.moveLeft()
        val children = moved.rootChildren()

        // Expected: [MathMn("4"), MathCursor, MathMn("2")]
        assertEquals(3, children.size)
        assertEquals("4", (children[0] as MathMn).text)
        assertIs<MathCursor>(children[1])
        assertEquals("2", (children[2] as MathMn).text)
    }

    @Test
    fun moveLeft_onSingleDigit_movesBeforeNode() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
        // [MathMn("4"), MathCursor]

        val moved = state.moveLeft()
        val children = moved.rootChildren()

        // Expected: [MathCursor, MathMn("4")]
        assertEquals(2, children.size)
        assertIs<MathCursor>(children[0])
        assertEquals("4", (children[1] as MathMn).text)
    }

    @Test
    fun moveLeft_andThenRight_digitsRemainSeparate() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
                .insertLeaf(MathMn("2"))
                .moveLeft()
        // [MathMn("4"), MathCursor, MathMn("2")]

        val moved = state.moveLeft()
        // Expected: [MathCursor, MathMn("4"), MathMn("2")] — no consolidation
        val children = moved.rootChildren()
        assertEquals(3, children.size)
        assertIs<MathCursor>(children[0])
        assertEquals("4", (children[1] as MathMn).text)
        assertEquals("2", (children[2] as MathMn).text)
    }

    @Test
    fun moveRight_onMathMn_movesAfterNode() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
                .insertLeaf(MathMn("2"))
                .moveLeft()
                .moveLeft()
        // [MathCursor, MathMn("4"), MathMn("2")]

        val moved = state.moveRight()
        val children = moved.rootChildren()

        // Expected: [MathMn("4"), MathCursor, MathMn("2")]
        assertEquals(3, children.size)
        assertEquals("4", (children[0] as MathMn).text)
        assertIs<MathCursor>(children[1])
        assertEquals("2", (children[2] as MathMn).text)
    }

    @Test
    fun moveRight_pastMathMn_digitsRemainSeparate() {
        val state =
            FormulaInputState()
                .insertLeaf(MathMn("4"))
                .insertLeaf(MathMn("2"))
                .moveLeft()
        // [MathMn("4"), MathCursor, MathMn("2")]

        val moved = state.moveRight()
        // Expected: [MathMn("4"), MathMn("2"), MathCursor] — no consolidation
        val children = moved.rootChildren()
        assertEquals(3, children.size)
        assertEquals("4", (children[0] as MathMn).text)
        assertEquals("2", (children[1] as MathMn).text)
        assertIs<MathCursor>(children[2])
    }

    /**
     * Walks the tree to find the path to the MathCursor node.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun findCursorPath(
        elem: MathElement,
        prefix: List<Int> = emptyList(),
    ): List<Int>? {
        if (elem is MathCursor) return prefix
        val children: List<MathElement> =
            when (elem) {
                is MathMrow -> elem.children
                is MathMsup -> listOf(elem.main, elem.sup)
                is MathMsub -> listOf(elem.main, elem.sub)
                is MathMsubsup -> listOf(elem.main, elem.sub, elem.sup)
                is MathMfrac -> listOf(elem.denominator, elem.numerator)
                is MathMsqrt -> listOf(elem.main)
                is MathMroot -> listOf(elem.main, elem.root)
                else -> emptyList()
            }
        return children.withIndex().firstNotNullOfOrNull { (i, child) ->
            findCursorPath(child, prefix + i)
        }
    }

    /**
     * Collects cursor paths for all distinct states reachable by repeatedly calling [move].
     * Stops when [maxSteps] is reached or the state stops changing.
     */
    @Suppress("ReturnCount")
    private fun collectPaths(
        initial: FormulaInputState,
        move: FormulaInputState.() -> FormulaInputState,
        maxSteps: Int = 30,
    ): List<List<Int>> {
        val paths = mutableListOf<List<Int>>()
        var state = initial
        paths.add(findCursorPath(state.root) ?: return paths)
        repeat(maxSteps) {
            val next = state.move()
            if (next.root == state.root) return paths // reached boundary
            state = next
            paths.add(findCursorPath(state.root) ?: return paths)
        }
        return paths
    }

    /**
     * Verifies that no two consecutive cursor paths are identical for a given formula.
     */
    private fun assertNoDuplicateStops(
        formula: FormulaInputState,
        label: String,
    ) {
        var state = formula
        repeat(40) { state = state.moveLeft() }

        val rightPaths = collectPaths(state, FormulaInputState::moveRight)
        for (i in 0 until rightPaths.size - 1) {
            assertNotEquals(
                rightPaths[i],
                rightPaths[i + 1],
                "$label: duplicate cursor stop at index $i going right: ${rightPaths[i]}",
            )
        }

        repeat(40) { state = state.moveRight() }
        val leftPaths = collectPaths(state, FormulaInputState::moveLeft)
        for (i in 0 until leftPaths.size - 1) {
            assertNotEquals(
                leftPaths[i],
                leftPaths[i + 1],
                "$label: duplicate cursor stop at index $i going left: ${leftPaths[i]}",
            )
        }
    }

    /** Every moveRight step in 1^{2^3} produces a distinct cursor position. */
    @Test
    fun navigation_nestedSup_noDuplicateStops() {
        val formula =
            FormulaInputState()
                .insertLeaf(MathMn("1"))
                .insertSup()
                .insertLeaf(MathMn("2"))
                .insertSup()
                .insertLeaf(MathMn("3"))
        assertNoDuplicateStops(formula, "1^{2^3}")
    }

    /** No duplicate stops in x_{y^z} (sub with sup inside). */
    @Test
    fun navigation_subWithSupInside_noDuplicateStops() {
        val formula =
            FormulaInputState()
                .insertLeaf(MathMi("x"))
                .insertSub()
                .insertLeaf(MathMi("y"))
                .insertSup()
                .insertLeaf(MathMi("z"))
        assertNoDuplicateStops(formula, "x_{y^z}")
    }

    /** No duplicate stops when navigating through a fraction with scripts in slots. */
    @Test
    fun navigation_fracWithScripts_noDuplicateStops() {
        val formula =
            FormulaInputState()
                .insertFrac()
                .insertLeaf(MathMi("y"))
                .insertSub()
                .insertLeaf(MathMn("3"))
                .moveRight()
                .insertLeaf(MathMi("x"))
                .insertSup()
                .insertLeaf(MathMn("2"))
        assertNoDuplicateStops(formula, "(x^2)/(y_3)")
    }

    /** Cursor must reach the position before sqrt in a frac numerator. */
    @Test
    fun moveLeft_sqrtAtStartOfFracNumerator_canReachPositionBeforeSqrt() {
        // Build frac(numerator=sqrt(x), denominator=y); cursor starts after x inside sqrt.
        // insertFrac places cursor in denominator; moveRight enters numerator.
        val formula =
            FormulaInputState()
                .insertFrac()
                .insertLeaf(MathMn("y"))
                .moveRight()
                .insertSqrt()
                .insertLeaf(MathMn("x"))

        val beforeX = formula.moveLeft()
        // From the first position inside sqrt's radicand, moveLeft must land BEFORE sqrt,
        // not skip over it and land in the denominator.
        val beforeSqrt = beforeX.moveLeft()

        // Cursor path: root[0]=frac, frac[1]=numerator, numerator[0]=cursor (before sqrt)
        assertEquals(listOf(0, 1, 0), findCursorPath(beforeSqrt.root))
    }

    /** Regression: cursor before inner-sqrt must be reachable when navigating sqrt(sqrt(x)). */
    @Test
    fun moveLeft_sqrtInsideSqrt_canReachPositionBeforeInnerSqrt() {
        val formula =
            FormulaInputState()
                .insertSqrt()
                .insertSqrt()
                .insertLeaf(MathMn("x"))

        val beforeX = formula.moveLeft()
        val beforeInnerSqrt = beforeX.moveLeft()

        // Cursor path: root[0]=outer_sqrt, [0,0]=outer_mrow, [0,0,0]=cursor (before inner_sqrt)
        assertEquals(listOf(0, 0, 0), findCursorPath(beforeInnerSqrt.root))
    }

    /** No duplicate stops when sqrt is the first element of a frac numerator. */
    @Test
    fun navigation_sqrtAtStartOfFracNumerator_noDuplicateStops() {
        val formula =
            FormulaInputState()
                .insertFrac()
                .insertLeaf(MathMn("y"))
                .moveRight()
                .insertSqrt()
                .insertLeaf(MathMn("x"))
        assertNoDuplicateStops(formula, "frac(sqrt(x), y)")
    }

    /** No duplicate stops when navigating through a nested sqrt. */
    @Test
    fun navigation_nestedSqrt_noDuplicateStops() {
        val formula =
            FormulaInputState()
                .insertSqrt()
                .insertSqrt()
                .insertLeaf(MathMn("x"))
        assertNoDuplicateStops(formula, "sqrt(sqrt(x))")
    }
}
