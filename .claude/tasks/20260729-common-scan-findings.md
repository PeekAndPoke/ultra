# ultra/common scan — findings backlog

**Status:** IN PROGRESS — the backlog is worked through; six items remain, listed at the bottom
**Plan:** none — output of a 7-agent scan of `ultra/common`, 2026-07-29
**Security-critical:** no

All 49 main-source files were documented (comment-only) and the defects below were found along the
way. Everything marked DONE was mutation-checked and is green on JVM, JS and native.

## DONE — fixed 2026-07-29

| Finding | Fix |
|---|---|
| `GetAndSet.of(...)` declared `Observable` but never called `emit` — subscribing worked, nothing ever fired | emit on set |
| `Observer.observe` passed the *observable* as its own observer (bare `this` in a member extension is the extension receiver) | interface deleted, see below |
| `emit` iterated the live set, so a callback that subscribed or unsubscribed threw `ConcurrentModificationException` — only with ≥2 subscribers, since `hasNext()` does not check modCount | iterate a snapshot |
| JVM `toFixed` used the machine's default locale, so a German-locale server rendered `1,50` where its own JS client rendered `1.50` | pinned to `Locale.ROOT` |
| Native `RunSync` was a global, non-re-entrant spin lock — any nested call spun forever | re-entrant via a `@ThreadLocal` depth counter |
| JS `WeakSet.size` returned `NaN` (`undefined + Int`) and `toSet()` threw — a JS `WeakSet` is neither sizeable nor iterable | both removed from the API |
| `MutableTypedAttributes` locked writes but not reads | reads take the same lock |
| `ComparableTo` existed twice, in `ultra/common` and `ultra/datetime` | unified on common's |

**The weak-observer machinery was deleted rather than fixed.** `Subscription` held the observer
weakly *and* the callback strongly, and any callback touching the observer keeps it reachable through
the emitter — so the advertised auto-cleanup could never fire for a realistic callback. The one
production caller (`Mutator.kt:121`) observed itself, making the weak reference inert anyway.
`Observer` had zero implementors. Subscriptions now live until `Unsubscribe` is called or the
observable is collected, which is what Kraft's `unSubscribers` pattern already does properly.

**Why `ComparableTo` was duplicated, for the record:** `ultra/datetime` declared `ultra:common` in
its **`commonTest`** block, not `commonMain`, so its main sources could not see common at all. Moved
to `commonMain` as `api` — required, because `ultra/maths` and `kraft/addons/datetime` depend on
datetime *without* depending on common.

## DONE — second pass, 2026-07-29 (walking the backlog item by item)

### 1. `fromBase64` threw where callers assumed it could not

Added `fromBase64OrNull()` alongside the strict `fromBase64()` — Kotlin's `x()` / `xOrNull()` pair.
All three call sites fixed:

- `StatelessCsrfProtection.validateToken` — a token that is not base64 now returns `false`, like
  every other malformed shape it already guarded. It used to throw `IllegalArgumentException` out of
  a function whose entire contract is answering true/false.
- `PBKDF2WithHmacSHA256PasswordHasher.check` — an undecodable **stored** hash or salt now fails
  closed. One corrupt or legacy row used to throw out of the auth check.
- `AwsSesSender` — still fails the send (a broken attachment must not go out silently) but names the
  attachment instead of an opaque `Illegal base64 character`.

New `EncodingSpec`, plus regressions in both security specs. Mutation-checked: reverting the two
security call sites fails exactly those two tests.

### 2. `EmailRegex` could exhaust the stack

`isEmail()` now rejects anything longer than the new `MAX_EMAIL_LENGTH` (254) before matching. That
is the *correct* answer — RFC 5321 caps a forward-path there — and it keeps the regex away from the
depth where it overflows.

Measured, rather than assumed: the threshold is **~8000 characters** on a default JVM stack, not the
~2000 first reported, and it is stack-size dependent so a smaller stack fails sooner. The
`EmailRegex` KDoc was corrected accordingly.

**ReDoS audit of every regex in the module — clean.** Timed, not eyeballed:

| input | time |
|---|---|
| `EmailRegex`, adversarial `(A+)*` shapes | < 0.2 ms at every size |
| `EmailRegex`, 50 000 chars | 2.1 ms |
| `isEmail` on 1 000 000 chars | 4.3 ms (guard short-circuits) |
| `UrlWithProtocolRegex`, 50 000 chars | 87.7 ms — slowest, but scaling is **linear** |
| `SlugRegex`, `Placeholders` scan | linear, sub-millisecond |

No catastrophic backtracking anywhere. The email regex survives because every repetition is anchored
by a required literal `.`; the URL regex is capped by a bounded `{1,256}` host class.

### 3. `Placeholders` — malformed shapes were invisible, and colliding patterns were dropped

Four defects fixed together, since they shared a cause:

- **`findErrorsIn` only saw well-formed shapes.** It scanned with `\{\{[a-zA-Z0-9_-]+\}\}`, so
  `{{Name}` and `{{ Name }}` matched nothing, were reported as no error, and shipped verbatim —
  `validate()` returned `true` for a broken template. Each syntax now has a deliberately *loose*
  scan regex, and anything it matches that is not a known pattern is reported.
- **The scan's name class was narrower than what `renderPattern` accepts.** A registered
  `{{user.name}}` was invisible in both directions. The loose scan (`[^{}]*`) fixes this as a
  side effect.
- **`findErrorsIn` was duplicated verbatim** in `DoubleCurly` and `TripleHash`. Now implemented once
  in `Abstract`, driven by an abstract `scanRegex`.
- **`fill()` silently dropped colliding values.** `values.associateBy(::renderPattern)` means a
  non-injective `toStr` makes one value permanently unsubstitutable. Now a `require` that names the
  colliding pattern.

`Placeholders` has no in-repo callers but IS used outside this repo, so it stays. Note this is a
behaviour change for external users: a template with a stray `{{` now fails `validate()`.

**Still to do — unify the three placeholder implementations.** `ultra.common.Placeholders`,
`ultra/i18n/MessageResolver` (`PLACEHOLDER.replace`, with its own injection guard) and
`tooling/i18n/I18nChecker` each implement `{{name}}` substitution and/or checking independently.
`MessageResolver`'s is the best thought through. Agreed to unify; scoped as its own piece of work
because it crosses three modules — pick it up with the i18n pass.

### 4. `List.remove` / `removeAt` shadowed the `MutableList` members

Renamed to **`without` / `withoutStrict` / `withoutAt`**.

The trap: identical source text either mutated the receiver or built and discarded a copy, decided
only by the declared type, with no warning since Kotlin does not flag unused return values. Measured
before changing anything — a rename probe across every JVM and JS compile target found **zero**
production call sites; only `ListsSpec` itself resolved to the extensions.

`without` rather than `removed`: the extension drops **every** occurrence while `MutableList.remove`
and `kotlin.collections.minus` both drop only the **first**, so it is not merely copy-vs-mutate — the
answers differ. `without` also sits beside the existing `addAt`/`swapAt`/`replaceAt` without forcing a
tense-rename of the whole file.

Checked systematically for others: all 41 extensions in the module declared on a stdlib receiver were
compared against that receiver's members. `remove`/`removeAt` were the only genuine collisions —
`joinToString`, `toList`, `replace`, `encodeToString`, `decode` and `digest` looked like collisions in
a first grep but are call sites inside function bodies, not declarations.

### 6. `ObjectSizeEstimator` kept its visited set between calls

Not strictly `ultra/common` — the fix is in `ultra/cache` — but it came out of the `WeakSet`
question, so it is recorded here.

`seen` was an instance field that was never cleared, so an estimate depended on what the same
estimator had measured before: a repeated `estimate()` of the same object returned **0**, and a
freshly built but structurally equal graph did too. Matching was also `equals`-based, so two
equal-but-distinct objects counted as one.

That hit `FastCache.MaxMemoryUsageBehaviour`, which holds one estimator (`FastCache.kt:342`) and
calls it per entry (`:380-381`): the first entry measured correctly and every structurally similar
later entry measured as 0 bytes, so the memory cap under-counted and stopped evicting — exactly what
it exists to prevent.

Fixed both halves: the visited set is now built per call, and it compares with `===` while bucketing
by `hashCode`. Five regression tests, each half mutation-checked separately. Side effect: `WeakSet`
lost its only consumer.

### 5. Helpers the stdlib has since absorbed

Audited every helper against Kotlin 2.3.10, by running both sides rather than by recall.

**Deprecated at `DeprecationLevel.ERROR` with `ReplaceWith`:**

| Helper | Replacement | Evidence |
|---|---|---|
| `MutableList.pop()` | `removeLastOrNull()` | `[1,2]` → `(2,[1])` both; `[]` → `null` both |
| `MutableList.shift()` | `removeFirstOrNull()` | `[1,2]` → `(1,[2])` both; `[]` → `null` both |

Both stdlib functions are Stable since 1.4. The two real callers (`classes.kt:36-37`) were migrated
first, and the specs for them removed — the stdlib covers that behaviour now.

**Looked equivalent, is not — kept:**

- `fromBase64()` tolerates missing padding (`"QQ"` → `"A"`); `Base64.decode` throws on it.
- `without(element)` removes all occurrences; `minus(element)` removes the first.
- `ucFirst`/`lcFirst` behave identically to `replaceFirstChar { it.uppercase() }` (checked `ß` → `SS`),
  but that is a general higher-order function, not a named duplicate. Judgement call; kept.
- `containsAny`/`containsNone`/`prepend` have no stdlib *function*, only compositions.

**Identical output but the stdlib API is still Experimental — deliberately not deprecated:**
`toHex()` vs `toHexString()`, `toBase64()` vs `Base64.encode()`. Adopting them would force an
`@OptIn` onto every downstream consumer. Tracked in
`.claude/tasks/20260729-kotlin-24-upgrade.md`.

## DONE — third pass, 2026-07-29 (the remaining backlog, decided item by item)

| Item | Outcome |
|---|---|
| `TypedAttributes` | constructor and `copy()` are now `internal` (`@ConsistentCopyVisibility`), so `Builder` is the only public way in and `size` cannot disagree with `entries` |
| `files.kt` `child()` | rejects absolute paths and `..` escapes, checked on the NORMALISED path so `a/../b` still works and `a/../../b` does not |
| `files.kt` `cleanDirectory()` | uses `Files.walkFileTree`, which does not follow symlinks — `deleteRecursively` walked into a linked directory and deleted its contents outside the tree |
| `files.kt` `ensureDirectory()` | throws when the path is a regular file or `mkdirs()` failed, instead of returning as if it had worked |
| `classes.kt` | a null `package` (array types, default package) counts as root instead of NPE-ing |
| `ellipsis` | never splits a surrogate pair; a negative `maxLength` clamps instead of throwing |
| `toUri` | a `#fragment` is split off and re-appended AFTER the query; the `List<Pair>` overload keeps repeated keys; no doubled `?`/`&` |
| `safeEnumsOf` | goes straight to a `List`, keeping repeats in input order |
| `safeEnumOf` / `safeEnumOrNull` | a miss is a plain lookup, no thrown-and-caught exception |
| `containsAny` | short-circuits on the first hit rather than building two sets and an intersection |
| `recurse` | linear — a `seen` set replaces the per-step scan of everything collected so far |
| `GetAndSet.equals` | restricted to `Impl`, so it is symmetric with `Mutator`, which uses identity |
| `WeakSet` | kept as published API, as decided |

19 regression tests added across `CommonFixesSpec` (commonMain, so all three platforms) and
`CommonFixesJvmSpec`. Mutation-checked by reverting three fixes at once — exactly the three matching
tests failed.

### Two that were asked for and deliberately NOT done

**`toggle` cannot be made order-idempotent.** `[a,b,c]` toggled off and on gives `[a,c,b]`. The
position is gone once the value is removed, and `Set<X>.toggle(value)` has no reference point to
reinsert at. The round trip IS `==` to the original — only iteration order differs. The KDoc now says
that instead of claiming otherwise, and points at `List` where order carries meaning.

**`ellipsis` still appends the suffix on top of `maxLength`.** This was reported as "overshoots
maxLength", but the original KDoc said *"Takes maxLength of the string and adds the suffix"* — the
code matched its documentation, and five tests pinned it. `maxLength` is a misleading name for "kept
text", not a defect. Redefining it as a total budget would silently change output for every
downstream caller. Confirmed by the maintainer: `maxLength + dots` is the intended contract.

## Open

Marked **[verified]** where I probed it myself, **[reported]** where it is a scan finding I have not
re-checked. Treat the reported ones as leads, not facts — several agent claims in this sweep did not
survive contact with a probe.

### Worth a decision

- **`ultra/datetime/recurse.kt` is a byte-identical copy of `common/recursion.kt`.** Same root cause
  as `ComparableTo`, and removable now that commonMain can see common. Belongs to the datetime pass.
  **[verified]**

### Correctness, no decision needed

- **`maxLineLength`** counts the `\r` in CRLF text; **`camelCaseSplit`** only breaks on ASCII
  `A`..`Z`, so a non-ASCII capital is never a word boundary. **[reported]**

### Performance / hygiene — left alone, none looked straightforward

- **`flattenTreeToSet`** recurses with no depth bound. **[reported]**

- **`tuple.kt`** — `asList` degrades to `List<Any?>` from arity 2, there is a `by lazy` delegate
  allocated per instance, and `+` nests rather than concatenating when adding a tuple. **[reported]**
- **`NetworkUtils.getNetworkFingerPrint`** is not stable across restarts. **[reported]**
- **`hashing.kt`** offers no HMAC or KDF, which is why the CSRF signer hand-rolls `H(data || secret)`
  — a construction with known weaknesses. Worth a look during a security pass. **[reported]**

## Test evidence

- `ultra/common` after the third pass: 351 jvmTest, all three platforms green
- Full compile sweep across EVERY project, main and test, JVM and JS: 0 errors. Worth doing rather
  than trusting module-scoped runs — the root project has its own `src/jvmMain` that is not a module
  in `settings.gradle`, and a change to `Observable` broke it while every module suite stayed green.
- Consumers verified after the `ComparableTo` change: datetime 5234, slumber 1219, funktor/core 772,
  karango 1649, monko 249, kraft/core-tests 394, cache 290, vault 285, model 170, maths 343 — all 0
- `WeakSetSpec` moved to `commonTest`, so it now runs on all three platforms instead of JVM only —
  which is exactly how the JS defects would have been caught
- New: `UtilsSpec`, `NumbersJvmSpec`, `EncodingSpec`, plus additions to `ObservableSpec`,
  `GetAndSetSpec`, `ListsSpec`, `PlaceholdersSpec`, `StringsExtSpec` and both `ultra/security` specs.
  Several covered functions that had **no** tests at all (`hasSubscriptions` had neither tests nor
  callers; `unsubscribeAll`, `prepend`, `modifyIf` were used but untested). The `pop`/`shift` specs
  were then removed along with the functions themselves — see item 5.
