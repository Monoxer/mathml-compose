package com.monoxer.mathmlcompose.render.compose

/**
 * Result of a hit-test on the formula layout, delivered via [MathCanvas]'s `onTap` callback.
 * [path] = child-index path from the root to the tapped element.
 * [onRight] = true if tap was on the right half of the element (cursor goes after it).
 *
 * Pass [path] and [onRight] straight through to [moveCursorTo] — callers never need to interpret
 * them directly.
 */
data class HitResult(
    val path: List<Int>,
    val onRight: Boolean,
)
