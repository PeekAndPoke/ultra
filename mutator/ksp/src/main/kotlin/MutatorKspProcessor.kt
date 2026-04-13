package io.peekandpoke.mutator.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import io.peekandpoke.mutator.Mutable
import io.peekandpoke.mutator.NotMutable
import io.peekandpoke.mutator.ksp.builtin.BuiltInMutableObjectsPlugin
import io.peekandpoke.mutator.ksp.builtin.BuiltInPlatformTypesVetoingPlugin
import java.util.*
import kotlin.reflect.KClass

class MutatorKspProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    val plugins by lazy { loadPlugins() }

    val logger: KSPLogger get() = environment.logger

    private val codeGenerator: CodeGenerator get() = environment.codeGenerator

    @Suppress("unused")
    private val options: Map<String, String> get() = environment.options

    init {
        plugins.plugins.forEachIndexed { index, plugin ->
            logger.info("Plugin ${index + 1}: ${plugin.name} | ${plugin::class.qualifiedName}")
        }
    }

    private fun loadPlugins(): MutatorKspPlugins {
        val loaded = try {
            ServiceLoader.load(MutatorKspPlugin::class.java).toList()
        } catch (e: Exception) {
            logger.warn("ServiceLoader failed: ${e.message}")
            emptyList()
        }

        val builtIn = listOf(
            BuiltInPlatformTypesVetoingPlugin(),
            BuiltInMutableObjectsPlugin(),
        )

        return MutatorKspPlugins(
            processor = this,
            plugins = loaded.plus(builtIn),
        )
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {

        // Phase A: Collect all @Mutable annotated classes
        val annotatedClasses = resolver
            .getSymbolsWithAnnotation(Mutable::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        // Phase B: Split into deep (transitive) and shallow (this class only)
        val (deepClasses, shallowClasses) = annotatedClasses.partition { it.getMutableDeep() }

        // Phase C: Transitively expand only deep classes
        val expandedFromDeep = deepClasses.combineWithReferencedTypes()

        // Phase D: Combine all annotated + transitive from deep, then filter
        val allTypes = (shallowClasses + expandedFromDeep)
            .toSet()
            .filterNot { it.hasAnnotation(NotMutable::class) }
            .filterNot { plugins.isVetoed(it) }
            .sortedBy { it.qualifiedName?.asString() }

        // Phase E: Populate the processing set before code generation
        val processingSet = allTypes.toSet()
        plugins.setProcessingSet(processingSet)

        // Phase F: Generate code
        processingSet.forEach { cls ->
            logger.info("Generating code for type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
            generateCode(cls)
        }

        return listOf()
    }

    private fun KSAnnotated.hasAnnotation(cls: KClass<*>): Boolean {
        return annotations.any { ann ->
            val type = ann.annotationType.resolve().declaration as? KSClassDeclaration
            type?.qualifiedName?.asString() == cls.qualifiedName
        }
    }

    private fun generateCode(cls: KSClassDeclaration) {
        val packageName = cls.packageName.asString()

        val ctx =
            MutatorKspPlugin.MutatorGeneratorContext(codeBlocks = MutatorCodeBlocks(), cls = cls, plugins = plugins)

        val handler = plugins.findMutatorGenerator(cls)
        handler?.generateMutatorFor(processor = this, ctx = ctx)

        ctx.codeBlocks.takeIf { it.isNotEmpty() }?.let { codeBlocks ->
            codeBlocks.prepend(
                """
                    @file:Suppress("NOTHING_TO_INLINE")

                    package $packageName

                    import io.peekandpoke.mutator.ListMutator
                    import io.peekandpoke.mutator.MapMutator
                    import io.peekandpoke.mutator.Mutator
                    import io.peekandpoke.mutator.MutatorDsl
                    import io.peekandpoke.mutator.ObjectMutator
                    import io.peekandpoke.mutator.SetMutator
                    import io.peekandpoke.mutator.mutator
                    import io.peekandpoke.mutator.onChange

                """.trimIndent()
            )

            val simpleNames = cls.getSimpleNames()
            val subjectName = simpleNames.joinToString(".") { it.asString() }

            val file = codeGenerator.createNewFile(
                dependencies = Dependencies(
                    aggregating = false,
                    sources = listOfNotNull(cls.containingFile).toTypedArray(),
                ),
                packageName = packageName,
                fileName = subjectName + "${"$$"}mutator",
                extensionName = "kt",
            )

            file.write(codeBlocks.build().toByteArray())
        }
    }

    private fun KSClassDeclaration.getMutableDeep(): Boolean {
        val ann = annotations.firstOrNull { a ->
            val type = a.annotationType.resolve().declaration as? KSClassDeclaration
            type?.qualifiedName?.asString() == Mutable::class.qualifiedName
        } ?: return true

        return ann.arguments.firstOrNull { it.name?.asString() == "deep" }?.value as? Boolean ?: true
    }

    private fun List<KSClassDeclaration>.combineWithReferencedTypes(): Set<KSClassDeclaration> {
        val referenced = this
            .flatMap { it.asStarProjectedType().getReferencedTypesRecursive() }
            .mapNotNull { it.declaration as? KSClassDeclaration }

        return this.plus(referenced).toSet()
    }

    private fun KSType.getReferencedTypesRecursive(): Set<KSType> {

        var visited: Set<KSType> = setOf()
        var nextRound: Set<KSType> = setOf(this)

        while (nextRound.isNotEmpty()) {
            val found = nextRound.flatMap { it.getReferencedTypes() }.toSet().minus(visited)
            // Visit all that we did not yet visit
            nextRound = found.minus(visited)
            // Remember all the ones that were visited
            visited = visited.plus(nextRound)
        }

        return visited
    }

    private fun KSType.getReferencedTypes(): Set<KSType> {
        val cls = (declaration as? KSClassDeclaration)
            ?.takeIf { !it.isPrimitiveOrString() }
            ?: return emptySet()

        logger.info("- Finding referenced types of ${cls.qualifiedName?.asString()}")
        logger.info("  --> Kind: ${cls.classKind} ")
        logger.info("  --> Modifier: ${cls.modifiers.joinToString(", ") { it.name }}")

        val propertyTypes = cls.getAllProperties()
            .map { property -> property to property.type.resolve() }
            .toList()

        logger.info("  --> Properties:")

        propertyTypes.forEach { (property, type) ->
            logger.info("    --> ${property.simpleName.asString()}: $type")
        }

        val genericTypes: List<Pair<KSTypeArgument, KSType>> = when (arguments.isEmpty()) {
            true -> emptyList()
            else -> {
                logger.info("  --> Type Parameters:")

                val args = arguments.mapNotNull { arg -> arg.type?.resolve()?.let { arg to it } }

                args.forEach { (arg, type) ->
                    logger.info("    --> $arg: $type")
                }

                args
            }
        }

        val sealedSubclasses = cls.getSealedSubclasses()

        return emptyList<KSType>()
            .plus(propertyTypes.map { (_, type) -> type })
            .plus(genericTypes.map { (_, type) -> type })
            .plus(sealedSubclasses.map { it.asStarProjectedType() })
            .toSet()
    }
}
