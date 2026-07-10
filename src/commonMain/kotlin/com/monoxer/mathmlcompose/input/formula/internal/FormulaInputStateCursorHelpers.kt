@file:Suppress("CyclomaticComplexMethod")

package com.monoxer.mathmlcompose.input.formula.internal

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathPlaceholder

/**
 * Inserts [MathCursor] at the first editable position inside [elem].
 * If [elem] itself is a [MathPlaceholder] or [MathMrow], cursor is placed there directly.
 * Otherwise, checks children recursively.
 */
internal fun withCursorAtFirst(elem: MathElement): MathElement =
    when (elem) {
        is MathPlaceholder -> {
            MathMrow(listOf(MathCursor))
        }

        is MathMrow -> {
            // Skip directly into the first complex child to avoid duplicate visual stops
            // (e.g. "before Msup" and "inside Msup at start" look identical).
            // Exception: radicals (√) render a symbol left of content, so both stops are distinct.
            val first = elem.children.firstOrNull()
            if (first != null && hasEditableSlots(first) && !isRadicalElement(first)) {
                elem.copy(children = listOf(withCursorAtFirst(first)) + elem.children.drop(1))
            } else {
                elem.copy(children = listOf(MathCursor) + elem.children)
            }
        }

        else -> {
            val children = childrenOf(elem)
            val hit =
                children.withIndex().firstOrNull { (_, child) ->
                    child is MathPlaceholder || child is MathMrow || hasEditableSlots(child)
                }
            if (hit == null) elem else withChild(elem, hit.index, withCursorAtFirst(hit.value))
        }
    }

/**
 * Inserts [MathCursor] at the last editable position inside [elem].
 * If [elem] itself is a [MathPlaceholder] or [MathMrow], cursor is placed there directly.
 */
internal fun withCursorAtLast(elem: MathElement): MathElement =
    when (elem) {
        is MathPlaceholder -> {
            MathMrow(listOf(MathCursor))
        }

        // For MathMrow: cursor goes at the END (after the last element), NOT inside the last element.
        // "After last element in slot" is visually distinct from "inside last element at end"
        // because they render at different font sizes (the cursor outside a nested script is
        // larger than one inside it). Contrast with withCursorAtFirst, where "before first
        // complex element" and "inside first complex element at start" look identical.
        is MathMrow -> {
            elem.copy(children = elem.children + MathCursor)
        }

        else -> {
            val children = childrenOf(elem)
            val hit =
                children.withIndex().lastOrNull { (_, child) ->
                    child is MathPlaceholder || child is MathMrow || hasEditableSlots(child)
                }
            if (hit == null) elem else withChild(elem, hit.index, withCursorAtLast(hit.value))
        }
    }

/**
 * Removes the [MathCursor] from [elem]:
 * - If a [MathMrow] loses its cursor and becomes empty → replaced by [MathPlaceholder]
 * - If a slot WAS just the cursor → replaced by [MathPlaceholder]
 */
internal fun withCursorRemoved(elem: MathElement): MathElement =
    when (elem) {
        is MathCursor -> {
            MathPlaceholder
        }

        is MathMrow if elem.children.any { it is MathCursor } -> {
            val remaining = elem.children.filterNot { it is MathCursor }
            if (remaining.isEmpty()) MathPlaceholder else elem.copy(children = remaining)
        }

        else -> {
            val children = childrenOf(elem)
            val idx = children.indexOfFirst { hasCursor(it) }
            if (idx < 0) elem else withChild(elem, idx, withCursorRemoved(children[idx]))
        }
    }
