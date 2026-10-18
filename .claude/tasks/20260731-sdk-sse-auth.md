# Generated SSE clients bypass the SDK's auth mechanism

**Status:** TODO
**Plan:** `.claude/tasks/20260729-ts-sdk-codegen.md` → Phase 2 follow-up
**Security-critical:** yes — a stream a developer believes is authenticated is silently anonymous.

## The problem

The generated SDK has ONE documented auth mechanism: wrap the transport.
`runtime/http.ts:50-58` states it, and `runtime/client.ts:15-21` repeats it as the reason `SdkConfig`
carries no `headers` field.

**SSE does not go through the transport.** `sseStream` calls `globalThis.fetch` directly
(`ultra/codegen/src/main/resources/ts/runtime/sse.ts`), because the response is consumed as a byte
stream and `HttpTransport.send` returns a fully-read `body: string` — it cannot express a stream. So
`stream()` takes an `SdkConfig` but uses only `baseUrl`.

The generated member makes the options argument trailing and OPTIONAL:

```ts
readonly watch = (params: { room: string }, options?: SseOptions): AsyncGenerator<SseEvent> =>
```

so `client.status.watch({ room: 'x' })` compiles, runs, and sends **no Authorization header**.

## Why this is worse than "an unauthenticated request"

- A team that wrapped the transport exactly as documented has every `request` member authenticated
  and every stream member silently not. Nothing signals the asymmetry.
- Against a cross-origin `baseUrl` — the documented `https://api.example.com` case — `fetch` also
  sends no cookies by default, so the stream is fully anonymous.
- On a route with an **optional-auth floor** the stream SUCCEEDS and delivers the anonymous view.
  That is the worst shape: no error, just quietly different data.
- The same bypass drops CSRF headers, org-scoping headers and base-URL rewrites the wrapper adds —
  and defeats an injected test transport, so no test catches it.

## Maintainer's steer (2026-07-31)

> "this is true ... not sure what to do about it ... we could send the jwt token as a header again and
> check for it"

So: a header-based token on the stream request is the accepted direction. `sse.ts`'s own KDoc already
explains why `EventSource` was rejected — it cannot send an `Authorization` header — so the fetch-based
reader exists precisely to make this possible. The plumbing is there; what is missing is a way for the
SDK's auth to reach it without the caller passing it per call.

## Options to weigh (not yet decided)

1. **A hook on `SdkConfig`** — e.g. `streamHeaders?: () => Record<string, string>`, merged by
   `stream()`. Smallest change. Downside: two auth mechanisms in one config, which is exactly what the
   "auth is a transport wrapper" rule was written to avoid.
2. **Extend `HttpTransport` with a `stream()` capability**, so one wrapper covers both. Conceptually
   right — one mechanism, no asymmetry — but every existing transport implementation must grow a
   method, and the default has to be written against `fetch` anyway.
3. **Make `options` REQUIRED on generated stream members.** Does not fix anything, but converts a
   silent runtime bypass into a compile error the author must answer. Cheap, and composes with 1 or 2.

Whatever is chosen, **the asymmetry must stop being silent.**

## Spec

- [ ] Decide between the options above (or a better one) with the maintainer.
- [ ] Implement so a transport-level auth wrapper reaches SSE, or so omitting stream auth cannot compile.
- [ ] `runtime/http.ts` and `runtime/client.ts` KDoc updated — they currently state a rule that has an
      unmentioned exception.
- [ ] ts-verify: a check that a configured auth reaches the stream request. The existing stream check
      injects `fetchImpl`, so the header is observable.
- [ ] Mutation-test it: removing the auth plumbing must fail.

## Test evidence

- [ ] Unit/behaviour tests
- [ ] ts-verify check that executes an authenticated stream
- [ ] Full test command(s) run + green: `...`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up** (required — this is auth): `.claude/tasks/YYYYMMDD-redteam-sdk-sse-auth.md`
