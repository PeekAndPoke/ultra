package de.peekandpoke.ultra.security

import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.security.csrf.CsrfProtection
import de.peekandpoke.ultra.security.csrf.StatelessCsrfProtection
import de.peekandpoke.ultra.security.password.PBKDF2PasswordHasher
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.security.user.UserPermissionsProvider
import de.peekandpoke.ultra.security.user.UserRecordProvider

@Suppress("unused")
fun KontainerBuilder.ultraSecurity(config: UltraSecurityConfig) = module(Ultra_Security, config)

val Ultra_Security = module { config: UltraSecurityConfig ->

    dynamic.factory(UserRecordProvider::class) { UserRecordProvider.anonymous }

    dynamic.factory(UserPermissionsProvider::class) { UserPermissionsProvider.anonymous }

    // Csrf protection
    dynamic(CsrfProtection::class) { userRecordProvider: UserRecordProvider ->
        StatelessCsrfProtection(config.csrfSecret, config.csrfTtlMillis, userRecordProvider)
    }

    singleton.factory(PasswordHasher::class) {
        PBKDF2PasswordHasher()
    }
}
