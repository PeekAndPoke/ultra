package de.peekandpoke.ultra.common

/**
 * Defines a system for replacing placeholders in text with dynamic values.
 *
 * @param T The type of values that will be represented and replaced in placeholders.
 */
interface Placeholders<T> {
    /**
     * [Filled] [Placeholders] can be used to [replace] patterns in a text.
     */
    class Filled<T>(private val mapping: Map<String, T>, private val replace: (T) -> String) {

        fun replace(text: String) = mapping.entries.fold(text) { acc, (pattern, value) ->
            acc.replace(pattern, replace(value))
        }

        operator fun invoke(text: String) = replace(text)
    }

    data class NameToPattern(
        val name: String,
        val pattern: String,
    )

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
    class DoubleCurly<T>(values: Set<T>, toStr: (T) -> String) : Abstract<T>(values, toStr) {

        companion object {
            /** NOTICE: the all curly must be escaped for Javascript */
            @Suppress("RegExpRedundantEscape")
            private val regex = "\\{\\{[a-zA-Z0-9_-]+\\}\\}".toRegex()

            inline operator fun <reified E : Enum<E>> invoke(): DoubleCurly<E> {
                return invoke { it.name }
            }

            inline operator fun <reified E : Enum<E>> invoke(noinline toStr: (E) -> String): DoubleCurly<E> {
                return invoke(enumValues<E>().toSet(), toStr)
            }

            operator fun <T> invoke(values: Iterable<T>, toStr: (T) -> String): DoubleCurly<T> {
                return DoubleCurly(values.toSet(), toStr)
            }
        }

        override fun renderPattern(value: T): String = "{{${toStr(value)}}}"

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
    class TripleHash<T>(values: Set<T>, toStr: (T) -> String) : Abstract<T>(values, toStr) {

        companion object {
            private val regex = "###[a-zA-Z0-9_-]+###".toRegex()

            inline operator fun <reified E : Enum<E>> invoke(): TripleHash<E> {
                return invoke { it.name }
            }

            inline operator fun <reified E : Enum<E>> invoke(noinline toStr: (E) -> String): TripleHash<E> {
                return invoke(enumValues<E>().toSet(), toStr)
            }

            operator fun <T> invoke(values: Iterable<T>, toStr: (T) -> String): TripleHash<T> {
                return TripleHash(values.toSet(), toStr)
            }
        }

        override fun renderPattern(value: T): String = "###${toStr(value)}###"

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
