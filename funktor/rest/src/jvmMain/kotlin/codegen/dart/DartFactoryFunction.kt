package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.CodeGenDsl
import de.peekandpoke.funktor.rest.codegen.Tags
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter

class DartFactoryFunction(
    val cls: DartClass.Definition,
    override val tags: Tags,
    val name: String,
    val params: List<DartFunctionParameter>,
    val body: DartCodePrintFn?,
) : DartFileElement, DartClassElement {

    class Definition(
        val cls: DartClass.Definition,
        override val tags: Tags,
        val name: String,
        val builder: Definition.() -> Unit,
    ) : DartFileElement.Definition, DartClassElement.Definition {

        override val file: DartFile.Definition get() = cls.file

        internal val params = mutableListOf<DartFunctionParameter>()

        internal var body: DartCodePrintFn? = null

        override fun implement() = apply(builder).run {
            DartFactoryFunction(
                cls = cls,
                tags = tags,
                name = name,
                params = params.toList(),
                body = body,
            )
        }

        fun addParameter(type: DartType, name: String) {
            params.add(DartFunctionParameter(type, name))
        }
    }

    override val file: DartFile.Definition get() = cls.file

    @Suppress("DuplicatedCode")
    override fun print(printer: DartCodePrinter) = printer {

        append("factory ").append(cls.name).append(".")

        append(name).append("(").append(params) { append(", ") }.append(")")

        if (body == null) {
            append(" {}")
        } else {
            append(" {")
            indent {
                body.invoke(this)
                nl()
            }
            append("}")
        }

        nl()
    }
}

@CodeGenDsl
fun DartFactoryFunction.Definition.addParameter(
    type: DartType,
    name: String,
    required: Boolean = false,
    initialize: DartCodePrintFn? = null,
) {
    params.add(DartFunctionParameter(type, name, required, initialize))
}

@CodeGenDsl
fun DartFactoryFunction.Definition.body(str: String) {
    body = { append(str) }
}

@CodeGenDsl
fun DartFactoryFunction.Definition.body(block: DartCodePrintFn) {
    body = block
}
