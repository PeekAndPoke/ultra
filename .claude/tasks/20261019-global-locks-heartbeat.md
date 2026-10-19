# funktor/cluster: global locks expire by heartbeat, not by creation time

**Status:** TODO — plan agreed with the maintainer 2026-10-19; implementation waits for the maintainer's running test suite (no concurrent builds)
**Plan:** none — maintainer request, 2026-10-19
**Security-critical:** no (concurrency correctness; no auth or data boundary)

## Why

Today (`funktor/cluster/src/jvmMain/kotlin/locks/VaultGlobalLocksProvider.kt`) a lock is inserted with
`expires = created + 15.minutes` and is stolen on the next acquire attempt (`tryStealExpiredLock`) or released by
`GlobalLocksCleanupWorker` (every 10 s) once `expires` has passed — regardless of whether the holder is still working.
That gives two failures:

- **A long handler loses its lock while it runs** (> 15 min) → another server acquires it → the guarded work runs
  twice.
- **A dead holder blocks for up to 15 min** unless its whole server's beacon goes stale (`ServerBeacon`, 3 min). A
  handler that is stuck or whose coroutine died on a live server keeps the lock the full 15 minutes.

Maintainer's idea: while a lock is held, a coroutine updates a "last heard from the owner" timestamp; a lock counts
as dead when that heartbeat is older than a TTL, not when it is old.

## Spec

- [ ] **Domain** (`locks/domain/GlobalLockEntry.kt`): add `token: String = ""` (random per acquisition) and
      `lastHeartbeat: MpInstant = created`; the defaults make records written by older versions decode (no
      migration). `@Vault` for KSP query paths. `created` and `expires` stay.
- [ ] **Atomic repo operations** — `VaultGlobalLocksProvider.Repo : Repository<GlobalLockEntry>`, the same pattern as
      `BackgroundJobs.Queue.Vault.Repo`, implemented natively in `KarangoGlobalLocksRepo` (AQL `FILTER … UPDATE` /
      `REMOVE`) and `MonkoGlobalLocksRepo` (`findOneAndUpdate` / filtered delete):
      - `heartbeat(key, token, at)` — updates only if `(key, token)` still matches; returns whether it did.
      - `removeIfHeld(key, token)` — the holder's release. **Fixes a pre-existing bug:** today the holder's `finally`
        deletes by key only, so a holder whose lock was taken over deletes the NEW owner's lock.
      - `removeIfUnchanged(entry)` — removes only if `(key, token, lastHeartbeat)` are still what was read. Used by the
        steal-on-acquire and by `releaseBy`/the cleanup worker. **Fixes a pre-existing race:** today both read, then
        delete by key, so a lock refreshed or re-acquired in between is removed.
- [ ] **Provider** (`VaultGlobalLocksProvider`): new parameters `ttl = 60.seconds`, `heartbeatInterval = ttl / 3`,
      `log`. While the handler runs, a child coroutine heartbeats; it stops before the release. Heartbeat finds the
      lock gone → one warning, beating stops, the handler finishes (decision). Heartbeat throws (DB hiccup) → warning,
      next beat retries.
- [ ] **Dead rule**, in one place in the provider: a lock with a token is dead when `now - lastHeartbeat > ttl`; a
      LEGACY lock (no token, written by an older version) keeps the old rule `now > expires`. Each heartbeat also
      pushes `expires` to `lastHeartbeat + 15 min`. Together these keep a ROLLING DEPLOY safe: new servers do not steal
      a lock an old server still holds, and old servers (which only know `expires`) do not steal a live new-style
      lock. `GlobalLocksCleanupWorker` asks the provider for the dead locks instead of comparing `expires` itself
      (new `GlobalLocksProvider.releaseExpired()`; `PrimitiveGlobalLocksProvider` keeps its in-memory behaviour).
- [ ] **Inspect** (`GlobalLockEntryModel`, `GlobalLocksListPage`, `GlobalLocksApi`): show the last heartbeat. The
      token is not exposed.
- [ ] **Tests**, storage on BOTH backends:
      - repo operations on Karango and Monko: a wrong token neither updates nor removes; `removeIfUnchanged` loses to
        a concurrent heartbeat;
      - provider (in-memory test repo + Karango + Monko via `GlobalLockProviderSpecBase`): a handler running longer
        than the TTL keeps its lock (nobody else gets it meanwhile); a lock whose heartbeat stopped is taken after the
        TTL; a FRESH lock with an old `created` is NOT taken (the old 15-minute rule is gone); a legacy record
        decodes and follows `expires`; a holder whose lock was taken over does not delete the new owner's lock;
      - e2e (`funktor/all` `ClusterApiSpec`, both backends): the locks listing carries `lastHeartbeat`;
      - mutation-checked (persistence and concurrency).
- [ ] Docs follow-up at archive time (public behaviour changed): `docs-site/src/pages/ultra/funktor/cluster.astro`
      + LLM mirror — how long a lock lives, the TTL and heartbeat settings.

## Decisions (maintainer, 2026-10-19)

- **Lost lock → log only.** When a holder's heartbeat finds its lock gone (taken over after it went silent), it logs a
  warning and the handler runs to completion — no cancellation, no new exception. (Consequence, accepted: the guarded
  work can then run twice in that situation, as it can today.)
- **No absolute maximum lifetime**: heartbeat only. `expires` is dropped from the decision logic; old records keep
  decoding.
- **Timing:** TTL 60 s, heartbeat every TTL/3 = 20 s by default (maintainer: "TTL/3 seems ok"); tolerates one fully
  missed beat. Both configurable on the provider.

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
