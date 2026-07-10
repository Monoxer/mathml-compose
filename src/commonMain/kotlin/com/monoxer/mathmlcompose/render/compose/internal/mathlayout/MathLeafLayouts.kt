package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.monoxer.mathmlcompose.math.MathFilledSlot
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMo
import com.monoxer.mathmlcompose.math.MathMspace
import com.monoxer.mathmlcompose.math.MathMtext
import com.monoxer.mathmlcompose.render.compose.HitResult

@Suppress("MagicNumber")
internal abstract class MathLeafLayout : MathElementLayout() {
    protected var textLayoutResult: TextLayoutResult? = null
    protected var margin: Float = 0f

    /** Subclasses can disable minWidth padding to prevent margin inflation when nodes are split. */
    protected open val enforceMinWidth: Boolean = true

    private var _inkUpperHeight: Float = 0f
    override val inkUpperHeight: Float get() = _inkUpperHeight

    abstract fun getText(): String

    open fun getFontStyle(): FontStyle = FontStyle.Normal

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        val text = getText()
        val fontSizeSp = (config.baseHeight / config.scale).sp
        val style = TextStyle(fontSize = fontSizeSp, fontStyle = getFontStyle(), fontFamily = config.fontFamily)

        if (text.isEmpty()) {
            val refResult = textMeasurer.measure("M", TextStyle(fontSize = fontSizeSp, fontFamily = config.fontFamily))
            textLayoutResult = null
            measuredUpperHeight = refResult.firstBaseline + config.baseVSpace
            measuredBottomHeight = maxOf(refResult.size.height - refResult.firstBaseline, 0f) + config.baseVSpace
            measuredWidth = config.baseHeight * 0.7f + margin * 2
            _inkUpperHeight = measuredUpperHeight
            return
        }

        val result = textMeasurer.measure(AnnotatedString(text), style)
        textLayoutResult = result
        val topToBaseline = result.firstBaseline
        measuredUpperHeight = topToBaseline + config.baseVSpace
        measuredBottomHeight = maxOf(result.size.height.toFloat() - topToBaseline, 0f) + config.baseVSpace

        val w = result.size.width.toFloat()
        if (enforceMinWidth) {
            val minWidth = config.baseHeight * 0.7f
            if (w + margin * 2 < minWidth) {
                margin = (minWidth - w) / 2f
            }
        }
        measuredWidth = w + margin * 2

        _inkUpperHeight = config.baseHeight * INK_UPPER_HEIGHT_RATIO + config.baseVSpace
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val result = textLayoutResult ?: return
        drawText(
            textLayoutResult = result,
            color = config.textColor,
            topLeft = Offset(margin, -result.firstBaseline),
        )
    }

    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? {
        val outOfBounds =
            x < layoutX || x > layoutX + measuredWidth ||
                y < layoutY - measuredUpperHeight || y > layoutY + measuredBottomHeight
        if (outOfBounds) return null
        return HitResult(emptyList(), x > layoutX + measuredWidth / 2f)
    }
}

internal class MathMnLayout(
    val element: MathMn,
) : MathLeafLayout() {
    override fun getText(): String = element.text

    // Disable minWidth so adjacent digits render compactly without extra padding between them.
    override val enforceMinWidth: Boolean = false

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        // margin = 0: adjacent operators (MathMo) already provide wideHMargin spacing.
        margin = 0f
        super.measure(textMeasurer, config)
    }
}

internal class MathMoLayout(
    val element: MathMo,
) : MathLeafLayout() {
    override fun getText(): String = element.text

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        margin = config.wideHMargin
        super.measure(textMeasurer, config)
    }
}

internal class MathMiLayout(
    val element: MathMi,
) : MathLeafLayout() {
    override fun getText(): String = element.text

    override fun getFontStyle(): FontStyle = FontStyle.Italic

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        // margin = 0: adjacent operators (MathMo) already provide wideHMargin spacing.
        margin = 0f
        super.measure(textMeasurer, config)
    }
}

internal class MathMtextLayout(
    val element: MathMtext,
) : MathLeafLayout() {
    override fun getText(): String = element.text

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        margin = config.wideHMargin
        super.measure(textMeasurer, config)
    }
}

internal class MathPlaceholderLayout : MathElementLayout() {
    // Visual box extent stored during measure for draw() and hitTest().
    // The dashed square uses the same proportions as MathCursorLayout (0.9/0.1 split),
    // independent of font metrics so it always looks like a square slot.
    private var visualUpperHeight: Float = 0f
    private var visualLowerHeight: Float = 0f

    // Tight ink upper bound for SCRIPT POSITIONING (subDy/supDy).
    // Uses the same 0.70*bH formula as MathLeafLayout so that script distance is consistent
    // whether a slot holds a leaf or a cursor/placeholder.
    private var _inkUpperHeight: Float = 0f
    override val inkUpperHeight: Float get() = _inkUpperHeight

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        // Use actual font firstBaseline for layout metrics so the Placeholder matches the
        // empty cursor slot exactly. This makes the frac/sub/sup positioning stable when the
        // user taps a placeholder to enter it (Placeholder ↔ cursor slot transition).
        val fontSizeSp = (config.baseHeight / config.scale).sp
        val refResult = textMeasurer.measure("1", TextStyle(fontSize = fontSizeSp, fontFamily = config.fontFamily))
        measuredUpperHeight = refResult.firstBaseline + config.baseVSpace
        measuredBottomHeight = (refResult.size.height.toFloat() - refResult.firstBaseline) + config.baseVSpace
        measuredWidth = config.baseHeight
        // Visual square proportions (unchanged from before — user sees a square dashed box).
        visualUpperHeight = config.baseHeight * SLOT_UPPER_HEIGHT_RATIO
        visualLowerHeight = config.baseHeight - visualUpperHeight
        // Positioning bound: matches leaf inkUpperHeight formula so script distance is stable.
        _inkUpperHeight = config.baseHeight * INK_UPPER_HEIGHT_RATIO + config.baseVSpace
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        drawRect(
            color = config.textColor.copy(alpha = 0.4f),
            // Draw the visual square at the cursor-height position, not at -measuredUpperHeight,
            // so the dashed box stays aligned with cursor lines in surrounding slots.
            topLeft = Offset(0f, -visualUpperHeight),
            size = Size(measuredWidth, visualUpperHeight + visualLowerHeight),
            style =
                Stroke(
                    width = config.scale,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(
                                SLOT_DASH_LENGTH * config.scale,
                                SLOT_DASH_GAP * config.scale,
                            ),
                            0f,
                        ),
                ),
        )
    }

    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? {
        // Use visual bounds so the full dashed-box area is tappable.
        val outOfBounds =
            x < layoutX || x > layoutX + measuredWidth ||
                y < layoutY - visualUpperHeight || y > layoutY + visualLowerHeight
        if (outOfBounds) return null
        return HitResult(emptyList(), x > layoutX + measuredWidth / 2f)
    }
}

/** Renders the active cursor as a thin vertical caret line (blinking-style insertion point). */
internal class MathCursorLayout : MathElementLayout() {
    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        // Zero dimensions: cursor is an overlay and must not affect layout or spacing.
        measuredWidth = 0f
        measuredUpperHeight = 0f
        measuredBottomHeight = 0f
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        // Use config.baseHeight so the cursor height matches the current context
        // (automatically smaller inside sub/superscripts).
        val upper = config.baseHeight * SLOT_UPPER_HEIGHT_RATIO
        val bottom = config.baseHeight - upper
        drawRect(
            color = config.colors.accentColor.copy(alpha = config.cursorAlpha),
            topLeft = Offset(0f, -upper),
            size = Size(config.scale, upper + bottom),
        )
    }

    // Cursor itself is not a tap target
    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? = null
}

/**
 * Renders a user-filled hint slot: draws the child element and a dashed border box
 * around it so the filled slot remains visually distinct from the fixed formula parts.
 */
internal class MathFilledSlotLayout(
    element: MathFilledSlot,
) : MathElementLayout() {
    private val child = createLayout(element.child)

    // Visual box extents stored at measure time, matching MathPlaceholder proportions.
    private var visualUpperHeight: Float = 0f
    private var visualLowerHeight: Float = 0f

    override val inkUpperHeight: Float get() = child.inkUpperHeight

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        child.measure(textMeasurer, config)
        measuredUpperHeight = child.measuredUpperHeight
        measuredBottomHeight = child.measuredBottomHeight
        measuredWidth = child.measuredWidth
        visualUpperHeight = config.baseHeight * SLOT_UPPER_HEIGHT_RATIO
        visualLowerHeight = config.baseHeight - visualUpperHeight
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        with(child) { draw(config) }
        // Dashed border.
        drawRect(
            color = config.textColor.copy(alpha = 0.4f),
            topLeft = Offset(0f, -visualUpperHeight),
            size = Size(measuredWidth, visualUpperHeight + visualLowerHeight),
            style =
                Stroke(
                    width = config.scale,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(SLOT_DASH_LENGTH * config.scale, SLOT_DASH_GAP * config.scale),
                            0f,
                        ),
                ),
        )
    }

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        child.computeLayout(x, y)
    }

    override fun layoutChildren(): List<MathElementLayout> = listOf(child)

    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? = child.hitTest(x, y)
}

internal class MathNoneLayout : MathElementLayout() {
    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        measuredWidth = 0f
        measuredUpperHeight = 0f
        measuredBottomHeight = 0f
    }

    override fun DrawScope.draw(config: MathCanvasConfig) = Unit
}

internal class MathMspaceLayout(
    val element: MathMspace,
) : MathElementLayout() {
    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        measuredWidth = 0f
        measuredUpperHeight = 0f
        measuredBottomHeight = 0f
    }

    override fun DrawScope.draw(config: MathCanvasConfig) = Unit
}
