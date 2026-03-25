package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import kotlin.reflect.KType

/**
 * Plugin interface for teaching Slumber how to handle custom types.
 *
 * Modules are queried in order by [SlumberConfig]. Return null from [getAwaker]/[getSlumberer]
 * to pass to the next module.
 */
interface SlumberModule {

    /** Returns an [Awaker] for the given [type], or null if this module doesn't handle it. */
    fun getAwaker(type: KType, attributes: TypedAttributes): Awaker?

    /** Returns a [Slumberer] for the given [type], or null if this module doesn't handle it. */
    fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer?

    /** Wraps the [awaker] in [NonNullAwaker] if the type is non-nullable. */
    fun KType.wrapIfNonNull(awaker: Awaker): Awaker = when (isMarkedNullable) {
        true -> awaker
        false -> NonNullAwaker(awaker)
    }

    /** Wraps the [slumberer] in [NonNullSlumberer] if the type is non-nullable. */
    fun KType.wrapIfNonNull(slumberer: Slumberer): Slumberer = when (isMarkedNullable) {
        true -> slumberer
        false -> NonNullSlumberer(slumberer)
    }
}
