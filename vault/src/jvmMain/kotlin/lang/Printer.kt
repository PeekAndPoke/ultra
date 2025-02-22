package de.peekandpoke.ultra.vault.lang

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Common interface for all vault query code printer
 */
interface Printer {

    companion object {
        /**
         * Shared json printer
         */
        private val jsonPrinter = ObjectMapper().writerWithDefaultPrettyPrinter()
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

    /**
     * Appends a name
     */
    fun name(name: String): Printer

    /**
     * Append a printable
     */
    fun append(printable: Printable): Printer

    /**
     * Appends a list of printables
     */
    fun append(printables: List<Printable>): Printer

    /**
     * Appends an array value expression
     */
    fun <T> append(array: ArrayValueExpr<T>): Printer

    /**
     * Appends an object value expression
     */
    fun <T> append(obj: ObjectValueExpr<T>): Printer

    /**
     * Appends a user value (e.g. obtained by "abc".expr("paramName")
     *
     * If the given expression is an instance of Aliased, then the name for the user parameter will be taken from it.
     * Otherwise the name will default to "v".
     */
    fun value(expression: Expression<*>, value: Any?): Printer

    /**
     * Appends a user value with the given parameter name
     */
    fun value(parameterName: String, value: Any?): Printer

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun join(args: List<Expression<*>>): Printer

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun join(args: List<Expression<*>>, delimiter: String): Printer

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun join(args: Array<out Expression<*>>): Printer

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    fun join(args: Array<out Expression<*>>, delimiter: String): Printer

    /**
     * Appends a raw string
     */
    fun append(str: String): Printer

    /**
     * Appends a string followed by a line break
     */
    fun appendLine(str: String = ""): Printer

    /**
     * Increases the indent for everything added by the [block]
     */
    fun indent(block: Printer.() -> Unit): Printer
}
