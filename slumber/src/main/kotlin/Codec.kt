package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

@Suppress("Detekt:TooManyFunctions")
open class Codec(
    private val config: SlumberConfig,
    private val attributes: TypedAttributes = TypedAttributes.empty,
) {
    companion object {
        val default = Codec(
            config = SlumberConfig.default
        )
    }

    val firstPassAwakerContext: Awaker.Context by lazy {
        Awaker.Context.Fast(
            codec = this,
            attributes = attributes,
        )
    }

    fun createSecondPassAwakerContext(
        rootType: KType,
    ): Awaker.Context.Tracking = Awaker.Context.Tracking(
        codec = this,
        rootType = rootType,
        attributes = attributes,
        path = "root",
        logs = mutableListOf(),
    )

    open val firstPassSlumbererContext: Slumberer.Context by lazy {
        Slumberer.Context.Fast(this, attributes)
    }

    open val secondPassSlumbererContext: Slumberer.Context
        get() = Slumberer.Context.Tracking(this, attributes, "root")

    private val class2typeCache = mutableMapOf<KClass<*>, KType>()

    fun <T> getAwaker(type: TypeRef<T>) = getAwaker(type.type)

    fun getAwaker(type: KType): Awaker = config.getAwaker(type)

    fun <T> getSlumberer(type: TypeRef<T>) = getSlumberer(type.type)

    fun getSlumberer(type: KType): Slumberer = config.getSlumberer(type)

    fun <T : Any> awake(type: KClass<T>, data: Any?): T? {
        return awakeInternal(type.kType(), data)
    }

    fun awake(type: KType, data: Any?): Any? {
        return awakeInternal(type, data)
    }

    fun <T> awake(type: TypeRef<T>, data: Any?): T? {
        @Suppress("UNCHECKED_CAST")
        return awake(type.type, data) as T?
    }

    inline fun <reified T> awake(data: Any?): T? {
        return awake(kType<T>().type, data) as T?
    }

    internal fun <T : Any> awake(type: KClass<T>, data: Any?, context: Awaker.Context): T? {
        @Suppress("UNCHECKED_CAST")
        return awake(type.kType(), data, context) as T?
    }

    internal fun awake(type: KType, data: Any?, context: Awaker.Context): Any? {
        return getAwaker(type).awake(data, context)
    }

    private fun <T> awakeInternal(type: KType, data: Any?): T? {

        val awaker = getAwaker(type)

        @Suppress("UNCHECKED_CAST")
        return try {
            awaker.awake(data, firstPassAwakerContext)
        } catch (e: AwakerException) {
            awaker.awake(data, createSecondPassAwakerContext(type))
        } as? T?
    }

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
        return slumberInternal(targetType, data)
    }

    internal fun <T : Any> slumber(data: Any?, context: Slumberer.Context): T? {
        val cls = when {
            data != null -> data::class
            else -> Nothing::class
        }

        @Suppress("UNCHECKED_CAST")
        return slumber(cls, data, context) as T?
    }

    internal fun <T : Any> slumber(type: KClass<T>, data: Any?, context: Slumberer.Context): T? {
        @Suppress("UNCHECKED_CAST")
        return slumber(type.kType(), data, context) as T?
    }

    internal fun slumber(type: KType, data: Any?, context: Slumberer.Context): Any? {

//        val slumberer = if (data == null) {
//            getSlumberer(type)
//        } else if (type.isSupertypeOf(data::class.starProjectedType)) {
//            getSlumberer(data::class.starProjectedType)
//        } else {
//            getSlumberer(type)
//        }
//
        return getSlumberer(type).slumber(data, context)
    }

    private fun <T> slumberInternal(type: KType, data: Any?): T? {

        val slumberer = getSlumberer(type)

        @Suppress("UNCHECKED_CAST")
        return try {
            slumberer.slumber(data, firstPassSlumbererContext)
        } catch (e: SlumberException) {
            slumberer.slumber(data, secondPassSlumbererContext)
        } as? T?
    }

    /**
     * Creates a KType from the given class by trying to assign somewhat useful type parameters
     */
    private fun KClass<*>.kType() = class2typeCache.getOrPut(this) {
        createType(
            typeParameters.map { KTypeProjection.invariant(it.upperBounds[0]) }
        )
    }
}
