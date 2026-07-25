# Monko `findById` silently coerces a foreign-collection `_id` — Karango does not

**Status:** IDEA / not scheduled. Surfaced 2026-07-25 by the security review of
`.claude/tasks/20260725-value-class-userid.md`. **Pre-existing** — not introduced by that work, and
**not exploitable today** (see the guards below).

## The asymmetry

`ultra/vault/src/jvmMain/kotlin/helpers.kt` —
```kotlin
val String.ensureKey get() = if (contains('/')) split('/')[1] else this
```

- **Monko** (`monko/core/src/main/kotlin/MonkoRepository.kt`, `findById`) applies `ensureKey`, so it
  STRIPS any collection prefix. `b2b2cUsers.findById("b2b_users/K")` happily resolves the
  `b2b2c_users` document with key `K` — a DIFFERENT entity than the caller asked for.
- **Karango** (`karango/core/src/main/kotlin/vault/EntityRepository.kt`) issues
  `DOCUMENT(collection, id)`; ArangoDB's two-argument form validates the collection prefix, so the
  same call returns nothing.

So the two supported backends disagree on what a cross-collection `_id` means. Any code that passes
a realm-qualified `_id` to the "wrong" repository is silently wrong on Mongo and correctly empty on
Arango.

## Why it matters (and why it is not urgent)

The exposed shape is: **realm comes from the URL, user id comes from the caller's token.**
`AuthUserAdapter.loadById` is a bare `repo.findById(id)` in all four demo realms, and both
`AuthRealm.refreshToken` and `EmailAndPasswordAuth.setPassword` are reachable that way.

Three independent guards currently hold, which is why this is an idea and not a bug report:
1. `refreshToken` re-validates `expectedUserType` against the freshly minted token.
2. `setPassword` re-derives `ownerId` from the LOADED `user._id`, and still requires the victim's
   current password.
3. `B2bMembersApi.b2bUserOf` has an explicit round-trip guard: `findById(x)?.takeIf { it._id == x }`.

Note that only #3 is explicit about the hazard. The others hold for unrelated reasons, which is a
fragile way to be safe.

## Options

1. **Align Monko with Karango (preferred):** `findById` rejects (returns null for) an id whose
   collection prefix is present and ≠ this repo's `name`. Framework-wide behaviour change — needs its
   own gated feature + a `MatrixTest2d` differential test asserting both backends agree.
2. **Type the boundary:** give `AuthUserAdapter.loadById` a `UserId` parameter and have adapters do
   `findById(id.value)?.takeIf { it._id == id.value }`, making the round-trip guard the default
   everywhere instead of a one-off in the demo.
3. Do nothing; keep it as a red-team probe (`.claude/tasks/20260725-redteam-value-class-userid.md`
   §3 and §4 cover it).

Option 1 and 2 are complementary — 1 fixes the framework, 2 makes the call sites self-documenting.

## Also worth folding in

`ensureKey` uses `split('/')[1]`, i.e. the segment after the FIRST slash — a key that itself contains
a slash is silently truncated. Decide whether that is a documented constraint or a latent bug.
