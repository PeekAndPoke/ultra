package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils

abstract class CollectionPropertyRendererBase(
    override val ctx: ProcessorUtils.Context,
    protected val root: PropertyRenderers
) : PropertyRenderer {

    protected fun KotlinPrinter.internalRenderProperty(name: String, typeParam: TypeName) {

        line("val $name by lazy {").indent {
            line("getResult().$name.mutator(").indent {
                line("{ modify(getResult()::$name, getResult().$name, it) },")

                append("{ ${1.asParam} -> ")
                root.run { renderBackwardMapper(typeParam, 1) }
                line(" },")

                line("{ ${1.asParam}, ${1.asOnModify} -> ").indent {
                    root.run { renderForwardMapper(typeParam, 1) }
                    newline()
                }
                line("}")
            }
            line(")")
        }
        line("}")
    }

    fun KotlinPrinter.internalRenderForwardMapper(type: TypeName, depth: Int) {

        val plus1 = depth + 1

        append("${depth.asParam}.mutator(${depth.asOnModify}, { ${plus1.asParam} -> ")
        root.run { renderBackwardMapper(type, plus1) }
        line(" }) { ${plus1.asParam}, ${plus1.asOnModify} -> ").indent {
            root.run { renderForwardMapper(type, plus1) }
            newline()
        }
        append("}")
    }

    fun KotlinPrinter.internalRenderBackwardMapper(depth: Int) {
        append("${depth.asParam}.getResult()")
    }
}
