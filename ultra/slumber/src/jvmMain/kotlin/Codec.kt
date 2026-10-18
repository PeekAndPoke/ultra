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
 * Uses a two-pass strategy: the first pass runs with a lightweight [Awaker.Context.Fast] /
 * [Slumberer.Context.Fast] that tracks no path. If that pass throws, the whole operation is repeated with
 * an [Awaker.Context.Tracking] / [Slumberer.Context.Tracking], whose error carries the exact path
 * (e.g. `root.items.2.name`) and the collected diagnostic logs.
 *
 * Use [Codec.default] for standard usage with built-in type support.
 */
@Suppress("Detekt:TooManyFunctions")
open class Codec(
    val config: SlumberConfig,
) {
    companion object {
        // TODO(scan): process-wide, unsynchronized and unbounded. `mutableMapOf` is a LinkedHashMap and
        //  `createType()` runs once per value in every slumbered object graph, on every request thread.
        //  It also strongly retains every KClass it ever saw, pinning their ClassLoaders forever.
        private val class2typeCache = mutableMapOf<KClass<*>, KType>()

        /** Default codec: primitives, collections, data classes, datetime, enums and polymorphism. */
        val default = Codec(
            config = SlumberConfig.default
        )

        /**
         * Memoised, non-nullable [KType] for this class, with every type parameter projected
         * invariantly onto its first upper bound (so `Box<T>` becomes `Box<Any?>`).
         *
         * Only an approximation: any call site that has the real type arguments should pass a [KType].
         */
        internal fun KClass<*>.createType() = class2typeCache.getOrPut(this) {
            this.createType(
                typeParameters.map { KTypeProjection.invariant(it.upperBounds[0]) }
            )
        }
    }

    /** Attributes handed to every [Awaker.Context] / [Slumberer.Context] this codec creates. */
    val attributes: TypedAttributes = config.attributes

    /** Shared, immutable context for the first (fast) awake pass. Carries no path and no logs. */
    val firstPassAwakerContext: Awaker.Context by lazy {
        Awaker.Context.Fast(
            codec = this,
            attributes = attributes,
        )
    }

    /** Creates a fresh path-tracking context rooted at [rootType], with its own log buffer. */
    fun createSecondPassAwakerContext(
        rootType: KType,
    ): Awaker.Context.Tracking = Awaker.Context.Tracking(
        codec = this,
        rootType = rootType,
        attributes = attributes,
        path = "root",
        logs = mutableListOf(),
    )

    /** Shared, immutable context for the first (fast) slumber pass. Carries no path. */
    open val firstPassSlumbererContext: Slumberer.Context by lazy {
        Slumberer.Context.Fast(this, attributes)
    }

    /** A fresh path-tracking context for the second (diagnostic) slumber pass. */
    open val secondPassSlumbererContext: Slumberer.Context
        get() = Slumberer.Context.Tracking(this, attributes, "root")

    /** Returns the [Awaker] registered for the given [type]. Throws if no awaker is found. */
    fun getAwaker(type: KType): Awaker = config.getAwaker(type)

    /**
     * Deserializes [data] into an object of the given [type], throwing [AwakerException] on failure.
     *
     * Runs the fast pass first; on [AwakerException] the whole graph is awoken a second time with a
     * tracking context, so the reported error names the failing path.
     */
    // TODO(scan): the contract below is unsound - `awake(typeOf<Unit>(), 1)` and
    //  `awake(typeOf<Int?>(), "abc")` both return null for non-null data.
    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    fun awake(type: KType, data: Any?): Any? {
        contract {
            (data != null) implies (returnsNotNull())
        }

        return awakeInternal(type, data)
    }

    /** Returns the [Slumberer] registered for the given [type]. Throws if no slumberer is found. */
    fun getSlumberer(type: KType): Slumberer = config.getSlumberer(type)

    /** Nested awake against an approximated type; see `Codec.Companion.createType` for the approximation. */
    internal fun <T : Any> awake(type: KClass<T>, data: Any?, context: Awaker.Context): T? {
        @Suppress("UNCHECKED_CAST")
        return awake(type.createType(), data, context) as T?
    }

    /** Nested awake that reuses the caller's [context], so path tracking and logs keep accumulating. */
    internal fun awake(type: KType, data: Any?, context: Awaker.Context): Any? {
        return getAwaker(type).awake(data, context)
    }

    // TODO(scan): the whole graph is awoken TWICE whenever the first pass throws, and a second pass that
    //  happens to succeed silently swallows the first failure. `as? T?` is erased and never checks anything.
    private fun <T> awakeInternal(type: KType, data: Any?): T? {

        val awaker = getAwaker(type)

        @Suppress("UNCHECKED_CAST")
        return try {
            awaker.awake(data, firstPassAwakerContext)
        } catch (_: AwakerException) {
            awaker.awake(data, createSecondPassAwakerContext(type))
        } as? T?
    }

    /**
     * Serializes [data] as the given [targetType], throwing [SlumbererException] on failure.
     *
     * Runs the fast pass first; on [SlumberException] the whole graph is slumbered a second time with a
     * tracking context, so the reported error names the failing path.
     */
    // TODO(scan): the contract below is unsound - `slumber(typeOf<Unit>(), 1)` and
    //  `slumber(typeOf<Int?>(), "abc")` both return null for non-null data.
    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    fun slumber(targetType: KType, data: Any?): Any? {
        contract {
            (data != null) implies (returnsNotNull())
        }

        return slumberInternal(targetType, data)
    }

    /** Nested slumber that picks the slumberer from the RUNTIME class of [data] (`Nothing` when null). */
    // TODO(scan): the `T` here is the SOURCE type, but the returned value is raw data (Map/List/scalar),
    //  and the cast is unchecked - a caller that names T gets a silently mistyped reference.
    internal fun <T : Any> slumber(data: Any?, context: Slumberer.Context): T? {
        val cls = when {
            data != null -> data::class
            else -> Nothing::class
        }

        @Suppress("UNCHECKED_CAST")
        return slumber(cls, data, context) as T?
    }

    /** Nested slumber against an approximated type; see `Codec.Companion.createType` for the approximation. */
    internal fun <T : Any> slumber(type: KClass<T>, data: Any?, context: Slumberer.Context): T? {
        @Suppress("UNCHECKED_CAST")
        return slumber(type.createType(), data, context) as T?
    }

    /** Nested slumber that reuses the caller's [context], so path tracking keeps accumulating. */
    internal fun slumber(type: KType, data: Any?, context: Slumberer.Context): Any? {
        return getSlumberer(type).slumber(data, context)
    }

    // TODO(scan): catches SlumberException, the BASE type, so it also retries on AwakerException; and it
    //  retries on any SlumberException a custom Slumberer throws, however unrecoverable.
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
