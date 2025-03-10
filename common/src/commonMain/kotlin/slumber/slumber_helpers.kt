package de.peekandpoke.ultra.slumber

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

// TODO: Test me
fun <T : Any> SerializersModuleBuilder.polymorphic(c2s: PolymorphicChildrenToSerializers<T>) {
    polymorphic(baseClass = c2s.base) {
        addChildren(c2s)
    }
}

// TODO: Test me
fun <T : Any> PolymorphicModuleBuilder<T>.addChildren(items: PolymorphicChildrenToSerializers<T>) {
    items.entries.forEach {
        @Suppress("UNCHECKED_CAST")
        subclass(it.type as KClass<T>, it.serializer as KSerializer<T>)
    }
}

// TODO: Test me
@kotlin.ConsistentCopyVisibility
data class PolymorphicChildrenToSerializers<T : Any> internal constructor(
    val base: KClass<T>,
    val entries: List<TypeAndSerializer<T>>,
) : Set<KClass<out T>> {

    class Builder<T : Any>(
        val base: KClass<T>,
    ) {
        @PublishedApi
        internal val entries = mutableListOf<TypeAndSerializer<T>>()

        @PublishedApi
        internal fun build() = PolymorphicChildrenToSerializers(base, entries.toList())

        inline fun <reified X : T> add() {
            entries.add(X::class with serializer())
        }

        fun addAll(nested: PolymorphicChildrenToSerializers<out T>) {
            entries.addAll(nested.entries)
        }

        @PublishedApi
        internal infix fun <T : Any> KClass<T>.with(serializer: KSerializer<T>) =
            TypeAndSerializer(this, serializer)
    }

    data class TypeAndSerializer<out T : Any>(
        val type: KClass<out T>,
        val serializer: KSerializer<out T>,
    )

    val typeSet: Set<KClass<out T>> = entries.map { it.type }.toSet()

    override val size: Int = typeSet.size

    override fun contains(element: KClass<out T>): Boolean {
        return typeSet.contains(element)
    }

    override fun containsAll(elements: Collection<KClass<out T>>): Boolean {
        return typeSet.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return typeSet.isEmpty()
    }

    override fun iterator(): Iterator<KClass<out T>> {
        return typeSet.iterator()
    }
}
