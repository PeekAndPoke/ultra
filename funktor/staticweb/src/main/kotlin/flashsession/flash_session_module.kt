package io.peekandpoke.funktor.staticweb.flashsession

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.funktorFlashSession() = module(Funktor_FlashSession)

/**
 * FlashSession kontainer module
 *
 * Defines the following dynamic services:
 *
 * - [FlashSession] which defaults to [NullFlashSession]
 */
internal val Funktor_FlashSession = module {
    dynamic(FlashSession::class) { current: CurrentSession -> FlashSession.of(current) }
}

inline val ApplicationCall.flashSession: FlashSession get() = kontainer.get(FlashSession::class)
inline val RoutingContext.flashSession: FlashSession get() = call.flashSession
