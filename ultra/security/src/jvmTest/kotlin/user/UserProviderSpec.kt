package de.peekandpoke.ultra.security.user

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class UserProviderSpec : StringSpec({

    "UserRecordProvider.lazy should provide the UserRecord correctly" {
        val subject = UserProvider.lazy {
            User(
                record = UserRecord(userId = "id", clientIp = "1.2.3.4"),
                permissions = UserPermissions.system,
            )
        }

        subject.invoke() shouldBeSameInstanceAs subject.invoke()
        subject.invoke().record.userId shouldBe "id"
        subject.invoke().record.clientIp shouldBe "1.2.3.4"
    }

    "UserRecordProvider.system() should provide the UserRecord correctly" {
        val subject = UserProvider.system()

        subject.invoke() shouldBeSameInstanceAs subject.invoke()
        subject.invoke().record.userId shouldBe "system"
        subject.invoke().record.clientIp.shouldNotBeNull()
    }
})
