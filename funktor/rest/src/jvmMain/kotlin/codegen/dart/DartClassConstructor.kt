package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.Tags
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter

class DartClassConstructor(
    override val tags: Tags,
    val cls: DartClass.Definition,
    val body: DartCodePrintFn?,
) : DartClassElement {

    data class Parameters(
        val ownParams: List<DartFunctionParameter>,
        val superParams: List<DartFunctionParameter>,
    ) {
        val all by lazy { ownParams.plus(superParams).distinctBy { it.name } }

        val nonOverridden by lazy { superParams.filter { ownParams.none { own -> own.name == it.name } } }

        companion object {
            fun of(cls: DartClass.Definition): Parameters {
                // Get all properties defines on this class
                val ownParams = cls.getAllProps()
                    .filter { it.isNotStatic() }
                    .map { DartFunctionParameter(type = it.type, name = it.name) }.toMutableSet()

                // Get all properties defined on the parent classes recursively
                val superParams = mutableSetOf<DartFunctionParameter>()

                var current = cls.extends

                while (current != null) {
                    val toAdd = current.cls.getAllProps()
                        .filter { it.isNotStatic() }
                        .map { DartFunctionParameter(type = it.type, name = it.name) }

                    superParams.addAll(toAdd)

                    current = current.cls.extends
                }

                return Parameters(
                    ownParams = ownParams.toList(),
                    superParams = superParams.toList(),
                )
            }
        }
    }

    class Definition(
        override val tags: Tags,
        val classDef: DartClass.Definition,
        val body: DartCodePrintFn?,
    ) : DartClassElement.Definition {
        override fun implement(): DartClassConstructor {

            return DartClassConstructor(
                tags = tags,
                cls = classDef,
                body = body,
            )
        }
    }

    fun getParameters() = Parameters.of(cls)

    override fun print(printer: DartCodePrinter) = printer {

        val params = getParameters()

        // Print the code
        append(cls.name).append("(").indent {
            append(params.all) { append(",").nl() }
            nl()
        }
        append(")")

        if (params.superParams.isNotEmpty()) {
            append(" : super(").indent {
                append(params.superParams.map { it.name }) { append(",").nl() }
                nl()
            }
            append(")")
        }

        append(" {").indent {
            params.ownParams.forEach {
                appendLine("this.${it.name} = ${it.name};")
            }

            body?.let {
                it()
                nl()
            }
        }
        append("}")
        nl()
    }
}
