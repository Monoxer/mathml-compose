package com.monoxer.mathmlcompose.mxhtml

internal fun splitMxhtmlSegments(input: String): List<String> = buildList { collectSegments(input) }

private tailrec fun MutableList<String>.collectSegments(remaining: String) {
    if (remaining.isEmpty()) return
    val mathStart = remaining.indexOf("<math")
    val mathEnd = if (mathStart != -1) remaining.indexOf("</math>", mathStart) else -1
    when {
        mathStart == -1 -> {
            add(remaining)
        }

        mathEnd == -1 -> {
            if (mathStart > 0) add(remaining.substring(0, mathStart))
            add(remaining.substring(mathStart))
        }

        else -> {
            if (mathStart > 0) add(remaining.substring(0, mathStart))
            add(remaining.substring(mathStart, mathEnd + "</math>".length))
            collectSegments(remaining.substring(mathEnd + "</math>".length))
        }
    }
}
