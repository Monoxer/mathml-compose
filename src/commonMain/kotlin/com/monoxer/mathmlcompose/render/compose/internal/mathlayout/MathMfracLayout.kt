package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.render.compose.HitResult

internal class MathMfracLayout(
    val element: MathMfrac,
) : MathElementLayout() {
    private val num: MathElementLayout = createLayout(element.numerator)
    private val den: MathElementLayout = createLayout(element.denominator)

    // Child offsets from this element's baseline, stored during measure for computeLayout
    private var numDx = 0f
    private var numDy = 0f
    private var denDx = 0f
    private var denDy = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller()
        num.measure(textMeasurer, smaller)
        den.measure(textMeasurer, smaller)

        val margin = config.baseVSpace / 2f
        val drawMargin = config.baseVSpace
        val barY = config.baseHeight / GEOMETRY_THIRD
        measuredWidth = maxOf(den.measuredWidth, num.measuredWidth) + config.baseHSpace * FRAC_HSPACE_FACTOR

        // numDy: anchor numerator top for stability (top-anchored), but also prevent the numerator
        // content from crossing the bar (bottom-clamped). Use whichever places the baseline higher.
        // - Top-anchored: top of content at -(barY + drawMargin + smaller.bH) from frac baseline.
        //   Stable for cursor↔leaf transitions because smaller.bH is fixed and num.mUH is stable.
        // - Bottom-clamped: numerator bottom stays drawMargin above the bar.
        //   Activates when the numerator has a sub or other extra downward content.
        val numDyTopAnchored = -barY - drawMargin - smaller.baseHeight + num.measuredUpperHeight
        val numDyBottomClamped = -barY - drawMargin - num.measuredBottomHeight
        numDy = minOf(numDyTopAnchored, numDyBottomClamped)

        // denDy: denominator top anchored drawMargin below the bar.
        denDy = den.measuredUpperHeight + margin + drawMargin - barY

        numDx = (measuredWidth - num.measuredWidth) / 2f
        denDx = (measuredWidth - den.measuredWidth) / 2f

        // measuredUpperHeight: enough to contain the numerator top (+ margin).
        // For normal content (cursor/leaf) numDy = topAnchored → numTopExtent = barY + drawMargin +
        // smaller.bH (fixed). For bottom-clamped content the height grows to accommodate extra extent.
        val numTopExtent = -numDy + num.measuredUpperHeight
        measuredUpperHeight = numTopExtent + margin

        // measuredBottomHeight: accommodate denominator content (including subs) and any numerator
        // content that overflows below the frac baseline (e.g. a sub in the numerator).
        val denBottomExtent = denDy + den.measuredBottomHeight
        val numBottomOverflow = maxOf(0f, numDy + num.measuredBottomHeight)
        measuredBottomHeight =
            maxOf(
                margin + smaller.baseHeight - barY, // minimum for an empty denominator slot
                denBottomExtent + margin, // actual denominator extent
                numBottomOverflow, // numerator sub that overflows below frac baseline
            )
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller()
        val barY = config.baseHeight / GEOMETRY_THIRD

        // Draw fraction bar
        drawLine(
            color = config.textColor,
            start = Offset(config.baseHSpace, -barY),
            end = Offset(measuredWidth - config.baseHSpace, -barY),
            strokeWidth = maxOf(config.scale, 1f),
        )

        // Draw numerator
        translate(numDx, numDy) {
            with(num) { draw(smaller) }
        }

        // Draw denominator
        translate(denDx, denDy) {
            with(den) { draw(smaller) }
        }
    }

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        num.computeLayout(x + numDx, y + numDy)
        den.computeLayout(x + denDx, y + denDy)
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(num, den)

    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? = if (isOutsideHitBounds(x, y)) null else hitTestContent(x, y)

    private fun isOutsideHitBounds(
        x: Float,
        y: Float,
    ): Boolean {
        // Taps in the horizontal padding zones (outside both num/den content) should not
        // be claimed by the frac — let the parent MathMrow handle them as "before/after frac".
        val contentLeft = layoutX + minOf(numDx, denDx)
        val contentRight = layoutX + maxOf(numDx + num.measuredWidth, denDx + den.measuredWidth)
        return x < layoutX || x > layoutX + measuredWidth ||
            y < layoutY - measuredUpperHeight || y > layoutY + measuredBottomHeight ||
            x !in contentLeft..contentRight
    }

    private fun hitTestContent(
        x: Float,
        y: Float,
    ): HitResult {
        // childrenOf(MathMfrac) = [denominator(0), numerator(1)]
        return den.hitTest(x, y)?.let { HitResult(listOf(0) + it.path, it.onRight) }
            ?: num.hitTest(x, y)?.let { HitResult(listOf(1) + it.path, it.onRight) }
            ?: nearestSlotByY(y)
    }

    /** Fallback for the fraction bar area: returns the slot whose baseline is nearest to [y]. */
    private fun nearestSlotByY(y: Float): HitResult {
        val numBaselineY = layoutY + numDy
        val denBaselineY = layoutY + denDy
        return if (kotlin.math.abs(y - numBaselineY) <= kotlin.math.abs(y - denBaselineY)) {
            HitResult(listOf(1), true)
        } else {
            HitResult(listOf(0), true)
        }
    }
}
