# DOCS — the sign-in response contract, and the REST Content-Type requirement

**Status:** TODO — created 2026-08-02 on archiving
`.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md` and
`.claude/tasks-archive/2026-08/20260802-rest-content-type-enforcement.md`, per the CLAUDE.md rule that a
public-API change gets a docs follow-up once the code is settled.
**Plan:** none.
**Security-critical:** no — but it documents a security boundary, so be precise about what enforces what.

## Settled? Yes

Bearer + `localStorage` is a maintainer decision (2026-08-02), not a default anyone is still weighing, and
`Session` is deliberately sealed with one variant so a future transport is additive. The shape will not
move next week. That is the precondition for writing docs against it.

## What changed, and what the docs currently say

| Change | Docs state |
|---|---|
| `AuthSignInResponse.Success.token: Token` → `session: Session` (sealed, `Bearer(token)` only), plus `permissions`, `expiresAt` (nullable), `userId` | **Stale line confirmed:** `docs-site/src/pages/ultra/funktor/auth.astro:225` still says `// response.data?.token — the new JWT`. That member no longer exists. |
| `AuthRealm.generateJwt` returns `String` | Already corrected during the review gate (`4aa6b808`) — the snippet at `auth.astro:60` is right. Verify, do not re-fix. |
| `funktor/rest` answers **415** unless a body-bearing route gets `Content-Type: application/json` | Undocumented. `rest.astro` says nothing about it, and it is the one error an integrator must self-fix. |

## Spec

- [ ] Fix `auth.astro:225`. The accessor is the `bearerToken` extension
      (`funktor/auth/src/commonMain/kotlin/model/AuthSignInResponse.kt`), not a member — say why in one
      clause: the point of sealing is that a client narrows before reaching a token.
- [ ] Document that the sign-in / refresh response **states** permissions, expiry and user id, so no
      client decodes the JWT. One short paragraph. It replaces, rather than supplements, anything implying
      the frontend reads claims — check the "Session lifecycle" section (`auth.astro:280-295`) for that.
- [ ] Say plainly that `permissions` here is **display-only**; the server re-derives from the verified
      token on every request. Do not let the docs make it sound authoritative.
- [ ] `rest.astro`: body-bearing routes require `application/json`, else 415 in the standard
      `ApiResponse` envelope. A charset parameter is fine; an absent or `*/*` header is not.
- [ ] Mirror all of it in `docs-site/src/data/llms/funktor.md` — the auth section is around `:150-185`.
      The mirror is a reference, not a transcript: shorter than the pages, not longer.
- [ ] `pnpm run build` from `docs-site/` — zero errors, page count unchanged.

## Do not document

Cookie mode, `X-Funktor-Csrf`, or an `Origin` check. All designed, all dropped
(`20260719-token-storage-hardening.md` §DROPPED). Documenting a road not taken is how a reader ends up
implementing it.
