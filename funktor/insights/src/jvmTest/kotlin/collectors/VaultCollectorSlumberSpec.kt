package io.peekandpoke.funktor.insights.collectors

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.profiling.DefaultQueryProfiler
import io.peekandpoke.ultra.vault.profiling.QueryProfiler
import io.peekandpoke.ultra.vault.profiling.withholdingValues
import java.time.Instant
import java.util.Date

/**
 * The spec that would have caught the defect this DTO exists to fix.
 *
 * `VaultCollector.Data` used to hold `List<QueryProfiler.Entry.Impl>`, which **Slumber has no codec
 * for**: `Entry.Impl` is a plain class with a three-argument constructor, so `BuiltInModule` matches
 * neither its `isData` branch nor its empty-primary-constructor one. The throw happened inside
 * `launch(Dispatchers.IO)` after the response was sent, so it silently dropped the ENTIRE record — and
 * across 1193 real depot records, `vault.entries` was `[]` in every single one.
 *
 * Nothing had ever slumbered this class. That is the lesson worth keeping: the collector had tests, the
 * API had tests, and the one operation the data actually undergoes in production had none.
 */
class VaultCollectorSlumberSpec : FreeSpec() {

    companion object {
        private const val TOKEN = "a-live-session-token"

        /** What `MonkoDriver` records for a token lookup: the value is a placeholder, the token is in `vars`. */
        private val MONKO_QUERY =
            """{"collection": "sessions", "filter": {"token": "@p1:string"}, "sort": null, "limit": 1, "skip": null}"""

        /** What `MonkoDriver` recorded before 2026-10-19, under the label `json`: the value inline. */
        private val OLD_MONKO_QUERY = """{"filter": {"token": "$TOKEN"}, "sort": null, "limit": 1, "skip": null}"""

        /** An entry as a driver really fills it in, values and all. */
        private fun entry(language: String, query: String) = QueryProfiler.Entry.Impl(
            connection = "ArangoDB::test",
            queryLanguage = language,
            query = query,
        ).apply {
            vars = mapOf("v1" to TOKEN)
            count = 3
            totalCount = 10
            queryExplained = """FILTER ((d.`token` == "$TOKEN"))"""
            measureQuery { }
            measureDeserializer { }
        }
    }

    private val codec = Codec.default

    /** Stands in for a driver type with no JSON form of its own, like BSON's `ObjectId`: recorded by `toString`. */
    private data class Token(val value: String)

    init {
        "the slice slumbers at all — the whole point of the DTO" {
            val data = VaultCollector.Data.of(listOf(entry("aql", "FOR d IN c FILTER d.t == @v1")))

            val slumbered = codec.slumber(data)

            withClue("a null result is Slumber saying it has no codec, which is how the record used to vanish") {
                slumbered.shouldNotBeNull()
            }
        }

        "the timings survive, rather than serialising to an empty object" {
            // StopWatch.totalNs and .count are COMPUTED GETTERS over entriesNs, so a codec that emits
            // constructor parameters wrote `{}` for them. Every cell would have read "0.00 ms (0x)"
            // even once the slice became writable — a second defect hiding behind the first.
            val data = VaultCollector.Data.of(listOf(entry("aql", "FOR d IN c")))

            @Suppress("UNCHECKED_CAST")
            val slumbered = codec.slumber(data) as Map<String, Any?>

            @Suppress("UNCHECKED_CAST")
            val entries = slumbered["entries"] as List<Map<String, Any?>>


            entries.size shouldBe 1
            entries[0].containsKey("totalNs") shouldBe true

            @Suppress("UNCHECKED_CAST")
            val measure = entries[0]["measureQuery"] as Map<String, Any?>

            withClue("measureQuery ran once, so its count must be on the wire") {
                measure["count"] shouldBe 1
                measure.containsKey("totalNs") shouldBe true
            }
        }

        "an ordinary repository's entry records its values, its raw text and its plan (ULTRA-00171)" {
            val data = VaultCollector.Data.of(
                listOf(
                    entry("mongo-bound", MONKO_QUERY),
                    entry("aql-raw", "FOR d IN c FILTER d.t == '$TOKEN'"),
                )
            )

            data.entries.forEach { entry ->
                withClue(entry) {
                    entry.valuesWithheld shouldBe false
                    entry.values shouldBe mapOf("v1" to "\"$TOKEN\"")
                    entry.explain shouldBe """FILTER ((d.`token` == "$TOKEN"))"""
                    entry.varsCount shouldBe 1
                }
            }
            data.entries[0].query shouldBe MONKO_QUERY
            data.entries[1].query shouldBe "FOR d IN c FILTER d.t == '$TOKEN'"

            withClue("and it slumbers, values and all") {
                codec.slumber(data).toString() shouldContain TOKEN
            }
        }

        "a repository that withholds its values: no value reaches the record, by any of the three routes" {
            // `vars`, the query text (inline values: `json`, `sql`, `aql-raw`) and the EXPLAIN plan, which the
            // database fills with the values. Through the real profiler, as a withholding driver gets it.
            val profiler = DefaultQueryProfiler(explainQueries = true).withholdingValues()

            withClue("a withholding driver never asks the database to explain") {
                profiler.explainQueries shouldBe false
            }

            val languages = listOf(
                "mongo-bound" to MONKO_QUERY,
                "json" to OLD_MONKO_QUERY,
                "aql" to "FOR d IN c FILTER d.t == @v1",
                "sql" to "SELECT * FROM c WHERE t = '$TOKEN'",
                "aql-raw" to "FOR d IN c FILTER d.t == '$TOKEN'",
            )

            languages.forEach { (language, query) ->
                profiler.profile(connection = "ArangoDB::test", queryLanguage = language, query = query) { entry ->
                    entry.vars = mapOf("v1" to TOKEN)
                    entry.queryExplained = """FILTER ((d.`token` == "$TOKEN"))"""
                }
            }

            val data = VaultCollector.Data.of(profiler.entries.filterIsInstance<QueryProfiler.Entry.Impl>())

            withClue("the recorded slice must not contain the token anywhere: ${codec.slumber(data)}") {
                codec.slumber(data).toString() shouldNotContain TOKEN
            }

            data.entries.size shouldBe languages.size
            data.entries.forEach { entry ->
                withClue(entry) {
                    entry.valuesWithheld shouldBe true
                    entry.values shouldBe null
                    entry.explain shouldBe null
                    entry.varsCount shouldBe 1
                }
            }

            withClue("only text from a placeholder-ising printer survives") {
                data.entries.map { it.query } shouldBe listOf(MONKO_QUERY, null, "FOR d IN c FILTER d.t == @v1", null, null)
            }
        }

        "the profiler entry itself drops the values, so they never sit in memory either" {
            val entry = QueryProfiler.Entry.Impl(connection = "c", queryLanguage = "aql").apply {
                valuesWithheld = true
                vars = mapOf("v1" to TOKEN)
                queryExplained = TOKEN
            }

            entry.vars shouldBe mapOf("v1" to null)
            entry.queryExplained shouldBe null
        }

        "a bind value is recorded as JSON text, whatever type the driver bound" {
            val date = Date.from(Instant.parse("2026-11-10T12:00:00Z"))

            VaultCollector.Data.valueText("a\"b") shouldBe "\"a\\\"b\""
            VaultCollector.Data.valueText(42) shouldBe "42"
            VaultCollector.Data.valueText(true) shouldBe "true"
            VaultCollector.Data.valueText(null) shouldBe "null"
            VaultCollector.Data.valueText(listOf(1, "x")) shouldBe "[1,\"x\"]"
            VaultCollector.Data.valueText(mapOf("k" to mapOf("n" to null))) shouldBe "{\"k\":{\"n\":null}}"
            VaultCollector.Data.valueText(Token("t-1")) shouldBe "\"Token(value=t-1)\""
            VaultCollector.Data.valueText(date) shouldBe "\"2026-11-10T12:00:00Z\""
        }

        "a value JSON has no number for is recorded as text, never thrown: a throw here loses the whole record" {
            VaultCollector.Data.valueText(Double.POSITIVE_INFINITY) shouldBe "\"Infinity\""
            VaultCollector.Data.valueText(Double.NaN) shouldBe "\"NaN\""
            VaultCollector.Data.valueText(Float.NEGATIVE_INFINITY) shouldBe "\"-Infinity\""
            VaultCollector.Data.valueText(mapOf("max" to Double.POSITIVE_INFINITY)) shouldBe "{\"max\":\"Infinity\"}"
            VaultCollector.Data.valueText(1.5) shouldBe "1.5"
        }

        "a long value is cut, and says how long it was" {
            val long = "x".repeat(VaultCollector.Data.MAX_VALUE_CHARS + 10)
            val text = VaultCollector.Data.valueText(long)

            text.length shouldBe VaultCollector.Data.MAX_VALUE_CHARS + " … (${long.length + 2} chars)".length
            text shouldEndWith " … (${long.length + 2} chars)"
        }
    }
}
