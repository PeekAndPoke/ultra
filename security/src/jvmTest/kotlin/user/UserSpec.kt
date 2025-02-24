package de.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class UserSpec : FreeSpec() {

    init {
        "UserRecord.anonymous" - {
            "must be correct" {

                val subject = User.anonymous

                subject.record.userId shouldBe "anonymous"
                subject.record.clientIp shouldBe null
                subject.record.desc shouldBe null
                subject.record.type shouldBe null

                subject.permissions.isSuperUser shouldBe false
                subject.permissions.organisations shouldBe emptySet()
                subject.permissions.branches shouldBe emptySet()
                subject.permissions.groups shouldBe emptySet()
                subject.permissions.roles shouldBe emptySet()
                subject.permissions.permissions shouldBe emptySet()
            }

            "must be comparable" {
                User.anonymous shouldBeSameInstanceAs User.anonymous

                User.anonymous.record shouldBe UserRecord.anonymous
                User.anonymous.permissions shouldBe UserPermissions.anonymous
            }
        }

        "UserRecord.isAnonymous()" - {
            "must be correct when the it is the anonymous user" {
                User.anonymous.isAnonymous() shouldBe true
                User.system("IP").isAnonymous() shouldBe false
            }
        }

        "UserRecord.system" - {
            "must be correct" {
                val subject = User.system(null)

                subject.record shouldBe UserRecord.system(null)
                subject.permissions shouldBe UserPermissions.system
            }

            "must be comparable" {
                User.system("IP") shouldNotBeSameInstanceAs User.system("IP")
                User.system("IP") shouldBe User.system("IP")
            }
        }

        "UserRecord.isSystem()" - {
            "must be correct when the it is the anonymous user" {
                User.system(null).isSystem() shouldBe true
                User.anonymous.isSystem() shouldBe false
            }
        }
    }
}
