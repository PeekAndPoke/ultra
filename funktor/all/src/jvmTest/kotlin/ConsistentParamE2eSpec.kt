package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.broker.ConsistentParam
import io.peekandpoke.funktor.core.broker.OrgScopedParam
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes
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
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.value
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger

//  Entities + read-counting repositories  /////////////////////////////////////////////////////////

@Vault
@Serializable
data class KaWidget(val orgId: String, val name: String)

class KaWidgetsRepo(driver: KarangoDriver) : EntityRepository<KaWidget>(
    name = "cp_e2e_ka_widgets",
    storedType = kType(),
    driver = driver,
) {
    // Counts findById calls globally (companion) so it survives the request-scoped repo instances.
    override suspend fun findById(id: String?): Stored<KaWidget>? {
        findByIdCount.incrementAndGet()
        return super.findById(id)
    }

    companion object {
        val findByIdCount = AtomicInteger(0)
    }
}

@Vault
@Serializable
data class MoWidget(val orgId: String, val name: String)

class MoWidgetsRepo(driver: MonkoDriver) : MonkoRepository<MoWidget>(
    name = "cp_e2e_mo_widgets",
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

//  Route params  //////////////////////////////////////////////////////////////////////////////////

/** Single entity — the oracle route. No consistency possible with one entity. */
data class KaWidgetParams(val w: Stored<KaWidget>)
data class MoWidgetParams(val w: Stored<MoWidget>)

/** Two entities — must be referentially consistent AND the caller must own the org. */
data class KaPairParams(val a: Stored<KaWidget>, val b: Stored<KaWidget>) : ConsistentParam, OrgScopedParam {
    override val orgId: String get() = a.value.orgId
    override fun isConsistent(): Boolean = a.value.orgId == b.value.orgId
}

data class MoPairParams(val a: Stored<MoWidget>, val b: Stored<MoWidget>) : ConsistentParam, OrgScopedParam {
    override val orgId: String get() = a.value.orgId
    override fun isConsistent(): Boolean = a.value.orgId == b.value.orgId
}

//  Feature — one group flooring authenticated(), routes per backend  //////////////////////////////

class ConsistentParamE2eApi : ApiRoutes("cp-e2e", defaultAuth = { authenticated() }) {

    // Handlers only read the ALREADY-resolved params (the entity was loaded by the converter),
    // so the group needs no repo dependency.
    val kaGet = route {
        get<KaWidgetParams, ApiResponse<String>>("/e2e/ka/widgets/{w}").handle { ApiResponse.ok(it.w.value.name) }
    }
    val kaPair = route {
        get<KaPairParams, ApiResponse<String>>("/e2e/ka/pairs/{a}/{b}").handle { ApiResponse.ok(it.a.value.name) }
    }
    val moGet = route {
        get<MoWidgetParams, ApiResponse<String>>("/e2e/mo/widgets/{w}").handle { ApiResponse.ok(it.w.value.name) }
    }
    val moPair = route {
        get<MoPairParams, ApiResponse<String>>("/e2e/mo/pairs/{a}/{b}").handle { ApiResponse.ok(it.a.value.name) }
    }
    // NOTE: no SSE route here — the ktor SSE plugin is not installed in any funktor app, so SSE
    // routes are currently unmountable. The SSE dispatch reuses the same phase1Denials /
    // checkParamPhase logic the HTTP paths use (unit-tested in AuthPhaseSpec); see the NOTE on
    // routing.kt `handleSse` for the within-session delivery consideration to validate if SSE is adopted.
}

class ConsistentParamE2eFeature : ApiFeature {
    private val api = ConsistentParamE2eApi()
    override val name = "cp-e2e"
    override val description = "Part-3 e2e: two-phase auth + ConsistentParam + caller-binding"
    override fun getRouteGroups() = listOf(api)
}

val ConsistentParamE2eModule = module {
    singleton(ConsistentParamE2eFeature::class)
    dynamic(KaWidgetsRepo::class)
    dynamic(MoWidgetsRepo::class)
}

//  A dedicated app so the Mongo driver is scoped to THIS spec (not imposed on other funktor:all specs)

private fun createConsistentParamBlueprint(config: FunktorAllTestConfig) = kontainer {
    funktor(
        config = config,
        rest = { jwt() },
        logging = { useKarango() },
        cluster = { useKarango() },
        messaging = { useKarango() },
        saas = {
            useKarango()
            ensureOrganisation(slug = "system-default", name = "System Default")
        },
        auth = { useKarango() },
    )

    karango(config = ArangoDbConfig.forUnitTests)
    monko(config = MongoDbConfig.forUnitTests)

    module(TestUserModule)
    module(ConsistentParamE2eModule)
}

private val consistentParamTestApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config -> AppKontainers(createConsistentParamBlueprint(config)) },
)

/**
 * End-to-end proof of part 3 (two-phase auth + ConsistentParam + caller-binding), run against BOTH
 * DB backends (Karango + Monko). The auth logic is backend-independent; the entity load (`findById`)
 * is the only backend-touching step, exercised here on each.
 *
 * Covered per backend:
 * - **Oracle**: an anonymous caller to a protected `Stored`-param route gets an identical 401 for an
 *   EXISTING and a NON-EXISTING id, and NO repository read happens (phase-1 floor denies before
 *   conversion).
 * - **Not-found parity**: an authenticated caller gets 200 for a real id, 404 for an unknown id.
 * - **Consistency**: an inconsistent entity pair answers 404 (identical to not-found), a consistent
 *   pair 200.
 * - **Caller-binding**: a consistent pair the caller's org cannot access answers 404.
 */
class ConsistentParamE2eSpec : AppSpec<FunktorAllTestConfig>(consistentParamTestApp) {

    private val kaRepo by service(KaWidgetsRepo::class)
    private val moRepo by service(MoWidgetsRepo::class)
    private val jwt by service(JwtGenerator::class)

    private fun token(permissions: UserPermissions): String = jwt.createJwt(
        user = JwtUserData(id = "e2e-user", desc = "e2e", type = TestUser.USER_TYPE, email = "e2e@test.com"),
        permissions = permissions,
    )

    private suspend fun seed(backend: String, orgId: String, name: String): String = when (backend) {
        "karango" -> kaRepo.insert(KaWidget(orgId = orgId, name = name))._key
        else -> moRepo.insert(MoWidget(orgId = orgId, name = name))._key
    }

    init {
        val backends = listOf(
            Triple("karango", "/e2e/ka", KaWidgetsRepo.findByIdCount),
            Triple("monko", "/e2e/mo", MoWidgetsRepo.findByIdCount),
        )

        backends.forEach { (backend, prefix, counter) ->

            "$backend: anonymous → identical 401 for existing and missing ids, with NO repo read (oracle)" {
                val existing = seed(backend, orgId = "acme", name = "oracle")
                counter.set(0)

                apiApp {
                    anonymous {
                        request(HttpMethod.Get, "$prefix/widgets/$existing") {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                        request(HttpMethod.Get, "$prefix/widgets/does-not-exist-xyz") {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }

                // The floor denied in phase 1, BEFORE conversion — the converter never ran findById.
                // count == 0 IS the byte-identity proof: a response produced without ANY resource
                // lookup cannot encode whether the id exists, so the existing-id and missing-id 401s
                // are necessarily indistinguishable w.r.t. existence. (A raw full-body compare would
                // be wrong here — the 401 body's insights.url echoes the caller's OWN request uri,
                // which differs by input, not by resource state.)
                counter.get() shouldBe 0
            }

            "$backend: authenticated → 200 for a real id, 404 for an unknown id" {
                val existing = seed(backend, orgId = "acme", name = "authed")

                apiApp {
                    authenticate(token(UserPermissions(isSuperUser = true))) {
                        request(HttpMethod.Get, "$prefix/widgets/$existing") {
                            status shouldBe HttpStatusCode.OK
                        }
                        request(HttpMethod.Get, "$prefix/widgets/unknown-id") {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "$backend: inconsistent pair → 404, consistent pair → 200 (ConsistentParam)" {
                val a = seed(backend, orgId = "acme", name = "pair-a")
                val b = seed(backend, orgId = "acme", name = "pair-b")
                val foreign = seed(backend, orgId = "globex", name = "pair-foreign")

                apiApp {
                    authenticate(token(UserPermissions(isSuperUser = true))) {
                        // consistent — both acme
                        request(HttpMethod.Get, "$prefix/pairs/$a/$b") {
                            status shouldBe HttpStatusCode.OK
                        }
                        // inconsistent — acme + globex → hidden as not-found
                        request(HttpMethod.Get, "$prefix/pairs/$a/$foreign") {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "$backend: caller-binding uses the SELECTED org, not accessibleOrgs" {
                val acme1 = seed(backend, orgId = "acme", name = "cb-a1")
                val acme2 = seed(backend, orgId = "acme", name = "cb-a2")
                val globex1 = seed(backend, orgId = "globex", name = "cb-g1")
                val globex2 = seed(backend, orgId = "globex", name = "cb-g2")

                apiApp {
                    // Selected org = acme, but the caller is ALSO a member of globex (accessibleOrgs).
                    // Binding must use the SELECTED org — so globex stays forbidden even though the
                    // caller "can access" it. (Under the old canAccessOrg binding this returned 200.)
                    authenticate(token(UserPermissions(org = "acme", accessibleOrgs = setOf("acme", "globex")))) {
                        // selected acme → consistent acme pair → 200
                        request(HttpMethod.Get, "$prefix/pairs/$acme1/$acme2") {
                            status shouldBe HttpStatusCode.OK
                        }
                        // NOT selected globex (though accessible) → consistent globex pair → 404 (hidden)
                        request(HttpMethod.Get, "$prefix/pairs/$globex1/$globex2") {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

        }
    }
}
