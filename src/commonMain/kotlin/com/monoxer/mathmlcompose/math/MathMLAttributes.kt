package com.monoxer.mathmlcompose.math

// Custom Monoxer MathML attributes
const val MX_DATA_IS_HIDDEN = "mx-data-is-hidden"
const val MX_IS_PLACEHOLDER = "mx-is-placeholder"

private val DATA_ATTRIBUTE_NAME = Regex("data-[a-z0-9_][a-z0-9_-]*")
private val DATA_ATTRIBUTE_VALUE = Regex("[A-Za-z0-9_-]+")

internal fun isValidDataAttribute(
    name: String,
    value: String,
): Boolean = DATA_ATTRIBUTE_NAME.matches(name) && DATA_ATTRIBUTE_VALUE.matches(value)

internal fun requireValidDataAttributes(attributes: Map<String, String>) {
    attributes.forEach { (name, value) ->
        require(isValidDataAttribute(name, value)) {
            "Data attribute name must match 'data-[a-z0-9_][a-z0-9_-]*' and its value must match " +
                "'[A-Za-z0-9_-]+'"
        }
    }
}

// Standard MathML attribute values used by <mspace>
const val MSPACE_LINEBREAK_ATTR = "linebreak"
const val MSPACE_LINEBREAK_NEWLINE = "newline"
