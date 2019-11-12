package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.slumber.builtin.BuiltInModule
import de.peekandpoke.ultra.slumber.builtin.DateTimeModule
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

open class Codec(
    private val config: Config,
    private val attributes: TypedAttributes = TypedAttributes.empty
) {

    companion object {
        val default = Codec(
            Config(
                listOf(
                    DateTimeModule,
                    BuiltInModule
                )
            )
        )
    }

    open val awakerContext by lazy(LazyThreadSafetyMode.NONE) {
        Awaker.Context(this, attributes, "root")
    }

    open val slumbererContext by lazy(LazyThreadSafetyMode.NONE) {
        Slumberer.Context(this, attributes, "root")
    }

    private val kTypeCache = mutableMapOf<KClass<*>, KType>()

    fun <T : Any> awake(type: KClass<T>, data: Any?): T = awake(type.kType(), data)

    fun <T> awake(type: KType, data: Any?): T {
        @Suppress("UNCHECKED_CAST")
        return awakeOrNull(type, data) as T?
            ?: throw AwakerException("Could not awake '${type}'")
    }

    fun <T : Any> awakeOrNull(type: KClass<T>, data: Any?): T? {
        @Suppress("UNCHECKED_CAST")
        return awakeOrNull(type.kType(), data) as T?
    }

    fun awakeOrNull(type: KType, data: Any?): Any? = awakeOrNull(type, data, awakerContext)

    internal fun awakeOrNull(type: KType, data: Any?, context: Awaker.Context): Any? {
        return getAwaker(type).awake(data, context)
    }

    fun getAwaker(type: KType): Awaker = config.getAwaker(type)

    fun slumber(data: Any?): Any? {

        val cls = when {
            data != null -> data::class
            else -> Nothing::class
        }

        return slumber(cls, data)
    }

    fun <T : Any> slumber(targetType: KClass<T>, data: Any?): T? {
        return slumber(targetType.kType(), data)
    }

    fun <T : Any> slumber(targetType: KType, data: Any?): T? {
        @Suppress("UNCHECKED_CAST")
        return config.getSlumberer(targetType).slumber(data, slumbererContext) as T?
    }

    /**
     * Creates a KType from the given class by trying to assign somewhat useful type parameters
     */
    private fun KClass<*>.kType() = kTypeCache.getOrPut(this) {
        createType(
            typeParameters.map { KTypeProjection.invariant(it.upperBounds[0]) }
        )
    }
}
