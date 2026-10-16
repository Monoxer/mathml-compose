package com.monoxer.mathmlcompose.render.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoxer.mathmlcompose.input.hint.hintInputSlot
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMmultiscriptsRegularized
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMo
import com.monoxer.mathmlcompose.math.MathMover
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
import com.monoxer.mathmlcompose.render.compose.internal.mathlayout.MathCanvasConfig
import com.monoxer.mathmlcompose.render.compose.internal.mathlayout.createLayout
import com.monoxer.mathmlcompose.render.compose.preview.PreviewDefaults
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Suppress("CyclomaticComplexMethod")
fun MathCanvas(
    element: MathElement,
    colors: MxhtmlColors,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    color: Color = Color.Unspecified,
    fontFamily: FontFamily = FontFamily.Default,
    onTap: ((HitResult) -> Unit)? = null,
    editable: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textColor = if (color != Color.Unspecified) color else LocalContentColor.current

    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }

    val layoutConfig =
        remember(fontSize, density, textColor, fontFamily, editable, colors) {
            val scale = density.density
            val baseHeight = with(density) { fontSize.toPx() }
            MathCanvasConfig(
                scale = scale,
                baseHeight = baseHeight,
                textColor = textColor,
                colors = colors,
                fontFamily = fontFamily,
                editable = editable,
            )
        }
    val config = layoutConfig.copy(cursorAlpha = if (cursorVisible) 1f else 0f)

    val layout = remember(element) { createLayout(element) }

    // Re-measure and compute absolute positions when layout config or text measurer changes
    remember(layout, textMeasurer, layoutConfig) {
        layout.measure(textMeasurer, layoutConfig)
        layout.computeLayout(0f, layout.measuredUpperHeight)
    }

    // Add 1px buffer so a trailing zero-width cursor is not clipped at the right edge.
    val width = with(density) { (layout.measuredWidth + layoutConfig.scale).toDp() }
    val height = with(density) { layout.measuredHeight().toDp() }

    val latestLayout by rememberUpdatedState(layout)

    val tapModifier =
        if (onTap != null) {
            Modifier.pointerInput(onTap) {
                detectTapGestures { offset ->
                    // offset is in the same layout-pixel coordinate system as layoutX/layoutY
                    val hit = latestLayout.hitTest(offset.x, offset.y)
                    if (hit != null) onTap(hit)
                }
            }
        } else {
            Modifier
        }

    Canvas(modifier = modifier.size(width, height).then(tapModifier)) {
        translate(0f, layout.measuredUpperHeight) {
            if (editable) {
                val bounds = layout.findActiveMrowBounds()
                if (bounds != null) {
                    drawActiveMrowHighlight(bounds, layout.measuredUpperHeight, layoutConfig)
                }
            }
            with(layout) { draw(config) }
        }
    }
}

private const val ACTIVE_MROW_DASH_LENGTH_FACTOR = 10f
private const val ACTIVE_MROW_DASH_GAP_FACTOR = 5f

private fun DrawScope.drawActiveMrowHighlight(
    bounds: Rect,
    translateY: Float,
    layoutConfig: MathCanvasConfig,
) {
    val boxLeft = bounds.left
    val boxTop = bounds.top - translateY
    val boxWidth = bounds.right - bounds.left
    val boxHeight = bounds.bottom - bounds.top
    // Fill: very light overlay
    drawRect(
        color = layoutConfig.colors.focusedBackground,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
    )
    // Dashed border
    drawRect(
        color = layoutConfig.colors.focusedBorder,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        style =
            Stroke(
                width = maxOf(layoutConfig.scale / 2f, 1f),
                pathEffect =
                    PathEffect.dashPathEffect(
                        floatArrayOf(
                            ACTIVE_MROW_DASH_LENGTH_FACTOR * layoutConfig.scale,
                            ACTIVE_MROW_DASH_GAP_FACTOR * layoutConfig.scale,
                        ),
                        0f,
                    ),
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun MathCanvasPreview() {
    val formula =
        MathMrow(
            listOf(
                // ∑_{i=0}^{n}
                MathMunderover(
                    main = MathMo("∑"),
                    under = MathMrow(listOf(MathMi("i"), MathMo("="), MathMn("0"))),
                    over = MathMi("n"),
                ),
                // y_i²
                MathMsubsup(main = MathMi("y"), sub = MathMi("i"), sup = MathMn("2")),
                MathMo("+"),
                // a_0
                MathMsub(main = MathMi("a"), sub = MathMn("0")),
                MathMo("+"),
                // √(b²-4av) / 2a
                MathMfrac(
                    numerator =
                        MathMsqrt(
                            MathMrow(
                                listOf(
                                    MathMsup(MathMi("b"), MathMn("2")),
                                    MathMo("-"),
                                    MathMn("4"),
                                    MathMi("a"),
                                    MathMover(main = MathMi("v"), over = MathMo("→")),
                                ),
                            ),
                        ),
                    denominator = MathMrow(listOf(MathMn("2"), MathMi("a"))),
                ),
                // --- line break ---
                MathMspace(isLinebreak = true),
                MathMo("+"),
                // ³√(a+b)
                MathMroot(
                    main = MathMrow(listOf(MathMi("a"), MathMo("+"), MathMi("b"))),
                    root = MathMn("3"),
                ),
                MathMo("+"),
                // lim_{x→0} f(x)
                MathMunder(
                    main = MathMtext("lim"),
                    under = MathMrow(listOf(MathMi("x"), MathMo("→"), MathMn("0"))),
                ),
                MathMi("f"),
                MathMo("("),
                MathMi("x"),
                MathMo(")"),
                MathMo("+"),
                // ²⁰⁸Pb (pre-superscript mass number)
                MathMmultiscriptsRegularized(
                    base = MathMrow(listOf(MathMi("P"), MathMi("b"))),
                    postSubScripts = MathNone,
                    postSupScripts = MathNone,
                    preSubScripts = MathNone,
                    preSupScripts = MathMn("208"),
                ),
                MathMo("+"),
                // v⃗ (vector with arrow over)
                MathMover(main = MathMi("v"), over = MathMo("→")),
                MathMo("+"),
                // □ (input placeholder)
                MathPlaceholder,
            ),
        )

    MathCanvas(
        element = formula,
        colors = PreviewDefaults.colors,
        modifier = Modifier.padding(16.dp),
        fontSize = 20.sp,
    )
}

@Preview(showBackground = true)
@Composable
private fun MathCanvasHintInputPreview() {
    val filled = listOf<MathElement?>(MathMi("cos"), MathMi("sin"), null, null)

    fun slot(i: Int) = hintInputSlot(filled, activeBlankIndex = 1, index = i)
    val element =
        MathMrow(
            listOf(
                slot(0),
                MathMn("2.5"),
                MathMi("π"),
                MathMo("<"),
                slot(1),
                MathMn("2.5"),
                MathMi("π"),
                slot(2),
                MathMi("tan"),
                MathMo("("),
                MathMo("-"),
                MathMfrac(numerator = slot(3), denominator = MathMn("3")),
                MathMo(")"),
            ),
        )
    MathCanvas(
        element = element,
        colors = PreviewDefaults.colors,
        modifier = Modifier.padding(16.dp),
        fontSize = 20.sp,
        editable = true,
    )
}
