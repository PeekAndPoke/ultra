package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.Tags
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter

class DartComment(
    override val file: DartFile.Definition,
    override val tags: Tags,
    val comment: String,
) : DartFileElement {

    class Definition(
        override val file: DartFile.Definition,
        override val tags: Tags,
        val builder: Definition.() -> Unit,
    ) : DartFileElement.Definition {

        var comment: String = ""

        override fun implement() = apply(builder).run {
            DartComment(
                file = file,
                tags = tags,
                comment = comment
            )
        }
    }

    override val info: String
        get() = ""

    override fun print(printer: DartCodePrinter) = printer {

        val lines = comment.split(System.lineSeparator())

        if (lines.size == 1) {
            appendLine("// ${lines[0]}")
        } else {
            appendLine("/**")
            lines.forEach {
                appendLine(" * $it")
            }
            appendLine(" */")
        }
    }
}
