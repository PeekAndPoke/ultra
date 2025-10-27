package de.peekandpoke.monko.lang

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.math.max

/**
 * The Aql printer
 */
class MongoPrinter {

    companion object {
        /** Shared json printer */
        private val jsonPrinter = ObjectMapper().writerWithDefaultPrettyPrinter()

        /** Prints the raw query, with all parameter value included */
        fun <T> MongoExpression<T>.printRawQuery(): String = printRawQuery(this)

        /** Prints the query, with placeholders for parameters */
        fun <T> MongoExpression<T>.printQuery(): String = printQuery(this)

        /** Prints the query and returns a [Result] */
        fun <T> MongoExpression<T>.print(): Result = print(this)

        /** Prints the raw query, with all parameter value included */
        internal fun <T> printRawQuery(printable: MongoExpression<T>): String = print(printable).raw

        /** Prints the query, with placeholders for parameters */
        internal fun <T> printQuery(printable: MongoExpression<T>): String = print(printable).query

        /** Prints the query and returns a [Result] */
        internal fun <T> print(printable: MongoExpression<T>): Result = MongoPrinter().append(printable).build()
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
     * Build the [Result]
     */
    fun build() = Result(stringBuilder.toString(), queryVars)

    /**
     * Appends a name
     */
    fun name(name: String): MongoPrinter = append(name.escapeName())

    /**
     * Append a printable
     */
    fun <T> append(printable: MongoExpression<T>): MongoPrinter = apply { printable.print(this) }

    /**
     * Appends a list of printables
     */
    fun <T> append(printables: List<MongoExpression<T>>): MongoPrinter = apply { printables.forEach { append(it) } }

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun <T> join(args: List<MongoExpression<T>>): MongoPrinter = join(args.toTypedArray(), ", ")

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun <T> join(args: List<MongoExpression<T>>, delimiter: String): MongoPrinter = join(args.toTypedArray(), delimiter)

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun <T> join(args: Array<out MongoExpression<T>>): MongoPrinter = join(args, ", ")

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun <T> join(args: Array<out MongoExpression<T>>, delimiter: String): MongoPrinter = apply {

        args.forEachIndexed { idx, a ->

            append(a)

            if (idx < args.size - 1) {
                append(delimiter)
            }
        }
    }

    /**
     * Appends a raw string
     */
    fun append(str: String): MongoPrinter = apply {

        if (newLine) {
            stringBuilder.append(indent)
            newLine = false
        }

        stringBuilder.append(str)
    }

    /**
     * Appends a line break
     */
    fun nl(): MongoPrinter = appendLine("")

    /**
     * Appends a string followed by a line break
     */
    fun appendLine(str: String = ""): MongoPrinter = apply {

        append(str)
        stringBuilder.appendLine()

        newLine = true
    }

    /**
     * Increases the indent for everything added by the [block]
     */
    fun indent(block: MongoPrinter.() -> Unit): MongoPrinter = apply {
        indent += "    "

        this.block()

        indent = indent.substring(0, max(0, indent.length - 4))
    }

    /**
     * Ensures that all names are surrounded by ticks
     */
    private fun String.escapeName() = this
}
