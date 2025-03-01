package de.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class UserRecordSpec : FreeSpec() {

    init {
        "UserRecord.anonymous" - {
            "must be correct" {

                val subject = UserRecord.anonymous

                subject.userId shouldBe "anonymous"
                subject.clientIp shouldBe null
                subject.desc shouldBe null
                subject.type shouldBe null
            }

            "must be comparable" {
                UserRecord.anonymous shouldBeSameInstanceAs UserRecord.anonymous

                UserRecord.anonymous shouldBe UserRecord(userId = "anonymous")
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

        "UserRecord.system" - {
            "must be correct" {

                val subject = UserRecord.system("IP")

                subject.userId shouldBe "system"
                subject.clientIp shouldBe "IP"
                subject.desc shouldBe null
                subject.type shouldBe null
            }

            "must be comparable" {
                UserRecord.system("IP") shouldNotBeSameInstanceAs UserRecord.system("IP")

                UserRecord.system("IP") shouldBe UserRecord(userId = "system", clientIp = "IP")
            }
        }

        "UserRecord.isSystem()" - {
            "must be correct when the it is the anonymous user" {
                UserRecord.system(null).isSystem() shouldBe true

                UserRecord(
                    userId = "system",
                    clientIp = "__clientIp__",
                    desc = "__desc__",
                    type = "__type__",
                ).isSystem() shouldBe true
            }
        }
    }
}
