package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.broker.ConsistentParam
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.RouteBootCheck
import io.peekandpoke.funktor.rest.RouteParamsGuard
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.isolation.OrgAware
import io.peekandpoke.funktor.saas.isolation.OrgAwareParam
import io.peekandpoke.funktor.saas.isolation.OrgIsolationBootCheck
import io.peekandpoke.funktor.saas.isolation.OrgIsolationGuard
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.security.jwt.JwtGenerator
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.value
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

//  Org-owned entities + read-counting repositories  ///////////////////////////////////////////////
//  The entity stores the owning org's `_id` as a plain String and exposes OrgAware.org as a COMPUTED
//  Ref — never persisted, never resolved (the guard reads only `_id`), so no Ref-in-@Vault surprises.

@Vault
data class KaWidget(val orgId: String, val name: String) : OrgAware {
    override val org: Ref<Organisation> get() = Ref(orgId) { error("org not resolved in e2e") }
}

class KaWidgetsRepo(driver: KarangoDriver) : EntityRepository<KaWidget>(
    name = "org_iso_e2e_ka_widgets",
    storedType = kType(),
    driver = driver,
) {
    override suspend fun findById(id: String?): Stored<KaWidget>? {
        findByIdCount.incrementAndGet()
        return super.findById(id)
    }

    companion object {
        val findByIdCount = AtomicInteger(0)
    }
}

@Vault
data class MoWidget(val orgId: String, val name: String) : OrgAware {
    override val org: Ref<Organisation> get() = Ref(orgId) { error("org not resolved in e2e") }
}

class MoWidgetsRepo(driver: MonkoDriver) : MonkoRepository<MoWidget>(
    name = "org_iso_e2e_mo_widgets",
    storedType = kType(),
    driver = driver,
) {
    override suspend fun findById(id: String?): Stored<MoWidget>? {
        findByIdCount.incrementAndGet()
        return super.findById(id)
    }

    companion object {
        val findByIdCount = AtomicInteger(0)
    }
}

//  Route params — OrgAwareParam carries the addressed {org}  ///////////////////////////////////////

data class KaWidgetParams(override val org: Stored<Organisation>, val w: Stored<KaWidget>) : OrgAwareParam
data class MoWidgetParams(override val org: Stored<Organisation>, val w: Stored<MoWidget>) : OrgAwareParam

// ALSO opts into ConsistentParam (non-org): the two widgets must share a name. Org-isolation still
// applies (OrgAwareParam); this isolates the opt-in ConsistentParamRule → 404 dispatch path.
data class KaConsistentParams(
    override val org: Stored<Organisation>,
    val a: Stored<KaWidget>,
    val b: Stored<KaWidget>,
) : OrgAwareParam, ConsistentParam {
    override fun isConsistent(): Boolean = a.value.name == b.value.name
}

class OrgIsolationE2eApi : ApiRoutes("org-iso-e2e", authFloor = { authenticated() }) {
    // Handlers only read the already-resolved params; the group needs no repo dependency.
    val kaGet = route {
        get<KaWidgetParams, ApiResponse<String>>("/e2e/ka/orgs/{org}/widgets/{w}").handle { ApiResponse.ok(it.w.value.name) }
    }
    val moGet = route {
        get<MoWidgetParams, ApiResponse<String>>("/e2e/mo/orgs/{org}/widgets/{w}").handle { ApiResponse.ok(it.w.value.name) }
    }
    val kaConsistent = route {
        get<KaConsistentParams, ApiResponse<String>>("/e2e/ka/orgs/{org}/consistent/{a}/{b}").handle { ApiResponse.ok(it.a.value.name) }
    }
}

class OrgIsolationE2eFeature : ApiFeature {
    private val api = OrgIsolationE2eApi()
    override val name = "org-iso-e2e"
    override val description = "Part-3 e2e: org-isolation (OrgAware/OrgAwareParam guard)"
    override fun getRouteGroups() = listOf(api)
}

val OrgIsolationE2eModule = module {
    singleton(OrgIsolationE2eFeature::class)
    dynamic(KaWidgetsRepo::class)
    dynamic(MoWidgetsRepo::class)
}

private fun createOrgIsolationBlueprint(config: FunktorAllTestConfig) = kontainer {
    funktor(
        config = config,
        rest = { jwt() },
        logging = { useKarango() },
        cluster = { useKarango() },
        messaging = { useKarango() },
        saas = { useKarango() },
        auth = { useKarango() },
    )

    karango(config = ArangoDbConfig.forUnitTests)
    monko(config = MongoDbConfig.forUnitTests)

    module(TestUserModule)
    module(OrgIsolationE2eModule)
}

private val orgIsolationTestApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config -> AppKontainers(createOrgIsolationBlueprint(config)) },
)

/**
 * End-to-end proof of part-3 org-isolation, run against BOTH DB backends (Karango + Monko). A
 * `Widget : OrgAware` route is guarded by the saas `OrgIsolationGuard`. Covered per backend:
 * - **Oracle**: an anonymous caller to a protected route gets an identical 401 for an EXISTING and a
 *   NON-EXISTING widget id, with NO repository read (the `authenticated()` floor denies in phase 1).
 * - **Not-found parity**: authed caller in the org gets 200 for a real id, 404 for an unknown id.
 * - **Caller-binding**: a caller whose SELECTED org is not the addressed org gets 404 — even when
 *   the org is in their `accessibleOrgs`.
 * - **Org-consistency**: a widget from a foreign org, spoofed into the caller's own-org url, gets 404.
 */
class OrgIsolationE2eSpec : AppSpec<FunktorAllTestConfig>(orgIsolationTestApp) {

    private val orgs by service(OrgsStorage::class)
    private val kaRepo by service(KaWidgetsRepo::class)
    private val moRepo by service(MoWidgetsRepo::class)
    private val jwt by service(JwtGenerator::class)

    private fun token(permissions: UserPermissions): String = jwt.createJwt(
        user = JwtUserData(id = "e2e-user", desc = "e2e", type = TestUser.USER_TYPE, email = "e2e@test.com"),
        permissions = permissions,
    )

    // Two orgs, seeded once (orgs cleared first so reruns don't collide on slug).
    private val orgPair by lazy {
        runBlocking {
            orgs.clear()
            orgs.create(Organisation(slug = "acme-iso", name = "Acme")) to
                    orgs.create(Organisation(slug = "globex-iso", name = "Globex"))
        }
    }

    private suspend fun seedWidget(backend: String, org: Stored<Organisation>): String = when (backend) {
        "karango" -> kaRepo.insert(KaWidget(orgId = org._id, name = "w"))._key
        else -> moRepo.insert(MoWidget(orgId = org._id, name = "w"))._key
    }

    private suspend fun seedKaWidget(org: Stored<Organisation>, name: String): String =
        kaRepo.insert(KaWidget(orgId = org._id, name = name))._key

    init {
        val backends = listOf(
            Triple("karango", "/e2e/ka", KaWidgetsRepo.findByIdCount),
            Triple("monko", "/e2e/mo", MoWidgetsRepo.findByIdCount),
        )

        backends.forEach { (backend, prefix, counter) ->

            "$backend: anonymous → identical 401 for existing and missing ids, with NO repo read (oracle)" {
                val (acme, _) = orgPair
                val widget = seedWidget(backend, acme)
                counter.set(0)

                apiApp {
                    anonymous {
                        request(HttpMethod.Get, "$prefix/orgs/${acme._key}/widgets/$widget") {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                        request(HttpMethod.Get, "$prefix/orgs/${acme._key}/widgets/does-not-exist") {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }

                // Denied in phase 1 before conversion — no findById distinguishes the two ids.
                counter.get() shouldBe 0
            }

            "$backend: authed caller in the org → 200 for a real id, 404 for an unknown id" {
                val (acme, _) = orgPair
                val widget = seedWidget(backend, acme)

                apiApp {
                    authenticate(token(UserPermissions(org = acme._key))) {
                        request(HttpMethod.Get, "$prefix/orgs/${acme._key}/widgets/$widget") {
                            status shouldBe HttpStatusCode.OK
                        }
                        request(HttpMethod.Get, "$prefix/orgs/${acme._key}/widgets/unknown-id") {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "$backend: caller-binding uses the SELECTED org, not accessibleOrgs" {
                val (acme, globex) = orgPair
                val acmeWidget = seedWidget(backend, acme)
                val globexWidget = seedWidget(backend, globex)

                apiApp {
                    // Selected org = acme, but the caller is ALSO a member of globex (accessibleOrgs).
                    authenticate(token(UserPermissions(org = acme._key, accessibleOrgs = setOf(acme._key, globex._key)))) {
                        // addressing the SELECTED org → 200
                        request(HttpMethod.Get, "$prefix/orgs/${acme._key}/widgets/$acmeWidget") {
                            status shouldBe HttpStatusCode.OK
                        }
                        // addressing globex (accessible but NOT selected) → 404 (caller-binding)
                        request(HttpMethod.Get, "$prefix/orgs/${globex._key}/widgets/$globexWidget") {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "$backend: a foreign org's widget spoofed into the own-org url → 404 (org-consistency)" {
                val (acme, globex) = orgPair
                val globexWidget = seedWidget(backend, globex)

                apiApp {
                    authenticate(token(UserPermissions(org = acme._key))) {
                        // url org = acme (caller-binding passes), but the widget belongs to globex
                        request(HttpMethod.Get, "$prefix/orgs/${acme._key}/widgets/$globexWidget") {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        // Opt-in ConsistentParam (backend-independent dispatch — exercised on karango only): both
        // widgets are same-org (org-isolation passes), so the ConsistentParamRule gates on the name.
        "karango: opt-in ConsistentParam rejects an inconsistent pair (404), accepts a consistent one (200)" {
            val (acme, _) = orgPair
            val same1 = seedKaWidget(acme, "same")
            val same2 = seedKaWidget(acme, "same")
            val diff = seedKaWidget(acme, "different")

            apiApp {
                authenticate(token(UserPermissions(org = acme._key))) {
                    request(HttpMethod.Get, "/e2e/ka/orgs/${acme._key}/consistent/$same1/$same2") {
                        status shouldBe HttpStatusCode.OK
                    }
                    request(HttpMethod.Get, "/e2e/ka/orgs/${acme._key}/consistent/$same1/$diff") {
                        status shouldBe HttpStatusCode.NotFound
                    }
                }
            }
        }

        // Fail-loud wiring: the saas module must actually register the check + guard, else org
        // isolation is silently absent (round-2 security finding). Assert they are in the kontainer.
        "the saas module registers the org-isolation boot check + guard" {
            kontainer.getAll(RouteBootCheck::class).any { it is OrgIsolationBootCheck } shouldBe true
            kontainer.getAll(RouteParamsGuard::class).any { it is OrgIsolationGuard } shouldBe true
        }
    }
}
