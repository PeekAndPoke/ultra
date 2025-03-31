package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter

data class DartFunctionParameter(
    val type: DartType,
    val name: String,
    val required: Boolean = false,
    val initialize: DartCodePrintFn? = null,
) : DartPrintable {
    override fun print(printer: DartCodePrinter) = printer {
        append(if (required) "required " else "").append(type).append(" ").append(name)

        initialize?.let {
            append(" = ")
            it.invoke(this)
        }
    }
}
