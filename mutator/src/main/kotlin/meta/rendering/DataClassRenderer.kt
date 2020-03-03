package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ClassName
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

    private fun KotlinPrinter.renderFor(target: MType, mutatorClassName: ClassName) {

        // Get the short name for the mutator class
        val mutatorClassShort = mutatorClassName.simpleNames.joinToString("_")
        // Get the name for @JVMName annotation
        val jvmName = mutatorClassShort.replace("[^0-9a-zA-Z]+".toRegex(), "")
        // Import the target type
        val imported = target.import()

        block(
            """
                @JvmName("mutate${jvmName}")
                fun ${imported}.mutate(mutation: ${mutatorClassShort}.() -> Unit) = 
                    mutator({ x: $imported -> Unit }).apply(mutation).getResult()

                @JvmName("mutator${jvmName}")
                fun ${imported}.mutator(onModify: OnModify<${imported}> = {}): $mutatorClassShort = 
                    ${mutatorClassShort}(this, onModify)

            """.trimIndent()
        )

        when (target.isInterface) {
            ////  Render an interface of a mutator  ////////////////////////////////////////////////////////////////////
            true -> {
                block(
                    """
                        interface $mutatorClassShort : Mutator<${imported}>
                        
                    """.trimIndent()
                )
            }


            ////  Render a mutator class  //////////////////////////////////////////////////////////////////////////////
            false -> {

                block(
                    """
                        class ${mutatorClassShort}(
                            target: ${imported}, 
                            onModify: OnModify<${imported}> = {}
                        ) : DataClassMutator<${imported}>(target, onModify) {
                        
                    """.trimIndent()
                )

                indent {
                    target.variables.filtered().forEach { renderers.run { renderPropertyImplementation(it) } }
                }

                append("}").newline()
            }
        }
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
