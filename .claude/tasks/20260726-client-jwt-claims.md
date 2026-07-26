# Client-side JWT claim decoding + the selected org in the session

**Status:** IN REVIEW (implemented 2026-07-26)
**Security-critical:** yes — touches what the client believes about its own permissions, and turns on
the session-refresh lifecycle. (No authorization decision rests on it; see below.)
**Why a separate task:** this rode along in the `EmailAddress` review gate, but it is its own framework
feature — new public API in `funktor/auth`, a changed default on a public constructor parameter, and a
new field on a published `@Serializable` model. Written up per the CLAUDE.md rule after reviewer 1
flagged the omission.

## The defect

`AuthState`'s `jwtDecoder` parameter defaulted to `{ emptyMap() }`, and **none of the four demo apps
passed one**. So `readJwt` decoded zero claims and every claim-derived client feature silently
no-opped:

| field | consequence |
|---|---|
| `permissions` | always empty → b2b Members page reported "No organisation is selected" forever; role-gated UI never appeared |
| `tokenUserId` | always null → `ChangePasswordWidget` hit `State.Error` on EVERY attempt |
| `tokenExpires` | always null → proactive refresh and the boot expiry check both returned early; the app never refreshed until the server 401'd |

Nothing failed loudly — `UserPermissions()` defaults are empty, not absent — which is why it survived.
A comment in `DashboardPage` ("Populated once the client-side JWT decoder is wired (deferred)") shows
it was a known deferral that then got forgotten.

Two further mapping bugs made it wrong even WITH a decoder:
- `isSuperUser` was never read, though the server writes `"$ns/superuser"`.
- `exp` was read as `as? Int` — `JSON.parse` yields a JS number, so that cast is
  representation-dependent and breaks past 2038 (beyond Int32).

## Spec

- [x] `funktor/auth/src/jsMain/kotlin/jwtClaims.kt` — `decodeJwtClaims(token)`: split, base64url→base64
      with padding restored, `atob`, percent-encode + `decodeURIComponent` (UTF-8: `atob` yields one
      char per BYTE), `JSON.parse`, `jsObjectToMap`. Synchronous, no npm, never throws — any
      malformation returns `emptyMap()`, i.e. the previous behaviour, so a bad token cannot break
      sign-in. Logs to `console.warn` so it can never fail silently again.
- [x] It is now the DEFAULT for `jwtDecoder` in both declarations (`authState(...)` and the `AuthState`
      ctor). The parameter stays, so apps and tests can override.
- [x] NOT `kraft:addons:jwtdecode`: that loads `jwt-decode` via a dynamic `import()` behind an
      `AddonRegistry`, i.e. asynchronous, while `readJwt` is synchronous. Using it would make `readJwt`
      `suspend` and thread a registry through `AuthState` — a large change for a base64 decode. The
      now-dead dependency was removed from `funktor/auth`.
- [x] `readJwt`: map `isSuperUser`; read `exp` as `Number`.
- [x] `AuthState.Data.Session.org: AuthOrgRef? = null` + a `Data.org` pass-through, populated from the
      sign-in/select-org/refresh response. Defaulted so a session persisted before the field existed
      still decodes. The JWT carries only the org's ID, and its `_key` is a generated Arango key — the
      NAME is what a UI needs, and it already rides the response, so this costs no roundtrip.
- [x] b2b sidebar shows the org name with a "Funktor B2B" sub-header; `MembersPage` shows the name too
      (it was rendering the raw Arango key).
- [x] `Data.permissions` KDoc'd DISPLAY-ONLY.

## Fixes applied from the review round

- **A session persisted before the decoder existed can never self-heal.** It has no `exp`, so
  `checkAndRefreshToken` returned early forever — it would never refresh and never repopulate
  permissions/org, and the new sidebar would read "No organisation" indefinitely. The boot check now
  treats an unknown expiry as stale, so the user re-authenticates once.
- **A transient refresh failure hard-logged the user out.** `.catch { emit(null) }` collapsed a network
  blip or a 502 into `handleSessionExpired()` — roughly two minutes BEFORE the token actually expires,
  with no retry. Newly reachable now that the lifecycle runs. It now warns and leaves the session
  alone; the next tick retries, and expiry still happens for real once `now >= exp`.

## Security position

Client-side decode is a **read, never a verification** — no signature check, and the whole session
lives in user-editable localStorage. Documented at the top of `jwtClaims.kt` and on
`Data.permissions`. Verified in review: no client authorization decision rests on a decoded claim that
the server does not independently re-derive from the signature-verified token (`forAnyRole` on
`user.permissions`, `OrgIsolationGuard` on `permissions.hasOrganisation`). A hand-crafted
`permissions/superuser: true` unlocks nothing today — no `jsMain` code reads it. It also adds no new
tamperable surface: the entire `Data`, permissions included, was already persisted client-side.

Residual, recorded not fixed: `isSuperUser` short-circuits every `has*` helper on `UserPermissions`, so
the first UI that consults one gets all its gates flipped by a single localStorage edit. Display-only
is now stated in KDoc.

## Test evidence

- [x] `funktor/auth/src/jsTest/kotlin/JwtClaimsSpec.kt` — 8 tests, green in headless Chrome (a new
      `jsTest` source set for the module; the Gradle block existed, the directory did not). Covers claim
      decoding, **the exact `as?` types `readJwt` casts to**, `exp` as a `Number` and a post-2038 value,
      multi-byte UTF-8, base64url padding at all four length remainders, and six malformed inputs.
- [x] `:funktor:auth:jsTest` and `:funktor:auth:jvmTest` green; whole-project `assemble` green.
- [ ] Manual: verified in the browser by the user — org name renders in the sidebar.

## Follow-ups (not done here)

- `ultra/security` has no `jsTest` source set, so `EmailAddress` — a commonMain type whose `init` runs
  in the browser on every decode — is only pinned on JVM. Canonicality is defined by `trim()` and
  `lowercase()`, which have separate JVM/JS implementations. Verified equivalent for ASCII (and the
  ASCII guard means nothing new can be non-ASCII), so this is low risk, but a decode-time invariant on
  a multiplatform type deserves a both-platform test.
- Multi-tab: `isRefreshing` is per-instance, so N tabs each run a timer and each write the shared
  `"auth"` localStorage key. Not a storm (≤2/h/tab) but tabs can clobber one another. Pre-existing
  design, newly exercised.

## Review record

Covered by the round-2 gate over the combined uncommitted diff — see the Review record in
`.claude/tasks/20260726-value-class-emailaddress.md`.
