# ULTRA-00328: Kontainer: a dependency cycle through List is not detected

> **Status (2026-11-14): NOT STARTED.** 
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

## The work

- Decide whether a cycle through `List`, `Lazy<List>` and `Lookup` is an error at validation (as for plain parameters) or allowed where the injection is lazy; a non-lazy `List` cycle must be reported at validation, not as a stack overflow at `get`.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
