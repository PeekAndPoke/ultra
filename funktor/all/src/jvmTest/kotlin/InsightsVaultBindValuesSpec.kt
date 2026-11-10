package io.peekandpoke.funktor

import com.mongodb.client.model.Filters
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.db.karango.KarangoAuthRecordsRepo
import io.peekandpoke.funktor.auth.db.monko.MonkoAuthRecordsRepo
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.insights.InsightsFileRepository
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.funktor.insights.api.InsightsApi
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.funktor.insights.collectors.VaultCollector
import io.peekandpoke.funktor.messaging.storage.karango.KarangoSentMessagesRepo
import io.peekandpoke.funktor.messaging.storage.monko.MonkoSentMessagesRepo
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.karango.vault.AqlTypedQuery
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.UltraLogManager
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.model.PagedSearchFilter
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
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.Document
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

    /** Raw AQL with the token inlined as a literal -- what `AqlTypedQuery.of` permits. */
    suspend fun countByRawAql(token: String): Long =
        query(AqlTypedQuery.of(kType<Any>(), "FOR d IN insights_s1_ka_sessions FILTER d.token == \"$token\" RETURN d")).count
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

    /** Embedded-document equality with the token as the KEY: matches nothing; the document is one bind value. */
    suspend fun countByTokenAsKey(token: String): Long = find {
        filter(Filters.eq("token", Document(token, 1)))
    }.count

    /**
     * A document keyed by a password HASH, which starts with `$` like an operator does. The server rejects it as an
     * unknown operator; the record of the rejected query must not carry the hash (ULTRA-00228).
     */
    suspend fun lookupByHashAsKey(hash: String): Result<Long> = runCatching {
        find { filter(Document("token", Document(hash, 1))) }.count
    }

    /** A query the server rejects: `$noSuchOperator` is no operator. */
    suspend fun failingLookup(): Result<Long> = runCatching {
        find { filter(Document("token", Document("\$noSuchOperator", 1))) }.count
    }
}

/** Looks up [S1ProbeApi.token] in both repositories, so the request's record holds one query per backend. */
class S1ProbeApi : ApiRoutes("insights-s1-probe", authFloor = { authenticated() }) {

    companion object {
        /** Seeded per JVM, so a value from an earlier run can never make the leak check pass by accident. */
        val token: String = "s1-session-token-${UUID.randomUUID()}"

        /** A bcrypt-shaped hash, seeded per JVM like [token]: `$2b$10$` and then random text. */
        val hashSecret: String = UUID.randomUUID().toString().replace("-", "")
        val hash: String = "\$2b\$10\$$hashSecret"

        /** What the repositories that hold credentials store and look up: they withhold their values (ULTRA-00171). */
        val credential: String = "s1-credential-${UUID.randomUUID()}"

        val Probe = Get(uri = "/_/e2e/insights-s1/probe", response = Int.serializer().api())
    }

    val probe = Probe.mount {
        docs { name = "S1 probe" }.handle {
            val kontainer = call.kontainer
            val kaSessions = kontainer.get(S1KaSessionsRepo::class)
            val moSessions = kontainer.get(S1MoSessionsRepo::class)
            val found = listOfNotNull(
                kaSessions.findByToken(token),
                moSessions.findByToken(token),
            )

            // After the lookups. The three document lookups print the same text and are told apart by counting.
            check(kaSessions.countByRawAql(token) == 1L)
            check(moSessions.countByTokenAsKey(token) == 0L)
            check(moSessions.failingLookup().isFailure) { "the server accepted \$noSuchOperator" }
            check(moSessions.lookupByHashAsKey(hash).isFailure) { "the server accepted a hash as an operator" }

            // The repositories that hold credentials, wired as an app wires them: Karango's from the kontainer,
            // Monko's built the same way on their own collections. A write and a lookup of the auth records, a
            // lookup of the sent messages: none may leave the credential in the record.
            val realm = RealmId("insights-s1")
            val owner = UserId("insights-s1-owner")
            val monko = kontainer.get(MonkoDriver::class)
            val timestamped = kontainer.get(TimestampedHook::class)
            val kaAuth = kontainer.get(KarangoAuthRecordsRepo::class)
            val moAuth = MonkoAuthRecordsRepo("insights_s1_mo_auth_records", monko, emptyList(), timestamped)

            listOf(kaAuth, moAuth).forEach { auth ->
                auth.insert(AuthRecord.Password(realm = realm, ownerId = owner, token = credential))
                checkNotNull(auth.findByToken(realm, AuthRecord.Password.identifier, credential))
            }

            kontainer.get(KarangoSentMessagesRepo::class).findByRef(setOf(credential), PagedSearchFilter())
            MonkoSentMessagesRepo("insights_s1_mo_sent_messages", monko, timestamped)
                .findByRef(setOf(credential), PagedSearchFilter())

            // Two log lines, so the record's log slice is real too -- for the Vue fixtures. No token in them:
            // a log line IS recorded verbatim, which is the app's own business.
            kontainer.get(UltraLogManager::class).getLogger(S1ProbeApi::class).apply {
                info("S1 probe: looked a session token up on both backends")
                warning("S1 probe: found ${found.size} of 2")
            }

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
 * Bind values in an insights record, on both backends (ULTRA-00171): an ordinary repository's are recorded and
 * shown; a repository that holds credentials (auth records, sent messages) withholds them, by every route.
 *
 * The query TEXT stays placeholder-ised either way (`@pN`, S1 of the 2026-08-24 gate; ULTRA-00228): it is all a
 * withholding repository keeps.
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

    /** The Mongo entries whose filter compares `token` with ONE bound document. */
    private fun boundDocumentLookups(body: String): Int = vaultEntries(body).count {
        it["queryLanguage"]?.jsonPrimitive?.content == "mongo-bound" &&
                (it["query"]?.jsonPrimitive?.content ?: "").contains("\"token\": \"@p1:object\"")
    }

    private fun vaultEntries(body: String): List<JsonObject> =
        Json.parseToJsonElement(body).jsonObject["data"]!!.jsonObject["collectors"]!!.jsonArray
            .map { it.jsonObject }
            .single { it["key"]!!.jsonPrimitive.content == VaultCollector.KEY }["data"]!!.jsonObject["entries"]!!
            .jsonArray.map { it.jsonObject }

    /** The first entry of [language] whose query text contains [text]; by default this backend's token lookup. */
    private fun lookup(body: String, language: String, text: String = "token"): JsonObject {
        val entries = vaultEntries(body).filter { it["queryLanguage"]?.jsonPrimitive?.content == language }

        return withClue("no $language entry contains '$text', among ${entries.size}: $entries") {
            entries.firstOrNull { (it["query"]?.jsonPrimitive?.content ?: "").contains(text) }.shouldNotBeNull()
        }
    }

    /** Every recorded query text: values must never be in one from a placeholder-ising printer. */
    private fun queryTexts(body: String): List<String> = vaultEntries(body)
        .filter { it["queryLanguage"]?.jsonPrimitive?.content in setOf("aql", "mongo-bound") }
        .mapNotNull { it["query"]?.jsonPrimitive?.contentOrNull }

    private fun JsonObject.values(): Map<String, String>? = (this["values"] as? JsonObject)
        ?.mapValues { (_, v) -> v.jsonPrimitive.content }

    init {
        "the repositories that hold credentials leave no trace of one, on either backend (ULTRA-00171)" {
            val body = probeRecord()

            withClue("values, raw text and the explain plan are all routes a credential could take") {
                body shouldNotContain S1ProbeApi.credential
            }
        }

        "their queries are recorded, marked as withholding, with the values counted but not shown" {
            val withheld = vaultEntries(probeRecord()).filter { it["valuesWithheld"]?.jsonPrimitive?.content == "true" }

            withClue("auth records: a write and a lookup on Arango, a lookup on Mongo; sent messages: one lookup each") {
                withheld.size shouldBeGreaterThanOrEqual 5
                withheld.count { it["connection"]!!.jsonPrimitive.content.contains("Arango") } shouldBeGreaterThanOrEqual 3
                withheld.count { it["connection"]!!.jsonPrimitive.content.contains("Mongo") } shouldBeGreaterThanOrEqual 2
            }
            withheld.forEach { entry ->
                withClue(entry) {
                    entry.values().shouldBeNull()
                    entry["explain"]?.jsonPrimitive?.contentOrNull.shouldBeNull()
                    withClue("no EXPLAIN ran at all, not one whose plan was dropped") {
                        entry["measureExplain"]!!.jsonObject["count"]!!.jsonPrimitive.int shouldBe 0
                    }
                    entry["varsCount"]!!.jsonPrimitive.int shouldBeGreaterThanOrEqual 1
                }
            }
        }

        "an ordinary repository's lookup is recorded with its value, on both backends (ULTRA-00171)" {
            val body = probeRecord()
            val mongo = lookup(body, language = "mongo-bound")
            val arango = lookup(body, language = "aql")

            mongo["valuesWithheld"]!!.jsonPrimitive.content shouldBe "false"
            mongo.values() shouldBe mapOf("p1" to "\"${S1ProbeApi.token}\"")

            arango["valuesWithheld"]!!.jsonPrimitive.content shouldBe "false"
            arango.values()!!.values.toList() shouldBe listOf("\"${S1ProbeApi.token}\"")
        }

        "the explain plan of an ordinary query is recorded, values and all" {
            val arango = lookup(probeRecord(), language = "aql")

            withClue("the database fills the plan with the values: why a withholding repository never explains") {
                arango["explain"]!!.jsonPrimitive.content shouldContain S1ProbeApi.token
            }
        }

        "the Mongo lookup is recorded as typed placeholder text under Monko's own label, with the value counted" {
            // `mongo-bound`, not `json`: the collector trusts the label, and `json` is what Monko used while it
            // still inlined values (MonkoDriver.FindQueryBuilder.Printed.LANGUAGE, internal to monko).
            val entry = lookup(probeRecord(), language = "mongo-bound")
            val query = entry["query"]!!.jsonPrimitive.content

            withClue(query) {
                query shouldContain "\"collection\": \"insights_s1_mo_sessions\""
                query shouldContain "\"token\": \"@p1:string\""
            }
            withClue("exactly the token: a wrongly bound limit would make it 2") {
                entry["varsCount"]!!.jsonPrimitive.int shouldBe 1
            }
            entry["connection"]!!.jsonPrimitive.content shouldContain "MongoDB"
        }

        "raw AQL of an ordinary repository is recorded under its own label, its text with the token inlined" {
            val raw = vaultEntries(probeRecord()).filter { it["queryLanguage"]?.jsonPrimitive?.content == "aql-raw" }

            raw.size shouldBe 1
            raw.single()["query"]!!.jsonPrimitive.content shouldContain S1ProbeApi.token
        }

        "a Mongo filter comparing with an embedded document binds it whole -- its keys can be data" {
            val entry = lookup(probeRecord(), language = "mongo-bound", text = "\"token\": \"@p1:object\"")

            withClue("the token was the document's KEY") {
                entry["query"]!!.jsonPrimitive.content shouldNotContain S1ProbeApi.token
            }
            entry["varsCount"]!!.jsonPrimitive.int shouldBe 1
        }

        "a document keyed by a hash that starts with \$ is one value: no query TEXT carries the hash (ULTRA-00228)" {
            val body = probeRecord()

            queryTexts(body).forEach { withClue(it) { it shouldNotContain S1ProbeApi.hashSecret } }

            withClue("an ordinary repository shows it as the value it is") {
                body shouldContain S1ProbeApi.hashSecret
            }
        }

        "a FAILED Mongo query is recorded too; its unknown operator is bound whole, like any document of data (ULTRA-00228)" {
            val body = probeRecord()

            withClue("token-as-key, hash-as-key and \$noSuchOperator: three lookups, each ONE bound document") {
                boundDocumentLookups(body) shouldBe 3
            }
            withClue("only known operators are structure; an unknown one could be a hash") {
                queryTexts(body).forEach { it shouldNotContain "noSuchOperator" }
            }
        }

        "the Arango lookup is recorded as placeholder text, with the value counted" {
            val entry = lookup(probeRecord(), language = "aql")
            val query = entry["query"]!!.jsonPrimitive.content

            withClue(query) {
                query shouldContain "`token` == @v_"
            }
            withClue("exactly the token: Karango inlines LIMIT, so it is the only bind value") {
                entry["varsCount"]!!.jsonPrimitive.int shouldBe 1
            }
        }

        "the probe's log lines are recorded, so the log slice of this record is real" {
            val log = Json.parseToJsonElement(probeRecord()).jsonObject["data"]!!.jsonObject["collectors"]!!.jsonArray
                .map { it.jsonObject }
                .single { it["key"]!!.jsonPrimitive.content == "log" }["data"]!!.jsonObject["entries"]!!.jsonArray

            // In order and complete: `UltraLogManager` hands each appender the event on its own, so the
            // per-request collector is no longer held behind the Karango appender's insert (lost a line, 2026-10-19).
            log.map { it.jsonObject["level"]!!.jsonPrimitive.content } shouldBe listOf("INFO", "WARNING")
        }

        "no entry uses the old keys vars or queryExplained, which the GUI reads as a pre-2026-08 record" {
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
