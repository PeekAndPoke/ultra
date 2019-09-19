package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import de.peekandpoke.ultra.mutator.meta.Context
import de.peekandpoke.ultra.mutator.meta.info
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement

class DataClassRenderer(

    logPrefix: String,
    processingEnv: ProcessingEnvironment,
    private val element: TypeElement,
    context: Context,
    private val renderers: PropertyRenderers

) : RendererBase(logPrefix, processingEnv) {

    private val info = element.info

    private val fieldBlocks = mutableListOf<String>()

    private val goodVariables = element.variables
        // filter delegated properties (e.g. by lazy)
        .filter { !it.simpleName.contains("${"$"}delegate") }
        // we only look at public properties
        .filter { element.hasPublicGetterFor(it) }

    private val genericUsages = context.genericsUsages.get(element.asClassName())

    private val nonGenericVariables = goodVariables.filter { it.asTypeName() !is TypeVariableName }

    private val genericVariables = goodVariables.filter { it.asTypeName() is TypeVariableName }

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
            .filter { element.hasPublicGetterFor(it) }
            .forEach {

                val type = it.asTypeName()
                val prop = it.simpleName

                fieldBlocks.add("    //// $prop ".padEnd(120, '/'))
                fieldBlocks.add("    // -> of type      ${it.fqn}")
                fieldBlocks.add("    // -> reflected by ${type::class.java}")

                when {
                    // parameterized types are treated differently
                    renderers.canHandle(type) -> {
                        // add all imports
                        imports.addAll(renderers.getImports(type))

                        fieldBlocks.add(
                            renderers.render(it).prependIndent("    ")
                        )
                    }

                    else -> {
                        val message =
                            "There is no known way to mutate the property $element::$prop of type ${it.fqn} yet ... sorry!"

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

                package ${info.packageName}

            """.trimIndent(),

            // imports
            imports.sorted().joinToString("\n") { "import $it" },

            // extension functions
            """

                fun ${info.typeParamsStr} ${info.receiverStr}.mutate(mutation: ${info.mutatorClassStr}.() -> Unit) = 
                    mutator().apply(mutation).getResult()

                fun ${info.typeParamsStr} ${info.receiverStr}.mutator(onModify: OnModify<${info.receiverStr}> = {}) = 
                    ${info.mutatorClassStr}(this, onModify)

                class ${info.mutatorClassStr}(
                    target: ${info.receiverStr}, 
                    onModify: OnModify<${info.receiverStr}> = {}
                ) : DataClassMutator<${info.receiverStr}>(target, onModify) {

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
                        val ${info.mutatorClass(usage)}.$varName get() = 
                            getResult().$varName.mutator { modify(getResult()::$varName, getResult().$varName, it) }
                                                 
                    """.trimIndent()
                )
            }
        }

        return@lazy contentBlocks.joinToString(System.lineSeparator())
    }

    fun render() = rendered
}
