package de.peekandpoke.ultra.slumber

import kotlin.reflect.KType

interface SlumberModule {

    fun getAwaker(type: KType, config: Config): Awaker?

    fun getSlumberer(type: KType, config: Config): Slumberer?
}

