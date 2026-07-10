package com.monoxer.mathmlcompose.mathml

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathFilledSlot
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMi
import com.monoxer.mathmlcompose.math.MathMmultiscriptsRegularized
import com.monoxer.mathmlcompose.math.MathMn
import com.monoxer.mathmlcompose.math.MathMo
import com.monoxer.mathmlcompose.math.MathMover
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
import com.monoxer.mathmlcompose.math.ScoringCriterion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MathElementSerializerTest {
    // ── Leaf elements: toMathML output + roundtrip ────────────────────────────

    @Test
    fun toMathML_leafElements_roundtrip() {
        listOf(
            MathMi("x") to "<math><mi>x</mi></math>",
            MathMo("+") to "<math><mo>+</mo></math>",
            MathMn("42") to "<math><mn>42</mn></math>",
            MathMtext("hello") to "<math><mtext>hello</mtext></math>",
        ).forEach { (elem, expected) ->
            val xml = elem.toMathML()
            assertEquals(expected, xml, "toMathML output for ${elem::class.simpleName}(\"${elem.minText()}\")")
            assertEquals(
                elem,
                MathMLParser.parse(xml),
                "roundtrip for ${elem::class.simpleName}(\"${elem.minText()}\")",
            )
        }
    }

    // ── Special character encoding ────────────────────────────────────────────

    @Test
    fun toMathML_specialChars_encodedAndRoundtrip() {
        listOf(
            MathMi("a&b") to "&amp;",
            MathMo("<") to "&lt;",
            MathMo(">") to "&gt;",
        ).forEach { (elem, encodedForm) ->
            val xml = elem.toMathML()
            assertTrue(xml.contains(encodedForm), "Expected '$encodedForm' in output: $xml")
            assertEquals(elem, MathMLParser.parse(xml), "roundtrip after encoding for ${elem.minText()}")
        }
    }

    // ── MathMrow: 0 / 1 / N children have distinct outputs ───────────────────

    @Test
    fun toMathML_emptyMrow_producesEmptyMath() {
        assertEquals("<math></math>", MathMrow(emptyList()).toMathML())
    }

    @Test
    fun toMathML_singleChildMrow_childNotWrapped() {
        // MathMrow with one child delegates directly — no <mrow> tag emitted
        assertEquals("<math><mi>x</mi></math>", MathMrow(listOf(MathMi("x"))).toMathML())
    }

    @Test
    fun toMathML_multipleChildMrow_wrappedAndRoundtrip() {
        val elem = MathMrow(listOf(MathMi("a"), MathMo("+"), MathMi("b")))
        val xml = elem.toMathML()
        assertEquals("<math><mrow><mi>a</mi><mo>+</mo><mi>b</mi></mrow></math>", xml)
        val parsed = MathMLParser.parse(xml)
        assertIs<MathMrow>(parsed)
        assertEquals(3, parsed.children.size)
    }

    // ── Composite elements: roundtrip ─────────────────────────────────────────

    @Test
    fun toMathML_compositeElements_roundtrip() {
        listOf<MathElement>(
            MathMfrac(numerator = MathMi("a"), denominator = MathMi("b")),
            MathMsqrt(main = MathMi("x")),
            MathMroot(main = MathMi("x"), root = MathMn("3")),
            MathMsup(main = MathMi("x"), sup = MathMn("2")),
            MathMsub(main = MathMi("x"), sub = MathMn("0")),
            MathMsubsup(main = MathMi("x"), sub = MathMn("0"), sup = MathMn("2")),
            MathMover(main = MathMi("x"), over = MathMo("+")),
            MathMunder(main = MathMi("x"), under = MathMo("-")),
            MathMunderover(main = MathMi("x"), under = MathMn("0"), over = MathMn("1")),
        ).forEach { elem ->
            val xml = elem.toMathML()
            assertEquals(elem, MathMLParser.parse(xml), "roundtrip for ${elem::class.simpleName}")
        }
    }

    // ── mspace ────────────────────────────────────────────────────────────────

    @Test
    fun toMathML_mspace_outputAndRoundtrip() {
        listOf(
            MathMspace(isLinebreak = false) to "<math><mspace/></math>",
            MathMspace(isLinebreak = true) to "<math><mspace linebreak=\"newline\"/></math>",
        ).forEach { (elem, expected) ->
            val xml = elem.toMathML()
            assertEquals(expected, xml, "mspace(isLinebreak=${elem.isLinebreak}) output")
            assertEquals(elem, MathMLParser.parse(xml), "mspace(isLinebreak=${elem.isLinebreak}) roundtrip")
        }
    }

    // ── Transient / structural nodes produce no output ────────────────────────

    @Test
    fun toMathML_transientNodes_produceEmptyMath() {
        listOf<MathElement>(MathNone, MathCursor, MathStructuralNone)
            .forEach { node ->
                assertEquals("<math></math>", node.toMathML(), "${node::class.simpleName} should produce empty math")
            }
    }

    // ── mx-is-placeholder ────────────────────────────────────────────────────

    @Test
    fun toMathML_placeholder_serializedAsPlaceholderMtext() {
        assertEquals(
            "<math><mtext mx-is-placeholder=\"true\">□</mtext></math>",
            MathPlaceholder.toMathML(),
        )
    }

    @Test
    fun toMathML_placeholder_roundtrip() {
        val xml = MathPlaceholder.toMathML()
        assertEquals(MathPlaceholder, MathMLParser.parse(xml), "MathPlaceholder should roundtrip via mx-is-placeholder")
    }

    // ── mx-judge / MathMrowWithCriterion ─────────────────────────────────────

    @Test
    fun toMathML_mrowWithCriterion_includesMxJudgeAttribute() {
        listOf(
            MathMrowWithCriterion(listOf(MathMi("x")), ScoringCriterion.STRICT) to "strict",
            MathMrowWithCriterion(listOf(MathMi("x")), ScoringCriterion.NUMERIC) to "numeric",
            MathMrowWithCriterion(listOf(MathMi("x")), ScoringCriterion.NUMERIC_AND_SYMBOLIC) to "symbolic",
            MathMrowWithCriterion(listOf(MathMi("x")), ScoringCriterion.NUMERIC_AND_SIMPLIFIED) to "simplified",
        ).forEach { (elem, judgeValue) ->
            val xml = elem.toMathML()
            assertTrue(xml.contains("mx-judge=\"$judgeValue\""), "Expected mx-judge=\"$judgeValue\" in: $xml")
        }
    }

    @Test
    fun toMathML_mrowWithCriterion_roundtrip() {
        listOf(
            ScoringCriterion.STRICT,
            ScoringCriterion.NUMERIC,
            ScoringCriterion.NUMERIC_AND_SYMBOLIC,
            ScoringCriterion.NUMERIC_AND_SIMPLIFIED,
        ).forEach { criterion ->
            val elem = MathMrowWithCriterion(listOf(MathMi("x"), MathMo("+"), MathMn("1")), criterion)
            val xml = elem.toMathML()
            assertEquals(elem, MathMLParser.parse(xml), "MathMrowWithCriterion(criterion=$criterion) roundtrip")
        }
    }

    // ── MathFilledSlot delegates to child ────────────────────────────────────

    @Test
    fun toMathML_filledSlot_delegatesToChild() {
        assertEquals("<math><mi>a</mi></math>", MathFilledSlot(MathMi("a")).toMathML())
    }

    // ── MathMmultiscriptsRegularized ──────────────────────────────────────────

    @Test
    fun toMathML_mmultiscripts_withoutPrescripts_noPrescriptsTag() {
        val elem =
            MathMmultiscriptsRegularized(
                base = MathMi("A"),
                postSubScripts = MathStructuralNone,
                postSupScripts = MathStructuralNone,
                preSubScripts = MathStructuralNone,
                preSupScripts = MathStructuralNone,
            )
        val xml = elem.toMathML()
        assertTrue(xml.contains("<mmultiscripts>"), "should contain <mmultiscripts>: $xml")
        assertTrue(!xml.contains("<mprescripts/>"), "should NOT contain <mprescripts/>: $xml")
    }

    @Test
    fun toMathML_mmultiscripts_withPrescripts_includesPrescriptsTag() {
        val elem =
            MathMmultiscriptsRegularized(
                base = MathMi("A"),
                postSubScripts = MathStructuralNone,
                postSupScripts = MathStructuralNone,
                preSubScripts = MathMn("1"),
                preSupScripts = MathStructuralNone,
            )
        val xml = elem.toMathML()
        assertTrue(xml.contains("<mprescripts/>"), "should contain <mprescripts/>: $xml")
        assertTrue(xml.contains("<mn>1</mn>"), "should contain pre-subscript: $xml")
    }

    // ── stripHiddenAttrs ──────────────────────────────────────────────────────

    @Test
    fun stripHiddenAttrs_removesAttributeAndPreservesOthers() {
        listOf(
            "<math><mi>x</mi></math>" to "<math><mi>x</mi></math>",
            """<math><mi mx-data-is-hidden="true">x</mi></math>""" to "<math><mi>x</mi></math>",
            """<math><mi mx-data-is-hidden="true">a</mi><mn mx-data-is-hidden="true">2</mn></math>""" to
                "<math><mi>a</mi><mn>2</mn></math>",
        ).forEach { (input, expected) ->
            assertEquals(expected, stripHiddenAttrs(input), "stripHiddenAttrs on: $input")
        }
    }

    @Test
    fun stripHiddenAttrs_preservesOtherAttributes() {
        val input = """<math><mspace linebreak="newline" mx-data-is-hidden="true"/></math>"""
        val result = stripHiddenAttrs(input)
        assertTrue(result.contains("""linebreak="newline""""), "linebreak attr should be preserved: $result")
        assertTrue(!result.contains("mx-data-is-hidden"), "mx-data-is-hidden should be removed: $result")
    }

    // ── stripPlaceholders ─────────────────────────────────────────────────────

    @Test
    fun stripPlaceholders_removesPlaceholderMtext() {
        listOf(
            "<math><mi>x</mi></math>" to "<math><mi>x</mi></math>",
            """<math><mtext mx-is-placeholder="true">□</mtext></math>""" to "<math></math>",
            """<math><mfrac><mtext mx-is-placeholder="true">□</mtext><mi>b</mi></mfrac></math>""" to
                "<math><mfrac><mi>b</mi></mfrac></math>",
        ).forEach { (input, expected) ->
            assertEquals(expected, stripPlaceholders(input), "stripPlaceholders on: $input")
        }
    }

    // ── Empty child slots use <mrow/> placeholder ─────────────────────────────

    @Test
    fun toMathML_mfracWithNoneChild_usesEmptyMrowPlaceholder() {
        // MathNone serializes to "" — mfrac must still emit two children
        val elem = MathMfrac(numerator = MathNone, denominator = MathMi("b"))
        assertEquals("<math><mfrac><mrow/><mi>b</mi></mfrac></math>", elem.toMathML())
    }

    @Test
    fun toMathML_msupWithNoneChildren_usesEmptyMrowPlaceholders() {
        val elem = MathMsup(main = MathNone, sup = MathNone)
        assertEquals("<math><msup><mrow/><mrow/></msup></math>", elem.toMathML())
    }

    @Test
    fun toMathML_msubWithPlaceholderChild_serializesPlaceholderMtext() {
        val elem = MathMsub(main = MathMi("x"), sub = MathPlaceholder)
        assertEquals(
            "<math><msub><mi>x</mi><mtext mx-is-placeholder=\"true\">□</mtext></msub></math>",
            elem.toMathML(),
        )
    }

    // ── Roundtrip smoke test ──────────────────────────────────────────────────

    @Test
    fun toMathML_quadraticFormula_roundtrips() {
        val original =
            "<math><mfrac>" +
                "<mrow><mo>-</mo><mi>b</mi></mrow>" +
                "<mrow><mn>2</mn><mi>a</mi></mrow>" +
                "</mfrac></math>"
        val parsed = MathMLParser.parse(original)
        val reparsed = MathMLParser.parse(parsed.toMathML())
        assertEquals(parsed, reparsed, "quadratic formula should survive a serialize→parse roundtrip")
    }
}
