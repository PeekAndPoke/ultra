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

    fun getAwaker(type: KType): Awaker = config.getAwaker(type)

    fun <T : Any> awake(type: KClass<T>, data: Any?): T? = awake(type, data, awakerContext)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> awake(type: KClass<T>, data: Any?, context: Awaker.Context): T? =
        awake(type.kType(), data, context) as T?

    fun awake(type: KType, data: Any?): Any? =
        awake(type, data, awakerContext)

    fun awake(type: KType, data: Any?, context: Awaker.Context): Any? =
        getAwaker(type).awake(data, context)

    fun slumber(data: Any?): Any? {

        val cls = when {
            data != null -> data::class
            else -> Nothing::class
        }

        return slumber(cls, data)
    }

    fun <T : Any> slumber(targetType: KClass<T>, data: Any?): T? {
        @Suppress("UNCHECKED_CAST")
        return slumber(targetType.kType(), data) as T?
    }

    fun slumber(targetType: KType, data: Any?): Any? {
        return config.getSlumberer(targetType).slumber(data, slumbererContext)
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
