package io.peekandpoke.funktor.auth

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId

/**
 * The renamed-key format of a retired account: `<original>#retired-<32 hex>.invalid`.
 *
 * Each property here is one the 2026-10-19 decision requires — never collide with a sign-up, stay a valid
 * stored [EmailAddress], stay unique across repeated retirements of one address.
 */
class AccountRetirementSpec : StringSpec({

    val adam = EmailAddress.of("adam@example.com")

    "the format: the original, then #retired-, 32 lowercase hex, .invalid" {
        val retired = AccountRetirement.of(UserId("users/123")).rename(adam)

        retired.value shouldMatch Regex("adam@example\\.com#retired-[0-9a-f]{32}\\.invalid")
    }

    "a retired address can NEVER be signed up: it fails isValidFormat whatever the original was" {
        // Sign-up (and every other account-creating path) asserts isValidFormat. The suffix always lands
        // in the domain part, where `#` is illegal — including for originals that never passed validation.
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

    "a retired address is a valid stored EmailAddress, even for the longest original" {
        // The constructor re-validates: canonical, no control characters, at most MAX_LENGTH.
        val longest = EmailAddress.of("a".repeat(64) + "@" + "b".repeat(185) + ".com")
        longest.value.length shouldBe EmailAddress.MAX_LENGTH

        val retired = AccountRetirement.of(UserId("users/1")).rename(longest)

        retired.value.length shouldBeLessThanOrEqual EmailAddress.MAX_LENGTH
        EmailAddress(retired.value) shouldBe retired
        AccountRetirement.isRetired(retired) shouldBe true
    }

    "two retirements of the SAME address never collide — the hex comes from the account" {
        val first = AccountRetirement.of(UserId("users/1")).rename(adam)
        val second = AccountRetirement.of(UserId("users/2")).rename(adam)

        first shouldNotBe second
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
        AccountRetirement.isRetired(EmailAddress.of("a#retired-0123456789abcdef0123456789abcdef@b.invalid")) shouldBe false
    }

    "any other unique key renames in the same scheme, idempotently" {
        val retirement = AccountRetirement.of(UserId("users/1"))
        val renamed = retirement.rename("adam-the-username")

        renamed shouldMatch Regex("adam-the-username#retired-[0-9a-f]{32}")
        retirement.rename(renamed) shouldBe renamed
    }
})
