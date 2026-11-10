package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.util.Date

/**
 * Database queries run while handling the request.
 *
 * ## Why there is an explicit DTO here
 *
 * This collector used to store `List<QueryProfiler.Entry.Impl>` directly, and **that could never be
 * written**: `Entry.Impl` is a plain class with a three-argument constructor, so `BuiltInModule` has no
 * codec branch for it (not `isData`, not an empty primary constructor) and `SlumberConfig` raised
 * *"no known way to slumber the type"*. That happened inside `launch(Dispatchers.IO)` after the response
 * had been sent, so the failure silently dropped the **entire record**, not merely this slice.
 *
 * Found by the `/feature-review` gate on 2026-08-24 and confirmed across 1193 real depot records:
 * `vault.entries` was `[]` in every single one, on every app that ever ran with profiling on.
 *
 * Two further traps the DTO removes, both of which would have produced a slice full of zeros even once
 * the codec existed:
 *
 * - `StopWatch.Impl.totalNs` and `.count` are **computed getters** over `entriesNs`, so a codec that
 *   emits constructor parameters writes `{}` for them.
 * - `Entry.totalNs` is an interface getter, likewise never emitted.
 *
 * Everything the frontend needs is therefore a constructor parameter of a `data class` below.
 */
class VaultCollector(private val profiler: QueryProfiler) : InsightsCollector {

    override val key = KEY

    companion object {
        /**
         * **The frontend keys its tab on this exact string.** `InsightsDetailPage.vue`'s registry maps
         * `"vault"` to the tab that renders queries and their values; an unregistered key falls through to a raw
         * JSON view. So renaming this does not break a build — it silently swaps a redacting view for a
         * dumping one. Pinned by `VaultCollectorKeySpec`.
         */
        const val KEY = "vault"
    }

    /**
     * One stop watch, flattened to the two numbers a reader wants.
     *
     * A `data class` with both as constructor parameters, precisely because [QueryProfiler.StopWatch]
     * exposes them as computed getters that no codec emits.
     */
    data class Measure(
        val totalNs: Long,
        val count: Int,
    ) {
        companion object {
            fun of(watch: QueryProfiler.StopWatch) = Measure(totalNs = watch.totalNs, count = watch.count)
        }
    }

    /**
     * One profiled query.
     *
     * The bind values are recorded (maintainer, 2026-11-09, ULTRA-00171: "query values: show them again"), as JSON
     * text per name in [values], and so are the raw query text and the EXPLAIN plan, which carry them too. A
     * repository that holds credentials withholds them: its driver comes from `withholdingQueryValues()`, and its
     * entries carry [valuesWithheld], only the [varsCount], and query text only from a placeholder-ising printer.
     */
    data class Entry(
        val connection: String?,
        val queryLanguage: String?,
        /** The query text; null when withheld and the text could hold literals. */
        val query: String?,
        /** How many bind variables the query had. */
        val varsCount: Int,
        /** True when the repository withholds its values: [values] and [explain] are then null. */
        val valuesWithheld: Boolean,
        /** Each bind value as JSON text, by name; each cut at [Data.MAX_VALUE_CHARS]. */
        val values: Map<String, String>?,
        /** The EXPLAIN plan, when the app explains queries (`VaultConfig.explain`). */
        val explain: String?,
        val count: Long?,
        val totalCount: Long?,
        val totalNs: Long,
        val measureQuery: Measure,
        val measureIterator: Measure,
        val measureSerializer: Measure,
        val measureDeserializer: Measure,
        val measureExplain: Measure,
    )

    data class Data(
        val entries: List<Entry>,
    ) : InsightsCollectorData {
        companion object {
            /** The longest bind value recorded, in characters of its JSON text. */
            const val MAX_VALUE_CHARS = 4_000

            /**
             * Query languages whose recorded text is known to keep bind values OUT of it: the only text a
             * repository that withholds its values keeps.
             *
             * `aql`: Karango records under it only the text its query DSL printed, `@placeholders` intact. Raw text
             * (`AqlTypedQuery.of`) is `aql-raw` and may hold literals. `mongo-bound`: `MonkoDriver` records the text
             * of `FindQueryBuilder.printQuery()` under that label, every filter value an `@pN:<type>` placeholder.
             * Monko's old label `json` was the filter WITH its values.
             *
             * **Trusted by the label alone.** Each label names exactly one placeholder-ising printer, and a new
             * one is added here only after its printer is.
             */
            private val PLACEHOLDER_SAFE_LANGUAGES = setOf("aql", "mongo-bound")

            /**
             * Takes [QueryProfiler.Entry.Impl], not the `Entry` interface.
             *
             * `connection` and `queryLanguage` are declared on the implementation only, so the interface
             * cannot supply them — and widening `ultra:vault`'s public interface to suit one consumer is
             * the wrong direction. The previous version filtered the same way; this keeps that, which
             * also excludes `Entry.Null`.
             */
            fun of(entries: List<QueryProfiler.Entry.Impl>) = Data(
                entries = entries.map { entry ->
                    val withheld = entry.valuesWithheld

                    Entry(
                        connection = entry.connection,
                        queryLanguage = entry.queryLanguage,
                        query = entry.query.takeIf { !withheld || entry.queryLanguage in PLACEHOLDER_SAFE_LANGUAGES },
                        varsCount = entry.vars?.size ?: 0,
                        valuesWithheld = withheld,
                        values = entry.vars.takeUnless { withheld }?.mapValues { (_, value) -> valueText(value) },
                        explain = entry.queryExplained.takeUnless { withheld },
                        count = entry.count,
                        totalCount = entry.totalCount,
                        totalNs = entry.totalNs,
                        measureQuery = Measure.of(entry.measureQuery),
                        measureIterator = Measure.of(entry.measureIterator),
                        measureSerializer = Measure.of(entry.measureSerializer),
                        measureDeserializer = Measure.of(entry.measureDeserializer),
                        measureExplain = Measure.of(entry.measureExplain),
                    )
                },
            )

            /**
             * One bind value as compact JSON text, cut at [MAX_VALUE_CHARS]: a write binds a whole document.
             *
             * Must not throw: this runs in `finish`, after the response, and a throw there loses the whole record.
             */
            internal fun valueText(value: Any?): String {
                val text = Json.encodeToString(JsonElement.serializer(), toJson(value))

                return when {
                    text.length <= MAX_VALUE_CHARS -> text
                    else -> text.take(MAX_VALUE_CHARS) + " … (${text.length} chars)"
                }
            }

            /** What the drivers bind: Karango slumbers to plain maps, lists and scalars; Monko adds BSON's own types. */
            private fun toJson(value: Any?): JsonElement = when (value) {
                null -> JsonNull
                is String -> JsonPrimitive(value)
                // JSON has no number for NaN or Infinity (also Decimal128's): those become text
                is Number -> JsonPrimitive(value).takeIf { it.doubleOrNull?.isFinite() != false }
                    ?: JsonPrimitive(value.toString())
                is Boolean -> JsonPrimitive(value)
                is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJson(v) })
                is Iterable<*> -> JsonArray(value.map { toJson(it) })
                is Array<*> -> JsonArray(value.map { toJson(it) })
                is Date -> JsonPrimitive(value.toInstant().toString())
                else -> JsonPrimitive(value.toString())
            }
        }
    }

    override fun finish(call: ApplicationCall): Data =
        Data.of(profiler.entries.filterIsInstance<QueryProfiler.Entry.Impl>())
}
