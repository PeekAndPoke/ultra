package io.peekandpoke.funktor.rest.codegen.dart

import io.peekandpoke.funktor.rest.codegen.CodeGenDsl
import io.peekandpoke.funktor.rest.codegen.Tags
import io.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrintFn
import io.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter

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
