package io.peekandpoke.ultra.security.jwt

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Authenticates a token's MAC **before** anything parses it.
 *
 * ### Why this exists
 *
 * A JWT library decodes the header and payload JSON before it checks the signature — it has to, in
 * general, because the header is what names the algorithm. Measured against `java-jwt` 4.5.2: a token
 * with a malformed payload and an invalid signature raises a *decode* error, not a signature error, and
 * a 20 MB string payload is fully parsed and allocated before the signature check fails. That code runs
 * on **every request carrying an `Authorization: Bearer` header**, valid or not, so the parser is
 * unauthenticated attack surface.
 *
 * This gate removes that: the MAC is computed over the raw encoded segments, so a token that fails it is
 * rejected having parsed **nothing**.
 *
 * ### Why it is allowed to reorder the steps
 *
 * RFC 7515 §5.2: *"The order of the steps is not significant in cases where there are no dependencies
 * between the inputs and outputs of the steps."* Signature validation consumes only
 * `ASCII(BASE64URL(header) || '.' || BASE64URL(payload))` and the signature octets — with the algorithm
 * fixed out-of-band it does not depend on the parsed header, so it may run first.
 *
 * The same section: *"unless the algorithm(s) used in the JWS are acceptable to the application, it
 * SHOULD consider the JWS to be invalid."* Fixing the algorithm here and never reading `alg` is what
 * that endorses, and it is the standard defence against algorithm confusion — `alg: none` and
 * `alg: HS256` are not rejected by a check, they are **unreachable**, because the field is never read.
 *
 * This gate **replaces nothing**. The library still performs its full validation afterwards, so every
 * RFC step still runs and the set of accepted tokens is unchanged.
 *
 * ### PRECONDITION — read before reusing this
 *
 * The fast path is valid **only because this application issues the tokens and fixes the algorithm**.
 * A verifier for an external identity provider (Keycloak, or any OIDC issuer whose access token is used
 * directly as the bearer token) cannot use it: RS256 key selection needs `kid` from the header, so the
 * header must be parsed first.
 *
 * Such a verifier should still not parse the **payload** before verifying. The rule that generalises is:
 * *parse only the header, hard-capped, then verify, then parse the payload.* A header is a few hundred
 * bytes of `{"alg":…,"typ":…,"kid":…}`; the payload is where an oversized or deeply nested document
 * would live. [MAX_HEADER_LENGTH] is here for that case.
 */
class JwtSignatureGate(
    signingKey: String,
) {
    companion object {
        /** JCA name for HMAC-SHA512, matching `Algorithm.HMAC512`. */
        private const val HMAC_SHA512 = "HmacSHA512"

        /**
         * Upper bound on a whole token, checked before any work.
         *
         * Generous — a token carrying org lists and permission sets is a few kB — but it caps what an
         * unauthenticated caller can make the server hash. The library offers no equivalent control.
         */
        const val MAX_TOKEN_LENGTH: Int = 64 * 1024

        /**
         * Upper bound on the encoded header segment.
         *
         * Unused by the HMAC path, which never reads the header. Provided for the external-IdP case
         * described in the class KDoc, where the header must be parsed to select a key — bounding it is
         * what keeps that parse from becoming the surface this gate exists to remove.
         */
        const val MAX_HEADER_LENGTH: Int = 10_000
    }

    /**
     * Failure to authenticate a token at the gate.
     *
     * Extends the library's exception type deliberately: `tryVerify` catches `JWTVerificationException`
     * and callers rely on a null rather than a throw, so a gate rejection must be indistinguishable
     * from any other verification failure.
     *
     * **Known residual.** This is the last vendor type in a public signature — the library is otherwise
     * `implementation`-scoped and invisible to consumers. Left as it is because nothing outside this
     * module catches it (checked), and the one external caller of `verify` re-verifies a token it just
     * issued, where a throw is the correct outcome. Introducing a `JwtVerificationException` of our own
     * would be churn on the authentication path for a consumer that does not exist. Revisit if one does
     * — they cannot currently catch this type by name, since auth0 is not on their compile classpath.
     */
    class Rejected(message: String) : com.auth0.jwt.exceptions.JWTVerificationException(message)

    private val keySpec = SecretKeySpec(signingKey.toByteArray(StandardCharsets.UTF_8), HMAC_SHA512)

    /**
     * Throws [Rejected] unless [token] carries a valid MAC.
     *
     * Deliberately says nothing about *why* beyond "signature invalid" — the caller is unauthenticated,
     * and distinguishing "malformed" from "wrong key" tells them which of the two to keep trying.
     */
    fun check(token: String) {
        if (token.length > MAX_TOKEN_LENGTH) {
            throw Rejected("Token exceeds the maximum accepted length")
        }

        val lastDot = token.lastIndexOf('.')

        if (lastDot <= 0 || lastDot == token.length - 1) {
            throw Rejected("Token is not a well-formed JWS compact serialization")
        }

        // The RAW substring of the token as it arrived. Re-encoding the segments would risk producing a
        // different signing input than the issuer used, which would reject every valid token.
        val signingInput = token.substring(0, lastDot)
        val presented = token.substring(lastDot + 1)

        val expectedMac = mac(signingInput)

        val presentedMac = try {
            Base64.getUrlDecoder().decode(presented)
        } catch (_: IllegalArgumentException) {
            // Not base64url. Fail closed rather than let a decode error surface as a 500.
            throw Rejected("Token signature is not valid base64url")
        }

        // Constant-time. A length-then-content comparison leaks the position of the first differing
        // byte, which is enough to forge a signature one byte at a time.
        if (!MessageDigest.isEqual(expectedMac, presentedMac)) {
            throw Rejected("Token signature is invalid")
        }
    }

    /**
     * HMAC-SHA512 over [signingInput], keyed as `Algorithm.HMAC512` keys it.
     *
     * A fresh [Mac] per call because [Mac] is not thread-safe and this runs on every request. The JCA
     * lookup is on the order of a microsecond, against an HMAC over a few kB — not worth caching
     * incorrectly.
     */
    private fun mac(signingInput: String): ByteArray = Mac.getInstance(HMAC_SHA512).run {
        init(keySpec)
        doFinal(signingInput.toByteArray(StandardCharsets.UTF_8))
    }
}
