package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import de.peekandpoke.mutator.Mutable
import de.peekandpoke.mutator.NotMutable
import de.peekandpoke.mutator.ksp.builtin.MutatorKspDataClassPlugin
import java.util.*
import kotlin.reflect.KClass

class MutatorKspProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    companion object {
        val blackListedPackages = listOf(
            "java.",
            "javax.",
            "kotlin.",
            "kotlinx.",
        )

        val blackListedClasses = listOf<String>(
        )
    }

    private val plugins by lazy { loadPlugins() }

    private val codeGenerator: CodeGenerator get() = environment.codeGenerator

    private val logger: KSPLogger get() = environment.logger

    @Suppress("unused")
    private val options: Map<String, String> get() = environment.options

    init {
        plugins.plugins.forEachIndexed { index, plugin ->
            logger.warn("Plugin ${index + 1}: ${plugin::class.qualifiedName} - ${plugin.name}")
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
            MutatorKspDataClassPlugin(),
        )

        return MutatorKspPlugins(plugins = loaded.plus(builtIn))
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {

        // Find all types that have a Karango Annotation
        val withAnnotations = resolver
            .getSymbolsWithAnnotation(Mutable::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        // Find all referenced classes
        val allTypes = withAnnotations
            .combineWithReferencedTypes()
            .filterNot { it.hasAnnotation(NotMutable::class) }
            .filter { it.qualifiedName != null }
            .sortedBy { it.qualifiedName!!.asString() }

        val (pool, blacklisted) = allTypes.partition { !it.isBlackListed() }

        blacklisted.toSet().forEach { cls ->
            logger.warn("Blacklisted type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
        }

        pool.toSet().let { pool ->
            pool.forEach { cls ->
                logger.warn("Generating code for type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
                generateCode(cls)
            }
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
        handler?.generateMutatorFor(ctx)

        ctx.codeBlocks.takeIf { it.isNotEmpty() }?.let { codeBlocks ->
            codeBlocks.prepend(
                """
                    @file:Suppress("RemoveRedundantBackticks", "unused", "NOTHING_TO_INLINE")
    
                    package $packageName
            
                    import de.peekandpoke.mutator.*
            
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

    private fun KSClassDeclaration.isBlackListed(): Boolean {

        val predicates = listOf<(KSClassDeclaration) -> Boolean>(
            { cls -> cls.qualifiedName == null },
            { cls -> cls.qualifiedName?.asString() in blackListedClasses },
            { cls -> !cls.isData() && !cls.isAbstract() && !cls.isSealed() },
            { cls -> cls.classKind == ClassKind.ENUM_CLASS },
            { cls -> cls.isCompanionObject },
            { cls -> cls.isPrimitiveOrString() },
            { cls ->
                val fqn = cls.qualifiedName?.asString()

                blackListedPackages.any { pkg -> fqn?.startsWith(pkg) == true }
            },
        )

        return predicates.any { it(this) }
    }

    private fun Sequence<KSClassDeclaration>.combineWithReferencedTypes(): Set<KSClassDeclaration> {
        val list = toList()

        val referenced = list
            .flatMap { it.asStarProjectedType().getReferencedTypesRecursive() }
            .mapNotNull { it.declaration as? KSClassDeclaration }

        return list.plus(referenced).toSet()
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
