package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.render.compose.HitResult

internal class MathMrowLayout(
    val element: MathMrow,
) : MathElementLayout() {
    private val children: List<MathElementLayout> = element.children.map { createLayout(it) }

    companion object {
        // Extra vertical margin added above/below each line at a linebreak, as a fraction of baseHeight.
        private const val LINEBREAK_MARGIN_FACTOR = 0.7f
    }

    // baseHeight stored during measure so findActiveMrowBounds() can use it for a stable box height.
    private var storedBaseHeight: Float = 0f

    // Per-line heights for multi-line support
    private val lineUpperHeights = mutableListOf<Float>()
    private val lineBottomHeights = mutableListOf<Float>()

    // Tight ink bounds propagated from children (first line only).
    private var lineInkUpperHeights = mutableListOf<Float>()

    override val inkUpperHeight: Float get() = lineInkUpperHeights.firstOrNull() ?: measuredUpperHeight

    override fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    ) {
        storedBaseHeight = config.baseHeight
        val singleCursor = children.singleOrNull()
        if (singleCursor is MathCursorLayout) {
            singleCursor.measure(textMeasurer, config)
            // Square box: total height = width = baseHeight.
            // measuredUpperHeight uses actual font firstBaseline so it matches a typed character
            // exactly — this eliminates the layout jump when the first char enters the slot.
            // measuredBottomHeight is derived to keep (upper + bottom) = baseHeight (square).
            val fontSizeSp = (config.baseHeight / config.scale).sp
            val refResult = textMeasurer.measure("1", TextStyle(fontSize = fontSizeSp, fontFamily = config.fontFamily))
            measuredUpperHeight = refResult.firstBaseline + config.baseVSpace
            // Use actual font descender so that subDy + sub.measuredBottomHeight is the same for
            // the empty slot and content, preventing canvas clipping on nested sub/sup structures.
            // The dashed box is still drawn as a visual square (width = height = baseHeight).
            measuredBottomHeight = (refResult.size.height.toFloat() - refResult.firstBaseline) + config.baseVSpace
            measuredWidth = config.baseHeight
            lineUpperHeights.clear()
            lineUpperHeights.add(measuredUpperHeight)
            lineBottomHeights.clear()
            lineBottomHeights.add(measuredBottomHeight)
            // inkUpperHeight for positioning uses 0.70*bH formula (matches leaf/placeholder)
            // so subDy/supDy are identical whether the slot has content or just a cursor.
            lineInkUpperHeights.clear()
            lineInkUpperHeights.add(config.baseHeight * INK_UPPER_HEIGHT_RATIO + config.baseVSpace)
            return
        }
        if (children.isEmpty()) {
            measuredWidth = config.baseHeight
            measuredUpperHeight = config.baseHeight
            measuredBottomHeight = 0f
            return
        }

        lineUpperHeights.clear()
        lineBottomHeights.clear()
        lineInkUpperHeights.clear()

        var widthTemp = 0f
        var upperTemp = 0f
        var bottomTemp = 0f
        var inkUpperTemp = 0f
        measuredWidth = 0f
        val linebreakMargin = config.baseHeight * LINEBREAK_MARGIN_FACTOR

        for (child in children) {
            val elem = (child as? MathMspaceLayout)?.element
            if (elem != null && elem.isLinebreak) {
                measuredWidth = maxOf(widthTemp, measuredWidth)
                lineUpperHeights.add(upperTemp + linebreakMargin)
                lineBottomHeights.add(bottomTemp + linebreakMargin)
                lineInkUpperHeights.add(inkUpperTemp)
                widthTemp = 0f
                upperTemp = 0f
                bottomTemp = 0f
                inkUpperTemp = 0f
            } else {
                child.measure(textMeasurer, config)
                upperTemp = maxOf(upperTemp, child.measuredUpperHeight)
                bottomTemp = maxOf(bottomTemp, child.measuredBottomHeight)
                inkUpperTemp = maxOf(inkUpperTemp, child.inkUpperHeight)
                widthTemp += child.measuredWidth
            }
        }

        measuredWidth = maxOf(widthTemp, measuredWidth)
        // Do NOT add extra baseVSpace here: each leaf already includes its own baseVSpace in its
        // measuredUpperHeight / measuredBottomHeight. Adding it again caused the slot to be taller
        // when it had content than when it was empty (cursor-only), producing visible layout jumps
        // in sup/sub boxes when the user first typed a character.
        lineUpperHeights.add(upperTemp)
        lineBottomHeights.add(bottomTemp)
        lineInkUpperHeights.add(inkUpperTemp)

        measuredBottomHeight = lineBottomHeights.sum()
        measuredUpperHeight = lineUpperHeights.sum()
    }

    override fun DrawScope.draw(config: MathCanvasConfig) {
        val singleCursor = children.singleOrNull()
        if (singleCursor is MathCursorLayout) {
            drawEmptySlot(config)
            with(singleCursor) { draw(config) }
            return
        }
        drawChildren(config)
    }

    private fun DrawScope.drawEmptySlot(config: MathCanvasConfig) {
        drawRect(
            color = config.textColor.copy(alpha = 0.4f),
            // Align the box with the cursor: cursor uses SLOT_UPPER_HEIGHT_RATIO * baseHeight as
            // its upper extent (same as MathCursorLayout). measuredUpperHeight uses actual font
            // firstBaseline for layout stability; the visual box is pinned to the cursor height.
            topLeft = Offset(0f, -config.baseHeight * SLOT_UPPER_HEIGHT_RATIO),
            size = Size(measuredWidth, config.baseHeight),
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

    private fun DrawScope.drawChildren(config: MathCanvasConfig) {
        var dx = 0f
        var dy = -measuredUpperHeight + (lineUpperHeights.firstOrNull() ?: 0f)
        var lineIndex = 0

        for (child in children) {
            val elem = (child as? MathMspaceLayout)?.element
            if (elem != null && elem.isLinebreak) {
                dx = 0f
                if (lineIndex + 1 < lineUpperHeights.size) {
                    dy += lineBottomHeights[lineIndex] + lineUpperHeights[lineIndex + 1]
                }
                lineIndex++
                continue
            }
            translate(dx, dy) {
                with(child) { draw(config) }
            }
            dx += child.measuredWidth
        }
    }

    override fun computeLayout(
        x: Float,
        y: Float,
    ) {
        super.computeLayout(x, y)
        val singleCursor = children.singleOrNull()
        if (singleCursor is MathCursorLayout) {
            singleCursor.computeLayout(x, y)
            return
        }
        var dx = 0f
        var dy = -measuredUpperHeight + (lineUpperHeights.firstOrNull() ?: 0f)
        var lineIndex = 0

        for (child in children) {
            val elem = (child as? MathMspaceLayout)?.element
            if (elem != null && elem.isLinebreak) {
                dx = 0f
                if (lineIndex + 1 < lineUpperHeights.size) {
                    dy += lineBottomHeights[lineIndex] + lineUpperHeights[lineIndex + 1]
                }
                lineIndex++
                continue
            }
            // child baseline is at canvas y = y + dy
            child.computeLayout(x + dx, y + dy)
            dx += child.measuredWidth
        }
    }

    override fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? {
        val outOfBounds =
            x < layoutX || x > layoutX + measuredWidth ||
                y < layoutY - measuredUpperHeight || y > layoutY + measuredBottomHeight
        if (outOfBounds) return null
        val hittableChildren =
            children
                .asSequence()
                // Skip MathCursor when building the path index, because the path will be used
                // in moveCursorTo() against a cursor-removed tree (withCursorRemoved). If the cursor
                // were counted, every element after it would have an off-by-one index there.
                .filterNot { it is MathCursorLayout }
                .toList()
        // Try to find a child that directly claims the tap; fall back to nearest child
        // so gaps and padding zones (e.g. frac horizontal padding) always resolve.
        return hittableChildren
            .mapIndexedNotNull { idx, child ->
                child.hitTest(x, y)?.let { HitResult(listOf(idx) + it.path, it.onRight) }
            }.firstOrNull()
            ?: nearestChildHit(hittableChildren, x)
    }

    /** Fallback: place cursor before/after whichever child edge is closest to [x]. */
    private fun nearestChildHit(
        hittableChildren: List<MathElementLayout>,
        x: Float,
    ): HitResult {
        if (hittableChildren.isEmpty()) return HitResult(emptyList(), false)
        var bestIdx = 0
        var bestOnRight = false
        var bestDist = Float.MAX_VALUE
        hittableChildren.forEachIndexed { idx, child ->
            val leftDist = kotlin.math.abs(x - child.layoutX)
            val rightDist = kotlin.math.abs(x - (child.layoutX + child.measuredWidth))
            if (leftDist < bestDist) {
                bestDist = leftDist
                bestIdx = idx
                bestOnRight = false
            }
            if (rightDist < bestDist) {
                bestDist = rightDist
                bestIdx = idx
                bestOnRight = true
            }
        }
        return HitResult(listOf(bestIdx), bestOnRight)
    }

    override fun layoutChildren(): List<MathElementLayout> = children

    override fun findActiveMrowBounds(): Rect? {
        if (children.any { it is MathCursorLayout }) {
            // Complex content (frac, sub/sup, etc.) changes the slot height — use measured bounds.
            // Simple content (leaves only) keeps the same height as the empty slot — use the
            // fixed baseHeight box so the box does not jump when the first character is typed.
            val hasComplex = children.any { it !is MathCursorLayout && it.layoutChildren().isNotEmpty() }
            val (upper, lower) =
                if (hasComplex) {
                    measuredUpperHeight to measuredBottomHeight
                } else {
                    val visualUpper = storedBaseHeight * SLOT_UPPER_HEIGHT_RATIO
                    visualUpper to (storedBaseHeight - visualUpper)
                }
            return Rect(
                left = layoutX,
                top = layoutY - upper,
                right = layoutX + measuredWidth,
                bottom = layoutY + lower,
            )
        }
        return children.firstNotNullOfOrNull { it.findActiveMrowBounds() }
    }
}
