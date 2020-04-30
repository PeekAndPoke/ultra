package de.peekandpoke.ultra.security.user

import io.kotlintest.assertSoftly
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class UserRecordProviderSpec : StringSpec({

    "UserRecordProvider.lazy should provide the UserRecord correctly" {

        val subject = UserRecordProvider.lazy {
            UserRecord(userId = "id", clientIp = "1.2.3.4")
        }

        assertSoftly {
            subject.invoke() shouldBeSameInstanceAs subject.invoke()
            subject.invoke().userId shouldBe "id"
            subject.invoke().clientIp shouldBe "1.2.3.4"
        }
    }
})
