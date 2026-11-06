package io.peekandpoke.funktor.auth.domain

import java.security.MessageDigest
import java.util.Base64

/**
 * What the auth records store of a one-shot secret (a mailed link, an org-selection token): SHA-256 of the raw
 * token, base64url. A read of the records then yields no working link.
 *
 * No salt and no slow hash: the raw token is `RealmTokenConfig.randomTokenByteLength` random bytes, so there is
 * nothing to guess. The lookup stays an indexed equality on the hash. The raw token exists only on its way into the
 * mail or the response. The constructor is private: [of] is the only way to make one, so no caller can store the
 * clear text by mistake.
 */
@JvmInline
value class TokenHash private constructor(val value: String) {
    companion object {
        /** The hash of [raw], the token as mailed or handed to the client. */
        fun of(raw: String): TokenHash = TokenHash(
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            )
        )
    }
}
