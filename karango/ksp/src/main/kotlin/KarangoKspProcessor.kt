package de.peekandpoke.karango.ksp

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.NonExistLocation
import de.peekandpoke.ultra.slumber.Slumber
import de.peekandpoke.ultra.vault.LazyRef
import de.peekandpoke.ultra.vault.New
import de.peekandpoke.ultra.vault.Ref
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.Vault
import kotlin.reflect.KClass

class KarangoKspProcessor(
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
            New::class.qualifiedName!!,
            Ref::class.qualifiedName!!,
            LazyRef::class.qualifiedName!!,
            Stored::class.qualifiedName!!,
            Storable::class.qualifiedName!!,
        )
    }

    private val codeGenerator: CodeGenerator get() = environment.codeGenerator

    private val logger: KSPLogger get() = environment.logger

    @Suppress("unused")
    private val options: Map<String, String> get() = environment.options

    override fun process(resolver: Resolver): List<KSAnnotated> {

        // Find all types that have a Karango Annotation
        val withAnnotations = resolver
            .getSymbolsWithAnnotation(Vault::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        logger.warn("KARANGO ... Found ${withAnnotations.count()} types with @${Vault::class.simpleName}")

        // Find all referenced classes
        val allTypes = withAnnotations
            .combineWithReferencedTypes()
            .sortedBy { it.qualifiedName?.asString() }

        val (pool, blacklisted) = allTypes.partition { !it.isBlackListed() }

        blacklisted.forEach { cls ->
            logger.warn("Blacklisted type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
        }

        pool.forEach { cls ->
            logger.warn("Generating code for type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
            generateCode(cls)
        }

        return listOf()
    }

    private fun generateCode(cls: KSClassDeclaration) {
        val simpleNames = cls.getSimpleNames()
        val subjectName = simpleNames.joinToString(".") { it.asString() }
        val packageName = cls.packageName.asString()

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(
                aggregating = false,
                sources = listOfNotNull(cls.containingFile).toTypedArray(),
            ),
            packageName = packageName,
            fileName = subjectName + "${"$$"}karango",
            extensionName = "kt",
        )

        val codeBlocks = mutableListOf<String>()

        codeBlocks.add(
            """
                package $packageName
        
                import de.peekandpoke.karango.*
                import de.peekandpoke.karango.aql.*
                import de.peekandpoke.ultra.vault.lang.*
        
            """.trimIndent()
        )

        codeBlocks.add("//// generic property")
        codeBlocks.add(
            """
                inline fun <reified T> AqlIterableExpr<$subjectName>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

            """.trimIndent()
        )

        val allFields = cls.getDeclaredProperties()

        val ctorFields = allFields.filter { it.isPrimaryCtorParameter() }

        // TODO:
        //  - test that ctor parameters are picked up properly
        ctorFields.forEach { field ->
            logger.info("  --> Found ctor field ${field.simpleName.asString()} with annotations ${field.annotations.print()}")
        }

        // TODO:
        //  - test that @Vault.Field and @Slumber.Field are picked up and will get code generated
        //  - test that @Vault.Ignore does not get code generated
        //  - test that non-ctor fields do not get code generated, unless annotated
        val annotatedFields = allFields
            .filterNot { it.isPrimaryCtorParameter() }
            .filter { it.hasAnyAnnotation(Vault.Field::class, Slumber.Field::class) }
            .filterNot { it.hasAnnotation(Vault.Ignore::class) }

        annotatedFields.forEach { field ->
            logger.info("  --> Found annotated field ${field.simpleName.asString()} with annotations ${field.annotations.print()}")
        }

        ctorFields.plus(annotatedFields).forEach { field ->
            renderProperty(
                codeBlocks = codeBlocks,
                subject = subjectName,
                property = field,
            )
        }

        file.write(codeBlocks.joinToString("\n").toByteArray())
    }

    fun renderProperty(
        codeBlocks: MutableList<String>,
        subject: String,
        property: KSPropertyDeclaration,
    ) {
        val replacements = listOf(
            // Map mutable kotlin collection to immutable names
            "kotlin.collections.MutableList" to List::class.qualifiedName!!,
            "kotlin.collections.MutableSet" to Set::class.qualifiedName!!,
            "kotlin.collections.MutableMap" to Map::class.qualifiedName!!,
            // Vault types
            Ref::class.qualifiedName!! to "kotlin.String",
            LazyRef::class.qualifiedName!! to "kotlin.String",
        )

        val prop = property.simpleName.asString()
        val annotations = property.annotations.toList()
        val propType = property.type.resolve()

        val definedType = propType.toFullyQualifiedString()
        val type = replacements.fold(definedType) { acc, (from, to) -> acc.replace(from, to) }

        val definedAs = when (property.isPrimaryCtorParameter()) {
            true -> "Primary Constructor Param"
            else -> "Property"
        }

        val definedBy = when (val p = property.parentDeclaration) {
            null -> "Unknown"
            is KSClassDeclaration -> "Class ${p.qualifiedName?.asString()}"
            else -> "$p (${p::class.qualifiedName})"
        }

        val definedAt = when (val l = property.location) {
            is FileLocation -> "${l.filePath}:${l.lineNumber}"
            is NonExistLocation -> "Unknown"
        }

        codeBlocks.add("// $prop ".padEnd(160, '/'))
        codeBlocks.add("// annotations: ${annotations.print()}")
        codeBlocks.add("// defined as:   $definedAs")
        codeBlocks.add("// defined by:   $definedBy")
        codeBlocks.add("// defined type: $definedType")
        codeBlocks.add("// cleaned type: $type")
        codeBlocks.add("// defined at:   $definedAt")
        codeBlocks.add(
            """

                inline val AqlIterableExpr<$subject>.$prop inline get() = AqlPropertyPath.start(this).append<$type, $type>("$prop")
                inline val AqlExpression<$subject>.$prop inline get() = AqlPropertyPath.start(this).append<$type, $type>("$prop")

                inline val AqlPropertyPath<$subject, $subject>.$prop @JvmName("${prop}_0") inline get() = append<$type, $type>("$prop")
                inline val AqlPropertyPath<$subject, L1<$subject>>.$prop @JvmName("${prop}_1") inline get() = append<$type, L1<$type>>("$prop")
                inline val AqlPropertyPath<$subject, L2<$subject>>.$prop @JvmName("${prop}_2") inline get() = append<$type, L2<$type>>("$prop")
                inline val AqlPropertyPath<$subject, L3<$subject>>.$prop @JvmName("${prop}_3") inline get() = append<$type, L3<$type>>("$prop")
                inline val AqlPropertyPath<$subject, L4<$subject>>.$prop @JvmName("${prop}_4") inline get() = append<$type, L4<$type>>("$prop")
                inline val AqlPropertyPath<$subject, L5<$subject>>.$prop @JvmName("${prop}_5") inline get() = append<$type, L5<$type>>("$prop")

            """.trimIndent()
        )
    }

    private fun KSType.toFullyQualifiedString(): String {
        val fqn = declaration.qualifiedName?.asString()

        // TODO:
        //  - test for Ref and LazyRef
        //  - tests for Type Parameters
        return when (fqn) {
            null -> "kotlin.Any?"
            // References are treated as just strings
            Ref::class.qualifiedName -> "kotlin.String"
            // Lazy References are treated as just strings
            LazyRef::class.qualifiedName -> "kotlin.String"

            // otherwise we take the original type
            else -> when (arguments.isEmpty()) {
                true -> fqn
                else -> {
                    val mapped = arguments.map { it.type?.resolve()?.toFullyQualifiedString() ?: "*" }
                    val joined = mapped.joinToString(", ") { it }

                    "$fqn<$joined>"
                }
            }
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
