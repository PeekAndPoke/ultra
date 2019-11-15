package de.peekandpoke.ultra.mutator.meta

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.asClassName
import de.peekandpoke.ultra.common.startsWithNone
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.rendering.*
import me.eugeniomarletti.kotlin.processing.KotlinAbstractProcessor
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement


@Suppress("unused")
@AutoService(Processor::class)
open class MutatorAnnotationProcessor : KotlinAbstractProcessor(), ProcessorUtils {

    override val logPrefix: String = "[Mutator] "

    private val renderers by lazy {

        // TODO: check for collections

        PropertyRenderers(logPrefix, env) { root ->
            listOf(
                ListAndSetPropertyRenderer(root, logPrefix, env),
                MapPropertyRenderer(root, logPrefix, env),
                DataClassPropertyRenderer(logPrefix, env),
                // Fallback for primitive types, String, Any, and all others that or not support yet
                PureGetterSetterRenderer(logPrefix, env)
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
        fun <T : Element> List<T>.blacklist() = asSequence()
            .distinct()
            .filter { it.fqn.startsWithNone(blacklisted) }
            .toList()

        val all: List<TypeElement> = pool.filterIsInstance<TypeElement>().blacklist()

        logNote("all types (nested): $all")

        val allGenericTypesUsed = all.fold(GenericUsages(logPrefix, env)) { acc, type -> acc.add(type) }

        logNote("all generic types used")
        logNote(allGenericTypesUsed.registry.toString())

        return Context(all, allGenericTypesUsed)
    }

    private fun buildMutatorFileFor(element: TypeElement, context: Context) {

        logNote("Found type ${element.simpleName} in ${element.asClassName().packageName}")

        val content = DataClassRenderer(logPrefix, env, element, context, renderers).render()

        val info = element.info

        val dir = File("$generatedDir/${info.packageName.replace('.', '/')}").also { it.mkdirs() }
        val file = File(dir, "${info.simpleName}${"$$"}mutator.kt")

        file.writeText(content)
    }
}
