package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.core.broker.funktorBroker
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.config.funktor.FunktorConfig
import de.peekandpoke.funktor.messaging.funktorMessaging
import de.peekandpoke.funktor.rest.funktorRest
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.log.ultraLogging
import de.peekandpoke.ultra.security.UltraSecurityConfig
import de.peekandpoke.ultra.security.jwt.JwtConfig
import de.peekandpoke.ultra.security.ultraSecurity
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.VaultConfig
import de.peekandpoke.ultra.vault.ultraVault

val testAppConfig = AppConfig.of(
    funktor = FunktorConfig(
        auth = FunktorConfig.AuthConfig(
            jwt = JwtConfig(
                signingKey = "secret",
                issuer = "issuer",
                audience = "audience",
                permissionsNs = "permissions",
                userNs = "user",
            ),
        )
    ),
)

suspend fun createAuthTestContainer(
    configureKontainer: KontainerBuilder.() -> Unit,
    configureAuth: FunktorAuthBuilder.() -> Unit = {},
): Kontainer {
    val kontainer = kontainer {
        instance(testAppConfig)
        instance(Kronos.systemUtc)

        ultraLogging()
        ultraVault(VaultConfig.default)
        ultraSecurity(UltraSecurityConfig.empty)

        funktorBroker()
        funktorRest(testAppConfig) { jwt() }
        funktorMessaging()

        funktorAuth { configureAuth() }

        configureKontainer()
    }

    return kontainer.create().also {
        it.get(Database::class).ensureRepositories()
    }
}
