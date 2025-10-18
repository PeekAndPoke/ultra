package de.peekandpoke.ultra.log

import kotlin.reflect.KClass

class LogImpl internal constructor(private val caller: KClass<*>, private val manager: UltraLogManager) : Log {

    override fun log(level: LogLevel, message: String) {
        manager.log(level, message, caller.qualifiedName ?: "n/a")
    }
}
