package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.MVariable
import de.peekandpoke.ultra.mutator.meta.RenderHelper

class DataClassRenderer(

    override val ctx: ProcessorUtils.Context,
    private val type: MType,
    private val renderers: PropertyRenderers

) : ProcessorUtils {

    private val helper = RenderHelper(type)

    private val fieldBlocks = mutableListOf<String>()

    private val goodVariables: List<MVariable> = type.variables
        // filter delegated properties (e.g. by lazy)
        .filter { !it.isDelegate }
        // we only look at public properties
        .filter { type.hasPublicGetterFor(it) }

    private val genericUsages = type.genericUsages

    private val nonGenericVariables = goodVariables.filter { !it.isGeneric }

    private val genericVariables = goodVariables.filter { it.isGeneric }

    private val imports = mutableSetOf(
        "de.peekandpoke.ultra.mutator.*",
        *genericUsages
            .flatMap { it.typeArguments }.mapNotNull {
                when (it) {
                    is ParameterizedTypeName -> it.rawType.packageName + ".mutator"

                    is ClassName -> it.packageName + ".mutator"

                    else -> null
                }
            }.toTypedArray()
    )

    private val rendered by lazy {

        nonGenericVariables
            // filter delegated properties (e.g. by lazy)
            .filter { !it.simpleName.contains("${"$"}delegate") }
            // we only look at public properties
            .filter { type.hasPublicGetterFor(it) }
            .forEach { variable ->

                val fqn = variable.fqn
                val type = variable.typeName
                val prop = variable.simpleName

                fieldBlocks.add(
                    """
                    /**
                     * Mutator for field [${helper.receiverName}.$prop]
                     *
                     * Info:
                     *   - type:         $fqn
                     *   - reflected by: ${type::class.qualifiedName}
                     */ 
                """.trimIndent().prependIndent("    ")
                )

                when {
                    // parameterized types are treated differently
                    renderers.canHandle(type) -> {
                        // add all imports
                        imports.addAll(renderers.getImports(variable))

                        fieldBlocks.add(
                            renderers.render(variable).prependIndent("    ")
                        )
                    }

                    else -> {
                        val message =
                            "There is no known way to mutate the property ${this.type}::$prop of type $fqn yet ... sorry!"

                        logWarning("  .. $message")

                        fieldBlocks.add(
                            """
                                @Deprecated("$message", level = DeprecationLevel.ERROR)
                                val $prop: Any? = null

                            """.trimIndent().prependIndent("    ")
                        )
                    }
                }
            }

        val contentBlocks = mutableListOf(
            // file header
            """
                @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

                package ${type.packageName}

            """.trimIndent(),

            // imports
            imports.sorted().joinToString("\n") { "import $it" },

            // extension functions
            """

                fun ${helper.generics}${helper.receiverName}.mutate(mutation: ${helper.mutatorClassName}.() -> Unit) = 
                    mutator().apply(mutation).getResult()

                fun ${helper.generics}${helper.receiverName}.mutator(onModify: OnModify<${helper.receiverName}> = {}) = 
                    ${helper.mutatorClassName}(this, onModify)

                class ${helper.mutatorClassName}(
                    target: ${helper.receiverName}, 
                    onModify: OnModify<${helper.receiverName}> = {}
                ) : DataClassMutator<${helper.receiverName}>(target, onModify) {
                
            """.trimIndent(),

            // fields
            *fieldBlocks.toTypedArray(),

            // closing mutator class
            """
                }
                
            """.trimIndent()
        )

        genericVariables.forEach { variable ->

            val varName = variable.simpleName

            contentBlocks.add("///////////////////////////////////////////////////////////////////////////////////////")
            contentBlocks.add("// ${variable.simpleName} generics")

            genericUsages.forEach { usage ->

                contentBlocks.add(
                    """
                        val ${helper.mutatorClass(usage)}.$varName get() = 
                            getResult().$varName.mutator { modify(getResult()::$varName, getResult().$varName, it) }
                                                 
                    """.trimIndent()
                )
            }
        }

        return@lazy contentBlocks.joinToString(System.lineSeparator())
    }

    fun render() = rendered
}
