# Emit real TypeScript generics instead of monomorphizing

**Status:** CODE DONE `63f9f186` — needs `/feature-review` before DONE. Direction and sub-decisions agreed 2026-07-30 — direction and sub-decisions agreed 2026-07-30 (maintainer: "full effort, this is pure foundation")
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

- [x] A declaration for a generic type is keyed by the RAW class, not by an instantiation. `TypeId`
      currently canonicalises the reified type including arguments; generic declarations need an
      identity that does not.
- [x] New `TsTypeRef.TypeParam(name)` for a property that refers to its owner's parameter.
- [x] References to an instantiation carry arguments: `TsTypeRef.Named(id, args)` or a new variant.
- [x] `TsTypeDecl` gains the declared parameter names, in order.

### Walker

- [x] `declareObj` must build props from the UN-substituted constructor types, mapping each type
      parameter to `TypeParam`. `ReifiedKType` exists to do the opposite, so expect this to be the bulk
      of the work.
- [x] The walk must still traverse instantiations to discover reachable types — `PageOf<Talk>` has to
      enqueue `Talk` — while declaring `PageOf` exactly once.
- [x] `declareUnion` substitutes the parent's arguments into each variant instead of
      `createBareType()`. This is what fixes problem 1 above.
- [x] Value classes with parameters (`value class Box<T>(val v: T)`) alias to a parameter.

### Emitter

- [x] Generic declarations emit interface + factory. Non-generic output must stay byte-identical —
      pin that with the existing golden tests before touching anything.
- [x] Recursion still needs `z.lazy`; the spike confirms a recursive generic works.
- [x] A generic variant inside `z.discriminatedUnion` is fine: a factory call returns a concrete object
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

- [x] Non-generic output is byte-identical to today — assert FIRST, so the blast radius is known.
- [x] `PageOf<Talk>` and `PageOf<Speaker>` produce ONE declaration and two distinct use sites.
- [x] The nullability distinction survives: `Box<String>` vs `Box<String?>` (see `2623a08d` — this was
      already got wrong once, and generic emission changes how it is represented).
- [x] Generic sealed hierarchy: `Storable<Organisation>` carries a real payload type, not `unknown`.
- [x] Recursive generic emits `z.lazy` and parses.
- [x] Generic variant as a `z.discriminatedUnion` option.
- [x] ts-verify fixtures for each of the above — `tsc` is what proves the emitted text is real. Note
      `TsFixtureGenerator` derives `requiredFields` from a declaration's props, which for a generic
      declaration reference parameters; the manifest will need the instantiated form.
- [x] Mutation-test every claim. This run found three wrong fixes and four assertions that passed for
      the wrong reason — all by mutation, none by reading.
- [x] Compile sweep, and `:ultra:slumber:jvmTest` still green.

## What `tsc` caught that nothing else would have

Both surfaced only because the emitted text is compiled for real, and neither is visible in a Kotlin
assertion about that text:

1. **Annotating a factory `: z.ZodType<Foo<T>>` erases its object-ness**, so a generic variant is
   rejected as a `z.discriminatedUnion` option — TS2322. Fixed with `satisfies`, which checks the schema
   against the interface while KEEPING the narrower inferred type. Verified erasable under
   `erasableSyntaxOnly`.
2. **A recursive generic cannot use `satisfies`** — `z.lazy` has nothing to infer from — so it stays
   annotated. That is the same trade the non-generic path already makes, and a lazy variant already
   forces its union onto `z.union`.

## Emitted shape, for reference

```ts
export interface FxBox<T> { item: T; label: string }
export const FxBox = <T>(TSchema: z.ZodType<T>) =>
    z.object({ item: TSchema, label: z.string() }) satisfies z.ZodType<FxBox<T>>

export interface FxTreeOf<T> { value: T; children: FxTreeOf<T>[] }
export const FxTreeOf = <T>(TSchema: z.ZodType<T>): z.ZodType<FxTreeOf<T>> =>
    z.lazy(() => z.object({ value: TSchema, children: z.array(FxTreeOf(TSchema)) }))

export type FxStorable<T> = FxStorableNew<T> | FxStorableStored<T>
export const FxStorable = <T>(TSchema: z.ZodType<T>): z.ZodType<FxStorable<T>> =>
    z.discriminatedUnion('_type', [FxStorableNew(TSchema), FxStorableStored(TSchema)])
```

The schema argument is named `<Param>Schema` — suffixed rather than case-mangled so it is injective:
parameter names are unique within a declaration, whereas lower-casing would collapse `T` and `t`.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation | 10 raised | 10 confirmed |
| 2. Domain expert | 5 raised | 5 confirmed |
| 3. Adversarial | 6 raised + 7 independently reproduced | 6 confirmed |

Run 2026-07-30 over `git diff 8eccd823..HEAD -- ultra/codegen`. **All three reviewers COMPILED and RAN
the emitted TypeScript** rather than reasoning about it, and reviewers 1 and 2 independently
constructed the same top three findings. Fixed in `72937b71` and `caa4cd01`.

### Fixed

| Finding | Why it mattered |
|---|---|
| Star projection on a user generic silently dropped | `mapIndexedNotNull` discarded the position, emitting the bare FACTORY where a schema belongs. tsc accepts it (`z.object`'s shape is loose) and every parse then throws. The `List<*>`/`Map<String,*>` branches already handled this |
| Recursive generic UNION and ALIAS never got `z.lazy` | both generic branches returned before the `isRecursive` branch. tsc clean, first parse blows the stack. The uncovered cell was exactly "recursive × generic × (alias\|union)" |
| Variant type parameters bound positionally | `Left<R, L> : Either<L, R>` inverts under a pass-through, and type and schema agree WITH EACH OTHER — so `satisfies` holds, tsc is clean, and the client rejects every real payload. Now resolved via the child's own supertype entry |
| A duplicate binding (`Both<A> : Same<A, A>`) slipped through | coverage check passed it last-wins; the KDoc already promised a bijection |
| Claimed generic dropped its arguments | a payload reachable only through one vanished from the model entirely |
| Declaration and type-parameter names never validated | escaping covers literals and property keys — these are different positions. A class named `Record` captures the global every generic `Map` emits; a parameter named `infer` is a SYNTAX error that makes tsc skip semantic checking for the whole file |

### Confirmed but NOT fixed — tracked

- [x] **DONE — `TypeModel.declFor`** added and `TsFixtureGenerator` rerouted through it. Pinned by four
      cases including the negative one (the raw id lookup must NOT match). Mutation-tested.
- [ ] **Claim imports are always value imports** (`ts/TsModelEmitter.kt:54`). If a claimed `tsName`
      resolves to an `interface`/`type`, `verbatimModuleSyntax` gives TS1484. Latent only because every
      shipped claim points at a `const` — and `runtime/apiResponse.ts` is exactly the shape that breaks
      it (`export interface ApiResponse<T>` beside `export function apiResponse`).
- [ ] **`TsSdkOutput` does not confine paths** (`sdk/TsSdkOutput.kt:57,71,87`). `out.file("../../etc/x")`
      escapes the SDK root. A contributor is already arbitrary JVM code so this is not a privilege
      boundary; the realistic harm is an accidental write outside the target tree, and `--check` reading
      outside it. One `canonicalPath.startsWith` in `add()`.
- [ ] **A phantom type parameter yields an unusable factory signature** — `Keyed<K, V>(val m: Map<K, V>)`
      never puts `K` on the wire (JSON keys are strings) yet the factory demands `KSchema`. Compiles;
      the caller must invent a schema for something never validated.

### Probed and CLEAN — do not re-tread

- `selfType` + `ReifiedKType` with inherited generic supertype properties is SOUND, both when the
  subclass binds concretely and when it forwards, including inherited `@Slumber.Field` properties.
- ONE codec-parity representative is sufficient: `BuiltInModule` dispatches purely on the classifier.
- Polymorphic recursion (`Rec<T>` containing `Rec<Rec<T>>`) terminates and emits correctly —
  monomorphization could not express it at all.
- Generic value classes correct for every underlying tried; optionality unaffected by generic emission.
- Output size is now linear in the source rather than in instantiation count.
- ts-verify toolchain pinning: no finding. `ultra:codegen` reaches no production classpath.
- A type parameter named `z` is safe (qualified-name resolution skips the type-parameter namespace);
  a local `Array` is safe (`T[]` does not resolve `Array` lexically). Only `Record` bites.

## Follow-ups

- [ ] **`expects<T>(tsName)` now looks unnecessary.** A generated name is the class's simple name and
      nothing else, so a component author no longer has to guess at `PageOfLock`. Confirm and delete the
      proposal from `20260730-frontend-sdk-vue-contributors.md`.
- [x] Update the plan doc's locked-decisions table and the DOCS follow-up: generic emission changes the
      public shape of every generated SDK.
