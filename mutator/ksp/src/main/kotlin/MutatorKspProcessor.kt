package de.peekandpoke.ultra.ksp

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Modifier
import de.peekandpoke.mutator.Mutable
import de.peekandpoke.mutator.NotMutable
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

    private val codeGenerator: CodeGenerator get() = environment.codeGenerator

    private val logger: KSPLogger get() = environment.logger

    @Suppress("unused")
    private val options: Map<String, String> get() = environment.options

    override fun process(resolver: Resolver): List<KSAnnotated> {

        // Find all types that have a Karango Annotation
        val withAnnotations = resolver
            .getSymbolsWithAnnotation(Mutable::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        // Find all referenced classes
        val allTypes = withAnnotations
            .combineWithReferencedTypes()
            .filterNot { it.hasAnnotation(NotMutable::class) }
            .filter { it.isData() }
            .filter { it.qualifiedName != null }
            .sortedBy { it.qualifiedName!!.asString() }

        val (pool, blacklisted) = allTypes.partition { !it.isBlackListed() }

        blacklisted.toSet().forEach { cls ->
            logger.warn("Blacklisted type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
        }

        pool.toSet().let { pool ->
            pool.forEach { cls ->
                logger.warn("Generating code for type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
                generateCode(cls, pool)
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


    private fun generateCode(cls: KSClassDeclaration, poolOfMutables: Set<KSClassDeclaration>) {
        val simpleNames = cls.getSimpleNames()
        val subjectName = simpleNames.joinToString(".") { it.asString() }
        val packageName = cls.packageName.asString()

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(
                aggregating = false,
                sources = listOfNotNull(cls.containingFile).toTypedArray(),
            ),
            packageName = packageName,
            fileName = subjectName + "${"$$"}mutator",
            extensionName = "kt",
        )

        val codeBlocks = MutatorCodeBlocks()

        codeBlocks.add(
            """
                @file:Suppress("RemoveRedundantBackticks")

                package $packageName
        
                import de.peekandpoke.mutator.*
        
            """.trimIndent()
        )

        when {
            cls.isData() -> {
                codeBlocks.add("// Mutator for data class $subjectName")

                val allFields = cls.getDeclaredProperties()

                val ctorFields = allFields.filter { it.isPrimaryCtorParameter() }

                // TODO: how to handle nested classes ?
                val clsName = codeBlocks.getClassName(cls)

                codeBlocks.add(
                    """
                        @MutatorDsl
                        fun $clsName.mutator(): DataClassMutator<$clsName> = DataClassMutator(this)
                        
                        @MutatorDsl
                        fun $clsName.mutate(mutation: Mutation<$clsName>): $clsName = mutator().apply(mutation).get()
                        
                    """.trimIndent()
                )

                ctorFields.forEach { field ->
                    // Is this field if a Mutable type?
                    if (poolOfMutables.contains(field.type.resolve().declaration)) {
                        codeBlocks.addMutatorField(cls, field)
                    } else {
                        codeBlocks.addGetterSetterField(cls, field)
                    }
                }
            }
        }




        if (codeBlocks.isNotEmpty()) {
            file.write(codeBlocks.build().toByteArray())
        }
    }

    private fun KSPropertyDeclaration.isPrimaryCtorParameter(): Boolean {

        // Get the containing class
        val classDeclaration = parentDeclaration as? KSClassDeclaration ?: return false

        // Get the primary constructor
        val primaryConstructor = classDeclaration.primaryConstructor ?: return false

        // Check if this property name matches any of the constructor parameters
        return primaryConstructor.parameters.any { param ->
            param.name?.asString() == this.simpleName.asString()
        }
    }

    private fun Sequence<KSAnnotation>.print(): String {
        return toList().print()
    }

    private fun List<KSAnnotation>.print(): String {
        return joinToString(", ") { ann -> ann.annotationType.resolve().declaration.qualifiedName?.asString() ?: "n/a" }
    }

    private fun KSPropertyDeclaration.hasAnyAnnotation(vararg cls: KClass<*>): Boolean {
        return cls.any { hasAnnotation(it) }
    }

    private fun KSPropertyDeclaration.hasAnnotation(cls: KClass<*>): Boolean {
        return annotations.any { ann ->
            val type = ann.annotationType.resolve().declaration as? KSClassDeclaration

            type?.qualifiedName?.asString() == cls.qualifiedName
        }
    }

    private fun KSClassDeclaration.isData(): Boolean {
        return modifiers.contains(Modifier.DATA)
    }

    private fun KSClassDeclaration.isSealed(): Boolean {
        return modifiers.contains(Modifier.SEALED)
    }

    private fun KSClassDeclaration.getSimpleNames(): List<KSName> {
        val names = mutableListOf<KSName>()
        var current: KSClassDeclaration? = this

        while (current != null) {
            names.add(0, current.simpleName)
            current = current.parentDeclaration as? KSClassDeclaration
        }

        return names.toList()
    }

    private fun KSClassDeclaration.isPrimitiveOrString(): Boolean {
        val qualifiedName = this.qualifiedName?.asString() ?: return false

        return when (qualifiedName) {
            Byte::class.qualifiedName,
            Short::class.qualifiedName,
            Int::class.qualifiedName,
            Long::class.qualifiedName,
            Float::class.qualifiedName,
            Double::class.qualifiedName,
            Boolean::class.qualifiedName,
            Char::class.qualifiedName,
            String::class.qualifiedName,
                -> true

            else -> false
        }
    }

    private fun KSClassDeclaration.isBlackListed(): Boolean {

        val predicates = listOf<(KSClassDeclaration) -> Boolean>(
            { cls -> cls.qualifiedName == null },
            { cls -> cls.qualifiedName?.asString() in blackListedClasses },
            { cls -> !cls.isData() && !cls.isAbstract() && !cls.isSealed() },
            { cls -> cls.typeParameters.isNotEmpty() },
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
