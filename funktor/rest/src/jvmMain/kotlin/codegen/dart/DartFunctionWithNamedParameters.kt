package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.CodeGenDsl
import de.peekandpoke.ktorfx.rest.codegen.Tags
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.appendDocBlock
import kotlin.reflect.KType

class DartFunctionWithNamedParameters(
    override val file: DartFile.Definition,
    override val tags: Tags,
    val name: String,
    val params: List<DartFunctionParameter>,
    val body: DartCodePrintFn?,
    val returnType: DartType?,
    val docBlock: String,
    val async: Boolean,
) : DartFileElement, DartClassElement {

    class Definition(
        override val file: DartFile.Definition,
        override val tags: Tags,
        val name: String,
        val builder: Definition.() -> Unit,
    ) : DartFileElement.Definition, DartClassElement.Definition {

        internal val params = mutableListOf<DartFunctionParameter>()
        internal var body: DartCodePrintFn? = null
        internal var returnType: DartType? = null
        internal var async = false

        internal val docBlocks = mutableListOf<String>()

        override fun implement() = apply(builder).run {
            DartFunctionWithNamedParameters(
                file = file,
                tags = tags,
                name = name,
                params = params.toList(),
                body = body,
                returnType = returnType,
                docBlock = docBlocks.joinToString(System.lineSeparator()),
                async = async
            )
        }
    }

    override val info: String
        get() = name

    @Suppress("DuplicatedCode")
    override fun print(printer: DartCodePrinter) = printer {

        appendDocBlock(docBlock)

        returnType?.let { append(it).append(" ") }

        val mandatoryParams = params.filter { it.initialize == null }.map { it.copy(required = true) }
        val optionalParams = params.filter { it.initialize != null }

        append(name).append("(")

        if (params.isNotEmpty()) {
            append("{").indent {
                append(mandatoryParams) { append(", ").nl() }

                if (optionalParams.isNotEmpty()) {
                    if (mandatoryParams.isNotEmpty()) {
                        append(", ")
                    }
                    append(optionalParams) { append(", ").nl() }
                }
            }
            append("}")
        }

        append(") ")

        if (async) {
            append("async ")
        }

        if (body == null) {
            append("{}")
        } else {
            append("{")
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
fun DartFunctionWithNamedParameters.Definition.addParameter(
    type: KType,
    name: String,
    required: Boolean = false,
    initialize: DartCodePrintFn? = null,
) {

    val resolved = file.resolveDartType(type)

    addParameter(resolved.type, name, required, initialize)
}

@CodeGenDsl
fun DartFunctionWithNamedParameters.Definition.addParameter(
    type: DartType,
    name: String,
    required: Boolean = false,
    initialize: DartCodePrintFn? = null,
) {
    params.add(DartFunctionParameter(type, name, required, initialize))
}

@CodeGenDsl
fun DartFunctionWithNamedParameters.Definition.addDoc(doc: String) {
    docBlocks.add(doc)
}

@CodeGenDsl
fun DartFunctionWithNamedParameters.Definition.async() {
    async = true
}

@CodeGenDsl
fun DartFunctionWithNamedParameters.Definition.body(str: String) {
    body = { append(str) }
}

@CodeGenDsl
fun DartFunctionWithNamedParameters.Definition.body(block: DartCodePrintFn) {
    body = block
}

@CodeGenDsl
fun DartFunctionWithNamedParameters.Definition.returnType(type: DartType) {
    returnType = type
}
