# JWT rotation: make it operable, plus two config loose ends

**Status:** TODO — created 2026-08-01 on archiving `.claude/tasks-archive/2026-07/20260731-jwt-kid-key-rotation.md`
**Plan:** none — this is the actionable residue of that task's OPEN section (items 2, 4, 5)
**Security-critical:** partly — nothing here changes the verify path; the value is in making a
misconfiguration visible instead of silent.

**Priority: below `.claude/tasks/20260728-session-revocation-wiring.md`.** Rotation turned out to be
hygiene on a schedule, not incident response — it cannot contain a compromised key, because refresh
launders a forged token onto the new one. Session revocation is what actually fixes that.

## 1. A mis-ordered rotation is invisible

`keys.first()` signs. Appending a new key instead of prepending it boots clean, reports a fresh
`issued` date, and goes on signing with the old key. Today the only way to notice is to decode a
freshly minted token.

**A boot check cannot fix this** — and this is the part worth not re-deriving: a check requiring the
first key to be the newest would break the two-phase rolling rotation, whose phase 1 *deliberately*
keeps the older key first while the new one is distributed for verification. The reviewer who
proposed that check had not read the reviewer who proposed the procedure.

So the fix is visibility, not validation:

- [ ] Log at boot, once, which key will sign: `kid`, `alg`, and `issued` if present. `FunktorRestBuilder.jwt()`
      (`funktor/rest/src/jvmMain/kotlin/index_jvm.kt`) is where the eager validation already runs, and
      it has the kontainer in scope — check what logger is reachable there before assuming one is.
- [ ] Never log any part of the secret, not even a prefix or a length. The key id is not sensitive;
      nothing else about a key belongs in a log line.
- [ ] Consider surfacing the same thing through the insights app-config slice, which already redacts
      the secret via `Redacted<T>` — the signing `kid` is exactly the kind of thing an operator wants
      to confirm after a deploy.

## 2. Nothing generates a key, and nothing ages one out

- [ ] A documented one-liner is already the answer for generation (`openssl rand -base64 64`) and is
      going into the docs task. Decide whether a gradle task or CLI helper earns its keep on top of
      that — probably not.
- [ ] `JwtSigningKey.issued` exists as the hook for a "refuse or warn on keys older than N days"
      policy and is currently read by nothing. If this is wanted, a **warning** is the right shape,
      not a refusal: a hard failure on an old key turns a missed rotation into an outage.
- [ ] Note the parsing cost: `issued` is a `String?` in `commonMain`. Anything that reads it needs a
      date parser there, or the check has to live in `jvmMain`.

## 3. Loose ends from the same review

- [ ] **A committed AWS Access Key ID** — `funktor-demo/server/src/main/resources/config/application.common.conf:37`
      has `accessKeyId = "AKIA…"`. Not a secret on its own, but it names the account and principal and
      is what credential scanners key on; its matching secret is correctly gitignored. Same fix as the
      JWT key already got: `accessKeyId = ${AWS_SES_ACCESS_KEY}`, which `keys.env.conf.tpl` already
      declares. Pre-existing, LOW, one line.
- [ ] **DECISION — should `exp` be required?** RFC 7519 makes it OPTIONAL and the verifier currently
      treats an absent `exp` as "no expiry check", preserved deliberately from `java-jwt` and pinned by
      `JwtWireCompatSpec`'s no-exp fixture. This issuer always writes `exp` and the public builder
      cannot remove it, so requiring it would cost zero valid tokens while turning a future mis-mint
      from an eternal credential into a rejection. Overturning the earlier decision is the
      maintainer's call; if taken, the fixture and its comment must be rewritten, not deleted.

## Out of scope

The `kid` timing oracle (an unknown `kid` returns before the MAC, a known one after it). Deliberately
accepted, reasoning recorded in `JwtSignatureGate`'s KDoc, and the attack is collected in
`.claude/tasks/20260731-redteam-jwt-own-verifier.md` scenario 20 for measurement in a pen-test
session rather than speculative hardening here.
