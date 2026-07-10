package com.monoxer.mathmlcompose.mxhtml

import kotlin.test.Test
import kotlin.test.assertEquals

class SplitMxhtmlSegmentsTest {
    @Test
    fun emptyString_returnsEmptyList() {
        assertEquals(emptyList(), splitMxhtmlSegments(""))
    }

    @Test
    fun plainText_returnsSingleSegment() {
        assertEquals(listOf("hello world"), splitMxhtmlSegments("hello world"))
    }

    @Test
    fun pureMath_returnsSingleMathSegment() {
        val math = "<math><mi>x</mi></math>"
        assertEquals(listOf(math), splitMxhtmlSegments(math))
    }

    @Test
    fun textBeforeMath_returnsTwoSegments() {
        val input = "solve: <math><mi>x</mi></math>"
        assertEquals(listOf("solve: ", "<math><mi>x</mi></math>"), splitMxhtmlSegments(input))
    }

    @Test
    fun textAfterMath_returnsTwoSegments() {
        val input = "<math><mi>x</mi></math> is the answer"
        assertEquals(listOf("<math><mi>x</mi></math>", " is the answer"), splitMxhtmlSegments(input))
    }

    @Test
    fun textBetweenTwoMaths_returnsThreeSegments() {
        val input = "<math><mi>a</mi></math> and <math><mi>b</mi></math>"
        assertEquals(
            listOf("<math><mi>a</mi></math>", " and ", "<math><mi>b</mi></math>"),
            splitMxhtmlSegments(input),
            "text between two math blocks should produce 3 segments",
        )
    }

    @Test
    fun textSurroundingMath_returnsThreeSegments() {
        val input = "left <math><mi>x</mi></math> right"
        assertEquals(
            listOf("left ", "<math><mi>x</mi></math>", " right"),
            splitMxhtmlSegments(input),
        )
    }

    @Test
    fun unclosedMath_returnsTextPrefixAndRemainingMath() {
        // <math> without </math> — remainder from mathStart is kept as-is
        val input = "prefix <math><mi>x</mi>"
        assertEquals(listOf("prefix ", "<math><mi>x</mi>"), splitMxhtmlSegments(input))
    }

    @Test
    fun consecutiveMaths_noTextBetween_returnsTwoMathSegments() {
        val a = "<math><mi>a</mi></math>"
        val b = "<math><mi>b</mi></math>"
        assertEquals(listOf(a, b), splitMxhtmlSegments("$a$b"))
    }
}
