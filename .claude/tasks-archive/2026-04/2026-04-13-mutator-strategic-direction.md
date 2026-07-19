# Mutator Strategic Direction — Six Hats Analysis (Revised)

**Date:** 2026-03-25
**Rounds:** 3 (re-run with corrected facts)
**Related:** [mutator-quality-improvements.md](mutator-quality-improvements.md) (execute quality plan first)

---

## White Hat — Facts & Context

- Mutator uses KSP to generate stateful mutable wrappers on immutable data classes with `isModified()`, `commit()`, and
  `onChange` observable back-propagation
- The KSP plugin architecture (`MutatorKspPlugin`) is **already pluggable** — all code generation goes through plugin
  implementations. Adding new output types = new plugin, not architecture change.
- **Sub-mutators already exist** — every object property accessor returns a `Mutator<T>` with `onChange` propagation.
  This IS the "slice" concept, already working.
- Supports: data classes, sealed classes, List/Set/Map, generics with bounds
- Primary use case: Kraft SPA framework — nested form components where mutators eliminate callback wiring
- **Not competing with Arrow Optics.** Mutator stands on its own merits or it doesn't.
- Current state: Tier 2 (Yellow) — 43 src files, 18 tests, quality improvement plan exists but unexecuted

---

## Core Identity (consensus)

> **Mutator is "Reactive immutable state management for Kotlin."**
>
> Its value is proportional to object depth. The deeper the nesting, the more pain it eliminates.

---

## Discussion Summary

### Round 1 — Opening Positions

#### Red Hat — Feelings & Intuition

- "Mutate a draft, get a new copy" clicks instantly — the immer.js moment of "wait, that's it?"
- Nested form editing where you never think about callback plumbing is addictive
- If the magic breaks (debugging, cryptic KSP errors), frustration is proportional to how magical it felt before
- Lean HARD into the Kraft form story — that's where the emotional payoff is strongest

#### Black Hat — Risks & Problems

- Mutator has exactly one customer (Kraft) — single point of failure
- KSP is a maintenance trap: every Kotlin version bump risks breakage, KSP2 migration looms
- "Why not just copy()?" is a real objection for shallow data classes — Mutator only wins on deep nesting
- Users can't incrementally adopt — you're all-in or not using it
- 43 source files with 18 tests is a correctness time bomb

#### Yellow Hat — Benefits & Value

- Double down on Kraft integration — form state with dirty tracking is a real niche nobody else serves
- Server-side editing (dirty-check before DB writes) is a natural expansion of the same pattern
- Pluggable KSP is a hidden gem — future plugins (diffs, validation, undo) need no core changes
- Needs depth, not pivots

#### Green Hat — Creative Ideas

- Mutator is really a "change-tracking substrate" — mutation is the interface, observation is the value
- JSON Patch/diff generation from mutation history
- Server-side mutation auditing (wrap entity, extract typed changelog before persisting)
- Time-travel debugging (capture every `commit()` as snapshot, replay forward/backward)
- copy() is a point-in-time operation; Mutator is a stream of changes

#### Blue Hat — Process & Next Steps

- Quality plan first, no exceptions
- Then decide: Kraft-only or general-purpose?
- If general, need standalone demo proving it works outside Kraft
- Plugin arch means features are additive, not architectural risk
- Sequence matters more than vision

### Round 2 — Key Debates

The sharpest exchange was between Black and everyone else on **scope**. Black insisted server-side expansion is "
divergence disguised as depth" — different error handling, different performance profiles, different users. Yellow and
Red pushed back: the emotional core ("I changed something nested, tell me what") is the same in both contexts. Green
reframed: copy() is a point-in-time operation; Mutator is a stream of changes — that distinction makes plugins like
diffing and dirty-checking depth, not pivot.

Red Hat raised an underappreciated concern: **quality work needs a visible reward at the end or motivation dies**. The
quality plan is unglamorous cleanup. Without a compelling "next step" waiting on the other side, energy dissipates. Blue
Hat accepted this and sequenced a tangible milestone (Kraft integration deepening) immediately after quality completion.

Black Hat's most useful contribution was the **"why not copy()?" challenge**. Every hat agreed this must be addressed
head-on — not defensively, but with honest scoping. Mutator adds ceremony for flat data classes. Its value only appears
with deep nesting. The docs must say this explicitly. Yellow added: build the exact copy() vs Mutator comparison into
documentation and make it undeniable for the 3+ levels of nesting case.

Green Hat dropped collaborative editing from the vision (acknowledged as a pivot) but held firm on change-tracking
plugins as depth. Blue Hat brokered the final sequence: quality first, deepen Kraft, then evaluate substrate vision with
real usage data.

### Round 3 — Final Verdicts

| Hat    | Final Verdict                                                                   |
|--------|---------------------------------------------------------------------------------|
| Red    | Ship the "why" before shipping the "what." Conviction fuels everything.         |
| Black  | Honesty about limitations is the only path to credibility.                      |
| Yellow | Depth-perfection is both the floor and the ceiling for now. Nail it completely. |
| Green  | Teach before you extend. Patterns guide first, features second.                 |
| Blue   | Make deep nesting bulletproof. That is Mutator's moat.                          |

---

## Synthesis

All five hats converged on a single thesis: **Mutator's value is proportional to nesting depth, and the roadmap should
deepen that strength before broadening scope.**

The most important insight was Green Hat's reframe, validated by all others: copy() is a point-in-time operation;
Mutator is a stream of changes. That distinction — stateful observation of typed mutation trees — is what makes
onChange, `isModified()`, and `commit()` possible. No other Kotlin library provides this. But it only matters when
nesting is deep enough that manual copy() chains become painful. For flat data classes, Mutator is ceremony.

Black Hat forced a critical honesty moment: the documentation must say "you don't need Mutator when..." alongside "you
need Mutator when..." This earns credibility with experienced Kotlin developers who will immediately test the library
against trivial cases and dismiss it if it oversells.

The unresolved tension is timing: Green Hat wants a patterns guide and change-tracking plugins; Black Hat wants no
features until quality is bulletproof; Red Hat warns that motivation dies during unglamorous cleanup. Blue Hat's phased
approach resolves this — quality first, but with the Kraft deepening milestone visible as the immediate reward.

---

## Use Cases

### Tier 1 — Core (what Mutator does today)

1. **Nested form state management** — Kraft components with deep data models; onChange eliminates callback wiring
2. **Dirty tracking / undo-reset** — `isModified()` + `commit()` give undo/reset for free
3. **Immutable domain model mutation** — Replace nested `copy(a = a.copy(b = ...))` chains
4. **Sealed class polymorphic editing** — Already supported via `filterMutatorsOf()` + `cast()`

### Tier 2 — Natural extensions (depth, not pivots)

5. **Reactive UI state** — Observable pattern integrates naturally with any reactive framework
6. **Server-side dirty-checking** — Wrap entity, mutate, check `isModified()` before DB write (same pattern, different
   context)
7. **Configuration builders** — Admin panels, CMS builders — all "complex nested editing" problems

### Tier 3 — Future plugins (if demand arises)

8. **JSON Patch / structural diff** — Generate diffs between initial and mutated state
9. **Mutation auditing** — Typed changelog extraction before persistence
10. **Time-travel / undo-redo** — Capture `commit()` snapshots, replay forward/backward
11. **Declarative validation** — Combine mutator paths with onChange for reactive field validation

---

## Arrow Optics — Reference Comparison

Not competing. Included for positioning clarity.

### What Arrow can do that Mutator cannot

| Arrow Capability                                    | Mutator Equivalent                           | Gap Type      |
|-----------------------------------------------------|----------------------------------------------|---------------|
| Composable first-class lenses (store, pass, reuse)  | None — mutation is scoped, not composable    | Architectural |
| `Every.list()` traversal (modify all declaratively) | Imperative loop inside mutation block        | Ergonomic     |
| `FilterIndex` (modify elements by predicate)        | Imperative filter + loop                     | Ergonomic     |
| `Prism` for sealed class subtypes                   | `filterMutatorsOf()` + `cast()` — equivalent | Parity        |
| `Iso` for value/inline classes                      | Not supported                                | Feature gap   |
| `copy {}` DSL for multi-field update                | `mutate {}` block — equivalent               | Parity        |
| Stateless pure functions (no GC overhead)           | Stateful wrappers (shadow tree)              | Architectural |

### What Mutator can do that Arrow cannot

| Mutator Capability                        | Arrow Equivalent              | Gap Type      |
|-------------------------------------------|-------------------------------|---------------|
| `onChange` automatic back-propagation     | None — must wire manually     | Architectural |
| `isModified()` dirty tracking             | None — must diff manually     | Feature       |
| `commit()` checkpoint/reset               | None — must snapshot manually | Feature       |
| Observable pattern (subscribe to changes) | None — stateless by design    | Architectural |
| Sub-mutators with automatic propagation   | Must compose optics manually  | Ergonomic     |

---

## Action Plan

### Phase 0 — Quality first (prerequisite)

> Execute [mutator-quality-improvements.md](mutator-quality-improvements.md) before any strategic work.
> Trust fixes, generated code polish, test coverage, correctness fixes.

### Phase 1 — Narrative & Documentation

1. **Write the "why not copy()?" narrative** — honest, with a "you DON'T need Mutator when..." section
2. **Mutator Patterns guide** — 3-5 real Kraft form scenarios:
    - Deep nesting (address within person within order)
    - Lists within lists (address book, line items)
    - Conditional fields (sealed class polymorphic editing)
    - Cross-field dirty tracking
    - Undo/reset via `commit()`
3. **Side-by-side comparison** — same form with copy() chains vs Mutator, showing where it pays off

### Phase 2 — Deepen Kraft Integration

- Polish the Mutator + Kraft form story until it's frictionless
- Better error messages and diagnostic mode for debugging generated code
- Examples in docs site

### Phase 3 — Evaluate Expansion (data-driven)

Only if real usage demands it:

- Server-side dirty-checking plugin
- Change-tracking / diff generation plugin
- Each as a new `MutatorKspPlugin`, no core changes

### Guard Rails

- **Value is proportional to depth** — don't oversell for flat data classes
- **Every addition must pass the "that's it?" test** — if it feels more complex, reject it
- **Teach before you extend** — patterns guide and docs before any new features
- **Honesty earns credibility** — acknowledge where copy() is sufficient
- **No phase skipping** — quality, then narrative, then Kraft depth, then maybe expansion
