package io.peekandpoke.ultra.log

import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

/**
 * Installs the [Ultra_Logging] module into this [KontainerBuilder].
 *
 * @param minLevel a global lower bound. An event less critical than this reaches no appender,
 *   whatever the individual appenders accept. Defaults to no bound.
 */
@Suppress("unused")
fun KontainerBuilder.ultraLogging(minLevel: LogLevel = LogLevel.ALL) = module(Ultra_Logging, minLevel)

/**
 * Ultra Logging kontainer module
 *
 * You can inject context aware instances of [Log] into your kontainer services:
 *
 * <code>
 *     class MyService(log: Log) { ... }
 * </code>
 *
 * For a simple [ConsoleAppender] add a:
 *
 * <code>
 * singleton(ConsoleAppender::class)
 * </code>
 *
 * For an [Slf4jAppender] add a:
 *
 * <code>
 * singleton(Slf4jAppender::class)
 * </code>
 */
val Ultra_Logging = module { minLevel: LogLevel ->

    // Dynamic because appenders may themselves be dynamic (per-request), e.g. the insights LogCollector.
    // TODO(scan): consequently every service injecting a `Log` is silently promoted to SemiDynamic
    //
    // Built by hand rather than by ctor injection: kontainer resolves every ctor parameter as a
    // service and does not see Kotlin default values, so `minLevel` has to be closed over.
    dynamic(UltraLogManager::class) { appenders: List<LogAppender> ->
        UltraLogManager(appenders = appenders, minLevel = minLevel)
    }

    // TODO: we need another injection type: dynamicPrototype.
    //       It must upgrade the injecting service to SemiDynamic.
    //       But it must be instantiated like a prototype.
    prototype(Log::class) { manager: UltraLogManager, context: InjectionContext ->
        manager.getLogger(context.requestingClass)
    }
}
