package de.peekandpoke.karango.aql

import com.fasterxml.jackson.databind.ObjectMapper
import de.peekandpoke.ultra.vault.lang.Aliased
import kotlin.math.max


/** The Aql printer */
class AqlPrinter {

    companion object {
        /** Shared json printer */
        private val jsonPrinter = ObjectMapper().writerWithDefaultPrettyPrinter()

        /** Prints the raw query, with all parameter value included */
        fun <T> AqlExpression<T>.printRawQuery(): String = printRawQuery(this)

        /** Prints the query, with placeholders for parameters */
        fun <T> AqlExpression<T>.printQuery(): String = printQuery(this)

        /** Prints the query and returns a [AqlPrinter.Result] */
        fun <T> AqlExpression<T>.print(): Result = print(this)

        /** Prints the raw query, with all parameter value included */
        internal fun <T> printRawQuery(printable: AqlExpression<T>): String = print(printable).raw

        /** Prints the query, with placeholders for parameters */
        internal fun <T> printQuery(printable: AqlExpression<T>): String = print(printable).query

        /** Prints the query and returns a [AqlPrinter.Result] */
        internal fun <T> print(printable: AqlExpression<T>): Result = AqlPrinter().append(printable).build()
    }

    /**
     * Printer result
     */
    data class Result(val query: String, val vars: Map<String, Any?>) {

        /**
         * The raw text query with parameters (like @my_param) replaced with actual values.
         *
         * This is a debugging helper and e.g. used in the unit tests.
         */
        val raw: String by lazy(LazyThreadSafetyMode.NONE) {

            vars.entries.fold(query) { acc, (key, value) ->
                acc.replace("@$key", jsonPrinter.writeValueAsString(value))
            }
        }
    }

    private val stringBuilder = StringBuilder()
    private val queryVars = mutableMapOf<String, Any?>()

    private var indent = ""
    private var newLine = true

    /**
     * Build the [AqlPrinter.Result]
     */
    fun build() = Result(stringBuilder.toString(), queryVars)

    /**
     * Appends a name
     */
    fun name(name: String): AqlPrinter = append(name.toName())

    /**
     * Append a printable
     */
    fun <T> append(printable: AqlExpression<T>): AqlPrinter = apply { printable.print(this) }

    /**
     * Appends a list of printables
     */
    fun <T> append(printables: List<AqlExpression<T>>): AqlPrinter = apply { printables.forEach { append(it) } }

    /**
     * Appends an array value expression
     */
    fun <T> append(array: AqlArrayValueExpr<T>): AqlPrinter = apply {
        append("[").join(array.items).append("]")
    }

    /**
     * Appends an object value expression
     */
    fun <T> append(obj: AqlObjectValueExpr<T>): AqlPrinter = apply {

        val lastIdx = obj.pairs.size - 1

        append("{")

        indent {
            obj.pairs.forEachIndexed { idx, p ->

                append(p.first).append(": ").append("(").append(p.second).append(")")

                if (idx < lastIdx) {
                    append(", ")
                }
            }
        }

        append("}")
    }

    /**
     * Appends a user value (e.g. obtained by "abc".aql("paramName")
     *
     * If the given expression is an instance of Aliased, then the name for the user parameter will be taken from it.
     * Otherwise the name will default to "v".
     */
    fun <T> value(expression: AqlExpression<T>, value: T?): AqlPrinter =
        value(if (expression is Aliased) expression.getAlias() else "v", value)

    /**
     * Appends a user value with the given parameter name
     */
    fun <T> value(parameterName: String, value: T?): AqlPrinter = apply {

        // check if there already is a parameter with the exact same value, starting with the same name
        val existing = queryVars.entries
            .firstOrNull { (k, v) -> v == value && k.startsWith(parameterName) }
            ?.key

        when (existing) {
            null -> {
                val key = parameterName.toParamName() + "_" + (queryVars.size + 1)

                append("@$key")
                queryVars[key] = value
            }

            else -> {
                append("@$existing")
            }
        }
    }

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun <T> join(args: List<AqlExpression<out T>>): AqlPrinter = join(args.toTypedArray(), ", ")

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun <T> join(args: List<AqlExpression<out T>>, delimiter: String): AqlPrinter = join(args.toTypedArray(), delimiter)

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun <T> join(args: Array<out AqlExpression<out T>>): AqlPrinter = join(args, ", ")

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun <T> join(args: Array<out AqlExpression<out T>>, delimiter: String): AqlPrinter = apply {

        args.forEachIndexed { idx: Int, a: AqlExpression<out T> ->

            append(a)

            if (idx < args.size - 1) {
                append(delimiter)
            }
        }
    }

    /**
     * Appends a raw string
     */
    fun append(str: String): AqlPrinter = apply {

        if (newLine) {
            stringBuilder.append(indent)
            newLine = false
        }

        stringBuilder.append(str)
    }

    /**
     * Appends a line break
     */
    fun nl(): AqlPrinter = appendLine("")

    /**
     * Appends a string followed by a line break
     */
    fun appendLine(str: String = ""): AqlPrinter = apply {
        append(str)
        stringBuilder.appendLine()

        newLine = true
    }

    /**
     * Increases the indent for everything added by the [block]
     */
    fun indent(block: AqlPrinter.() -> Unit): AqlPrinter = apply {

        indent += "    "

        this.block()

        indent = indent.substring(0, max(0, indent.length - 4))
    }

    /**
     * Cleans up parameter names
     */
    private fun String.toParamName() = this
        .replace("`", "")
        .replace(".", "__")
        .replace("[*]", "_STAR")
        .replace("[**]", "_STAR2")
        .replace("[^a-zA-Z0-9_]".toRegex(), "_")

    /**
     * Ensures that all names are surrounded by ticks
     */
    private fun String.toName() = split(".").joinToString(".", transform = ::tick)

    /**
     * Surround a string with ticks and replaces all inner ticks with a "?"
     */
    private fun tick(str: String) = "`${str.replace("`", "?")}`"
}
