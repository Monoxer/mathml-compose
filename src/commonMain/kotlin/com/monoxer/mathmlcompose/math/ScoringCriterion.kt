package com.monoxer.mathmlcompose.math

object ScoringCriterion {
    const val STRICT = 0
    const val NUMERIC = 1
    const val NUMERIC_AND_SYMBOLIC = 3
    const val NUMERIC_AND_SIMPLIFIED = 5
}

fun convertStringToCriterion(value: String): Int? =
    when (value) {
        "strict" -> ScoringCriterion.STRICT
        "numeric" -> ScoringCriterion.NUMERIC
        "symbolic" -> ScoringCriterion.NUMERIC_AND_SYMBOLIC
        "simplified" -> ScoringCriterion.NUMERIC_AND_SIMPLIFIED
        else -> null
    }

fun criterionToString(criterion: Int): String =
    when (criterion) {
        ScoringCriterion.STRICT -> "strict"
        ScoringCriterion.NUMERIC -> "numeric"
        ScoringCriterion.NUMERIC_AND_SYMBOLIC -> "symbolic"
        ScoringCriterion.NUMERIC_AND_SIMPLIFIED -> "simplified"
        else -> throw IllegalArgumentException("Unknown scoring criterion: $criterion")
    }
