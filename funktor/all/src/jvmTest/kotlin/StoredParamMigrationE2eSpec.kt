package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.core.AppKontainers
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

//  Plain (org-agnostic) entities with read-counting repos  /////////////////////////////////////////
//  These prove the part-4 entity-binding param migration in isolation: a `Stored<T>` route param
//  resolves the entity in the converter (not the handler), independent of any org semantics.

@Vault
data class KaThing(val name: String)

class KaThingsRepo(driver: KarangoDriver) : EntityRepository<KaThing>(
    name = "stored_param_e2e_ka_things",
    storedType = kType(),
    driver = driver,
) {
    override suspend fun findById(id: String?): Stored<KaThing>? {
        findByIdCount.incrementAndGet()
        return super.findById(id)
    }

    companion object {
        val findByIdCount = AtomicInteger(0)
    }
}

@Vault
data class MoThing(val name: String)

class MoThingsRepo(driver: MonkoDriver) : MonkoRepository<MoThing>(
    name = "stored_param_e2e_mo_things",
    storedType = kType(),
    driver = driver,
) {
    override suspend fun findById(id: String?): Stored<MoThing>? {
        findByIdCount.incrementAndGet()
        return super.findById(id)
    }

    companion object {
        val findByIdCount = AtomicInteger(0)
    }
}

//  Route params — the migrated shape: the id segment binds a Stored<Entity>  ///////////////////////

data class KaThingParams(val id: Stored<KaThing>)
data class MoThingParams(val id: Stored<MoThing>)

@Serializable
data class ThingUpdate(val note: String)

// `isSuperUser()` is a caller-only (phase-1) floor, so a denied caller never reaches param conversion
// — the binding's findById never runs. This mirrors the migrated admin routes (OrgsApi / conf writes).
class StoredParamMigrationE2eApi : ApiRoutes("stored-param-e2e", authFloor = { isSuperUser() }) {
    // Read shape (WithParams).
    val kaGet = route {
        get<KaThingParams, ApiResponse<String>>("/e2e/sp/ka/things/{id}").handle { ApiResponse.ok(it.id.value.name) }
    }
    val moGet = route {
        get<MoThingParams, ApiResponse<String>>("/e2e/sp/mo/things/{id}").handle { ApiResponse.ok(it.id.value.name) }
    }

    // Write shape (WithBodyAndParams): proves the floor still gates before the entity binding when a
    // body is present, and that the bound entity loads exactly once (no double-load) on the write path.
    val kaUpdate = route {
        put<KaThingParams, ThingUpdate, ApiResponse<String>>("/e2e/sp/ka/things/{id}").handle { params, _ ->
            ApiResponse.ok(params.id.value.name)
        }
    }
    val moUpdate = route {
        put<MoThingParams, ThingUpdate, ApiResponse<String>>("/e2e/sp/mo/things/{id}").handle { params, _ ->
            ApiResponse.ok(params.id.value.name)
        }
    }
}

class StoredParamMigrationE2eFeature : ApiFeature {
    private val api = StoredParamMigrationE2eApi()
    override val name = "stored-param-e2e"
    override val description = "Part-4 e2e: entity-binding Stored<T> route-param migration"
    override fun getRouteGroups() = listOf(api)
}

val StoredParamMigrationE2eModule = module {
    singleton(StoredParamMigrationE2eFeature::class)
    dynamic(KaThingsRepo::class)
    dynamic(MoThingsRepo::class)
}

private fun createStoredParamBlueprint(config: FunktorAllTestConfig) = kontainer {
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
    module(StoredParamMigrationE2eModule)
}

private val storedParamTestApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config -> AppKontainers(createStoredParamBlueprint(config)) },
)

/**
 * End-to-end proof of the part-4 migration (in-handler `findById` → `Stored<T>` route param), run
 * against BOTH DB backends (Karango + Monko). The route has no org semantics — it isolates the
 * entity-binding param behaviour that OrgsApi / the conf endpoints now rely on. Per backend:
 * - **Ordering / oracle**: an anonymous caller (denied by the caller-only `isSuperUser()` floor in
 *   phase 1) gets an identical 401 for an EXISTING and a NON-EXISTING id, with NO repository read —
 *   the entity is never loaded before auth, so there is no timing/existence oracle.
 * - **Happy path**: a super-user gets 200 for a real id (the entity is bound and read in the handler).
 * - **Envelope parity**: a super-user gets 404 for a missing id — the binding's `NotFoundException`
 *   surfaces as the SAME 404 the old hand-written `okOrNotFound(null)` / `notFound()` produced.
 */
class StoredParamMigrationE2eSpec : AppSpec<FunktorAllTestConfig>(storedParamTestApp) {

    private val kaRepo by service(KaThingsRepo::class)
    private val moRepo by service(MoThingsRepo::class)
    private val jwt by service(JwtGenerator::class)

    private fun superUserToken(): String = jwt.createJwt(
        user = JwtUserData(id = "e2e-su", desc = "e2e", type = TestUser.USER_TYPE, email = "e2e-su@test.com"),
        permissions = UserPermissions(isSuperUser = true),
    )

    private suspend fun seed(backend: String, name: String): String = when (backend) {
        "karango" -> kaRepo.insert(KaThing(name = name))._key
        else -> moRepo.insert(MoThing(name = name))._key
    }

    init {
        val backends = listOf(
            Triple("karango", "/e2e/sp/ka", KaThingsRepo.findByIdCount),
            Triple("monko", "/e2e/sp/mo", MoThingsRepo.findByIdCount),
        )

        backends.forEach { (backend, prefix, counter) ->

            "$backend: anonymous → identical 401 for existing and missing ids, with NO pre-auth read (oracle)" {
                val id = seed(backend, "thing")
                counter.set(0)

                apiApp {
                    anonymous {
                        request(HttpMethod.Get, "$prefix/things/$id") {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                        request(HttpMethod.Get, "$prefix/things/does-not-exist") {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }

                // Denied in phase 1 before conversion — the entity-binding findById never runs.
                counter.get() shouldBe 0
            }

            "$backend: super-user + real id → 200, body is the bound entity, loaded exactly once" {
                val id = seed(backend, "hello")
                counter.set(0)

                apiApp {
                    authenticate(superUserToken()) {
                        request(HttpMethod.Get, "$prefix/things/$id") {
                            status shouldBe HttpStatusCode.OK
                            // The handler returns `it.id.value.name` — proves the converter bound the
                            // CORRECT entity, not merely that some 200 came back.
                            apiResponseData<String>() shouldBe "hello"
                        }
                    }
                }

                // Exactly one load: the binding's findById, with NO handler-side re-read — this is the
                // double-load the migration removes. A reintroduced handler findById would make it 2.
                counter.get() shouldBe 1
            }

            "$backend: super-user + missing id → 404 at the binding (envelope parity)" {
                apiApp {
                    authenticate(superUserToken()) {
                        request(HttpMethod.Get, "$prefix/things/unknown-id") {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "$backend: anonymous PUT (write shape) → 401 with NO pre-auth read (ordering on body+params)" {
                val id = seed(backend, "thing")
                counter.set(0)

                apiApp {
                    anonymous {
                        request(
                            HttpMethod.Put,
                            "$prefix/things/$id",
                            setup = {
                                setBody("""{"note":"x"}""")
                                header(HttpHeaders.ContentType, "application/json")
                            },
                        ) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }

                // The caller-only floor denies in phase 1 — neither the body nor the entity binding is reached.
                counter.get() shouldBe 0
            }

            "$backend: super-user PUT (write shape) + real id → 200, entity loaded exactly once" {
                val id = seed(backend, "world")
                counter.set(0)

                apiApp {
                    authenticate(superUserToken()) {
                        request(
                            HttpMethod.Put,
                            "$prefix/things/$id",
                            setup = {
                                setBody("""{"note":"x"}""")
                                header(HttpHeaders.ContentType, "application/json")
                            },
                        ) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<String>() shouldBe "world"
                        }
                    }
                }

                counter.get() shouldBe 1
            }
        }
    }
}
