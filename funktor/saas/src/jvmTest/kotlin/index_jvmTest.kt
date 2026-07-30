package io.peekandpoke.funktor.saas

import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.VaultConfig
import io.peekandpoke.ultra.vault.ultraVault

/**
 * Boots a kontainer with the saas module plus a DB backend for storage tests.
 *
 * Indexes are recreated so unique constraints (e.g. the organisation slug) are actually enforced.
 */
suspend fun createSaasTestContainer(
    configureKontainer: KontainerBuilder.() -> Unit,
    configureSaas: FunktorSaasBuilder.() -> Unit = {},
): Kontainer {
    val kontainer = kontainer {
        instance(Kronos.systemUtc)
        ultraLogging()
        ultraVault(VaultConfig.default)
        funktorSaas { configureSaas() }
        configureKontainer()
    }

    return kontainer.create().also {
        it.get(Database::class).ensureRepositories()
        it.get(Database::class).recreateIndexes()
    }
}
