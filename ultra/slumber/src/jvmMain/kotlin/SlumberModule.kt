package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import kotlin.reflect.KType

interface SlumberModule {

    fun getAwaker(type: KType, attributes: TypedAttributes): Awaker?

    fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer?

    fun KType.wrapIfNonNull(awaker: Awaker): Awaker = when (isMarkedNullable) {
        true -> awaker
        false -> NonNullAwaker(awaker)
    }

    fun KType.wrapIfNonNull(slumberer: Slumberer): Slumberer = when (isMarkedNullable) {
        true -> slumberer
        false -> NonNullSlumberer(slumberer)
    }
}
