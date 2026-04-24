package io.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class UserRecordSpec : FreeSpec() {

    init {
        "UserRecord.anonymous" - {
            "must be the Anonymous variant with default fields" {
                val subject = UserRecord.anonymous

                subject.shouldBeInstanceOf<UserRecord.Anonymous>()
                subject.userId shouldBe "anonymous"
                subject.clientIp shouldBe null
                subject.email shouldBe null
                subject.desc shouldBe null
                subject.type shouldBe null
            }

            "must be a singleton" {
                UserRecord.anonymous shouldBeSameInstanceAs UserRecord.anonymous
            }

            "must compare equal to a fresh Anonymous()" {
                UserRecord.anonymous shouldBe UserRecord.Anonymous()
            }
        }

        "UserRecord.system" - {
            "must produce the System variant carrying the IP" {
                val subject = UserRecord.system("IP")

                subject.shouldBeInstanceOf<UserRecord.System>()
                subject.userId shouldBe "system"
                subject.clientIp shouldBe "IP"
                subject.email shouldBe null
                subject.desc shouldBe null
                subject.type shouldBe null
            }

            "must be value-equal but not reference-equal" {
                UserRecord.system("IP") shouldNotBeSameInstanceAs UserRecord.system("IP")
                UserRecord.system("IP") shouldBe UserRecord.System(clientIp = "IP")
            }
        }

        "isAnonymous() / isSystem()" - {
            "report correctly across variants" {
                UserRecord.anonymous.isAnonymous() shouldBe true
                UserRecord.anonymous.isSystem() shouldBe false

                UserRecord.system(null).isSystem() shouldBe true
                UserRecord.system(null).isAnonymous() shouldBe false

                UserRecord.LoggedIn(userId = "alice").isAnonymous() shouldBe false
                UserRecord.LoggedIn(userId = "alice").isSystem() shouldBe false

                UserRecord.ApiKey(userId = "alice", keyId = "k").isAnonymous() shouldBe false
                UserRecord.ApiKey(userId = "alice", keyId = "k").isSystem() shouldBe false
            }
        }

        "UserRecord.LoggedIn" - {
            "exposes all fields" {
                val subject = UserRecord.LoggedIn(
                    userId = "alice",
                    clientIp = "1.2.3.4",
                    email = "alice@example.com",
                    desc = "Alice",
                    type = "user",
                )

                subject.userId shouldBe "alice"
                subject.clientIp shouldBe "1.2.3.4"
                subject.email shouldBe "alice@example.com"
                subject.desc shouldBe "Alice"
                subject.type shouldBe "user"
            }
        }

        "UserRecord.ApiKey" - {
            "exposes key metadata alongside user identity" {
                val subject = UserRecord.ApiKey(
                    userId = "alice",
                    clientIp = "1.2.3.4",
                    keyId = "key-1",
                    keyName = "ci-deploy",
                    email = "alice@example.com",
                    desc = "Alice",
                    type = "user",
                )

                subject.userId shouldBe "alice"
                subject.clientIp shouldBe "1.2.3.4"
                subject.keyId shouldBe "key-1"
                subject.keyName shouldBe "ci-deploy"
                subject.email shouldBe "alice@example.com"
                subject.desc shouldBe "Alice"
                subject.type shouldBe "user"
            }
        }
    }
}
