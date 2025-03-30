package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.CodeGenDsl
import de.peekandpoke.ktorfx.rest.codegen.Tags
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.appendDocBlock
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.appendModifiers

class DartProperty(
    override val file: DartFile.Definition,
    override val tags: Tags,
    val name: String,
    val type: DartType,
    val docBlock: String?,
    val modifiers: Set<DartModifier>,
    val annotations: List<DartAnnotation>,
    val initialize: DartCodePrintFn?,
) : DartFileElement, DartClassElement {

    class Definition(
        override val file: DartFile.Definition,
        override val tags: Tags,
        val modifiers: Set<DartModifier>,
        val name: String,
        val type: DartType,
        val builder: Definition.() -> Unit,
    ) : DartFileElement.Definition, DartClassElement.Definition {

        internal val docBlocks = mutableListOf<String>()

        internal val annotations = mutableListOf<DartAnnotation.Definition>()

        internal var initialize: DartCodePrintFn? = null

        fun isStatic() = modifiers.contains(DartModifier.static)
        fun isNotStatic() = !isStatic()

        override fun implement() = apply(builder).run {
            DartProperty(
                file = file,
                tags = tags,
                name = name,
                type = type,
                docBlock = docBlocks.joinToString(System.lineSeparator()),
                modifiers = modifiers,
                annotations = annotations.map { it.implement() },
                initialize = initialize,
            )
        }
    }

    override fun print(printer: DartCodePrinter) = printer {

        appendDocBlock(docBlock)
        append(annotations) { nl() }

        appendModifiers(modifiers).append(type).append(" ").append(name)

        // Add initializer if given
        initialize?.let {
            append(" = ")
            it()
        }

        append(";")

        nl()
    }
}

@CodeGenDsl
fun DartProperty.Definition.addDoc(doc: String) {
    docBlocks.add(doc)
}

@CodeGenDsl
fun DartProperty.Definition.addAnnotation(annotation: DartAnnotation.Definition) {
    annotations.add(annotation)
}

@CodeGenDsl
fun DartProperty.Definition.addAnnotation(complete: String) {
    addAnnotation(DartAnnotation.Definition(complete))
}

@CodeGenDsl
fun DartProperty.Definition.initialize(code: DartCodePrintFn) {
    initialize = code
}
