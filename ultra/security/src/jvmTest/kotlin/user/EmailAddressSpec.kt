package io.peekandpoke.ultra.security.user

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.descriptors.PrimitiveKind

/**
 * The load-bearing distinction in this type is WHICH invariant lives WHERE:
 *
 * - `init` asserts only what this codebase GUARANTEES — canonicality, non-blank, bounded. It runs on
 *   decode, so anything asserted here is a claim about every row already in the database.
 * - `of` / `parseOrNull` canonicalize, bound the input, and reject non-ASCII. They do NOT check RFC
 *   format, so they are safe on a LOOKUP path.
 * - `isValidFormat` is the format check, applied only where a malformed address does harm: CREATING
 *   an account.
 *
 * Format is excluded from BOTH `init` and `of`, for the same population and two different reasons.
 * The SSO signup paths historically persisted whatever the provider returned with no validation:
 * putting format in `init` would make such a row undecodable (unreadable account, list queries
 * throwing); putting it in `of` would make it unmatchable (the account lists and renders but can never
 * sign in or reset — matching is matching, and rejecting an odd-but-stored address protects nothing).
 * These tests pin the split in every direction, because collapsing it any way is a silent regression.
 */
class EmailAddressSpec : StringSpec({

    //  init — canonicality, the invariant the code controls  ///////////////////////////////////////

    "init REJECTS a non-canonical value — it validates, it does not transform" {
        shouldThrow<IllegalArgumentException> { EmailAddress("User@Example.com") }
        shouldThrow<IllegalArgumentException> { EmailAddress("user@example.com ") }
        shouldThrow<IllegalArgumentException> { EmailAddress(" user@example.com") }
        shouldThrow<IllegalArgumentException> { EmailAddress("USER@EXAMPLE.COM") }
    }

    "init rejects blank values" {
        shouldThrow<IllegalArgumentException> { EmailAddress("") }
        shouldThrow<IllegalArgumentException> { EmailAddress("   ") }
    }

    "init accepts a canonical address" {
        EmailAddress("user@example.com").value shouldBe "user@example.com"
        EmailAddress("user.name+tag@sub.domain.com").value shouldBe "user.name+tag@sub.domain.com"
    }

    "the length bound accepts exactly MAX_LENGTH and rejects one more" {
        val suffix = "@x.com"
        val atLimit = "a".repeat(EmailAddress.MAX_LENGTH - suffix.length) + suffix
        atLimit.length shouldBe EmailAddress.MAX_LENGTH

        shouldNotThrowAny { EmailAddress(atLimit) }
        shouldThrow<IllegalArgumentException> { EmailAddress("a$atLimit") }
    }

    "init does NOT enforce RFC format" {
        // A row written before this type existed (SSO signup never validated format) must stay
        // readable. This is the decodability half of the trade-off documented on the class.
        shouldNotThrowAny { EmailAddress("not-an-email") }
        shouldNotThrowAny { EmailAddress("admin@localhost") }
    }

    //  of / parseOrNull — the boundary, where format and ASCII are enforced  ///////////////////////

    "of() NORMALIZES — the boundary constructor for raw input" {
        EmailAddress.of("  User@Example.COM  ") shouldBe EmailAddress("user@example.com")
        EmailAddress.of("USER@EXAMPLE.COM") shouldBe EmailAddress("user@example.com")
    }

    "of() does NOT enforce RFC format either — so a LOOKUP stays possible" {
        // THE reachability half. If `of` rejected these, an account stored with such an address
        // could never be found again: sign-in and password-reset both canonicalize through `of` /
        // `parseOrNull` before calling `loadByEmail`.
        EmailAddress.of("admin@localhost").value shouldBe "admin@localhost"
        EmailAddress.of("  Admin@LOCALHOST ").value shouldBe "admin@localhost"
        EmailAddress.parseOrNull("not-an-email") shouldNotBe null
    }

    "of() still rejects what it cannot canonicalize safely" {
        // Bounded first, so an unbounded request body cannot force large copies.
        shouldThrow<IllegalArgumentException> { EmailAddress.of("a".repeat(EmailAddress.MAX_LENGTH + 1)) }
        shouldThrow<IllegalArgumentException> { EmailAddress.of("") }
    }

    "isValidFormat is the format check, and it is separate from construction" {
        EmailAddress("user@example.com").isValidFormat shouldBe true
        EmailAddress("user.name+tag@sub.domain.com").isValidFormat shouldBe true

        // Constructible and matchable, but NOT something to create a new account with.
        EmailAddress("admin@localhost").isValidFormat shouldBe false
        EmailAddress("not-an-email").isValidFormat shouldBe false
    }

    "of() rejects non-ASCII BEFORE lowercasing, so no character can collapse onto another address" {
        // U+212A KELVIN SIGN lowercases to ASCII 'k'. Without the raw-input ASCII guard,
        // `of("\u212Aarsten@x.com")` would canonicalize to "karsten@x.com" — a DIFFERENT, possibly
        // existing account. The guard makes that collapse impossible rather than relying on every
        // downstream comparison being consistent.
        shouldThrow<IllegalArgumentException> { EmailAddress.of("\u212Aarsten@x.com") }
        EmailAddress.parseOrNull("\u212Aarsten@x.com") shouldBe null

        // The plain ASCII spelling is of course fine, and is a different address.
        EmailAddress.of("karsten@x.com").value shouldBe "karsten@x.com"
    }

    "parseOrNull degrades instead of throwing, for attacker-supplied input" {
        EmailAddress.parseOrNull("  User@Example.com ") shouldBe EmailAddress("user@example.com")
        EmailAddress.parseOrNull(null) shouldBe null
        EmailAddress.parseOrNull("") shouldBe null
        EmailAddress.parseOrNull("a".repeat(EmailAddress.MAX_LENGTH + 1)) shouldBe null
    }

    //  projections  ////////////////////////////////////////////////////////////////////////////////

    "localPart and domain split on the LAST @" {
        val subject = EmailAddress("user.name@sub.example.com")

        subject.localPart shouldBe "user.name"
        subject.domain shouldBe "sub.example.com"
    }

    "a quoted local part containing @ does not fool the domain projection" {
        // The RFC regex admits a quoted local part, which may itself contain '@'. Splitting on the
        // FIRST '@' would report `corp.com"@evil.com` as the domain — which reads like a tenant
        // allowlist bypass waiting to happen.
        val subject = EmailAddress("\"a@corp.com\"@evil.com")

        subject.domain shouldBe "evil.com"
        subject.localPart shouldBe "\"a@corp.com\""
    }

    //  identity + serialization  ///////////////////////////////////////////////////////////////////

    "equality is by value — and case can no longer make two of the same address differ" {
        EmailAddress("user@example.com") shouldBe EmailAddress.of("USER@EXAMPLE.COM")
        EmailAddress("user@example.com") shouldNotBe EmailAddress("other@example.com")
        EmailAddress("user@example.com").hashCode() shouldBe EmailAddress.of("User@Example.com").hashCode()
    }

    "toString returns the bare value" {
        EmailAddress("user@example.com").toString() shouldBe "user@example.com"
    }

    "serializes as a plain string in kotlinx" {
        EmailAddress.serializer().descriptor.isInline shouldBe true
        EmailAddress.serializer().descriptor.getElementDescriptor(0).kind shouldBe PrimitiveKind.STRING
    }

    "slumber round-trips it as a bare scalar, and the CANONICAL invariant runs on decode" {
        val codec = Codec.default

        codec.slumber(EmailAddress("user@example.com")) shouldBe "user@example.com"
        codec.awake<EmailAddress>("user@example.com") shouldBe EmailAddress("user@example.com")

        // Non-canonical stored data is rejected on the way in — the fail-loud half of the split.
        shouldThrow<Throwable> { codec.awake<EmailAddress>("User@Example.com") }
    }

    "decoding does NOT re-validate format — an old row stays readable" {
        // The other half of the split, and the reason for it: a row written by a path that never
        // validated format must not become an undecodable, un-loggable-in account.
        Codec.default.awake<EmailAddress>("admin@localhost") shouldBe EmailAddress("admin@localhost")
    }
})
