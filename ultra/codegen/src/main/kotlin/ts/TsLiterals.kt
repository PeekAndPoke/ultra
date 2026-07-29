package io.peekandpoke.ultra.codegen.ts

/**
 * Renders [value] as a single-quoted TypeScript string literal, escaping anything that would end the
 * literal early.
 *
 * Needed because none of the strings reaching generated source are identifier-shaped. `@SerialName`
 * values, `Polymorphic.Child.identifier`, `Polymorphic.Parent.discriminator` and `@Slumber.Field`
 * renames are all arbitrary developer-authored text. Splicing one in raw lets a quote or a line break
 * close the literal, which at best emits a file that does not parse — `identifier = "O'Brien"` was
 * enough — and at worst appends executable TypeScript to output that is checked in and bundled.
 */
internal fun tsStringLiteral(value: String): String {
    val escaped = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                // Line separators terminate a string in ECMAScript before 2019, and still bite when
                // the emitted file is itself embedded in JSON or in another JS string.
                Char(0x2028) -> append("\\u2028")
                Char(0x2029) -> append("\\u2029")
                else -> append(ch)
            }
        }
    }

    return "'$escaped'"
}

/**
 * Renders [name] for property position: bare when it is a valid TypeScript identifier, otherwise as a
 * quoted and escaped key.
 */
internal fun tsPropertyName(name: String): String = when {
    name.matches(BARE_IDENTIFIER) -> name
    else -> tsStringLiteral(name)
}

private val BARE_IDENTIFIER = Regex("[A-Za-z_$][A-Za-z0-9_$]*")
