package de.peekandpoke.ultra.common

/**
 * Regex for camel case splitting a string
 *
 * Usage:
 *
 * camelCaseSplitRegex.split("camelCaseSplit") // -> ["camel", "Case", "Split"]
 *
 * @see [camelCaseSplit]
 */
val camelCaseSplitRegex = "(?<=[a-z0-9])(?=[A-Z])".toRegex()
