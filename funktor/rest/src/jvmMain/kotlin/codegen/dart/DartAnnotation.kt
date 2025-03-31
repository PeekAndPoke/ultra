package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter

data class DartAnnotation(
    val complete: String,
) : DartPrintable {

    companion object {
        val override = Definition("@override")
    }

    data class Definition(val complete: String) {
        fun implement() = DartAnnotation(
            complete = complete
        )
    }

    override fun print(printer: DartCodePrinter) = printer {
        appendLine(complete)
    }
}
