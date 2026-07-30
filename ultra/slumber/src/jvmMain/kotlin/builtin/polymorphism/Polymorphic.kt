package io.peekandpoke.ultra.slumber.builtin.polymorphism

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.slumber.AdditionalSerialName
import io.peekandpoke.ultra.slumber.Polymorphic
import io.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicChildUtil.getAdditionalIdentifiers
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicChildUtil.getIdentifier
import kotlinx.serialization.SerialName
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

/**
 * Reflection helpers for the CHILD side of a polymorphic hierarchy: recognition and type identifiers.
 *
 * Mirrored by `ultra/codegen`'s TypeWalker, so any change here changes the generated TypeScript too.
 */
object PolymorphicChildUtil {

    /**
     * Builds the slumberer for a concrete child [type]: a [DataClassSlumberer] plus the
     * discriminator/identifier pair the owning parent expects.
     *
     * The discriminator NAME is resolved from the child's polymorphic parent (see
     * [PolymorphicParentUtil.getParent]), so a child always writes the field its parent reads.
     */
    fun createChildSlumberer(type: KType, attributes: TypedAttributes): PolymorphicChildSlumberer {

        val cls = type.classifier as KClass<*>

        val parent = PolymorphicParentUtil.getParent(cls) ?: cls

        val discriminator = PolymorphicParentUtil.getDiscriminator(parent)

        val identifier = getIdentifier(cls)

        return PolymorphicChildSlumberer(
            discriminator = discriminator,
            identifier = identifier,
            childSlumberer = DataClassSlumberer(type, attributes)
        )
    }

    /**
     * Checks if the given [cls] is a polymorphic child.
     *
     * A class is recognized as a polymorphic child when ANY of these holds:
     * 1. its companion object implements [Polymorphic.Child]
     * 2. any of its supertypes is sealed
     * 3. it carries a [SerialName] annotation
     * 4. one of its supertypes has a companion object implementing [Polymorphic.Parent]
     */
    // TODO(scan): condition 3 fires for a standalone class that merely carries @SerialName for
    //  kotlinx compatibility — slumbering it then injects a `_type` key it never had, and awaking it
    //  (DataClassAwaker) ignores that key. Asymmetric and surprising; see the codegen mirror at
    //  ultra/codegen/src/main/kotlin/model/TypeWalker.kt:489-503, which reproduces the same rule.
    fun isPolymorphicChild(cls: KClass<*>): Boolean =
        cls.companionObjectInstance is Polymorphic.Child ||
                cls.allSupertypes.any { (it.classifier as? KClass<*>)?.isSealed ?: false } ||
                cls.annotations.filterIsInstance<SerialName>().isNotEmpty() ||
                PolymorphicParentUtil.getParent(cls) != null

    /**
     * Gets all serial identifiers of the class, each paired with [cls] itself.
     *
     * Combines the results of [getIdentifier] and [getAdditionalIdentifiers]. The FIRST entry is
     * always the primary identifier — the one that gets written when slumbering; the rest are
     * read-only aliases.
     */
    fun getAllIdentifiers(cls: KClass<*>): List<Pair<String, KClass<*>>> {
        return listOf(
            getIdentifier(cls) to cls
        ).plus(
            getAdditionalIdentifiers(cls).map { it to cls }
        )
    }

    /**
     * Get the primary type identifier of a child class — the value written on slumbering.
     *
     * First we try to get the identifier from [Polymorphic.Child.identifier].
     * Then we look for a [SerialName] annotation.
     * Otherwise, we use the qualified name of the class — which means renaming or moving the class
     * invalidates already-persisted data unless an [AdditionalSerialName] alias is added.
     */
    fun getIdentifier(cls: KClass<*>) = when (val companion = cls.companionObjectInstance) {

        is Polymorphic.Child -> companion.identifier

        else -> {

            val annotation = cls.annotations.filterIsInstance<SerialName>().firstOrNull()

            when {
                annotation != null -> annotation.value
                else -> cls.qualifiedName ?: cls.jvmName
            }
        }
    }

    /**
     * Gets the read-only alias identifiers of a class from its [AdditionalSerialName] annotations.
     *
     * Class-level annotations only; these are accepted on awaking and never written on slumbering,
     * which is what makes them usable for migrating away from obsolete serial names.
     */
    private fun getAdditionalIdentifiers(cls: KClass<*>): List<String> {
        return cls.annotations.filterIsInstance<AdditionalSerialName>()
            .map { it.value }
    }
}

/**
 * Reflection helpers for the PARENT side of a polymorphic hierarchy: discriminator, child set and
 * the awaker/slumberer built from them.
 *
 * Also consumed outside slumber — `ultra/vault`'s `Repository.getAllStoredClasses` and
 * `ultra/codegen`'s TypeWalker both build on [getChildren] / [getDiscriminator].
 */
object PolymorphicParentUtil {

    /**
     * Creates a polymorphic awaker for the given [cls].
     *
     * The identifier map is a fixed allow-list derived from [getChildren] — incoming data selects a
     * class from it and can never name one outside it.
     */
    // TODO(scan): the discriminator NAME is read from `cls` alone, while createParentSlumberer reads
    //  it from `getParent(cls) ?: cls`. For an intermediate sealed level with no Parent companion
    //  under a root that declares a custom discriminator, slumbering writes the root's field name and
    //  awaking looks for "_type" — the round trip breaks. Same for getDefaultType.
    fun createParentAwaker(cls: KClass<*>): PolymorphicAwaker {

        val discriminator: String = getDiscriminator(cls)

        val map: Map<String, KClass<*>> = getIdentifiersToChildClasses(cls)

        val default: KClass<*>? = getDefaultType(cls)

        return PolymorphicAwaker(discriminator, map, default)
    }

    /**
     * Maps every identifier (primary and alias) of every child of [cls] to the class it awakes to.
     */
    // TODO(scan): `.toMap()` makes duplicate identifiers silently last-wins — two children sharing a
    //  @SerialName, or one child's @AdditionalSerialName shadowing another child's primary name,
    //  compiles and runs while one of the two becomes unreachable on awaking.
    fun getIdentifiersToChildClasses(cls: KClass<*>): Map<String, KClass<*>> {
        return getChildren(cls)
            .flatMap { child -> PolymorphicChildUtil.getAllIdentifiers(child) }
            .toMap()
    }

    /**
     * Creates a polymorphic slumberer for the given [cls].
     *
     * Reached only when the DECLARED slumber target is the parent type; a parent-typed data-class
     * field or collection element dispatches on the runtime class and lands on
     * [PolymorphicChildUtil.createChildSlumberer] instead.
     */
    // TODO(scan): the child map is keyed by runtime class and never contains `cls` itself, so
    //  slumbering an instance of an INSTANTIABLE parent (a non-sealed Polymorphic.Parent) recurses
    //  through PolymorphicParentSlumberer forever -> StackOverflowError.
    fun createParentSlumberer(cls: KClass<*>): PolymorphicParentSlumberer {

        val parent: KClass<out Any> = getParent(cls) ?: cls

        val discriminator: String = getDiscriminator(parent)

        val map: Map<KClass<*>, String> = getChildren(parent).associateWith { getIdentifier(it) }

        return PolymorphicParentSlumberer(discriminator, map)
    }

    /**
     * Checks if the given [cls] is a polymorphic parent.
     *
     * A class is recognized as a polymorphic parent when it either:
     *    1. is a sealed class
     * or 2. has a companion object of type [Polymorphic.Parent]
     */
    fun isPolymorphicParent(cls: KClass<*>): Boolean =
        cls.isSealed || cls.companionObjectInstance is Polymorphic.Parent

    /**
     * Gets the name of the discriminator field.
     *
     * Returns [Polymorphic.Parent.discriminator] from the companion object of [cls] itself — it does
     * NOT walk up the hierarchy. Pass [getParent] first when the inherited name is wanted.
     * Falls back to [Polymorphic.defaultDiscriminator].
     */
    fun getDiscriminator(cls: KClass<*>?): String =
        (cls?.companionObjectInstance as? Polymorphic.Parent)?.discriminator
            ?: Polymorphic.defaultDiscriminator

    /**
     * Gets the default type used for awaking polymorphic children.
     *
     * Looks at the companion object of [cls] itself (not its supertypes) for [Polymorphic.Parent] and
     * returns its [Polymorphic.Parent.defaultType]. Otherwise returns null.
     *
     * A null default means [PolymorphicAwaker] yields null when
     * - the discriminator field is not present in the data
     * - or when it contains an unknown type identifier
     *
     * For a non-nullable target type the surrounding `NonNullAwaker` turns that null into an
     * `AwakerException`; for a nullable one the value silently becomes null.
     */
    fun getDefaultType(cls: KClass<*>?): KClass<*>? = when (val companion = cls?.companionObjectInstance) {

        is Polymorphic.Parent -> companion.defaultType

        else -> null
    }

    /**
     * Gets all CONCRETE child types of a parent type, transitively.
     *
     * Seeds from [Polymorphic.Parent.childTypes] plus [KClass.sealedSubclasses], then recurses into
     * every seed. Abstract and sealed intermediates are dropped from the result (they can never be a
     * runtime type), as is anything that is not actually a subclass of [cls].
     */
    // TODO(scan): the result is not distinct — a childTypes set that lists both an intermediate and
    //  its leaves (what indexedSubClasses() returns) yields each leaf twice. Map-building callers
    //  dedupe by accident; codegen's declareUnion does not and emits duplicate union variants.
    // TODO(scan): no cycle/self guard — childTypes containing the parent itself, or two parents
    //  listing each other, recurses until StackOverflowError.
    fun <T : Any> getChildren(cls: KClass<T>?): List<KClass<out T>> {

        if (cls == null) {
            return emptyList()
        }

        val annotated = when (val companion = cls.companionObjectInstance) {
            is Polymorphic.Parent -> companion.childTypes
            else -> emptySet()
        }

        return annotated
            .plus(cls.sealedSubclasses)
            .flatMap { getChildren(it).plus(it) }
            .filter { !it.isAbstract }
            .filter { !it.isSealed }
            .filter { it.isSubclassOf(cls) }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as KClass<out T>
            }
    }

    /**
     * Gets the nearest declared [Polymorphic.Parent] of the given class, or null.
     *
     * If [cls]'s own companion object implements [Polymorphic.Parent] it is returned as is.
     * Otherwise the first supertype (superclasses AND superinterfaces) whose companion object does
     * is returned. A pure sealed hierarchy without any Parent companion therefore yields null.
     */
    // TODO(scan): "first" is whatever order `allSuperclasses` happens to produce; a class reachable
    //  from two Polymorphic.Parent declarations (e.g. two sealed interfaces) picks one arbitrarily,
    //  which decides the discriminator field name.
    fun getParent(cls: KClass<*>): KClass<*>? = when (cls.companionObjectInstance) {

        is Polymorphic.Parent -> cls

        else -> cls.allSuperclasses.firstOrNull { it.companionObjectInstance is Polymorphic.Parent }
    }
}
