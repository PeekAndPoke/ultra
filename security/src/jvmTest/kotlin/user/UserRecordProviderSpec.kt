package de.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class UserRecordProviderSpec : StringSpec({

    "UserRecordProvider.lazy should provide the UserRecord correctly" {

        val subject = UserRecordProvider.lazy {
            UserRecord(userId = "id", clientIp = "1.2.3.4")
        }

        subject.invoke() shouldBeSameInstanceAs subject.invoke()
        subject.invoke().userId shouldBe "id"
        subject.invoke().clientIp shouldBe "1.2.3.4"
    }

    "UserRecordProvider.systemUser() should provide the UserRecord correctly" {

        val subject = UserRecordProvider.system()

        subject.invoke() shouldBeSameInstanceAs subject.invoke()
        subject.invoke().userId shouldBe "system"
    }
})
