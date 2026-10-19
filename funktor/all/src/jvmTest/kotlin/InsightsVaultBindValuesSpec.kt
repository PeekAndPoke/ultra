package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.insights.InsightsFileRepository
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.funktor.insights.api.InsightsApi
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.funktor.insights.collectors.VaultCollector
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.security.jwt.JwtGenerator
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.VaultConfig
import io.peekandpoke.ultra.vault.firstOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.io.path.createTempDirectory
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

//  A session-token lookup on each backend, the shape of `findByToken`  ///////////////////////////////

@Vault
data class S1KaSession(val token: String)

@Vault
data class S1MoSession(val token: String)

class S1KaSessionsRepo(driver: KarangoDriver) : EntityRepository<S1KaSession>(
    name = "insights_s1_ka_sessions",
    storedType = kType(),
    driver = driver,
) {
    suspend fun findByToken(token: String): Stored<S1KaSession>? = findFirst {
        FOR(repo) { r ->
            FILTER(r.token EQ token)
            LIMIT(1)
            RETURN(r)
        }
    }
}

class S1MoSessionsRepo(driver: MonkoDriver) : MonkoRepository<S1MoSession>(
    name = "insights_s1_mo_sessions",
    storedType = kType(),
    driver = driver,
) {
    suspend fun findByToken(token: String): Stored<S1MoSession>? = find { r ->
        filter(r.token eq token)
        limit(1)
    }.firstOrNull()
}

/** Looks up [S1ProbeApi.token] in both repositories, so the request's record holds one query per backend. */
class S1ProbeApi : ApiRoutes("insights-s1-probe", authFloor = { authenticated() }) {

    companion object {
        /** Seeded per JVM, so a value from an earlier run can never make the leak check pass by accident. */
        val token: String = "s1-session-token-${UUID.randomUUID()}"

        val Probe = Get(uri = "/_/e2e/insights-s1/probe", response = Int.serializer().api())
    }

    val probe = Probe.mount {
        docs { name = "S1 probe" }.handle {
            val kontainer = call.kontainer
            val found = listOfNotNull(
                kontainer.get(S1KaSessionsRepo::class).findByToken(token),
                kontainer.get(S1MoSessionsRepo::class).findByToken(token),
            )

            ApiResponse.ok(found.size)
        }
    }
}

class S1ProbeFeature : ApiFeature {
    val api = S1ProbeApi()
    override val name = "insights-s1-probe"
    override val description = "Looks a session token up on both backends, with profiling on"
    override fun getRouteGroups() = listOf(api)
}

/** One depot for this spec alone, so the records in it are exactly the probe's. */
private val s1DepotDir: String by lazy {
    createTempDirectory("funktor-all-insights-s1").toFile()
        .also { dir -> Runtime.getRuntime().addShutdownHook(Thread { dir.deleteRecursively() }) }
        .absolutePath
}

private fun createS1Blueprint(config: FunktorAllTestConfig) = kontainer {
    // Profiling AND explain on: explain is the second route a bind value used to take (S2).
    val profiled = config.copy(funktor = config.funktor.copy(vault = VaultConfig(profile = true, explain = true)))

    funktor(
        config = profiled,
        rest = { jwt() },
        logging = { useKarango() },
        cluster = { useKarango() },
        messaging = { useKarango() },
        saas = { useKarango() },
        auth = { useKarango() },
    )

    karango(config = ArangoDbConfig.forUnitTests)
    monko(config = MongoDbConfig.forUnitTests)

    dynamic(InsightsRepository::class) { InsightsFileRepository(s1DepotDir) }

    module(TestUserModule)

    singleton(S1ProbeFeature::class)
    dynamic(S1KaSessionsRepo::class)
    dynamic(S1MoSessionsRepo::class)
}

private val s1TestApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config -> AppKontainers(createS1Blueprint(config)) },
)

/**
 * No bind value reaches an insights record, on either backend -- S1 of the 2026-08-24 review gate.
 *
 * `MonkoDriver` used to record the filter WITH its values, so a session-token lookup wrote the live
 * token into the record. It now records `@pN` placeholders, and `VaultCollector` keeps that text.
 */
class InsightsVaultBindValuesSpec : AppSpec<FunktorAllTestConfig>(s1TestApp) {

    private val insightsApi by service(InsightsApiFeature::class)
    private val probeFeature by service(S1ProbeFeature::class)
    private val kaSessions by service(S1KaSessionsRepo::class)
    private val moSessions by service(S1MoSessionsRepo::class)
    private val jwt by service(JwtGenerator::class)

    private val superUser: String by lazy {
        jwt.createJwt(
            user = JwtUserData(
                id = UserId("s1-e2e"),
                desc = "s1 e2e",
                type = TestUser.USER_TYPE,
                email = EmailAddress("s1-e2e@test.com"),
            ),
            permissions = UserPermissions(isSuperUser = true),
        )
    }

    /** The record's body as the API serves it, cached: one probe request for the whole spec. */
    private var served: String? = null

    private suspend fun listRecords(): List<InsightsRecordSummary> {
        var items: List<InsightsRecordSummary> = emptyList()

        testApp(host = "insights.funktor.local").apply {
            authenticate(superUser) {
                request(insightsApi.insights.listRecords, InsightsApiFeature.PagingParam(page = 1, epp = InsightsApi.MAX_EPP)) {
                    status shouldBe HttpStatusCode.OK
                    items = apiResponseData<Paged<InsightsRecordSummary>>()?.items ?: emptyList()
                }
            }
        }

        return items
    }

    private suspend fun probeRecord(): String = served ?: run {
        kaSessions.insert(S1KaSession(token = S1ProbeApi.token))
        moSessions.insert(S1MoSession(token = S1ProbeApi.token))

        val before = listRecords().size

        testApp(host = "insights.funktor.local").apply {
            authenticate(superUser) {
                request(probeFeature.api.probe) {
                    status shouldBe HttpStatusCode.OK

                    withClue("both lookups must FIND the session: placeholders in the record, values in the query") {
                        Json.parseToJsonElement(bodyAsText()).jsonObject["data"]!!.jsonPrimitive.int shouldBe 2
                    }
                }
            }
        }

        var found: List<InsightsRecordSummary> = emptyList()

        withTimeoutOrNull(10.seconds) {
            while (found.size <= before) {
                found = listRecords()
                if (found.size <= before) delay(25.milliseconds)
            }
        }

        withClue("no NEW insights record appeared within 10s (had $before) -- was the vault slice slumberable?") {
            (found.size > before) shouldBe true
        }

        val ref = found.first().ref
        var body = ""

        testApp(host = "insights.funktor.local").apply {
            authenticate(superUser) {
                request(insightsApi.insights.getRecord, InsightsApiFeature.RecordParam(ref.bucket, ref.file)) {
                    status shouldBe HttpStatusCode.OK
                    body = bodyAsText()
                }
            }
        }

        // opt-in, like the other insights specs: a real record with queries on both backends
        insightsWireSample("record-response-vault-two-backends.json", body)

        body.also { served = it }
    }

    private fun vaultEntries(body: String): List<JsonObject> =
        Json.parseToJsonElement(body).jsonObject["data"]!!.jsonObject["collectors"]!!.jsonArray
            .map { it.jsonObject }
            .single { it["key"]!!.jsonPrimitive.content == VaultCollector.KEY }["data"]!!.jsonObject["entries"]!!
            .jsonArray.map { it.jsonObject }

    /** This backend's token lookup: the entry of [language] whose query text names the `token` field. */
    private fun lookup(body: String, language: String): JsonObject {
        val entries = vaultEntries(body).filter { it["queryLanguage"]?.jsonPrimitive?.content == language }

        return withClue("no $language entry names the token field, among ${entries.size}: $entries") {
            entries.firstOrNull { (it["query"]?.jsonPrimitive?.content ?: "").contains("token") }.shouldNotBeNull()
        }
    }

    init {
        "the token appears NOWHERE in the record, on either backend" {
            val body = probeRecord()

            withClue("vars, query text and the explain plan were all routes a bind value took") {
                body shouldNotContain S1ProbeApi.token
            }
        }

        "the Mongo lookup is recorded as placeholder text, with the value counted" {
            val entry = lookup(probeRecord(), language = "json")
            val query = entry["query"]!!.jsonPrimitive.content

            withClue(query) {
                query shouldContain "\"token\": \"@p1\""
            }
            entry["varsCount"]!!.jsonPrimitive.int shouldBeGreaterThanOrEqual 1
            entry["connection"]!!.jsonPrimitive.content shouldContain "MongoDB"
        }

        "the Arango lookup is recorded as placeholder text, with the value counted" {
            val entry = lookup(probeRecord(), language = "aql")
            val query = entry["query"]!!.jsonPrimitive.content

            withClue(query) {
                query shouldContain "@"
            }
            entry["varsCount"]!!.jsonPrimitive.int shouldBeGreaterThanOrEqual 1
        }

        "no entry carries the fields that used to hold values" {
            val entries = vaultEntries(probeRecord())

            entries.shouldNotBeEmpty()
            entries.forEach { entry ->
                withClue(entry) {
                    entry.containsKey("vars") shouldBe false
                    entry.containsKey("queryExplained") shouldBe false
                }
            }
        }
    }
}
