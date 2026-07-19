# Unify the HTTP client on Ktor (remove custom fetch/interceptor machinery)

**Status:** IN PROGRESS
**Plan:** (infra groundwork — prerequisite for the typed-`ApiResponse`-error work)
**Security-critical:** no (transport refactor; no authz logic changes — bearer header just moves
into ktor's `defaultRequest`. Keep an eye on CORS/credentials parity.)

## Background

`ultra:remote` had two divergent transport implementations:

- **JS** (`RemoteRequestImpl`): raw `window.fetch`; **throws `RemoteException` on any non-2xx**,
  before the response body is decoded → the typed `ApiResponse` envelope is discarded on 4xx.
- **JVM** (`RemoteRequestImpl`): Ktor `HttpClient`; **emits the response regardless of status**
  (`// TODO: call response interceptors` — response interceptors never ran on JVM).

This asymmetry is why `response.isSuccess()` works in JVM tests but is dead on JS (the flow throws
first). Ktor's client now works on JS (already used here for SSE), so the `window.fetch` path and
the whole `RemoteRequest`/`RemoteResponse`/interceptor abstraction are obsolete.

Reason this comes first: it independently fixes (a) the false-positive error logging spilling
401/403-on-login into the logging system, and (b) the JS/JVM 4xx asymmetry — and it makes the
follow-up "typed error data" decision trivial (envelope always decodes on both platforms).

## Spec

- [ ] Single **commonMain** Ktor-`HttpClient`-backed `RemoteRequest` implementation; delete the
      `expect/actual createRequest`, both platform `RemoteRequestImpl`s, both `RemoteResponseImpl`s.
- [ ] `expectSuccess = false` semantics: non-2xx (incl. 4xx/5xx that carry an `ApiResponse`
      envelope) **emit**, never throw. Genuine transport failures (network/DNS/engine) still throw
      through the flow.
- [ ] Keep a **slim `RemoteResponse`** interface (`status`, `statusText`, `body`, `ok`, `is4xx`,
      `is5xx`; drop the `request` back-reference — unused).
- [ ] **Bearer token via Ktor machinery**: app configures `defaultRequest { }` on its `HttpClient`
      (token evaluated per-request). Delete `SetBearerRequestInterceptor`.
- [ ] **Delete** `ErrorLoggingResponseInterceptor` (the false-positive source),
      `RequestInterceptor`, `ResponseInterceptor`, `RemoteException`, JS `remote.kt`
      (onError/btoa helpers), remote `_mp.kt` (`encodeURIComponent` here is dead — call sites use
      `ultra.common.encodeUriComponent`).
- [ ] **Devtools request list preserved** via an `onResponse` observer hook applied with
      `.onEach {}` on the response flow (not an interceptor). Convert `DevtoolsApiResponseInterceptor`
      (funktor/inspect) to a plain `suspend (RemoteResponse) -> Unit` observer.
- [ ] `ApiClient.Config`: `client: HttpClient` becomes **required**; drop `requestInterceptors`/
      `responseInterceptors`; add `onResponse: List<suspend (RemoteResponse) -> Unit>`.
- [ ] Public `ApiClient` / `call()` / `TypedApiEndpoint` API stays byte-compatible; only the app
      `api.kt` files (ops-app, adminapp) and the devtools observer change.
- [ ] Add `ktor-client-js` (JS engine) dependency — the one genuinely new dep. jvmMain gets a
      default engine (`cio`) so `HttpClient {}` auto-resolves per platform.

## Implementation notes

- Slim `RemoteRequest` to verb methods + `sse` only (header-mutation methods had zero external
  callers once interceptors are gone).
- `execute()` applies observers: `flow { ... emit(resp) }.onEach { r -> onResponse.forEach { it(r) } }`.
- Engine auto-selection: `HttpClient {}` (no explicit engine) picks the single engine on the
  runtime classpath — `ktor-client-js` on JS, `ktor-client-cio` on JVM (both `implementation` in
  `ultra:remote`). Verify the app JS bundle links a working engine.

## Blast radius (measured)

- `ultra:remote` internals rewritten; `RemoteException.onError` and the header-mutation methods have
  **0** external callers.
- App-side churn: **2** files (`funktor-demo/ops-app/.../api.kt`, `funktor-demo/adminapp/.../api.kt`).
- Devtools: **1** file (`DevtoolsApiResponseInterceptor.kt`), registered in adminapp only.

## Test evidence

- [x] `RemoteRequestTransportSpec` (commonTest, MockEngine) — 5 cases green on JVM **and** JS:
      non-2xx emits (not throws) with status+body; a client-level `expectSuccess=true` is overridden
      (4xx still emits — verified the case fails without the per-request `expectSuccess=false`);
      transport failure propagates; `onResponse` observers run per response incl. errors; a throwing
      observer does not break delivery.
- [x] `ultra:remote` compiles (JS + JVM); `:ultra:remote:jvmTest` + `:ultra:remote:jsTest` green.
- [x] `:funktor-demo:ops-app:compileKotlinJs` + `:funktor-demo:adminapp:compileKotlinJs` green
      (Ktor Js engine links; devtools observer wired via `onResponse`).
- [x] `:funktor-demo:server:compileKotlin` + `compileTestKotlin` green (e2e harness uses ktor
      `testApplication`, not `ApiClient` — unaffected).
- [x] Behavior check (code trace): JS login on 401/403 now emits `ApiResponse(data=null)` instead
      of throwing → `AuthState.login` yields `null` → "sign-in failed" (same UX), no console-error
      spam (interceptor deleted). See `funktor/auth/src/jsMain/kotlin/AuthState.kt:158`.
- [ ] Manual (user): boot dev server + ops-app; confirm login 401/403 is a clean failure, the
      devtools request list still populates, and cross-origin bearer/CORS behaves as before.

## Behavior change (call-site relevant)

On **JS**, a non-2xx no longer throws `RemoteException` — it emits an `ApiResponse` with
`isSuccess() == false` and `data == null` (matching JVM). Call sites that did `.map { it.data!! }`
now NPE-in-flow on 4xx instead of RemoteException-in-flow; both surface identically through
`dataLoader`/`.catch`. No call sites relied on catching `RemoteException` (0 external `.onError`
users). `RemoteException` is deleted.

## Review record (/feature-review, 2026-07-19, 3× Opus/high)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | APPROVE, no CRIT/HIGH | expectSuccess unenforced (MED); observer-throws-breaks-flow (LOW); SSE now bearer'd (LOW); wildcard import (LOW); dead KDoc ref (NIT) |
| 2. Domain expert | 2 MED caller regressions | `.catch`-before-`.map{it.data!!}` in `LogsBulkActionPopup` and `AuthState.requestSetPassword` (MED×2); zero transport test coverage (MED); observer isolation (LOW) |
| 3. Security | No CRIT/HIGH; fail-closed | CORS/credentials parity unverified (MED, runtime-only); bearer-on-redirect JVM-only (LOW); devtools decode-stacktrace to console (LOW) |

**The emit-not-throw change had two real caller regressions** (found by domain reviewer, both
verified): a `.catch` placed *before* `.map { it.data!! }` used to catch the source throw on 4xx;
now the 4xx emits `data=null` and the `!!` NPEs *downstream* of the catch → uncaught. The task's
earlier "surface identically" claim was wrong for exactly these two sites (all other `.data!!`
sites are DataLoader-wrapped or correctly ordered — confirmed).

Fixes applied:
- Reordered `.map { it.data!! }.catch { }` in `LogsBulkActionPopup.kt` and
  `AuthState.requestSetPassword` (the latter ships with the Session-refactor commit).
- Forced `expectSuccess = false` per-request in `RemoteRequestImpl` so the emit-not-throw guarantee
  can't hinge on caller client config; documented on `Config.client`. Locked by the transport spec.
- Isolated each `onResponse` observer in try/catch (rethrowing `CancellationException`) so a
  misbehaving observer can't turn a delivered response into a flow error.
- De-wildcarded `import io.ktor.client.*` in `ApiClient.kt`; fixed the dead `ResponseInterceptor`
  KDoc reference in `DevtoolsResponseObserver`.
- Added `RemoteRequestTransportSpec` (the missing coverage that would have caught the regressions).

## Security follow-ups (verify before marking the branch done)

- **CORS / credentials parity (MED, runtime-only):** old JS used `window.fetch` with credentials at
  browser-default `same-origin`; the ktor Js engine's default is unverified and the API is
  cross-origin (`api.*` subdomain). Manually confirm the engine does not now send cookies
  cross-origin (and that CORS isn't a wildcard that would reject credentialed requests). If undesired,
  pin the credentials mode. This is the boot-and-check item already listed under Test evidence.
- **Bearer-on-redirect (LOW, future):** `defaultRequest` re-applies `Authorization` to redirect
  follow-ups. Browser fetch strips it cross-origin (JS apps safe); a future **JVM** `ApiClient`
  could leak the token to an off-origin 3xx target — scope token attachment to the API host, or
  verify `HttpRedirect` cross-host stripping, when/if a JVM ApiClient is introduced. (Red-team note.)
