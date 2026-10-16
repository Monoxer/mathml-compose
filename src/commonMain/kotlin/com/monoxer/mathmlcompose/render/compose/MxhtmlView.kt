package com.monoxer.mathmlcompose.render.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.monoxer.mathmlcompose.mathml.MathMLParser
import com.monoxer.mathmlcompose.mxhtml.splitMxhtmlSegments
import com.monoxer.mathmlcompose.render.compose.preview.PreviewDefaults
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Renders Monoxer formula node text, which may be:
 * - Pure MathML: `<math>...</math>`
 * - Mixed mxhtml: `<mxhtml>text <math>...</math> text</mxhtml>`
 * - Plain text
 */
@Composable
fun MxhtmlView(
    text: String,
    colors: MxhtmlColors,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    mathFontFamily: FontFamily = FontFamily.Default,
) {
    val trimmed = text.trim()
    when {
        trimmed.startsWith("<math") -> {
            val element = remember(trimmed) { MathMLParser.parse(trimmed) }
            MathCanvas(
                element = element,
                colors = colors,
                modifier = modifier.horizontalScroll(rememberScrollState()),
                fontSize = fontSize,
                color = color,
                fontFamily = mathFontFamily,
            )
        }

        trimmed.contains("<math") -> {
            // Mixed text + math (with or without <mxhtml> wrapper)
            val inner =
                if (trimmed.startsWith("<mxhtml>")) {
                    trimmed.removePrefix("<mxhtml>").removeSuffix("</mxhtml>")
                } else {
                    trimmed
                }
            val segments = remember(inner) { splitMxhtmlSegments(inner) }
            Column(modifier = modifier.fillMaxWidth()) {
                segments.forEach { segment ->
                    if (segment.startsWith("<math")) {
                        val element = remember(segment) { MathMLParser.parse(segment) }
                        MathCanvas(
                            element = element,
                            colors = colors,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            fontSize = fontSize,
                            color = color,
                            fontFamily = mathFontFamily,
                        )
                    } else if (segment.isNotBlank()) {
                        Text(
                            text = segment.trim(),
                            fontSize = fontSize,
                            color = color,
                            fontWeight = fontWeight,
                        )
                    }
                }
            }
        }

        else -> {
            Text(
                text = trimmed,
                fontSize = fontSize,
                color = color,
                fontWeight = fontWeight,
                modifier = modifier,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MxhtmlViewPlainTextPreview() {
    MxhtmlView(text = "二次方程式 ax²+bx+c=0 の解を求めよ。", colors = PreviewDefaults.colors)
}

@Preview(showBackground = true)
@Composable
private fun MxhtmlViewPureMathPreview() {
    MxhtmlView(
        text =
            "<math><mfrac><mrow><mo>-</mo><mi>b</mi><mo>&#177;</mo>" +
                "<msqrt><mrow><msup><mi>b</mi><mn>2</mn></msup><mo>-</mo><mn>4</mn><mi>a</mi><mi>c</mi></mrow>" +
                "</msqrt></mrow><mrow><mn>2</mn><mi>a</mi></mrow></mfrac></math>",
        colors = PreviewDefaults.colors,
    )
}

@Preview(showBackground = true)
@Composable
private fun MxhtmlViewMixedPreview() {
    MxhtmlView(
        text =
            "<mxhtml>解の公式は <math><mi>x</mi><mo>=</mo>" +
                "<mfrac><mrow><mo>-</mo><mi>b</mi></mrow><mrow><mn>2</mn><mi>a</mi></mrow></mfrac></math> です。</mxhtml>",
        colors = PreviewDefaults.colors,
    )
}
