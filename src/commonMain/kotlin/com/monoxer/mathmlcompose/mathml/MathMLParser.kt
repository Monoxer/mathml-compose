package com.monoxer.mathmlcompose.mathml

import com.monoxer.mathmlcompose.math.MSPACE_LINEBREAK_ATTR
import com.monoxer.mathmlcompose.math.MSPACE_LINEBREAK_NEWLINE
import com.monoxer.mathmlcompose.math.MX_DATA_IS_HIDDEN
import com.monoxer.mathmlcompose.math.MX_IS_PLACEHOLDER
import com.monoxer.mathmlcompose.math.MathElement
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
import com.monoxer.mathmlcompose.math.isValidDataAttribute

/**
 * Parses a MathML XML string into a [MathElement] tree.
 * Supports the MathML subset used by Monoxer formula entries.
 */
@Suppress("CyclomaticComplexMethod", "TooManyFunctions")
object MathMLParser {
    private const val ATTRIBUTE_NAME_GROUP = 1
    private const val DOUBLE_QUOTED_VALUE_GROUP = 2
    private const val SINGLE_QUOTED_VALUE_GROUP = 3

    /** Parses [mathml], preserving valid `data-*` attributes on `mrow` and ignoring invalid ones. */
    fun parse(mathml: String): MathElement {
        val tokens = tokenize(mathml.trim())
        val (element, _) = parseTokens(tokens, 0)
        return element ?: MathMrow(emptyList())
    }

    private sealed interface Token {
        data class Open(
            val name: String,
            val attrs: Map<String, String>,
        ) : Token

        data class Close(
            val name: String,
        ) : Token

        data class SelfClose(
            val name: String,
            val attrs: Map<String, String>,
        ) : Token

        data class Text(
            val content: String,
        ) : Token
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            if (input[i] == '<') {
                val end = input.indexOf('>', i)
                if (end == -1) break
                val tagContent = input.substring(i + 1, end)
                when {
                    tagContent.startsWith('/') -> {
                        tokens.add(Token.Close(tagContent.drop(1).trim().lowercase()))
                    }

                    tagContent.endsWith('/') -> {
                        val inner = tagContent.dropLast(1).trim()
                        val (name, attrs) = splitNameAttrs(inner)
                        tokens.add(Token.SelfClose(name, attrs))
                    }

                    else -> {
                        val (name, attrs) = splitNameAttrs(tagContent.trim())
                        tokens.add(Token.Open(name, attrs))
                    }
                }
                i = end + 1
            } else {
                val end = input.indexOf('<', i).takeIf { it != -1 } ?: input.length
                val text = decodeEntities(input.substring(i, end))
                if (text.isNotBlank()) tokens.add(Token.Text(text.trim()))
                i = end
            }
        }
        return tokens
    }

    private fun splitNameAttrs(tagContent: String): Pair<String, Map<String, String>> {
        val spaceIdx = tagContent.indexOfFirst { it.isWhitespace() }
        if (spaceIdx == -1) return tagContent.lowercase() to emptyMap()
        val name = tagContent.substring(0, spaceIdx).lowercase()
        val attrStr = tagContent.substring(spaceIdx + 1)
        val attrs = mutableMapOf<String, String>()
        val regex = Regex("""([^\s=]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'))?""")
        regex.findAll(attrStr).forEach { m ->
            val attributeName = m.groupValues[ATTRIBUTE_NAME_GROUP]
            val doubleQuotedValue = m.groupValues[DOUBLE_QUOTED_VALUE_GROUP]
            val singleQuotedValue = m.groupValues[SINGLE_QUOTED_VALUE_GROUP]
            val value = doubleQuotedValue.ifEmpty { singleQuotedValue }
            attrs[attributeName] = decodeEntities(value)
        }
        return name to attrs
    }

    private fun decodeEntities(text: String): String =
        MATHML_ENTITY_PATTERN.replace(text) {
            MATHML_CHAR_BY_ENTITY[it.value]
                ?: it.value
        }

    private val MATHML_CHAR_BY_ENTITY =
        mapOf(
            "&#43;" to "+",
            "&#61;" to "=",
            "&#177;" to "±",
            "&#8722;" to "−",
            "&#8723;" to "∓",
            "&#8734;" to "∞",
            "&#960;" to "π",
            "&#8804;" to "≤",
            "&#8805;" to "≥",
            "&#8800;" to "≠",
            "&#215;" to "×",
            "&#247;" to "÷",
            "&#8730;" to "√",
            "&#8747;" to "∫",
            "&#8721;" to "∑",
            "&#8719;" to "∏",
            "&#945;" to "α",
            "&#946;" to "β",
            "&#947;" to "γ",
            "&#948;" to "δ",
            "&#952;" to "θ",
            "&#955;" to "λ",
            "&#956;" to "μ",
            "&#963;" to "σ",
            "&#964;" to "τ",
            "&#969;" to "ω",
            "&lt;" to "<",
            "&gt;" to ">",
            "&amp;" to "&",
            "&quot;" to "\"",
            "&apos;" to "'",
            "&pi;" to "π",
            "&infin;" to "∞",
        )

    /** Matches numeric (`&#N;`) and named (`&name;`) XML/HTML entity references for single-pass decoding. */
    private val MATHML_ENTITY_PATTERN = Regex("""&#\d+;|&\w+;""")

    private fun parseTokens(
        tokens: List<Token>,
        startIndex: Int,
    ): Pair<MathElement?, Int> {
        if (startIndex >= tokens.size) return null to startIndex
        return when (val token = tokens[startIndex]) {
            is Token.Open -> parseOpenElement(tokens, startIndex, token)
            is Token.SelfClose -> parseSelfClose(token) to startIndex + 1
            is Token.Text -> MathMtext(token.content) to startIndex + 1
            is Token.Close -> null to startIndex
        }
    }

    private fun parseOpenElement(
        tokens: List<Token>,
        startIndex: Int,
        open: Token.Open,
    ): Pair<MathElement?, Int> {
        val hidden = open.attrs[MX_DATA_IS_HIDDEN] == "true"
        val (children, nextIndex) =
            consumeChildren(tokens, startIndex + 1, open.name)
        return if (hidden) {
            // Preserve tree structure but replace every leaf with MathNone so each
            // hidden slot renders as a separate blank box in the input area.
            val built = buildElement(open.name, open.attrs, children)
            replaceAllLeavesWithNone(built) to nextIndex
        } else {
            buildElement(open.name, open.attrs, children) to nextIndex
        }
    }

    /** Recursively replaces every leaf element with [MathNone]. */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun replaceAllLeavesWithNone(element: MathElement): MathElement =
        when (element) {
            is MathMn -> {
                if (element.text.length > 1) MathMrow(element.text.map { MathNone }) else MathNone
            }

            is MathMi, is MathMo, is MathMtext -> {
                MathNone
            }

            is MathMrow -> {
                element.copy(children = element.children.map { replaceAllLeavesWithNone(it) })
            }

            is MathMfrac -> {
                element.copy(
                    numerator = replaceAllLeavesWithNone(element.numerator),
                    denominator = replaceAllLeavesWithNone(element.denominator),
                )
            }

            is MathMsqrt -> {
                element.copy(main = replaceAllLeavesWithNone(element.main))
            }

            is MathMroot -> {
                element.copy(
                    main = replaceAllLeavesWithNone(element.main),
                    root = replaceAllLeavesWithNone(element.root),
                )
            }

            is MathMsup -> {
                element.copy(
                    main = replaceAllLeavesWithNone(element.main),
                    sup = replaceAllLeavesWithNone(element.sup),
                )
            }

            is MathMsub -> {
                element.copy(
                    main = replaceAllLeavesWithNone(element.main),
                    sub = replaceAllLeavesWithNone(element.sub),
                )
            }

            is MathMsubsup -> {
                element.copy(
                    main = replaceAllLeavesWithNone(element.main),
                    sub = replaceAllLeavesWithNone(element.sub),
                    sup = replaceAllLeavesWithNone(element.sup),
                )
            }

            is MathMover -> {
                element.copy(
                    main = replaceAllLeavesWithNone(element.main),
                    over = replaceAllLeavesWithNone(element.over),
                )
            }

            is MathMunder -> {
                element.copy(
                    main = replaceAllLeavesWithNone(element.main),
                    under = replaceAllLeavesWithNone(element.under),
                )
            }

            is MathMunderover -> {
                element.copy(
                    main = replaceAllLeavesWithNone(element.main),
                    under = replaceAllLeavesWithNone(element.under),
                    over = replaceAllLeavesWithNone(element.over),
                )
            }

            is MathMmultiscriptsRegularized -> {
                element.copy(
                    base = replaceAllLeavesWithNone(element.base),
                    postSubScripts = replaceAllLeavesWithNone(element.postSubScripts),
                    postSupScripts = replaceAllLeavesWithNone(element.postSupScripts),
                    preSubScripts = replaceAllLeavesWithNone(element.preSubScripts),
                    preSupScripts = replaceAllLeavesWithNone(element.preSupScripts),
                )
            }

            else -> {
                element
            }
        }

    private fun consumeChildren(
        tokens: List<Token>,
        startIndex: Int,
        parentName: String,
    ): Pair<List<MathElement>, Int> {
        val children = mutableListOf<MathElement>()
        var i = startIndex
        while (i < tokens.size) {
            if (tokens[i] is Token.Close && (tokens[i] as Token.Close).name == parentName) {
                return children to i + 1
            }
            val prev = i
            val (child, nextI) = parseTokens(tokens, i)
            if (child != null) children.add(child)
            i = if (nextI > prev) nextI else nextI + 1
        }
        return children to i
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun buildElement(
        name: String,
        attrs: Map<String, String>,
        children: List<MathElement>,
    ): MathElement {
        val leafText = (children.singleOrNull() as? MathMtext)?.text ?: ""
        return when (name) {
            "math", "mstyle", "mphantom" -> {
                wrapChildren(children)
            }

            "mrow" -> {
                val dataAttributes = attrs.validDataAttributes()
                if (dataAttributes.isNotEmpty()) {
                    MathMrow(
                        children = children,
                        attributes = dataAttributes,
                    )
                } else {
                    wrapChildren(children)
                }
            }

            "mi" -> {
                MathMi(leafText)
            }

            "mo" -> {
                MathMo(leafText)
            }

            "mn" -> {
                MathMn(leafText)
            }

            "mtext" -> {
                if (attrs[MX_IS_PLACEHOLDER] == "true") MathPlaceholder else MathMtext(leafText)
            }

            "mfrac" -> {
                MathMfrac(
                    numerator = children.getOrElse(0) { MathNone },
                    denominator = children.getOrElse(1) { MathNone },
                )
            }

            "msqrt" -> {
                MathMsqrt(wrapChildren(children))
            }

            "mroot" -> {
                MathMroot(
                    main = children.getOrElse(0) { MathNone },
                    root = children.getOrElse(1) { MathNone },
                )
            }

            "msup" -> {
                MathMsup(
                    main = children.getOrElse(0) { MathNone },
                    sup = children.getOrElse(1) { MathNone },
                )
            }

            "msub" -> {
                MathMsub(
                    main = children.getOrElse(0) { MathNone },
                    sub = children.getOrElse(1) { MathNone },
                )
            }

            "msubsup" -> {
                MathMsubsup(
                    main = children.getOrElse(0) { MathNone },
                    sub = children.getOrElse(1) { MathNone },
                    sup = children.getOrElse(2) { MathNone },
                )
            }

            "mover" -> {
                MathMover(
                    main = children.getOrElse(0) { MathNone },
                    over = children.getOrElse(1) { MathNone },
                )
            }

            "munder" -> {
                MathMunder(
                    main = children.getOrElse(0) { MathNone },
                    under = children.getOrElse(1) { MathNone },
                )
            }

            "munderover" -> {
                MathMunderover(
                    main = children.getOrElse(0) { MathNone },
                    under = children.getOrElse(1) { MathNone },
                    over = children.getOrElse(2) { MathNone },
                )
            }

            "mspace" -> {
                MathMspace(isLinebreak = attrs[MSPACE_LINEBREAK_ATTR] == MSPACE_LINEBREAK_NEWLINE)
            }

            "none" -> {
                MathStructuralNone
            }

            "mmultiscripts" -> {
                parseMmultiscripts(children)
            }

            else -> {
                wrapChildren(children)
            }
        }
    }

    private fun parseSelfClose(token: Token.SelfClose): MathElement =
        when (token.name) {
            "mrow" -> parseSelfClosingMrow(token)

            "mspace" -> MathMspace(isLinebreak = token.attrs[MSPACE_LINEBREAK_ATTR] == MSPACE_LINEBREAK_NEWLINE)

            // <none/> marks an empty script slot inside <mmultiscripts>; use MathStructuralNone
            // so it remains distinguishable from hidden-input MathNone (used as blank input slots).
            "none" -> MathStructuralNone

            // <mprescripts/> is the boundary marker between post-scripts and pre-scripts.
            // Use a dedicated type so parseMmultiscripts can distinguish it from hidden-input
            // MathNone slots (which also appear as MathNone after replaceAllLeavesWithNone).
            "mprescripts" -> MathMprescriptsMarker

            else -> MathNone
        }

    private fun parseSelfClosingMrow(token: Token.SelfClose): MathElement =
        token.attrs.validDataAttributes().let { dataAttributes ->
            if (dataAttributes.isNotEmpty()) {
                MathMrow(emptyList(), dataAttributes)
            } else {
                MathNone
            }
        }

    private fun Map<String, String>.validDataAttributes(): Map<String, String> =
        filter { (name, value) -> isValidDataAttribute(name, value) }

    private fun wrapChildren(children: List<MathElement>): MathElement =
        when (children.size) {
            0 -> MathMrow(emptyList())
            1 -> children.first()
            else -> MathMrow(children)
        }

    private fun parseMmultiscripts(children: List<MathElement>): MathElement {
        // format: base [postSub postSup]* [<mprescripts/> preSub preSup]*
        // <none/>        → MathStructuralNone (empty script slot, not an input slot)
        // <mprescripts/> → MathMprescriptsMarker (dedicated boundary marker, never stored in tree)
        val prescriptsIdx =
            children
                .drop(1)
                .indexOfFirst { it is MathMprescriptsMarker }
                .takeIf { it != -1 }
                ?.plus(1) ?: -1
        val base = children.getOrElse(0) { MathStructuralNone }
        return MathMmultiscriptsRegularized(
            base = base,
            postSubScripts = children.getOrElse(1) { MathStructuralNone },
            postSupScripts = children.getOrElse(2) { MathStructuralNone },
            preSubScripts =
                if (prescriptsIdx >= 0) {
                    children.getOrElse(prescriptsIdx + 1) { MathStructuralNone }
                } else {
                    MathStructuralNone
                },
            preSupScripts =
                if (prescriptsIdx >= 0) {
                    children.getOrElse(prescriptsIdx + 2) { MathStructuralNone }
                } else {
                    MathStructuralNone
                },
        )
    }
}
