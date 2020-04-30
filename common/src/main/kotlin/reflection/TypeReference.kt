package de.peekandpoke.ultra.common.reflection

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

/**
 * Taken from Jackson 2.9.9
 */
abstract class TypeReference<T> protected constructor(
    private val classLoader: ClassLoader
) : Comparable<TypeReference<T>> {

    private val type: Type

    init {
        val superClass = javaClass.genericSuperclass

        require(superClass !is Class<*>) {
            // sanity check, should never happen
            "Internal error: TypeReference constructed without actual type information"
        }

        type = (superClass as ParameterizedType).actualTypeArguments[0]
    }

    open fun getType() = type

    /**
     * The only reason we define this method (and require implementation
     * of `Comparable`) is to prevent constructing a
     * reference without type information.
     */
    override fun compareTo(other: TypeReference<T>): Int {
        return 0
    }
    // just need an implementation, not a good one... hence ^^^

    /**
     * Returns the resulting type as a [KType]
     */
    fun toKType(): KType = type.toKType()

    /**
     * Internal helper for creating a [KType] from a [Type]
     */
    private fun Type.toKType(): KType = when (this) {

        is ParameterizedType -> toClass().createType(
            arguments = actualTypeArguments.map {
                KTypeProjection.invariant(it.toKType())
            },
            nullable = false // This is not so good. But there is currently no way to get the nullability
        )

        is WildcardType -> toClass().createType(
            arguments = upperBounds[0].toKType().arguments.map {
                KTypeProjection.invariant(
                    it.type!!
                )
            },
            nullable = false // This is not so good. But there is currently no way to get the nullability
        )

        else -> toClass().createType(
            arguments = listOf(),
            nullable = false // This is not so good. But there is currently no way to get the nullability
        )
    }

    /**
     * Internal helper for creating a [KClass] from the a [Type]
     */
    private fun Type.toClass(): KClass<*> = when (this) {
        is ParameterizedType -> classForName(this.rawType.typeName)
        is WildcardType -> this.upperBounds[0].toClass()
        else -> classForName(this.typeName)
    }

    /**
     * Loads a class for the given [fqn] with the given [classLoader]
     */
    private fun classForName(fqn: String): KClass<*> =
        Class.forName(fqn, true, classLoader).kotlin
}
