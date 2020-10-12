package de.peekandpoke.ultra.logging

import de.peekandpoke.ultra.kontainer.InjectionContext
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

@Suppress("unused")
fun KontainerBuilder.ultraLogging() = module(Ultra_Logging)

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
 * singleton { logger: org.slf4j.Logger -> Slf4jAppender(logger) }
 * </code>
 */
val Ultra_Logging = module {

    dynamic(UltraLogManager::class)

    // TODO: we need another injection type: dynamicPrototype.
    //       It must upgrade the injecting service to SemiDynamic.
    //       But it must be instantiated like a prototype.
    prototype(Log::class) { manager: UltraLogManager, context: InjectionContext ->
        manager.getLogger(context.requestingClass)
    }
}
