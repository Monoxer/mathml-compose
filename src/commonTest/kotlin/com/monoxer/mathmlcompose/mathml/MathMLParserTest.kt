package com.monoxer.mathmlcompose.mathml

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
import kotlin.test.assertIs

class MathMLParserTest {
    // ── Leaf elements ──────────────────────────────────────────────────────────

    @Test
    fun parse_leafElements_returnsCorrectNode() {
        listOf(
            "<math><mi>x</mi></math>" to MathMi("x"),
            "<math><mo>+</mo></math>" to MathMo("+"),
            "<math><mn>42</mn></math>" to MathMn("42"),
            "<math><mtext>hello</mtext></math>" to MathMtext("hello"),
        ).forEach { (input, expected) ->
            assertEquals(expected, MathMLParser.parse(input), "parsing: $input")
        }
    }

    // ── Composite elements ─────────────────────────────────────────────────────

    @Test
    fun parse_compositeElements_returnsCorrectTree() {
        listOf(
            "<math><mfrac><mi>a</mi><mi>b</mi></mfrac></math>" to
                MathMfrac(MathMi("a"), MathMi("b")),
            "<math><msqrt><mi>x</mi></msqrt></math>" to
                MathMsqrt(MathMi("x")),
            "<math><mroot><mi>x</mi><mn>3</mn></mroot></math>" to
                MathMroot(main = MathMi("x"), root = MathMn("3")),
            "<math><msup><mi>x</mi><mn>2</mn></msup></math>" to
                MathMsup(main = MathMi("x"), sup = MathMn("2")),
            "<math><msub><mi>x</mi><mn>0</mn></msub></math>" to
                MathMsub(main = MathMi("x"), sub = MathMn("0")),
            "<math><msubsup><mi>x</mi><mn>0</mn><mn>2</mn></msubsup></math>" to
                MathMsubsup(main = MathMi("x"), sub = MathMn("0"), sup = MathMn("2")),
            "<math><mover><mi>x</mi><mo>→</mo></mover></math>" to
                MathMover(main = MathMi("x"), over = MathMo("→")),
            "<math><munder><mi>x</mi><mo>_</mo></munder></math>" to
                MathMunder(main = MathMi("x"), under = MathMo("_")),
            "<math><munderover><mi>x</mi><mn>0</mn><mn>1</mn></munderover></math>" to
                MathMunderover(main = MathMi("x"), under = MathMn("0"), over = MathMn("1")),
        ).forEach { (input, expected) ->
            assertEquals(expected, MathMLParser.parse(input), "parsing: $input")
        }
    }

    // ── mrow wrapping ──────────────────────────────────────────────────────────

    @Test
    fun parse_emptyMath_returnsMrowEmpty() {
        assertEquals(MathMrow(emptyList()), MathMLParser.parse("<math></math>"))
    }

    @Test
    fun parse_multipleChildrenUnderMath_wrappedInMrow() {
        val result = MathMLParser.parse("<math><mi>a</mi><mo>+</mo><mi>b</mi></math>")
        assertIs<MathMrow>(result)
        assertEquals(3, result.children.size)
    }

    @Test
    fun parse_explicitMrow_preservesChildren() {
        val result = MathMLParser.parse("<math><mrow><mi>a</mi><mi>b</mi></mrow></math>")
        assertIs<MathMrow>(result)
        assertEquals(2, result.children.size)
    }

    // ── mspace ────────────────────────────────────────────────────────────────

    @Test
    fun parse_mspace_outputMatchesLinebreakFlag() {
        listOf(
            "<math><mspace/></math>" to MathMspace(isLinebreak = false),
            "<math><mspace linebreak=\"newline\"/></math>" to MathMspace(isLinebreak = true),
        ).forEach { (input, expected) ->
            assertEquals(expected, MathMLParser.parse(input), "parsing: $input")
        }
    }

    // ── Entity decoding ───────────────────────────────────────────────────────

    @Test
    fun parse_entities_decodedCorrectly() {
        listOf(
            "<math><mn>&#177;</mn></math>" to MathMn("±"),
            "<math><mo>&lt;</mo></math>" to MathMo("<"),
            "<math><mo>&gt;</mo></math>" to MathMo(">"),
            "<math><mi>&amp;</mi></math>" to MathMi("&"),
            "<math><mi>&pi;</mi></math>" to MathMi("π"),
            "<math><mo>&#8722;</mo></math>" to MathMo("−"),
        ).forEach { (input, expected) ->
            assertEquals(expected, MathMLParser.parse(input), "entity decoding in: $input")
        }
    }

    // ── mx-data-is-hidden ─────────────────────────────────────────────────────

    @Test
    fun parse_hiddenSingleCharLeaves_replacedWithMathNone() {
        listOf(
            """<math><mi mx-data-is-hidden="true">x</mi></math>""",
            """<math><mn mx-data-is-hidden="true">5</mn></math>""",
            """<math><mo mx-data-is-hidden="true">+</mo></math>""",
        ).forEach { input ->
            assertIs<MathNone>(MathMLParser.parse(input), "expected MathNone for: $input")
        }
    }

    @Test
    fun parse_hiddenMultiCharMn_replacedWithMrowOfNones() {
        val result = MathMLParser.parse("""<math><mn mx-data-is-hidden="true">42</mn></math>""")
        assertIs<MathMrow>(result)
        assertEquals(2, result.children.size, "each digit should produce one MathNone")
        result.children.forEach { assertIs<MathNone>(it) }
    }

    @Test
    fun parse_visibleElement_notReplacedWithNone() {
        val result = MathMLParser.parse("<math><mi>x</mi></math>")
        assertEquals(MathMi("x"), result)
    }

    // ── mmultiscripts ─────────────────────────────────────────────────────────

    @Test
    fun parse_mmultiscripts_withoutPrescripts_baseAndStructuralNones() {
        val result =
            MathMLParser.parse(
                "<math><mmultiscripts><mi>x</mi><none/><none/></mmultiscripts></math>",
            )
        assertIs<MathMmultiscriptsRegularized>(result)
        assertEquals(MathMi("x"), result.base)
        assertIs<MathStructuralNone>(result.postSubScripts)
        assertIs<MathStructuralNone>(result.postSupScripts)
    }

    @Test
    fun parse_mmultiscripts_withPrescripts_preScriptsPopulated() {
        val result =
            MathMLParser.parse(
                "<math><mmultiscripts><mi>A</mi><none/><none/><mprescripts/>" +
                    "<mn>1</mn><mn>2</mn></mmultiscripts></math>",
            )
        assertIs<MathMmultiscriptsRegularized>(result)
        assertEquals(MathMi("A"), result.base)
        assertEquals(MathMn("1"), result.preSubScripts)
        assertEquals(MathMn("2"), result.preSupScripts)
    }

    @Test
    fun parse_hiddenMultiCharMnWithDecimal_replacedWithMrowOfNones() {
        // "2.5" has 3 chars → MathMrow of 3 MathNones; this is what causes the "." key bug
        // when collectHiddenLeaves pairs this MathMrow with the full MathMn("2.5")
        val result = MathMLParser.parse("""<math><mn mx-data-is-hidden="true">2.5</mn></math>""")
        assertIs<MathMrow>(result)
        assertEquals(3, result.children.size)
        result.children.forEach { assertIs<MathNone>(it) }
    }

    @Test
    fun parse_hiddenCompositeElement_allLeavesReplacedWithNone() {
        // Hidden on a composite element (mfrac) — entire subtree becomes Nones
        val result =
            MathMLParser.parse(
                """<math><mfrac mx-data-is-hidden="true"><mi>a</mi><mi>b</mi></mfrac></math>""",
            )
        assertIs<MathMfrac>(result)
        assertIs<MathNone>(result.numerator)
        assertIs<MathNone>(result.denominator)
    }

    // ── Unknown tags ──────────────────────────────────────────────────────────

    @Test
    fun parse_unknownTag_childrenWrappedInMrow() {
        // Unknown tags fall back to wrapping children — should not crash
        val result = MathMLParser.parse("<math><custom><mi>x</mi><mi>y</mi></custom></math>")
        assertIs<MathMrow>(result)
        assertEquals(2, result.children.size)
    }

    // ── Complex formula smoke tests ───────────────────────────────────────────

    @Test
    fun parse_complexFormula_cosTrigWithDecimalAndFrac() {
        // cos2.5π < sin2.5π > tan(-π/3) + √(x_0) — covers mrow, mi, mn(decimal), mo, mfrac, msqrt, msub
        val mathml =
            "<math><mrow>" +
                "<mi>cos</mi><mn>2.5</mn><mi>π</mi>" +
                "<mo>&lt;</mo>" +
                "<mi>sin</mi><mn>2.5</mn><mi>π</mi>" +
                "<mo>&gt;</mo>" +
                "<mi>tan</mi><mo>(</mo><mo>-</mo>" +
                "<mfrac><mi>π</mi><mn>3</mn></mfrac>" +
                "<mo>)</mo>" +
                "<mo>+</mo>" +
                "<msqrt><msub><mi>x</mi><mn>0</mn></msub></msqrt>" +
                "</mrow></math>"
        val result = assertIs<MathMrow>(MathMLParser.parse(mathml))
        assertEquals(15, result.children.size)
        assertIs<MathMfrac>(result.children[11])
        val sqrt = assertIs<MathMsqrt>(result.children[14])
        assertIs<MathMsub>(sqrt.main)
    }

    @Test
    fun parse_complexFormula_partiallyHidden_mixedVisibility() {
        // cos hidden, 2.5 hidden (→ MathMrow of 3 Nones), π visible, √(x_0) hidden — covers
        // mixed visibility including msqrt+msub all-leaves-replaced
        val mathml =
            "<math><mrow>" +
                """<mi mx-data-is-hidden="true">cos</mi>""" +
                """<mn mx-data-is-hidden="true">2.5</mn>""" +
                "<mi>π</mi>" +
                """<msqrt mx-data-is-hidden="true"><msub><mi>x</mi><mn>0</mn></msub></msqrt>""" +
                "</mrow></math>"
        val result = assertIs<MathMrow>(MathMLParser.parse(mathml))
        assertIs<MathNone>(result.children[0]) // cos → single None
        assertIs<MathMrow>(result.children[1]) // 2.5 → MathMrow([None,None,None])
        assertEquals(3, (result.children[1] as MathMrow).children.size)
        assertEquals(MathMi("π"), result.children[2]) // π visible
        val sqrt = assertIs<MathMsqrt>(result.children[3])
        val sub = assertIs<MathMsub>(sqrt.main) // msub structure preserved, leaves replaced with None
        assertIs<MathNone>(sub.main)
        assertIs<MathNone>(sub.sub)
    }

    // ── mx-is-placeholder ────────────────────────────────────────────────────

    @Test
    fun parse_mtextWithIsPlaceholder_returnsMathPlaceholder() {
        val result = MathMLParser.parse("""<math><mtext mx-is-placeholder="true">□</mtext></math>""")
        assertIs<MathPlaceholder>(result)
    }

    @Test
    fun parse_mtextWithoutIsPlaceholder_returnsMathMtext() {
        val result = MathMLParser.parse("<math><mtext>hello</mtext></math>")
        assertEquals(MathMtext("hello"), result)
    }

    // ── data mrow attributes ─────────────────────────────────────────────────

    @Test
    fun parse_preservesAllDataAttributes() {
        val result =
            MathMLParser.parse(
                """<math><mrow data-mode="numeric" data-source="Answer_123-value"><mi>x</mi></mrow></math>""",
            )

        assertEquals(
            MathMrow(
                children = listOf(MathMi("x")),
                attributes = mapOf("data-mode" to "numeric", "data-source" to "Answer_123-value"),
            ),
            result,
        )
    }

    @Test
    fun parse_ignoresNonDataAttributes() {
        val result = MathMLParser.parse("""<math><mrow custom-meta="value"><mi>x</mi><mi>y</mi></mrow></math>""")

        assertEquals(MathMrow(listOf(MathMi("x"), MathMi("y"))), result)
    }

    @Test
    fun parse_selfClosingMrow_preservesAllDataAttributes() {
        val result =
            MathMLParser.parse(
                """<math><mrow data-mode="numeric" data-source="answer"/></math>""",
            )

        assertEquals(
            MathMrow(
                emptyList(),
                mapOf("data-mode" to "numeric", "data-source" to "answer"),
            ),
            result,
        )
        assertEquals(result, MathMLParser.parse(result.toMathML()))
    }

    @Test
    fun parse_invalidDataAttributes_ignoresThemForOpenAndSelfClosingMrows() {
        val cases =
            listOf(
                "empty value" to """data-meta=""""",
                "space in value" to """data-meta="any value"""",
                "dot in value" to """data-meta="value.with.dot"""",
                "punctuation in value" to """data-meta="value!"""",
                "non-ASCII value" to """data-meta="日本語"""",
                "decoded XML special characters" to """data-meta='&quot;&apos;&amp;&lt;&gt;'""",
                "empty name suffix" to """data-="value"""",
                "dot in name" to """data-mode.="value"""",
                "punctuation in name" to """data-mode!="value"""",
                "uppercase name" to """data-Mode="value"""",
            )

        cases.forEach { (description, attribute) ->
            assertEquals(
                MathMi("x"),
                MathMLParser.parse("<math><mrow $attribute><mi>x</mi></mrow></math>"),
                "Expected $description to be ignored on an open mrow: $attribute",
            )
            assertEquals(
                MathNone,
                MathMLParser.parse("<math><mrow $attribute/></math>"),
                "Expected $description to be ignored on a self-closing mrow: $attribute",
            )
        }
    }

    @Test
    fun parse_mixedDataAttributes_preservesOnlyValidAttributes() {
        val openResult =
            MathMLParser.parse(
                """<math><mrow data-mode="numeric" data-note="invalid value" data-Mode="strict">""" +
                    """<mi>x</mi></mrow></math>""",
            )
        val selfClosingResult =
            MathMLParser.parse(
                """<math><mrow data-mode="numeric" data-note="日本語" data-mode.="strict"/></math>""",
            )

        assertEquals(
            MathMrow(listOf(MathMi("x")), mapOf("data-mode" to "numeric")),
            openResult,
        )
        assertEquals(
            MathMrow(emptyList(), mapOf("data-mode" to "numeric")),
            selfClosingResult,
        )
    }

    @Test
    fun parse_hiddenMrowWithDataAttributes_allLeavesReplacedWithNone() {
        val result =
            MathMLParser.parse(
                mathml =
                    """<math><mrow data-meta="value" mx-data-is-hidden="true">""" +
                        """<mi>x</mi><mn>1</mn></mrow></math>""",
            )
        val row = assertIs<MathMrow>(result)
        assertEquals(mapOf("data-meta" to "value"), row.attributes)
        row.children.forEach { assertIs<MathNone>(it) }
        val reparsed = assertIs<MathMrow>(MathMLParser.parse(result.toMathML()))
        assertEquals(row.attributes, reparsed.attributes)
    }

    // ── mstyle / mphantom passthrough ─────────────────────────────────────────

    @Test
    fun parse_mstyle_treatedAsMrow() {
        assertEquals(MathMi("x"), MathMLParser.parse("<math><mstyle><mi>x</mi></mstyle></math>"))
    }

    // ── Quadratic formula smoke test ──────────────────────────────────────────

    @Test
    fun parse_quadraticFormula_topLevelIsMfrac() {
        val mathml =
            "<math><mfrac>" +
                "<mrow><mo>-</mo><mi>b</mi><mo>&#177;</mo><msqrt><mrow><msup><mi>b</mi><mn>2</mn></msup><mo>-</mo>" +
                "<mn>4</mn><mi>a</mi><mi>c</mi></mrow></msqrt></mrow>" +
                "<mrow><mn>2</mn><mi>a</mi></mrow>" +
                "</mfrac></math>"
        val result = MathMLParser.parse(mathml)
        assertIs<MathMfrac>(result)
        assertIs<MathMrow>(result.numerator)
        assertIs<MathMrow>(result.denominator)
    }
}
