package de.peekandpoke.ultra.common.cache

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class SynchronizedTtlCacheSpec : FreeSpec() {

    init {

        "Setting and getting values" {

            val subject = SynchronizedTtlCache<String, String>(ttlMs = 1_000)

            subject.put("k1", "v1")
            subject.put("k2", "v2")

            withClue("Check for existing entries") {
                subject.getOrPut("k1") { "other1" } shouldBe "v1"
                subject.getOrPut("k2") { "other2" } shouldBe "v2"
            }

            withClue("Check for non existing entries") {
                subject.get("k3") shouldBe null
                subject.getOrPut("k3") { "other3" } shouldBe "other3"
            }
        }

        "Values must time out" {

            val subject = SynchronizedTtlCache<String, String>(ttlMs = 100)

            subject.put("k1", "v1")

            withClue("Check before timeout") {
                subject.has("k1") shouldBe true
                subject.get("k1") shouldBe "v1"
            }

            // Wait for the timeout to happen
            delay(200)

            withClue("Check after entries must have expired") {
                subject.has("k1") shouldBe false
                subject.get("k1") shouldBe null
                subject.getOrPut("k1") { "new1" } shouldBe "new1"
            }
        }
    }
}
