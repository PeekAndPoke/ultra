# ULTRA-00329: Kontainer: a global singleton's Lookup stays bound to the first kontainer and keeps every prototype it creates

> **Status (2026-11-24): IN PROGRESS.** track D builds K3a and K3b; gate 1 next
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

A global singleton that injects `Lookup<T>` keeps the lookup of the first kontainer it was created in. Prototypes fetched through it are created by, and recorded in, that kontainer's provider, also when the singleton is used from later kontainers: the provider's `instances` list keeps every one, and the singleton keeps that kontainer alive. Memory grows for as long as the lookup is used; a per-request service can reach into another request's kontainer.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/InjectionFormsCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

Two effects, two causes (read from the code): the memory growth comes only from the introspection record (every
provider appends each created instance, with a reference to it, to `instances`, `ServiceProvider.kt:241-250`; readers
are the debug dump and the insights collector; the recording is always on); the retained first kontainer comes from
the lookup's own reference to its kontainer (`LazyServiceLookup`). `KontainerTracker` holds kontainers weakly.

The maintainer: "let us rethink how we collect the stats ... probably plain numbers / counters everywhere is totally
enough", and "ok to both":

1. **Counters in place of instance lists.** Each provider keeps a count, the time of the first and of the last
   creation, and the class name of the last created instance (a factory can return a subclass of the registered type).
   All fixed-size; no reference to an instance is kept. `KontainerTools` (dump, `DebugInfo`) and the insights collector
   and GUIs (`funktor/insights` `KontainerCollector`, `funktor/inspect` `KontainerTab`, `slices.kt`, the Vue GUI) show
   the count and the two times where they list every instance today. The insights wire shape changes: plan it with
   the insights track's rules (records on disk, the graph API).
2. **A global singleton's lazy injections (`Lookup<T>`, `Lazy<T>`, `Lazy<List<T>>`) bind to a blueprint-wide kontainer
   without request data**, not to the kontainer of the request that happened to create the singleton. By definition
   a global singleton reaches nothing dynamic, so both kontainers resolve the same services for it; prove that with
   the characterization specs.

Added from the K1 review (implementation reviewer, 2026-11-14, by reading): the retained first kontainer also keeps
that request's override lambdas and values reachable for the blueprint's lifetime (the kontainer holds them for
`clone`). That is the same class of retention as ULTRA-00337, by another path; change 2 above removes it.

## The work

- Decide what a `Lookup` in a global singleton is bound to (promotion to SemiDynamic already happens when `T` is dynamic; the gap is prototypes and the record of instances). At least: no unbounded record of prototype instances behind a long-lived lookup.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## Plan answers (maintainer, 2026-11-24)

Track D's plan (2026-11-23) asked five questions; answered one at a time.

- **Q1, scope of change 2:** the whole creation of every global singleton happens in the blueprint-wide kontainer,
  with all its parameters (direct, `Lazy`, `Lazy<List>`, `Lookup`, `List`), not only its lazy ones. The maintainer:
  "yes create all as you recommended". This closes the transitive case (a static prototype holding a `Lookup`,
  injected directly into a global singleton).

- **Q2, visibility:** the blueprint-wide kontainer stays internal; tests reach it from inside the module. Its
  creations show in no request dump and no insights record. Process-wide counters (for example in the insights
  graph endpoint) would be a follow-up ticket if wanted. The maintainer: "yes keep it internal".

- **Q3, insights wire shape:** per service `{"cls", "type", "count", "firstCreatedAt", "lastCreatedAt"}`, nothing
  else. **No `lastCls`**: this drops "the class name of the last created instance" from decision 1 above (it differs
  from `cls` only when a factory returns a subclass, and for prototypes it shows an arbitrary last one). **No
  backward compatibility**: the GUIs read this shape only and turn no old instance list into counters; the new
  fields have defaults, so a record stored by 0.119.0 reads as a service with no creations instead of an error. The
  maintainer: "No need for backward compat. Simplify the model and the code", then "yes take it like that".

- **Q4, the instance API:** removed outright, no deprecation: `ServiceProvider.instances`,
  `ServiceProvider.CreatedInstance`, `DebugInfo.InstanceDebugInfo`, `DebugInfo.ServiceDebugInfo.instances`,
  `KontainerCollector.Data.Service.instances`, `Data.Instance`. A deprecated `instances` could only return an empty
  or a made-up list. The replacement (`provider.created.count` and the two times) goes into the changelog under
  breaking changes. The maintainer: "yes remove them".

- **Q5, prototype creation:** lock-free; the count is updated atomically, the two times without a lock (they may be
  off by microseconds under overlap, fine for a statistic). The lock guarded only the `instances` list; kept, it
  would serialize every prototype created through a global singleton across all requests. The maintainer: "yes
  make it lock-free ... check other service providers for the same pattern and possible simplification" (the check
  is the next entry).

- **Locks elsewhere (from Q5's check), all three taken:** `ForSingleton` keeps its double-checked lock (it
  guarantees the one instance) and needs no counter: the count is 0 or 1 from `instance != null`, with one creation
  time stored beside it. `ForOverride` records constants: count 1, both times the moment the kontainer was made.
  `ServiceProviderFactory` keeps its providers in a `ConcurrentHashMap` with `computeIfAbsent` instead of one lock
  around a plain map: after change 2 every `Lookup` of a global singleton goes through the blueprint-wide
  kontainer's factory, so that lock would be shared by all requests. `LiveKontainerTracker` is unchanged (debug
  only, a tiny critical section). The maintainer: "yes take all three".

- **Area edits allowed:** track D may edit `funktor/insights` (collector, models, `TAB-SPECS.md`), `funktor/inspect`
  (`KontainerTab`), `funktor/codegen` (TS models, `KontainerTab.vue`, the `vue-verify` and
  `funktor-demo/sdkgen-app` copies). ULTRA-00190 has no active work there; the paused `wt/insights-kraft` overlaps
  in two lines of `KontainerCollector.kt` and resolves that when it resumes. The maintainer: "yes allow the edits".
- **Where the plan changes by these answers:** K3a has no `lastCls` (and no mutant for it); K3c writes v3 only and
  has no frozen v2 record to render; K3d reads v3 only (no folding of a v2 list, no `refresh.mjs` v2 target), an
  old record shows as a service without creations; K3a or K3b also takes the three lock changes above.
- **Gates:** two strict gates, after K3b (kontainer: domain and concurrency) and after K3d (insights wire and GUIs).
  While the Fable credits are used up (week of 2026-11-24), each Fable rung runs on Opus one level higher: rung 2 as
  Opus high, rung 3 as Opus xhigh (maintainer, 2026-11-24).

## Progress (track D, `wt/kontainer`)

| Batch | What | State |
|---|---|---|
| K3a | counters (`ServiceProvider.created`), lock-free `ForPrototype`, `ForSingleton` time beside the instance, `ForOverride` constants, `ServiceProviderFactory` on `ConcurrentHashMap`; `ultra/log` compile edit | committed, mutants 10/10 RED |
| K3c, JVM half | `KontainerCollector.Data` v3 `{cls, type, count, firstCreatedAt, lastCreatedAt}`, Slumber spec, `funktor/all` slice spec | committed ahead of K3b (K3a removed the API the collector read, and the change 2 suites need `funktor/insights` to compile); mutants 6/6 RED |
| K3b | blueprint-wide kontainer (`KontainerBlueprint.globalKontainer`, lazy, untracked); `ForSingleton` with kind Static creates in it | mutants 7/7 RED |
| gate 1 | strict, kontainer and the JVM wire | PASS in round 1; MINOR batch applied once (ledger below) |
| K3d | GUIs (`funktor/inspect`, `funktor/codegen` TS and Vue, fixtures), `TAB-SPECS.md` | committed, mutants 12/12 RED |
| gate 2 | strict, GUIs and `TAB-SPECS.md` | PASS in round 1; final batch applied once (ledger below) |

Deviation from the batches of the plan: the JVM half of K3c moved before K3b, for the reason in the table.
K3d reads v3 only (Q3): an older record's instance list is not read, and `readInstantMillis` reads only Slumber's
`{ts}` object (a bare number is no longer taken for epoch seconds). After gate 2 (maintainer, 2026-11-24: "drop the
v1 path too, no backward compat") the v1 (fat) shape is not read at all and shows as the raw tree; a v2 record reads
as services without creations. A `DynamicOverride` names no class: it shows as "handed in at create".
The plan's `ForPrototype` counter is one `AtomicReference` snapshot updated by CAS with first = min and last = max:
lock-free as Q5 asks, and the snapshot never has `lastAt < firstAt`.

## Gate 1 ledger (strict; round 1, Opus high standing in for rung 2)

Result: PASS in round 1, zero CRITICAL, zero MAJOR. The MINORs were applied once as a batch, not re-reviewed.

| Charter | Severity | Finding | Disposition |
|---|---|---|---|
| impl | MINOR | docs show the removed instance API (`introspection.astro`, `docs-site/src/data/llms/kontainer.md`, the stale `ultra/kontainer/docs/ultra::docs/index.md` copy) | out of scope: DOCS follow-up at archive |
| impl | MINOR | KDoc of `globalKontainerLazy` said none of its creations show in a request's dump; the global singletons do (shared providers) | fix (M1) |
| impl | surplus | duplicate test in `ServiceProviderCreatedSpec` ("same creation in every kontainer") | fix (M4): removed, the characterization test stays |
| impl, security | surplus | `globalKontainerLazy` plus a getter | reject: reviewers split, the domain reviewer argues to keep it; a one-line convenience |
| impl, security | surplus | `DebugInfo.CreatedDebugInfo` mirrors `ServiceProvider.Created` | reject: the `DebugInfo` mirror pattern of the module (domain reviewer) |
| security | MINOR | no concurrency test of the blueprint-wide lazy | fix (M2) |
| security | surplus | JVM defaults on `KontainerCollector.Data.Service` | reject: Q3 decision |
| domain | MINOR | docs, as the impl docs finding | as above |
| domain | MINOR | the "everything a Static service reaches is Static" guard checks the kinds against their own announcements | fix (M3): a runtime check that the blueprint-wide kontainer provides no non-Static class |

CLEAN (verified by the reviewers):
- No request data reaches the blueprint-wide kontainer.
- The Static premise holds for every parameter form.
- No deadlock: the lazy initializer runs no service code, and there is no reentrant `computeIfAbsent`.
- The counter CAS is exact and side-effect free.
- The singleton's `createdAt` and `instance` are published safely.
- No unbounded growth.
- The insights wire leaks nothing new.

Fix-batch mutants:
- M2, `SYNCHRONIZED` to `PUBLICATION`: survives, as an equivalent mutant. `PUBLICATION` hands every caller the one published value, and the initializer has no side effect but an allocation, so no test can tell them apart.
- M2, `SYNCHRONIZED` to `NONE`: RED (the concurrency test; prototypes counted in a kontainer other than the published one).
- M3, `ForLazyService` announces nothing: RED, but on the announcement guard, which runs first.
- M3, `ForLazyService` also resolves the unannounced dynamic `Kontainer`: RED on the runtime check only, the one test in the suite that sees it.

## Gate 2 ledger (strict; round 1, Opus high standing in for rung 2)

Result: PASS in round 1, zero CRITICAL, zero MAJOR; the security charter had no findings. The final batch was applied
once, not re-reviewed.

| Charter | Severity | Finding | Disposition |
|---|---|---|---|
| impl | MINOR | the instances graph of a full (v1) record is empty, since v1 instance lists are not read | fix, by removing the v1 path (maintainer decision below) |
| impl | MINOR | no-op counter reset when graph-only services are joined (`slices.kt`, `slices.ts`) | fix: removed |
| impl | MINOR | long lines (`KontainerTab.kt:34`, `KontainerTab.vue:10`) | fix: re-wrapped |
| domain | MINOR | a `DynamicOverride` names `UserProvider`, an interface, as the class it creates | fix: no class named, "handed in at create" in both GUIs and `TAB-SPECS.md` |
| domain | MINOR | `TAB-SPECS.md` "Three shapes on disk; the readers read two." | fix: two shapes read, v1 as the raw tree |
| domain | surplus | no-op counter reset | same as the impl finding |
| domain | question | keep the v1 reading path? | maintainer decision, 2026-11-24: "drop the v1 path too, no backward compat": removed from both readers and tabs, `record-fat.json` and the v1-only cases deleted |
| domain | note | a v2 record shows "0 instantiated" and "-" cells | reject: Q3 decision ("reads as a service with no creations") |
| security | none | no findings | |

CLEAN (verified by the reviewers):
- No raw HTML sink in either tab or in the graph labels.
- The readers survive wrong types, NaN, Infinity, negative counts and `__proto__`.
- The per-instance DOM growth is gone.
- The fixtures carry no local paths or secrets.
- Writer and readers agree on every field: `{ts}` millis, count 0 absent, nulls.
- Kraft/Vue parity of the cell, the sort, the summary and the graph pools.
- The fixture drift is attributed correctly: `TemplateInsightsCollector` removed, `InsightsConfig` in `FunktorConfig`.

Final-batch mutants:
- Kraft reads v1 again: RED on the raw-tree pin of `InsightsPagesSpec`.
- Vue reads v1 again: RED on four cases (both v1 pins, both "neither shape" cases).
- Kraft names the override's class: RED on the new "handed in at create" test.
- Vue names the override's class: RED on the parity definition sort.

## History

- 2026-11-14: created on the maintainer's word.
- 2026-11-23: re-confirmed by the maintainer before implementing ("yes confirmed"): both changes as decided,
  including the change of the insights wire shape (lists of instances become a count and two times); track D writes
  a plan without code first.
- 2026-11-24: the plan's five questions, the lock check and the area edits answered (Plan answers).
- 2026-11-24: IN PROGRESS. track D builds K3a and K3b; gate 1 next
- 2026-11-24: K3a, the JVM half of K3c and K3b committed; gate 1 PASS in round 1, MINOR batch M1 to M4 applied.
- 2026-11-24: K3c (TAB-SPECS) and K3d committed; gate 2 PASS in round 1, final batch applied (v1 path dropped on the
  maintainer's word, override names no class).
