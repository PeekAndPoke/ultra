# Vault test-gap sweep — find untested code in `ultra:vault` and cover it

**Status:** IDEA / not scheduled. Raised 2026-07-26 by the user while relocating `isForbiddenInId`
into `ultra:common`.

## Why

`ultra/vault/src/jvmMain/kotlin/helpers.kt` was the trigger. It has four public declarations and
`HelpersSpec.kt` tests exactly one of them (`ensureKey`, three cases). Two of the others —
`filterValueIsInstanceOf` and `resolveAll` — have **zero callers anywhere in the repo**, which is why
the IDE greys them out, and zero tests. That is the worst combination: published API that nothing
exercises, so nothing would notice if it broke.

The suspicion is that this is not unique to `helpers.kt`. Vault is the storage foundation under
Karango, Monko, funktor's auth records, messaging and saas — a silent behaviour change there
surfaces as data corruption several layers away, in code that looks innocent.

## The work

1. **Measure, don't guess.** Run a coverage pass over `:ultra:vault:jvmTest` (kover or jacoco — check
   what the build already has before adding a plugin) and produce a per-file uncovered-lines list.
   Coverage is the map, not the goal.
2. **Separately, list the DEAD surface.** For every public declaration in `ultra/vault/src/*Main`,
   grep the whole repo for callers. Split the results:
   - **used but untested** → write the test;
   - **unused AND untested** → this is a delete-or-keep decision, NOT an automatic
     "write a test for it" (see the standing rule in `CLAUDE.md` / memory: flag dead framework
     surface for the user rather than hardening or deleting it unilaterally). `filterValueIsInstanceOf`
     and `resolveAll` are the two known members of this set — start there and ask.
3. **Prioritize by blast radius, not by line count.** In rough order: `Storable`/`Stored`/`New`/`Ref`
   semantics and `resolve()`, the repository hook pipeline, cursors and paging, `EntityCache`,
   soft-delete, timestamped, and the slumber-facing serialization edges. A helper that formats a
   string matters far less than one that decides which document gets written.
4. **Write tests that would actually fail.** Mutation-check the important ones: break the
   implementation on purpose and confirm the new test goes red. A test that passes against a broken
   implementation is worse than no test, because it reads as coverage. (This session caught two real
   gaps exactly this way.)
5. **Multiplatform note:** `ultra/vault/src/commonMain` compiles to JS, but the module has only
   `jvmTest`. Anything in `commonMain` whose behaviour is platform-sensitive (string ops, number
   formatting, reflection fallbacks) is currently pinned on the JVM only. Decide whether a
   `commonTest` source set is worth adding, or record why not.

## Not in scope

Karango and Monko have their own gaps (see `20260725-monko-findbyid-collection-prefix.md`, which is a
genuine behavioural divergence rather than a coverage hole). Keep this sweep to `ultra:vault`; if the
same treatment is wanted for the drivers, that is a follow-up doc.
