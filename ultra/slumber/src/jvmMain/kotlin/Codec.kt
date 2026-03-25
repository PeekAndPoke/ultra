package io.peekandpoke.ultra.slumber

import io.peekandpoke.ultra.common.TypedAttributes
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

/**
 * Orchestrates serialization (slumber) and deserialization (awake) using a [SlumberConfig].
 *
 * Uses a two-pass strategy: the first pass uses a lightweight [Awaker.Context.Fast] / [Slumberer.Context.Fast]
 * with no path tracking. If it fails, a second pass with full [Awaker.Context.Tracking] provides
 * detailed error diagnostics including the exact path where deserialization failed.
 *
 * Use [Codec.default] for standard usage with built-in type support.
 */
@Suppress("Detekt:TooManyFunctions")
open class Codec(
    val config: SlumberConfig,
) {
    companion object {
        private val class2typeCache = mutableMapOf<KClass<*>, KType>()

        /** Default codec with built-in support for primitives, collections, data classes, datetime, enums, and polymorphism. */
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

    val attributes: TypedAttributes = config.attributes

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


    /** Returns the [Awaker] registered for the given [type]. Throws if no awaker is found. */
    fun getAwaker(type: KType): Awaker = config.getAwaker(type)

    /** Deserializes [data] into an object of the given [type]. Uses two-pass error handling. */
    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    fun awake(type: KType, data: Any?): Any? {
        contract {
            (data != null) implies (returnsNotNull())
        }

        return awakeInternal(type, data)
    }

    /** Returns the [Slumberer] registered for the given [type]. Throws if no slumberer is found. */
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

    /** Serializes [data] as the given [targetType]. Uses two-pass error handling. */
    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    fun slumber(targetType: KType, data: Any?): Any? {
        contract {
            (data != null) implies (returnsNotNull())
        }

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
