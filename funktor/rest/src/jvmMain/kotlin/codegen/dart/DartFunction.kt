package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.CodeGenDsl
import de.peekandpoke.ktorfx.rest.codegen.Tags
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.appendDocBlock
import kotlin.reflect.KType

class DartFunction(
    override val file: DartFile.Definition,
    override val tags: Tags,
    val name: String,
    val params: List<DartFunctionParameter>,
    val body: DartCodePrintFn?,
    val returnType: DartType?,
    val docBlock: String,
    val annotations: List<DartAnnotation>,
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

        /** List of annotations added to the class */
        internal val annotations = mutableListOf<DartAnnotation.Definition>()

        override fun implement() = apply(builder).run {
            DartFunction(
                file = file,
                tags = tags,
                name = name,
                params = params.toList(),
                body = body,
                returnType = returnType,
                docBlock = docBlocks.joinToString(System.lineSeparator()),
                annotations = annotations.map { it.implement() },
                async = async
            )
        }
    }

    override val info: String
        get() = name

    @Suppress("DuplicatedCode")
    override fun print(printer: DartCodePrinter) = printer {

        appendDocBlock(docBlock)
        append(annotations) { nl() }

        returnType?.let { append(it).append(" ") }

        val mandatoryParams = params.filter { it.initialize == null }
        val optionalParams = params.filter { it.initialize != null }

        append(name).append("(").append(mandatoryParams) { append(", ") }

        if (optionalParams.isNotEmpty()) {
            if (mandatoryParams.isNotEmpty()) {
                append(", ")
            }
            append("{").append(optionalParams) { append(", ") }.append("}")
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
fun DartFunction.Definition.addParameter(type: KType, name: String, initialize: DartCodePrintFn? = null) {

    val resolved = file.resolveDartType(type)

    addParameter(resolved.type, name, initialize)
}

@CodeGenDsl
fun DartFunction.Definition.addParameter(type: DartType, name: String, initialize: DartCodePrintFn? = null) {
    params.add(DartFunctionParameter(type, name, false, initialize))
}

@CodeGenDsl
fun DartFunction.Definition.addDoc(doc: String) {
    docBlocks.add(doc)
}

@CodeGenDsl
fun DartFunction.Definition.addAnnotation(annotation: DartAnnotation.Definition) {
    annotations.add(annotation)
}

@CodeGenDsl
fun DartFunction.Definition.addAnnotation(complete: String) {
    addAnnotation(DartAnnotation.Definition(complete))
}


@CodeGenDsl
fun DartFunction.Definition.async() {
    async = true
}

@CodeGenDsl
fun DartFunction.Definition.body(str: String) {
    body = { append(str) }
}

@CodeGenDsl
fun DartFunction.Definition.body(block: DartCodePrintFn) {
    body = block
}

@CodeGenDsl
fun DartFunction.Definition.returnType(type: DartType) {
    returnType = type
}
