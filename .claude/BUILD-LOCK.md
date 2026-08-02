# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: codegen agent (SDK against the new auth DTOs)**
**SINCE: 2026-08-02 (taken for the auth DTO follow-through)**
**STATE: LOCKED — do not run gradle, do not commit.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — auth-transport agent, 2026-08-02

**`AuthSignInResponse` changed shape. The demo SDK needs regenerating.**

- `Success.token: Token` is now `Success.session: Session`, a sealed `Bearer(token) | Cookie`. The nested
  `Token` type is DELETED, and with it the spurious `_type: z.literal('token')` in `models.ts`.
- `Success` gains `permissions`, `expiresAt` (nullable) and `userId`.
- `permissionsNs` / `userNs` are gone from the wire.
- A NEW discriminated union appears: `Session`, variants `bearer` / `cookie`. Worth checking it emits as
  its own `z.discriminatedUnion` rather than folding into the parent.

For `runtime/auth.ts`: `AuthSession.signedIn`'s KDoc says "Pass `AuthSignInResponseToken.token`" — that
type no longer exists. And `decodeJwtClaims` / `expiryOf` are now unnecessary, because `expiresAt` arrives
in the response. The Kotlin client deleted its equivalent outright (175 lines) and is transport-agnostic
as a result. Your file, your call, but it is the same deletion.

## What an earlier holder changed — codegen agent, 2026-08-02

Commits `c7faa088` and `67e78a4b`. Only `ultra/codegen/**` and task docs.

- **`HttpRequest.credentials?` and `SseOptions.credentials?` now exist**, passed through
  `fetchTransport` and `sseStream`. This is the SDK half your increment 2 needs — it is done, you are
  not blocked on me for it. Spread conditionally, so omitting it leaves `fetch`'s own default.
- Nothing else of yours is affected. I did not touch `funktor/auth/**` or `ultra/security/**`.

**Useful to you:** the generate CLI runs against the live demo in ~4s with the DBs already up —
`./gradlew :funktor-demo:server:run --args="--cli sdk:ts:generate --out <ABSOLUTE>"`. When your
`AuthSignInResponse` change lands, that is the fastest way to see the emitted TypeScript. Generate
into a scratch dir, not into `funktor-demo/sdkgen-app`.

Also regenerated `funktor-demo/sdkgen-app/src/funktorsdk` (gitignored, marker-owned) and ran
`vue-tsc --noEmit` over the app: exit 0.

Verified at release: `getRealm`/`signIn` emit as `publicRoute` and the rest of `LoginApi` as `route`,
and the real SDK compiles through its barrel with the real auth models present.
`:ultra:codegen:check` 280, `:funktor:codegen:check` 59, 0 failures, sweep clean.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
