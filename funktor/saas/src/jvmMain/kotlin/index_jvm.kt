package io.peekandpoke.funktor.saas

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.RoutingContext
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.saas.api.OrgsApiFeature
import io.peekandpoke.funktor.saas.domain.normalizeSlug
import io.peekandpoke.funktor.saas.isolation.OrgIsolationBootCheck
import io.peekandpoke.funktor.saas.isolation.OrgIsolationGuard
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.funktor.saas.storage.karango.KarangoOrgMembersRepo
import io.peekandpoke.funktor.saas.storage.karango.KarangoOrgsRepo
import io.peekandpoke.funktor.saas.storage.monko.MonkoOrgMembersRepo
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
inline val ApplicationCall.funktorSaas: OrgsStorage get() = kontainer.funktorSaas
inline val RoutingContext.funktorSaas: OrgsStorage get() = call.funktorSaas

inline val KontainerAware.funktorOrgMembers: OrgMembersStorage get() = kontainer.get()
inline val ApplicationCall.funktorOrgMembers: OrgMembersStorage get() = kontainer.funktorOrgMembers
inline val RoutingContext.funktorOrgMembers: OrgMembersStorage get() = call.funktorOrgMembers

val Funktor_Saas = module { builder: FunktorSaasBuilder.() -> Unit ->
    // Organisation storage — no-op until a backend is selected
    singleton(OrgsStorage::class, OrgsStorage.Null::class)

    // Org membership storage (the org↔user collection) — no-op until a backend is selected
    singleton(OrgMembersStorage::class, OrgMembersStorage.Null::class)

    // Api
    singleton(OrgsApiFeature::class)

    // Org-isolation: the boot check (RouteBootCheck) forces OrgAwareParam on org-owned routes; the
    // guard (RouteParamsGuard) enforces caller-binding + entity-org consistency at request time.
    singleton(OrgIsolationBootCheck::class)
    singleton(OrgIsolationGuard::class)

    /////////////////////////////////////////////////////////////////////////////////
    // Apply external configuration
    FunktorSaasBuilder(this).apply(builder)
}

class FunktorSaasBuilder internal constructor(private val kontainer: KontainerBuilder) {

    fun useKarango(
        orgsRepoName: String = "saas_organisations",
        orgMembersRepoName: String = "saas_org_members",
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

            singleton(KarangoOrgsRepo.Fixtures::class)

            singleton(KarangoOrgMembersRepo::class) { driver: KarangoDriver, timestamped: TimestampedHook ->
                KarangoOrgMembersRepo(
                    driver = driver,
                    timestamped = timestamped,
                    repoName = orgMembersRepoName,
                )
            }

            singleton(OrgMembersStorage::class) { repo: KarangoOrgMembersRepo ->
                OrgMembersStorage.Vault(repo = repo)
            }

            singleton(KarangoOrgMembersRepo.Fixtures::class)
        }
    }

    fun useMonko(
        orgsRepoName: String = "saas_organisations",
        orgMembersRepoName: String = "saas_org_members",
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

            singleton(MonkoOrgsRepo.Fixtures::class)

            singleton(MonkoOrgMembersRepo::class) { driver: MonkoDriver, timestamped: TimestampedHook ->
                MonkoOrgMembersRepo(
                    name = orgMembersRepoName,
                    driver = driver,
                    timestamped = timestamped,
                )
            }

            singleton(OrgMembersStorage::class) { repo: MonkoOrgMembersRepo ->
                OrgMembersStorage.Vault(repo = repo)
            }

            singleton(MonkoOrgMembersRepo.Fixtures::class)
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
        val normalized = normalizeSlug(slug)
        require(normalized.isNotBlank()) { "ensureOrganisation requires a non-blank slug" }

        with(kontainer) {
            singleton(EnsureOrganisationOnAppStarting::class) { orgs: OrgsStorage ->
                EnsureOrganisationOnAppStarting(orgs = orgs, slug = normalized, name = name)
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
