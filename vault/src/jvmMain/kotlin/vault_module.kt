package de.peekandpoke.ultra.vault

import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.vault.cli.VaultIndexesEnsureCommand
import de.peekandpoke.ultra.vault.cli.VaultIndexesRecreateCommand
import de.peekandpoke.ultra.vault.cli.VaultIndexesValidateCommand
import de.peekandpoke.ultra.vault.cli.VaultRepositoriesEnsureCommand
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import de.peekandpoke.ultra.vault.hooks.TimestampedMillisHook
import de.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import de.peekandpoke.ultra.vault.profiling.QueryProfiler
import de.peekandpoke.ultra.vault.tools.DatabaseGraphBuilder
import de.peekandpoke.ultra.vault.tools.DatabaseTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun KontainerBuilder.ultraVault() = module(Ultra_Vault)

/**
 * Vault kontainer module.
 *
 * Defines two dynamic services :
 *
 * - [EntityCache] which defaults to [DefaultEntityCache]
 */
val Ultra_Vault
    get() = module {
        // Database
        singleton(Database::class)
        singleton(SharedRepoClassLookup::class)

        // Caching
        dynamic(EntityCache::class, DefaultEntityCache::class)

        // Hooks
        dynamic(TimestampedHook::class)
        dynamic(TimestampedMillisHook::class)

        // Profiling
        dynamic0(QueryProfiler::class) { NullQueryProfiler }

        // Tools
        singleton(DatabaseTools::class)
        dynamic(DatabaseGraphBuilder::class)

        // Cli command
        singleton(VaultRepositoriesEnsureCommand::class)
        singleton(VaultIndexesEnsureCommand::class)
        singleton(VaultIndexesRecreateCommand::class)
        singleton(VaultIndexesValidateCommand::class)
    }

object Vault {
    private val job = SupervisorJob()
    internal val scope = job + Dispatchers.IO

    fun launch(block: suspend () -> Unit) {
        runBlocking {
            launch(scope) {
                block()
            }
        }
    }
}
