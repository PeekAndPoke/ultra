package de.peekandpoke.ultra.slumber

import kotlin.reflect.KType

open class SlumberException(message: String) : Throwable(message)

class AwakerException(
    message: String,
    val logs: List<String>,
    val rootType: KType?,
    val input: Any?,
) : SlumberException(message)

class SlumbererException(
    message: String,
    val input: Any?,
) : SlumberException(message)
