package io.peekandpoke.ultra.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpInstant

class AuditLogSpec : StringSpec({

    fun entry(message: String, epochMs: Long = 0L) = AuditLog.Entry(
        userId = "user-1",
        userType = null,
        userDesc = null,
        userEmail = null,
        clientIp = null,
        type = AuditLog.Entry.Type.info,
        message = message,
        ts = MpInstant.fromEpochMillis(epochMs),
    )

    "empty has no entries" {
        AuditLog.empty.entries.shouldBeEmpty()
    }

    "add appends an entry" {
        val log = AuditLog.empty.add(entry("first"))

        log.entries shouldHaveSize 1
        log.entries[0].message shouldBe "first"
    }

    "add is immutable - returns new instance" {
        val original = AuditLog.empty
        val updated = original.add(entry("new"))

        original.entries.shouldBeEmpty()
        updated.entries shouldHaveSize 1
    }

    "last(n) returns last n entries sorted by ts descending" {
        val log = AuditLog(
            listOf(
                entry("a", epochMs = 1000),
                entry("b", epochMs = 3000),
                entry("c", epochMs = 2000),
            )
        )

        val last2 = log.last(2)

        last2 shouldHaveSize 2
        last2[0].message shouldBe "b" // ts 3000 first (descending)
        last2[1].message shouldBe "c" // ts 2000 second
    }

    "last(n) with n larger than entries returns all sorted" {
        val log = AuditLog(listOf(entry("only", epochMs = 100)))

        val result = log.last(10)

        result shouldHaveSize 1
        result[0].message shouldBe "only"
    }
})
