package de.peekandpoke.ultra.slumber

import kotlin.reflect.KType

interface SlumberModule {

    fun getAwaker(type: KType): Awaker?

    fun getSlumberer(type: KType): Slumberer?
}

