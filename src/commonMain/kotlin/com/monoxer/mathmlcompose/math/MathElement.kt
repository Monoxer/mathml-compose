package com.monoxer.mathmlcompose.math

// MathML elements as defined by the W3C MathML standard: https://www.w3.org/TR/MathML/
sealed interface MathElement {
    fun minText(): String
}

data class MathMi(
    val text: String,
) : MathElement {
    override fun minText(): String = text
}

data class MathMo(
    val text: String,
) : MathElement {
    override fun minText(): String = text
}

data class MathMn(
    val text: String,
) : MathElement {
    override fun minText(): String = text
}

data class MathMtext(
    val text: String,
) : MathElement {
    override fun minText(): String = text
}

data class MathMsup(
    val main: MathElement,
    val sup: MathElement,
) : MathElement {
    override fun minText(): String = "${main.minText()}^(${sup.minText()})"
}

data class MathMsub(
    val main: MathElement,
    val sub: MathElement,
) : MathElement {
    override fun minText(): String = "${main.minText()}_(${sub.minText()})"
}

data class MathMsubsup(
    val main: MathElement,
    val sub: MathElement,
    val sup: MathElement,
) : MathElement {
    override fun minText(): String = "${main.minText()}_(${sub.minText()})^(${sup.minText()})"
}

data class MathMsqrt(
    val main: MathElement,
) : MathElement {
    override fun minText(): String = "(${main.minText()})^(1/2)"
}

data class MathMroot(
    val main: MathElement,
    val root: MathElement,
) : MathElement {
    override fun minText(): String = "(${main.minText()})^(1/(${root.minText()}))"
}

data class MathMfrac(
    val numerator: MathElement,
    val denominator: MathElement,
) : MathElement {
    override fun minText(): String = "(${numerator.minText()})/(${denominator.minText()})"
}

data class MathMrow(
    val children: List<MathElement>,
) : MathElement {
    override fun minText(): String = children.joinToString("") { it.minText() }
}

/**
 * An [MathMrow] variant that carries a [ScoringCriterion] from the `mx-judge` MathML attribute.
 * Used to mark sub-expressions that should be evaluated with a specific scoring criterion.
 */
data class MathMrowWithCriterion(
    val children: List<MathElement>,
    val criterion: Int,
) : MathElement {
    override fun minText(): String = children.joinToString("") { it.minText() }
}

data class MathMover(
    val main: MathElement,
    val over: MathElement,
) : MathElement {
    override fun minText(): String = "${main.minText()}^(${over.minText()})"
}

data class MathMunder(
    val main: MathElement,
    val under: MathElement,
) : MathElement {
    override fun minText(): String = "${main.minText()}_(${under.minText()})"
}

data class MathMunderover(
    val main: MathElement,
    val under: MathElement,
    val over: MathElement,
) : MathElement {
    override fun minText(): String = "${main.minText()}_(${under.minText()})^(${over.minText()})"
}

data class MathMspace(
    val isLinebreak: Boolean = false,
) : MathElement {
    override fun minText(): String = ""
}

data class MathMmultiscriptsRegularized(
    val base: MathElement,
    val postSubScripts: MathElement,
    val postSupScripts: MathElement,
    val preSubScripts: MathElement,
    val preSupScripts: MathElement,
) : MathElement {
    override fun minText(): String = base.minText()
}

data object MathNone : MathElement {
    override fun minText(): String = ""
}

/**
 * Structural empty placeholder used by `<none/>` inside `<mmultiscripts>` to mark
 * empty script positions. Distinct from [MathNone] (which marks hidden input slots).
 */
data object MathStructuralNone : MathElement {
    override fun minText(): String = ""
}

/**
 * Transient boundary marker produced by `<mprescripts/>` during MathML parsing.
 * Used only inside [MathMLParser.parseMmultiscripts] to locate the prescripts boundary
 * in the children list. Never stored in the final [MathElement] tree.
 */
internal data object MathMprescriptsMarker : MathElement {
    override fun minText(): String = ""
}

data object MathPlaceholder : MathElement {
    override fun minText(): String = ""
}

data object MathCursor : MathElement {
    override fun minText(): String = ""
}

/**
 * A hint input slot that has been filled by the user.
 * Renders [child] with a dashed border box so the user can distinguish their
 * typed values from the fixed parts of the hint formula.
 */
data class MathFilledSlot(
    val child: MathElement,
) : MathElement {
    override fun minText(): String = child.minText()
}
