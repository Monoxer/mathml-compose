package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathMsqrt
import com.monoxer.mathmlcompose.render.compose.HitResult
import com.monoxer.mathmlcompose.testsupport.TEST_COLORS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [MathElementLayout.hitTest]. The TextMeasurer used by the MathMrow child-path
 * propagation tests is constructed directly (outside of composition) so these tests run as
 * plain synchronous code rather than depending on Compose's composition/recomposition machinery.
 */
class MathHitTestTest {
    private val config =
        MathCanvasConfig(
            scale = 1f,
            baseHeight = 40f,
            textColor = Color.Black,
            colors = TEST_COLORS,
        )
    private val textMeasurer =
        TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(),
            defaultDensity = Density(density = 1f),
            defaultLayoutDirection = LayoutDirection.Ltr,
        )

    // ── helpers ──────────────────────────────────────────────────────────────────

    /** Positions a layout at (x, y) with the given pixel dimensions. */
    private fun MathElementLayout.place(
        x: Float,
        y: Float,
        width: Float,
        upper: Float,
        bottom: Float,
    ) {
        measuredWidth = width
        measuredUpperHeight = upper
        measuredBottomHeight = bottom
        computeLayout(x, y)
    }

    // ── MathLeafLayout ────────────────────────────────────────────────────────────

    @Test
    fun leaf_tapLeftHalf_returnsOnRightFalse() {
        val layout = createLayout(MathMi("x"))
        layout.place(x = 10f, y = 50f, width = 30f, upper = 20f, bottom = 10f)
        // center at x = 10 + 15 = 25; tap at 15 < 25 → left half
        assertEquals(HitResult(emptyList(), false), layout.hitTest(15f, 50f))
    }

    @Test
    fun leaf_tapRightHalf_returnsOnRightTrue() {
        val layout = createLayout(MathMi("x"))
        layout.place(x = 10f, y = 50f, width = 30f, upper = 20f, bottom = 10f)
        // center at x = 25; tap at 30 > 25 → right half
        assertEquals(HitResult(emptyList(), true), layout.hitTest(30f, 50f))
    }

    @Test
    fun leaf_outsideBounds_returnsNull() {
        // layout bounds: x ∈ [10, 30], y ∈ [35, 55]
        val layout = createLayout(MathMn("1"))
        layout.place(x = 10f, y = 50f, width = 20f, upper = 15f, bottom = 5f)
        listOf(
            9f to 50f, // left of x-range
            31f to 50f, // right of x-range
            15f to 34f, // above y-range  (top = layoutY - upper = 35)
            15f to 56f, // below y-range  (bottom = layoutY + bottom = 55)
        ).forEach { (x, y) ->
            assertNull(layout.hitTest(x, y), "Expected null for tap at ($x, $y)")
        }
    }

    // ── MathMsqrtLayout ───────────────────────────────────────────────────────────

    /**
     * Tapping within sqrt bounds on the radical-symbol area (where main content
     * has zero dimensions, i.e. is not hit) must fall back to the start of the
     * sqrt content rather than returning null.
     */
    @Test
    fun sqrt_tapOnRadicalSymbolArea_returnsFallbackToContentStart() {
        val layout = createLayout(MathMsqrt(MathMrow(emptyList())))
        // Place sqrt; main child has zero dimensions after computeLayout (not measured).
        layout.place(x = 0f, y = 40f, width = 60f, upper = 40f, bottom = 20f)
        // Tap at (5, 40) — within sqrt bounds, but main is zero-size → fallback.
        assertEquals(HitResult(listOf(0), false), layout.hitTest(5f, 40f))
    }

    @Test
    fun sqrt_outsideBounds_returnsNull() {
        // layout bounds: x ∈ [10, 70], y ∈ [0, 60]
        val layout = createLayout(MathMsqrt(MathMrow(emptyList())))
        layout.place(x = 10f, y = 40f, width = 60f, upper = 40f, bottom = 20f)
        listOf(
            9f to 40f, // left of x-range
            71f to 40f, // right of x-range
            30f to -1f, // above y-range  (top = layoutY - upper = 0)
            30f to 61f, // below y-range  (bottom = layoutY + bottom = 60)
        ).forEach { (x, y) ->
            assertNull(layout.hitTest(x, y), "Expected null for tap at ($x, $y)")
        }
    }

    // ── MathMrow child-path propagation (needs real TextMeasurer) ──────────────────

    /**
     * Tapping on the first character of a two-element row must return path=[0]
     * (first non-cursor child) with correct onRight based on tap position.
     */
    @Test
    fun mrow_tapOnFirstLeaf_returnsPathZero() {
        // Two-element row without cursor so both are hittable.
        val layout = createLayout(MathMrow(listOf(MathMi("x"), MathMn("1"))))
        layout.measure(textMeasurer, config)
        layout.computeLayout(0f, layout.measuredUpperHeight)
        // Tap the left edge of the row — should land in first element ("x").
        val hit = layout.hitTest(1f, layout.measuredUpperHeight)
        assertEquals(0, hit?.path?.firstOrNull(), "Expected path[0] for tap on first element")
    }

    /**
     * Tapping in the horizontal padding zone to the RIGHT of a fraction must return null from
     * the frac itself so the parent MathMrow places the cursor after it instead of entering it.
     */
    @Test
    fun frac_tapRightPadding_returnsNull() {
        val layout = createLayout(MathMfrac(numerator = MathMn("3"), denominator = MathMn("4")))
        layout.measure(textMeasurer, config)
        layout.computeLayout(0f, layout.measuredUpperHeight)
        // Tap just inside the right edge (padding zone, past the denominator content).
        val rightEdge = layout.measuredWidth - 1f
        val hit = layout.hitTest(rightEdge, layout.measuredUpperHeight)
        assertNull(hit, "Tap in right padding zone should return null so parent handles it")
    }

    /**
     * Tapping on the content area of the numerator must enter it (path starts with 1 for num).
     */
    @Test
    fun frac_tapNumeratorContent_returnsNumeratorPath() {
        val layout = createLayout(MathMfrac(numerator = MathMn("3"), denominator = MathMn("4")))
        layout.measure(textMeasurer, config)
        layout.computeLayout(0f, layout.measuredUpperHeight)
        // Tap the center of the frac horizontally, above the bar → numerator area.
        val cx = layout.measuredWidth / 2f
        val hit = layout.hitTest(cx, 1f) // y=1 is near the top → numerator
        val path = hit?.path
        assertNotNull(path, "Expected a hit on numerator content")
        assertEquals(1, path.first(), "Expected path to start with 1 (numerator)")
    }

    /**
     * Tapping in the gap between a fraction and the next leaf in a row must place the cursor
     * after the fraction (not inside it).
     */
    @Test
    fun mrow_tapGapAfterFrac_placesCursorAfterFrac() {
        // Row: [frac(3/4), "2"]  — no cursor so both are hittable at idx 0 and 1
        val layout =
            createLayout(
                MathMrow(
                    listOf(
                        MathMfrac(numerator = MathMn("3"), denominator = MathMn("4")),
                        MathMn("2"),
                    ),
                ),
            )
        layout.measure(textMeasurer, config)
        layout.computeLayout(0f, layout.measuredUpperHeight)
        // Find the frac child layout to get its right edge.
        val fracLayout = layout.layoutChildren().first()
        // Tap just to the right of the frac's right edge → gap between frac and "2".
        val gapX = fracLayout.layoutX + fracLayout.measuredWidth + 1f
        val hit = layout.hitTest(gapX, layout.measuredUpperHeight)
        // Should be path=[0] onRight=true (after frac, index 0 in row) OR path=[1] onRight=false
        // (before "2", index 1 in row) — both mean cursor lands between frac and "2".
        val path = hit?.path
        assertNotNull(path, "Expected a hit in gap zone")
        val idx = path.first()
        val onRight = hit.onRight
        val cursorAfterFrac = (idx == 0 && onRight) || (idx == 1 && !onRight)
        assertEquals(true, cursorAfterFrac, "Expected cursor between frac and '2', got path=$path onRight=$onRight")
    }

    /**
     * Tapping outside the row bounds returns null.
     */
    @Test
    fun mrow_outsideBounds_returnsNull() {
        val layout = createLayout(MathMrow(listOf(MathMi("x"))))
        layout.measure(textMeasurer, config)
        layout.computeLayout(0f, layout.measuredUpperHeight)
        val hit = layout.hitTest(-1f, layout.measuredUpperHeight)
        assertNull(hit)
    }
}
