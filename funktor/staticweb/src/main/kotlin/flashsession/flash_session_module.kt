package de.peekandpoke.funktor.staticweb.flashsession

import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

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
