package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter

class DartImport(
    val uri: String,
    val keyword: String,
) : DartPrintable {

    interface Definition {
        fun implement(): DartImport
    }

    data class PureDefinition(val packageUri: String) : Definition {
        override fun implement() = DartImport(
            uri = packageUri,
            keyword = "import"
        )
    }

    data class PartDefinition(val filename: String) : Definition {
        override fun implement() = DartImport(
            uri = filename,
            keyword = "part"
        )
    }

    override fun print(printer: DartCodePrinter) = printer {
        appendLine("$keyword '$uri';")
    }
}
