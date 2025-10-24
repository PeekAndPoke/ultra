package de.peekandpoke.ultra.log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.reflect.KClass

class UltraLogManager(appenders: List<LogAppender>) {

    companion object {
        private val scopeJob = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.Unconfined + scopeJob)
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
