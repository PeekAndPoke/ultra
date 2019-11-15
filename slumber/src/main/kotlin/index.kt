package de.peekandpoke.ultra.slumber

import kotlin.reflect.KType

interface SlumberModule {

    fun getAwaker(type: KType): Awaker?

    fun getSlumberer(type: KType): Slumberer?

    fun KType.wrapIfNonNull(awaker: Awaker): Awaker = when (isMarkedNullable) {
        true -> awaker
        false -> NonNullAwaker(awaker)
    }

    fun KType.wrapIfNonNull(slumberer: Slumberer): Slumberer = when (isMarkedNullable) {
        true -> slumberer
        false -> NonNullSlumberer(slumberer)
    }
}

