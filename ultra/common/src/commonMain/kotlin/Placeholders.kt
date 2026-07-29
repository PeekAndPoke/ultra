package io.peekandpoke.ultra.common

/**
 * Defines a system for replacing placeholders in text with dynamic values.
 *
 * @param T The type of values that will be represented and replaced in placeholders.
 */
interface Placeholders<T> {
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
            .filter { it.isNotEmpty() } // drop any empty pattern: it would match zero-width everywhere
            .takeIf { it.isNotEmpty() }
            ?.sortedByDescending { it.length }
            ?.joinToString("|") { Regex.escape(it) }
            ?.toRegex()

        /**
         * Replaces every known placeholder pattern in [text] with its computed value, each exactly once.
         *
         * Returns [text] unchanged when it holds no known pattern, or when the placeholder set is
         * empty. Patterns the set does not know (`{{unknown}}`) and malformed ones (`{{unclosed`) are
         * left verbatim — they are never an error here, see [Placeholders.findErrorsIn].
         */
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

        /**
         * Matches anything shaped like a placeholder of this syntax, well-formed or not.
         *
         * Deliberately loose: it has to see `{{ Name }}` and `{{unclosed` too, because those are the
         * mistakes a template author actually makes. Anything it matches that is not a known
         * [patterns] entry is reported by [findErrorsIn].
         */
        protected abstract val scanRegex: Regex

        override val patterns: Set<String> by lazy {
            values.map(::renderPattern).toSet()
        }

        override val namesToPatterns: Set<NameToPattern> by lazy {
            values.map { NameToPattern(renderName(it), renderPattern(it)) }.toSet()
        }

        override fun renderName(value: T): String = toStr(value)

        /**
         * Finds everything in [text] that looks like a placeholder but is not a known one.
         *
         * Reports both unknown names (`{{nmae}}`) and malformed shapes (`{{Name}`, `{{ Name }}`) —
         * neither would be substituted, so both are template bugs.
         */
        override fun findErrorsIn(text: String): Set<String> {
            return scanRegex.findAll(text)
                .map { it.value }
                .filter { !patterns.contains(it) }
                .toSet()
        }
    }

    /**
     * Double-curly-brace placeholders, e.g. `{{Name}}`, `{{age}}`.
     */
    class DoubleCurly<T>(values: Set<T>, toStr: (T) -> String) : Abstract<T>(values, toStr) {

        companion object {
            /**
             * Matches any well-formed `{{name}}` shape, whether or not it is a known placeholder.
             *
             * NOTICE: the all curly must be escaped for Javascript
             */
            @Suppress("RegExpRedundantEscape")
            private val scan = "\\{\\{[^{}]*\\}{0,2}".toRegex()

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

        override val scanRegex: Regex get() = scan

        /** Renders the [value] as a `{{...}}` placeholder pattern. */
        override fun renderPattern(value: T): String = "{{${toStr(value)}}}"
    }

    /**
     * Triple-hash placeholders, e.g. `###Name###`, `###age###`.
     */
    class TripleHash<T>(values: Set<T>, toStr: (T) -> String) : Abstract<T>(values, toStr) {

        companion object {
            /** Matches anything shaped like `###name###`, well-formed or not. */
            private val scan = "###[^#]*#{0,3}".toRegex()

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

        override val scanRegex: Regex get() = scan

        /** Renders the [value] as a `###...###` placeholder pattern. */
        override fun renderPattern(value: T): String = "###${toStr(value)}###"
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

    /**
     * Returns true when the given [text] contains valid patterns only.
     *
     * Exactly as strict as [findErrorsIn]: a text with no placeholder at all validates, but an
     * unknown name or a malformed shape does not.
     */
    fun validate(text: String): Boolean = findErrorsIn(text).isEmpty()

    /**
     * Fills the placeholders with values.
     *
     * Values are keyed by their [renderPattern], so two values rendering the same pattern collapse:
     * only the last one is reachable.
     */
    fun fill(replace: (T) -> String): Filled<T> {
        val mapping = values.associateBy(::renderPattern)

        require(mapping.size == values.size) {
            val collisions = values.groupBy(::renderPattern).filterValues { it.size > 1 }.keys
            "Placeholder patterns must be unique, but these are produced by more than one value: " +
                    collisions.joinToString()
        }

        return Filled(mapping = mapping, replace = replace)
    }
}
