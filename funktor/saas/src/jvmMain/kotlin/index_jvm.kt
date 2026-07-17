package io.peekandpoke.funktor.saas

import io.ktor.server.application.Application
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.funktor.saas.storage.karango.KarangoOrgsRepo
import io.peekandpoke.funktor.saas.storage.monko.MonkoOrgsRepo
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

fun KontainerBuilder.funktorSaas(
    builder: FunktorSaasBuilder.() -> Unit = {},
) = module(Funktor_Saas, builder)

inline val KontainerAware.funktorSaas: OrgsStorage get() = kontainer.get()

val Funktor_Saas = module { builder: FunktorSaasBuilder.() -> Unit ->
    // Organisation storage — no-op until a backend is selected
    singleton(OrgsStorage::class, OrgsStorage.Null::class)

    /////////////////////////////////////////////////////////////////////////////////
    // Apply external configuration
    FunktorSaasBuilder(this).apply(builder)
}

class FunktorSaasBuilder internal constructor(private val kontainer: KontainerBuilder) {

    fun useKarango(
        orgsRepoName: String = "system_organisations",
    ) {
        with(kontainer) {
            singleton(KarangoOrgsRepo::class) { driver: KarangoDriver, timestamped: TimestampedHook ->
                KarangoOrgsRepo(
                    driver = driver,
                    timestamped = timestamped,
                    repoName = orgsRepoName,
                )
            }

            singleton(OrgsStorage::class) { repo: KarangoOrgsRepo ->
                OrgsStorage.Vault(repo = repo)
            }
        }
    }

    fun useMonko(
        orgsRepoName: String = "system_organisations",
    ) {
        with(kontainer) {
            singleton(MonkoOrgsRepo::class) { driver: MonkoDriver, timestamped: TimestampedHook ->
                MonkoOrgsRepo(
                    name = orgsRepoName,
                    driver = driver,
                    timestamped = timestamped,
                )
            }

            singleton(OrgsStorage::class) { repo: MonkoOrgsRepo ->
                OrgsStorage.Vault(repo = repo)
            }
        }
    }

    /**
     * Ensures an organisation with the given [slug]/[name] exists on every app startup — the
     * single-tenant / default-org story: users then always resolve exactly one org and never see
     * an org picker.
     *
     * Registers an [EnsureOrganisationOnAppStarting] hook. Calling this more than once replaces the
     * previously configured default organisation.
     */
    fun ensureOrganisation(slug: String, name: String) {
        with(kontainer) {
            singleton(EnsureOrganisationOnAppStarting::class) { orgs: OrgsStorage ->
                EnsureOrganisationOnAppStarting(orgs = orgs, slug = slug, name = name)
            }
        }
    }
}

/**
 * Startup hook that idempotently ensures a default organisation exists (see
 * [FunktorSaasBuilder.ensureOrganisation]). Runs at [AppLifeCycleHooks.ExecutionOrder.Normal], i.e.
 * after the repositories have been ensured.
 */
class EnsureOrganisationOnAppStarting(
    private val orgs: OrgsStorage,
    private val slug: String,
    private val name: String,
) : AppLifeCycleHooks.OnAppStarting {
    override suspend fun onAppStarting(application: Application) {
        orgs.ensureBySlug(slug = slug, name = name)
    }
}
