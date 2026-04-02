package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.vault.cli.VaultIndexesEnsureCommand
import io.peekandpoke.ultra.vault.cli.VaultIndexesRecreateCommand
import io.peekandpoke.ultra.vault.cli.VaultIndexesValidateCommand
import io.peekandpoke.ultra.vault.cli.VaultRepositoriesEnsureCommand
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.peekandpoke.ultra.vault.hooks.TimestampedMillisHook
import io.peekandpoke.ultra.vault.profiling.DefaultQueryProfiler
import io.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import io.peekandpoke.ultra.vault.profiling.QueryProfiler
import io.peekandpoke.ultra.vault.tools.DatabaseGraphBuilder
import io.peekandpoke.ultra.vault.tools.DatabaseTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun KontainerBuilder.ultraVault(config: VaultConfig) = module(Ultra_Vault, config)

/**
 * Vault kontainer module.
 *
 * Defines two dynamic services :
 *
 * - [EntityCache] which defaults to [DefaultEntityCache]
 */
val Ultra_Vault = module { config: VaultConfig ->
        // Database
        dynamic(Database::class)
        singleton(SharedRepoClassLookup::class)

        // Caching
        dynamic(EntityCache::class, DefaultEntityCache::class)

        // Hooks
        dynamic(TimestampedHook::class)
        dynamic(TimestampedMillisHook::class)

        // Profiling
    dynamic(QueryProfiler::class) {
        if (config.profile) {
            DefaultQueryProfiler(explainQueries = config.explain)
        } else {
            NullQueryProfiler
        }
    }

        // Tools
        singleton(DatabaseTools::class)
        dynamic(DatabaseGraphBuilder::class)

        // Cli command
        singleton(VaultRepositoriesEnsureCommand::class)
        singleton(VaultIndexesEnsureCommand::class)
        singleton(VaultIndexesRecreateCommand::class)
        singleton(VaultIndexesValidateCommand::class)
    }

object VaultScope {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    fun launch(block: suspend () -> Unit) {
        scope.launch {
            block()
        }
    }

    fun shutdown() {
        job.cancel()
    }
}
