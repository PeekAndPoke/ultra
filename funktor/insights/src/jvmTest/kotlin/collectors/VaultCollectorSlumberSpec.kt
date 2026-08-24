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
            // values on Mongo, and `queryExplained` on both backends -- found by the review gate.
            val data = VaultCollector.Data.of(
                listOf(
                    entry("json", """{"filter":{"token":"$TOKEN"}}"""),
                    entry("aql", "FOR d IN c FILTER d.t == @v1"),
                )
            )

            val text = codec.slumber(data).toString()

            withClue("the recorded slice must not contain the token anywhere: $text") {
                text shouldNotContain TOKEN
            }
        }

        "a driver that inlines its values has its query text dropped, and one that does not keeps it" {
            val data = VaultCollector.Data.of(
                listOf(
                    entry("json", """{"filter":{"token":"$TOKEN"}}"""),
                    entry("aql", "FOR d IN c FILTER d.t == @v1"),
                )
            )

            withClue("mongo inlines values into the query text, so there is no safe text to keep") {
                data.entries[0].query shouldBe null
            }
            withClue("aql keeps @placeholders and passes values separately, so its text is safe") {
                data.entries[1].query shouldBe "FOR d IN c FILTER d.t == @v1"
            }
            withClue("the COUNT of bind variables is still useful, and discloses nothing") {
                data.entries[0].varsCount shouldBe 1
            }
        }
    }
}
