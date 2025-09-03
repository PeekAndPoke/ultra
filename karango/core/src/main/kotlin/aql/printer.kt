package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.Aliased
import de.peekandpoke.ultra.vault.lang.ArrayValueExpr
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.ObjectValueExpr
import de.peekandpoke.ultra.vault.lang.Printable
import de.peekandpoke.ultra.vault.lang.Printer
import kotlin.math.max

/**
 * Prints the raw query, with all parameter value included
 */
fun Printable.printRawQuery(): String = AqlPrinter.printRawQuery(this)

/**
 * Prints the query, with placeholders for parameters
 */
fun Printable.printQuery(): String = AqlPrinter.printQuery(this)

/**
 * Prints the query and returns a [Printer.Result]
 */
fun Printable.print(): Printer.Result = AqlPrinter.print(this)

/**
 * The Aql printer
 */
class AqlPrinter : Printer {

    /**
     * Internal static helpers
     */
    companion object {
        internal fun printRawQuery(printable: Printable): String = print(printable).raw

        internal fun printQuery(printable: Printable): String = print(printable).query

        internal fun print(printable: Printable): Printer.Result = AqlPrinter().append(printable).build()
    }

    private val stringBuilder = StringBuilder()
    private val queryVars = mutableMapOf<String, Any?>()

    private var indent = ""
    private var newLine = true

    /**
     * Build the [Printer.Result]
     */
    fun build() = Printer.Result(stringBuilder.toString(), queryVars)

    /**
     * Appends a name
     */
    override fun name(name: String): AqlPrinter = append(name.toName())

    /**
     * Append a printable
     */
    override fun append(printable: Printable): AqlPrinter = apply { printable.print(this) }

    /**
     * Appends a list of printables
     */
    override fun append(printables: List<Printable>): AqlPrinter = apply { printables.forEach { append(it) } }

    /**
     * Appends an array value expression
     */
    override fun <T> append(array: ArrayValueExpr<T>): Printer = apply {
        append("[").join(array.items).append("]")
    }

    /**
     * Appends an object value expression
     */
    override fun <T> append(obj: ObjectValueExpr<T>): Printer = apply {

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
    override fun value(expression: Expression<*>, value: Any?): AqlPrinter =
        value(if (expression is Aliased) expression.getAlias() else "v", value)

    /**
     * Appends a user value with the given parameter name
     */
    override fun value(parameterName: String, value: Any?): AqlPrinter = apply {

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
    override fun join(args: List<Expression<*>>): AqlPrinter = join(args.toTypedArray(), ", ")

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    override fun join(args: List<Expression<*>>, delimiter: String): AqlPrinter = join(args.toTypedArray(), delimiter)

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    override fun join(args: Array<out Expression<*>>): AqlPrinter = join(args, ", ")

    /**
     * Appends multiple expression and put the delimiter between each of them
     */
    override fun join(args: Array<out Expression<*>>, delimiter: String): AqlPrinter = apply {

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
    override fun append(str: String): AqlPrinter = apply {

        if (newLine) {
            stringBuilder.append(indent)
            newLine = false
        }

        stringBuilder.append(str)
    }

    /**
     * Appends a line break
     */
    fun appendLine(): AqlPrinter = appendLine("")

    /**
     * Appends a string followed by a line break
     */
    override fun appendLine(str: String): AqlPrinter = apply {

        append(str)
        stringBuilder.appendLine()

        newLine = true
    }

    /**
     * Increases the indent for everything added by the [block]
     */
    override fun indent(block: Printer.() -> Unit): AqlPrinter = apply {

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
