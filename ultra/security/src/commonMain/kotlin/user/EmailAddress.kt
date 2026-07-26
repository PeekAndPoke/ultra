package io.peekandpoke.ultra.security.user

import io.peekandpoke.ultra.common.isEmail
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A user's email address, ALWAYS in canonical form: trimmed and lowercased.
 *
 * This is the type that makes case-insensitive email matching structural instead of a convention.
 * Emails were already STORED canonically (every creation path went through
 * `CreateUserForSignupParams.of`, which lowercases), but LOOKUP canonicalization was a per-caller
 * habit — `loadByEmail` and the repos' `findByEmail` are case-sensitive exact matches, and several
 * callers passed the raw string. The consequences were silent: a mixed-case password-reset request
 * found no user and sent no email; a mixed-case SSO login failed to match; a mixed-case SSO signup
 * missed the existence check and created a duplicate user. With this type the rule lives in one
 * place and a non-canonical value cannot be constructed at all.
 *
 * **[init] validates, [of] normalizes.** `init` runs on EVERY construction including
 * deserialization, so it only ever ACCEPTS-or-REJECTS; it deliberately does not transform, because
 * silently rewriting a value while decoding would hide bad data. [of] is the boundary constructor
 * that trims and lowercases. So:
 * - reading user input (a request body, an SSO claim, a CSV import) → [of] / [parseOrNull]
 * - a value that should already be canonical (from the DB, from another `EmailAddress`) → the ctor,
 *   which throws if that assumption is wrong.
 *
 * **Neither `init` nor [of] enforces RFC format — only [isValidFormat] does, where it is checked.** This was
 * tightened twice during review, and the reasoning matters:
 *
 * - Format is not a decode-time invariant, because the SSO signup paths historically persisted
 *   whatever the provider returned with no validation at all. Asserting it in `init` would render
 *   such a row permanently unreadable — the user unable to log in, and any list query returning that
 *   row throwing.
 * - Format is not a LOOKUP-time invariant either, for the same population and a subtler reason:
 *   **matching is matching.** Rejecting `admin@localhost` before a lookup does not protect anything —
 *   a non-matching value simply does not match — it just makes the row unreachable, so the account
 *   lists and renders but can never sign in or recover. That is the same silent failure this type was
 *   built to eliminate, pointed the other way.
 *
 * Format is therefore checked exactly where a bad value has consequences: CREATING an account —
 * [isValidFormat], asserted by `AuthUserAdapter.CreateUserForSignupParams`.
 *
 * NOTE this type is deliberately **ASCII-only** (see [of]); internationalized (EAI/IDN) addresses are
 * not representable. That also keeps canonicalization free of Unicode normalization concerns.
 *
 * API request bodies keep a raw `String`: a user types `Foo@Example.COM `, and rejecting that with a
 * 400 would be hostile. Handlers normalize with [of] at the boundary.
 *
 * NOT usable as a URI/query param today: the generic value-class param converter constructs via the
 * primary constructor, never [of], so `?email=Foo@Bar.com` would fail the canonical check and bind to
 * a 404 instead of matching. Give the converter an `of()`-style hook before routing on this type.
 */
@Serializable
@JvmInline
value class EmailAddress(val value: String) {
    init {
        require(value.isNotBlank()) { "EmailAddress must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "EmailAddress must be at most $MAX_LENGTH chars, got ${value.length}"
        }
        // The ONLY semantic invariant, and one the codebase itself guarantees.
        require(value == value.trim().lowercase()) {
            "EmailAddress must be canonical (trimmed + lowercased) — use EmailAddress.of(raw)"
        }
    }

    /**
     * The part before the last `@`. Display only — do NOT use for a trust decision; a quoted local
     * part may itself contain `@`.
     */
    val localPart: String get() = value.substringBeforeLast('@')

    /**
     * The part after the last `@`. Display only — do NOT use for a domain allowlist or any other
     * trust decision without re-validating; see [localPart].
     */
    val domain: String get() = value.substringAfterLast('@')

    /**
     * True when this address is well-formed per [isEmail].
     *
     * Deliberately NOT part of the type's invariant — see the class doc. Check it where a malformed
     * address would actually cause harm (creating an account); never before a LOOKUP, where rejecting
     * an odd-but-stored address only makes that account unreachable.
     */
    val isValidFormat: Boolean get() = value.isEmail()

    override fun toString(): String = value

    companion object {
        /** RFC 5321 caps a forward-path at 254 characters. */
        const val MAX_LENGTH: Int = 254

        /**
         * Canonicalizes [raw] (trim + lowercase) and returns it as an [EmailAddress].
         *
         * THE boundary constructor — use it for anything a human or a third party supplied. Throws
         * [IllegalArgumentException] only for a value that could not be canonicalized safely (too
         * long, or non-ASCII); RFC format is NOT checked here — see [isValidFormat]. Use
         * [parseOrNull] where a bad value must degrade rather than throw.
         */
        fun of(raw: String): EmailAddress {
            // Bounded FIRST: everything below copies the string, and `raw` arrives unbounded from a
            // request body (the codec permits very large JSON strings).
            require(raw.length <= MAX_LENGTH) {
                "EmailAddress must be at most $MAX_LENGTH chars, got ${raw.length}"
            }
            // Rejected BEFORE lowercasing, and deliberately so: U+212A KELVIN SIGN lowercases to
            // ASCII `k`, so a non-ASCII input could otherwise canonicalize onto a different, existing
            // address. Checking the raw input makes that collapse impossible by construction rather
            // than relying on every comparison downstream being consistent.
            require(raw.all { it.code < 0x80 }) { "EmailAddress must be ASCII-only" }

            return EmailAddress(raw.trim().lowercase())
        }

        /**
         * Like [of], but returns `null` instead of throwing.
         *
         * THE lookup-path parser: it canonicalizes without demanding RFC format, so an address that
         * predates validation stays matchable. Use at every boundary reading ATTACKER-SUPPLIED input —
         * request bodies, SSO provider claims, the JWT `email` claim — where a bad value must degrade
         * rather than turn a payload into a 500. When the value will CREATE an account, additionally
         * check [isValidFormat].
         */
        fun parseOrNull(raw: String?): EmailAddress? = raw?.let {
            try {
                of(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
