package de.peekandpoke.ultra.slumber

import kotlin.reflect.KType

interface SlumberModule {

    fun getAwaker(type: KType, shared: Shared): Awaker?

    fun getSlumberer(type: KType, shared: Shared): Slumberer?
}

