package io.peekandpoke.funktor.auth

import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import java.security.MessageDigest

/**
 * The renames that retire an account, handed to [AuthUserAdapter.retire].
 *
 * A retired account keeps its row (other collections may reference it) but gives up its unique keys, so
 * the address can sign up again. The format is `<original>#retired-<32 hex>.invalid`:
 * - the suffix has no `@`, so it always lands in the domain part, where `#` is illegal — the value
 *   fails [EmailAddress.isValidFormat] whatever the original was, so no sign-up can ever produce it;
 * - the hex is derived from the account's `_id`, so repeated retirements of one address never collide;
 * - `.invalid` (RFC 2606) never resolves, so nothing can deliver mail to it.
 */
class AccountRetirement private constructor(
    /** The account being retired. */
    val userId: UserId,
    private val tag: String,
) {
    companion object {
        private const val MARKER = "#retired-"
        private const val TLD = ".invalid"

        /** `#retired-` + 32 hex + `.invalid`. */
        private const val SUFFIX_LENGTH = MARKER.length + 32 + TLD.length

        private val retiredEmail = Regex("#retired-[0-9a-f]{32}\\.invalid$")

        /** The retirement of the account [userId]. */
        fun of(userId: UserId): AccountRetirement {
            val digest = MessageDigest.getInstance("SHA-256").digest(userId.value.toByteArray(Charsets.UTF_8))

            return AccountRetirement(
                userId = userId,
                tag = digest.take(16).joinToString("") { "%02x".format(it) },
            )
        }

        /** True when [email] is a retired account's renamed address. */
        fun isRetired(email: EmailAddress): Boolean = retiredEmail.containsMatchIn(email.value)
    }

    /**
     * [email] renamed, so the address is free for a new sign-up. Idempotent: an already-retired
     * address is returned unchanged.
     */
    fun rename(email: EmailAddress): EmailAddress {
        if (isRetired(email)) return email

        val original = email.value.take(EmailAddress.MAX_LENGTH - SUFFIX_LENGTH)

        return EmailAddress("$original$MARKER$tag$TLD")
    }

    /** Any OTHER unique-key value of the account, renamed in the same scheme. Idempotent. */
    fun rename(value: String): String = when {
        value.endsWith("$MARKER$tag") -> value
        else -> "$value$MARKER$tag"
    }
}
