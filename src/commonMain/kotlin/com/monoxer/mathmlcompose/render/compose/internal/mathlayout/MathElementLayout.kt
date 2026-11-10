package com.monoxer.mathmlcompose.render.compose.internal.mathlayout

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathFilledSlot
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMmultiscriptsRegularized
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMo
import com.monoxer.mathmlcompose.math.MathMover
import com.monoxer.mathmlcompose.math.MathMprescriptsMarker
import com.monoxer.mathmlcompose.math.MathMroot
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathMspace
import com.monoxer.mathmlcompose.math.MathMsqrt
import com.monoxer.mathmlcompose.math.MathMsub
import com.monoxer.mathmlcompose.math.MathMsubsup
import com.monoxer.mathmlcompose.math.MathMsup
import com.monoxer.mathmlcompose.math.MathMtext
import com.monoxer.mathmlcompose.math.MathMunder
import com.monoxer.mathmlcompose.math.MathMunderover
import com.monoxer.mathmlcompose.math.MathNone
import com.monoxer.mathmlcompose.math.MathPlaceholder
import com.monoxer.mathmlcompose.math.MathStructuralNone
import com.monoxer.mathmlcompose.render.compose.HitResult

// Smaller level for script elements (sub, sup, multiscripts).
// Each level reduces font size by 2*scale; level 4 ≈ one noticeable size step down.
internal const val MODIFIER_SMALLER_LEVEL = 4

// Smaller level for over/under elements — slightly less reduction than scripts.
internal const val OVER_UNDER_SMALLER_LEVEL = 2

// VSpace multiplier for the gap between the sqrt bar and the content below it.
internal const val SQRT_BAR_VSPACE_FACTOR = 1.5f

// Tight ink upper bound as a fraction of baseHeight, shared by MathLeafLayout and
// MathPlaceholderLayout / MathMrowLayout so that script positioning (subDy/supDy)
// is identical whether a slot holds a leaf or a cursor/placeholder.
internal const val INK_UPPER_HEIGHT_RATIO = 0.70f

// Visual upper extent of empty/filled slot boxes and cursor carets as a fraction of baseHeight.
// Shared by MathPlaceholderLayout, MathCursorLayout, and MathFilledSlotLayout so all slot
// boxes maintain the same height regardless of content.
internal const val SLOT_UPPER_HEIGHT_RATIO = 0.75f

internal const val SLOT_DASH_LENGTH = 8f
internal const val SLOT_DASH_GAP = 4f

// Geometric divisor used to position the fraction bar and radical symbol vertices
// relative to baseHeight (i.e. baseHeight / GEOMETRY_THIRD = one-third of font height).
internal const val GEOMETRY_THIRD = 3f

// Horizontal space multiplier for fraction width padding (baseHSpace * FRAC_HSPACE_FACTOR).
internal const val FRAC_HSPACE_FACTOR = 4f

internal abstract class MathElementLayout {
    var measuredUpperHeight: Float = 0f
    var measuredBottomHeight: Float = 0f
    var measuredWidth: Float = 0f

    fun measuredHeight(): Float = measuredUpperHeight + measuredBottomHeight

    // Tight glyph ink bounds above the baseline, used to position over/under/script elements.
    // Defaults to the full layout ascent. Leaf elements override this with ~70% of font size.
    open val inkUpperHeight: Float get() = measuredUpperHeight

    // Absolute canvas position set by computeLayout(). layoutY is the baseline Y in canvas px.
    var layoutX: Float = 0f
    var layoutY: Float = 0f

    abstract fun measure(
        textMeasurer: TextMeasurer,
        config: MathCanvasConfig,
    )

    abstract fun DrawScope.draw(config: MathCanvasConfig)

    /**
     * Propagates absolute canvas positions to this element and its children.
     * Must be called after [measure]. [x] = left edge, [y] = baseline, both in canvas px.
     */
    open fun computeLayout(
        x: Float,
        y: Float,
    ) {
        layoutX = x
        layoutY = y
    }

    /**
     * Returns the [HitResult] for the deepest element containing canvas-pixel point ([x], [y]),
     * or null if nothing was hit. [HitResult.path] is relative to this element's children.
     * [HitResult.onRight] is true when the tap was on the right half of the element.
     */
    open fun hitTest(
        x: Float,
        y: Float,
    ): HitResult? = null

    /**
     * Returns the sub-layout children of this element (used for [findActiveMrowBounds] traversal).
     * Override in compound layouts to expose their child layout instances.
     */
    open fun layoutChildren(): List<MathElementLayout> = emptyList()

    /**
     * Returns the canvas-pixel bounding [Rect] of the innermost [MathMrowLayout] that directly
     * contains the active cursor, or null if no cursor is present in this subtree.
     * Coordinates use the same system as [layoutX]/[layoutY] (baseline at [layoutY]).
     */
    open fun findActiveMrowBounds(): Rect? = layoutChildren().firstNotNullOfOrNull { it.findActiveMrowBounds() }
}

@Suppress("CyclomaticComplexMethod")
internal fun createLayout(element: MathElement): MathElementLayout =
    when (element) {
        is MathMn -> MathMnLayout(element)

        is MathMo -> MathMoLayout(element)

        is MathMi -> MathMiLayout(element)

        is MathMtext -> MathMtextLayout(element)

        is MathMrow -> MathMrowLayout(element)

        is MathMfrac -> MathMfracLayout(element)

        is MathMsqrt -> MathMsqrtLayout(element)

        is MathMroot -> MathMrootLayout(element)

        is MathMsup -> MathMsupLayout(element)

        is MathMsub -> MathMsubLayout(element)

        is MathMsubsup -> MathMsubsupLayout(element)

        is MathMover -> MathMoverLayout(element)

        is MathMunder -> MathMunderLayout(element)

        is MathMunderover -> MathMunderoverLayout(element)

        is MathMmultiscriptsRegularized -> MathMmultiscriptsLayout(element)

        is MathMspace -> MathMspaceLayout(element)

        is MathNone -> MathNoneLayout()

        is MathStructuralNone -> MathNoneLayout()

        // MathMprescriptsMarker is consumed by parseMmultiscripts and never stored in the tree.
        // Reaching this branch indicates a parser bug.
        is MathMprescriptsMarker -> error("MathMprescriptsMarker must not appear in the final element tree")

        is MathPlaceholder -> MathPlaceholderLayout()

        is MathFilledSlot -> MathFilledSlotLayout(element)

        is MathCursor -> MathCursorLayout()
    }
