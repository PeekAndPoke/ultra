package io.peekandpoke.ultra.common

/**
 * Defines a system for replacing placeholders in text with dynamic values.
 *
 * @param T The type of values that will be represented and replaced in placeholders.
 */
interface Placeholders<T> {
    /**
     * [Filled] [Placeholders] can be used to [replace] patterns in a text.
     */
    /**
     * A set of placeholders that have been filled with a replacement function.
     *
     * Use [replace] or [invoke] to substitute all placeholder patterns in a text.
     */
    class Filled<T>(private val mapping: Map<String, T>, private val replace: (T) -> String) {

        /**
         * A single combined pattern over all placeholder strings, so substitution is **single-pass**:
         * the replacement text is never re-scanned. Without this, [replace] was a `fold` of
         * `String.replace` over the accumulating text, so a value substituted for one placeholder was
         * re-examined by later iterations — a value that itself contained `{{other}}` (e.g. user input)
         * got expanded into another placeholder's value. That is a second-order injection (and an
         * exponential-amplification vector). Longest patterns first so overlapping patterns resolve
         * to the intended one.
         */
        private val combined: Regex? = mapping.keys
            .takeIf { it.isNotEmpty() }
            ?.sortedByDescending { it.length }
            ?.joinToString("|") { Regex.escape(it) }
            ?.toRegex()

        /** Replaces every placeholder pattern in [text] with its computed value, each exactly once. */
        fun replace(text: String): String {
            val regex = combined ?: return text
            return regex.replace(text) { match ->
                mapping[match.value]?.let { replace(it) } ?: match.value
            }
        }

        /** Shorthand for [replace]. */
        operator fun invoke(text: String) = replace(text)
    }

    /** Associates a human-readable [name] with a placeholder [pattern]. */
    data class NameToPattern(
        val name: String,
        val pattern: String,
    )

    /**
     * Base implementation of [Placeholders] that derives patterns and names from a [toStr] function.
     */
    abstract class Abstract<T>(override val values: Set<T>, val toStr: (T) -> String) : Placeholders<T> {
        override val patterns: Set<String> by lazy {
            values.map(::renderPattern).toSet()
        }

        override val namesToPatterns: Set<NameToPattern> by lazy {
            values.map { NameToPattern(renderName(it), renderPattern(it)) }.toSet()
        }

        override fun renderName(value: T): String = toStr(value)
    }

    /**
     * Triple Hash Tag Placeholders, like ###Name###, ###age###, ...
     */
    /**
     * Double-curly-brace placeholders, e.g. `{{Name}}`, `{{age}}`.
     */
    class DoubleCurly<T>(values: Set<T>, toStr: (T) -> String) : Abstract<T>(values, toStr) {

        companion object {
            /** NOTICE: the all curly must be escaped for Javascript */
            @Suppress("RegExpRedundantEscape")
            private val regex = "\\{\\{[a-zA-Z0-9_-]+\\}\\}".toRegex()

            /** Creates a [DoubleCurly] for all constants of the enum type [E], using enum names. */
            inline operator fun <reified E : Enum<E>> invoke(): DoubleCurly<E> {
                return invoke { it.name }
            }

            /** Creates a [DoubleCurly] for all constants of the enum type [E], using [toStr] for naming. */
            inline operator fun <reified E : Enum<E>> invoke(noinline toStr: (E) -> String): DoubleCurly<E> {
                return invoke(enumValues<E>().toSet(), toStr)
            }

            /** Creates a [DoubleCurly] from arbitrary [values], using [toStr] for naming. */
            operator fun <T> invoke(values: Iterable<T>, toStr: (T) -> String): DoubleCurly<T> {
                return DoubleCurly(values.toSet(), toStr)
            }
        }

        /** Renders the [value] as a `{{...}}` placeholder pattern. */
        override fun renderPattern(value: T): String = "{{${toStr(value)}}}"

        /** Finds any `{{...}}` patterns in the [text] that are not valid placeholders. */
        override fun findErrorsIn(text: String): Set<String> {
            return regex.findAll(text)
                .map { it.value }
                .filter { !patterns.contains(it) }
                .toSet()
        }
    }

    /**
     * Triple Hash Tag Placeholders, like ###Name###, ###age###, ...
     */
    /**
     * Triple-hash placeholders, e.g. `###Name###`, `###age###`.
     */
    class TripleHash<T>(values: Set<T>, toStr: (T) -> String) : Abstract<T>(values, toStr) {

        companion object {
            private val regex = "###[a-zA-Z0-9_-]+###".toRegex()

            /** Creates a [TripleHash] for all constants of the enum type [E], using enum names. */
            inline operator fun <reified E : Enum<E>> invoke(): TripleHash<E> {
                return invoke { it.name }
            }

            /** Creates a [TripleHash] for all constants of the enum type [E], using [toStr] for naming. */
            inline operator fun <reified E : Enum<E>> invoke(noinline toStr: (E) -> String): TripleHash<E> {
                return invoke(enumValues<E>().toSet(), toStr)
            }

            /** Creates a [TripleHash] from arbitrary [values], using [toStr] for naming. */
            operator fun <T> invoke(values: Iterable<T>, toStr: (T) -> String): TripleHash<T> {
                return TripleHash(values.toSet(), toStr)
            }
        }

        /** Renders the [value] as a `###...###` placeholder pattern. */
        override fun renderPattern(value: T): String = "###${toStr(value)}###"

        /** Finds any `###...###` patterns in the [text] that are not valid placeholders. */
        override fun findErrorsIn(text: String): Set<String> {
            return regex.findAll(text)
                .map { it.value }
                .filter { !patterns.contains(it) }
                .toSet()
        }
    }

    /** The values the placeholders represent */
    val values: Set<T>

    /** The patterns that represent the placeholders */
    val patterns: Set<String>

    /** A set of [NameToPattern] for each value */
    val namesToPatterns: Set<NameToPattern>

    /** Renders the value as a string */
    fun renderName(value: T): String

    /** Renders the value as a placeholder pattern */
    fun renderPattern(value: T): String

    /** Finds invalid placeholder patterns in the given [text] */
    fun findErrorsIn(text: String): Set<String>

    /** Returns true when the given [text] contains valid patterns only */
    fun validate(text: String): Boolean = findErrorsIn(text).isEmpty()

    /** Fills the placeholders with values */
    fun fill(replace: (T) -> String): Filled<T> {
        return Filled(
            mapping = values.associateBy(::renderPattern), replace = replace
        )
    }
}
