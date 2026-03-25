package io.peekandpoke.ultra.security

import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.security.csrf.CsrfProtection
import io.peekandpoke.ultra.security.csrf.StatelessCsrfProtection
import io.peekandpoke.ultra.security.password.CompoundPasswordHasher
import io.peekandpoke.ultra.security.password.PasswordHasher
import io.peekandpoke.ultra.security.user.UserProvider

@Suppress("unused")
fun KontainerBuilder.ultraSecurity(config: UltraSecurityConfig) = module(Ultra_Security, config)

val Ultra_Security = module { config: UltraSecurityConfig ->

    dynamic(UserProvider::class) { UserProvider.anonymous }

    // Csrf protection
    dynamic(CsrfProtection::class) { userProvider: UserProvider ->
        StatelessCsrfProtection(config.csrfSecret, config.csrfTtlMillis, userProvider)
    }

    singleton(PasswordHasher::class) { CompoundPasswordHasher.default }
}
