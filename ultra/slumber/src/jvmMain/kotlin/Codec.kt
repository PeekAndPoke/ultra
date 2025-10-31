package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
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
        private val class2typeCache = mutableMapOf<KClass<*>, KType>()

        val default = Codec(
            config = SlumberConfig.default
        )

        /**
         * Creates a KType from the given class by trying to assign somewhat useful type parameters
         */
        internal fun KClass<*>.createType() = class2typeCache.getOrPut(this) {
            this.createType(
                typeParameters.map { KTypeProjection.invariant(it.upperBounds[0]) }
            )
        }
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


    fun getAwaker(type: KType): Awaker = config.getAwaker(type)

    fun awake(type: KType, data: Any?): Any? {
//        contract {
//            (data != null) implies (returnsNotNull())
//        }

        return awakeInternal(type, data)
    }

    fun getSlumberer(type: KType): Slumberer = config.getSlumberer(type)


    internal fun <T : Any> awake(type: KClass<T>, data: Any?, context: Awaker.Context): T? {
        @Suppress("UNCHECKED_CAST")
        return awake(type.createType(), data, context) as T?
    }

    internal fun awake(type: KType, data: Any?, context: Awaker.Context): Any? {
        return getAwaker(type).awake(data, context)
    }

    private fun <T> awakeInternal(type: KType, data: Any?): T? {

        val awaker = getAwaker(type)

        @Suppress("UNCHECKED_CAST")
        return try {
            awaker.awake(data, firstPassAwakerContext)
        } catch (_: AwakerException) {
            awaker.awake(data, createSecondPassAwakerContext(type))
        } as? T?
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
        return slumber(type.createType(), data, context) as T?
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
        } catch (_: SlumberException) {
            slumberer.slumber(data, secondPassSlumbererContext)
        } as? T?
    }
}
