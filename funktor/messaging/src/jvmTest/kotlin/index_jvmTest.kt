package io.peekandpoke.funktor.messaging

import io.peekandpoke.funktor.core.config.AppConfig
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
    // Defaults to the "prod" environment, NOT "test": these specs exercise storage, and a test-mode
    // config would have the module swap in a NullEmailSender. Pass a test config explicitly when the
    // suppression itself is what is under test.
    config: AppConfig = AppConfig.of(),
    configureMessaging: FunktorMessagingBuilder.() -> Unit = {},
): Kontainer {
    val kontainer = kontainer {
        instance(Kronos.systemUtc)

        ultraLogging()
        ultraVault(VaultConfig.default)

        funktorMessaging(config) { configureMessaging() }

        configureKontainer()
    }

    return kontainer.create().also {
        it.get(Database::class).ensureRepositories()
    }
}
