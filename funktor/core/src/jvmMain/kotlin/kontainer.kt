package de.peekandpoke.funktor.core

import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.ultra.kontainer.Kontainer
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.*

val AppKey = AttributeKey<App<*>>("app")

fun <C : AppConfig> Attributes.provideApp(app: App<C>) = put(AppKey, app)
fun <C : AppConfig> Application.provideApp(app: App<C>) = attributes.provideApp(app)

fun Attributes.getApp(): App<*> = getOrNull(AppKey) ?: throw IllegalStateException("No app found in attributes")
fun Application.getApp(): App<*> = attributes.getApp()

/**
 * [AttributeKey] for the [Kontainer]
 */
val KontainerKey = AttributeKey<Kontainer>("kontainer")

/** Puts a [Kontainer] instance into an [Attributes] set */
fun Attributes.provideKontainer(value: Kontainer) = put(KontainerKey, value)

/** Removes the [Kontainer] instance from the [Attributes] */
fun Attributes.removeKontainer() = remove(KontainerKey)

/** Checks if the kontainer is in the attributes */
inline val ApplicationCall.hasKontainer: Boolean get() = attributes.contains(KontainerKey)

/** Checks if the kontainer is in the attributes */
inline val RoutingContext.hasKontainer: Boolean get() = call.hasKontainer

/** Retrieves the [Kontainer] from the [Attributes] of an [ApplicationCall] */
inline val ApplicationCall.kontainer: Kontainer get() = attributes[KontainerKey]

/** Retrieves the [Kontainer] from the [Attributes] of an [ApplicationCall] */
inline val ApplicationCall.kontainerOrNull: Kontainer? get() = attributes.getOrNull(KontainerKey)

/** Retrieves the [Kontainer] from the [Attributes] of an [ApplicationCall] */
inline val RoutingContext.kontainer: Kontainer get() = call.kontainer

/** Retrieves the [Kontainer] from the [Attributes] of an [ApplicationCall] */
inline val RoutingContext.kontainerOrNull: Kontainer? get() = call.kontainerOrNull
