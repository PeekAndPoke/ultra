package de.peekandpoke.ktorfx.rest.codegen.dart.printer

import de.peekandpoke.ktorfx.rest.codegen.dart.DartFile
import de.peekandpoke.ktorfx.rest.codegen.dart.DartPrintable
import java.io.File

typealias DartCodePrintFn = DartCodePrinter.() -> Unit

fun dartCodePrinterFn(block: DartCodePrintFn) = block

class DartCodePrinter {

    private val stringBuilder: StringBuilder = StringBuilder()

    private var indent = ""

    private var isNewLine: Boolean = false

    data class PrintedFile(val file: DartFile, val content: String)

    data class WrittenFile(val path: File, val printed: PrintedFile)

    companion object {
        fun print(file: DartFile) = PrintedFile(
            file,
            DartCodePrinter().append(file).build()
        )
    }

    fun build() = stringBuilder.toString()

    operator fun invoke(block: DartCodePrinter.() -> Any?) {
        this.block()
    }

    @JvmName("appendStrings")
    fun append(strings: List<String>, glue: DartCodePrintFn = {}) = apply {
        strings.forEachIndexed { idx, it ->
            append(it)
            if (idx < strings.size - 1) {
                glue()
            }
        }
    }

    @JvmName("appendPrintables")
    fun append(printables: List<DartPrintable>, glue: DartCodePrintFn = {}) = apply {
        printables.forEachIndexed { idx, it ->
            append(it)
            if (idx < printables.size - 1) {
                glue()
            }
        }
    }

    fun append(printable: DartPrintable) = apply {
        printable.print(this)
    }

    fun appendWhen(condition: Boolean, string: String) = apply {
        if (condition) {
            append(string)
        }
    }

    fun appendWhen(condition: Boolean, printable: DartPrintable) = apply {
        if (condition) {
            append(printable)
        }
    }

    fun <T> join(items: List<T>, glue: String = ", ", builder: DartCodePrinter.(T) -> Any?) = apply {

        items.forEachIndexed { idx, item ->
            builder(item)

            if (idx < items.size - 1) {
                append(glue)
            }
        }
    }

    fun indent(builder: DartCodePrinter.() -> Unit) = apply {

        val oldIndent = indent

        indent += "  "

        nl()
        apply(builder)

        indent = oldIndent
    }

    fun appendLine(string: String) = append(string).nl()

    fun append(string: String) = apply {

        val parts = string.split(System.lineSeparator())

        when (parts.size == 1) {

            true -> {
                if (isNewLine) {
                    stringBuilder.append(indent)
                    isNewLine = false
                }

                stringBuilder.append(parts[0])
            }

            false -> parts.forEachIndexed { idx, part ->

                if (isNewLine) {
                    stringBuilder.append(indent)
                    isNewLine = false
                }

                stringBuilder.append(part)

                if (idx < parts.size - 1) {
                    nl()
                }
            }
        }
    }

    fun nl(count: Int = 1) = apply {

        repeat(count) {
            stringBuilder.appendLine()
        }

        isNewLine = true
    }
}
