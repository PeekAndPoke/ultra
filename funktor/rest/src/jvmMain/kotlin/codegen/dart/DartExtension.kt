package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.CodeGenDsl
import de.peekandpoke.ktorfx.rest.codegen.Tags
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.appendDocBlock

class DartExtension(
    override val tags: Tags,
    override val file: DartFile.Definition,
    val name: String,
    val on: DartType,
    val annotations: List<DartAnnotation>,
    val elements: List<DartClassElement>,
    val docBlock: String?,
) : DartFileElement {

    @CodeGenDsl
    class Definition(
        override val file: DartFile.Definition,
        override val tags: Tags,
        val name: String,
        val on: DartType,
        val builder: Definition.() -> Unit,
    ) : DartFileElement.Definition {

        private val docBlocks = mutableListOf<String>()

        private val annotations = mutableListOf<DartAnnotation.Definition>()

        private val elements = mutableListOf<DartClassElement.Definition>()

        override fun implement() = apply(builder).run {
            DartExtension(
                tags = tags,
                file = file,
                name = name,
                on = on,
                annotations = annotations.map { it.implement() },
                elements = elements.map { it.implement() },
                docBlock = docBlocks.joinToString(System.lineSeparator()),
            )
        }

        fun getElements() = elements

        inline fun <reified T> filterElements() = getElements().filterIsInstance<T>()

        @CodeGenDsl
        fun addDoc(doc: String) {
            docBlocks.add(doc)
        }

        @CodeGenDsl
        fun addAnnotation(complete: String) {
            DartAnnotation.Definition(complete).apply { annotations.add(this) }
        }

        @CodeGenDsl
        fun addFunction(name: String, tags: Tags = Tags.empty, builder: DartFunction.Definition.() -> Unit) {
            elements.add(
                DartFunction.Definition(this.file, tags, name, builder)
            )
        }
    }

    override val info: String
        get() = name

    override fun print(printer: DartCodePrinter) = printer {

        appendDocBlock(docBlock)

        append(annotations) { nl() }

        append("extension ").append(name).append(" on ").append(on).append(" {").indent {
            elements.forEach { append(it).nl() }
        }
        append("}")

        nl()
    }
}
