# DOCS — `@Slumber.As` and the generated datetime query paths

**Status:** TODO — created 2026-08-01 on passing the `/feature-review` gate for
`.claude/tasks/20260731-slumber-as-declared-wire-shape.md`.
**Plan:** none.
**Security-critical:** no.

## Why this exists

The change is **breaking** and moves public API in three places, so applications must edit code:

- `io.peekandpoke.ultra.slumber.Slumber` → `io.peekandpoke.ultra.common.slumber.Slumber`
- `io.peekandpoke.karango.aql.ts` and `io.peekandpoke.monko.lang.ts` are **deleted**; the replacement is
  the generated `io.peekandpoke.ultra.datetime.ts`
- new public types: `Slumber.As`, `MpDateTimeRawData` (was `internal SerializationTuple`)

## Wait for settling before writing

Per CLAUDE.md, docs go against SETTLED code, and the maintainer's own review comes after the gate. The
review already changed the annotation's KDoc twice. **Do not start until the maintainer has looked at it**
— and check `.claude/tasks/20260801-slumber-as-followups.md` §3 first, because that decision (whether the
round-trip check becomes public API) changes what there is to document.

## What to write, when it is time

- [ ] **The migration**, since every consumer hits it: the import change for `Slumber`, and `.ts` now
      coming from `io.peekandpoke.ultra.datetime`.
- [ ] **What `@Slumber.As` is for** — a custom-coded type declares its wire shape once, instead of each
      consumer mirroring it. One form, `As(KClass)`, scalars included.
- [ ] **That it is descriptive, not generative.** It does not create the codec and nothing derives the
      codec from it. Keeping it true is the type author's job, like the codec itself.
- [ ] **The two limits**, both already on the KDoc — it describes the SLUMBER direction only
      (`Redacted<T>` proves the directions can differ), and replacing a codec via `prependModules`
      silently invalidates every generated path for that type.
- [ ] **The datetime query paths**, which is what most readers actually came for: `MpInstant` and friends
      store as `{ts, timezone, human}`, so `entity.createdAt.ts` is the sortable/indexable field. Say
      plainly that `timezone` is a literal `"UTC"` for all but `MpZonedDateTime`, and that `human` is a
      debug rendering with no stable format — filtering on either is a footgun that compiles.

## What NOT to write

Per the docs-vs-skills split: the KSP internals stay out — `getSlumberAsShape`, the blacklist, the
per-module generation, the `AqlPathExpr` typealias equivalence. Those are "what will bite me if I change
this code" and belong in a skill or the task record, not on a docs page.
