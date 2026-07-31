package io.peekandpoke.funktor.insights.api

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The API DTOs must round-trip through Slumber, because that — not kotlinx — is what serializes an
 * `ApiRoutes` response. A field Slumber cannot describe would fail at runtime on the first request, and
 * would later hard-fail the TypeScript generator, which reads the same `SlumberConfig`.
 *
 * Note where the guard actually sits: "no `Any`, no star projections" is enforced by the COMPILER, not
 * here — kotlinx `@Serializable` has no serializer for `Any`, so declaring such a field fails the build
 * (verified by mutation). This spec proves the complementary thing: that the runtime shape Slumber
 * produces is the one a client will parse back.
 */
class InsightsModelsSlumberSpec : StringSpec({

    val codec = Codec.default

    val record = InsightsRecord(
        ref = InsightsRecordRef("records-2026-07-31", "12-00-00.json"),
        recordedAt = MpInstant.fromEpochMillis(1_700_000_000_000),
        method = "GET",
        path = "/api/things",
        status = 200,
        durationMs = 12.5,
        collectors = listOf(
            InsightsCollectorSlice(
                key = "request",
                data = buildJsonObject {
                    put("method", "GET")
                    put("uri", "/api/things")
                },
            ),
            InsightsCollectorSlice(key = "empty", data = JsonNull),
        ),
        next = InsightsRecordRef("records-2026-07-31", "12-00-01.json"),
        previous = null,
    )

    "InsightsRecord round-trips, and really is serialized on the way" {
        val slumbered = codec.slumber(record)

        // Pin that slumbering did actual work rather than handing the object back: without this the
        // round-trip would pass even if `slumber` were the identity function.
        slumbered.shouldBeInstanceOf<Map<*, *>>()
        (slumbered["ref"] as Map<*, *>)["bucket"] shouldBe record.ref.bucket
        (slumbered["collectors"] as List<*>).size shouldBe 2

        codec.awake<InsightsRecord>(slumbered) shouldBe record
    }


    "InsightsRecordSummary round-trips, nulls included" {
        val summary = InsightsRecordSummary(
            ref = InsightsRecordRef("records-2026-07-31", "12-00-00.json"),
            recordedAt = MpInstant.fromEpochMillis(1_700_000_000_000),
            method = "GET",
            // a PATH, not a URL — the writer stores request.path() so no `?token=` reaches the record
            path = "/api/things",
            status = 200,
            durationMs = 12.5,
        )

        codec.awake<InsightsRecordSummary>(codec.slumber(summary)) shouldBe summary

        val empty = InsightsRecordSummary(
            ref = InsightsRecordRef("b", "f"), recordedAt = null, method = null, path = null,
            status = null, durationMs = null,
        )

        codec.awake<InsightsRecordSummary>(codec.slumber(empty)) shouldBe empty
    }

    "an arbitrary JSON payload survives — the envelope really is open" {
        // A collector the framework has never heard of, carrying a shape it cannot name.
        val exotic = InsightsCollectorSlice(
            key = "some-app-collector",
            data = buildJsonObject {
                put("nested", buildJsonObject { put("deep", true) })
                put("number", 42)
            },
        )

        val back = codec.awake<InsightsCollectorSlice>(codec.slumber(exotic))

        back shouldBe exotic
        (back?.data as JsonObject)["number"] shouldBe JsonPrimitive(42)
    }

    "MpInstant is the timestamp type, so no java.time codec is needed" {
        // java.time.LocalDateTime would need a JavaTimeTsContributor for codegen, which does not exist.
        val slumbered = codec.slumber(record.recordedAt)

        // MpInstant goes through a custom codec, so pin the wire shape rather than only the round-trip
        slumbered.shouldBeInstanceOf<Map<*, *>>()
        codec.awake<MpInstant>(slumbered) shouldBe record.recordedAt
    }
})
