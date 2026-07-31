package io.peekandpoke.ultra.security.jwt

import kotlinx.serialization.Serializable

@Serializable
data class JwtConfig(
    /**
     * The signing keys — **the first one signs, all of them verify**.
     *
     * A list rather than a single key so keys can be rotated without invalidating tokens that are
     * still within their expiry. Order is authoritative and deliberately not derived from
     * [JwtSigningKey.issued], so that editing a date cannot silently change which key signs.
     *
     * ### ORDER IS THE ROTATION. Nothing warns you if you get it wrong.
     *
     * Appending a new key instead of prepending it is a **silent no-op**: the config gains a fresh
     * key with a fresh `issued` date, boots without complaint, and goes on signing with the old one.
     * Boot validation cannot catch this — it cannot tell an appended new key from phase 1 of the
     * rolling rotation below, which is the same shape on purpose. Check `kid` on a freshly minted
     * token after any rotation; that is the only proof.
     *
     * ### Graceful rotation, single node
     *
     * Prepend the new key, deploy, and drop the old one once no token signed under it can still be
     * within its expiry.
     *
     * ### Graceful rotation, a fleet (rolling deploy)
     *
     * Two phases, no code, no logouts. One phase is not enough: mid-rollout, updated nodes would sign
     * with a `kid` the not-yet-updated nodes reject as unknown, so users bounce to anonymous
     * depending on which node they land on.
     *
     * 1. Append the new key — `[old, new]`. Every node can now VERIFY it; `old` still signs.
     * 2. Once the whole fleet is on phase 1, reorder to `[new, old]` to promote it.
     *
     * ### COMPROMISED key — rotation does NOT contain it. Everyone gets logged out.
     *
     * **Remove the key immediately.** A grace period does not merely delay containment, it gives it
     * away completely, because **refresh launders a token onto the new key**:
     *
     * 1. The attacker holds a token forged with the leaked key.
     * 2. You prepend a new key. The leaked one stays, so existing sessions survive — and so does the
     *    attacker's token, because the gate MACs under whatever key the `kid` names and has no
     *    notion of a key being retired.
     * 3. The attacker calls `refreshToken`. It authenticates on the token alone, reloads the user,
     *    and mints a replacement **signed with the new key**.
     * 4. You drop the leaked key. Every honest session is fine. So is the attacker's, now indefinitely
     *    renewable, and no longer distinguishable from a legitimate one.
     *
     * So there is no version of a graceful rotation that contains a compromise. Removing the key at
     * once is the only option, and it logs everyone out — which is the honest cost, not a failure.
     *
     * A `verifyOnly` flag would NOT help, and is worth not adding for that reason: the attacker's
     * problem is minting tokens *we accept*, and a verify-only key still verifies them.
     *
     * **The real fix is session revocation, not key rotation** — a per-session id in the token,
     * checked against a store, so individual sessions die without touching the keys. The storage half
     * already exists and is unwired; see `.claude/tasks/20260728-session-revocation-wiring.md`. Until
     * it lands, key removal is the only revocation this system has, and its granularity is "everyone".
     *
     * Validated at boot by `JwtGenerator.requireUsableConfig` — non-empty, unique ids, no blank id,
     * each secret long enough for its algorithm.
     */
    val keys: List<JwtSigningKey>,
    /** The issuer to be applied to the tokens */
    val issuer: String,
    /** The audience to be applied to the tokens */
    val audience: String,
    /** The namespace for permissions */
    val permissionsNs: String,
    /** The namespace for user data */
    val userNs: String,
) {
    // No hand-written redacting toString(): [Redacted] redacts itself, so the generated one is safe.
    // The previous hand-written one covered ONLY toString and looked like protection while every
    // serializer wrote the key in full — which is how it reached every insights record.
}
