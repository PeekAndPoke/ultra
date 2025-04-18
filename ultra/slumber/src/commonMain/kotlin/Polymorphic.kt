package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.slumber.Polymorphic.Companion.defaultDiscriminator
import kotlin.reflect.KClass

/**
 * Used to apply custom polymorphic settings
 */
interface Polymorphic {

    companion object {
        /**
         * Default type discriminator field name
         */
        const val defaultDiscriminator: String = "_type"
    }

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
        companion object {
            @Suppress("UnusedReceiverParameter")
            inline fun <reified T : Any> Parent.children(
                builder: PolymorphicChildrenToSerializers.Builder<T>.() -> Unit,
            ): PolymorphicChildrenToSerializers<T> {
                return PolymorphicChildrenToSerializers.Builder(T::class).apply(builder).build()
            }
        }

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
        val childTypes: Set<KClass<*>>
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

    /**
     * Similar to [Child].
     *
     * Carries additional type information about the owning type.
     * This is useful for polymorphic queries where the type and the serialName are needed.
     *
     * Example:
     *
     * <code>
     *     class MyChild : MyParent() {
     *         companion object : Polymorphic.TypedChild<MyChild> {
     *             override val identifier = "Child"
     *         }
     *     }
     * </code>
     *
     * This can then be used to query a repo:
     *
     * <code>
     *     suspend inline fun <reified T : MyParent> MyRepo.findFirst(type: TypedChild<T>): Stored<T>? {
     *         return findFirst {
     *             FOR(repo) { item ->
     *                 FILTER(item._type EQ type.identifier)
     *                 RETURN(item)
     *             }
     *         }?.castTyped()
     *     }
     * </code>
     */
    interface TypedChild<T> : Child
}
