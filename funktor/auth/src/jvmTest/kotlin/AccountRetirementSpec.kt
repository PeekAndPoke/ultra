package io.peekandpoke.funktor.auth

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId

/**
 * The renamed key of a retired account: `retired-<sha256(address)>@<id tag>#retired.invalid`.
 *
 * Each property here is one the decisions of 2026-10-19 require — no plaintext of the address kept, never
 * collide with a sign-up, stay a valid stored [EmailAddress], stay unique across repeated retirements, and
 * stay findable by an app-level merge.
 */
class AccountRetirementSpec : StringSpec({

    val adam = EmailAddress.of("adam.smith@example.com")

    "the format: retired-, 64 hex, @, 32 hex, #retired.invalid" {
        val retired = AccountRetirement.of(UserId("users/123")).rename(adam)

        retired.value shouldMatch Regex("retired-[0-9a-f]{64}@[0-9a-f]{32}#retired\\.invalid")
    }

    "NO plaintext of the original address is kept — not the address, its local part or its domain" {
        val retired = AccountRetirement.of(UserId("users/123")).rename(adam).value

        retired shouldNotContain "adam"
        retired shouldNotContain "smith"
        retired shouldNotContain "example"
    }

    "an app-level merge finds the retired keys of an address by its prefix, and only that address's" {
        val first = AccountRetirement.of(UserId("users/1")).rename(adam)
        val second = AccountRetirement.of(UserId("users/2")).rename(adam)
        val other = AccountRetirement.of(UserId("users/3")).rename(EmailAddress.of("eve@example.com"))

        val prefix = AccountRetirement.retiredKeyPrefix(adam)

        first.value shouldStartWith prefix
        second.value shouldStartWith prefix
        other.value.startsWith(prefix) shouldBe false

        withClue("the prefix hashes the CANONICAL address, so however it was typed, it matches") {
            AccountRetirement.retiredKeyPrefix(EmailAddress.of("  Adam.Smith@EXAMPLE.com ")) shouldBe prefix
        }
    }

    "a retired key can NEVER be signed up: it fails isValidFormat whatever the original was" {
        val originals = listOf(
            "adam@example.com",
            "first.last+tag@sub.example.co.uk",
            "\"quoted@local\"@example.com",
            "user@[127.0.0.1]",
            "admin@localhost",
            "no-at-sign-at-all",
            "x@y",
        ).map { EmailAddress.of(it) }

        originals.forEach { original ->
            withClue(original.value) {
                AccountRetirement.of(UserId("users/1")).rename(original).isValidFormat shouldBe false
            }
        }
    }

    "a retired key is a valid stored EmailAddress of fixed length, even for the longest original" {
        // The constructor re-validates: canonical, no control characters, at most MAX_LENGTH.
        val longest = EmailAddress.of("a".repeat(64) + "@" + "b".repeat(185) + ".com")
        longest.value.length shouldBe EmailAddress.MAX_LENGTH

        val retired = AccountRetirement.of(UserId("users/1")).rename(longest)

        retired.value.length shouldBeLessThanOrEqual EmailAddress.MAX_LENGTH
        retired.value.length shouldBe AccountRetirement.of(UserId("users/1")).rename(adam).value.length
        EmailAddress(retired.value) shouldBe retired
        AccountRetirement.isRetired(retired) shouldBe true
    }

    "two retirements of the SAME address never collide — the tag comes from the account" {
        AccountRetirement.of(UserId("users/1")).rename(adam) shouldNotBe AccountRetirement.of(UserId("users/2")).rename(adam)
    }

    "an id with upper-case characters still yields a canonical (lower-case) value" {
        // Arango keys are case-sensitive; lower-casing the raw id would merge `Ab` and `aB`.
        val upper = AccountRetirement.of(UserId("users/AbC")).rename(adam)
        val lower = AccountRetirement.of(UserId("users/abc")).rename(adam)

        upper shouldNotBe lower
        upper.value shouldBe upper.value.lowercase()
    }

    "deterministic and idempotent: the same account renames to the same value, and renaming twice is a no-op" {
        // Two concurrent retirements of one account then write the SAME value instead of fighting over the
        // unique index, and a retry after a half-done retirement changes nothing.
        val retirement = AccountRetirement.of(UserId("users/7"))
        val once = retirement.rename(adam)

        AccountRetirement.of(UserId("users/7")).rename(adam) shouldBe once
        retirement.rename(once) shouldBe once
        AccountRetirement.of(UserId("users/8")).rename(once) shouldBe once
    }

    "isRetired recognises exactly the retired form" {
        AccountRetirement.isRetired(AccountRetirement.of(UserId("users/1")).rename(adam)) shouldBe true

        AccountRetirement.isRetired(adam) shouldBe false
        AccountRetirement.isRetired(EmailAddress.of("retired@example.invalid")) shouldBe false
        AccountRetirement.isRetired(
            EmailAddress.of("retired-${"0".repeat(64)}@${"0".repeat(32)}.retired.invalid")
        ) shouldBe false
    }
})
