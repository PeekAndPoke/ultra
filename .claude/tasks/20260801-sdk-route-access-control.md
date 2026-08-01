# Route access control in the generated TypeScript SDK

**Status:** IN REVIEW — implemented 2026-08-01; gate run, findings fixed, awaiting maintainer sign-off
**Plan:** `.claude/tasks/20260729-ts-sdk-codegen.md` → Phase 4 / Vue-contributors family
**Security-critical:** yes — see the scope note below, it is narrower than it looks

Companion: `.claude/tasks/20260801-kotlin-apiacl-naming-alignment.md` (the Kotlin side of the same
vocabulary). The two can land in either order and neither blocks the other.

## Why

A frontend needs to ask *"may this user call this route?"* to decide whether to show a button, hide
it, or render read-only. The server already answers it: `AuthUserApi.getMyApiAccess` returns a
`UserApiAccessMatrix`, and `ApiAcl` (`funktor/rest/src/commonMain/kotlin/acl/ApiAcl.kt`) is the
Kotlin-side lookup. **The TypeScript SDK generates the models but exposes no way to ask the
question**, because a generated client member is a bare function that does not know its own
method and uri.

## The shape

A client member becomes **callable AND self-describing** — a function with properties attached.

```ts
readonly getEvent = route('GET', '/funktor-conf/events/{id}', (params: { id: string }) =>
    request(this.config, 'GET', '/funktor-conf/events/{id}', EventModel, {
        path: { id: params.id },
    }))
```

```ts
await api.getEvent({ id: 'abc' })   // still callable, return type intact
api.getEvent.method                  // 'GET'
acl.canAccess(api.getEvent)          // queryable
```

**This is purely additive at the type level** — `route()` returns `F & RouteRef`, so every existing
call site keeps compiling untouched.

### Verified, not assumed

A prototype compiled against the real generated SDK under `--strict --erasableSyntaxOnly`, exit 0,
with five `@ts-expect-error` negative tests that all genuinely error (no TS2578). It confirmed:
callability, parameter types preserved, awaited payload type preserved, metadata readable,
`canAccess` accepts a member structurally, and **destructuring still works** — `const { getEvent } =
api`. That last one matters: these are arrow fields, so `Object.assign` mutates a per-instance
function object with no prototype sharing.

```ts
export interface RouteRef {
    readonly method: HttpMethod
    readonly uri: string
}

export type Route<F extends (...args: never[]) => unknown> = F & RouteRef

export function route<F extends (...args: never[]) => unknown>(
    method: HttpMethod, uri: string, call: F,
): Route<F> {
    return Object.assign(call, { method, uri })
}
```

`F` is inferred from the call, so the wrap is transparent. **The bound is a strictness choice, not a
correctness one** — `(...args: any[]) => any` and `Function` preserve inference just as well, proven
by compiling all three (2026-08-01). An earlier draft of this file and of `route.ts` claimed the
opposite; a surviving mutant caught it.

## The vocabulary — decided 2026-08-01

`ApiAccessLevel` is `Granted | Partial | Denied` (`ultra/remote/src/commonMain/kotlin/ApiAccessLevel.kt`).
**`Partial` means the route is callable but the server applies a further check on the ARGUMENTS** —
typically that an id in the path is the caller's own (`/users/{id}/update`). The matrix cannot
evaluate that; the client must supply the equivalent rule itself.

| predicate | level |
|---|---|
| `canAccess` | **not** `Denied` — the everyday "should I show this at all" |
| `canFullyAccess` | `Granted` |
| `canPartiallyAccess` | `Partial` |
| `isDenied` | `Denied` |

Invariants to state in the KDoc, because they are the whole mental model:

- `canAccess` ≡ `!isDenied` — the one true complement
- `canFullyAccess`, `canPartiallyAccess`, `isDenied` — mutually exclusive and exhaustive
- `canAccess` ≡ `canFullyAccess || canPartiallyAccess`

**Why `canAccess` is the permissive one.** Forcing a strict-or-permissive decision at every call site
is a tax that gets paid badly — people copy whichever they saw last. Making the common question the
short name is right, and over-hiding is the worse UX failure: a button that appears and then errors
explains itself, one that silently never appears produces "why can't I edit my own profile?".

**The cost, which must be documented rather than designed away:** a careless `canAccess` now fails
permissive. `if (acl.canAccess(api.deleteUser)) showDeleteButton()` shows the button on every row
when the user may only delete their own. The server still enforces — this is UX, not a breach — but
`canAccess`'s doc comment must say outright that it includes `Partial`, and destructive actions
should reach for `canFullyAccess` deliberately.

The branching case, which is why `canPartiallyAccess` survives despite being derivable:

```ts
const mayEdit = acl.canFullyAccess(api.updateUser)
    || (acl.canPartiallyAccess(api.updateUser) && user.id === session.userId)
```

## Two facts that make this cheap

**1. The lookup keys match by construction, not coincidence.** Both sides read the same two
expressions off the same route object:

| | |
|---|---|
| `ApiAccessDescriptor.kt:31-33` | `method = route.method.value`, `uri = route.pattern.pattern` |
| `RestApiTsContributor.kt:235-236` | `httpMethod = route.method.value`, `pattern = route.pattern.pattern` |

Including the `{id}` placeholder form. There is no normalization gap to bridge — but pin it with a
test anyway, because a future change to either side would break it silently and secure-by-default
means the symptom is "every button disappeared".

**2. `ApiAccessLevel` and `UserApiAccessMatrix` already generate** into `models.ts` (lines 762,
919-929). No model work is needed.

## Absence means denied, and it is load-bearing

`ApiAccessDescriptor` **filters `Denied` entries out** before sending, deliberately, so the matrix
does not disclose the full API surface (`ApiAccessDescriptor.kt:28-30`). So the `?? 'Denied'`
fallback in the lookup is not defensive padding, it is the actual mechanism. Say so in a comment or
someone will "simplify" it.

## OPEN QUESTION — the logged-out case. Decide before writing `runtime/acl.ts`

`getMyApiAccess` lives in `AuthUserApi`, which floors `authenticated()`
(`funktor/auth/src/jvmMain/kotlin/api/AuthUserApi.kt:6`). An anonymous visitor cannot fetch a matrix
at all, so they get an empty ACL and **every route reads as Denied — including the public ones, and
including "Sign in"**. Options, none chosen:

1. Serve an anonymous matrix from a public endpoint.
2. Treat public routes as always-granted client-side. Needs route auth metadata in the SDK, which
   the generator currently discards — `funktor/rest` has `public()` / `authenticated()`
   (`auth/AuthRuleBuilder.kt:48,172`) and `RestApiTsContributor` reads none of it.
3. Make the empty ACL permissive and rely on the server. Simplest, and inverts the secure-by-default
   property — probably wrong, listed for completeness.

Option 2 is the most useful long-term (it also lets an auth transport wrapper skip token attachment
on public endpoints, which matters — sending a stale token to `signIn` is how you get confusing 401s
during re-login), and it is the most work.

## Spec

- [x] `runtime/route.ts`: `HttpMethod`, `RouteRef`, `Route<F>`, `route()`.
- [x] `runtime/acl.ts`: `ApiAcl` with `getAccessLevel` + the four predicates + `ApiAcl.empty`.
- [x] Emitter wraps every REST member in `route()` (`TsClientEmitter.kt`).
- [x] Emitter wraps every **SSE** member too — verified against a real stream fixture.
- [ ] **Decide the logged-out question above.** STILL OPEN — it does not block anything shipped here,
      because it is about which matrix gets fed in, not about the lookup.
- [x] Barrel and name-collision handling — the barrel already globs `TsRuntime.Module.entries`, and
      `ts-verify` imports through `index.ts`, so a collision is a compile error.
- [x] KDoc: `canAccess` includes `Partial`; the `?? 'Denied'` fallback is the mechanism.

### Also done, not in the original spec

- `TsClientSpec.Endpoint` REFUSES an HTTP method `HttpMethod` does not list, naming the route. The
  closed union would otherwise emit TypeScript that fails to compile inside output nobody can edit.
- `AclRuntimeParitySpec` in `funktor:codegen` — the level union, the key format, and the strings a
  generated member carries, all pinned against their Kotlin sources.

## Test evidence

- [x] ts-verify: 28 new checks — metadata on GET/POST/stream members, the wrapped member still
      performing its call, metadata surviving destructuring, the full 4x3 predicate grid, absence
      reading `Denied`, `ApiAcl.empty`, a near-miss key, a destructured predicate, and **7
      `@ts-expect-error` sites** proving the wrap did not widen the signature.
- [x] **Key parity** — `AclRuntimeParitySpec` asserts every emitted member carries exactly
      `route.method.value` / `route.pattern.pattern`, the same two expressions `ApiAccessDescriptor`
      builds its entries from. Derived from the route objects, not from hardcoded strings.
- [x] Unit: `ApiAclSpec` rewritten as the full 4x3 grid plus the three documented invariants at every
      level. The previous version asserted two predicates per level and could not have caught a
      predicate that ignores its argument.
- [x] **Mutation-tested, 8 mutants, 7 killed.** Killed: the `?? 'Denied'` fallback flipped;
      `canAccess` made strict; the lookup key dropping the method; `isDenied` inverted; `uri` losing
      `readonly` (TS2578); `HttpMethod` widened to `string` (TS2578); the level union dropping
      `Partial`; the key separator changed; the emitter emitting a pattern with `{}` stripped.
- [x] Full test command(s) run + green: `./gradlew :ultra:codegen:check :funktor:codegen:check
      :funktor:rest:jvmTest` — 259 / 59 / 111 tests, 0 failures. Compile sweep
      (`compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
      compileTestKotlin --continue`) clean.

### The surviving mutant, and why it stays alive

Widening `route()`'s bound to `(...args: any[]) => any` changed NOTHING — every check still passed
and every `@ts-expect-error` still errored. That is not a coverage hole: `F` is inferred from the
argument, so all of `never[]`, `any[]` and `Function` preserve the signature. Compiling all three
side by side confirmed it. **The `route.ts` KDoc and this file both claimed the opposite and were
wrong; both are corrected.** No test was added, because there is no observable difference to pin.

## Review record — /feature-review, 2026-08-01

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | FAIL -> fixed | 1 CRITICAL, 3 MEDIUM, 4 LOW |
| 2. Domain expert | FAIL -> fixed | 1 HIGH, 3 MEDIUM |
| 3. Security | PASS (no security defect) | 2 MEDIUM, 2 LOW — all non-security |

**The gate earned its keep.** It caught a build break that all of my own testing missed.

### CRITICAL — the barrel did not compile for any SDK that carries the auth models

Found INDEPENDENTLY by reviewers 1 and 3, and reproduced by me before acting. `acl.ts` exported
`ApiAccessLevel`; an SDK reaching `UserApiAccessMatrix` generates that same name into `models.ts` as
both a const and a type, and the barrel `export *`s both — `error TS2308`. It landed on exactly the
SDKs the feature exists for.

**Why my testing missed it, which is the transferable part:** the ts-verify fixture SDK has no auth
models, so the barrel never had two sources for the name. I had ticked "barrel and name-collision
handling — `ts-verify` imports through `index.ts`, so a collision is a compile error." The guard was
real; its fixture had a hole precisely where the feature ships. **A guard is only as good as the
fixture that exercises it — check the fixture covers the shipping case, not just a case.**

Fixed by renaming the union to `AccessLevel` (the same reason `AccessMatrix`/`AccessMatrixEntry`
already avoid their Kotlin names), plus a `FxAccessProbe` fixture that emits `ApiAccessLevel` into
generated output so `tsc` now compiles the barrel with both modules present.

### HIGH — `canFullyAccess` promised something funktor does not deliver

`Partial` is produced by NO framework rule: every DSL leaf yields `Granted` or `Denied`, `and`/`or`
fold with `maxOf`/`minOf`, `ConsistentParamRule.estimate` returns `Granted` deliberately, and
`OrgIsolationGuard` is a `RouteParamsGuard` — not an `AuthRule` — so `estimateAccess` cannot see it.
Verified by enumerating every `estimate()` override in the repo.

So my KDoc's "callable unconditionally, with no argument-dependent check" was false, and its worked
example was the exact case funktor reports as `Granted`: `AuthUserApi.setPassword` has only the
group's `authenticated()` floor and enforces ownership inside the handler. A frontend following that
KDoc renders "Change password" on every row, with the `canPartiallyAccess` branch dead.

Fixed in the KDoc on both sides, and `canPartiallyAccess` now says outright that it reads false for
every funktor route today. **The deeper fix is a MAINTAINER DECISION, see below.**

### Everything else fixed

| | |
|---|---|
| `canAccess` failed OPEN for a level outside the union | predicates reformulated — only the two POSITIVE ones test a literal, `isDenied` is their negation, so the invariants now hold unconditionally and an unknown level denies on all four |
| `route()` mutates its argument; `readonly` was type-level only | result is `Object.freeze`d, so identity is structurally immutable; ts-verify asserts the runtime throw as well as the type error |
| the parity spec's 4th test was vacuous (asserted a data class returns what it was given) | replaced with a real codec round-trip asserting the WIRE names `acl.ts` actually reads |
| `KNOWN_HTTP_METHODS` vs `HttpMethod` had no parity test, and the `require` had no test | both added in `AclRuntimeSpec` |
| the stream + no-params emitter branch had ZERO coverage — a live production shape | parameterless SSE fixture added; dropping its paren is now a TS1005 |
| 3 of 7 `@ts-expect-error` sites were byte-identical to pre-existing ones | removed; replaced with the one thing nothing pinned — the awaited PAYLOAD type |
| the key-format test's rationale was wrong (cross-language separator parity is unobservable) | comment and name corrected to what it really pins |
| KDoc claimed `ultra:codegen` cannot see `ultra:remote` — false, `build.gradle.kts:44` | the two union tests MOVED to `ultra:codegen`, next to the resources they guard |
| docs-site + LLM mirror still taught `hasAccessTo`/`hasAnyAccessTo` | both rewritten in lockstep, with the advisory framing they never carried |

Mutation-tested after fixing, 4/4 killed: re-introducing the collision, removing the freeze, dropping
the stream branch's paren, and drifting the two method unions apart.

### Still open — MAINTAINER DECISIONS, not defects

1. **Should `estimateAccess` degrade `Granted` to `Partial` when the rule chain contains a rule with
   `isCallerOnly() == false`?** That predicate already means "the decision depends on the arguments",
   which is exactly what `Partial` is defined as. It would make `Partial` reachable and give
   `canPartiallyAccess` a purpose — but it changes what every existing client sees, so it is not
   mine to make. Without it, `Partial` stays vestigial.
2. **`ApiAcl.empty` collapses four states into `Denied`** — server denied, matrix not loaded yet,
   fetch failed, route newer than the matrix. A 401 during token refresh is a normal event and makes
   every control vanish with no error, indistinguishable from an unprivileged account. Options:
   expose `isLoaded`, or make consumers hold `ApiAcl?` so the type forces the decision.
3. **The logged-out case** (unchanged from above) — still the one that blocks nothing but matters.
4. **Kotlin `ApiAcl` keys on `TypedApiEndpoint.uri`, which does NOT carry the group's `mountPoint`;
   the matrix keys on `pattern.pattern`, which does.** Latent — no production group passes a
   `mountPoint` today. Pre-existing, not introduced here, but it is the same class of silent
   deny-everything failure and deserves its own task.

**Red-team follow-up** (required): `.claude/tasks/YYYYMMDD-redteam-sdk-route-access-control.md`

**Scope note for the red team.** This layer is **advisory only** — the server is the authority and
nothing here enforces anything. The scenarios worth attempting are therefore about *misplaced trust*,
not bypass: does anything in the SDK or its docs invite a developer to treat `ApiAcl` as
enforcement; can a tampered matrix response cause the client to reveal anything it would not
otherwise; does `canAccess`'s permissive default surface an affordance that leaks the existence of
data the user cannot read.
