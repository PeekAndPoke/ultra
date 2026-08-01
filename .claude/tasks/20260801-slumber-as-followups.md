# `@Slumber.As` — review residue

**Status:** TODO — created 2026-08-01 from the `/feature-review` gate on
`.claude/tasks-archive/2026-07/20260731-slumber-as-declared-wire-shape.md` (§15 is the full record).
**Plan:** none. These are the findings that were confirmed but deliberately not fixed in the gate,
because each is either a separate change or needs a decision.
**Security-critical:** no. Reviewer 3 passed the feature and probed the sharp edges; see §15.

## 1. No behavioural test through the generated accessor, on either driver

**All three reviewers raised this independently**, which is the strongest signal in the record.

Today the feature's only coverage is text-matching over generated source
(`karango/ksp/.../SlumberAsCodeGenSpec.kt`, `monko/ksp/.../SlumberAsCodeGenSpec.kt`). Nothing compiles a
real query through `.ts` and asserts the resulting path. So a source-incompatible change to a public API
could ship green: update the two text specs to match the new output and everything passes.

It got worse in the same commit — `monko/core`'s four `SlumberPropertiesSpec` cases used to call `.ts`
and now call `property<Long>("ts")` directly, so they no longer exercise the accessor at all while their
names still advertise datetime coverage.

- [ ] Add `kspTest(project(":monko:ksp"))` to `monko/core/build.gradle.kts`. `karango/core` already has
      the equivalent (`:46`) — this is a parity gap, not a new idea. Check the blast radius first: it
      runs the processor over every `monko/core` test source.
- [ ] Restore `SlumberPropertiesSpec` to assert through the generated `.ts`, i.e.
      `entity.createdAt.ts.toFieldPath() shouldBe "createdAt.ts"`.
- [ ] Add the karango equivalent — `karango/core` has the wiring and, checked at `4c4daf69~1`, has never
      had a spec referencing either the old helper or the new accessor.

**Why it matters concretely:** if a future generator change made the `AqlExpression<T>`/`MongoExpression<T>`
supertype overload win over the exact `AqlPropertyPath<T, T>` one, the path would start fresh and
`dueAt.ts` would render as `"ts"` instead of `"dueAt.ts"` — silently wrong queries and a wrongly-keyed
index (`MonkoBackgroundJobsQueueRepo.kt:49,100`). Reviewer 3 confirmed the exact overload wins **today**;
nothing pins it.

## 2. The referenced-type walker does not follow the declared shape

`generateCode` takes fields from the declared shape, but `combineWithReferencedTypes()` /
`getReferencedTypes()` still walk `cls.getAllProperties()` — the Kotlin properties the annotation just
declared irrelevant (`KarangoKspProcessor.kt:409`, monko twin). The two halves of the processor disagree
about what a `@Slumber.As` type contains.

Zero impact today: all three fields of `MpDateTimeRawData` are scalars, and `MpDateTimeRawData$$karango.kt`
is correctly never generated. The first NESTED shape hits it immediately —
`@Slumber.As(OrderRaw::class)` where `OrderRaw` has `val address: AddressRaw` emits an `.address`
accessor typed `AddressRaw`, but `AddressRaw$$karango.kt` never exists, so `it.order.address.city` does
not compile and reads as a missing import rather than an unsupported case.

- [ ] When a class carries `@Slumber.As`, walk the declared shape's property graph and add the shape
      itself to the pool. Both processors.

## 3. DECISION — should the round-trip check be callable by downstream users?

`@Slumber.As` is public API in `ultra:common`, and both processors honour it for **any** type, not just
the six here. But the check that makes a declaration trustworthy is a `jvmTest` spec inside
`ultra:slumber`. A user who annotates their own type gets the code generation and none of the
verification, and has nothing they could run.

Concretely: `@Slumber.As(MoneyRaw::class) data class Money(...)` with a codec that actually emits
`{amount, currency}` while `MoneyRaw` says `{cents, cur}` — karango generates `.cents`/`.cur`, the index
is built on keys that do not exist, every filter returns empty, and nothing says a word.

- [ ] Decide whether to expose it — e.g. `Codec.checkSlumberAs(value: Any)` in `ultra:slumber`, or via
      the already-published `ultra:slumber-test-classes` sibling module. Cheap either way; the round trip
      is ~6 lines. The question is whether the annotation is meant for downstream use at all, or is an
      internal contract that happens to be public.

## 4. Lower priority

- [ ] **Generated facades are duplicated per consuming module.** Six copies of
      `io.peekandpoke.ultra.datetime.MpInstant$$karango.kt` exist (`funktor/{all,auth,cluster,messaging,saas}`,
      `karango/core`), because `packageName = cls.packageName`. Pre-existing, and reviewer 1 verified the
      one-source-plus-one-binary case resolves fine and compiles. It only became load-bearing now that
      `.ts` is the sole way to write these queries. Generating the fixed classpath-type accessors once in
      `karango:core`/`monko:core` would remove it.
- [ ] **Release note: the annotation package move needs version lockstep.** `@Slumber.Field` matching is
      type-based, so a pre-`4902d6bf` `ultra:vault` resolved against a post-`4902d6bf` `ultra:slumber`
      silently stops serializing `@Vault.Field` properties — JVM reflection skips annotations whose type
      is unresolvable. The symptom is missing data, not an error. Worth one line in the release notes.
- [ ] **`MonkoPrinter.escapeName()` is `= this`**, a complete no-op (`monko/core/src/main/kotlin/lang/printer.kt:176`),
      while `toFieldPath()` strips backticks that are therefore never added. Pre-existing and unrelated to
      this feature; not exploitable, since names are compile-time constants. But the KDoc says it "ensures
      that all names are surrounded by ticks", which is false. Fix the code or the KDoc.

## 5. Out of scope, recorded so it is not re-derived

E2E coverage is asymmetric: karango is exercised end-to-end via `ClusterApiSpec` → `BackgroundJobsApi`,
monko is not, because `funktor/all/src/jvmTest/kotlin/index_testJvm.kt` wires `useKarango()` exclusively
and no funktor spec uses `MatrixTest2d` (which exists, in `funktor/testing`). That predates this feature
and is a repo-wide gap, not one to solve under this task.
