package io.peekandpoke.funktor.inspect.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.peekandpoke.funktor.insights.api.InsightsApiClient
import io.peekandpoke.funktor.insights.api.InsightsCollectorSlice
import io.peekandpoke.funktor.insights.api.InsightsDatabaseGraph
import io.peekandpoke.funktor.insights.api.InsightsKontainerGraph
import io.peekandpoke.funktor.insights.api.InsightsRecord
import io.peekandpoke.funktor.insights.api.InsightsRecordRef
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.funktor.insights.api.InsightsServerInfo
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiPaged
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.serialization.json.Json
import org.w3c.dom.Element
import kotlin.js.Date
import kotlin.js.Promise

/** Markup in a recorded value: it must reach the page as text, never as an element. */
private const val ATTACK = "<img src=x onerror=alert(1)>"

private val codec = Json { ignoreUnknownKeys = true }

/** The insights API, faked: the server is the one thing a browser test cannot run. */
private class FakeInsightsServer {
    val calls = mutableListOf<String>()
    val records = mutableMapOf<String, InsightsRecord>()
    var summaries: List<InsightsRecordSummary> = emptyList()
    var kontainerGraph = InsightsKontainerGraph(fingerprint = "fp1", services = emptyList())
    var databaseGraph = InsightsDatabaseGraph(repos = emptyList())
    /** False: the list answers 404, as the API does while insights are off. */
    var listAnswers = true

    private fun answer(path: String): String? = when {
        path == "/_/funktor/insights/records" && !listAnswers -> null

        path == "/_/funktor/insights/records" -> codec.encodeToString(
            InsightsRecordSummary.serializer().apiPaged(),
            ApiResponse.ok(Paged(items = summaries, page = 1, epp = 20, fullItemCount = summaries.size.toLong())),
        )

        path.startsWith("/_/funktor/insights/records/") -> records[path.removePrefix("/_/funktor/insights/records/")]
            ?.let { codec.encodeToString(InsightsRecord.serializer().api(), ApiResponse.ok(it)) }

        path == "/_/funktor/insights/graphs/kontainer" ->
            codec.encodeToString(InsightsKontainerGraph.serializer().api(), ApiResponse.ok(kontainerGraph))

        path == "/_/funktor/insights/graphs/database" ->
            codec.encodeToString(InsightsDatabaseGraph.serializer().api(), ApiResponse.ok(databaseGraph))

        path == "/_/funktor/insights/server" -> codec.encodeToString(
            InsightsServerInfo.serializer().api(),
            ApiResponse.ok(InsightsServerInfo(git = "main-abc123", gitDesc = "v1.2-3-gabc123", applicationId = "demo", environment = "dev")),
        )

        else -> null
    }

    val api = InsightsApiClient(
        ApiClient.Config(
            baseUrl = "http://api.test",
            codec = codec,
            client = HttpClient(
                MockEngine { request ->
                    val path = request.url.encodedPath
                    calls += path

                    when (val body = answer(path)) {
                        // As funktor answers a NotFoundException and `okOrNotFound`: an envelope, no data
                        null -> respond(
                            content = codec.encodeToString(
                                InsightsRecord.serializer().api(),
                                ApiResponse.notFound<InsightsRecord>().withError("The Resource '$path' was not found"),
                            ),
                            status = HttpStatusCode.NotFound,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )

                        else -> respond(
                            content = body,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
            ),
        )
    )

    fun count(path: String) = calls.count { it == path }
}

private fun slice(key: String, json: String) = InsightsCollectorSlice(key = key, data = codec.parseToJsonElement(json))

private fun record(file: String, vararg collectors: InsightsCollectorSlice, previous: String? = null) = InsightsRecord(
    ref = InsightsRecordRef(bucket = "records-2026-11-10", file = file),
    recordedAt = MpInstant.fromEpochMillis(1_794_000_000_000),
    method = "GET",
    path = "/search/$ATTACK",
    status = 200,
    durationMs = 12.5,
    collectors = collectors.toList(),
    next = null,
    previous = previous?.let { InsightsRecordRef(bucket = "records-2026-11-10", file = it) },
)

private val requestSlice = slice(
    "request",
    """{"method": {"value": "GET"}, "scheme": "http", "host": "api.test", "port": 80, "uri": "/search",
       "headers": {"X-Attack": ["$ATTACK"]}, "queryParams": {"q": ["$ATTACK"]}}""",
)

private val responseSlice = slice("response", """{"status": {"value": 200, "description": "OK"}, "headers": {}}""")

private val vaultSlice = slice(
    "vault",
    """{"entries": [
        {"connection": "arango", "count": 1, "totalCount": 1, "query": "FOR u IN users FILTER u.name == @p1 RETURN u",
         "queryLanguage": "aql", "varsCount": 1, "values": {"p1": "\"$ATTACK\""}, "valuesWithheld": false,
         "totalNs": 2000000, "measureQuery": {"totalNs": 1000000, "count": 1}},
        {"connection": "arango", "count": 0, "totalCount": 0, "query": "FOR t IN tokens FILTER t.hash == @p1 RETURN t",
         "queryLanguage": "aql", "varsCount": 1, "valuesWithheld": true, "totalNs": 1000000}
    ]}""",
)

private val kontainerSlice = slice(
    "kontainer",
    """{"graphFingerprint": "fp1", "services": [
        {"cls": "com.example.UserRepo", "type": "Singleton", "instances": [{"cls": "com.example.UserRepo", "createdAt": {"ts": 1794000000000}}]}
    ]}""",
)

private val customSlice = slice("custom", """{"note": "$ATTACK"}""")

private val graph = InsightsKontainerGraph(
    fingerprint = "fp1",
    services = listOf(
        InsightsKontainerGraph.Service(
            cls = "com.example.UserRepo",
            type = "Singleton",
            definition = InsightsKontainerGraph.Definition(
                creates = "com.example.UserRepo",
                injectionType = "Singleton",
                injects = listOf(InsightsKontainerGraph.Injection("db", "Direct", listOf("com.example.Database"))),
                codeLocation = "Module.kt:42",
                overwrites = null,
            ),
        ),
        InsightsKontainerGraph.Service(
            cls = "com.example.Database",
            type = "Singleton",
            definition = InsightsKontainerGraph.Definition(
                creates = "com.example.Database",
                injectionType = "Singleton",
                injects = emptyList(),
                codeLocation = "Module.kt:41",
                overwrites = null,
            ),
        ),
    ),
)

// Host that shows the detail page of whatever record the stream names, in place (the router mounts one per path)
@Suppress("TestFunctionName")
private fun Tag.DetailHost(ui: InsightsUi, ref: StreamSource<InsightsRecordRef>) =
    comp(DetailHost.Props(ui, ref)) { DetailHost(it) }

private class DetailHost(ctx: Ctx<Props>) : Component<DetailHost.Props>(ctx) {
    data class Props(val ui: InsightsUi, val ref: StreamSource<InsightsRecordRef>)

    private val ref by subscribingTo(props.ref)

    override fun VDom.render() {
        div { props.ui { InsightsDetailPage(bucket = ref.bucket, file = ref.file) } }
    }
}

/** Counts `Network.setData` and `setOptions` calls of every vis-network instance while [block] runs. */
private suspend fun countingSetData(block: suspend (calls: () -> Int) -> Unit) {
    val module: dynamic = (js("import('vis-network/standalone')") as Promise<dynamic>).await()
    val prototype = module.Network.prototype
    val originalData = prototype.setData
    val originalOptions = prototype.setOptions
    val counter: dynamic = js("({ calls: 0 })")
    val wrap = js("(function (orig, counter) { return function (d) { counter.calls++; return orig.call(this, d); }; })")

    prototype.setData = wrap(originalData, counter)
    prototype.setOptions = wrap(originalOptions, counter)

    try {
        block { counter.calls.unsafeCast<Int>() }
    } finally {
        prototype.setData = originalData
        prototype.setOptions = originalOptions
    }
}

private suspend fun KQuery<Element>.tab(label: String) {
    eventually { selectCss(".tabular.menu .item").filter { it.textContent?.trim() == label }.size shouldBe 1 }
    selectCss(".tabular.menu .item").filter { it.textContent?.trim() == label }.first().let { KQuery(listOf(it)) }.click()
}

private suspend fun KQuery<Element>.button(label: String) {
    eventually { selectCss(".button").filter { it.textContent?.trim() == label }.size shouldBe 1 }
    KQuery(selectCss(".button").filter { it.textContent?.trim() == label }).click()
}

class InsightsPagesSpec : StringSpec() {

    private suspend fun withDetail(
        server: FakeInsightsServer,
        file: String,
        test: suspend (root: KQuery<Element>, ref: StreamSource<InsightsRecordRef>) -> Unit,
    ) {
        val ui = InsightsUi(api = server.api)
        val ref = StreamSource(InsightsRecordRef(bucket = "records-2026-11-10", file = file))

        TestBed.preact(view = { DetailHost(ui, ref) }) { root ->
            eventually { root.selectCss(".tabular.menu").count() shouldBe 1 }
            test(root, ref)
        }
    }

    init {
        "the list shows the records and the running server; a path is text" {
            val server = FakeInsightsServer()
            server.summaries = listOf(
                InsightsRecordSummary(
                    ref = InsightsRecordRef("records-2026-11-10", "a.json"),
                    recordedAt = MpInstant.fromEpochMillis(1_794_000_000_000),
                    method = "GET",
                    path = "/search/$ATTACK",
                    status = 503,
                    durationMs = 450.0,
                ),
            )

            val ui = InsightsUi(api = server.api)

            TestBed.preact(view = { ui { InsightsListPage() } }) { root ->
                eventually { root.selectCss(".insights-records tbody tr").count() shouldBe 1 }
                eventually { root.textContent() shouldContain "main-abc123 (v1.2-3-gabc123)" }

                root.selectCss(".insights-path").textContent() shouldBe "/search/$ATTACK"
                root.selectCss("img").count() shouldBe 0
                // 5xx and above 300 ms are red
                root.selectCss(".insights-records .red.label").count() shouldBe 2
                root.textContent() shouldContain "Page 1 of 1"
            }
        }

        "the detail page shows the overview and its tabs; recorded markup is text" {
            val server = FakeInsightsServer()
            server.records["records-2026-11-10/a.json"] =
                record("a.json", requestSlice, responseSlice, vaultSlice, customSlice)

            withDetail(server, "a.json") { root, _ ->
                root.textContent() shouldContain "GET /search/$ATTACK"
                root.textContent() shouldContain "200 OK"
                // The Database strip in the Overview: two queries, 3.00 ms in total
                root.textContent() shouldContain "3.00 ms"

                // The first tab is open: the request
                eventually { root.textContent() shouldContain "http://api.test:80/search" }
                root.selectCss(".insights-mono").filter { it.textContent == ATTACK }.size shouldBe 2

                // An app-defined collector: its raw JSON, highlighted, the markup in it as text
                root.tab("custom")
                eventually { root.textContent() shouldNotContain "http://api.test:80/search" }
                eventually { root.selectCss(".bottom.attached.segment code .token.property").textContent() shouldBe "\"note\"" }
                root.selectCss(".bottom.attached.segment code").textContent() shouldContain ATTACK
                root.selectCss("img").count() shouldBe 0
            }
        }

        "the database tab shows values, says when they were withheld, and highlights the query" {
            val server = FakeInsightsServer()
            server.records["records-2026-11-10/a.json"] = record("a.json", vaultSlice)

            withDetail(server, "a.json") { root, _ ->
                eventually { root.selectCss("code .token.keyword").count() shouldBe 8 }

                val text = root.textContent()
                text shouldContain "\"p1\": \"$ATTACK\""
                text shouldContain "Values withheld: this repository holds credentials"
                text shouldContain "had 1 bind variable."
                root.selectCss("img").count() shouldBe 0
            }
        }

        "an unreadable or legacy database slice shows no query and no raw view" {
            val server = FakeInsightsServer()
            server.records["records-2026-11-10/a.json"] = record(
                "a.json",
                slice("vault", """{"entries": "secret-token-1"}"""),
            )
            server.records["records-2026-11-10/b.json"] = record(
                "b.json",
                slice(
                    "vault",
                    """{"entries": [{"query": "FOR t IN tokens FILTER t.hash == 'secret-token-2' RETURN t",
                       "queryExplained": "secret-token-3", "vars": {"p1": "secret-token-4"}, "totalNs": 1}]}""",
                ),
            )

            withDetail(server, "a.json") { root, ref ->
                eventually { root.textContent() shouldContain "This database slice could not be read" }
                root.textContent() shouldNotContain "secret-token-1"

                ref(InsightsRecordRef("records-2026-11-10", "b.json"))

                eventually { root.textContent() shouldContain "Query text is hidden" }
                root.textContent() shouldNotContain "secret-token"
            }
        }

        "the kontainer graph is fetched on the first click, once per UI; the table fills in" {
            val server = FakeInsightsServer()
            server.kontainerGraph = graph
            server.records["records-2026-11-10/a.json"] = record("a.json", kontainerSlice)
            server.records["records-2026-11-10/b.json"] = record("b.json", kontainerSlice)

            withDetail(server, "a.json") { root, ref ->
                eventually { root.textContent() shouldContain "Injects and definitions are not part of this record" }
                server.count("/_/funktor/insights/graphs/kontainer") shouldBe 0

                root.button("Show instances Graph")

                eventually { root.selectCss(".green.active.button").count() shouldBe 1 }
                eventually { server.count("/_/funktor/insights/graphs/kontainer") shouldBe 1 }
                eventually { root.textContent() shouldContain "Module.kt:42" }
                eventually { root.selectCss(".vis-network canvas").count() shouldBe 1 }
                server.count("/_/funktor/insights/graphs/kontainer") shouldBe 1

                // Sorting the table re-renders the tab; the drawn graph must not be handed new data
                countingSetData { calls ->
                    KQuery(root.selectCss("th").filter { it.textContent?.trim() == "Type" }).click()
                    eventually { root.selectCss("th.ascending.sorted").count() shouldBe 1 }
                    calls() shouldBe 0
                }

                // Another record of the same fingerprint: the graph is not asked for again
                ref(InsightsRecordRef("records-2026-11-10", "b.json"))
                eventually { root.textContent() shouldContain "Injects and definitions are not part of this record" }
                root.button("Show Full Graph")
                eventually { root.textContent() shouldContain "Module.kt:42" }
                server.count("/_/funktor/insights/graphs/kontainer") shouldBe 1
            }
        }

        "the database graph is fetched on the first click, once per UI, and kept while the tab re-renders" {
            val server = FakeInsightsServer()
            server.databaseGraph = InsightsDatabaseGraph(
                repos = listOf(
                    InsightsDatabaseGraph.Repo(
                        id = InsightsDatabaseGraph.Repo.Id(name = "users", connection = "arango::demo"),
                        storedClasses = listOf(
                            InsightsDatabaseGraph.StoredClass(
                                fqn = "com.example.User",
                                references = listOf(
                                    InsightsDatabaseGraph.Reference(
                                        repo = InsightsDatabaseGraph.Repo.Id(name = "orgs", connection = "arango::demo"),
                                        fqn = "com.example.Org",
                                    )
                                ),
                            )
                        ),
                    ),
                    InsightsDatabaseGraph.Repo(
                        id = InsightsDatabaseGraph.Repo.Id(name = "orgs", connection = "arango::demo"),
                        storedClasses = emptyList(),
                    ),
                )
            )
            server.records["records-2026-11-10/a.json"] = record("a.json", vaultSlice)
            server.records["records-2026-11-10/b.json"] = record("b.json", vaultSlice)

            withDetail(server, "a.json") { root, ref ->
                eventually { root.textContent() shouldContain "Show Database Graph" }
                server.count("/_/funktor/insights/graphs/database") shouldBe 0

                root.button("Show Database Graph")
                eventually { root.selectCss(".vis-network canvas").count() shouldBe 1 }
                root.textContent() shouldContain "2 nodes, 1 edges."

                countingSetData { calls ->
                    root.button("Database Graph Data")
                    eventually { root.textContent() shouldContain "arango::demo" }
                    calls() shouldBe 0
                }

                // The other record's tab is a new one, with no graph shown yet
                ref(InsightsRecordRef("records-2026-11-10", "b.json"))
                eventually { root.selectCss(".vis-network canvas").count() shouldBe 0 }
                root.button("Show Database Graph")
                eventually { root.selectCss(".vis-network canvas").count() shouldBe 1 }
                server.count("/_/funktor/insights/graphs/database") shouldBe 1
            }
        }

        "older record shapes read: a full kontainer slice with epoch seconds, a Monko json entry, no view" {
            val server = FakeInsightsServer()
            server.records["records-2026-11-10/a.json"] = record(
                "a.json",
                slice(
                    "kontainer",
                    """{"numOld": 0, "numTotal": 1, "info": {"services": [
                        {"cls": {"fqn": "com.example.Clock"}, "type": "Prototype",
                         "definition": {"creates": {"fqn": "com.example.Clock"}, "injectionType": "Prototype",
                                        "injects": [], "codeLocation": {"location": "Clocks.kt:7"}},
                         "instances": [{"cls": {"fqn": "com.example.Clock"}, "createdAt": 1700000000.5}]}
                    ]}}""",
                ),
                slice(
                    "vault",
                    """{"entries": [{"connection": "mongo", "count": 1, "totalCount": 1, "query": null,
                       "queryLanguage": "json", "varsCount": 0, "totalNs": 1000000}]}""",
                ),
                slice("template", """{"timeNs": null}"""),
            )

            withDetail(server, "a.json") { root, _ ->
                eventually { root.textContent() shouldContain "full -- every definition inline" }
                root.textContent() shouldContain "Clocks.kt:7"
                root.textContent() shouldContain "1 / 0 / 1 -- not measured"
                // Epoch SECONDS, as millis: not the year 58,000
                root.textContent() shouldContain Date(1_700_000_000_500.0).toLocaleString()

                root.tab("Database")
                eventually { root.textContent() shouldContain "when Monko still put the bind values into its query text" }
                root.textContent() shouldContain "unknownBind vars"

                root.tab("View")
                eventually { root.textContent() shouldContain "No view was rendered for this request." }
            }
        }

        "headers are sorted by name, one row per name and a line per value; a redacted value is muted" {
            val server = FakeInsightsServer()
            val headers = slice(
                "request",
                """{"method": {"value": "GET"}, "uri": "/", "queryParams": {},
                   "headers": {"b-second": ["2", "3"], "X-Attack": ["$ATTACK"], "A-first": ["$REDACTED"]}}""",
            )
            server.records["records-2026-11-10/a.json"] = record("a.json", headers)

            withDetail(server, "a.json") { root, _ ->
                eventually { root.selectCss(".insights-redacted").count() shouldBe 1 }

                val rows = root.selectCss(".bottom.attached.segment .celled.table tbody tr")
                    .map { row -> row.querySelector("td")?.textContent }
                // localeCompare, as the Vue GUI: not code point order, which puts "X" before "b"
                rows shouldBe listOf("A-first", "b-second", "X-Attack")
                root.selectCss(".insights-redacted").textContent() shouldBe REDACTED
                root.selectCss(".bottom.attached.segment .celled.table tbody tr td div").count() shouldBe 4
            }
        }

        "a missing record, and insights switched off, say what the 404 means" {
            val server = FakeInsightsServer()
            val ui = InsightsUi(api = server.api)
            val ref = StreamSource(InsightsRecordRef(bucket = "records-2026-11-10", file = "gone.json"))

            TestBed.preact(view = { DetailHost(ui, ref) }) { root ->
                eventually { root.textContent() shouldContain "This record does not exist, or insights are not active" }
            }

            // Switched off, the server answers 404 to the list too
            val off = FakeInsightsServer().apply { listAnswers = false }
            val offUi = InsightsUi(api = off.api)

            TestBed.preact(view = { offUi { InsightsListPage() } }) { root ->
                eventually { root.textContent() shouldContain "Insights are not active on this server" }
            }
        }

        "values print with two spaces, a bare word as a string" {
            valuesText(mapOf("a" to "abc", "b" to "{\"x\":[1]}")) shouldBe
                "{\n  \"a\": \"abc\",\n  \"b\": {\n    \"x\": [\n      1\n    ]\n  }\n}"
        }

        "tabs asking for a graph at once share one fetch" {
            val server = FakeInsightsServer()
            val graphs = InsightsGraphSource(server.api)

            val results = coroutineScope {
                listOf(
                    async { graphs.database() },
                    async { graphs.database() },
                    async { graphs.kontainer("fp1") },
                    async { graphs.kontainer("fp1") },
                ).awaitAll()
            }

            results.all { it is GraphResult.Ok } shouldBe true
            server.count("/_/funktor/insights/graphs/database") shouldBe 1
            server.count("/_/funktor/insights/graphs/kontainer") shouldBe 1
        }

        "the page follows a new record" {
            val server = FakeInsightsServer()
            server.records["records-2026-11-10/a.json"] = record("a.json", responseSlice)
            server.records["records-2026-11-10/b.json"] = record("b.json", requestSlice, previous = "a.json")

            withDetail(server, "a.json") { root, ref ->
                eventually { root.selectCss(".tabular.menu .item").textContent() shouldBe "Response" }

                ref(InsightsRecordRef("records-2026-11-10", "b.json"))

                eventually { root.selectCss(".tabular.menu .item").textContent() shouldBe "Request" }
                server.count("/_/funktor/insights/records/records-2026-11-10/b.json") shouldBe 1
            }
        }
    }
}
