package io.peekandpoke.ultra.slumber

import io.peekandpoke.ultra.slumber.Polymorphic.Companion.defaultDiscriminator
import kotlin.reflect.KClass

/**
 * Namespace for the polymorphism opt-ins: [Parent], [Child] and [TypedChild].
 *
 * Nothing implements `Polymorphic` itself — a type opts in by letting its companion object implement
 * one of the nested interfaces. Sealed hierarchies already work without any opt-in; these interfaces
 * only customise the discriminator field, the type identifiers and the child set.
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
     * To use it, let the companion object of the polymorphic parent implement this interface.
     *
     * ```
     * open class MyParent {
     *     companion object : Polymorphic.Parent {
     *         override val discriminator = "_field"
     *         override val childTypes = setOf(MyChild::class)
     *     }
     * }
     * ```
     */
    interface Parent {
        companion object {
            /**
             * Builds a [PolymorphicChildrenToSerializers] for [T] — usable both as [Parent.childTypes]
             * and as a kotlinx-serialization module registration.
             */
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
         * The default type for deserialization, or null.
         *
         * When set, an object of this type is created whenever the discriminator field is missing
         * from the data OR holds an identifier that maps to no known child.
         */
        val defaultType get(): KClass<*>? = null

        /**
         * Extra child types, for hierarchies that are not (only) sealed.
         *
         * Sealed subclasses are picked up automatically and need not be listed here. Entries are
         * expanded transitively, and entries that are not subclasses of the parent are dropped.
         * Identifiers are taken from the child types themselves.
         *
         * @see Child
         */
        val childTypes: Set<KClass<*>>
    }

    /**
     * Applies custom settings to a polymorphic child class.
     *
     * To use it, let the companion object of the polymorphic CHILD implement this interface.
     *
     * ```
     * class MyChild : MyParent() {
     *     companion object : Polymorphic.Child {
     *         override val identifier = "Child"
     *     }
     * }
     * ```
     */
    interface Child {
        /**
         * The identifier is used to decide, which child class is to be de-serialized.
         *
         * It will be written into or read from the [Parent.discriminator] field, and it takes
         * precedence over a `@SerialName` annotation on the same class.
         */
        val identifier: String
    }

    /**
     * Similar to [Child].
     *
     * Carries additional type information about the owning type.
     * This is useful for polymorphic queries where the type and the serialName are needed.
     *
     * ```
     * class MyChild : MyParent() {
     *     companion object : Polymorphic.TypedChild<MyChild> {
     *         override val identifier = "Child"
     *     }
     * }
     * ```
     *
     * This can then be used to query a repo:
     *
     * ```
     * suspend inline fun <reified T : MyParent> MyRepo.findFirst(type: TypedChild<T>): Stored<T>? {
     *     return findFirst {
     *         FOR(repo) { item ->
     *             FILTER(item._type EQ type.identifier)
     *             RETURN(item)
     *         }
     *     }?.castTyped()
     * }
     * ```
     *
     * [T] is a phantom parameter: it carries the owning type for call-site inference and is not used
     * by the serialization machinery.
     */
    interface TypedChild<T> : Child
}
