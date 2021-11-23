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

        "UserRecord.isAnonymous()" - {
            "must be correct when the it is the anonymous user" {
                UserRecord.anonymous.isAnonymous() shouldBe true

                UserRecord(
                    userId = "anonymous",
                    clientIp = "__clientIp__",
                    desc = "__desc__",
                    type = "__type__",
                ).isAnonymous() shouldBe true
            }
        }
    }
}
