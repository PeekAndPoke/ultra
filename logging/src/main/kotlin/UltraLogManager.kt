package de.peekandpoke.ultra.logging

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.reflect.KClass

class UltraLogManager(appenders: List<LogAppender>) {

    private val appenders = appenders.toMutableList()

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

    fun add(appender: LogAppender) {
        appenders.add(appender)
    }
}
