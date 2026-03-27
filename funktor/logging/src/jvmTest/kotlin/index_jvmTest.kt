package io.peekandpoke.funktor.logging

import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.VaultConfig
import io.peekandpoke.ultra.vault.ultraVault

suspend fun createLoggingTestContainer(
    configureKontainer: KontainerBuilder.() -> Unit,
    configureLogging: FunktorLoggingBuilder.() -> Unit = {},
): Kontainer {
    val kontainer = kontainer {
        ultraLogging()
        ultraVault(VaultConfig.default)

        funktorLogging { configureLogging() }

        configureKontainer()
    }

    return kontainer.create().also {
        it.get(Database::class).ensureRepositories()
    }
}
