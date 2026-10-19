package io.peekandpoke.funktor.insights.collectors

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.profiling.QueryProfiler

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

        "no bind value reaches the record, by any of the three routes that used to carry it" {
            // `vars` was the field this collector always intended to withhold. `query` carried the same
            // values on Mongo, and `queryExplained` on both backends -- found by the review gate. The
            // third entry is a driver nobody vetted, inlining its value: its text must not survive.
            val data = VaultCollector.Data.of(
                listOf(
                    entry("mongo-bound", MONKO_QUERY),
                    entry("json", OLD_MONKO_QUERY),
                    entry("aql", "FOR d IN c FILTER d.t == @v1"),
                    entry("sql", "SELECT * FROM c WHERE t = '$TOKEN'"),
                )
            )

            val text = codec.slumber(data).toString()

            withClue("the recorded slice must not contain the token anywhere: $text") {
                text shouldNotContain TOKEN
            }
        }

        "only drivers known to keep values out of the text keep it; any other has it dropped" {
            val data = VaultCollector.Data.of(
                listOf(
                    entry("mongo-bound", MONKO_QUERY),
                    entry("json", OLD_MONKO_QUERY),
                    entry("aql", "FOR d IN c FILTER d.t == @v1"),
                    entry("sql", "SELECT * FROM c WHERE t = '$TOKEN'"),
                )
            )

            withClue("monko's own label: placeholders since 2026-10-19 (MonkoDriver.FindQueryBuilder.printQuery)") {
                data.entries[0].query shouldBe MONKO_QUERY
            }
            withClue("monko's OLD label is no longer trusted: an older driver still inlines its values under it") {
                data.entries[1].query shouldBe null
            }
            withClue("aql keeps @placeholders and passes values separately, so its text is safe") {
                data.entries[2].query shouldBe "FOR d IN c FILTER d.t == @v1"
            }
            withClue("fail-closed: a language nobody vetted is suppressed, not trusted") {
                data.entries[3].query shouldBe null
            }
            withClue("the COUNT of bind variables is still useful, and discloses nothing") {
                data.entries[0].varsCount shouldBe 1
            }
        }
    }
}
