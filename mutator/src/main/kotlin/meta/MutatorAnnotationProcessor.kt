package de.peekandpoke.ultra.mutator.meta

import com.google.auto.service.AutoService
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.KotlinProcessor
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.model
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.NotMutable
import de.peekandpoke.ultra.mutator.meta.rendering.*
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@Suppress("unused")
@AutoService(Processor::class)
open class MutatorAnnotationProcessor : KotlinProcessor("[Mutator]") {

    private fun createRenderers(notMutableTypes: List<TypeElement>): PropertyRenderers {

        return PropertyRenderers(
            ctx,
            PureGetterSetterRenderer(ctx)
        ) { root ->
            listOf(
                // Lists
                CollectionListPropertyRenderer(ctx, root),
                // Sets
                CollectionSetPropertyRenderer(ctx, root),
                // Maps
                CollectionMapPropertyRenderer(ctx, root),
                // Data classes
                DataClassPropertyRenderer(ctx, notMutableTypes)
            )
        }
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Mutable::class.java.canonicalName
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        if (roundEnv.processingOver()) {
            return true
        }

        val types = roundEnv
            // First we get all top level type with the Mutable annotation
            .findAllTypesWithAnnotation(Mutable::class)
            // Then we add all enclosed type with the Mutable annotation recursively
            .plusAllEnclosedTypes { it.hasAnnotation(Mutable::class) }
            // Then we add all types that are references by the types we have already found
            .plusReferencedTypesRecursive()
            // We also add all supertypes of tall the types we have already found
            .plusAllSuperTypes()
            // We apply the default black list
            .defaultBlacklist()
            // And we make the types distinct
            .distinct()

        logNote("all types (with recursively referenced, with supertypes): $types")

        // Get all types that are not mutable
        val notMutableTypes: List<TypeElement> = types.filter { it.hasAnnotation(NotMutable::class) }

        // Get all types that are mutable
        val mutableTypes: List<TypeElement> = types.minus(notMutableTypes)

        // create the renderers
        val renderers: PropertyRenderers = createRenderers(notMutableTypes)

        // Create the holistic model and render mutator code files
        model(mutableTypes).forEach {
            buildMutatorFileFor(it, renderers)
        }

        return true
    }

    private fun buildMutatorFileFor(element: MType, renderers: PropertyRenderers) {

        if (element.type.hasAnnotation(NotMutable::class)) {
            logNote("Ignoring type ${element.className.simpleNames} in ${element.packageName} with @NotMutable\r\n")
            return
        }

        logNote("Found type ${element.className.simpleNames} in ${element.packageName}\r\n")

        val printer = KotlinPrinter(
            element.packageName,
            listOf(
                "@file:Suppress(\"UNUSED_ANONYMOUS_PARAMETER\")"
            ),
            listOf(
                "de.peekandpoke.ultra.mutator.*"
            )
        )

        DataClassRenderer(ctx, element, renderers).render(printer)

        val dir = File("$generatedDir/${element.packageName.replace('.', '/')}").also { it.mkdirs() }
        val file = File(dir, "${element.joinSimpleNames("$$")}${"$$"}mutator.kt")

        file.writeText(printer.toString())
    }
}
