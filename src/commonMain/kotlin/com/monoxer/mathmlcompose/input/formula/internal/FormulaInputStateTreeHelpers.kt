@file:Suppress("CyclomaticComplexMethod")

package com.monoxer.mathmlcompose.input.formula.internal

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMover
import com.monoxer.mathmlcompose.math.MathMroot
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathMsqrt
import com.monoxer.mathmlcompose.math.MathMsub
import com.monoxer.mathmlcompose.math.MathMsubsup
import com.monoxer.mathmlcompose.math.MathMsup
import com.monoxer.mathmlcompose.math.MathMunder
import com.monoxer.mathmlcompose.math.MathMunderover
import com.monoxer.mathmlcompose.math.MathPlaceholder

/** Returns the direct children of a [MathElement] in slot order. */
internal fun childrenOf(elem: MathElement): List<MathElement> =
    when (elem) {
        is MathMrow -> elem.children
        is MathMfrac -> listOf(elem.denominator, elem.numerator)
        is MathMsup -> listOf(elem.main, elem.sup)
        is MathMsub -> listOf(elem.main, elem.sub)
        is MathMsubsup -> listOf(elem.main, elem.sub, elem.sup)
        is MathMsqrt -> listOf(elem.main)
        is MathMroot -> listOf(elem.main, elem.root)
        is MathMover -> listOf(elem.main, elem.over)
        is MathMunder -> listOf(elem.main, elem.under)
        is MathMunderover -> listOf(elem.main, elem.under, elem.over)
        else -> emptyList()
    }

/** Rebuilds [parent] with child at [index] replaced by [newChild]. */
@Suppress("LongMethod")
internal fun withChild(
    parent: MathElement,
    index: Int,
    newChild: MathElement,
): MathElement =
    when (parent) {
        is MathMrow -> {
            val c = parent.children.toMutableList().also { it[index] = newChild }
            parent.copy(children = c)
        }

        is MathMfrac -> {
            when (index) {
                0 -> parent.copy(denominator = newChild)
                else -> parent.copy(numerator = newChild)
            }
        }

        is MathMsup -> {
            when (index) {
                0 -> parent.copy(main = newChild)
                else -> parent.copy(sup = newChild)
            }
        }

        is MathMsub -> {
            when (index) {
                0 -> parent.copy(main = newChild)
                else -> parent.copy(sub = newChild)
            }
        }

        is MathMsubsup -> {
            when (index) {
                0 -> parent.copy(main = newChild)
                1 -> parent.copy(sub = newChild)
                else -> parent.copy(sup = newChild)
            }
        }

        is MathMsqrt -> {
            parent.copy(main = newChild)
        }

        is MathMroot -> {
            when (index) {
                0 -> parent.copy(main = newChild)
                else -> parent.copy(root = newChild)
            }
        }

        is MathMover -> {
            when (index) {
                0 -> parent.copy(main = newChild)
                else -> parent.copy(over = newChild)
            }
        }

        is MathMunder -> {
            when (index) {
                0 -> parent.copy(main = newChild)
                else -> parent.copy(under = newChild)
            }
        }

        is MathMunderover -> {
            when (index) {
                0 -> parent.copy(main = newChild)
                1 -> parent.copy(under = newChild)
                else -> parent.copy(over = newChild)
            }
        }

        else -> {
            parent
        }
    }

/** Returns true if [elem] or any descendant is a [MathCursor]. */
internal fun hasCursor(elem: MathElement): Boolean = elem is MathCursor || childrenOf(elem).any { hasCursor(it) }

/**
 * Returns the path (list of child indices from root) to the [MathCursor] node,
 * or null if not found.
 */
internal fun findCursorPath(
    elem: MathElement,
    prefix: List<Int> = emptyList(),
): List<Int>? {
    if (elem is MathCursor) return prefix
    return childrenOf(elem).withIndex().firstNotNullOfOrNull { (i, child) ->
        findCursorPath(child, prefix + i)
    }
}

/** Returns the node at [path] starting from [root]. */
internal fun nodeAtPath(
    root: MathElement,
    path: List<Int>,
): MathElement = path.fold(root) { elem, idx -> childrenOf(elem)[idx] }

/** Rebuilds the tree with the node at [path] replaced by [newNode]. */
internal fun replaceAtPath(
    root: MathElement,
    path: List<Int>,
    newNode: MathElement,
): MathElement {
    if (path.isEmpty()) return newNode
    val idx = path.first()
    val newChild = replaceAtPath(childrenOf(root)[idx], path.drop(1), newNode)
    return withChild(root, idx, newChild)
}

/**
 * An "editable slot" is a position the cursor can enter: a [MathPlaceholder]
 * or a [MathMrow].
 */
internal fun isEditableSlot(elem: MathElement): Boolean = elem is MathPlaceholder || elem is MathMrow

/** True if [elem] has at least one editable slot among its children. */
internal fun hasEditableSlots(elem: MathElement): Boolean = childrenOf(elem).any { isEditableSlot(it) }

/**
 * True if [elem] is a radical element ([MathMsqrt] or [MathMroot]).
 */
internal fun isRadicalElement(elem: MathElement): Boolean = elem is MathMsqrt || elem is MathMroot
