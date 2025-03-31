package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.CodeGenDsl
import de.peekandpoke.funktor.rest.codegen.Tags
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter
import de.peekandpoke.funktor.rest.codegen.dart.printer.appendDocBlock

class DartTypedef(
    override val tags: Tags,
    override val file: DartFile.Definition,
    val name: String,
    val body: DartCodePrintFn,
    val docBlock: String?,
) : DartFileElement {

    @CodeGenDsl
    class Definition(
        override val file: DartFile.Definition,
        override val tags: Tags,
        val name: String,
        val builder: Definition.() -> Unit,
    ) : DartFileElement.Definition {

        internal var body: DartCodePrintFn = { append("") }

        /** List of doc blocks string added to the class */
        internal val docBlocks = mutableListOf<String>()

        override fun implement() = apply(builder).run {
            DartTypedef(
                tags = tags,
                file = file,
                name = name,
                body = body,
                docBlock = docBlocks.joinToString(System.lineSeparator()),
            )
        }
    }

    override val info: String
        get() = name

    override fun print(printer: DartCodePrinter) = printer {

        appendDocBlock(docBlock)

        append("typedef ").append(name).append(" = ")
        body.invoke(this)
        append(";")

        nl()
    }
}

@CodeGenDsl
fun DartTypedef.Definition.addDoc(doc: String) {
    docBlocks.add(doc)
}

@CodeGenDsl
fun DartTypedef.Definition.body(body: DartCodePrintFn) {
    this.body = body
}
