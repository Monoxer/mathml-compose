package com.monoxer.mathmlcompose.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoxer.mathmlcompose.input.formula.FormulaInputState
import com.monoxer.mathmlcompose.input.formula.backspace
import com.monoxer.mathmlcompose.input.formula.insertFrac
import com.monoxer.mathmlcompose.input.formula.insertLeaf
import com.monoxer.mathmlcompose.input.formula.insertSqrt
import com.monoxer.mathmlcompose.input.formula.insertSub
import com.monoxer.mathmlcompose.input.formula.insertSup
import com.monoxer.mathmlcompose.input.formula.moveCursorTo
import com.monoxer.mathmlcompose.input.formula.moveLeft
import com.monoxer.mathmlcompose.input.formula.moveRight
import com.monoxer.mathmlcompose.input.formula.toResultElement
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMo
import com.monoxer.mathmlcompose.mathml.toMathML
import com.monoxer.mathmlcompose.render.compose.MathCanvas
import com.monoxer.mathmlcompose.render.compose.MxhtmlColors
import com.monoxer.mathmlcompose.render.compose.MxhtmlView

@Suppress("MagicNumber") // Color literals define the demo palette.
private val DemoColors =
    lightColorScheme(
        primary = Color(0xFF315C49),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7E9DF),
        onPrimaryContainer = Color(0xFF18352A),
        secondary = Color(0xFF7A583B),
        background = Color(0xFFF5F3EE),
        surface = Color(0xFFFFFCF7),
        surfaceVariant = Color(0xFFE9E7E1),
        outline = Color(0xFF7A7A72),
    )

private const val SAMPLE_MXHTML =
    "<mxhtml>For the quadratic equation " +
        "<math><mi>a</mi><msup><mi>x</mi><mn>2</mn></msup><mo>+</mo><mi>b</mi><mi>x</mi>" +
        "<mo>+</mo><mi>c</mi><mo>=</mo><mn>0</mn></math>, the solution is</mxhtml>"

private const val SAMPLE_SOLUTION =
    "<math><mi>x</mi><mo>=</mo><mfrac><mrow><mo>-</mo><mi>b</mi><mo>&#177;</mo>" +
        "<msqrt><mrow><msup><mi>b</mi><mn>2</mn></msup><mo>-</mo><mn>4</mn><mi>a</mi><mi>c</mi>" +
        "</mrow></msqrt></mrow><mrow><mn>2</mn><mi>a</mi></mrow></mfrac></math>"

@Composable
fun DemoApp(modifier: Modifier = Modifier) {
    MaterialTheme(colorScheme = DemoColors) {
        val canvasColors =
            MxhtmlColors(
                accentColor = MaterialTheme.colorScheme.primary,
                focusedBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                focusedBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            )

        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .safeContentPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DemoHeader()
                ReadOnlyDemo(colors = canvasColors)
                FormulaEditor(colors = canvasColors)
            }
        }
    }
}

@Composable
private fun DemoHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "MathML Compose",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Native math, from display to input",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text =
                "The same renderer works on Android, iOS, Compose Desktop, and in the browser, " +
                    "without a WebView or MathJax.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ReadOnlyDemo(colors: MxhtmlColors) {
    DemoCard(
        eyebrow = "READ-ONLY",
        title = "Mixed MxHTML and MathML",
        description = "Rich text and nested formulas are parsed and drawn directly by Compose.",
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MxhtmlView(
                    text = SAMPLE_MXHTML,
                    colors = colors,
                    fontSize = 18.sp,
                )
                MxhtmlView(
                    text = SAMPLE_SOLUTION,
                    colors = colors,
                    fontSize = 28.sp,
                )
            }
        }
    }
}

@Composable
private fun FormulaEditor(colors: MxhtmlColors) {
    var state by remember { mutableStateOf(initialFormulaState()) }
    val mathML = state.toResultElement().toMathML()

    DemoCard(
        eyebrow = "INTERACTIVE",
        title = "Build a formula",
        description = "Tap the formula to move the cursor, or use the controls to edit its structure.",
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(14.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val viewportWidth = maxWidth
                val viewportHeight = maxHeight

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                            .verticalScroll(rememberScrollState()),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .widthIn(min = viewportWidth)
                                .heightIn(min = viewportHeight)
                                .padding(24.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        MathCanvas(
                            element = state.root,
                            colors = colors,
                            fontSize = 34.sp,
                            editable = true,
                            onTap = { hit -> state = state.moveCursorTo(hit.path, hit.onRight) },
                        )
                    }
                }
            }
        }

        FormulaToolbar(
            onLeaf = { state = state.insertLeaf(it) },
            onFraction = { state = state.insertFrac() },
            onSquareRoot = { state = state.insertSqrt() },
            onSuperscript = { state = state.insertSup() },
            onSubscript = { state = state.insertSub() },
            onLeft = { state = state.moveLeft() },
            onRight = { state = state.moveRight() },
            onBackspace = { state = state.backspace() },
            onReset = { state = initialFormulaState() },
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SERIALIZED MATHML",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF24302A),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = mathML,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFE2ECE6),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        }
    }
}

@Composable
private fun FormulaToolbar(
    onLeaf: (MathElement) -> Unit,
    onFraction: () -> Unit,
    onSquareRoot: () -> Unit,
    onSuperscript: () -> Unit,
    onSubscript: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onBackspace: () -> Unit,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ControlRow {
            listOf("1", "2", "3", "4", "5").forEach { value ->
                KeyButton(value) { onLeaf(MathMn(value)) }
            }
        }
        ControlRow {
            listOf("6", "7", "8", "9", "0").forEach { value ->
                KeyButton(value) { onLeaf(MathMn(value)) }
            }
        }
        ControlRow {
            listOf("x", "y", "a", "b", "c").forEach { value ->
                KeyButton(value) { onLeaf(MathMi(value)) }
            }
        }
        ControlRow {
            listOf(
                "+" to "+",
                "−" to "-",
                "×" to "×",
                "±" to "±",
                "=" to "=",
                "(" to "(",
                ")" to ")",
            ).forEach { (label, value) -> KeyButton(label) { onLeaf(MathMo(value)) } }
        }
        ControlRow {
            KeyButton(onClick = onFraction) { FractionKeyContent() }
            KeyButton("√", onClick = onSquareRoot)
            KeyButton(onClick = onSuperscript) { ScriptKeyContent(superscript = true) }
            KeyButton(onClick = onSubscript) { ScriptKeyContent(superscript = false) }
            KeyButton(onClick = onLeft) { ArrowKeyContent(pointsRight = false) }
            KeyButton(onClick = onRight) { ArrowKeyContent(pointsRight = true) }
            KeyButton(onClick = onBackspace) { BackspaceKeyContent() }
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun ControlRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun KeyButton(
    label: String,
    onClick: () -> Unit,
) {
    KeyButton(onClick = onClick) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        content()
    }
}

@Composable
private fun FractionKeyContent() {
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

    Column(
        modifier = Modifier.size(width = 34.dp, height = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        FormulaPlaceholder(
            modifier = Modifier.size(width = 26.dp, height = 9.dp),
            color = placeholderColor,
        )
        Spacer(
            modifier =
                Modifier
                    .size(width = 26.dp, height = 1.5.dp)
                    .background(placeholderColor),
        )
        FormulaPlaceholder(
            modifier = Modifier.size(width = 26.dp, height = 9.dp),
            color = placeholderColor,
        )
    }
}

@Composable
private fun ScriptKeyContent(superscript: Boolean) {
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val baseAlignment = if (superscript) Alignment.Bottom else Alignment.Top
    val placeholderAlignment = if (superscript) Alignment.Top else Alignment.Bottom

    Row(
        modifier = Modifier.size(width = 34.dp, height = 24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "x",
            modifier = Modifier.align(baseAlignment),
            fontSize = 20.sp,
            fontStyle = FontStyle.Italic,
        )
        FormulaPlaceholder(
            modifier =
                Modifier
                    .size(width = 13.dp, height = 10.dp)
                    .align(placeholderAlignment),
            color = placeholderColor,
        )
    }
}

@Composable
private fun FormulaPlaceholder(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier =
            modifier.drawBehind {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style =
                        Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                                ),
                        ),
                )
            },
    )
}

@Composable
private fun ArrowKeyContent(pointsRight: Boolean) {
    val color = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = Modifier.size(24.dp)) {
        val horizontalInset = size.width / 5f
        val arrowTipX = if (pointsRight) size.width - horizontalInset else horizontalInset
        val arrowTailX = if (pointsRight) horizontalInset else size.width - horizontalInset
        val arrowHeadX = size.width / 2f
        val centerY = size.height / 2f
        val arrowHeadInset = size.height / 4f
        val arrowPath =
            Path().apply {
                moveTo(arrowTailX, centerY)
                lineTo(arrowTipX, centerY)
                moveTo(arrowHeadX, arrowHeadInset)
                lineTo(arrowTipX, centerY)
                lineTo(arrowHeadX, size.height - arrowHeadInset)
            }

        drawPath(
            path = arrowPath,
            color = color,
            style =
                Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
        )
    }
}

@Composable
private fun BackspaceKeyContent() {
    val color = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        val horizontalInset = size.width / 12f
        val verticalInset = size.height / 5f
        val notchX = size.width / 3f
        val rightX = size.width - horizontalInset
        val centerY = size.height / 2f
        val outline =
            Path().apply {
                moveTo(horizontalInset, centerY)
                lineTo(notchX, verticalInset)
                lineTo(rightX, verticalInset)
                lineTo(rightX, size.height - verticalInset)
                lineTo(notchX, size.height - verticalInset)
                close()
            }
        val deleteMarkInset = size.width / 3f
        val deleteMarkSize = size.width / 4f
        val deleteMark =
            Path().apply {
                moveTo(deleteMarkInset, centerY - deleteMarkSize / 2f)
                lineTo(deleteMarkInset + deleteMarkSize, centerY + deleteMarkSize / 2f)
                moveTo(deleteMarkInset + deleteMarkSize, centerY - deleteMarkSize / 2f)
                lineTo(deleteMarkInset, centerY + deleteMarkSize / 2f)
            }

        val stroke =
            Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
        drawPath(path = outline, color = color, style = stroke)
        drawPath(path = deleteMark, color = color, style = stroke)
    }
}

@Composable
private fun DemoCard(
    eyebrow: String,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = eyebrow,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            content()
        }
    }
}

private fun initialFormulaState(): FormulaInputState =
    FormulaInputState()
        .insertLeaf(MathMi("x"))
        .insertLeaf(MathMo("="))
        .insertLeaf(MathMn("1"))
