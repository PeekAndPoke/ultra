package de.peekandpoke.ultra.mutator.meta

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import de.peekandpoke.ultra.common.startsWithNone
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.mutator.Mutable
import me.eugeniomarletti.kotlin.processing.KotlinAbstractProcessor
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement


@Suppress("unused")
@AutoService(Processor::class)
open class MutatorAnnotationProcessor : KotlinAbstractProcessor(), ProcessorUtils {

    override val logPrefix: String = "[Mutator] "

    private val renderers by lazy {

        // TODO: check for collections

        CodeRenderers(logPrefix, env) { root ->
            listOf(
                PrimitiveOrStringOrAnyTypeCodeRenderer(logPrefix, env),
                ListAndSetCodeRenderer(root, logPrefix, env),
                MapCodeRenderer(root, logPrefix, env),
                DataClassCodeRenderer(logPrefix, env)
//                GenericDataClassCodeRenderer(logPrefix, env)
            )
        }
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Mutable::class.java.canonicalName
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        buildContext(roundEnv).apply {
            types.forEach {
                // generate code for all the relevant types
                buildMutatorFileFor(it, this)
            }
        }

        return true
    }

    private fun buildContext(roundEnv: RoundEnvironment): Context {
        // Find all types that have a Karango Annotation
        val elems = roundEnv
            .getElementsAnnotatedWith(Mutable::class.java)
            .filterIsInstance<TypeElement>()

        // Find all the types that are referenced by these types and add them to the pool
        val pool = elems.plus(
            elems.map { it.getReferencedTypesRecursive().map { tm -> typeUtils.asElement(tm) } }.flatten().distinct()
        )

        val blacklisted = arrayOf("java.", "javax.", "javafx.", "kotlin.")

        // Black list some packages
        fun List<TypeElement>.blacklist() = asSequence()
            .distinct()
            .filter { it.fqn.startsWithNone(blacklisted) }
            .toList()

        fun List<ParameterizedTypeName>.blacklist() = asSequence()
            .distinct()
            .filter { it.fqn.startsWithNone(blacklisted) }
            .toList()

        val all: List<TypeElement> = pool.filterIsInstance<TypeElement>().blacklist()

        logNote("all types (nested): $all")


        val allGenericTypesUsed = all.map { element ->
            element.variables
                .map { it.asTypeName() }
                .filterIsInstance<ParameterizedTypeName>()
                .blacklist()
        }.fold(mutableMapOf<ClassName, MutableSet<ParameterizedTypeName>>()) { acc, elements ->
            elements.forEach { element ->
                acc.getOrPut(element.rawType) { mutableSetOf() }.add(element)
            }
            // return the acc
            acc
        }

        logNote("all generic types used")
        logNote(allGenericTypesUsed.toString())

        return Context(all, allGenericTypesUsed)
    }

    private fun buildMutatorFileFor(element: TypeElement, context: Context) {

        logNote("Found type ${element.simpleName} in ${element.asClassName().packageName}")

        val className = element.asClassName()
        val typeName = element.asTypeName()
        val packageName = className.packageName
        val simpleName = className.simpleName

        val typeParams = when (typeName) {
            is ParameterizedTypeName ->
                "<" + typeName.typeArguments.mapIndexed { idx, _ -> "P${idx}" }.joinToString(", ") + ">"

            else -> ""
        }

        val fieldBlocks = mutableListOf<String>()
        val extensionBlock = mutableListOf<String>()
        val imports = mutableSetOf("de.peekandpoke.ultra.mutator.*")

        element.variables
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
                    type is TypeVariableName -> {
                        fieldBlocks.add(
                            "    // type variables will be rendered as extension properties"
                        )
                        fieldBlocks.add("")


                    }

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

        val contentBlocks = listOf(
            // file header
            """
                @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

                package $packageName

            """.trimIndent(),

            // imports
            imports.sorted().joinToString("\n") { "import $it" },

            // extension functions
            """

                fun $typeParams $simpleName$typeParams.mutate(mutation: ${simpleName}Mutator$typeParams.() -> Unit) = 
                    mutator().apply(mutation).getResult()

                fun $typeParams $simpleName$typeParams.mutator(onModify: OnModify<$simpleName$typeParams> = {}) = 
                    ${simpleName}Mutator(this, onModify)

                class ${simpleName}Mutator$typeParams(
                    target: $simpleName$typeParams, 
                    onModify: OnModify<$simpleName$typeParams> = {}
                ) : DataClassMutator<$simpleName$typeParams>(target, onModify) {

            """.trimIndent(),

            // fields
            *fieldBlocks.toTypedArray(),

            // closing mutator class
            """
                }
            """.trimIndent()
        )

        val content = contentBlocks.joinToString(System.lineSeparator())

        val dir = File("$generatedDir/${className.packageName.replace('.', '/')}").also { it.mkdirs() }
        val file = File(dir, "${className.simpleName}${"$$"}mutator.kt")

        file.writeText(content)
    }
}
