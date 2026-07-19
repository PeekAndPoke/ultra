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

        // Runs after-save / after-delete hooks. Injectable as VaultHookScope as well.
        // Stays inline until something binds an application scope to it.
        singleton(DeferredVaultHookScope::class)

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
