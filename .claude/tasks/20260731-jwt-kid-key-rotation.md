# JWT `kid`-based key rotation

**Status:** GATE PASSED — implemented (`9fe2a21a`), review findings fixed (`493423b8`), 2026-07-31
**Plan:** `.claude/tasks/20260718-jwt-lib-consolidation-options.md` → the AMENDED section
**Security-critical:** yes → red-team scenarios collected in
`.claude/tasks/20260731-redteam-jwt-own-verifier.md` section F

## Spec

Replace the single JWT signing key with a `kid`-selected key list, so keys can be rotated.

- [x] `JwtConfig.signingKey: Redacted<String>` → `JwtConfig.keys: List<JwtSigningKey>`
- [x] `JwtSigningKey(id, secret: Redacted<String>, alg: JwtAlgorithm = HS512, issued: String? = null)`
- [x] The FIRST key signs; every key verifies
- [x] `JwtAlgorithm` enum carrying `headerValue`, `jcaName`, `minKeyBytes` — HS512 only
- [x] Every issued token carries `kid`; a token without one is rejected (NO backward compatibility)
- [x] An unknown `kid` rejects outright — never trial-verify against every key
- [x] The header's `alg` is COMPARED against the key's, never used to select
- [x] The header is length-capped **before** base64-decoding and parsed into a fixed shape
- [x] Eager boot validation in `FunktorRestBuilder.jwt()`
- [x] Config files migrated to the list shape

## Implementation notes

**Decisions taken with the maintainer before writing code, not during.**

- **No backward compatibility.** *"no backward compat needed so we do not need to care about floating
  tokens."* This is what removes the entire legacy-fallback problem: `kid` is simply required. The
  cost is a one-time logout of every session on the deploy that ships it.
- **List order decides the signer, not `issued`.** `JwtSigningKey.issued` is metadata and the hook
  for a future key-age policy. Deriving the signer from a date would mean editing a date silently
  changes who signs — invisible in review, and a rotation footgun.
  `ultra/security/src/jvmMain/kotlin/jwt/JwtSignatureGate.kt:212`.
- **No trial verification.** Rejected by the maintainer (*"Checking all key in the list for a forged /
  broken token sounds wrong"*) and the reasoning holds: it makes a forged token cost one HMAC per
  configured key on an unauthenticated path, and it destroys the record of which key authenticated a
  request. `JwtSignatureGate.kt` — absent/unknown `kid` throws before any MAC runs.
- **One enum entry.** A weaker algorithm in `JwtAlgorithm` is a weaker algorithm selectable from a
  config file. Adding one later is a line plus a fixture.
- **The gate's central claim changed and was REWRITTEN, not patched.** It used to parse nothing before
  the MAC. It now parses the header, because a key cannot be selected without one. What survives is
  the part that mattered: the PAYLOAD — unbounded in size and shape — is still never handed to a
  parser until the MAC checks out. `MAX_HEADER_LENGTH` (1024, on the ENCODED segment) is back; it was
  deleted in `bed37cc7` for advertising a protection no code performed, and code performs it now.
- **Header member order is `kid, alg, typ`** — matching what java-jwt 4.5.2 emits, measured. JSON
  order carries no meaning and nothing depends on it; it buys one thing, a byte-identity assertion
  covering the ENCODER as well as the MAC. `JwtGenerator.kt:59`.
- **`signingKey` is `internal` on both `JwtGenerator` and `JwtSignatureGate`** — it carries the secret,
  and no caller outside the module needs it. A caller wanting to know which key is signing wants the
  id, and can be given that when someone asks.

## Test evidence

- [x] Unit/behaviour: `JwtSignatureGateSpec` (35 cases — kid selection, unknown/absent kid, alg
      mismatch across five header shapes, header cap above and below the boundary, malformed header,
      rotation across two deploys, "the first key signs" with later `issued` on the others, boot
      validation), `JwtClaimValidationSpec`, `JwtWireCompatSpec`, `JwtGeneratorSpec`, `ExtractUserSpec`
- [x] Boot wiring: `funktor/rest/src/jvmTest/kotlin/JwtBootValidationSpec.kt` — the blueprint is built
      but never resolved, so a check that only ran inside the lazy singleton factory would not fire
- [x] End-to-end: `:funktor:all:jvmTest` boots the real app from `application.test.conf`. This is what
      MEASURED that Slumber awakes the HOCON list-of-objects shape (enum + `Redacted<String>` leaf) —
      a unit probe would not have proved the config file itself loads.
- [x] Full commands green: `:ultra:security:jvmTest :ultra:common:jvmTest :ultra:slumber:jvmTest
      :funktor:core:jvmTest :funktor:rest:jvmTest :funktor:auth:jvmTest :funktor:all:jvmTest
      :funktor:insights:jvmTest :funktor:messaging:jvmTest :funktor-demo:server:test` →
      **3541 tests, 0 failures, 0 errors** after the review fixes (3488 before), counts from
      `build/test-results/**/TEST-*.xml`, not console output
- [x] Compile sweep clean: `compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs
      compileKotlin compileTestKotlin --continue`, no `^e:`

### Mutation evidence — round 1: 12 of 13 killed

| # | Mutation | Result |
|---|---|---|
| M1 | `kid` required → fall back to the signing key | KILLED (3) |
| M2 | unknown `kid` → try every key | KILLED (3) |
| M3 | header `alg` comparison removed | KILLED (1) |
| M4 | `MAX_HEADER_LENGTH` check removed | KILLED (1) |
| M5 | exactly-two-dots clause removed | KILLED (1) |
| M6 | empty key list allowed | KILLED (2) |
| M7 | duplicate ids allowed | KILLED (2) |
| M8 | blank id allowed | KILLED (1) |
| M9 | key-length floor removed | KILLED (2) |
| M10 | `MessageDigest.isEqual` → `contentEquals` | **SURVIVED** |
| M11 | signing key = LAST instead of FIRST | KILLED (2) |
| M12 | `kid` omitted from the signed header | KILLED (25) |
| M13 | eager boot check removed from `FunktorRestBuilder.jwt()` | KILLED (3) |

M10 is expected and pre-recorded: the two have identical accept/reject sets and differ only in
timing, so no test can observe it. Constant-time behaviour rests on review, as it did before.

**This round was incomplete, and reviewer 1 found what it missed.** M14 below — removing the
`catch (IllegalArgumentException)` around the signature decode — also survived, because no test
reached it: the row labelled "signature is not base64url" died one step earlier in `decodeHeader`,
its header segment being a single base64 character. The "12 of 13" claim in `9fe2a21a`'s message was
therefore wrong. A mutation suite only proves what it enumerates.

### Mutation evidence — round 2 (after the review fixes): 7 of 7 killed

| # | Mutation | Result |
|---|---|---|
| M14 | `typ` check removed | KILLED (2) |
| M15 | `typ` check made case-SENSITIVE | KILLED (1) |
| M16 | `crit` rejection removed | KILLED (1) |
| M17 | `crit` field dropped from the header shape | KILLED (1) |
| M18 | canonical compare → decode-and-compare (the old behaviour) | KILLED (36) |
| M19 | namespace overlap check removed | KILLED (1) |
| M20 | namespace check dropped from `JwtGenerator.init` | KILLED (1) — **survived at first**; the boot path was tested and the construction path was not, so it needed its own test |

### Defects the tests found

- **A placeholder that made five assertions vacuous.** `handMadeToken`'s default signature was the
  literal `"not-a-valid-signature"` — 21 characters, so not valid base64 at all. Every row using it
  died in the base64url decoder one step BEFORE the MAC comparison, silently testing the wrong thing.
  Caught only because the new rows asserted on the rejection MESSAGE, not just the type. Replaced
  with 64 zero bytes, encoded — well-formed and wrong.

## Review record (filled by /feature-review)

Three Fable reviewers, run in parallel over `9fe2a21a` plus the funktor auth chain. Every finding was
verified against the code before acting.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & correctness | PASS with findings | An **untested guard that survived mutation** (the signature-decode `catch`), a **rotted assertion**, a dead parameter, a namespace-overlap hole. No correctness defect in the gate itself — it enumerated the dot arithmetic exhaustively and found it sound. |
| 2. JWT domain (RFC 7515/7519/7518/8725) | PASS with findings | One MUST violation (`crit`), one SHOULD (`typ`), signature non-canonicality, and a rolling-deploy gap. Confirmed compare-not-select is structurally correct: no token can reach the MAC under an algorithm the key did not authorise. |
| 3. Security / adversarial | PASS with findings | **No token-forgery path against the gate survived.** What did: a committed key (pre-existing), a silent mis-rotation, the compromise-vs-graceful procedure gap, and the timing oracle's justification being weaker than stated. |

### Fixed in `493423b8`

| Finding | Action |
|---|---|
| `crit` accepted and ignored (RFC 7515 §4.1.11 MUST) | Rejected outright. A spec row had actively asserted the violation; flipped. |
| `typ` unchecked (RFC 8725 §3.11) | Required to be `JWT`, case-insensitively. **See the correction below — this does not close what the reviewer claimed.** |
| Signature non-canonical: 32 token strings per token | Compare ENCODED forms. Also deletes the untested decode guard rather than testing it. |
| Signature-decode `catch` unreachable by any test | Gone with the decode. It mattered: `tryVerify` catches only `JwtVerificationException`, so that path was a 500 on the unauthenticated path. |
| `shouldNotContain "testSigningKey"` rotted | Asserts against the real secret now. |
| Namespace overlap silently drops user claims | Refused at boot and at construction. |
| Dead `key` parameter on the MAC primitive | Dropped. |

### Where I disagreed with a reviewer

- **The `typ` fix does not close the "second kind of JWT" hole**, contrary to reviewer 2's framing. A
  second kind minted through `createJwt` gets `typ: "JWT"` too. It buys something narrower — rejecting
  a token from another system that shares the secret and types differently — and turns an unread field
  into a validated one. The KDoc states this rather than claiming the finding is closed.
- **Rejected the proposed `issued`-ordering boot check.** Reviewer 3 wanted `keys.first()` required to
  be the newest whenever every key has a parseable date. That would break the two-phase rolling
  rotation reviewer 2 recommended in the same round, whose phase 1 deliberately keeps the OLDER key
  first. Cross-referencing the two is what caught it. Documented the trap instead.
- **Reviewer 3's F3 is imprecise as written** ("a leaked key still SIGNS"). Prepending the new key
  means the old one does not sign. The real content — a retained compromised key keeps the
  *attacker's* forgeries verifying through the grace window — is true and is now documented, along
  with why a `verifyOnly` flag would not help.
- **The committed key is pre-existing** (commit `6c55d3db`), not introduced here.

**Red-team follow-up:** `.claude/tasks/20260731-redteam-jwt-own-verifier.md` — section F was added for
this change (kid injection, key enumeration by timing, rotation races, downgrade between configured
keys, pre-auth header-parser abuse, boot-validation gaps).

## Known and deliberate

- Every token issued before this stops verifying. Intended, one-time.
- No `typ` check (RFC 8725 §3.11). Rests on an unenforced invariant: these keys sign exactly ONE kind
  of JWT. Documented in `JwtSignatureGate`'s KDoc, including what breaks it.
- An unknown `kid` and a bad MAC are distinguishable by TIMING, by roughly one HMAC. Accepted: `kid`
  values ride in every issued token, so they are not secret.
- Rotation is not yet *operable* — nothing generates a key and nothing warns that one is old.
  `issued` is the hook. Worth its own task when someone actually has to rotate.


## OPEN — needs a maintainer decision, not fixed here

### 1. ~~A working superuser signing key is committed~~ — FIXED 2026-08-01  *(reviewer 3 F1, HIGH)*

**Resolved on the maintainer's instruction:** the key now lives in the gitignored `keys.env.conf`
and the committed configs use HOCON substitution, exactly like every other secret in that directory.

- `keys.env.conf.tpl` (tracked) gained `JWT_SIGNING_KEY=""` with the `openssl rand -base64 64`
  instruction and the RFC 7518 floor spelled out.
- `application.{dev,test}.conf` now read `secret = ${JWT_SIGNING_KEY}`.
- A **fresh** key was generated for the local `keys.env.conf`. The old one is public forever and is
  gone from the tree.
- `funktor/all/src/jvmTest/resources/config/application.test.conf` keeps a committed key — it is a
  jvmTest resource that never ships and hermetic tests need a fixed one — but it is now a
  self-describing string (`funktor-all-test-only-signing-key-do-not-use-anywhere-else-…`) instead of
  a random-looking blob anyone might mistake for a credential.

Verified: `:funktor:all:jvmTest` and `:funktor-demo:server:test` both green, which is what proves the
substitution resolves.

**Reviewer 3's second F6 claim was REFUTED.** It said `application.common.conf:1`'s
`include "keys.env"` silently no-ops because the file does not exist. Measured against
Typesafe Config 1.4.5: an extension-less include appends `.conf`, so it loads `keys.env.conf` **at
the root**, which is precisely what makes `${AWS_SES_SECRET_KEY}` — and now `${JWT_SIGNING_KEY}` —
resolve. My own first probe reproduced the reviewer's claim and was wrong: a relative `File` with no
parent directory changes how includes resolve. The line is load-bearing; do not delete it.

<details><summary>Original finding, kept for the record</summary>

`funktor-demo/server/src/main/resources/config/application.{dev,test}.conf` and
`funktor/all/src/jvmTest/resources/config/application.test.conf` all carry the same 172-byte secret.
**Pre-existing** — commit `6c55d3db`, not introduced by the rotation work — but the rotation work is
what put fresh eyes on it.

The two demo-server files are under `src/main/resources`, so the key ships inside the built jar.
Anyone with the repo can mint `isSuperUser = true` for any deployment started on the dev or test
profile. Boot validation is structurally unable to catch it: 172 bytes clears the 64-byte floor, and
the floor's own KDoc already says length is a proxy for entropy — a *published* key is worse than a
weak one, and nothing looks for it.

`application.common.conf` gets this right (`keys = []`, refuses to start). The AWS/SendGrid/Resend
secrets in the same directory are already handled properly via the gitignored `keys.env.conf`. The
JWT key is the odd one out.

Options, none free:
- **Move the dev key to `keys.env.conf`** (gitignored, matching the other secrets). Costs: the demo
  no longer runs out of the box; every developer generates a key first.
- **Keep a committed key for TEST only** — hermetic tests need a deterministic one — and remove it
  from dev. `funktor/all`'s copy is under `jvmTest/resources` and never ships, so it is fine as is.
- **Leave it and mark it loudly** in the config files as publicly known and unsafe for any real
  deployment. Cheapest, and defeats "the operator did not realise", but not the vulnerability.

Not changed unilaterally because it trades against local-dev ergonomics, which is the maintainer's
call.
</details>

### 2. A committed AWS Access Key ID *(reviewer 3 F6, LOW, pre-existing)*

`application.common.conf:37` commits `AKIA…`. Not a secret alone, but it names the account and
principal, and it is what credential scanners key on. Its matching secret is correctly gitignored —
the asymmetry is how the two eventually get reunited by a `git add -f`. Same fix as item 1 if wanted:
`accessKeyId = ${AWS_SES_ACCESS_KEY}`, which the template already declares.

### 2b. Rotation cannot contain a compromised key — refresh launders it *(maintainer, 2026-08-01)*

Found by the maintainer, not by any of the three reviewers, and it is the sharpest thing in this
document. An attacker holding a token forged with a leaked key calls `refreshToken` during the grace
window and receives a replacement **signed with the new key**; dropping the leaked key afterwards
achieves nothing. `AuthUserApi` has `authFloor = { authenticated() }`, and `AuthRealm.refreshToken`
(`funktor/auth/src/jvmMain/kotlin/AuthRealm.kt:389`) authenticates on the token alone — it reloads
the user and re-derives permissions from the DB, but never asks whether this session may still exist.

So **a compromise always means logging everyone out.** There is no graceful variant. Documented in
`JwtConfig.keys`, added to the red-team doc as scenario 26, and recorded in
`.claude/tasks/20260728-session-revocation-wiring.md` — which is the actual fix, and now the
highest-value item in this area. Rotation is hygiene on a schedule, not incident response.

### 3. The unknown-kid timing oracle *(reviewer 3 F5, LOW)*

Documented rather than closed, with the reasoning upgraded (it leaks which keys are still in the
verify list, not merely that kids exist). Closing it means MACing against a dummy key on the
unknown-`kid` path — one HMAC, not N, so it does not conflict with the no-trial-verification
decision. Deliberately not taken: it makes every forged token cost an HMAC, and the leak only helps
an attacker who already holds a key's secret. Reviewer 3 also notes `MAX_TOKEN_LENGTH` (64 KB) may be
dead behind an 8 KB engine header cap — worth measuring before tuning either.

### 4. Require `exp`? *(reviewer 2 F5, LOW)*

RFC-compliant as is (`exp` is OPTIONAL), and this issuer always writes it, so requiring it would cost
zero valid tokens while converting a future mis-mint from an eternal credential into a rejection. It
would, however, overturn a policy decision taken deliberately in the previous session and pinned by
`JwtWireCompatSpec`'s no-exp fixture. Maintainer's call.

### 5. Rotation is not yet operable

Nothing generates a key, nothing warns that one is old, and nothing logs which key is signing — so a
mis-ordered rotation is invisible until someone decodes a fresh token. `issued` is the hook. Worth
its own task when someone actually has to rotate.
