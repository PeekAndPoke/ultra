# ULTRA-00178: JWT rotation: make it operable, plus two config loose ends

> **Status (2026-11-06): PAUSED, waiting on the maintainer** for the two decisions below; the parts that needed none are
> built (2026-11-06, "tackle all auth tickets that you can work on where no additional input is needed"). Earlier
> status: **(2026-08-01): NOT STARTED.** created 2026-08-01 on archiving ULTRA-00176 (jwt kid key rotation)
>
> **Part of:** ULTRA-00248 (Auth and sessions epic)

**Plan:** none — this is the actionable residue of that task's OPEN section (items 2, 4, 5)
**Security-critical:** partly — nothing here changes the verify path; the value is in making a
misconfiguration visible instead of silent.

**Priority: below ULTRA-00136 (session revocation wiring).** Rotation turned out to be
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

- [x] Log at boot, once, which key will sign: `kid`, `alg`, and `issued` if present. `FunktorRestBuilder.jwt()`
      (`funktor/rest/src/jvmMain/kotlin/index_jvm.kt`) is where the eager validation already runs, and
      it has the kontainer in scope — check what logger is reachable there before assuming one is.
- [x] Never log any part of the secret, not even a prefix or a length. The key id is not sensitive;
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

- [x] **A committed AWS Access Key ID** — `funktor-demo/server/src/main/resources/config/application.common.conf:37`
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

## Done 2026-11-06

- `funktor/rest/.../JwtSigningKeyOnAppStarting.kt`: an `OnAppStarting` hook that `FunktorRestBuilder.jwt()` registers;
  it logs `JWT: new tokens are signed with kid '<id>' (<alg>[, issued <date>]); <n> key(s) verify` through the
  kontainer's `Log` (silent without one). `JwtBootValidationSpec` pins the line (the FIRST key, no secret, no prefix,
  no length), `JwtSigningKeyBootLogSpec` (funktor-all) that a real app registers it; mutants K1 (the last key), K2
  (not registered) red.
- `funktor-demo/server/.../application.common.conf`: `accessKeyId = ${AWS_SES_ACCESS_KEY}`. **The maintainer's local
  `keys.env.conf` must set `AWS_SES_ACCESS_KEY` to the real key id** (the value removed from the conf): unset, the
  demo does not boot; set to `""` (as the template has it), it boots and fails at the first mail through SES. The old
  value stays in git history; only rotating or deactivating that IAM access key at AWS retires it (the maintainer's
  call; the id alone grants nothing, it names the account and principal).

## Review record

Round 1 (level 1, `reviewer-high`, two charters by risk: implementation, security; 2026-11-06): both CLEAN.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (impl; security noted it) | `JwtSigningKeyOnAppStarting.kt:22` | Nothing checks the hook LOGS (deleting or downgrading the call stays green) | fixed | `announce()`, called by `onAppStarting`; a spec with a capturing `Log` asserts one INFO line; the wiring with a null `Log` stays untested (the app's real `Log` cannot be captured from the spec) |
| 1 | MINOR (impl) | the ticket's note | An empty `AWS_SES_ACCESS_KEY` boots and fails at the first SES mail | fixed | The note says so |
| 1 | MINOR (security) | the ticket's note | The key id stays in history; no remedy named | fixed | Rotation at AWS named as the maintainer's call |
| 1 | NIT (impl) | `JwtSigningKeyOnAppStarting.kt:31` | The enum name, not the header value | fixed | `alg.headerValue` |
| 1 | NIT (impl) | `:27` | `message` public for a test | fixed | `internal` |
| 1 | NIT (impl) | `JwtBootValidationSpec.kt:24` | Stale `requireUsableKeys` | fixed | |
| 1 | NIT (impl) | `:28` | The log reads the key apart from the generator | no change | Both close over the same `config` (`index_jvm.kt:121-122`); `JwtGenerator.signingKey` is internal to ultra/security |
| 1 | NIT (security) | `:31` | The key count tells a log reader a rotation is on | no change | No key attribute; useful to an operator |

**Verdict:** gate PASS after round 1 for the two built items; the ticket stays PAUSED on the two decisions below.

## Open for the maintainer (2026-11-06)

1. **Require `exp`?** (section 3) Overturns an earlier decision, so it is yours. Recommendation: yes; this issuer
   always writes `exp`, so it costs no valid token, and a future mis-mint becomes a rejection instead of an eternal
   credential.
2. **A key-age warning on `JwtSigningKey.issued`?** (section 2) Recommendation: yes, a boot WARNING (never a refusal)
   past a configurable age, default 180 days, in `jvmMain` where a date parser is at hand.
- Not planned unless asked: a CLI or gradle task to generate keys (the docs' `openssl` line covers it), and the
  insights app-config slice showing the signing `kid` (a "consider" item).

## Out of scope

The `kid` timing oracle (an unknown `kid` returns before the MAC, a known one after it). Deliberately
accepted, reasoning recorded in `JwtSignatureGate`'s KDoc, and the attack is collected in
ULTRA-00172 (redteam jwt own verifier) scenario 20 for measurement in a pen-test
session rather than speculative hardening here.

## History

- 2026-10-21: numbered ULTRA-00178 (was `docs/tasks/20260801-jwt-rotation-operability.md`).
