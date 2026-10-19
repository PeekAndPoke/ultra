package io.peekandpoke.funktor.auth

import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import java.security.MessageDigest

/**
 * The renames that retire an account, handed to [AuthUserAdapter.retire].
 *
 * A retired account keeps its row (other collections may reference it) but gives up its unique key, so the
 * address can sign up again. The renamed key is `retired-<64 hex>@<32 hex>#retired.invalid`:
 * - the 64 hex are SHA-256 over the canonical original address — the plaintext is NOT kept, yet an app-level
 *   merge can still find an address's retired rows through [retiredKeyPrefix];
 * - the 32 hex come from the account's `_id`, so repeated retirements of one address never collide;
 * - the `#` makes the domain part illegal, so the value fails [EmailAddress.isValidFormat] and no sign-up can
 *   ever produce it; `.invalid` (RFC 2606) never resolves.
 */
class AccountRetirement private constructor(
    /** The account being retired. */
    val userId: UserId,
    private val tag: String,
) {
    companion object {
        private const val PREFIX = "retired-"
        private const val DOMAIN = "#retired.invalid"

        private val retiredEmail = Regex("retired-[0-9a-f]{64}@[0-9a-f]{32}#retired\\.invalid")

        /** The retirement of the account [userId]. */
        fun of(userId: UserId): AccountRetirement = AccountRetirement(userId = userId, tag = sha256Hex(userId.value).take(32))

        /** True when [email] is a retired account's renamed key. */
        fun isRetired(email: EmailAddress): Boolean = retiredEmail.matches(email.value)

        /**
         * What every retired key of [email] starts with: an app finds the retired rows of an address with a
         * prefix match on this, instead of re-deriving the hash.
         */
        fun retiredKeyPrefix(email: EmailAddress): String = "$PREFIX${sha256Hex(email.value)}@"

        private fun sha256Hex(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }

    /**
     * [email] renamed, so the address is free for a new sign-up and is not kept in plaintext. Idempotent: an
     * already-retired key is returned unchanged.
     */
    fun rename(email: EmailAddress): EmailAddress = when {
        isRetired(email) -> email
        else -> EmailAddress("${retiredKeyPrefix(email)}$tag$DOMAIN")
    }
}
