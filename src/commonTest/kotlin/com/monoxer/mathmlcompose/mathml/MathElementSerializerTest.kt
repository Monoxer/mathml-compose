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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    // ── MathMrow data attributes ─────────────────────────────────────────────

    @Test
    fun toMathML_mrowWithDataAttributes_preservesAttributesInSortedOrder() {
        val element =
            MathMrow(
                children = listOf(MathMi("x")),
                attributes = linkedMapOf("data-zeta" to "last", "data-alpha" to "first"),
            )

        assertEquals(
            """<math><mrow data-alpha="first" data-zeta="last"><mi>x</mi></mrow></math>""",
            element.toMathML(),
        )
    }

    @Test
    fun toMathML_mrowWithDataAttributes_serializesValidValueAndRoundtrips() {
        val element =
            MathMfrac(
                numerator =
                    MathMrow(
                        children = listOf(MathMi("x")),
                        attributes = mapOf("data-meta" to "Alpha_123-value"),
                    ),
                denominator = MathMn("2"),
            )
        val xml = element.toMathML()

        assertEquals(
            """<math><mfrac><mrow data-meta="Alpha_123-value"><mi>x</mi></mrow><mn>2</mn></mfrac></math>""",
            xml,
        )
        assertEquals(element, MathMLParser.parse(xml))
    }

    @Test
    fun mrowWithDataAttributes_withInvalidAttributes_throws() {
        val cases =
            listOf(
                "equals sign in name" to mapOf("data-data=meta" to "value"),
                "missing data prefix" to mapOf("custom-meta" to "value"),
                "empty name suffix" to mapOf("data-" to "value"),
                "uppercase name" to mapOf("data-Meta" to "value"),
                "empty value" to mapOf("data-meta" to ""),
                "space in value" to mapOf("data-meta" to "any value"),
                "punctuation in value" to mapOf("data-meta" to "value!"),
                "XML special characters in value" to mapOf("data-meta" to "\"'&<>"),
            )

        cases.forEach { (description, attributes) ->
            assertFailsWith<IllegalArgumentException>("Expected $description to be rejected") {
                MathMrow(listOf(MathMi("x")), attributes)
            }
        }
    }

    @Test
    fun toMathML_mrowWithDataAttributes_mutatedToInvalidAttributes_throws() {
        val cases =
            listOf(
                "invalid name" to mapOf("data-meta!" to "injected"),
                "invalid value" to mapOf("data-meta" to "invalid value"),
            )

        cases.forEach { (description, mutatedAttributes) ->
            val attributes = mutableMapOf("data-meta" to "value")
            val element = MathMrow(listOf(MathMi("x")), attributes)
            attributes.clear()
            attributes.putAll(mutatedAttributes)

            assertFailsWith<IllegalArgumentException>("Expected mutated $description to be rejected") {
                element.toMathML()
            }
        }
    }

    @Test
    fun toMathML_mrowWithEmptyAttributes_usesOriginalFoldingBehavior() {
        assertEquals("<math></math>", MathMrow(emptyList(), emptyMap()).toMathML())
        assertEquals("<math><mi>x</mi></math>", MathMrow(listOf(MathMi("x")), emptyMap()).toMathML())
    }

    @Test
    fun toMathML_mrowWithDataAttributes_preservesTagForAnyChildCount() {
        val attributes = mapOf("data-meta" to "value")

        assertEquals(
            """<math><mrow data-meta="value"></mrow></math>""",
            MathMrow(emptyList(), attributes).toMathML(),
        )
        assertEquals(
            """<math><mrow data-meta="value"><mi>x</mi></mrow></math>""",
            MathMrow(listOf(MathMi("x")), attributes).toMathML(),
        )
        assertEquals(
            """<math><mrow data-meta="value"><mi>x</mi><mo>+</mo><mn>1</mn></mrow></math>""",
            MathMrow(listOf(MathMi("x"), MathMo("+"), MathMn("1")), attributes).toMathML(),
        )
    }

    @Test
    fun toMathML_mrowMutatedToEmptyAttributes_usesOriginalFoldingBehavior() {
        val attributes = mutableMapOf("data-meta" to "value")
        val element = MathMrow(listOf(MathMi("x")), attributes)
        attributes.clear()

        assertEquals("<math><mi>x</mi></math>", element.toMathML())
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
    fun toMathML_mfracWithNoneChild_usesEmptyMrowPlaceholderAndRoundtrips() {
        // MathNone serializes to "" — mfrac must still emit two children
        val elem = MathMfrac(numerator = MathNone, denominator = MathMi("b"))
        val mathml = elem.toMathML()

        assertEquals("<math><mfrac><mrow/><mi>b</mi></mfrac></math>", mathml)
        assertEquals(elem, MathMLParser.parse(mathml))
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
