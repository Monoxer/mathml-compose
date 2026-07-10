package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import com.monoxer.mathmlcompose.math.MathMroot
import com.monoxer.mathmlcompose.math.MathMsqrt
import com.monoxer.mathmlcompose.render.compose.HitResult

internal class MathMsqrtLayout(
    val element: MathMsqrt,
) : MathElementLayout() {
    private val main: MathElementLayout = createLayout(element.main)
    private var rootWidth: Float = 0f
    private var mainDx: Float = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller()
        main.measure(textMeasurer, smaller)

        measuredUpperHeight = main.inkUpperHeight + config.baseVSpace * 2f
        measuredBottomHeight = main.measuredBottomHeight
        rootWidth = config.baseHeight / 2f
        measuredWidth = main.measuredWidth + rootWidth
        mainDx = measuredWidth - main.measuredWidth
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller()

        // Draw sqrt symbol path
        val x0 = 0f
        val y0 = measuredBottomHeight - config.baseVSpace
        val x1 = rootWidth / GEOMETRY_THIRD
        val y1 = measuredBottomHeight - config.baseVSpace * 2
        val x2 = rootWidth / GEOMETRY_THIRD * 2
        val y2 = measuredBottomHeight
        val x3 = rootWidth
        val y3 = -main.inkUpperHeight - config.baseVSpace * SQRT_BAR_VSPACE_FACTOR
        val x4 = measuredWidth

        val path = Path()
        path.moveTo(x0, y0)
        path.lineTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.lineTo(x4, y3)
        drawPath(path, config.textColor, style = Stroke(width = config.scale))

        // Draw main content
        translate(mainDx, 0f) {
            with(main) { draw(smaller) }
        }
    }

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        // childrenOf(MathMsqrt) = [main(0)]; main baseline same as sqrt baseline
        main.computeLayout(x + mainDx, y)
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(main)

    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? {
        val outOfBounds =
            x < layoutX || x > layoutX + measuredWidth ||
                y < layoutY - measuredUpperHeight || y > layoutY + measuredBottomHeight
        if (outOfBounds) return null
        return main.hitTest(x, y)?.let { HitResult(listOf(0) + it.path, it.onRight) }
            ?: HitResult(listOf(0), false) // fallback: cursor at start of sqrt content
    }
}

internal class MathMrootLayout(
    val element: MathMroot,
) : MathElementLayout() {
    private val main: MathElementLayout = createLayout(element.main)
    private val root: MathElementLayout = createLayout(element.root)
    private var rootHOffset: Float = 0f
    private var storedMainOffsetX: Float = 0f
    private var storedRootOffsetY: Float = 0f

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val smaller = config.getSmaller()
        val tiny = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        main.measure(textMeasurer, smaller)
        root.measure(textMeasurer, tiny)

        val rootIndexHeight = minOf(config.baseHeight, main.measuredHeight() / GEOMETRY_THIRD)
        measuredUpperHeight =
            maxOf(
                main.measuredUpperHeight + config.baseVSpace * 2f,
                root.measuredHeight() + config.baseVSpace + rootIndexHeight - main.measuredBottomHeight,
            )
        measuredBottomHeight = main.measuredBottomHeight
        rootHOffset = maxOf(root.measuredWidth, config.baseHSpace + config.baseHeight / GEOMETRY_THIRD)
        measuredWidth = rootHOffset + config.baseHeight / GEOMETRY_THIRD + main.measuredWidth

        storedMainOffsetX = measuredWidth - main.measuredWidth
        storedRootOffsetY = measuredBottomHeight - root.measuredBottomHeight - config.baseVSpace - rootIndexHeight
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(main, root)

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        main.computeLayout(x + storedMainOffsetX, y)
        root.computeLayout(x, y + storedRootOffsetY)
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val smaller = config.getSmaller()
        val tiny = config.getSmaller(MODIFIER_SMALLER_LEVEL)
        val rootIndexHeight = minOf(config.baseHeight, measuredHeight() / GEOMETRY_THIRD)

        // Draw root index (e.g. "3" in ³√)
        val dyRoot = measuredBottomHeight - root.measuredBottomHeight - config.baseVSpace - rootIndexHeight
        translate(0f, dyRoot) {
            with(root) { draw(tiny) }
        }

        // Draw radical symbol path
        val x0 = rootHOffset - config.baseHSpace - config.baseHeight / GEOMETRY_THIRD
        val y0 = measuredBottomHeight - rootIndexHeight + config.baseVSpace
        val x1 = rootHOffset - config.baseHeight / GEOMETRY_THIRD
        val y1 = measuredBottomHeight - rootIndexHeight
        val x2 = rootHOffset
        val y2 = measuredBottomHeight
        val x3 = rootHOffset + config.baseHeight / GEOMETRY_THIRD
        val y3 = -main.measuredUpperHeight - config.baseVSpace
        val x4 = measuredWidth

        val path = Path()
        path.moveTo(x0, y0)
        path.lineTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.lineTo(x4, y3)
        drawPath(path, config.textColor, style = Stroke(width = config.scale))

        // Draw main content
        val dx = measuredWidth - main.measuredWidth
        translate(dx, 0f) {
            with(main) { draw(smaller) }
        }
    }
}
