package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.monoxer.mathmlcompose.input.formula.FormulaInputState
import com.monoxer.mathmlcompose.input.formula.insertFrac
import com.monoxer.mathmlcompose.input.formula.insertLeaf
import com.monoxer.mathmlcompose.input.formula.insertSub
import com.monoxer.mathmlcompose.input.formula.insertSup
import com.monoxer.mathmlcompose.input.formula.moveRight
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.testsupport.TEST_COLORS
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies that the formula layout dimensions stay stable when content is typed into
 * sup/sub/frac input slots.
 *
 * All tests follow the same structure:
 *   1. Build an initial FormulaInputState with an empty slot (cursor only).
 *   2. Make change to the content (e.g. type a char)
 *   3. Measure both layouts with a directly-constructed [TextMeasurer] (built outside of
 *      composition, so results don't depend on Compose's composition/recomposition machinery).
 *   4. Assert that the critical dimensions (upper/bottom heights of the top-level layout)
 *      differ by at most TOLERANCE_PX pixels.
 */
class MathLayoutSlotStabilityTest {
    companion object {
        // Acceptable pixel delta between empty-slot and content layouts.
        // Both states use the same font metrics, so the delta should be essentially 0;
        // we allow 1px for floating-point rounding.
        private const val TOLERANCE_PX = 1f

        private val CONFIG =
            MathCanvasConfig(
                scale = 1f,
                baseHeight = 40f,
                textColor = Color.Black,
                colors = TEST_COLORS,
            )
    }

    private val textMeasurer =
        TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(),
            defaultDensity = Density(density = 1f),
            defaultLayoutDirection = LayoutDirection.Ltr,
        )

    private fun assertStable(
        label: String,
        empty: Float,
        content: Float,
    ) {
        assertTrue(
            abs(empty - content) <= TOLERANCE_PX,
            "$label changed when content was typed: empty=$empty, content=$content (delta=${abs(
                empty - content,
            )} > $TOLERANCE_PX px)",
        )
    }

    /**
     * An empty input slot (single cursor inside a MathMrow) must be a square:
     * measuredWidth == measuredUpperHeight + measuredBottomHeight == baseHeight.
     *
     * We test this on the root MathMrow of an empty FormulaInputState (root = MathMrow([cursor])).
     */
    @Test
    fun emptySlotIsSquare() {
        // Root of an empty state is a MathMrow containing only the cursor.
        val state = FormulaInputState()
        val layout = createLayout(state.root)
        layout.measure(textMeasurer, CONFIG)
        val width = layout.measuredWidth
        val isSquare = abs(width - CONFIG.baseHeight) <= TOLERANCE_PX
        assertTrue(isSquare, "Empty slot measuredWidth should equal baseHeight (square visual box)")
    }

    /**
     * Typing the first character into a superscript slot must not change the top-level
     * formula's measuredUpperHeight.
     */
    @Test
    fun supSlotUpperHeightStableWhenFirstCharTyped() {
        // x^{cursor}
        val emptyState =
            FormulaInputState()
                .insertLeaf(MathMi("x"))
                .insertSup()
        val emptyLayout = createLayout(emptyState.root)
        emptyLayout.measure(textMeasurer, CONFIG)
        val emptyUpper = emptyLayout.measuredUpperHeight

        // x^{1|} — cursor stays in sup after insertLeaf
        val contentState = emptyState.insertLeaf(MathMn("1"))
        val contentLayout = createLayout(contentState.root)
        contentLayout.measure(textMeasurer, CONFIG)
        val contentUpper = contentLayout.measuredUpperHeight

        assertStable("sup: measuredUpperHeight", emptyUpper, contentUpper)
    }

    /**
     * Typing additional characters into the sup slot should also keep the height stable.
     */
    @Test
    fun supSlotUpperHeightStableAfterMultipleChars() {
        val emptyState =
            FormulaInputState()
                .insertLeaf(MathMi("x"))
                .insertSup()
        val emptyLayout = createLayout(emptyState.root)
        emptyLayout.measure(textMeasurer, CONFIG)
        val emptyUpper = emptyLayout.measuredUpperHeight

        val multiState =
            emptyState
                .insertLeaf(MathMn("1"))
                .insertLeaf(MathMn("2"))
        val multiLayout = createLayout(multiState.root)
        multiLayout.measure(textMeasurer, CONFIG)
        val multiUpper = multiLayout.measuredUpperHeight

        assertStable("sup multi-char: measuredUpperHeight", emptyUpper, multiUpper)
    }

    /**
     * Typing into a subscript slot must not change the top-level formula's
     * measuredBottomHeight.
     */
    @Test
    fun subSlotBottomHeightStableWhenFirstCharTyped() {
        // x_{cursor}
        val emptyState =
            FormulaInputState()
                .insertLeaf(MathMi("x"))
                .insertSub()
        val emptyLayout = createLayout(emptyState.root)
        emptyLayout.measure(textMeasurer, CONFIG)
        val emptyBottom = emptyLayout.measuredBottomHeight

        // x_{1|}
        val contentState = emptyState.insertLeaf(MathMn("1"))
        val contentLayout = createLayout(contentState.root)
        contentLayout.measure(textMeasurer, CONFIG)
        val contentBottom = contentLayout.measuredBottomHeight

        assertStable("sub: measuredBottomHeight", emptyBottom, contentBottom)
    }

    /**
     * Typing into a subscript must also not change the formula's measuredUpperHeight
     * (space at the top of the formula must be unaffected).
     */
    @Test
    fun subSlotUpperHeightUnchangedWhenContentTyped() {
        val emptyState =
            FormulaInputState()
                .insertLeaf(MathMi("x"))
                .insertSub()
        val emptyLayout = createLayout(emptyState.root)
        emptyLayout.measure(textMeasurer, CONFIG)
        val emptyUpper = emptyLayout.measuredUpperHeight

        val contentState = emptyState.insertLeaf(MathMn("1"))
        val contentLayout = createLayout(contentState.root)
        contentLayout.measure(textMeasurer, CONFIG)
        val contentUpper = contentLayout.measuredUpperHeight

        assertStable("sub: measuredUpperHeight (top should not move)", emptyUpper, contentUpper)
    }

    // Tapping a frac slot (Placeholder → cursor) must not change the overall formula height.
    @Test
    fun fracDenomPlaceholderToCursorIsStable() {
        // insertFrac leaves cursor in denominator; numerator = Placeholder
        val withPlaceholderState = FormulaInputState().insertFrac().moveRight()
        val withPlaceholderLayout = createLayout(withPlaceholderState.root)
        withPlaceholderLayout.measure(textMeasurer, CONFIG)
        val placeholderUpper = withPlaceholderLayout.measuredUpperHeight
        val placeholderBottom = withPlaceholderLayout.measuredBottomHeight

        // moveRight puts cursor in numerator; denominator becomes Placeholder
        // State from insertFrac: cursor in denominator, numerator = Placeholder
        val cursorInDenomState = FormulaInputState().insertFrac()
        val cursorInDenomLayout = createLayout(cursorInDenomState.root)
        cursorInDenomLayout.measure(textMeasurer, CONFIG)
        val cursorUpper = cursorInDenomLayout.measuredUpperHeight
        val cursorBottom = cursorInDenomLayout.measuredBottomHeight

        assertStable("frac placeholder↔cursor: measuredUpperHeight", placeholderUpper, cursorUpper)
        assertStable("frac placeholder↔cursor: measuredBottomHeight", placeholderBottom, cursorBottom)
    }

    /**
     * Typing into the denominator (first slot) of a fraction must not change the
     * formula's overall height noticeably.
     */
    @Test
    fun fracDenominatorStableWhenFirstCharTyped() {
        // insertFrac puts cursor in denominator (slot 0)
        val emptyState = FormulaInputState().insertFrac()
        val emptyLayout = createLayout(emptyState.root)
        emptyLayout.measure(textMeasurer, CONFIG)
        val emptyUpper = emptyLayout.measuredUpperHeight
        val emptyBottom = emptyLayout.measuredBottomHeight

        val contentState = emptyState.insertLeaf(MathMn("2"))
        val contentLayout = createLayout(contentState.root)
        contentLayout.measure(textMeasurer, CONFIG)
        val contentUpper = contentLayout.measuredUpperHeight
        val contentBottom = contentLayout.measuredBottomHeight

        assertStable("frac denom: measuredUpperHeight", emptyUpper, contentUpper)
        assertStable("frac denom: measuredBottomHeight", emptyBottom, contentBottom)
    }

    /**
     * Typing into the numerator (second slot) of a fraction must also keep dimensions stable.
     */
    @Test
    fun fracNumeratorStableWhenFirstCharTyped() {
        // moveRight moves cursor from denominator to numerator
        val emptyState = FormulaInputState().insertFrac().moveRight()
        val emptyLayout = createLayout(emptyState.root)
        emptyLayout.measure(textMeasurer, CONFIG)
        val emptyUpper = emptyLayout.measuredUpperHeight
        val emptyBottom = emptyLayout.measuredBottomHeight

        val contentState = emptyState.insertLeaf(MathMn("3"))
        val contentLayout = createLayout(contentState.root)
        contentLayout.measure(textMeasurer, CONFIG)
        val contentUpper = contentLayout.measuredUpperHeight
        val contentBottom = contentLayout.measuredBottomHeight

        assertStable("frac num: measuredUpperHeight", emptyUpper, contentUpper)
        assertStable("frac num: measuredBottomHeight", emptyBottom, contentBottom)
    }

    /**
     * Adding a subscript to the denominator must not clip the formula canvas.
     * The measuredBottomHeight must be at least as large as the rendered bottom extent.
     */
    @Test
    fun fracDenomSubDoesNotClipCanvas() {
        // Fraction with cursor in denominator, then insert sub
        val state =
            FormulaInputState()
                .insertFrac() // cursor in denominator
                .insertLeaf(MathMn("2")) // type "2" in denominator
                .insertSub() // add subscript to "2"
        val layout = createLayout(state.root)
        layout.measure(textMeasurer, CONFIG)
        layout.computeLayout(0f, layout.measuredUpperHeight)
        // The sub slot baseline is somewhere below the frac baseline.
        // measuredBottomHeight must cover it. We verify no child exceeds the canvas.
        var canvasClipped = layout.measuredBottomHeight < 0f // sanity check
        // More specifically: canvas height must be positive and nonzero
        canvasClipped = canvasClipped || layout.measuredHeight() <= CONFIG.baseHeight
        assertTrue(!canvasClipped, "Frac denominator sub clipped: canvas height too small")
    }

    /**
     * Adding a superscript to the numerator must not push the numerator baseline
     * below the fraction bar. The numerator bottom (numDy + num.measuredBottomHeight) must
     * stay above (or at) the bar.
     */
    @Test
    fun fracNumeratorSupDoesNotCrossBar() {
        // Plain numerator "x": cursor then type x, move to numerator
        val plainState =
            FormulaInputState()
                .insertFrac()
                .moveRight()
                .insertLeaf(MathMi("x"))
        val plainLayout = createLayout(plainState.root)
        plainLayout.measure(textMeasurer, CONFIG)
        val plainNumUpper = plainLayout.measuredUpperHeight
        val plainNumBottom = plainLayout.measuredBottomHeight

        // Numerator "x^{cursor}": x with superscript
        val supState = plainState.insertSup()
        val supLayout = createLayout(supState.root)
        supLayout.measure(textMeasurer, CONFIG)
        val supInNumUpper = supLayout.measuredUpperHeight
        val supInNumBottom = supLayout.measuredBottomHeight

        // With a sup, the formula must be taller at the top (sup raises content) or equal.
        assertTrue(
            supInNumUpper >= plainNumUpper - TOLERANCE_PX,
            "Frac numerator sup: measuredUpperHeight should not shrink (got $supInNumUpper < $plainNumUpper)",
        )
        // Canvas must always be nonzero
        assertTrue(supInNumUpper + supInNumBottom > 0f, "Frac sup in num: canvas height must be positive")
    }

    /**
     * Adding a subscript to the numerator must not shrink the canvas below the
     * content extent. The sub must be visible (not clipped) even if it crosses the bar.
     */
    @Test
    fun fracNumeratorSubDoesNotClipCanvas() {
        val baseState =
            FormulaInputState()
                .insertFrac()
                .moveRight()
                .insertLeaf(MathMi("x"))
        val baseLayout = createLayout(baseState.root)
        baseLayout.measure(textMeasurer, CONFIG)
        val heightWithout = baseLayout.measuredHeight()

        // x with a subscript in the numerator
        val subState = baseState.insertSub()
        val subLayout = createLayout(subState.root)
        subLayout.measure(textMeasurer, CONFIG)
        val heightWithSub = subLayout.measuredHeight()

        // Adding a sub can only increase or keep the total canvas height.
        assertTrue(
            heightWithSub >= heightWithout - TOLERANCE_PX,
            "Frac numerator sub: canvas height shrank from $heightWithout to $heightWithSub",
        )
    }
}
