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

        val interfaceName = "I${mutatorClassName}"

        val jvmName = mutatorClassName.replace("[^0-9a-zA-Z]+".toRegex(), "")

        val imported = target.import()

        block(
            """
                @JvmName("mutate${jvmName}")
                fun ${imported}.mutate(mutation: ${interfaceName}.() -> Unit) = 
                    mutator({ x: $imported -> Unit }).apply(mutation).getResult()

                @JvmName("mutator${jvmName}")
                fun ${imported}.mutator(onModify: OnModify<${imported}> = {}): $interfaceName = 
                    ${mutatorClassName}(this, onModify)

            """.trimIndent()
        )

        ////  Render the interface of the mutator class  ///////////////////////////////////////////////////////////////
        block(
            """
                interface $interfaceName : Mutator<${imported}> {
                
            """.trimIndent()
        )

        indent {
            target.variables.filtered().forEach { renderers.run { renderPropertyDeclaration(it) } }
        }

        append("}").newline().newline()

        ////  Render the mutator class  ////////////////////////////////////////////////////////////////////////////////
        block(
            """
                class ${mutatorClassName}(
                    target: ${imported}, 
                    onModify: OnModify<${imported}> = {}
                ) : DataClassMutator<${imported}>(target, onModify), $interfaceName {
                
            """.trimIndent()
        )

        indent {
            target.variables.filtered().forEach { renderers.run { renderPropertyImplementation(it) } }
        }

        append("}").newline()
    }

    fun render(printer: KotlinPrinter) = with(printer) {

        when {
            !classType.isParameterized ->
                renderFor(classType, classType.mutatorClassName)

            else ->
                classType.genericUsages.forEach { type ->

                    val mutatorClassName = type.mutatorClassName

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
