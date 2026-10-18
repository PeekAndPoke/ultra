package io.peekandpoke.karango.ksp

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
import io.peekandpoke.ultra.common.slumber.Slumber
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.Vault
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

        val blackListedClasses = listOf(
            New::class.qualifiedName!!,
            Ref::class.qualifiedName!!,
            Stored::class.qualifiedName!!,
            Storable::class.qualifiedName!!,
        )

        val slumberAsName = Slumber.As::class.qualifiedName!!
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

        logger.info("KARANGO ... Found ${withAnnotations.count()} types with @${Vault::class.simpleName}")

        // Find all referenced classes
        val allTypes = withAnnotations
            .combineWithReferencedTypes()
            .sortedBy { it.qualifiedName?.asString() }

        val (pool, blacklisted) = allTypes.partition { !it.isBlackListed() }

        blacklisted.forEach { cls ->
            logger.info("Blacklisted type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
        }

        pool.forEach { cls ->
            logger.info("Generating code for type: ${cls.classKind} $cls : ${cls.qualifiedName?.asString()}")
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
        
                import io.peekandpoke.karango.aql.AqlExpression
                import io.peekandpoke.karango.aql.AqlIterableExpr
                import io.peekandpoke.karango.aql.AqlPropertyPath
                import io.peekandpoke.ultra.vault.lang.L1
                import io.peekandpoke.ultra.vault.lang.L2
                import io.peekandpoke.ultra.vault.lang.L3
                import io.peekandpoke.ultra.vault.lang.L4
                import io.peekandpoke.ultra.vault.lang.L5
        
            """.trimIndent()
        )

        codeBlocks.add("//// generic property")
        codeBlocks.add(
            """
                inline fun <reified T> AqlIterableExpr<$subjectName>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

            """.trimIndent()
        )

        // A type carrying @Slumber.As does not store its own Kotlin properties -- its codec writes the
        // declared shape instead. So the declared shape REPLACES them as the source of paths: accessors
        // for the type's own properties name keys that do not exist in the database.
        val declaredShape = cls.getSlumberAsShape()

        declaredShape?.let {
            val name = it.qualifiedName?.asString()
            logger.info("  --> @Slumber.As declares the wire shape as $name; generating paths for that instead")
            codeBlocks.add("//// wire shape declared by @Slumber.As: $name")
        }

        val allFields = when {
            declaredShape == null -> cls.getDeclaredProperties()
            // A bare scalar has no sub-paths at all -- generating any would be a path into a number.
            //
            // NOT covered by a test, and deliberately kept anyway: removing this line changes no output,
            // because KSP reports no declared properties for `kotlin.*` builtins -- not even
            // `String.length`. Both mutants survived. It encodes the intent rather than depending on
            // that, since it is KSP-internal behaviour we do not control. `SlumberAsCodeGenSpec` pins
            // the OUTCOME (no sub-paths for MpLocalTime/MpTimezone), which is the real tripwire.
            declaredShape.isPrimitiveOrString() -> emptySequence()
            // An enum, interface, object or generic shape would otherwise be walked like a data class:
            // `@Slumber.As(SomeEnum::class)` yields SomeEnum's ctor properties as query paths, i.e. a
            // sub-path into what is a bare string on the wire. That is exactly the defect this feature
            // removes, reintroduced through it, so it fails the build instead.
            declaredShape.isBlackListed() -> {
                logger.error(
                    "@Slumber.As on ${cls.qualifiedName?.asString()} declares " +
                            "${declaredShape.qualifiedName?.asString()}, which cannot describe a wire " +
                            "shape. Use a data class, or a primitive/String for a bare scalar.",
                    cls,
                )
                emptySequence()
            }

            else -> declaredShape.getDeclaredProperties()
        }

        val ctorFields = allFields.filter { it.isPrimaryCtorParameter() }

        ctorFields.forEach { field ->
            logger.info("  --> Found ctor field ${field.simpleName.asString()} with annotations ${field.annotations.print()}")
        }

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
            is FileLocation -> "Line ${l.lineNumber}"
            is NonExistLocation -> "Unknown"
        }

        codeBlocks.add("// $prop ".padEnd(160, '/'))
        codeBlocks.add("// annotations: ${annotations.print()}")
        codeBlocks.add("// defined as:   $definedAs")
        codeBlocks.add("// defined by:   $definedBy")
        codeBlocks.add("// defined at:   $definedAt")
        codeBlocks.add("// defined type: $definedType")
        codeBlocks.add("// cleaned type: $type")
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

        return when (fqn) {
            null -> "kotlin.Any?"
            // References are treated as just strings
            Ref::class.qualifiedName -> "kotlin.String"

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

    /**
     * The wire shape declared by `@Slumber.As`, or null when the type does not declare one.
     *
     * Readable here because the annotation is `@Retention(RUNTIME)` and the type is resolved from the
     * property that references it — so this works for types on the CLASSPATH, such as `MpInstant`, and
     * not only for the sources being compiled. `getSymbolsWithAnnotation` would not; it sees the current
     * round's sources only.
     */
    private fun KSClassDeclaration.getSlumberAsShape(): KSClassDeclaration? {

        val annotation = annotations.firstOrNull { ann ->
            ann.annotationType.resolve().declaration.qualifiedName?.asString() == slumberAsName
        } ?: return null

        // Named lookup first, then positional. KSP does back-fill the name for a positional argument in
        // source -- measured, and `SlumberAsCodeGenSpec` covers that case -- but falling back costs
        // nothing and this must not degrade silently.
        val shape = (annotation.arguments.firstOrNull { it.name?.asString() == "shape" }
            ?: annotation.arguments.firstOrNull())
            ?.value as? KSType

        val declaration = shape?.declaration as? KSClassDeclaration

        if (declaration == null) {
            // Present but unreadable is NOT the same as absent. Falling through to the type's own Kotlin
            // properties would generate paths naming keys that are not on the wire -- silently, which is
            // the failure this whole feature exists to remove.
            logger.error("@Slumber.As on ${qualifiedName?.asString()} is present but unreadable.", this)
        }

        return declaration
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
