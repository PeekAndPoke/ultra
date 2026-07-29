# ultra/common scan — findings backlog

**Status:** IN PROGRESS — items 1-5 of the backlog are closed; what remains is listed at the bottom
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

## Open — worth a decision

- **`WeakSet` element matching is not uniform.** JVM and native match with `equals`, JS by reference
  identity. For the one real caller — `ObjectSizeEstimator.seen`, doing cycle detection — *identity*
  is what is actually wanted, so the JVM/native behaviour is arguably the wrong one: a graph holding
  two equal-but-distinct values is counted once there and twice on JS. Making JVM identity-based
  needs a wrapper (the JDK has no `WeakIdentityHashMap`). Documented, not fixed.
- **`ultra/datetime/recurse.kt` is a byte-identical copy of `common/recursion.kt`.** Same root cause
  as `ComparableTo`, and now removable since commonMain can see common. Left for the datetime pass.
- **`List.remove(element)` / `List.removeAt(idx)` are shadowed by the `MutableList` members.**
  Identical source text either mutates the receiver or builds and discards a copy, depending only on
  the declared type. Widening a field from `MutableList` to `List` silently changes behaviour with no
  warning. Renaming them would be the fix; both have many callers.

## Open — smaller, no decision needed, just work

- `ellipsis` appends the suffix *on top of* `maxLength` (`"ab".ellipsis(1)` is 4 chars), truncates by
  UTF-16 code unit so it can split a surrogate pair, and throws on a negative length.
- `safeEnumsOf` returns a `List` but routes through a `Set`, so duplicates are silently collapsed.
- `toggle` is not order-idempotent — off-then-on moves the element to the end of iteration order.
- `toUri` is string concatenation: it appends query params *after* a `#fragment`, and collapses
  duplicate keys through `toMap()` so `listOf("tag" to "a", "tag" to "b")` loses the first.
- `recurse` is O(n²) (linear `!in` scan per step) and `flattenTreeToSet` is recursive with no depth
  bound.
- `safeEnumOf` uses a thrown-and-caught exception as its miss path, on request-supplied input.
- `TypedAttributes`' primary constructor is public, takes an unvalidated map and does not copy it, so
  `size` and `entries` can disagree and a type mismatch surfaces at the reader.

## Test evidence

- `ultra/common`: jvmTest 325, jsBrowserTest 159, linuxX64Test 153 — 0 failures
- Consumers verified after the `ComparableTo` change: datetime 5234, slumber 1219, funktor/core 772,
  karango 1649, monko 249, kraft/core-tests 394, cache 290, vault 285, model 170, maths 343 — all 0
- `WeakSetSpec` moved to `commonTest`, so it now runs on all three platforms instead of JVM only —
  which is exactly how the JS defects would have been caught
- New: `UtilsSpec`, `NumbersJvmSpec`, `EncodingSpec`, plus additions to `ObservableSpec`,
  `GetAndSetSpec`, `ListsSpec`, `PlaceholdersSpec`, `StringsExtSpec` and both `ultra/security` specs.
  Several covered functions that had **no** tests at all (`hasSubscriptions` had neither tests nor
  callers; `unsubscribeAll`, `prepend`, `modifyIf` were used but untested). The `pop`/`shift` specs
  were then removed along with the functions themselves — see item 5.
