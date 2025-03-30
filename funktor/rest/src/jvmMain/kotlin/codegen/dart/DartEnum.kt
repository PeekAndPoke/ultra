package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.Tags
import de.peekandpoke.ktorfx.rest.codegen.dart.DartNameSanitizer.sanitizeVariableName
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter

class DartEnum(
    override val file: DartFile.Definition,
    override val tags: Tags,
    val name: String,
    val annotations: List<DartAnnotation>,
    val values: List<Value>,
) : DartFileElement {

    class Definition(
        override val file: DartFile.Definition,
        override val tags: Tags,
        val name: String,
        val builder: Definition.() -> Unit,
    ) : DartFileElement.Definition {

        private val values = mutableListOf<Value>()

        // default the annotations with the @generate annotation for the dart package enum_annotation
        internal val annotations = mutableListOf(DartAnnotation.Definition("@generate"))

        override fun implement() = apply(builder).run {
            DartEnum(
                file = file,
                tags = tags,
                name = name,
                annotations = annotations.map { it.implement() },
                values = values
            )
        }

        fun addValues(vararg names: String) {
            values.addAll(names.map { Value(it) })
        }
    }

    class Value(val name: String) : DartPrintable {
        override fun print(printer: DartCodePrinter) = printer {
            append(name)
        }
    }

    override val info: String
        get() = name

    override fun print(printer: DartCodePrinter) = printer {

        append(annotations) { nl() }

        append("enum ").append(name).append(" {").indent {
            values.forEach {
                append(it.name.sanitizeVariableName())
                append(", // ignore: constant_identifier_names")
                nl()
            }
        }
        append("}")
        nl()
    }
}
