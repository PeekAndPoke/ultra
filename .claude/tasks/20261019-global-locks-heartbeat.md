# funktor/cluster: global locks expire by heartbeat, not by creation time

**Status:** IMPLEMENTED 2026-10-19 on `wt/locks-heartbeat`, awaiting the `/feature-review` gate (run by the coordinator)
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

- [x] **Domain** (`locks/domain/GlobalLockEntry.kt`): add `token: String = ""` (random per acquisition) and
      `lastHeartbeat: MpInstant = created`; the defaults make records written by older versions decode (no
      migration). `@Vault` for KSP query paths. `created` and `expires` stay.
- [x] **Atomic repo operations** — `VaultGlobalLocksProvider.Repo : Repository<GlobalLockEntry>`, the same pattern as
      `BackgroundJobs.Queue.Vault.Repo`, implemented natively in `KarangoGlobalLocksRepo` (AQL `FILTER … UPDATE` /
      `REMOVE`) and `MonkoGlobalLocksRepo` (`findOneAndUpdate` / filtered delete):
      - `heartbeat(key, token, at)` — updates only if `(key, token)` still matches; returns whether it did.
      - `removeIfHeld(key, token)` — the holder's release. **Fixes a pre-existing bug:** today the holder's `finally`
        deletes by key only, so a holder whose lock was taken over deletes the NEW owner's lock.
      - `removeIfUnchanged(entry)` — removes only if `(key, token, lastHeartbeat)` are still what was read. Used by the
        steal-on-acquire and by `releaseBy`/the cleanup worker. **Fixes a pre-existing race:** today both read, then
        delete by key, so a lock refreshed or re-acquired in between is removed.
- [x] **Provider** (`VaultGlobalLocksProvider`): new parameters `ttl = 60.seconds`, `heartbeatInterval = ttl / 3`,
      `log`. While the handler runs, a child coroutine heartbeats; it stops before the release. Heartbeat finds the
      lock gone → one warning, beating stops, the handler finishes (decision). Heartbeat throws (DB hiccup) → warning,
      next beat retries.
- [x] **Dead rule**, in one place in the provider: a lock with a token is dead when `now - lastHeartbeat > ttl`; a
      LEGACY lock (no token, written by an older version) keeps the old rule `now > expires`. Each heartbeat also
      pushes `expires` to `lastHeartbeat + 15 min`. Together these keep a ROLLING DEPLOY safe: new servers do not steal
      a lock an old server still holds, and old servers (which only know `expires`) do not steal a live new-style
      lock. `GlobalLocksCleanupWorker` asks the provider for the dead locks instead of comparing `expires` itself
      (new `GlobalLocksProvider.releaseExpired()`; `PrimitiveGlobalLocksProvider` keeps its in-memory behaviour).
- [x] **Inspect** (`GlobalLockEntryModel`, `GlobalLocksListPage`, `GlobalLocksApi`): show the last heartbeat. The
      token is not exposed.
- [x] **Tests**, storage on BOTH backends:
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

Paths are relative to `funktor/cluster/src/jvmMain/kotlin/locks/` unless given in full.

- **Domain**: `domain/GlobalLockEntry.kt:9` `@Vault`; `token` (`:20`) and `lastHeartbeat` (`:22`) appended with
  defaults, so a record without them decodes through Slumber's constructor defaults (`callBy`). Proven on both DBs
  with records written without the fields (`KarangoGlobalLockProviderSpec`/`MonkoGlobalLockProviderSpec`, "A record
  written before heartbeats existed…").
- **Repo**: `VaultGlobalLocksProvider.kt:51`. Deviation from the spec's signature: `heartbeat(key, token,
  heartbeatAt, expiresAt)` — the provider computes the legacy expiry, so that policy lives in one place.
  - Karango `karango/KarangoGlobalLocksRepo.kt:41/59/72`: `FOR … FILTER _key, token … UPDATE … RETURN NEW` /
    `REMOVE` (Karango prints `RETURN OLD`, so `findFirst != null` means removed). `removeIfUnchanged` treats an
    Arango **write-write conflict (1200) as "changed" → `false`** (`:93`). Found by the contenders test on its
    first run: concurrent takeovers of one dead lock made the losing REMOVE throw out of `lock()`. HEAD swallowed
    this only by accident (`EntityRepository.remove` catches `KarangoQueryException`). `heartbeat` and
    `removeIfHeld` still throw on a conflict: the beat retries next time, the release retries via `retry`.
  - Monko `monko/MonkoGlobalLocksRepo.kt:57/71/75`: filtered `updateOne` (`matchedCount`) / `deleteOne`, matched by a
    plain-string `_id` (`:92`). **Added `findById` override** (`:48`): the default turns a 24-hex key into an
    ObjectId and misses the lock, so on Monko a key that looks like an ObjectId was never taken over — and at HEAD
    never released either (`deleteOne` has the same conversion; mutant M4 below shows it).
  - The Monko heartbeat slumbers its instants (`:63`). Measured: without it, the driver writes the SAME shape
    `{ts, timezone, human}` through bson-kotlinx (`KotlinSerializerCodec` + `MpInstantSerializer`), so mutant M7 is
    equivalent. Kept, so the codec that reads the shape back also decides it; the comment was corrected.
- **Provider** `VaultGlobalLocksProvider.kt`: `ttl`/`heartbeatInterval`/`log` (`:41-43`), validated (`:71`); token
  per acquisition (`:95`); heartbeat child in `coroutineScope`, cancelled and joined before the release (`:128`).
  Decisions of mine, beyond the spec — please confirm:
  - **Heartbeat on `Dispatchers.IO`** (`:128`): a handler that blocks its thread (collected via `acquire()` on a
    confined dispatcher) would otherwise starve the beat and lose its lock after the ttl. Test "A handler that blocks
    its thread keeps its lock" (M13).
  - **Only our own cancellation ends the beating** (`:174`, `ensureActive()` in the catch): a foreign
    `CancellationException` (e.g. a driver-side timeout) is a failed beat, not a silent stop.
  - **Release in `withContext(NonCancellable)`** (`:145`, from the draft, kept): a cancelled handler still releases
    instead of leaving the lock for the ttl. Test "A cancelled handler still releases its lock" (M8).
  - Lost-lock warning says "released or taken over" (the cleanup worker or the CLI may also have removed it).
- **Dead rule** `isDead` (`:198`), used by the takeover (`:189`) and `releaseExpired` (`:213`); `releaseBy` removes
  through `removeIfUnchanged` and returns only what it actually removed (`:208`).
- `GlobalLocksProvider.releaseExpired()` is abstract (`GlobalLocksProvider.kt:100`); the compiler found one other
  implementor, `PrimitiveGlobalLocksProvider.kt:82` (returns `emptyList()`, like its `list`/`releaseBy`). Cleanup
  worker `workers/GlobalLocksCleanupWorker.kt:48`, log headers "Last heartbeat".
- `Log` injected into both factories (`funktor/cluster/src/jvmMain/kotlin/index_jvm.kt:222`, `:335`). **Scoping
  note, correcting the draft's reasoning:** the provider is `Dynamic` with or without `Log`, because
  `useKarango()`/`useMonko()` register a `singleton` over the module's `dynamic` default and an override keeps
  `Dynamic` (`ultra/kontainer/src/main/kotlin/KontainerBuilder.kt:44-46`); measured
  `getProvider(GlobalLocksProvider::class).type == Dynamic` in a request kontainer. "Log is a prototype" does not by
  itself prevent re-scoping: dependents are collected transitively through prototypes
  (`ultra/kontainer/src/main/kotlin/KontainerBlueprint.kt:77-81`), so a true singleton injecting `Log` becomes
  SemiDynamic (cf. the TODO at `ultra/log/src/jvmMain/kotlin/index_jvm.kt:41`). No change in behaviour here.
- **Inspect**: `GlobalLocksApi.kt:53`, `funktor/inspect/src/commonMain/kotlin/cluster/locks/api/GlobalLockEntryModel.kt:17`
  (appended, no default), `funktor/inspect/src/jsMain/kotlin/cluster/locks/GlobalLocksListPage.kt:86`. No token.
- **Tests**: `funktor/cluster/src/jvmTest/kotlin/locks/VaultGlobalLocksProviderSpecBase.kt` (provider and repo rules)
  runs against the in-memory repo (`VaultGlobalLockProviderSpec`), Karango and Monko; it extends
  `GlobalLockProviderSpecBase`, whose generic tests therefore now also run on both DBs. Two of those raced there and
  were fixed in the test: four started both acquires at once (now `onceHeld`: the second waits until the first holds
  the lock), and "Handler throwing an error" asserted an emission order that is a genuine race after the release
  (now order-independent; seen red once on Monko during the mutation runs).
- **E2e** in a NEW file `funktor/all/src/jvmTest/kotlin/GlobalLocksE2eSpec.kt` (not `ClusterApiSpec`: this track adds
  spec files to `funktor/all` only): `KarangoGlobalLocksE2eSpec` on the default test app, `MonkoGlobalLocksE2eSpec`
  on a blueprint with `cluster = { useMonko() }`; each pins its repo class. The app's provider holds the lock; one beat
  is written with the holder's own token at a moment distinct from `created`; the listing must show it, and the raw
  body must not contain the token. Trap met: the test app starts on the first request and
  `GlobalLocksCleanupOnAppStarting` releases every lock of this server id — the spec issues a request before taking
  the lock.

### Found, not changed (need a decision or are out of scope)

- **Startup race in the cleanup worker (read from code, not reproduced).** The dead-server rule releases every lock
  whose server is not in `GlobalServerList` (`workers/GlobalLocksCleanupWorker.kt:27-34`). That list starts empty
  (`GlobalServerList.kt:9`) and is filled only by `ServerBeaconCleanupWorker` (`workers/ServerBeaconCleanupWorker.kt:30`),
  and workers run concurrently (`funktor/cluster/src/jvmMain/kotlin/workers/WorkersFacade.kt:64`). If the lock cleanup
  reads the list first after a server start, it releases every lock in the cluster, live ones included. With
  heartbeats the rule is redundant for token locks (a dead server stops beating: dead after 60 s, faster than the 3 min
  beacon rule). Options: skip it while the list is empty, or restrict it to legacy locks.
- A cancelled `lock()` surfaces as `LocksException.Execution(cause = CancellationException)`
  (`VaultGlobalLocksProvider.kt:137-138`, pre-existing, Primitive too), so the caller's parent sees a failure rather
  than a cancellation.
- The dead rule compares the reader's clock with the writer's `lastHeartbeat`: clock skew eats into the
  `ttl - heartbeatInterval` margin (40 s by default). The ttl is the reader's setting and the interval the writer's,
  so a deploy that changes them can steal live locks for its duration.
- An insert that succeeds in the DB but fails on the client leaves our own lock blocking us until the ttl (it is not
  beaten). Previously 15 min.

## Test evidence

All runs via `console/wt-gradle.sh` (worktree DBs `ultra_wt_locks_heartbeat`); counts from
`build/test-results/**/TEST-*.xml`, failing cases read from the console.

- `:funktor:cluster:jvmTest` (all specs): **130 tests, 0 failures**, three runs in a row. Locks: in-memory
  `VaultGlobalLockProviderSpec` 27, `KarangoGlobalLockProviderSpec` 29, `MonkoGlobalLockProviderSpec` 29,
  `PrimitiveGlobalLockProviderSpec` 8.
- `:funktor:all:jvmTest`, filtered to `KarangoGlobalLocksE2eSpec` 1, `MonkoGlobalLocksE2eSpec` 1, `ClusterApiSpec` 30:
  **0 failures**.
- `:funktor:inspect:compileKotlinJs` (the page change): green.
- Full suite `console/wt-gradle.sh --full-suite` (`clean build --continue`, 16m19s, no `^e:`, no Kotlin daemon
  failure): **68 test targets, 15673 tests, 16 failures, 0 errors, 7 skipped.** `funktor/cluster` jvmTest 130/0,
  `funktor/all` jvmTest 160/0. All 16 failures are environmental, in files this change does not touch:
  - `funktor-demo:server:test` 14/14: every spec fails to instantiate on `${JWT_SIGNING_KEY}` (then
    `${AWS_SES_SECRET_KEY}`, `${SENDGRID_API_KEY}`), which come from the out-of-tree `keys.env.conf` that a new
    worktree does not get. Rerun with throwaway values in the environment: **47 tests, 0 failures**, including
    `B2bMembersApiTest`, which locks through `GlobalLocksProvider.tryToLock`.
  - `monko:core:test` 2: `MongoDbConfigSpec`/`MonkoModuleSpec` expect the test DB to be named `test`, but
    `wt-gradle.sh` sets `ULTRA_TEST_MONGO_DB` to the worktree's DB.

### Mutation table

Each mutant: `cp` backup to `/tmp/locks-mutants/` (source-set path in the name), one replacement, the three lock specs
(or the two e2e specs) run, restore with `cp`, verified with `cmp` and a clean `git diff`. Runner:
`/tmp/locks-mutants/mutate.py`.

| # | Mutant | Went red (backend) | Reason confirmed |
|---|---|---|---|
| M1a | Karango `heartbeat` without the token filter | "heartbeat moves … held with the token only", "A holder whose lock was taken over … warns once" (Karango) | wrong-token beat returned `true`; no warning |
| M1b | Monko `heartbeat` without the token filter | the same two (Monko) | same |
| M2a | Karango `removeIfUnchanged` without its heartbeat condition | "removeIfUnchanged loses to a heartbeat in between" (Karango) | removed after a beat |
| M2b | Monko, same | same (Monko) | same |
| M3 | `isDead` reads `expires` for a lock with a token | 6 per backend: takeover, still-beating, contenders, ObjectId key, `releaseExpired`, cleanup worker | timeouts / wrong survivors |
| M4 | release through plain `remove(key)` | "A holder whose lock was taken over …" (all 3); Monko ObjectId-key test | new owner's lock deleted; on Monko the ObjectId-like lock is never released |
| M5 | `keepAlive` keeps beating after the lock is lost | "… and warns once" (all 3) | `single()`: more than one warning |
| M6a | dead rule: legacy branch dropped | legacy takeover, `releaseExpired` (all 3); true legacy records (Karango, Monko) | legacy lock stolen |
| M6b | Karango `removeIfUnchanged`: legacy branch dropped | both true-legacy tests (Karango) | legacy record cannot be removed |
| M6c | Monko, same | both true-legacy tests (Monko) | same |
| M7 | Monko `heartbeat` without slumbering | **survived — equivalent** | measured: the driver's bson-kotlinx codec writes the identical `{ts, timezone, human}` shape; comment corrected, slumber kept |
| M8 | release without `NonCancellable` | "A cancelled handler still releases its lock" (Karango) | lock left behind. In-memory and Monko stay green: their remove runs (or is already sent) before cancellation is checked |
| M9 | Monko `findById` back to the default | ObjectId-key test (Monko) | takeover cannot find the lock → timeout |
| M10 | cleanup worker compares `expires` itself | cleanup-worker test (all 3) | silent lock kept, beating one removed |
| M11 | API maps `lastHeartbeat = it.created` | both e2e specs | listed heartbeat ≠ the beat |
| M12 | Karango: write-write conflict escapes `removeIfUnchanged` | first only by chance (contenders test, green once) → new crowd test "removeIfUnchanged of one lock by several at once", red 3/3 (Karango) | `1200 - AQL: write-write conflict` |
| M13 | heartbeat on the collector's dispatcher (no `Dispatchers.IO`) | "A handler that blocks its thread keeps its lock" (all 3) | contender got the lock |
| M14 | a failed beat stops the beating | "A failed heartbeat is retried with the next beat" (all 3) | heartbeat never moved |
| M15 | a beat no longer pushes `expires` | "While its handler runs … legacy expiry ahead of the beat" (all 3) | `expires - lastHeartbeat ≠ 15 min` |
| M16 | Karango `removeIfHeld` without the token | "removeIfHeld … own token only", "A holder whose lock was taken over …" (Karango) | plus one unrelated red: Monko "Handler throwing an error", a race in that base test (fixed, see notes) |
| M17 | Monko, same | the same two (Monko) | — |
| M18a | Karango `removeIfUnchanged` without the token | "removeIfUnchanged loses to a re-acquisition in between" (Karango) | — |
| M18b | Monko, same | same (Monko) | — |

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `GlobalLocksCleanupWorker.kt:27-34`, `GlobalServerList.kt:8-9` | The dead-server rule releases EVERY lock in the cluster on a server's first worker tick (the server list is still empty), and a peer deletes a booting server's new locks during the 10 s beacon lag | fix | Verified by the coordinator: the list starts empty and only `ServerBeaconCleanupWorker` fills it, after a DB read (`ServerBeaconCleanupWorker.kt:24-31`); every worker is due on the first tick (`WorkerTracker.kt:133`, Epoch) and all start together (`WorkersFacade.kt:52-75`); `GlobalServerList` is a true singleton (`index_jvm.kt:114`). Pre-existing, but it defeats this feature's guarantee, in a file this change edits. Fix: the rule applies to token-less locks only (token locks die by heartbeat in 60 s, before the 3-min beacon rule), and is skipped until the list has been populated once |
| 1 | MAJOR | `VaultGlobalLocksProvider.kt:189-196`, `:208-211` | The read-then-remove race fixes (`releaseBy` and the takeover → `removeIfUnchanged`) are pinned by no test: a delete-by-key mutant stays green | fix | Verified: no test changes a lock between `list()`/`findById` and the remove; the mutation table has no row for it (M4 is the holder's release, M10 the worker predicate). Fix: wrapper-repo tests on all three backends that beat or replace the lock between the read and the remove; record both mutants |
| 1 | MINOR | `GlobalLocksProvider.kt:100` | `releaseExpired()` is abstract: an outside implementor stops compiling | maintainer-decision | No outside implementor is known; a default body would silently give a new provider the legacy `expires` rule. Maintainer list |
| 1 | MINOR | `GlobalLockProviderSpecBase.kt:207-232` | "Handler throwing an error" no longer proves the second handler ran AFTER the failing one | fix (with the MAJOR batch; reviewed in round 2) | Cheap: a flag set before `error(...)`, asserted by the second handler |
| 1 | MINOR | `VaultGlobalLocksProvider.kt:70-74` | `heartbeatInterval` is not bounded by `legacyExpiry`: an interval over 15 min lets `expires` lapse between beats, and an OLD server steals the live lock | fix (batch) | `require(heartbeatInterval < legacyExpiry)` |
| 1 | MINOR | `KarangoGlobalLocksRepo.kt:59-70`, `:91-94` | Non-1200 Karango query errors are no longer swallowed in the holder's release and `releaseBy` | maintainer-decision (no change proposed) | The reviewer could not show such an error is reachable; recorded so it is not re-derived |
| 1 | MINOR | `VaultGlobalLocksProvider.kt:201` | Clock-skew tolerance shrinks from ~15 min to ~40 s: one skewed server's cleanup deletes live locks cluster-wide | maintainer-decision | Design fork: judge by the DB clock (`DATE_NOW()` / `$$NOW`) or a local monotonic observation, or at least warn when a read heartbeat lies in the reader's future |
| 1 | MINOR | `VaultGlobalLocksProvider.kt:128` | Heartbeats share `Dispatchers.IO` with blocking handlers; saturation for ~40 s loses the holders' own locks | fix (batch) | A dedicated `Dispatchers.IO.limitedParallelism(n)` view in the companion |
| 1 | MINOR | `GlobalLockEntryModel.kt:17` | `lastHeartbeat` has no default: a new UI talking to an old server during a rolling deploy fails to decode the listing | fix (batch) | `= created`; cheap. (The implementation reviewer judged it not a finding; the domain reviewer MINOR) |
| 1 | MINOR | `VaultGlobalLocksProvider.kt:128`, `:164-187` (security + domain) | A stuck-but-alive handler now holds its lock forever (the old 15-min ceiling is gone); the task's "Why" counted the stuck case as improved | maintainer-decision | Follows from the maintainer's "no maximum lifetime"; options: an opt-in `maxHold`, or record "no max hold" explicitly and correct the "Why" |
| 1 | MINOR (surplus) | `VaultGlobalLocksProvider.kt:67` | `legacyExpiry` is a public companion `val` nothing outside needs | fix (batch) | Both implementation and domain reviewers; make it `internal` |

**Probed and CLEAN in round 1:**
- authz floor unchanged (`GlobalLocksApi.kt:13`).
- the token leaves the server through no API, UI, CLI, log or insights record.
- lock keys are sanitized before AQL/Mongo/log use, so no injection and no log forging.
- old servers decode new records (unknown keys are ignored) and new servers decode legacy ones (defaults).
- heartbeat vs release ordering (`coroutineScope` joins the beat first).
- `emit` inside `coroutineScope` is legal.
- Monko `matchedCount` and the plain-string `_id` match.
- Karango REMOVE returns OLD.
- the in-memory repo is synchronized.
- code style: no wildcards, no FQCN, KDoc links resolve.
- Kontainer scoping is harmless (the provider holds no shared state).
- the four `onceHeld` test edits remove a real race without weakening anything.

**Verdict:** round 1 NOT clean (2 MAJOR, fixed in the round-2 batch) — round 2 follows.
