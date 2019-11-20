package de.peekandpoke.ultra.mutator.meta

import com.google.auto.service.AutoService
import de.peekandpoke.ultra.meta.KotlinProcessor
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.Model
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

        PropertyRenderers(ctx) { root ->
            listOf(
                // Lists and Sets
                ListAndSetPropertyRenderer(ctx, root),
                // Maps
                MapPropertyRenderer(ctx, root),
                // Data classes
                DataClassPropertyRenderer(ctx),
                // Fallback for primitive types, String, Any, and all others that or not support yet
                PureGetterSetterRenderer(ctx)
            )
        }
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Mutable::class.java.canonicalName
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val types = roundEnv
            .findAllTypeWithAnnotation(Mutable::class)
            .plusReferencedTypesRecursive()
            .defaultBlacklist()

        logNote("all types (with recursively referenced): $types")

        val model = Model.Builder(ctx).build(types)

        model.types.forEach {
            buildMutatorFileFor(it)
        }

        return true
    }

    private fun buildMutatorFileFor(element: MType) {

        logNote("Found type ${element.simpleName} in ${element.packageName}")

        val content = DataClassRenderer(ctx, element, renderers).render()

        val dir = File("$generatedDir/${element.packageName.replace('.', '/')}").also { it.mkdirs() }
        val file = File(dir, "${element.nestedFileName}${"$$"}mutator.kt")

        file.writeText(content)
    }
}
