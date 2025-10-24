package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.api.AuthApiFeature
import de.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import de.peekandpoke.funktor.auth.provider.GithubSsoAuth
import de.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*

/** Helper for importing [FunktorAuth] into a [KontainerBuilder] */
fun KontainerBuilder.funktorAuth() = module(FunktorAuth)

inline val KontainerAware.funktorAuth: AuthSystem get() = kontainer.get()
inline val ApplicationCall.funktorAuth: AuthSystem get() = kontainer.funktorAuth
inline val RoutingContext.funktorAuth: AuthSystem get() = call.funktorAuth

val FunktorAuth = module {
    // Facade
    dynamic(AuthSystem::class)
    dynamic(AuthSystem.Deps::class)
    // Services
    instance(AuthRandom())
    // Provider Factories
    dynamic(EmailAndPasswordAuth.Factory::class)
    dynamic(GoogleSsoAuth.Factory::class)
    dynamic(GithubSsoAuth.Factory::class)
    // API
    singleton(AuthApiFeature::class)
}
