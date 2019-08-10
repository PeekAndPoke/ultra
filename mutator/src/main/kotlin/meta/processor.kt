package de.peekandpoke.ultra.mutator.meta

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.asClassName
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.mutator.Mutable
import me.eugeniomarletti.kotlin.processing.KotlinAbstractProcessor
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement


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
            )
        }
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Mutable::class.java.canonicalName
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        generateMutatorFiles(roundEnv)

        return true
    }

    private fun generateMutatorFiles(roundEnv: RoundEnvironment) {
        // Find all types that have a Karango Annotation
        val elems = roundEnv
            .getElementsAnnotatedWith(Mutable::class.java)
            .filterIsInstance<TypeElement>()

        // Find all the types that are referenced by these types and add them to the pool
        val pool = elems.plus(
            elems.map { it.getReferencedTypesRecursive().map { tm -> typeUtils.asElement(tm) } }.flatten().distinct()
        )

        val all = pool.asSequence().filterIsInstance<TypeElement>()
            // Black list some packages
            .filter { !it.fqn.startsWith("java.") }
            .filter { !it.fqn.startsWith("javax.") }
            .filter { !it.fqn.startsWith("javafx.") }
            .filter { !it.fqn.startsWith("kotlin.") }
            .distinct()
            .filter { renderers.canHandle(it.asTypeName()) }
            .toList()

        logNote("all types (nested): $all")

        // generate code for all the relevant types
        all.forEach { buildMutatorFileFor(it) }
    }

    private fun buildMutatorFileFor(element: TypeElement) {

        logNote("Found type ${element.simpleName} in ${element.asClassName().packageName}")

        val className = element.asClassName()
        val packageName = className.packageName
        val simpleName = className.simpleName

        val codeBlocks = mutableListOf<String>()

        codeBlocks.add(
            """
                @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

                package $packageName

                import de.peekandpoke.ultra.mutator.*

                fun $simpleName.mutate(mutation: ${simpleName}Mutator.() -> Unit) = mutator().apply(mutation).getResult()

                fun $simpleName.mutator(onModify: OnModify<$simpleName> = {}) = ${simpleName}Mutator(this, onModify)

                class ${simpleName}Mutator(target: $simpleName, onModify: OnModify<$simpleName> = {}) : DataClassMutator<$simpleName>(target, onModify) {

            """.trimIndent()
        )

        element.variables
            // filter delegated properties (e.g. by lazy)
            .filter { !it.simpleName.contains("${"$"}delegate") }
            // we only look at public properties
            .filter { element.hasPublicGetterFor(it) }
            .forEach {

                val prop = it.simpleName

                codeBlocks.add("    //// $prop ".padEnd(120, '/') + System.lineSeparator())

                logNote("  '$prop' of type ${it.fqn}")

                when {
                    renderers.canHandle(it.asTypeName()) ->
                        codeBlocks.add(
                            renderers.render(it).prependIndent("    ")
                        )

                    else -> {
                        val message = "There is no known way to mutate the property $element::$prop of type ${it.fqn} yet ... sorry!"

                        logWarning("  .. $message")

                        codeBlocks.add(
                            """
                                @Deprecated("$message", level = DeprecationLevel.ERROR)
                                val $prop: Any? = null

                            """.trimIndent().prependIndent("    ")
                        )
                    }
                }
            }

        codeBlocks.add(
            """
                }
            """.trimIndent()
        )

        val content = codeBlocks.joinToString(System.lineSeparator())

        val dir = File("$generatedDir/${className.packageName.replace('.', '/')}").also { it.mkdirs() }
        val file = File(dir, "${className.simpleName}${"$$"}mutator.kt")

        file.writeText(content)
    }
}
