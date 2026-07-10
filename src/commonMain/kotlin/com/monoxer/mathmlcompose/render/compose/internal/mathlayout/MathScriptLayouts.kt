package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import com.monoxer.mathmlcompose.math.MathMsub
import com.monoxer.mathmlcompose.math.MathMsubsup
import com.monoxer.mathmlcompose.math.MathMsup
import com.monoxer.mathmlcompose.render.compose.HitResult

internal class MathMsupLayout(
    val element: MathMsup,
) : MathElementLayout() {
    private val main: MathElementLayout = createLayout(element.main)
    private val sup: MathElementLayout = createLayout(element.sup)
    private var _inkUpperHeight: Float = 0f
    override val inkUpperHeight: Float get() = _inkUpperHeight

    private var supDx = 0f
    private var supDy = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        main.measure(textMeasurer, config)
        sup.measure(textMeasurer, smaller)

        supDx = main.measuredWidth - config.scale
        // When the sup slot contains a subscript (e.g. x^{a_b}), its measuredBottomHeight exceeds
        // its inkUpperHeight. Raise the sup by that excess to keep visual symmetry with how
        // x_{y^z} pushes the sub lower (subDy grows when the sub slot has larger inkUpperHeight).
        val supExtraUp = maxOf(0f, sup.measuredBottomHeight - sup.inkUpperHeight)
        supDy = -main.inkUpperHeight + config.baseVSpace - supExtraUp

        // Account for the full rendered bottom extent of the sup slot.
        val supBottomExtent = supDy + sup.measuredBottomHeight
        measuredBottomHeight = maxOf(main.measuredBottomHeight, supBottomExtent)
        // sup baseline is at supDy above main baseline; express upper height via supDy so it
        // stays correct regardless of the supDy formula.
        measuredUpperHeight = maxOf(main.measuredUpperHeight, -supDy + sup.measuredUpperHeight)
        measuredWidth = main.measuredWidth + sup.measuredWidth - config.scale
        // Tight ink upper bound: distance from main baseline to the top of sup's ink.
        _inkUpperHeight = -supDy + sup.inkUpperHeight
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        with(main) { draw(config) }
        translate(supDx, supDy) {
            with(sup) { draw(smaller) }
        }
    }

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        main.computeLayout(x, y)
        sup.computeLayout(x + supDx, y + supDy)
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(main, sup)

    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? {
        val outOfBounds =
            x < layoutX || x > layoutX + measuredWidth ||
                y < layoutY - measuredUpperHeight || y > layoutY + measuredBottomHeight
        if (outOfBounds) return null
        // childrenOf(MathMsup) = [main(0), sup(1)]
        return main.hitTest(x, y)?.let { HitResult(listOf(0) + it.path, it.onRight) }
            ?: sup.hitTest(x, y)?.let { HitResult(listOf(1) + it.path, it.onRight) }
            ?: HitResult(listOf(0), true)
    }
}

internal class MathMsubLayout(
    val element: MathMsub,
) : MathElementLayout() {
    private val main: MathElementLayout = createLayout(element.main)
    private val sub: MathElementLayout = createLayout(element.sub)

    // Use main's tight ink bound (not measuredUpperHeight) so that when Placeholder is the
    // main element its inkUpperHeight (~0.73*bH+vspace after the font-metric fix) is used
    // instead of its visual measuredUpperHeight (0.9*bH). This propagates correctly up the
    // tree and prevents cascading subs from drifting too far apart.
    override val inkUpperHeight: Float get() = main.inkUpperHeight

    private var subDx = 0f
    private var subDy = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        main.measure(textMeasurer, config)
        sub.measure(textMeasurer, smaller)

        subDx = main.measuredWidth - config.scale
        subDy = sub.inkUpperHeight

        measuredUpperHeight = main.measuredUpperHeight
        // Use the actual rendered extent (subDy + sub.measuredBottomHeight) so the canvas never
        // clips nested sub content (e.g. 1_{2_{3}}). This is stable because inkUpperHeight for
        // both the empty cursor slot and a leaf uses firstBaseline + vspace (same font metric).
        measuredBottomHeight = maxOf(main.measuredBottomHeight, subDy + sub.measuredBottomHeight)
        measuredWidth = main.measuredWidth + sub.measuredWidth - config.scale
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        with(main) { draw(config) }
        translate(subDx, subDy) {
            with(sub) { draw(smaller) }
        }
    }

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        main.computeLayout(x, y)
        sub.computeLayout(x + subDx, y + subDy)
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(main, sub)

    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? {
        val outOfBounds =
            x < layoutX || x > layoutX + measuredWidth ||
                y < layoutY - measuredUpperHeight || y > layoutY + measuredBottomHeight
        if (outOfBounds) return null
        // childrenOf(MathMsub) = [main(0), sub(1)]
        return main.hitTest(x, y)?.let { HitResult(listOf(0) + it.path, it.onRight) }
            ?: sub.hitTest(x, y)?.let { HitResult(listOf(1) + it.path, it.onRight) }
            ?: HitResult(listOf(0), true)
    }
}

internal class MathMsubsupLayout(
    val element: MathMsubsup,
) : MathElementLayout() {
    private val main: MathElementLayout = createLayout(element.main)
    private val sub: MathElementLayout = createLayout(element.sub)
    private val sup: MathElementLayout = createLayout(element.sup)

    private var _inkUpperHeight: Float = 0f
    override val inkUpperHeight: Float get() = _inkUpperHeight

    private var storedScriptsOriginX: Float = 0f
    private var storedSubY: Float = 0f
    private var storedSupY: Float = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        main.measure(textMeasurer, config)
        sub.measure(textMeasurer, smaller)
        sup.measure(textMeasurer, smaller)

        storedScriptsOriginX = main.measuredWidth - config.scale
        storedSubY = sub.inkUpperHeight
        storedSupY = -main.inkUpperHeight + config.baseVSpace

        val subExtent = storedSubY + sub.measuredBottomHeight
        val supBottomExtent = storedSupY + sup.measuredBottomHeight
        measuredBottomHeight = maxOf(main.measuredBottomHeight, subExtent, supBottomExtent)
        measuredUpperHeight =
            maxOf(
                main.measuredUpperHeight,
                main.inkUpperHeight - config.baseVSpace + sup.measuredUpperHeight,
            )
        measuredWidth = main.measuredWidth + maxOf(sub.measuredWidth, sup.measuredWidth) - config.scale
        _inkUpperHeight = main.inkUpperHeight - config.baseVSpace + sup.inkUpperHeight
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(main, sub, sup)

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        main.computeLayout(x, y)
        sub.computeLayout(x + storedScriptsOriginX, y + storedSubY)
        sup.computeLayout(x + storedScriptsOriginX, y + storedSupY)
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        with(main) { draw(config) }
        translate(storedScriptsOriginX, storedSubY) {
            with(sub) { draw(smaller) }
        }
        translate(storedScriptsOriginX, storedSupY) {
            with(sup) { draw(smaller) }
        }
    }
}
