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

/** Registers [Ultra_Vault] with the given [config]. */
fun KontainerBuilder.ultraVault(config: VaultConfig) = module(Ultra_Vault, config)

/**
 * Vault kontainer module.
 *
 * Dynamic — one instance per kontainer, i.e. per request, because they carry per-request state:
 * [Database], [EntityCache] (defaulting to [DefaultEntityCache]), [TimestampedHook],
 * [TimestampedMillisHook], [QueryProfiler] and [DatabaseGraphBuilder].
 *
 * Process-wide singletons, and only because they declare no constructor dependencies:
 * [SharedRepoClassLookup], whose repository lookup cache has to outlive the per-request [Database],
 * and [DeferredVaultHookScope], which is bound to the application scope after the kontainer was
 * built and would be invisible to requests if it were rebuilt for each of them.
 *
 * [DatabaseTools] and the CLI commands are declared as singletons but each reaches a dynamic
 * service, so the kontainer downgrades them to semi-dynamic — again one instance per kontainer.
 * They are stateless, so this costs an allocation and nothing more; nothing may start caching
 * in them.
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

        // CLI commands
        singleton(VaultRepositoriesEnsureCommand::class)
        singleton(VaultIndexesEnsureCommand::class)
        singleton(VaultIndexesRecreateCommand::class)
        singleton(VaultIndexesValidateCommand::class)
    }
