package io.peekandpoke.ultra.slumber

import io.peekandpoke.ultra.common.TypedAttributes
import kotlin.reflect.KType

/**
 * Plugin interface for teaching Slumber how to handle custom types.
 *
 * Modules are queried in order by [SlumberConfig]. Return null from [getAwaker]/[getSlumberer]
 * to pass to the next module.
 *
 * Resolution happens ONCE per [KType] per config -- [SlumberConfig] caches what a module returns -- so a
 * returned codec outlives the call that created it. Read request-scoped state from the
 * [Awaker.Context]/[Slumberer.Context] at awake/slumber time, not from [TypedAttributes] here.
 */
interface SlumberModule {

    /** Returns an [Awaker] for the given [type], or null if this module doesn't handle it. */
    fun getAwaker(type: KType, attributes: TypedAttributes): Awaker?

    /** Returns a [Slumberer] for the given [type], or null if this module doesn't handle it. */
    fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer?

    /**
     * Wraps the [awaker] in [NonNullAwaker] if the type is non-nullable.
     *
     * Ambiguous with the [Slumberer] overload for a codec that implements both interfaces (most of the
     * built-ins do), so those call sites need an explicit `as Awaker` cast to pick this one.
     */
    fun KType.wrapIfNonNull(awaker: Awaker): Awaker = when (isMarkedNullable) {
        true -> awaker
        false -> NonNullAwaker(awaker)
    }

    /** Wraps the [slumberer] in [NonNullSlumberer] if the type is non-nullable. See the [Awaker] overload. */
    fun KType.wrapIfNonNull(slumberer: Slumberer): Slumberer = when (isMarkedNullable) {
        true -> slumberer
        false -> NonNullSlumberer(slumberer)
    }
}
