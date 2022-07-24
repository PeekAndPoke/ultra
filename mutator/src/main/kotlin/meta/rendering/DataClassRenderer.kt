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

    @Suppress("Detekt:LongMethod")
    private fun KotlinPrinter.renderFor(target: MType, mutatorClassName: ClassName) {

        // Get the short name for the mutator class
        val mutatorClassShort = mutatorClassName.simpleNames.joinToString("_")
        // Get the name for @JVMName annotation
        val jvmName = mutatorClassShort.replace("[^0-9a-zA-Z]+".toRegex(), "")
        // Import the target type
        val imported = target.import()

        // get all super interfaces
        val superMutators = target.directSuperTypes
            .filter { it.isInterface }
            .map { it.mutatorClassName.import() }
            .sorted()

        // create a template string from the super interfaces
        val superExtendsStr = when (superMutators.isEmpty()) {
            true -> ""
            else -> "${superMutators.joinToString(", ")} "
        }

        block(
            """
                @JvmName("mutate$jvmName")
                fun $imported.mutate(mutation: $mutatorClassShort.() -> Unit) = 
                    mutator({ x: $imported -> Unit }).apply(mutation).getResult()

            """.trimIndent()
        )

        when (target.isInterface) {
            //  Render an interface of a mutator  //////////////////////////////////////////////////////////////////////
            true -> {
                block(
                    """
                        @JvmName("mutator$jvmName")
                        fun $imported.mutator(onModify: OnModify<$imported> = {}): $mutatorClassShort = when (this) {
                    """.trimIndent()
                )

                indent {
                    target.directChildTypes.sortedBy { it.import() }.forEach {

                        val typeImported = it.import()
                        // TODO: code gen tests for mutator function in different package
                        val mutatorImported = it.import("mutator")

                        block(
                            """
                                is $typeImported -> $mutatorImported(onModify as OnModify<$typeImported>)
                            """.trimIndent()
                        )
                    }

                    block(
                        """
                            else -> error("Unknown child type ${"$"}{this::class}")
                        """.trimIndent()
                    )
                }

                // TODO: code gen tests for "override"
                block(
                    """
                        }
                        
                        interface $mutatorClassShort ${if (superExtendsStr.isNotEmpty()) ": $superExtendsStr" else ""}{
                            ${if (superExtendsStr.isNotEmpty()) "override " else ""}fun getResult(): $imported 
                        }
                        
                    """.trimIndent()
                )
            }

            //  Render a mutator class  ////////////////////////////////////////////////////////////////////////////////

            false -> {
                block(
                    """
                        @JvmName("mutator$jvmName")
                        fun $imported.mutator(onModify: OnModify<$imported> = {}) = 
                            $mutatorClassShort(this, onModify)
        
                        class $mutatorClassShort(
                            target: $imported, 
                            onModify: OnModify<$imported> = {}
                        ) : DataClassMutator<$imported>(target, onModify)${if (superExtendsStr.isNotEmpty()) ", $superExtendsStr" else ""} {
                        
                    """.trimIndent()
                )

                indent {
                    target.variables.filtered().forEach {
                        renderers.run { renderPropertyImplementation(it) }
                    }
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
                    line("// Mutator for ${type.import()} -> ${mutatorClassName.import()}")
                    divider()
                    newline()

                    renderFor(type, mutatorClassName)

                    newline()
                }
        }
    }
}
