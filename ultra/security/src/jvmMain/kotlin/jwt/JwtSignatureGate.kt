package io.peekandpoke.ultra.security.jwt

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Authenticates a token's MAC **before the payload is parsed**, using the key its header names.
 *
 * ### Why this exists
 *
 * A JWT library decodes the header and payload JSON before it checks the signature. Measured against
 * `java-jwt` 4.5.2: a token with a malformed payload and an invalid signature raises a *decode* error,
 * not a signature error, and a 20 MB string payload is fully parsed and allocated before the signature
 * check fails. That code runs on **every request carrying an `Authorization: Bearer` header**, valid or
 * not, so the parser is unauthenticated attack surface.
 *
 * This gate removes the part that matters: the **payload** — where an oversized or deeply nested
 * document would live — is never handed to a parser until its MAC has checked out.
 *
 * ### What IS parsed before the MAC, and why that is not the same thing
 *
 * Since key rotation was introduced the **header** is parsed first, because the key cannot be selected
 * without it. That is the ordinary shape of a verifier that supports more than one key, and this class
 * bounds it deliberately:
 *
 * - the whole token is capped at [MAX_TOKEN_LENGTH] before any work,
 * - the header **segment** is capped at [MAX_HEADER_LENGTH] *before it is base64-decoded*, so the
 *   parser is handed at most a few hundred bytes — a real header is around a hundred,
 * - it is parsed into [JwtHeader], a fixed three-field shape, not a free-form `JsonObject`.
 *
 * So the unauthenticated parser surface is a bounded, fixed-shape read of a small header, and the
 * unbounded part still runs only on authenticated bytes. An earlier version of this class parsed
 * nothing at all, because the algorithm was fixed and there was a single key; that claim no longer
 * holds and has been rewritten rather than patched.
 *
 * ### Why it is allowed to reorder the steps
 *
 * RFC 7515 §5.2: *"The order of the steps is not significant in cases where there are no dependencies
 * between the inputs and outputs of the steps."* Signature validation consumes only
 * `ASCII(BASE64URL(header) || '.' || BASE64URL(payload))` and the signature octets, so it has no
 * dependency on the parsed **payload** and may run first.
 *
 * The same section: *"unless the algorithm(s) used in the JWS are acceptable to the application, it
 * SHOULD consider the JWS to be invalid."* That is what [JwtAlgorithm] and the check below implement.
 *
 * ### The header's `alg` is compared, never obeyed
 *
 * The algorithm comes from the **configured key**, and the header's `alg` is only checked to agree
 * with it. This is the inverse of the classic algorithm-confusion bug: `alg: none` and `alg: HS256`
 * do not select anything, they simply disagree with the key and the token dies — and even if the
 * check were removed, the MAC would still run under the key's own algorithm.
 *
 * ### `kid` is required, and an unknown one is not retried
 *
 * A token that names no key, or names one that is not configured, is rejected outright. Verifying
 * against every configured key in turn would make a forged token cost one HMAC **per key** — a
 * self-inflicted amplification on an unauthenticated path — and would hide which key actually
 * authenticated a request. `kid` is attacker-controlled and is used for exactly one thing: a lookup
 * in the fixed map built from configuration.
 *
 * A caller can tell "unknown kid" from "bad signature" by timing: an unknown one returns before the
 * MAC, a known one after it, and the gap is an HMAC over a signing input the caller sizes. The
 * messages are identical (asserted by a spec), the timing is not.
 *
 * **Accepted, but not for the obvious reason.** "kid values are public, they ride in every issued
 * token" only covers the confidentiality of the id itself. What the oracle actually leaks is *which
 * keys are still in the verify list* — the reconnaissance step before trying a leaked retired key,
 * and a reason to remove a compromised key rather than retire it gracefully (see [JwtConfig.keys]).
 * Closing it would mean MACing against a dummy key on the unknown-`kid` path; that is a real option,
 * deliberately not taken, because it makes every forged token cost an HMAC and the leak is only
 * useful to an attacker who already holds a key's secret.
 *
 * ### Explicit typing, and the invariant it does NOT enforce
 *
 * `typ` must be `JWT` (RFC 8725 §3.11). Be precise about what that buys: it rejects a token minted by
 * some *other* system that shares this secret and types its tokens differently — an `at+jwt` access
 * token, say. It does **not** separate two kinds of JWT minted by this issuer, because [createJwt]
 * writes `JWT` for all of them.
 *
 * So the real invariant stands, unenforced: **these signing keys sign exactly ONE kind of JWT.**
 * Org-selection, activation and password-reset tokens are `SecureRandom` database rows, not JWTs
 * (verified repo-wide: [createJwt] is the only mint path). The day a second kind is minted under
 * these keys, `iss` and `aud` are the only separators — and `createJwt` hard-codes both identically
 * for every token — so that second kind would be accepted as a session bearer token. Introducing one
 * means giving it a DISTINCT `typ` and making the expected value a parameter here, not merely
 * "adding a typ check".
 *
 * ### What is deliberately NOT performed
 *
 * RFC 7519 §7.2's `cty`/nested-JWT branch is not taken, and unknown header members are ignored. Safe
 * here for one reason only: **the header is inside the signing input**, so nobody but a key holder
 * can put anything in it. `crit` (RFC 7515 §4.1.11) used to fall in this bucket and no longer does —
 * it is now rejected outright, because that is what the RFC's MUST requires and it costs one field.
 *
 * ### PRECONDITION — read before reusing this
 *
 * This still assumes **this application issues the tokens**: the key is looked up in local
 * configuration, not fetched from an issuer's JWKS endpoint. A verifier for an external identity
 * provider (Keycloak, or any OIDC issuer whose access token is used directly as the bearer token)
 * needs key discovery and asymmetric algorithms, neither of which is here. The rule it should keep is
 * the one this class is built around: *parse only the header, hard-capped, then verify, then parse
 * the payload.*
 */
class JwtSignatureGate(
    keys: List<JwtSigningKey>,
) {
    companion object {
        /**
         * Upper bound on a whole token, checked before any work.
         *
         * Generous — a token carrying org lists and permission sets is a few kB — but it caps what an
         * unauthenticated caller can make the server hash. The replaced library offered no such control.
         */
        const val MAX_TOKEN_LENGTH: Int = 64 * 1024

        /**
         * Upper bound on the **encoded** header segment, in characters, checked before it is decoded.
         *
         * Encoded rather than decoded because that is the only length available before the work it is
         * meant to bound. 1024 base64url characters cap the parser's input at 768 bytes; the header
         * this issuer writes is around 60.
         *
         * This constant existed once before as documentation and was deleted for advertising a
         * protection no code performed. It is back because code performs it now.
         */
        const val MAX_HEADER_LENGTH: Int = 1024

        /**
         * Parser for the ONE structure read before authentication. Strict about JSON, lenient about
         * members: extra header parameters are legal (RFC 7515 §4) and only a key holder can add any.
         *
         * `Json` is immutable and thread-safe, so one shared instance is correct on a request path.
         */
        private val headerJson = Json { ignoreUnknownKeys = true }

        /**
         * Throws unless [keys] can be used to sign and verify, with messages that say how to fix it.
         *
         * Exposed so a host can check at BOOT. `JwtGenerator` is bound lazily in the kontainer, so
         * without an eager call the first failure is a 500 on a live server rather than a refusal to
         * start — see `FunktorRestBuilder.jwt`.
         */
        fun requireUsableKeys(keys: List<JwtSigningKey>) {
            require(keys.isNotEmpty()) {
                "The JWT configuration has no signing keys. At least one is required, and the FIRST " +
                        "one signs. Add e.g. `keys = [{ id = \"1\", secret = \"<openssl rand -base64 " +
                        "64>\" }]`."
            }

            val duplicates = keys.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys

            require(duplicates.isEmpty()) {
                "The JWT signing key ids ${duplicates.sorted()} are used more than once. A `kid` " +
                        "selects exactly one key, so duplicates mean a token cannot be attributed to " +
                        "the key that signed it. Give every key a distinct id."
            }

            keys.forEach { requireUsableSigningKey(it) }
        }

        /**
         * Throws unless [key] is usable, with a message that says how to fix it.
         *
         * The size floor is RFC 7518 §3.2: *"A key of the same size as the hash output (for instance,
         * 256 bits for HS256) or larger MUST be used with this algorithm."*
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
        fun requireUsableSigningKey(key: JwtSigningKey) {
            require(key.id.isNotBlank()) {
                "A JWT signing key has a blank id. The id is the `kid` written into every token and " +
                        "the value a verifier looks up, so it must be set."
            }

            val size = key.secret.value.toByteArray(StandardCharsets.UTF_8).size

            require(size >= key.alg.minKeyBytes) {
                "The JWT signing key '${key.id}' is $size bytes; ${key.alg.headerValue} requires at " +
                        "least ${key.alg.minKeyBytes} (RFC 7518 §3.2). A shorter key is " +
                        "brute-forceable offline from a single captured token, because the key is " +
                        "used directly as MAC key material with no KDF to slow guessing down. " +
                        "Generate one with `openssl rand -base64 ${key.alg.minKeyBytes}` and set it " +
                        "as that key's secret."
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

    /** A configured key with its JCA key material built once, rather than per request. */
    private class Prepared(val key: JwtSigningKey, val spec: SecretKeySpec)

    private val prepared: Map<String, Prepared>

    /**
     * The key new tokens are signed with: the first configured one.
     *
     * Internal, not public: it carries the secret, and nothing outside this module needs it. A caller
     * that wants to know which key is signing wants the id, and can be given that when one asks.
     */
    internal val signingKey: JwtSigningKey

    init {
        // Fails at construction rather than at first use. `funktorRest { jwt() }` calls the same check
        // at boot, because the kontainer singleton is lazy and would otherwise defer every one of
        // these failures to the first request carrying a bearer token, on a running server.
        requireUsableKeys(keys)

        prepared = keys.associate { key ->
            key.id to Prepared(
                key = key,
                spec = SecretKeySpec(key.secret.value.toByteArray(StandardCharsets.UTF_8), key.alg.jcaName),
            )
        }

        signingKey = keys.first()
    }

    /**
     * Throws [Rejected] unless [token] carries a valid MAC under the key its header names.
     *
     * Deliberately says little about *why* — the caller is unauthenticated, and a precise reason tells
     * them which part to keep trying.
     */
    fun check(token: String) {
        if (token.length > MAX_TOKEN_LENGTH) {
            throw Rejected("Token exceeds the maximum accepted length")
        }

        val firstDot = token.indexOf('.')
        val lastDot = token.lastIndexOf('.')

        // Exactly two dots, and no empty segment. `indexOf` from just past the first dot finds the
        // SECOND one, so requiring it to be the last is what pins the segment count at three.
        if (firstDot <= 0 ||
            lastDot <= firstDot + 1 ||
            lastDot == token.length - 1 ||
            token.indexOf('.', firstDot + 1) != lastDot
        ) {
            throw Rejected("Token is not a well-formed JWS compact serialization")
        }

        if (firstDot > MAX_HEADER_LENGTH) {
            throw Rejected("Token header exceeds the maximum accepted length")
        }

        val header = decodeHeader(token.substring(0, firstDot))

        // Explicit typing, RFC 8725 §3.11. Case-insensitive because `typ` is a MEDIA TYPE (RFC 7519
        // §5.1) — deliberately unlike `alg` below, whose values are exact strings from the JWA
        // registry and are compared case-sensitively.
        if (!header.typ.equals("JWT", ignoreCase = true)) {
            throw Rejected("Token header is not valid")
        }

        // RFC 7515 §4.1.11 is a MUST: reject a JWS whose `crit` names an extension we do not
        // understand. We understand none, so any `crit` at all rejects.
        if (header.crit != null) {
            throw Rejected("Token header is not valid")
        }

        // An absent kid is an unknown kid: both mean there is no key to verify against. Never fall
        // back to trying them all — see the class KDoc.
        val entry = header.kid?.let { prepared[it] }
            ?: throw Rejected("Token signature is invalid")

        // The KEY's algorithm is authoritative; the header only has to agree with it. Comparing the
        // parsed enum rather than the raw string means an unknown `alg` cannot match by accident.
        if (JwtAlgorithm.byHeaderValue(header.alg) != entry.key.alg) {
            throw Rejected("Token signature is invalid")
        }

        // The RAW substring of the token as it arrived. Re-encoding the segments would risk producing a
        // different signing input than the issuer used, which would reject every valid token.
        val signingInput = token.substring(0, lastDot)
        val presented = token.substring(lastDot + 1)

        // Compare the ENCODED signatures, not the decoded bytes.
        //
        // Both authenticate equally — the encoding is a bijection on canonical input — but comparing
        // the encoded form additionally requires the token to be in CANONICAL base64url, and that is
        // worth having. `Base64.getUrlDecoder()` accepts padding and non-zero trailing bits, and an
        // HS512 signature is 86 characters carrying 4 unused bits, so decoding-then-comparing admits
        // 32 distinct token STRINGS per logical token (16 final characters x padded/unpadded —
        // measured, not reasoned). Any denylist, cache key, rate limiter or audit dedup keyed on the
        // raw token string would be bypassable by re-encoding. RFC 7515 mandates unpadded base64url,
        // so nothing standards-compliant is lost; our issuer and `java-jwt` both emit canonical form,
        // which the wire-compat fixtures prove.
        //
        // It also removes the decode step entirely, and with it an `IllegalArgumentException` path
        // that `tryVerify` does NOT catch — it catches only JwtVerificationException — so a
        // non-base64url signature would have surfaced as a 500 on the unauthenticated path had the
        // guard around it ever been removed. No guard is safer than a guarded hazard.
        val expected = base64Url(mac(signingInput, entry))

        // Constant-time. A length-then-content comparison leaks the position of the first differing
        // byte, which is enough to forge a signature one byte at a time.
        if (!MessageDigest.isEqual(expected.toByteArray(StandardCharsets.UTF_8), presented.toByteArray(StandardCharsets.UTF_8))) {
            throw Rejected("Token signature is invalid")
        }
    }

    /**
     * Decodes and parses the header segment, which has already been length-capped by [check].
     *
     * Every failure is the same [Rejected]: this runs on unauthenticated input, and telling a caller
     * whether their header was bad base64, bad UTF-8 or bad JSON only helps them iterate.
     */
    private fun decodeHeader(segment: String): JwtHeader = try {
        headerJson.decodeFromString(
            JwtHeader.serializer(),
            String(Base64.getUrlDecoder().decode(segment), StandardCharsets.UTF_8),
        )
    } catch (_: IllegalArgumentException) {
        throw Rejected("Token header is not valid")
    } catch (_: SerializationException) {
        throw Rejected("Token header is not valid")
    }

    /**
     * HMAC over [signingInput] under [key], which must be one of the configured keys.
     *
     * Internal because it is ALSO the signing primitive: [JwtGenerator.sign] MACs with this same
     * method, so issuer and verifier cannot drift apart on key bytes, algorithm or charset.
     *
     * A fresh [Mac] per call because [Mac] is not thread-safe and this runs on every request. The JCA
     * lookup is on the order of a microsecond, against an HMAC over a few kB — not worth caching
     * incorrectly.
     */
    internal fun mac(signingInput: String): ByteArray = mac(signingInput, prepared.getValue(signingKey.id))

    private fun mac(signingInput: String, entry: Prepared): ByteArray =
        Mac.getInstance(entry.key.alg.jcaName).run {
            init(entry.spec)
            doFinal(signingInput.toByteArray(StandardCharsets.UTF_8))
        }

    private fun base64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
