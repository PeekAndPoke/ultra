package de.peekandpoke.ultra.logging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

class UltraLogManager(appenders: List<LogAppender>) {

    companion object {
        val scope = CoroutineScope(EmptyCoroutineContext)
    }

    private val appenders = appenders.toMutableList()

    fun getLogger(cls: KClass<*>): Log {
        return LogImpl(cls, this)
    }

    fun log(level: LogLevel, message: String, loggerName: String) {

        val now = ZonedDateTime.now()

        scope.launch {
            appenders.forEach {
                it.append(now, level, message, loggerName)
            }
        }
    }

    fun add(appender: LogAppender) {
        appenders.add(appender)
    }
}
