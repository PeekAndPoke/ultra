package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.slumber.Polymorphic
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassCodec
import kotlinx.serialization.SerialName
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

internal object PolymorphicChildUtil {

    fun createChildSlumberer(type: KType): PolymorphicChildSlumberer {

        val cls = type.classifier as KClass<*>

        val parent = PolymorphicParentUtil.getParent(cls) ?: cls

        val discriminator = PolymorphicParentUtil.getDiscriminator(parent)

        val identifier = getIdentifier(cls)

        return PolymorphicChildSlumberer(
            discriminator = discriminator,
            identifier = identifier,
            childSlumberer = DataClassCodec(type)
        )
    }

    /**
     * Checks if the given [cls] is a polymorphic child.
     *
     * A class is recognized as a polymorphic child when it:
     * - has a companion object of type [Polymorphic.Child]
     */
    fun isPolymorphicChild(cls: KClass<*>): Boolean =
        cls.companionObjectInstance is Polymorphic.Child ||
                cls.allSupertypes.any { (it.classifier as? KClass<*>)?.isSealed ?: false } ||
                cls.annotations.filterIsInstance<SerialName>().isNotEmpty()

    /**
     * Get the type identifier of a child class
     *
     * First we try to get the identifier from [Polymorphic.Child.identifier].
     * The we look for a [SerialName] annotation.
     * Otherwise we use the qualified name of the class
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
}

internal object PolymorphicParentUtil {

    /**
     * Creates a polymorphic awaker for the given [cls]
     */
    fun createParentAwaker(cls: KClass<*>): PolymorphicAwaker {

        val discriminator = getDiscriminator(cls)

        val map = getChildren(cls).map { PolymorphicChildUtil.getIdentifier(it) to it }.toMap()

        val default = getDefaultType(cls)

        return PolymorphicAwaker(discriminator, map, default)
    }

    /**
     * Creates a polymorphic slumberer for the given [cls]
     */
    fun createParentSlumberer(cls: KClass<*>): PolymorphicParentSlumberer {

        val parent = getParent(cls) ?: cls

        val discriminator = getDiscriminator(parent)

        val map = getChildren(parent).map { it to PolymorphicChildUtil.getIdentifier(it) }.toMap()

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
     * First we try to look into for the companion object [Polymorphic.Parent.discriminator].
     * If this is not present the [Polymorphic.defaultDiscriminator] is returned
     */
    fun getDiscriminator(cls: KClass<*>?): String =
        (cls?.companionObjectInstance as? Polymorphic.Parent)?.discriminator
            ?: Polymorphic.defaultDiscriminator

    /**
     * Gets the default type used for awaking polymorphic children.
     *
     * Looks for a companion object with [Polymorphic.Parent] and returns the [Polymorphic.Parent.defaultType].
     * Otherwise returns null.
     *
     * The default type can by null, which means that nothing will be awoken, when
     * - the discriminator field is not present in the data
     * - or when it contains an invalid type identifier
     */
    fun getDefaultType(cls: KClass<*>?): KClass<*>? = when (val companion = cls?.companionObjectInstance) {

        is Polymorphic.Parent -> companion.defaultType

        else -> null
    }

    /**
     * Gets all child type for a parent type.
     *
     * First we try to get children from the companion object [Polymorphic.Parent.childTypes].
     * Then we add [KClass.sealedSubclasses].
     */
    fun getChildren(cls: KClass<*>?): List<KClass<*>> {

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
    }

    /**
     * Gets the [Polymorphic.Parent] of the given class
     *
     * If the cls itself is the parent it is returned as is.
     * If the cls is a [Polymorphic.Child] we try to find the first [Polymorphic.Parent] in its super classes.
     * Otherwise we return null
     */
    fun getParent(cls: KClass<*>): KClass<*>? = when (cls.companionObjectInstance) {

        is Polymorphic.Parent -> cls

        else -> cls.allSuperclasses.firstOrNull { it.companionObjectInstance is Polymorphic.Parent }
    }
}
