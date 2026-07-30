package io.peekandpoke.ultra.tooling.i18n

/** Kotlin hard keywords that cannot be used as identifiers without backticks. */
private val KOTLIN_HARD_KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
    "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
    "true", "try", "typealias", "typeof", "val", "var", "when", "while",
)

private val PLAIN_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

/**
 * Whitelist for any name that flows from a catalog into generated Kotlin. Only letters, digits, `_`
 * and `-` are permitted — none of which can break out of a Kotlin identifier or string literal, so a
 * crafted key like `x(): Unit; fun y` is rejected rather than injected.
 */
private val SAFE_NAME = Regex("[A-Za-z0-9_-]+")

/**
 * Renders [name] as a safe Kotlin member identifier (function/property/parameter): a plain identifier
 * passes through; a keyword, a leading-digit name, or a hyphenated name is backtick-quoted. A name
 * outside [SAFE_NAME] is REJECTED — this is the injection guard for the accessors file.
 */
fun kotlinMemberName(name: String, what: String): String {
    require(SAFE_NAME.matches(name)) { invalidNameMessage(name, what) }
    return when {
        !PLAIN_IDENTIFIER.matches(name) -> "`$name`" // leading digit or hyphen
        name in KOTLIN_HARD_KEYWORDS -> "`$name`"
        else -> name
    }
}

/**
 * Renders [name] as a valid PascalCase class-name segment; `-`/`_` split words (`my-ns` -> `MyNs`).
 * A leading digit is prefixed with `_`. Names outside [SAFE_NAME] are REJECTED.
 */
fun kotlinClassPart(name: String, what: String): String {
    require(SAFE_NAME.matches(name)) { invalidNameMessage(name, what) }
    val pascal = name.split('-', '_')
        .filter { it.isNotEmpty() }
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    return if (pascal.firstOrNull()?.isDigit() == true) "_$pascal" else pascal
}

private fun invalidNameMessage(name: String, what: String): String =
    "Invalid i18n $what '$name': only letters, digits, '_' and '-' are allowed (a valid Kotlin identifier)."
