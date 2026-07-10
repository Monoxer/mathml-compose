package com.monoxer.mathmlcompose.input.formula

import com.monoxer.mathmlcompose.input.formula.internal.childrenOf
import com.monoxer.mathmlcompose.input.formula.internal.exitLeft
import com.monoxer.mathmlcompose.input.formula.internal.exitRight
import com.monoxer.mathmlcompose.input.formula.internal.findCursorPath
import com.monoxer.mathmlcompose.input.formula.internal.hasEditableSlots
import com.monoxer.mathmlcompose.input.formula.internal.isEditableSlot
import com.monoxer.mathmlcompose.input.formula.internal.nodeAtPath
import com.monoxer.mathmlcompose.input.formula.internal.replaceAtPath
import com.monoxer.mathmlcompose.input.formula.internal.withChild
import com.monoxer.mathmlcompose.input.formula.internal.withCursorAtFirst
import com.monoxer.mathmlcompose.input.formula.internal.withCursorAtLast
import com.monoxer.mathmlcompose.input.formula.internal.withCursorRemoved
import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathMfrac
import com.monoxer.mathmlcompose.math.MathMrow
import com.monoxer.mathmlcompose.math.MathMsqrt
import com.monoxer.mathmlcompose.math.MathMsub
import com.monoxer.mathmlcompose.math.MathMsup
import com.monoxer.mathmlcompose.math.MathPlaceholder

/**
 * Immutable state for formula keyboard input.
 *
 * The formula is stored as a [MathElement] tree that always contains exactly one
 * [MathCursor] node marking the active cursor position. [MathPlaceholder] nodes
 * represent unfilled (but navigable) input slots inside complex elements such as
 * fractions and roots.
 *
 * Rendering: pass [root] directly to [com.monoxer.mathmlcompose.render.compose.MathCanvas].
 * The [MathCursor] renders as a highlighted box; [MathPlaceholder] renders as a
 * dashed outline, so only the active cursor stands out visually.
 */
data class FormulaInputState(
    val root: MathElement = MathMrow(listOf(MathCursor)),
)

// ── Insert ────────────────────────────────────────────────────────────────

/**
 * Inserts a leaf element ([MathMn], [MathMo], [MathMi], [MathMtext]) at the cursor.
 * The cursor advances to the right of the new element.
 */
fun FormulaInputState.insertLeaf(elem: MathElement): FormulaInputState {
    val cursorPath = findCursorPath(root) ?: return this

    val parentPath = cursorPath.dropLast(1)
    val cursorIdx = cursorPath.last()
    val parent = nodeAtPath(root, parentPath)

    val newRoot =
        when {
            cursorPath.isEmpty() -> {
                MathMrow(listOf(elem, MathCursor))
            }

            parent is MathMrow -> {
                insertLeafInMrow(root, parent, parentPath, cursorIdx, elem)
            }

            else -> {
                // Cursor is a direct child of a complex slot — wrap in MathMrow
                val newParent = withChild(parent, cursorIdx, MathMrow(listOf(elem, MathCursor)))
                if (parentPath.isEmpty()) newParent else replaceAtPath(root, parentPath, newParent)
            }
        }
    return copy(root = newRoot)
}

private fun insertLeafInMrow(
    root: MathElement,
    parent: MathMrow,
    parentPath: List<Int>,
    cursorIdx: Int,
    elem: MathElement,
): MathElement {
    val c = parent.children.toMutableList()
    c[cursorIdx] = elem
    c.add(cursorIdx + 1, MathCursor)
    val newParent = parent.copy(children = c)
    return if (parentPath.isEmpty()) newParent else replaceAtPath(root, parentPath, newParent)
}

/**
 * Inserts a fraction at the cursor. Cursor moves into the numerator slot.
 */
fun FormulaInputState.insertFrac(): FormulaInputState {
    val cursorPath = findCursorPath(root) ?: return this
    val newFrac =
        MathMfrac(
            numerator = MathPlaceholder,
            denominator = MathMrow(listOf(MathCursor)),
        )
    return copy(root = replaceAtPath(root, cursorPath, newFrac))
}

/**
 * Inserts a square root at the cursor. Cursor moves inside the radicand.
 */
fun FormulaInputState.insertSqrt(): FormulaInputState {
    val cursorPath = findCursorPath(root) ?: return this
    val newSqrt = MathMsqrt(MathMrow(listOf(MathCursor)))
    return copy(root = replaceAtPath(root, cursorPath, newSqrt))
}

/**
 * Inserts a superscript at the cursor.
 * If there is an element before the cursor, wraps it as the base; cursor goes to exponent.
 * Otherwise, creates an empty base placeholder with cursor in exponent.
 */
fun FormulaInputState.insertSup(): FormulaInputState {
    val cursorPath = findCursorPath(root)?.takeIf { it.isNotEmpty() } ?: return this

    val parentPath = cursorPath.dropLast(1)
    val cursorIdx = cursorPath.last()
    val parent = nodeAtPath(root, parentPath)

    return if (parent is MathMrow && cursorIdx > 0) {
        // Wrap the element before cursor as base; cursor goes to exponent
        val base = parent.children[cursorIdx - 1]
        val newSup =
            MathMsup(
                main = MathMrow(listOf(base)),
                sup = MathMrow(listOf(MathCursor)),
            )
        val c = parent.children.toMutableList()
        c.removeAt(cursorIdx) // remove the cursor placeholder
        c[cursorIdx - 1] = newSup // replace base with new Msup
        val newParent = parent.copy(children = c)
        copy(root = if (parentPath.isEmpty()) newParent else replaceAtPath(root, parentPath, newParent))
    } else {
        // Nothing before cursor: empty navigable base, cursor in exponent
        val newSup =
            MathMsup(
                main = MathPlaceholder,
                sup = MathMrow(listOf(MathCursor)),
            )
        copy(root = replaceAtPath(root, cursorPath, newSup))
    }
}

/**
 * Inserts a subscript at the cursor.
 * If there is an element before the cursor, wraps it as the base; cursor goes to subscript.
 * Otherwise, creates an empty base placeholder with cursor in subscript.
 */
fun FormulaInputState.insertSub(): FormulaInputState {
    val cursorPath = findCursorPath(root)?.takeIf { it.isNotEmpty() } ?: return this

    val parentPath = cursorPath.dropLast(1)
    val cursorIdx = cursorPath.last()
    val parent = nodeAtPath(root, parentPath)

    return if (parent is MathMrow && cursorIdx > 0) {
        val base = parent.children[cursorIdx - 1]
        val newSub =
            MathMsub(
                main = MathMrow(listOf(base)),
                sub = MathMrow(listOf(MathCursor)),
            )
        val c = parent.children.toMutableList()
        c.removeAt(cursorIdx)
        c[cursorIdx - 1] = newSub
        val newParent = parent.copy(children = c)
        copy(root = if (parentPath.isEmpty()) newParent else replaceAtPath(root, parentPath, newParent))
    } else {
        val newSub =
            MathMsub(
                main = MathPlaceholder,
                sub = MathMrow(listOf(MathCursor)),
            )
        copy(root = replaceAtPath(root, cursorPath, newSub))
    }
}

// ── Backspace ─────────────────────────────────────────────────────────────

/**
 * Deletes the element immediately before the cursor.
 *
 * - If cursor is at position > 0 in a [MathMrow]: removes the preceding element.
 * - If cursor is at position 0 in any slot of a complex element: exits the complex
 *   element and deletes the element that was immediately before it in the parent row.
 *   If there is nothing before it, just moves the cursor out (no deletion).
 */
fun FormulaInputState.backspace(): FormulaInputState {
    val cursorPath = findCursorPath(root)?.takeIf { it.isNotEmpty() } ?: return this

    val parentPath = cursorPath.dropLast(1)
    val cursorIdx = cursorPath.last()
    val parent = nodeAtPath(root, parentPath)

    return when {
        parent is MathMrow && cursorIdx > 0 -> backspaceAtPrevious(root, parent, parentPath, cursorIdx)
        parent is MathMrow && cursorIdx == 0 -> backspaceAtSlotStart(root, parent, parentPath)
        else -> copy(root = exitLeft(root, parentPath))
    }
}

private fun FormulaInputState.backspaceAtPrevious(
    root: MathElement,
    parent: MathMrow,
    parentPath: List<Int>,
    cursorIdx: Int,
): FormulaInputState {
    val c = parent.children.toMutableList()
    c.removeAt(cursorIdx - 1)
    val newParent = parent.copy(children = c)
    val newRoot = if (parentPath.isEmpty()) newParent else replaceAtPath(root, parentPath, newParent)
    return copy(root = newRoot)
}

@Suppress("CyclomaticComplexMethod", "ReturnCount")
private fun FormulaInputState.backspaceAtSlotStart(
    root: MathElement,
    parent: MathMrow,
    parentPath: List<Int>,
): FormulaInputState {
    if (parentPath.isEmpty()) return this // at very start of root

    // If slot has content, just move cursor out (no deletion)
    if (parent.children.size > 1) return copy(root = exitLeft(root, parentPath))

    // Cursor is alone in an empty MathMrow slot inside a complex element.
    val slotIdx = parentPath.last()
    val containerPath = parentPath.dropLast(1)
    if (containerPath.isEmpty()) return copy(root = exitLeft(root, parentPath))

    val container = nodeAtPath(root, containerPath)
    val containerIdx = containerPath.last()
    val grandparentPath = containerPath.dropLast(1)
    val grandparent = nodeAtPath(root, grandparentPath)

    // Empty sup/sub slot → unwrap complex element and restore base to parent row
    val isEmptyScriptSlot = (container is MathMsup || container is MathMsub) && slotIdx == 1
    if (isEmptyScriptSlot && grandparent is MathMrow) {
        val main =
            when (container) {
                is MathMsup -> container.main
                is MathMsub -> container.main
            }
        val mainChildren =
            when (main) {
                is MathMrow -> main.children
                is MathPlaceholder -> emptyList()
                else -> listOf(main)
            }
        val c = grandparent.children.toMutableList()
        c.removeAt(containerIdx)
        var insertIdx = containerIdx
        for (child in mainChildren) {
            c.add(insertIdx++, child)
        }
        c.add(insertIdx, MathCursor)
        val newGp = grandparent.copy(children = c)
        val newRoot = if (grandparentPath.isEmpty()) newGp else replaceAtPath(root, grandparentPath, newGp)
        return copy(root = newRoot)
    }

    if (grandparent is MathMrow && containerIdx > 0) {
        // Remove cursor from current position, then delete element before complex
        val rootWithoutCursor = withCursorRemoved(root)
        val gp = nodeAtPath(rootWithoutCursor, grandparentPath) as MathMrow
        val c = gp.children.toMutableList()
        c.removeAt(containerIdx - 1) // delete element before complex
        c.add(containerIdx - 1, MathCursor) // place cursor where it was
        val newGp = gp.copy(children = c)
        val newRoot = if (grandparentPath.isEmpty()) newGp else replaceAtPath(rootWithoutCursor, grandparentPath, newGp)
        return copy(root = newRoot)
    }

    // Nothing before the complex element → just move cursor out
    return copy(root = exitLeft(root, parentPath))
}

// ── Navigation ───────────────────────────────────────────────────────────

/**
 * Moves the cursor one position to the right.
 *
 * - Skips over leaf elements in the current [MathMrow].
 * - Enters complex elements (fraction, sqrt, sup, sub) at their first editable slot.
 * - Navigates from one slot to the next within a complex element (e.g., numerator → denominator).
 * - Exits the complex element when all slots are exhausted.
 */
fun FormulaInputState.moveRight(): FormulaInputState {
    val cursorPath = findCursorPath(root)?.takeIf { it.isNotEmpty() } ?: return this

    val parentPath = cursorPath.dropLast(1)
    val cursorIdx = cursorPath.last()
    val parent = nodeAtPath(root, parentPath)

    val newRoot =
        when (parent) {
            is MathMrow -> {
                moveRightInMrow(root, parent, parentPath, cursorIdx)
            }

            else -> {
                // Cursor is a direct child of a complex element's slot
                val siblings = childrenOf(parent)
                val nextSlotIdx = (cursorIdx + 1 until siblings.size).firstOrNull { isEditableSlot(siblings[it]) }
                if (nextSlotIdx != null) {
                    val withPlaceholder = withChild(parent, cursorIdx, MathPlaceholder)
                    val withNextCursor =
                        withChild(withPlaceholder, nextSlotIdx, withCursorAtFirst(siblings[nextSlotIdx]))
                    if (parentPath.isEmpty()) withNextCursor else replaceAtPath(root, parentPath, withNextCursor)
                } else {
                    exitRight(root, parentPath)
                }
            }
        }

    return copy(root = newRoot)
}

private fun moveRightInMrow(
    root: MathElement,
    parent: MathMrow,
    parentPath: List<Int>,
    cursorIdx: Int,
): MathElement {
    val nextIdx = cursorIdx + 1
    if (nextIdx >= parent.children.size) return exitRight(root, parentPath)
    val next = parent.children[nextIdx]
    val c = parent.children.toMutableList()
    c.removeAt(cursorIdx)
    if (hasEditableSlots(next)) {
        // Enter complex element from the left
        c[nextIdx - 1] = withCursorAtFirst(next)
    } else {
        // Leaf — skip past whole node
        c.add(nextIdx, MathCursor)
    }
    val newParent = parent.copy(children = c)
    return if (parentPath.isEmpty()) newParent else replaceAtPath(root, parentPath, newParent)
}

/**
 * Moves the cursor one position to the left — mirror of [moveRight].
 */
fun FormulaInputState.moveLeft(): FormulaInputState {
    val cursorPath = findCursorPath(root)?.takeIf { it.isNotEmpty() } ?: return this

    val parentPath = cursorPath.dropLast(1)
    val cursorIdx = cursorPath.last()
    val parent = nodeAtPath(root, parentPath)

    val newRoot =
        when (parent) {
            is MathMrow -> {
                moveLeftInMrow(root, parent, parentPath, cursorIdx)
            }

            else -> {
                val siblings = childrenOf(parent)
                val prevSlotIdx = (0 until cursorIdx).lastOrNull { isEditableSlot(siblings[it]) }
                if (prevSlotIdx != null) {
                    val withPlaceholder = withChild(parent, cursorIdx, MathPlaceholder)
                    val withPrevCursor =
                        withChild(withPlaceholder, prevSlotIdx, withCursorAtLast(siblings[prevSlotIdx]))
                    if (parentPath.isEmpty()) withPrevCursor else replaceAtPath(root, parentPath, withPrevCursor)
                } else {
                    exitLeft(root, parentPath)
                }
            }
        }

    return copy(root = newRoot)
}

private fun moveLeftInMrow(
    root: MathElement,
    parent: MathMrow,
    parentPath: List<Int>,
    cursorIdx: Int,
): MathElement {
    val prevIdx = cursorIdx - 1
    if (prevIdx < 0) return exitLeft(root, parentPath)
    val prev = parent.children[prevIdx]
    val c = parent.children.toMutableList()
    c.removeAt(cursorIdx)
    if (hasEditableSlots(prev)) {
        // Enter complex element from the right
        c[prevIdx] = withCursorAtLast(prev)
    } else {
        // Leaf — move cursor before whole node
        c.add(prevIdx, MathCursor)
    }
    val newParent = parent.copy(children = c)
    return if (parentPath.isEmpty()) newParent else replaceAtPath(root, parentPath, newParent)
}

// ── Mouse / tap selection ─────────────────────────────────────────────────

/**
 * Moves the cursor to the element at [path] in the current tree.
 * [onRight] = true when the tap was on the right half of the element.
 *
 * - [MathPlaceholder] at [path]: cursor enters that slot (position 0), replaces placeholder.
 * - Any other element: cursor is inserted before it (onRight=false) or after it (onRight=true)
 *   in its parent [MathMrow].
 */
@Suppress("ReturnCount")
fun FormulaInputState.moveCursorTo(
    path: List<Int>,
    onRight: Boolean,
): FormulaInputState {
    val rootWithoutCursor = withCursorRemoved(root)
    val target = nodeAtPath(rootWithoutCursor, path)

    val newRoot =
        when {
            target is MathPlaceholder -> {
                // Enter the placeholder slot; cursor goes to position 0
                replaceAtPath(rootWithoutCursor, path, MathMrow(listOf(MathCursor)))
            }

            target is MathMrow -> {
                // Enter the MathMrow; cursor goes to position 0
                replaceAtPath(rootWithoutCursor, path, target.copy(children = listOf(MathCursor) + target.children))
            }

            path.isNotEmpty() -> {
                moveCursorToLeafOrComplex(rootWithoutCursor, path, onRight) ?: return this
            }

            else -> {
                return this
            }
        }
    return copy(root = newRoot)
}

@Suppress("ReturnCount")
private fun moveCursorToLeafOrComplex(
    rootWithoutCursor: MathElement,
    path: List<Int>,
    onRight: Boolean,
): MathElement? {
    // Leaf or complex element: insert cursor before or after it in its parent MathMrow
    val parentPath = path.dropLast(1)
    val elemIdx = path.last()
    val parent = nodeAtPath(rootWithoutCursor, parentPath)
    if (parent !is MathMrow) return null
    val insertIdx = if (onRight) elemIdx + 1 else elemIdx
    val c = parent.children.toMutableList().also { it.add(insertIdx, MathCursor) }
    val newParent = parent.copy(children = c)
    return if (parentPath.isEmpty()) newParent else replaceAtPath(rootWithoutCursor, parentPath, newParent)
}

// ── Result ────────────────────────────────────────────────────────────────

/**
 * Returns the formula as a [MathElement] tree with the cursor removed.
 * Use this to read the final input value.
 */
fun FormulaInputState.toResultElement(): MathElement = withCursorRemoved(root)
