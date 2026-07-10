package com.monoxer.mathmlcompose.input.formula.internal

import com.monoxer.mathmlcompose.math.MathCursor
import com.monoxer.mathmlcompose.math.MathElement
import com.monoxer.mathmlcompose.math.MathMrow

/**
 * Exits the current slot ([mrowPath]) to the right:
 * tries the next editable sibling slot first, then exits the whole complex element.
 */
internal fun exitRight(
    root: MathElement,
    mrowPath: List<Int>,
): MathElement {
    val containerPath = mrowPath.dropLast(1)
    if (mrowPath.isEmpty() || containerPath.isEmpty()) return root // already at root end

    val slotIdx = mrowPath.last()
    val container = nodeAtPath(root, containerPath)
    val siblings = childrenOf(container)
    val mrowWithoutCursor = withCursorRemoved(nodeAtPath(root, mrowPath))

    // Try to enter the next editable sibling slot
    val nextEditableIdx = (slotIdx + 1 until siblings.size).firstOrNull { isEditableSlot(siblings[it]) }
    return if (nextEditableIdx != null) {
        val nextSlotWithCursor = withCursorAtFirst(siblings[nextEditableIdx])
        replaceAtPath(
            replaceAtPath(root, mrowPath, mrowWithoutCursor),
            containerPath + nextEditableIdx,
            nextSlotWithCursor,
        )
    } else {
        // No next slot → exit the complex element entirely into grandparent MathMrow
        val grandparentPath = containerPath.dropLast(1)
        val containerIdx = containerPath.last()
        val newRoot = replaceAtPath(root, mrowPath, mrowWithoutCursor)
        val grandparent = nodeAtPath(newRoot, grandparentPath)
        if (grandparent is MathMrow) {
            val c = grandparent.children.toMutableList().also { it.add(containerIdx + 1, MathCursor) }
            val newGrandparent = grandparent.copy(children = c)
            if (grandparentPath.isEmpty()) newGrandparent else replaceAtPath(newRoot, grandparentPath, newGrandparent)
        } else {
            exitRight(newRoot, containerPath)
        }
    }
}

/**
 * Exits the current slot ([mrowPath]) to the left:
 * tries the previous editable sibling slot first, then exits the whole complex element.
 */
@Suppress("CyclomaticComplexMethod")
internal fun exitLeft(
    root: MathElement,
    mrowPath: List<Int>,
): MathElement {
    val containerPath = mrowPath.dropLast(1)
    if (mrowPath.isEmpty() || containerPath.isEmpty()) return root // mrow IS the root → at start

    val slotIdx = mrowPath.last()
    val container = nodeAtPath(root, containerPath)
    val siblings = childrenOf(container)
    val mrowWithoutCursor = withCursorRemoved(nodeAtPath(root, mrowPath))

    // Try to enter the previous editable sibling slot
    val prevEditableIdx = (0 until slotIdx).lastOrNull { isEditableSlot(siblings[it]) }
    return if (prevEditableIdx != null) {
        val prevSlotWithCursor = withCursorAtLast(siblings[prevEditableIdx])
        replaceAtPath(
            replaceAtPath(root, mrowPath, mrowWithoutCursor),
            containerPath + prevEditableIdx,
            prevSlotWithCursor,
        )
    } else {
        // No previous slot → exit the complex element entirely, cursor goes BEFORE it
        val grandparentPath = containerPath.dropLast(1)
        val containerIdx = containerPath.last()
        val newRoot = replaceAtPath(root, mrowPath, mrowWithoutCursor)
        val grandparent = nodeAtPath(newRoot, grandparentPath)
        if (grandparent is MathMrow) {
            val c = grandparent.children.toMutableList().also { it.add(containerIdx, MathCursor) }
            val newGrandparent = grandparent.copy(children = c)
            val rootWithCursor =
                if (grandparentPath.isEmpty()) {
                    newGrandparent
                } else {
                    replaceAtPath(newRoot, grandparentPath, newGrandparent)
                }
            // Skip the duplicate stop when container is at position 0: "before container" and
            // "inside container at start" are visually identical (same size/location).
            // Exception: radicals (√) render a symbol left of content, so both stops are distinct.
            if (containerIdx == 0 && grandparentPath.isNotEmpty() && !isRadicalElement(container)) {
                exitLeft(rootWithCursor, grandparentPath)
            } else {
                rootWithCursor
            }
        } else {
            exitLeft(newRoot, containerPath)
        }
    }
}
