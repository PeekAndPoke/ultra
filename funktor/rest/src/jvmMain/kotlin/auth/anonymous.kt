package io.peekandpoke.funktor.rest.auth

import io.ktor.server.auth.*

/**
 * A Ktor auth provider that always succeeds with [Caller.AnonymousCaller] as the principal.
 *
 * Install it last in `authentication { }` and list its name **last** in the
 * `authenticate(jwt, apiKey, anon)` block on the route — Ktor's default `FirstSuccessful`
 * strategy stops at the first provider that produces a principal, so registering this one
 * before the credential-bearing providers would short-circuit them.
 *
 * Routes that require real authentication must check
 * [io.peekandpoke.ultra.security.user.User.isAnonymous] themselves.
 */
class AnonymousAuthenticationProvider internal constructor(config: Config) : AuthenticationProvider(config) {

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        context.principal(Caller.AnonymousCaller)
    }

    class Config internal constructor(name: String?) : AuthenticationProvider.Config(name)
}

/**
 * Registers an [AnonymousAuthenticationProvider] under [name] as a terminal fallback.
 *
 * MUST be the last provider in any `authenticate(...)` list — see [AnonymousAuthenticationProvider].
 */
fun AuthenticationConfig.anonymous(name: String? = null) {
    register(AnonymousAuthenticationProvider(AnonymousAuthenticationProvider.Config(name)))
}
