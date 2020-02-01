package de.peekandpoke.ultra.logging

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.reflect.KClass

class UltraLogManager(private val appenders: List<LogAppender>) {

    fun getLogger(cls: KClass<*>): Log {
        return LogImpl(cls, this)
    }

    fun log(level: LogLevel, message: String, loggerName: String) {

        val now = ZonedDateTime.now()

        GlobalScope.launch {
            appenders.forEach {
                it.append(now, level, message, loggerName)
            }
        }
    }
}
