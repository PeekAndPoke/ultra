package de.peekandpoke.ultra.mutator.meta

import com.google.auto.service.AutoService
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.KotlinProcessor
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.model
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.rendering.*
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@Suppress("unused")
@AutoService(Processor::class)
open class MutatorAnnotationProcessor : KotlinProcessor("[Mutator]") {

    private val renderers by lazy {

        PropertyRenderers(ctx, PureGetterSetterRenderer(ctx)) { root ->
            listOf(
                // Lists and Sets
                ListAndSetPropertyRenderer(ctx, root),
                // Maps
                MapPropertyRenderer(ctx, root),
                // Data classes
                DataClassPropertyRenderer(ctx)
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
            .findAllTypeWithAnnotation(Mutable::class)
            .plusReferencedTypesRecursive()
            .defaultBlacklist()

        logNote("all types (with recursively referenced): $types")

        model(types).forEach {
            buildMutatorFileFor(it)
        }

        return true
    }

    private fun buildMutatorFileFor(element: MType) {

        logNote("Found type ${element.className.simpleNames} in ${element.packageName}")

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
