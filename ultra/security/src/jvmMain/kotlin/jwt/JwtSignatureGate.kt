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
 * Since `java-jwt` was removed (2026-07-31) this gate IS the signature validation. [JwtGenerator.verify]
 * then parses the authenticated payload and validates the registered claims (`exp`, `nbf`, `iat`, `iss`,
 * `aud`) against the contract measured from the library before it went.
 *
 * ### What is deliberately NOT performed
 *
 * The header is never decoded, so **RFC 7515 §5.2 steps 2, 3 and 5 do not run** — no header parse, no
 * "is it valid UTF-8 JSON", no rejection of unsupported `crit` parameters (§4.1.11); nor does RFC 7519
 * §7.2's `cty`/nested-JWT branch. These are skipped, not relocated. That is safe here for one reason
 * only: **the header is inside the signing input**, so nobody but the signing-key holder can put
 * anything in it, and a `crit` or `cty` header would fail closed at [JwtGenerator]'s decode step
 * regardless. An earlier version of this KDoc claimed "every RFC 7519 step still runs" — it does not,
 * and that sentence is exactly what a future reader would have leaned on when extending this gate.
 *
 * Two consequences of never reading the header, both accepted deliberately:
 *
 * - The library rejected a valid-MAC token whose header names a different algorithm
 *   (`AlgorithmMismatchException`); here it verifies. Only the key holder can produce one, and the key
 *   holder can mint arbitrary valid tokens anyway.
 * - `typ` is not checked either (RFC 8725 §3.11, explicit typing). **This rests on an invariant that is
 *   true today and is not enforced: this signing key signs exactly ONE kind of JWT.** Org-selection,
 *   activation and password-reset tokens are `SecureRandom` database rows, not JWTs. The day a second
 *   kind of JWT is minted under this key, `iss` and `aud` are the only separators — and `createJwt`
 *   hard-codes both identically for every token — so that second kind would be accepted as a session
 *   bearer token. Introducing one means adding a `typ` check at the same time.
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
 * would live. That cap belongs to whoever writes such a verifier — a published constant no code reads
 * would advertise a protection this class does not perform.
 */
class JwtSignatureGate(
    signingKey: String,
) {
    companion object {
        /** JCA name for HMAC-SHA512 — the algorithm this issuer is fixed to (formerly `Algorithm.HMAC512`). */
        private const val HMAC_SHA512 = "HmacSHA512"

        /**
         * Upper bound on a whole token, checked before any work.
         *
         * Generous — a token carrying org lists and permission sets is a few kB — but it caps what an
         * unauthenticated caller can make the server hash. The replaced library offered no such control.
         */
        const val MAX_TOKEN_LENGTH: Int = 64 * 1024

        /**
         * Minimum signing-key size in bytes — RFC 7518 §3.2 makes this a MUST for HMAC.
         *
         * *"A key of the same size as the hash output (for instance, 256 bits for HS256) or larger MUST
         * be used with this algorithm."* SHA-512 outputs 64 bytes, so that is the floor.
         *
         * It matters because there is **no key-derivation function** between the configured string and
         * the MAC: the bytes are used as key material directly, so guessing costs one HMAC per attempt
         * with no work factor to hide behind. A single captured token is then an offline oracle, and
         * recovering the key is a total compromise of the issuer.
         *
         * **Length is a proxy for entropy, not a guarantee.** A 64-character English passphrase clears
         * this bar with perhaps 40 bytes of real entropy. This catches `changeme`, not a long weak
         * secret. Generate keys with a CSPRNG.
         */
        const val MIN_SIGNING_KEY_BYTES: Int = 64

        /**
         * Throws unless [signingKey] is usable, with a message that says how to fix it.
         *
         * Exposed so a host can check at BOOT. `JwtGenerator` is bound lazily in the kontainer, so
         * without an eager call the first failure is a 500 on a live server rather than a refusal to
         * start — see `FunktorRestBuilder.jwt`.
         */
        fun requireUsableSigningKey(signingKey: String) {
            val size = signingKey.toByteArray(StandardCharsets.UTF_8).size

            require(size >= MIN_SIGNING_KEY_BYTES) {
                "The JWT signing key is $size bytes; HMAC-SHA512 requires at least " +
                        "$MIN_SIGNING_KEY_BYTES (RFC 7518 §3.2). A shorter key is brute-forceable " +
                        "offline from a single captured token, because the key is used directly as MAC " +
                        "key material with no KDF to slow guessing down. Generate one with " +
                        "`openssl rand -base64 64` and set it as the JWT signingKey."
            }
        }
    }

    /**
     * Failure to authenticate a token at the gate.
     *
     * A subtype of [JwtVerificationException] so `tryVerify` treats a gate rejection like any other
     * verification failure — callers rely on a null rather than a throw.
     */
    class Rejected(message: String) : JwtVerificationException(message)

    private val keySpec: SecretKeySpec

    init {
        // Fails at construction rather than at first use. `SecretKeySpec` already rejected a zero-length
        // key with "Empty key", but that surfaced as a 500 on the first request carrying a bearer token,
        // because the kontainer singleton is lazy. `funktorRest { jwt() }` calls the same check at boot.
        requireUsableSigningKey(signingKey)

        keySpec = SecretKeySpec(signingKey.toByteArray(StandardCharsets.UTF_8), HMAC_SHA512)
    }

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
     * HMAC-SHA512 over [signingInput], keyed exactly as `Algorithm.HMAC512` keyed it.
     *
     * Internal because it is ALSO the signing primitive: [JwtGenerator.sign] MACs with this same
     * method, so issuer and verifier cannot drift apart on key bytes or charset.
     *
     * A fresh [Mac] per call because [Mac] is not thread-safe and this runs on every request. The JCA
     * lookup is on the order of a microsecond, against an HMAC over a few kB — not worth caching
     * incorrectly.
     */
    internal fun mac(signingInput: String): ByteArray = Mac.getInstance(HMAC_SHA512).run {
        init(keySpec)
        doFinal(signingInput.toByteArray(StandardCharsets.UTF_8))
    }
}
