package de.peekandpoke.ultra.security

import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.security.csrf.CsrfProtection
import de.peekandpoke.ultra.security.csrf.StatelessCsrfProtection
import de.peekandpoke.ultra.security.password.PBKDF2PasswordHasher
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.security.user.UserProvider

@Suppress("unused")
fun KontainerBuilder.ultraSecurity(config: UltraSecurityConfig) = module(Ultra_Security, config)

val Ultra_Security = module { config: UltraSecurityConfig ->

    dynamic0(UserProvider::class) { UserProvider.anonymous }

    // Csrf protection
    dynamic(CsrfProtection::class) { userProvider: UserProvider ->
        StatelessCsrfProtection(config.csrfSecret, config.csrfTtlMillis, userProvider)
    }

    singleton0(PasswordHasher::class) { PBKDF2PasswordHasher() }
}
