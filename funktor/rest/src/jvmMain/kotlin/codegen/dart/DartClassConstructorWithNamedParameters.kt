package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.Tags
import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter

class DartClassConstructorWithNamedParameters(
    override val tags: Tags,
    val cls: DartClass.Definition,
) : DartClassElement {

    class Definition(
        override val tags: Tags = Tags.empty,
        val classDef: DartClass.Definition,
    ) : DartClassElement.Definition {
        override fun implement(): DartClassConstructorWithNamedParameters {

            return DartClassConstructorWithNamedParameters(
                tags = tags,
                cls = classDef,
            )
        }
    }

    private fun getParameters() = DartClassConstructor.Parameters.of(cls)

    override fun print(printer: DartCodePrinter) = printer {

        val params = getParameters()

        // Print the code
        append(cls.name).append("(").indent {
            if (params.all.isNotEmpty()) {

                append("{").indent {
                    params.ownParams.forEach {
                        append("required this.").append(it.name)
                        append(", ").nl()
                    }

                    params.nonOverridden.forEach {
                        append("required ").append(it.type).append(" ").append(it.name)
                        append(", ").nl()
                    }
                }.append("}")
            }
        }
        append(")")

        if (params.superParams.isNotEmpty()) {
            append(" : super(").indent {
                params.superParams.forEach { it ->
                    append(it.name).append(": ").append(it.name)
                    append(", ").nl()
                }
            }
            append(")")
        }

        append(";")

        nl()
    }
}
