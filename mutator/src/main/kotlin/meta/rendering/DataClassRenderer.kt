package de.peekandpoke.ultra.mutator.meta.rendering

import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.MVariable

class DataClassRenderer(

    override val ctx: ProcessorUtils.Context,
    private val classType: MType,
    private val renderers: PropertyRenderers

) : ProcessorUtils {

    private fun List<MVariable>.filtered(): List<MVariable> =
        // filter delegated properties (e.g. by lazy)
        filter { !it.isDelegate }
            // we only look at public properties
            .filter { classType.hasPublicGetterFor(it) }

    private fun KotlinPrinter.renderFor(target: MType, mutatorClassName: String) {

        val jvmName = mutatorClassName.replace("[^0-9a-zA-Z]+".toRegex(), "")

        val imported = target.import()

        block(
            """
                @JvmName("mutate${jvmName}")
                fun ${imported}.mutate(mutation: ${mutatorClassName}.() -> Unit) = 
                    mutator().apply(mutation).getResult()

                @JvmName("mutator${jvmName}")
                fun ${imported}.mutator(onModify: OnModify<${imported}> = {}) = 
                    ${mutatorClassName}(this, onModify)

                class ${mutatorClassName}(
                    target: ${imported}, 
                    onModify: OnModify<${imported}> = {}
                ) : DataClassMutator<${imported}>(target, onModify) {
                
            """.trimIndent()
        )

        indent {

            target.variables.filtered().forEach { variable ->

                val type = variable.typeName
                val prop = variable.simpleName

                block(
                    """
                        /**
                         * Mutator for field [${imported}.$prop]
                         *
                         * Info:
                         *   - type:         [${variable.typeName.import()}]
                         *   - reflected by: [${type::class.qualifiedName}]
                         */ 
                    """.trimIndent()
                )

                renderers.run {
                    renderProperty(variable.typeName, variable.simpleName)
                }
            }
        }

        append("}").newline()
    }

    fun render(printer: KotlinPrinter) = with(printer) {

        when {
            !classType.isParameterized ->
                renderFor(classType, classType.joinSimpleNames() + "Mutator")

            else ->
                classType.genericUsages.forEachIndexed { idx, type ->

                    val mutatorClassName = type.joinSimpleNames() + "Mutator_" + idx

                    divider()
                    line("// Mutator for ${type.import()} -> $mutatorClassName")
                    divider()
                    newline()

                    renderFor(type, mutatorClassName)

                    newline()
                }
        }
    }
}
