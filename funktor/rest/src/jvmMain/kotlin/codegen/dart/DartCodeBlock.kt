package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.CodeGenDsl
import de.peekandpoke.ktorfx.rest.codegen.Tags
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter

class DartCodeBlock(
    override val file: DartFile.Definition,
    override val tags: Tags = Tags.empty,
    val code: DartCodePrintFn,
) : DartFileElement, DartClassElement {

    @CodeGenDsl
    class Definition(
        override val file: DartFile.Definition,
        override val tags: Tags = Tags.empty,
        val code: DartCodePrintFn,
    ) : DartFileElement.Definition, DartClassElement.Definition {

        override fun implement() = DartCodeBlock(file, tags, code)
    }

    override val info: String
        get() = "CodeBlock"

    override fun print(printer: DartCodePrinter) = printer {
        code()
    }
}
