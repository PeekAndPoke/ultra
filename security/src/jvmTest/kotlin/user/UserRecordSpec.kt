package de.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class UserRecordSpec : FreeSpec() {

    init {
        "UserRecord.anonymous" - {
            "must be correct" {

                val subject = UserRecord.anonymous

                subject.userId shouldBe "anonymous"
                subject.clientIp shouldBe "unknown"
                subject.desc shouldBe "n/a"
                subject.type shouldBe "n/a"
            }

            "must be comparable" {
                UserRecord.anonymous shouldBeSameInstanceAs UserRecord.anonymous

                UserRecord.anonymous shouldBe UserRecord(
                    userId = "anonymous",
                    clientIp = "unknown",
                    desc = "n/a",
                    type = "n/a",
                )
            }
        }
    }
}
