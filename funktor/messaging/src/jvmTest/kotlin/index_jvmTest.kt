package io.peekandpoke.funktor.messaging

import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.VaultConfig
import io.peekandpoke.ultra.vault.ultraVault

suspend fun createMessagingTestContainer(
    configureKontainer: KontainerBuilder.() -> Unit,
    configureMessaging: FunktorMessagingBuilder.() -> Unit = {},
): Kontainer {
    val kontainer = kontainer {
        instance(Kronos.systemUtc)

        ultraLogging()
        ultraVault(VaultConfig.default)

        funktorMessaging { configureMessaging() }

        configureKontainer()
    }

    return kontainer.create().also {
        it.get(Database::class).ensureRepositories()
    }
}
