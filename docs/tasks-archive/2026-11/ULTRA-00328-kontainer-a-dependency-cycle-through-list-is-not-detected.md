# ULTRA-00328: Kontainer: a dependency cycle through List is not detected

> **Status (2026-11-14): DONE.** Batch K2, merged into master as `8778c4a9`: strict gate round 1 CLEAN. Behaviour changed; the docs follow in ULTRA-00338 (kontainer docs).
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

With `A(List<B>)` and `B(A)`, validation passes and `get(A)` ends in `StackOverflowError`. The cycle check does not follow `List<T>` parameters, although `List` counts as a dependency for promotion.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/ValidationCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

The maintainer asked for a generic mechanism: "Can every injection type, plain, list, lookup etc. announce all the
other services it wants to inject? If yes we can always iterate those and by doing so force each injector to be part
of the cycle detection." And on the proposal below: "yes do it this way."

The announcements exist already: every `ParameterProvider` answers `getInjectedServiceTypes(blueprint)` and
`getProvisionType()` (Direct or Lazy); `ForListOfServices` says Direct. Today only the debug output uses them.
`DependencyLookup` decides by the raw parameter type with its own hard-coded list (`DependencyLookup.kt:36-62`), which
left `List` out of the cycle check by hand (`:56`).

So:

- `DependencyLookup` walks each definition's `ParameterProvider`s (`ServiceProducer.params`).
- The dependency graph (promotion to SemiDynamic) uses `getInjectedServiceTypes` of every parameter.
- The cycle check uses the same answers, only for parameters whose provision is Direct.
- The hard-coded type checks in `DependencyLookup` go. A new injection form only answers the two questions and
  cannot be forgotten.
- A cycle through a plain `List<T>` is then a validation error with the existing message ("Hint: Use Lazy<T> injection
  to break the cycle"). `Lazy<T>`, `Lazy<List<T>>` and `Lookup<T>` stay out of the cycle check (Lazy).

To verify by test before changing anything: the provider's candidate sets equal what `DependencyLookup` computes today
for every form (interfaces with several implementations, nullable parameters, `InjectionContext`, the built-in
`Kontainer`), so promotion does not change; the characterization specs and the promotion mutants are the proof.

## Plan as built (track D, batch K2, 2026-11-14)

- `DependencyLookup` walks each definition's `ParameterProvider`s and reads only their announcements.
  - Each announced type maps through `superTypeLookup.getAllCandidatesFor`: the candidates of a declared type, and
    unchanged for `List` and `Lookup` announcements, which are candidates already.
  - Promotion edges come from every parameter.
  - Cycle edges come from parameters whose provision is Direct (plain, nullable, `List`, `InjectionContext`). The
    Lazy ones (`Lazy<T>`, `Lazy<List<T>>`, `Lookup<T>`) stay out, as today.
- The hard-coded type checks in `DependencyLookup` go.
- `getInjectedServiceTypes` keeps its signature. `DependencyLookup` receives the blueprint under construction after
  `superTypeLookup` is set, the only member the announcements read; no public API change.
- What breaks a cycle today, and what the announcements say:

  | Injection | Breaks a cycle today | Announces |
  |---|---|---|
  | `Lazy<T>`, `Lazy<List<T>>`, `Lookup<T>` | yes, deliberately | Lazy |
  | `List<T>` | yes, by omission | Direct |
  | plain, nullable, `InjectionContext` | no | Direct |

  So only `List` changes.
- Lazy injections in the repo keep working, as they stay out of the cycle check. Searched: `Lookup<Repository<*>>`
  (vault `Database`), `Lookup<InsightsCollector>`, `Lookup<IncomingParamConverter>`, `Lazy<List<...>>`
  (`ValidateRoutesOnAppStarting`), the `Fixtures(repo: Lazy<...Repo>)` loaders, `Lazy<Kronos>` hooks,
  `Lazy<AuthSystem.Deps>`, `Lazy<Adapter>`, `Lazy<LogsStorage>`.

## Built (batch K2, 2026-11-14)

**Status: built on `wt/kontainer`, not merged; a strict gate follows.**

As planned: `DependencyLookup` reads only the announcements. Each announced type maps through the candidates;
promotion edges come from every parameter, cycle edges from Direct ones. The hard-coded type checks are gone.
- Promotion is unchanged: all other promotion and injection-form characterization tests passed untouched. The one
  that changed is the ambiguous nullable pin, which ULTRA-00335 turned.
- The `funktor:all` suite, which boots the full funktor blueprint, is green, so no `List` cycle exists in it.
- Lazy cycles in the repo keep working; they stay out of the cycle check as before (list in the plan).

Pins turned, before -> after:
- `ValidationCharacterizationSpec`, "CURRENT BEHAVIOUR: a cycle through List is not reported and overflows the stack
  at get":
  - `StackOverflowError` at `get` -> `KontainerInconsistent` at the first `create`
  - message `Circular dependency detected: ListCycleOne -> ListCycleTwo -> ListCycleOne`, plus the `Lazy<T>` hint
- New: a cycle through `Lazy<List<T>>` is not reported and works.

Mutation:

| Mutant | Result |
|---|---|
| L1: cycle check follows Lazy injections too | RED, 3 / 556, with `generateDocs` skipped. With docs generation on, the build already fails on the published example `LazyInjectionCycleBreakerExample`, a legitimate lazy cycle in the repo |
| L2: cycle check skips `List` | RED, 1 / 555 |
| L3: promotion only from Direct injections | RED, 10 / 555 |
| L4: announced types not mapped to candidates | RED, 4 / 555 |
| P2: dependents one step only (re-run) | RED, 27 / 555 |
| P6a: `Lazy<T>` announces nothing | RED, 7 / 555 |
| P6e: `InjectionContext` announces nothing | RED, 5 / 555 |

Changelog (behaviour change, no compile signal, can stop an app at boot): `ultra/kontainer`: the cycle check and the
dependency graph read what each parameter provider announces, so every injection form takes part.
- A dependency cycle through `List<T>` is now a validation error at the first `create` ("Kontainer has circular
  dependencies!", with the hint to use `Lazy<T>`). Before, validation passed and `get` ended in
  `StackOverflowError`.
- The likeliest hit is a service that injects a `List` of its own super type, which puts it in its own list:
  `CompositeHandler(all: List<Handler>) : Handler`. Before, it booted and `get` overflowed; now the first `create`
  fails. `Lazy<List<Handler>>` only defers this, because the list still contains the composite. The fix is to
  register the composite under a type outside the list.
- The `Lazy<T>` hint helps only when `.value` is first read after construction.

`Lazy<T>`, `Lazy<List<T>>` and `Lookup<T>` still break cycles; promotion to SemiDynamic is unchanged (ULTRA-00328).

Commits:
- `8dffdf22`: today's pins
- `4b404195`: the plans
- `01cbd47d`: the fix, with tests first (8 red before it, all green after)
- `2281c29d`: a `List` cycle with two candidates, added for mutant A2

Suites (XML, `--rerun`, 0 failures):

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 556 |
| `:ultra:log:jvmTest` | 74 |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 |
| `:funktor:cluster:jvmTest` | 162 |
| `:funktor:all:jvmTest` | 289 |

The repo-wide compile sweep is green (459 tasks, 0 errors). No blueprint in the repo turned red.

## K2 gate round 1 (strict, blind, reviewer-fable-xhigh)

The same ledger is written into ULTRA-00327, 00328, 00334 and 00335.

**Verdicts:** implementation CLEAN (6 MINOR); domain and security CLEAN (6 MINOR); no CRITICAL, no MAJOR. Neither
reviewer ran anything. The close-out has no production change (tests and texts only; commits `54b8e8c1`,
`a9ccf8c0`, plus the ticket commit).

**Found CLEAN:**
- Both reviewers traced all seven parameter providers against master: kinds and outcomes are unchanged outside
  rules 1 to 4.
- Rule 3 (an ambiguous single-service injection gets no cycle edges) cannot hide a real cycle.
- The nullable survey of seven is exact; funktor-demo adds none.
- There is no `List` cycle in the repo.

### Findings and outcomes

1. **The "exactly one announced type" conjunct was unpinned.** New test: `PartOwner(all: List<Part>)` with
   `MidPart(owner)` and its subclass `LeafPart`, all registered, is a cycle. Mutant D1 ("ambiguous when any announced
   type has several candidates"): RED, 1 / 561.
2. **The composite in its own list.** New tests:
   - `CompositeHandler(all: List<HandlerBase>) : HandlerBase` is `Circular dependency detected: CompositeHandler ->
     CompositeHandler`.
   - Characterization row: with `Lazy<List<HandlerBase>>` it boots, and the list contains the composite itself.
   - Mutant D2 (the owner left out of its own edges): RED, 2 / 561.
3. **The guard in `createChecked`.** New test through the public `ServiceProviderFactory(...)` constructor and
   `newKontainer` on an invalid blueprint that never ran `create`: `KontainerInconsistent`. Mutant D3 (guard deleted):
   RED, 1 / 561.
4. **Rule 2 for the other forms.** One test: a `List` cycle is found when the owner is a factory, a dynamic or a
   prototype. Mutant L2 (cycle check skips `List`), re-run: RED, 5 / 561.
5. **`Lazy<Absent?>` with no candidate is null.** Added to the existing test. Mutant D5 (that `ForLazyService` arm
   turned into an error): RED, 2 / 561.
6. **Literal class names** in the expectations K2 added (`ValidationCharacterizationSpec`,
   `InjectionFormsCharacterizationSpec`); master's older ones are left.
7. **Evidence.** The other suites that build a blueprint were run: `funktor:core`, `rest`, `auth`, `messaging`,
   `logging`, `codegen`, `saas`, and `funktor-demo:server` (it has a `test` task). All green; counts below.
8. **ULTRA-00334 changelog** is worded for the general case, with the `A(i: I)`, `B(a: A) : I`, `C : I` example and
   the parameter errors the cycle used to hide. Both are pinned by a new test, which mutant A1 kills as well (RED,
   3 / 562).
9. **ULTRA-00328 changelog:** it can stop an app at boot; the composite in its own list; `Lazy<List<T>>` only defers
   the cycle; the `Lazy<T>` hint helps only when `.value` is read after construction.
10. **ULTRA-00328 "Built":** "all other promotion and injection-form characterization tests passed untouched"; the
    one that changed is the ambiguous nullable pin, turned by ULTRA-00335.
11. **ULTRA-00335 "Built":** the `Lazy<T?>` claim with no candidate is now pinned (item 5); mutant recorded.
12. **ULTRA-00327 mutation table:** "validate on every create instead of remembering" is recorded as equivalent next
    to Q3. `validate()` tracks its throwaway kontainer, which is not observable from outside.
13. **This ledger and the status line.**

### Counts after round 1 (XML, `--rerun`, 0 failures)

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 562 |
| `:ultra:log:jvmTest` | 74 |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 |
| `:funktor:cluster:jvmTest` | 162 |
| `:funktor:all:jvmTest` | 289 |
| `:funktor:core:jvmTest` | 775 |
| `:funktor:rest:jvmTest` | 124 |
| `:funktor:auth:jvmTest` | 322 |
| `:funktor:messaging:jvmTest` | 67 |
| `:funktor:logging:jvmTest` | 14 |
| `:funktor:codegen:test` | 104 |
| `:funktor:saas:jvmTest` | 50 |
| `:funktor-demo:server:test` | 52 |

The kontainer suite had 561 tests in the run of all fourteen suites; it went to 562 with the item 8 test, re-run
alone. No blueprint in the repo turned red in any of these suites.

## The work

- Decide whether a cycle through `List`, `Lazy<List>` and `Lookup` is an error at validation (as for plain parameters) or allowed where the injection is lazy; a non-lazy `List` cycle must be reported at validation, not as a stack overflow at `get`.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
- 2026-11-14: DONE.
