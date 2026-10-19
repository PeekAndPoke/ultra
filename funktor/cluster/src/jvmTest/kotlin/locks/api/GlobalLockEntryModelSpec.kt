package io.peekandpoke.funktor.cluster.locks.api

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.inspect.cluster.locks.api.GlobalLockEntryModel
import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GlobalLockEntryModelSpec : StringSpec({

    "A listing from a server that predates heartbeats decodes, with the heartbeat at the creation" {
        val created = MpInstant.fromEpochMillis(1_790_000_000_000)

        val current = Json.encodeToJsonElement(
            GlobalLockEntryModel.serializer(),
            GlobalLockEntryModel(
                key = "key",
                serverId = "server",
                created = created,
                expires = created.plus(15.minutes),
                lastHeartbeat = created.plus(5.seconds),
            ),
        )
        // What such a server sends: the same, without the field.
        val old = JsonObject(current.jsonObject - "lastHeartbeat")

        Json.decodeFromJsonElement(GlobalLockEntryModel.serializer(), old).lastHeartbeat shouldBe created
    }
})
