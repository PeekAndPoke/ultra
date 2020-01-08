package de.peekandpoke.ultra.slumber.builtin.polymorphism

import org.atteo.classindex.ClassIndex
import org.atteo.classindex.IndexSubclasses
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

/**
 * Used to apply custom polymorphic settings
 */
interface Polymorphic {

    /**
     * Applies custom settings to a polymorphic parent class.
     *
     * In order to use, you need to make the companion object of the polymorphic parent implements this interface.
     *
     * Example:
     *
     * <code>
     *     class MyParent {
     *         companion object : Polymorphic.Parent {
     *             override val discriminator = "_field"
     *         }
     *     }
     * </code>
     */
    interface Parent {
        /**
         * The name of the data field which acts as the discriminator, defaulting to [defaultDiscriminator]
         *
         * The discriminator is used to tell which child class needs to be de-serialized.
         */
        val discriminator get(): String = defaultDiscriminator

        /**
         * The default type for deserialization or null.
         *
         * When the discriminator field is missing and the defaultType is set, then an object of the
         * defaultType will be created on de-serialization.
         */
        val defaultType get(): KClass<*>? = null

        /**
         * A set of child types.
         *
         * The identifiers are taken from the child types directly.
         *
         * @see [Child]
         */
        val childTypes get(): Set<KClass<*>> = emptySet()

        /**
         * A set of sub-classes indexed by [ClassIndex]
         *
         * To make this work, the parent class needs to be annotated with [IndexSubclasses]
         */
        val <T : Any> KClass<T>.indexedSubClasses
            get(): Set<KClass<out T>> {

                val loader = java.classLoader ?: Thread.currentThread().contextClassLoader

                return ClassIndex.getSubclasses(java, loader).map { it.kotlin }.toSet()
            }
    }

    /**
     * Applies custom settings to a polymorphic child class.
     *
     * In order to use, you need to make the companion object of the polymorphic parent implements this interface.
     *
     * Example:
     *
     * <code>
     *     class MyChild : MyParent() {
     *         companion object : Polymorphic.Child {
     *             override val identifier = "Child"
     *         }
     *     }
     * </code>
     */
    interface Child {
        /**
         * The identifier is used to decide, which child class is to be de-serialized.
         *
         * It will be written into or read from the [Parent.discriminator] field.
         */
        val identifier: String
    }

    companion object {

        /**
         * Default type discriminator field name
         */
        const val defaultDiscriminator: String = "_type"

        /**
         * Checks if the given [cls] is can be awoken as a polymorphic type
         */
        fun supports(cls: KClass<*>): Boolean = isPolymorphicParent(cls)

        /**
         * Creates a polymorphic awaker for the given [cls]
         */
        fun createAwaker(cls: KClass<*>): PolymorphicAwaker {

            val discriminator = getDiscriminator(cls)

            val map = getChildren(cls).map { it.getType() to it }.toMap()

            val default = getDefaultType(cls)

            return PolymorphicAwaker(discriminator, map, default)
        }

        /**
         * Creates a polymorphic slumberer for the given [cls]
         */
        fun createSlumberer(cls: KClass<*>): PolymorphicSlumberer {

            val discriminator = getDiscriminator(cls)

            val map = getChildren(cls).map { it to it.getType() }.toMap()

            val default = getDefaultType(cls)

            return PolymorphicSlumberer(discriminator, map, default)
        }

        /**
         * Checks if the given [cls] is a polymorphic parent
         */
        fun isPolymorphicParent(cls: KClass<*>): Boolean =
            cls.isSealed || cls.companionObjectInstance is Parent

        /**
         * Gets the name of the discriminator field.
         *
         * First we try to look into for the companion object [Parent.discriminator].
         * If this is not present the [defaultDiscriminator] is returned
         */
        fun getDiscriminator(cls: KClass<*>): String = when (val companion = cls.companionObjectInstance) {

            is Parent -> companion.discriminator

            else -> defaultDiscriminator
        }

        /**
         * Gets the default type used for awaking polymorphic children.
         *
         * Looks for a companion object with [Parent] and returns the [Parent.defaultType].
         * Otherwise returns null.
         *
         * The default type can by null, which means that nothing will be awoken, when
         * - the discriminator field is not present in the data
         * - or when it contains an invalid type identifier
         */
        fun getDefaultType(cls: KClass<*>): KClass<*>? = when (val companion = cls.companionObjectInstance) {

            is Parent -> companion.defaultType

            else -> null
        }

        /**
         * Gets all child type for a parent type.
         *
         * First we try to get children from the companion object [Parent.childTypes].
         * Then we add [KClass.sealedSubclasses].
         */
        fun getChildren(cls: KClass<*>): List<KClass<*>> {

            val annotated = when (val companion = cls.companionObjectInstance) {
                is Parent -> companion.childTypes
                else -> emptySet()
            }

            return annotated
                .plus(cls.sealedSubclasses)
                .flatMap { getChildren(it).plus(it) }
                .filter { !it.isAbstract }
                .filter { !it.isSealed }
        }

        /**
         * Get the type identifier of a child class
         *
         * First we try to get the identifier from [Child.identifier].
         * Otherwise we use the simple name of the class
         */
        fun KClass<*>.getType(): String = when (val companion = companionObjectInstance) {
            is Child -> companion.identifier
            else -> qualifiedName ?: jvmName
        }
    }
}
