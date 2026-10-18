# DOCS — JWT key configuration and rotation

**Status:** TODO — created 2026-08-01 on archiving `.claude/tasks-archive/2026-07/20260731-jwt-kid-key-rotation.md`
**Plan:** `.claude/tasks-archive/2026-07/20260718-jwt-lib-consolidation-options.md`
**Security-critical:** no (documentation), but it documents the authentication path, and one section
is a runbook someone will follow during an incident. Accuracy matters more than usual.

## Why this task exists

`kid`-based key rotation changed public API **breakingly**: `JwtConfig.signingKey: Redacted<String>`
became `JwtConfig.keys: List<JwtSigningKey>`, plus two new published types (`JwtSigningKey`,
`JwtAlgorithm`). Every application has to edit its config file. Nothing on the docs site describes
the old shape, so there is nothing stale to correct — but there is now something important that is
undocumented.

It is settled: it survived a three-agent review gate, two rounds of fixes and the maintainer's own
review (which found the sharpest defect of all, see below). No further change is expected.

## Where it goes — no scoping decision needed

Unlike `.claude/tasks/20260731-docs-redacted.md`, which is blocked on "where does `ultra:common` get
documented at all", this one has an obvious home. The config is consumed through funktor:

- `docs-site/src/pages/ultra/funktor/auth.astro`
- the `## Auth Module` section of `docs-site/src/data/llms/funktor.md`

Both already exist and both already discuss the auth pipeline and token refresh.

## What to write

Keep it compact — per CLAUDE.md, precise beats comprehensive, and this is a page that will be read
under time pressure.

- [ ] **The config shape.** A HOCON example of `funktor.auth.jwt.keys`, with `id`, `secret`, `alg`
      and `issued` explained in a sentence each. Say plainly: **the first key signs, all of them
      verify.** Point at `keys.env.conf` / `${JWT_SIGNING_KEY}` as the way to keep the secret out of
      the repo — that is the pattern `funktor-demo` now uses (`2397c0a6`).
- [ ] **Generating a key.** `openssl rand -base64 64`, and why 64 bytes is a floor and not a
      suggestion (RFC 7518 §3.2; the server refuses to start below it, with an actionable message).
- [ ] **Graceful rotation, single node:** prepend, deploy, drop the old one a token-lifetime later.
- [ ] **Graceful rotation, a fleet:** the two-phase procedure — append first so every node can verify,
      then reorder to promote. One phase bounces users to anonymous mid-rollout.
- [ ] **The trap:** appending instead of prepending is a silent no-op. Fresh key, fresh `issued`
      date, clean boot, still signing with the old one. Nothing warns. Check `kid` on a freshly minted
      token; that is the only proof.
- [ ] **A compromised key is NOT a rotation.** This is the one the maintainer found and none of the
      three reviewers did, and it is the most important sentence on the page: refresh launders a
      forged token onto the new key, so any grace period hands containment away entirely. **Remove the
      key at once and accept that everyone is logged out.** Say why a `verifyOnly` flag would not
      help. Link `.claude/tasks/20260728-session-revocation-wiring.md` as the real fix.

The source of truth for all of the above is the KDoc on `JwtConfig.keys`
(`ultra/security/src/commonMain/kotlin/jwt/JwtConfig.kt`), which is already written and reviewed.
This is largely a matter of moving it to where an application developer will find it.

## What NOT to write

Per the docs-vs-skills split in CLAUDE.md, the internals stay out: the pre-MAC header parse, the
`alg`-compare-not-select rule, `MAX_HEADER_LENGTH`, canonical base64url, the `kid` timing oracle.
Those are "what will bite me if I change this code", not "how do I use this", and they live in the
KDoc and in `.claude/tasks/20260731-redteam-jwt-own-verifier.md`.

Also skip the migration note — this repo requires no backward compatibility, and the release notes
already carry the breaking-change marker.
