# Emit real TypeScript generics instead of monomorphizing

**Status:** IN PROGRESS — direction and sub-decisions agreed 2026-07-30 (maintainer: "full effort, this is pure foundation")
**Plan:** `.claude/tasks/20260729-ts-sdk-codegen.md` — reverses its "Generics are monomorphized" decision
**Security-critical:** no (dev-time generator)

## Why

The monomorphization decision rested on two claims. One is shallow, the other is **false** — measured,
not argued, in the generics spike recorded in the plan doc (tsc 7.0.2 + zod 4.4.3, full harness config,
8/8 runtime checks):

- *"`z.infer` cannot see through a function-valued schema"* — **untrue.**
  `type PageOf<T> = z.infer<ReturnType<typeof pageOf<T>>>` compiles via instantiation expressions.
- *"The walker reifies type arguments anyway, so staying generic means un-reifying"* — true, and this
  is the actual cost. It is walker work, not emitter work.

Three open problems trace back to monomorphization, so this is not tidiness:

1. **Generic sealed hierarchies silently lose their payload.** `declareUnion` builds variants via
   `createBareType()`, filling parameters with `Any`, so `Storable<Organisation>` and `Storable<Talk>`
   become the SAME TypeScript type, both carrying `unknown`. The schema still parses — `unknown`
   accepts anything — so it is silent.
2. **`expects<T>(tsName)`** in `20260730-frontend-sdk-vue-contributors.md` exists ONLY because
   `PageOf<Lock>` → `PageOfLock` is a computed name a component author has to guess.
3. Declaration count grows with instantiation count.

**Do this BEFORE Phase 2.** It changes what every generated model looks like, and Phase 2 is where real
funktor APIs start being walked.

## Target output

TypeScript allows an interface and a const to share a name, so type and schema keep one identifier —
the same convention non-generic output already uses.

```ts
export interface PageOf<T> {
    items: T[]
    total: number
}

export const PageOf = <T>(item: z.ZodType<T>): z.ZodType<PageOf<T>> =>
    z.object({ items: z.array(item), total: z.number() })
```

Use sites split by position, and BOTH improve on `PageOfTalk`:

| Position | Today | With generics |
|---|---|---|
| type | `PageOfTalk` | `PageOf<Talk>` |
| schema | `PageOfTalk` | `PageOf(Talk)` |

`TsRenderer` already splits `type()` from `schema()`, so this needs no new machinery there. (Correcting
an earlier claim of mine: generic emission does NOT make every reference a call — only schema
positions, with the same ordering constraints as today.)

## The work

### Model

- [ ] A declaration for a generic type is keyed by the RAW class, not by an instantiation. `TypeId`
      currently canonicalises the reified type including arguments; generic declarations need an
      identity that does not.
- [ ] New `TsTypeRef.TypeParam(name)` for a property that refers to its owner's parameter.
- [ ] References to an instantiation carry arguments: `TsTypeRef.Named(id, args)` or a new variant.
- [ ] `TsTypeDecl` gains the declared parameter names, in order.

### Walker

- [ ] `declareObj` must build props from the UN-substituted constructor types, mapping each type
      parameter to `TypeParam`. `ReifiedKType` exists to do the opposite, so expect this to be the bulk
      of the work.
- [ ] The walk must still traverse instantiations to discover reachable types — `PageOf<Talk>` has to
      enqueue `Talk` — while declaring `PageOf` exactly once.
- [ ] `declareUnion` substitutes the parent's arguments into each variant instead of
      `createBareType()`. This is what fixes problem 1 above.
- [ ] Value classes with parameters (`value class Box<T>(val v: T)`) alias to a parameter.

### Emitter

- [ ] Generic declarations emit interface + factory. Non-generic output must stay byte-identical —
      pin that with the existing golden tests before touching anything.
- [ ] Recursion still needs `z.lazy`; the spike confirms a recursive generic works.
- [ ] A generic variant inside `z.discriminatedUnion` is fine: a factory call returns a concrete object
      schema. Confirmed by the spike.

## Sub-decisions — SETTLED 2026-07-30

1. **Codec-parity probe: keep a representative reified instantiation per declaration.** The check asks
   whether a type resolves to a structural slumberer, and Slumber dispatches custom codecs on
   `type.classifier` — so any reached instantiation answers the class-level question. The walker
   already holds a reified `KType` when it first meets an instantiation, so this is bookkeeping, not
   new machinery. Rejected: probing with `Any` arguments (can misreport), and skipping generic
   declarations (would gut the check exactly where types are most complex, days after `53b68126` made
   it mandatory).
2. **Nothing stays monomorphized.** Two emission modes in one file is a rule authors must reason about,
   and the `expects<T>` problem returns for whichever half keeps computed names.
3. **Bounds and variance are ignored, deliberately.** Kotlin `out T` / `in T` and upper bounds have no
   analogue worth emitting here. Recorded so it is not re-derived as an oversight.

## Test evidence

- [ ] Non-generic output is byte-identical to today — assert FIRST, so the blast radius is known.
- [ ] `PageOf<Talk>` and `PageOf<Speaker>` produce ONE declaration and two distinct use sites.
- [ ] The nullability distinction survives: `Box<String>` vs `Box<String?>` (see `2623a08d` — this was
      already got wrong once, and generic emission changes how it is represented).
- [ ] Generic sealed hierarchy: `Storable<Organisation>` carries a real payload type, not `unknown`.
- [ ] Recursive generic emits `z.lazy` and parses.
- [ ] Generic variant as a `z.discriminatedUnion` option.
- [ ] ts-verify fixtures for each of the above — `tsc` is what proves the emitted text is real. Note
      `TsFixtureGenerator` derives `requiredFields` from a declaration's props, which for a generic
      declaration reference parameters; the manifest will need the instantiated form.
- [ ] Mutation-test every claim. This run found three wrong fixes and four assertions that passed for
      the wrong reason — all by mutation, none by reading.
- [ ] Compile sweep, and `:ultra:slumber:jvmTest` still green.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

## Follow-ups

- [ ] Re-check whether `expects<T>(tsName)` is still wanted once names are no longer computed — it may
      become unnecessary, which would be a second problem removed.
- [ ] Update the plan doc's locked-decisions table and the DOCS follow-up: generic emission changes the
      public shape of every generated SDK.
