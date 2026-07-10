package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import com.monoxer.mathmlcompose.math.MathMmultiscriptsRegularized
import com.monoxer.mathmlcompose.math.MathMover
import com.monoxer.mathmlcompose.math.MathMunder
import com.monoxer.mathmlcompose.math.MathMunderover

internal class MathMoverLayout(
    val element: MathMover,
) : MathElementLayout() {
    private val main: MathElementLayout = createLayout(element.main)
    private val over: MathElementLayout = createLayout(element.over)
    private var _inkUpperHeight: Float = 0f
    override val inkUpperHeight: Float get() = _inkUpperHeight

    private var storedMainOffsetX: Float = 0f
    private var storedOverOffsetX: Float = 0f
    private var storedOverOffsetY: Float = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller(OVER_UNDER_SMALLER_LEVEL)
        main.measure(textMeasurer, config)
        over.measure(textMeasurer, smaller)

        measuredBottomHeight = main.measuredBottomHeight
        measuredUpperHeight = main.inkUpperHeight + config.baseVSpace + over.measuredUpperHeight
        measuredWidth = maxOf(main.measuredWidth, over.measuredWidth)
        _inkUpperHeight = main.inkUpperHeight + config.baseVSpace + over.inkUpperHeight

        storedMainOffsetX = (measuredWidth - main.measuredWidth) / 2f
        storedOverOffsetX = (measuredWidth - over.measuredWidth) / 2f
        storedOverOffsetY = -main.inkUpperHeight - config.baseVSpace
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(main, over)

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        main.computeLayout(x + storedMainOffsetX, y)
        over.computeLayout(x + storedOverOffsetX, y + storedOverOffsetY)
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller(OVER_UNDER_SMALLER_LEVEL)
        translate(storedMainOffsetX, 0f) {
            with(main) { draw(config) }
        }
        translate(storedOverOffsetX, storedOverOffsetY) {
            with(over) { draw(smaller) }
        }
    }
}

internal class MathMunderLayout(
    val element: MathMunder,
) : MathElementLayout() {
    private val main: MathElementLayout = createLayout(element.main)
    private val under: MathElementLayout = createLayout(element.under)

    private var storedMainOffsetX: Float = 0f
    private var storedUnderOffsetX: Float = 0f
    private var storedUnderOffsetY: Float = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller(OVER_UNDER_SMALLER_LEVEL)
        main.measure(textMeasurer, config)
        under.measure(textMeasurer, smaller)

        measuredUpperHeight = main.measuredUpperHeight
        measuredBottomHeight = under.inkUpperHeight + config.baseVSpace + under.measuredBottomHeight
        measuredWidth = maxOf(main.measuredWidth, under.measuredWidth)

        storedMainOffsetX = (measuredWidth - main.measuredWidth) / 2f
        storedUnderOffsetX = (measuredWidth - under.measuredWidth) / 2f
        storedUnderOffsetY = under.inkUpperHeight + config.baseVSpace
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(main, under)

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        main.computeLayout(x + storedMainOffsetX, y)
        under.computeLayout(x + storedUnderOffsetX, y + storedUnderOffsetY)
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller(OVER_UNDER_SMALLER_LEVEL)
        translate(storedMainOffsetX, 0f) {
            with(main) { draw(config) }
        }
        translate(storedUnderOffsetX, storedUnderOffsetY) {
            with(under) { draw(smaller) }
        }
    }
}

internal class MathMunderoverLayout(
    val element: MathMunderover,
) : MathElementLayout() {
    private val main: MathElementLayout = createLayout(element.main)
    private val under: MathElementLayout = createLayout(element.under)
    private val over: MathElementLayout = createLayout(element.over)
    private var _inkUpperHeight: Float = 0f
    override val inkUpperHeight: Float get() = _inkUpperHeight

    private var storedMainOffsetX: Float = 0f
    private var storedUnderOffsetX: Float = 0f
    private var storedUnderOffsetY: Float = 0f
    private var storedOverOffsetX: Float = 0f
    private var storedOverOffsetY: Float = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller(OVER_UNDER_SMALLER_LEVEL)
        main.measure(textMeasurer, config)
        under.measure(textMeasurer, smaller)
        over.measure(textMeasurer, smaller)

        measuredUpperHeight = main.inkUpperHeight + config.baseVSpace + over.measuredUpperHeight
        measuredBottomHeight = under.inkUpperHeight + config.baseVSpace + under.measuredBottomHeight
        measuredWidth = maxOf(main.measuredWidth, maxOf(under.measuredWidth, over.measuredWidth))
        _inkUpperHeight = main.inkUpperHeight + config.baseVSpace + over.inkUpperHeight

        storedMainOffsetX = (measuredWidth - main.measuredWidth) / 2f
        storedUnderOffsetX = (measuredWidth - under.measuredWidth) / 2f
        storedUnderOffsetY = under.inkUpperHeight + config.baseVSpace
        storedOverOffsetX = (measuredWidth - over.measuredWidth) / 2f
        storedOverOffsetY = -main.inkUpperHeight - config.baseVSpace
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(main, under, over)

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        main.computeLayout(x + storedMainOffsetX, y)
        under.computeLayout(x + storedUnderOffsetX, y + storedUnderOffsetY)
        over.computeLayout(x + storedOverOffsetX, y + storedOverOffsetY)
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller(OVER_UNDER_SMALLER_LEVEL)
        translate(storedMainOffsetX, 0f) {
            with(main) { draw(config) }
        }
        translate(storedUnderOffsetX, storedUnderOffsetY) {
            with(under) { draw(smaller) }
        }
        translate(storedOverOffsetX, storedOverOffsetY) {
            with(over) { draw(smaller) }
        }
    }
}

internal class MathMmultiscriptsLayout(
    val element: MathMmultiscriptsRegularized,
) : MathElementLayout() {
    private val base: MathElementLayout = createLayout(element.base)
    private val postSub: MathElementLayout = createLayout(element.postSubScripts)
    private val postSup: MathElementLayout = createLayout(element.postSupScripts)
    private val preSub: MathElementLayout = createLayout(element.preSubScripts)
    private val preSup: MathElementLayout = createLayout(element.preSupScripts)

    private var storedBaseOffsetX: Float = 0f
    private var storedPostSubOffsetX: Float = 0f
    private var storedPostSubOffsetY: Float = 0f
    private var storedPostSupOffsetX: Float = 0f
    private var storedPostSupOffsetY: Float = 0f
    private var storedPreSubOffsetX: Float = 0f
    private var storedPreSubOffsetY: Float = 0f
    private var storedPreSupOffsetX: Float = 0f
    private var storedPreSupOffsetY: Float = 0f

    private fun subHeight(): Float = maxOf(postSub.measuredHeight(), preSub.measuredHeight())

    private fun supHeight(config: MathCanvasConfig): Float =
        maxOf(
            (postSup.measuredHeight() - config.baseVSpace).coerceAtLeast(0f),
            (preSup.measuredHeight() - config.baseVSpace).coerceAtLeast(0f),
        )

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        base.measure(textMeasurer, config)
        postSub.measure(textMeasurer, smaller)
        postSup.measure(textMeasurer, smaller)
        preSub.measure(textMeasurer, smaller)
        preSup.measure(textMeasurer, smaller)

        val postWidth = maxOf(smaller.baseHeight, postSub.measuredWidth, postSup.measuredWidth)
        val preWidth = maxOf(smaller.baseHeight, preSub.measuredWidth, preSup.measuredWidth)
        measuredWidth = base.measuredWidth + postWidth + preWidth
        measuredUpperHeight = base.measuredUpperHeight + supHeight(config)
        measuredBottomHeight = base.measuredBottomHeight + subHeight()

        val offset = maxOf(preSub.measuredWidth, preSup.measuredWidth) - config.scale
        storedBaseOffsetX = offset
        storedPostSubOffsetX = base.measuredWidth - config.scale + offset
        storedPostSubOffsetY = postSub.inkUpperHeight
        storedPostSupOffsetX = base.measuredWidth - config.scale + offset
        storedPostSupOffsetY = -base.inkUpperHeight + config.baseVSpace
        storedPreSubOffsetX = -preSub.measuredWidth + config.scale + offset
        storedPreSubOffsetY = preSub.inkUpperHeight
        storedPreSupOffsetX = -preSup.measuredWidth + config.scale + offset
        storedPreSupOffsetY = -base.inkUpperHeight + config.baseVSpace
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(base, postSub, postSup, preSub, preSup)

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        base.computeLayout(x + storedBaseOffsetX, y)
        postSub.computeLayout(x + storedPostSubOffsetX, y + storedPostSubOffsetY)
        postSup.computeLayout(x + storedPostSupOffsetX, y + storedPostSupOffsetY)
        preSub.computeLayout(x + storedPreSubOffsetX, y + storedPreSubOffsetY)
        preSup.computeLayout(x + storedPreSupOffsetX, y + storedPreSupOffsetY)
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        translate(storedBaseOffsetX, 0f) {
            with(base) { draw(config) }
        }
        translate(storedPostSubOffsetX, storedPostSubOffsetY) {
            with(postSub) { draw(smaller) }
        }
        translate(storedPostSupOffsetX, storedPostSupOffsetY) {
            with(postSup) { draw(smaller) }
        }
        translate(storedPreSubOffsetX, storedPreSubOffsetY) {
            with(preSub) { draw(smaller) }
        }
        translate(storedPreSupOffsetX, storedPreSupOffsetY) {
            with(preSup) { draw(smaller) }
        }
    }
}
