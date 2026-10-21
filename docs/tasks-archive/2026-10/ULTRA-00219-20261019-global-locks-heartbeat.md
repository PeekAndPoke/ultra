# ULTRA-00219: funktor/cluster: global locks expire by heartbeat, not by creation time

**Status:** DONE (archived 2026-10-19) — gate PASS (round 1: 2 MAJOR fixed; round 2 clean; MINOR batches reviewed), merged `d9ffbb32`; master full suite green (15769 tests / 0 failures). Maintainer decisions 2026-10-19 on the open rows: clock skew → rely on NTP (all of the lock logic runs on the server; the DB clock is for TTL indexes); stuck handlers → an opt-in `maxHold`, follow-up ULTRA-00224 (global locks max hold). Docs follow-up: ULTRA-00223 (docs global locks heartbeat)
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
  - Karango `karango/KarangoGlobalLocksRepo.kt:46/71/84`: `FOR … FILTER _key, token … UPDATE … RETURN NEW` /
    `REMOVE` (Karango prints `RETURN OLD`, so `findFirst != null` means removed). Round 2: `heartbeat` builds its AQL
    with `buildAqlQuery` and calls the async driver directly (`:66`), skipping `KarangoDriver.query`'s
    `async(Dispatchers.IO)` hop (`karango/core/src/main/kotlin/vault/KarangoDriver.kt:120`) — and its profiler.
    Checked in the arangodb-java-driver 7.28.0 bytecode that this blocks no thread: `ArangoExecutorAsync.execute` is a
    `CompletableFuture` chain, `ConnectionPoolImpl.connection()` hands out a future (`AsyncQueue.poll`), and
    `HttpConnection.doExecute` sends through Vert.x `WebClient.sendBuffer`, completing in `onSuccess`/`onFailure`; no
    `get`/`join`/sleep on the path. `removeIfUnchanged` treats an Arango **write-write conflict (1200) as
    "changed" → `false`** (`:105`). Found by the contenders test on its
    first run: concurrent takeovers of one dead lock made the losing REMOVE throw out of `lock()`. HEAD swallowed
    this only by accident (`EntityRepository.remove` catches `KarangoQueryException`). `heartbeat` and
    `removeIfHeld` still throw on a conflict: the beat retries next time, the release retries via `retry`.
  - Monko `monko/MonkoGlobalLocksRepo.kt:58/72/76`: filtered `updateOne` (`matchedCount`) / `deleteOne`, matched by a
    plain-string `_id` (`:93`). **Added `findById` override** (`:49`): the default turns a 24-hex key into an
    ObjectId and misses the lock, so on Monko a key that looks like an ObjectId was never taken over — and at HEAD
    never released either (`deleteOne` has the same conversion; mutant M4 below shows it).
  - The Monko heartbeat slumbers its instants (`:64`). Measured: without it, the driver writes the SAME shape
    `{ts, timezone, human}` through bson-kotlinx (`KotlinSerializerCodec` + `MpInstantSerializer`), so mutant M7 is
    equivalent. Kept, so the codec that reads the shape back also decides it; the comment was corrected.
- **Provider** `VaultGlobalLocksProvider.kt`: `ttl`/`heartbeatInterval`/`log`; the class KDoc states the operating
  constraints (`:36`: ttl > worst DB stall + clock skew + one interval; same settings on every server, changed in two
  deploys; invariant-protecting writes need a conditional write). Validated: interval positive and below the ttl, and
  (round 2) `ttl <= legacyExpiry` (`:89`) — a server without heartbeats judges by `expires`, 15 min after the last
  beat, so a longer ttl lets it take over a lock this version counts as alive. It replaces round 1's
  `heartbeatInterval < legacyExpiry`, which it implies. `legacyExpiry` is `internal` (`:77`). Token per acquisition
  (`:113`); heartbeat child in `coroutineScope`, cancelled and joined before the release (`:146`).
  Decisions of mine, beyond the spec — please confirm:
  - **Heartbeat on its own `Dispatchers.IO.limitedParallelism(4)` view** (`:81`, `:146`). Off the handler's
    dispatcher, so a handler blocking its thread cannot starve it (M13); a view of IO gets threads beyond IO's own
    limit, so blocking handlers that saturate IO cannot starve it either (C1). Four threads: a beat suspends while
    the database works. Holds on all three backends since the Karango heartbeat skips the driver's IO hop (K1).
  - **One beat is bounded by `withTimeout(heartbeatInterval)`** (`:190`, round 2): a hanging call becomes a failed
    beat with the existing warning, and the next beat retries (H1).
  - **Only our own cancellation ends the beating** (`:195`, `ensureActive()` in the catch): a foreign
    `CancellationException` (the timeout above, or a driver-side one) is a failed beat, not a silent stop.
  - **Release in `withContext(NonCancellable)`** (`:163`, from the draft, kept): a cancelled handler still releases
    instead of leaving the lock for the ttl. Test "A cancelled handler still releases its lock" (M8).
  - Lost-lock warning says "released or taken over" (the cleanup worker or the CLI may also have removed it).
- **Legacy** (empty token) is one extension property, `GlobalLockEntry.isLegacy` (`domain/GlobalLockEntry.kt:30`),
  used in the dead rule, both repos' `removeIfUnchanged`, the cleanup worker and the in-memory test repo. An extension
  can never become part of the stored record (I1).
- **Dead rule** `isDead` (`:219`), used by the takeover (`:210`) and `releaseExpired` (`:234`); `releaseBy` removes
  through `removeIfUnchanged` and returns only what it actually removed (`:229`). Pinned by wrapper repos that beat or
  re-acquire the lock right after `findAll`/`findById` (X1, X2).
- `GlobalLocksProvider.releaseExpired()` is abstract (`GlobalLocksProvider.kt:100`); the compiler found one other
  implementor, `PrimitiveGlobalLocksProvider.kt:82` (returns `emptyList()`, like its `list`/`releaseBy`).
- **Cleanup worker** `workers/GlobalLocksCleanupWorker.kt`: `releaseExpired()` (`:38`). The log line shows the field a
  lock is judged by: `expires` for a legacy lock, else the last heartbeat (`:72`, round 2; L1).
  Round 1 (MAJOR): the dead-server rule applies to locks **without a token only** (`:55`) — a lock with one dies by its
  heartbeat within the ttl, before the 3-min beacon rule — and is **skipped until the beacons were read once**
  (`:29`, `GlobalServerList.isPopulated`, `GlobalServerList.kt:17`, set in `setAll` `:25`; `@Volatile` and written
  after the list, so a reader that sees it also sees the list).
- `Log` injected into both factories (`funktor/cluster/src/jvmMain/kotlin/index_jvm.kt:222`, `:335`). **Scoping
  note, correcting the draft's reasoning:** the provider is `Dynamic` with or without `Log`, because
  `useKarango()`/`useMonko()` register a `singleton` over the module's `dynamic` default and an override keeps
  `Dynamic` (`ultra/kontainer/src/main/kotlin/KontainerBuilder.kt:44-46`); measured
  `getProvider(GlobalLocksProvider::class).type == Dynamic` in a request kontainer. "Log is a prototype" does not by
  itself prevent re-scoping: dependents are collected transitively through prototypes
  (`ultra/kontainer/src/main/kotlin/KontainerBlueprint.kt:77-81`), so a true singleton injecting `Log` becomes
  SemiDynamic (cf. the TODO at `ultra/log/src/jvmMain/kotlin/index_jvm.kt:41`). No change in behaviour here.
- **Inspect**: `GlobalLocksApi.kt:53`, `funktor/inspect/src/commonMain/kotlin/cluster/locks/api/GlobalLockEntryModel.kt:17`
  (appended; round 1: defaults to `created`, so the new UI decodes an old server's listing — the server side encodes
  with Slumber, which always writes the field), `funktor/inspect/src/jsMain/kotlin/cluster/locks/GlobalLocksListPage.kt:86`.
  No token.
- **Tests**: `funktor/cluster/src/jvmTest/kotlin/locks/VaultGlobalLocksProviderSpecBase.kt` (provider and repo rules)
  runs against the in-memory repo (`VaultGlobalLockProviderSpec`), Karango and Monko; it extends
  `GlobalLockProviderSpecBase`, whose generic tests therefore now also run on both DBs. Two of those raced there and
  were fixed in the test: four started both acquires at once (now `onceHeld`: the second waits until the first holds
  the lock), and "Handler throwing an error" asserted an emission order that is a genuine race after the release
  (now order-independent; seen red once on Monko during the mutation runs). Round 1: the property that order carried
  is back as a flag — the second handler checks that the first had failed before it ran
  (`funktor/cluster/src/jvmTest/kotlin/locks/GlobalLockProviderSpecBase.kt:213`; A1).
- **E2e** in a NEW file `funktor/all/src/jvmTest/kotlin/GlobalLocksE2eSpec.kt` (not `ClusterApiSpec`: this track adds
  spec files to `funktor/all` only): `KarangoGlobalLocksE2eSpec` on the default test app, `MonkoGlobalLocksE2eSpec`
  on a blueprint with `cluster = { useMonko() }`; each pins its repo class. The app's provider holds the lock; one beat
  is written with the holder's own token at a moment distinct from `created`; the listing must show it, and the raw
  body must not contain the token. Trap met: the test app starts on the first request and
  `GlobalLocksCleanupOnAppStarting` releases every lock of this server id — the spec issues a request before taking
  the lock.

### Found, not changed (need a decision or are out of scope)

- (The startup race of the dead-server rule, listed here in round 0, is fixed in round 1 — see "Cleanup worker".)
- A cancelled `lock()` surfaces as `LocksException.Execution(cause = CancellationException)`
  (`VaultGlobalLocksProvider.kt:155-156`, pre-existing, Primitive too), so the caller's parent sees a failure rather
  than a cancellation.
- Clock skew and mixed ttl/interval settings during a deploy: in the ledger as a maintainer decision.
- An insert that succeeds in the DB but fails on the client leaves our own lock blocking us until the ttl (it is not
  beaten). Previously 15 min.

## Test evidence

All runs via `console/wt-gradle.sh` (worktree DBs `ultra_wt_locks_heartbeat`); counts from
`build/test-results/**/TEST-*.xml`, failing cases read from the console.

**Round 2 MINOR batch** (rebased onto master first):

- `:funktor:cluster:jvmTest` (all specs): **154 tests, 0 failures, 0 skipped**, three runs in a row — the saturated-IO
  test now runs on Karango too. Locks: in-memory 36, Karango 36, Monko 36, Primitive 8, `GlobalLockEntryModelSpec` 1.
- `:funktor:all:jvmTest` (all specs): **180 tests, 0 failures** (master's T2 added specs), incl. both e2e specs.
- Full suite `console/wt-gradle.sh --full-suite` (BUILD SUCCESSFUL, 8m51s, no `^e:`, no Kotlin daemon failure):
  **68 test targets, 15769 tests, 0 failures, 0 errors, 7 skipped.** `funktor/cluster` 154/0, `funktor/all` 180/0.

**Round 1 fixes** (rebased onto master `eb7b8d91` first):

- `:funktor:cluster:jvmTest` (all specs): **150 tests, 0 failures, 1 skipped**, three runs in a row. Locks: in-memory
  `VaultGlobalLockProviderSpec` 34, `KarangoGlobalLockProviderSpec` 35 (1 skipped: the saturated-IO test, see the
  notes), `MonkoGlobalLockProviderSpec` 35, `PrimitiveGlobalLockProviderSpec` 8, `GlobalLockEntryModelSpec` 1.
- `:funktor:all:jvmTest` (all specs): **160 tests, 0 failures**, incl. both e2e specs and `ClusterApiSpec`.
- Full suite `console/wt-gradle.sh --full-suite` (12m29s, no `^e:`, no Kotlin daemon failure): **68 test targets,
  15726 tests, 1 failure, 0 errors, 8 skipped.** `funktor/cluster` 150/0 (1 skipped), `funktor/all` 160/0,
  `funktor-demo:server` 47/0 and `monko:core` 249/0 (the round-0 environment failures are gone after the rebase).
  The one failure is `ultra:cache` `FastCacheOnEvictionSpec` "onEviction fires when expireAfterAccess evicts" on
  linuxX64 (expected two evictions within its time window, saw none): untouched code, passed in the round-0 suite, and
  `:ultra:cache:linuxX64Test` re-run alone: 123 tests, 0 failures — a timing flake under full-suite load.

**Round 0** (before the review):

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
| **Round 1** | | | |
| W1 | dead-server rule applies to locks with a token too | "The cleanup worker leaves a beating lock alone, though its server has no beacon (yet)" (all 3) | the beating lock released |
| W2 | dead-server rule runs before the beacons were read | "Before the beacons were read once, … releases no lock …" (all 3) | legacy lock released on the empty list |
| W3 | `setAll` never marks the list populated | "Once the beacons were read, … releases a lock without a token whose server is dead" (all 3) | dead server's lock kept |
| W4 | restriction inverted (`token.isNotEmpty()`) | W1's and W3's tests (all 3) | both |
| X1 | `releaseBy` removes by key | "releaseExpired and releaseBy leave a lock alone that changed after they read it" (all 3) | the beaten lock released |
| X2 | the takeover removes by key | "The takeover leaves a lock alone that changed after it was read" (all 3); on Monko also the contenders and ObjectId-key tests | taken over; Monko: `remove` deletes a fresh lock / misses the ObjectId-like key |
| A1 | a contender runs while the lock is held (insert failure counted as acquired) | "Handler throwing an error" (all 3, failure lists "2nd, while the 1st still ran") and 10 more exclusion tests | the restored flag; without it this test stayed green |
| B1 | the `heartbeatInterval < legacyExpiry` require dropped | "A heartbeat interval at or beyond the legacy expiry is rejected" (in-memory spec) | no exception |
| C1 | heartbeat on plain `Dispatchers.IO` | "A holder keeps beating while blocking work saturates Dispatchers.IO" (in-memory, Monko) | silent for ≥ ttl |
| M13b | heartbeat on the collector's dispatcher (M13 re-run) | "A handler that blocks its thread keeps its lock" (all 3), saturated-IO test (in-memory, Monko) | — |
| C2 | experiment: the saturated-IO test enabled on Karango, with the fix in place | red (Karango) | confirms the skip reason: the Karango driver queries on `Dispatchers.IO` |
| D1 | `GlobalLockEntryModel.lastHeartbeat` without default | `GlobalLockEntryModelSpec` | `MissingFieldException` |
| **Round 2 batch** | | | |
| K1 | Karango heartbeat back through `KarangoDriver.query` (its `Dispatchers.IO` hop) | "A holder keeps beating while blocking work saturates Dispatchers.IO" (Karango) | silent for ≥ ttl. C2's skip is gone: the test runs on Karango |
| C1 (re-run) | heartbeat on plain `Dispatchers.IO` | the saturated-IO test, now on all 3 | silent for ≥ ttl |
| T1 | `ttl <= legacyExpiry` → `<` | "A ttl of 15 minutes is accepted" (in-memory spec) | `IllegalArgumentException` |
| T2 | the ttl bound dropped | "A ttl longer than 15 minutes is rejected" (in-memory spec) | no exception |
| H1 | no `withTimeout` around one beat | "A hanging heartbeat times out, and the next beat retries" (all 3) | the beat hangs: heartbeat never moves |
| L1 | worker log shows the heartbeat for a legacy lock too | "Once the beacons were read, … server is dead" (all 3) | log lacks `expires …` |
| I1 | `isLegacy` inverted | 40 failures across the token/legacy tests (all 3) | — |

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `GlobalLocksCleanupWorker.kt:27-34`, `GlobalServerList.kt:8-9` | The dead-server rule releases EVERY lock in the cluster on a server's first worker tick (the server list is still empty), and a peer deletes a booting server's new locks during the 10 s beacon lag | fix | Verified by the coordinator: the list starts empty and only `ServerBeaconCleanupWorker` fills it, after a DB read (`ServerBeaconCleanupWorker.kt:24-31`); every worker is due on the first tick (`WorkerTracker.kt:133`, Epoch) and all start together (`WorkersFacade.kt:52-75`); `GlobalServerList` is a true singleton (`index_jvm.kt:114`). Pre-existing, but it defeats this feature's guarantee, in a file this change edits. Fix: the rule applies to token-less locks only (token locks die by heartbeat in 60 s, before the 3-min beacon rule), and is skipped until the list has been populated once |
| 1 | MAJOR | `VaultGlobalLocksProvider.kt:189-196`, `:208-211` | The read-then-remove race fixes (`releaseBy` and the takeover → `removeIfUnchanged`) are pinned by no test: a delete-by-key mutant stays green | fix | Verified: no test changes a lock between `list()`/`findById` and the remove; the mutation table has no row for it (M4 is the holder's release, M10 the worker predicate). Fix: wrapper-repo tests on all three backends that beat or replace the lock between the read and the remove; record both mutants |
| 1 | MINOR | `GlobalLocksProvider.kt:100` | `releaseExpired()` is abstract: an outside implementor stops compiling | SETTLED by the maintainer (2026-10-19): stays abstract | No outside implementor is known; a default body would silently give a new provider the legacy `expires` rule. Maintainer list |
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

**Round 2** (`reviewer-xhigh` ×3, two-phase; base `eb7b8d91`): **0 CRITICAL, 0 MAJOR → clean.** Both round-1 MAJOR fixes
hold (all three reviewers, duty (b)); no rejected CRITICAL/MAJOR existed (duty (c)). The open MINORs go into one
maintainer-requested batch ("fix the MINORs"), checked by one reviewer afterwards.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MINOR | `VaultGlobalLocksProvider.kt:69-71`, `KarangoDriver.kt:120` (all three) | Row 12 is incomplete: on Karango every query hops onto `Dispatchers.IO`, so a saturated pool still stalls the beat; the comment claims protection the default backend lacks | fix (batch) | Call the async driver directly in `KarangoGlobalLocksRepo.heartbeat` (`buildAqlQuery` + `driver.codec` + `driver.arangoDb.query(...).await()`, no IO hop), enable the saturation test on Karango; if the 7.x async driver turns out to block, narrow the comment instead |
| 2 | MINOR | `VaultGlobalLocksProvider.kt:75-81` (security, implementation) | The bound should be `ttl <= legacyExpiry` (old servers judge by `expires = beat + 15 min`); the `<` boundary is untested | fix (batch) | `require(ttl <= legacyExpiry)` plus boundary tests |
| 2 | MINOR | `VaultGlobalLocksProvider.kt:178-185` (domain) | A single beat has no timeout: a hung call stops the beating with no log line | fix (batch) | `withTimeout(heartbeatInterval)` around the beat, into the existing "failed, retrying" path |
| 2 | MINOR | `GlobalLocksCleanupWorker.kt:45,62,70` (implementation) | The log lost `expires`, the field that decides a legacy lock's death (prints `created` twice) | fix (batch) | Show `expires` for token-less locks |
| 2 | MINOR | five places (implementation) | "empty token = legacy" is written out five times | fix (batch) | One helper; an EXTENSION property (or function), so Slumber does not serialize it into records |
| 2 | MINOR | `VaultGlobalLocksProvider.kt:66` (domain) | KDoc "the only field versions without heartbeats read" is false: old servers also read `serverId` (beacon rule, release by server id) | fix (batch) | Correct the KDoc; the docs follow-up carries the upgrade caveat (three OLD-server paths can still delete a live token lock) |
| 2 | MINOR | `VaultGlobalLocksProvider.kt:206-210` (domain) | The contender's `ttl` judges, and `ttl` is not stored on the lock: mismatched settings across servers take live locks | fix (batch, KDoc) | Document "ttl and heartbeatInterval must be uniform across the cluster; change them in two deploys" (storing a deadline is not worth the record change: not configurable through `useKarango`/`useMonko`) |
| 2 | MINOR | `VaultGlobalLocksProvider.kt:160,174-184` (security) | A DB stall/failover of ~60 s lets a contender take over a live lock (all holders alive); the demo's last-owner rule (`B2bMembersApi.kt:205-207`) is exposed | fix (batch, KDoc) | No code change under the decided ttl: the KDoc states "ttl must exceed the worst DB stall + clock skew + one heartbeat interval; invariant-protecting writes need a conditional write, not the lock alone" |
| 2 | MINOR | `funktor-demo/.../ClusterShowcaseApi.kt:34,145-163` (security) | Pre-existing: the demo's PUBLIC `getActiveLocks` shows anonymous callers lock keys and `serverId`s (hostname, port, MAC addresses) | out-of-scope → follow-up | Recorded in the parallel plan's backlog |
| 2 | — | rows 11, 14 | Clock skew; stuck handler | still maintainer-decision | Facts added in round 2: a taken-over lock lets two owner demotions both pass the demo's `wouldOrphanOrg`; the CLI `funktor:cluster:release-locks` does free a beating lock (`removeIfUnchanged` succeeds between beats); the task's "Why" bullet 2 still needs correcting if "no max hold" stands |

**Verdict:** gate PASS (round 2 clean; e2e present on both backends and green). MINOR batch follows; rows 11 and 14 stay with the maintainer.

**Batch review** (one `reviewer-high`, the round-2 MINOR batch): 0 CRITICAL, 0 MAJOR, 3 MINOR — all fixed by the
coordinator: the `isLegacy` KDoc claimed only `expires` judges a legacy lock (its server's beacon does too); `isLegacy`
made `internal`; the two heartbeat-failure tests counted every "heartbeat failed" warning, so a slow real beat under load
(timeout = interval = 50 ms) could add one — they now count the injected causes. Verified clean: the direct Karango call
(same AQL and bind encoding; only the profiler entry and the `KarangoQueryException` wrapping are lost, nothing depends on
them), the `ttl <= legacyExpiry` boundary, the timeout/`ensureActive` interplay, `isLegacy` in all five places, the log, the
KDoc constraints, and the saturated-IO test (K1/C1 turn it red on Karango).

## History

- 2026-10-21: numbered ULTRA-00219 (was `docs/tasks-archive/2026-10/20261019-global-locks-heartbeat.md`).
