package com.monoxer.mathmlcompose.mathml

import com.monoxer.mathmlcompose.math.MSPACE_LINEBREAK_ATTR
import com.monoxer.mathmlcompose.math.MSPACE_LINEBREAK_NEWLINE
import com.monoxer.mathmlcompose.math.MX_DATA_IS_HIDDEN
import com.monoxer.mathmlcompose.math.MX_IS_PLACEHOLDER
import com.monoxer.mathmlcompose.math.MX_JUDGE
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
import com.monoxer.mathmlcompose.math.MathMrowWithCriterion
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
import com.monoxer.mathmlcompose.math.criterionToString

private const val PLACEHOLDER_CHAR = "□"

/**
 * Serializes a [MathElement] tree to a MathML string.
 * The result can be passed to [MathMLParser.parse] or rendered by [MxhtmlView].
 */
fun MathElement.toMathML(): String = "<math>${toMathMLInner()}</math>"

/** Strips `mx-data-is-hidden` attributes from a MathML string so all elements render visibly. */
fun stripHiddenAttrs(text: String): String = text.replace(Regex("""\s+$MX_DATA_IS_HIDDEN="[^"]*""""), "")

/** Removes `<mtext mx-is-placeholder="true">□</mtext>` elements from a MathML string. */
fun stripPlaceholders(text: String): String =
    text.replace(Regex("""<mtext\s+$MX_IS_PLACEHOLDER="true">[^<]*</mtext>"""), "")

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun MathElement.toMathMLInner(): String =
    when (this) {
        is MathMi -> {
            "<mi>${encodeText(text)}</mi>"
        }

        is MathMo -> {
            "<mo>${encodeText(text)}</mo>"
        }

        is MathMn -> {
            "<mn>${encodeText(text)}</mn>"
        }

        is MathMtext -> {
            "<mtext>${encodeText(text)}</mtext>"
        }

        is MathMrow -> {
            when (children.size) {
                0 -> ""
                1 -> children[0].toMathMLInner()
                else -> "<mrow>${children.joinToString("") { it.toMathMLInner() }}</mrow>"
            }
        }

        is MathMrowWithCriterion -> {
            // Always emit <mrow> with the mx-judge attribute — the tag cannot be elided even for
            // 0/1 children because the attribute itself must be preserved for roundtrip fidelity.
            "<mrow $MX_JUDGE=\"${criterionToString(
                criterion,
            )}\">${children.joinToString("") { it.toMathMLInner() }}</mrow>"
        }

        is MathMfrac -> {
            "<mfrac>${numerator.toMathMLSlot()}${denominator.toMathMLSlot()}</mfrac>"
        }

        is MathMsqrt -> {
            "<msqrt>${main.toMathMLSlot()}</msqrt>"
        }

        is MathMroot -> {
            "<mroot>${main.toMathMLSlot()}${root.toMathMLSlot()}</mroot>"
        }

        is MathMsup -> {
            "<msup>${main.toMathMLSlot()}${sup.toMathMLSlot()}</msup>"
        }

        is MathMsub -> {
            "<msub>${main.toMathMLSlot()}${sub.toMathMLSlot()}</msub>"
        }

        is MathMsubsup -> {
            "<msubsup>${main.toMathMLSlot()}${sub.toMathMLSlot()}${sup.toMathMLSlot()}</msubsup>"
        }

        is MathMover -> {
            "<mover>${main.toMathMLSlot()}${over.toMathMLSlot()}</mover>"
        }

        is MathMunder -> {
            "<munder>${main.toMathMLSlot()}${under.toMathMLSlot()}</munder>"
        }

        is MathMunderover -> {
            "<munderover>${main.toMathMLSlot()}${under.toMathMLSlot()}${over.toMathMLSlot()}</munderover>"
        }

        is MathMspace -> {
            if (isLinebreak) "<mspace $MSPACE_LINEBREAK_ATTR=\"$MSPACE_LINEBREAK_NEWLINE\"/>" else "<mspace/>"
        }

        is MathMmultiscriptsRegularized -> {
            val hasPreScripts =
                preSubScripts !is MathStructuralNone || preSupScripts !is MathStructuralNone
            buildString {
                append("<mmultiscripts>")
                append(base.toMathMLInner())
                append(postSubScripts.toMathMLInner())
                append(postSupScripts.toMathMLInner())
                if (hasPreScripts) {
                    append("<mprescripts/>")
                    append(preSubScripts.toMathMLInner())
                    append(preSupScripts.toMathMLInner())
                }
                append("</mmultiscripts>")
            }
        }

        is MathFilledSlot -> {
            child.toMathMLInner()
        }

        is MathPlaceholder -> {
            "<mtext $MX_IS_PLACEHOLDER=\"true\">$PLACEHOLDER_CHAR</mtext>"
        }

        // Structural / transient nodes produce no output
        is MathNone, is MathStructuralNone, is MathMprescriptsMarker, is MathCursor -> {
            ""
        }
    }

// Returns "<mrow/>" when the element serializes to empty string (e.g. MathNone, MathPlaceholder),
// ensuring fixed-arity containers (mfrac, msup, etc.) always have the required number of children.
private fun MathElement.toMathMLSlot(): String = toMathMLInner().ifEmpty { "<mrow/>" }

private fun encodeText(text: String): String =
    text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
