In this file i will collect observations i saw during reviewing the code.

Resolved items are marked DONE with the date. Keep this file as a live list, not a snapshot.

# Missing imports for referenced types in docs — DONE 2026-07-28

Problem:

For example in AuthError.kt the docs reference [AuthSignInResponse.ActivationRequired] but AuthSignInResponse was not
imported. This leads to an IDE warning

Solution:

Always import the referenced types. If the referenced type cannot be imported, since it lives in module not available to
the code, DO NOT write the type into brackets or leave the reference out completely.

# AuthRealm KnownRole and getKnownRoles — DONE 2026-07-28

Problem:

What are these needed for? Ok i see they are used for the ApiAccessDescriptor.collectKnownRoles () ... and here we
duplicate the code of the known roles again.

Suggestion:

We should move the KnownRole class somewhere more central. Where? The getKnownRoles () should return an empty list by
default. ApiAccessDescriptor.collectKnownRoles () should always append SuperUser and Anonymous to the list it gets.

KnownRole should have two static (companion properties):
.superUser = KnownRole ("SuperUser", UserPermissions (isSuperUser = true))
.anonymous = KnownRole ("Anonymous", UserPermissions ())
...which we will reuse.

Ok and each realm has to override the getKnownRoles, correct? This means whenever a roles is added, someone needs to
remember to adjust this function. Would be better to have some data structure that defines all roles for a realm, and is
passed into the realm... not sure exactly, we need to discuss.

# AuthRecordStorage comments and test coverage — DONE 2026-07-28

Problem:

AuthRecordStorage seems to not have a test for hasExpired (). Also not clear if the other functions are well tested.

Solution:

Test all the methods, for each db backend.

# Missing comments — DONE 2026-07-28

Problem:

The following files are missing comments and description of purpose:

- AuthSystemAppHooks.kt

Solution:

Add proper comments, precise and concise.

# OrgPolicy and SignupOrgBehavior placement — DONE 2026-07-28 (kept in auth)

Problem:

OrgPolicy and SignupOrgBehavior currently lives in the auth module, while they are related to SaaS.

Proposal:

Check if they can easily be moved to the saas module without the need to introduce more plumbing.

# SessionJwtClaims — KDoc DONE 2026-07-28, feature tasked

Questions:

This seems to be stubs for future features, like revoking tokens, correct? But there also is a SessionStore in place
already.

Do we have or plan to have a list of devices a user is logged into? How do we identify the device and send this info to
the backend? How do we persist the session id on the frontend, so we do not create an ever growing list of tokens,
whenever the user logs in. Do we plan to give the user or admins or superusers the chance to revoke individual tokens or
all tokens of a user or an entire org? Do we auto-prune expired tokens from the SessionStorage?

---

# Resolution notes (2026-07-28)

**KDoc refs.** 3 real cases, all in `commonMain/model` pointing at `jvmMain` types they cannot import
(`AuthOrgRef` → AuthRealm; `AuthSignInResponse` → AuthError, OrgPolicy). Brackets dropped. Note that
same-package refs resolve WITHOUT an import, so "always import" would add noise — the rule recorded in
CLAUDE.md is "if `[X]` does not resolve, import it or drop the brackets".

**KnownRole.** Moved to `ultra/security/.../user/KnownRole.kt` beside `UserPermissions`, with
`.superUser`, `.anonymous` and `.universal` companions. `getKnownRoles()` now returns `emptyList()`.
`ApiAccessDescriptor.collectKnownRoles()` appends `KnownRole.universal` once and dedupes.

This fixed a real bug, not just duplication of code: with 4 demo realms and none overriding the
default, the matrix was rendering **SuperUser ×4 and Anonymous ×4**, and the no-realms fallback branch
was unreachable (it could only fire with zero realms registered).

Your open question — "someone must remember to update the override when a role is added" — is NOT
solved by this change; it is still a function to override. Deliberate: fixing the default is the small
correct step, and the better shape (roles as data passed into the realm, or derived from wherever
roles are authored) needs a look at how roles are declared today.

**AuthRecordStorage.** Added 4 tests to the shared base spec, so they run on Karango AND Monko: 7 → 11
each. Covers `hasNotExpired` with a NULL expiry (the property that makes a PendingActivation marker
permanent — previously unpinned), the expiry boundary in both directions, `findAllByOwner` scoping and
expiry filtering, and `removeById`. Mutation-verified twice: making `hasNotExpired` always true, and
flipping the null branch, each fail the new tests.

**AuthSystemAppHooks.** KDoc added, and `import io.ktor.server.application.*` replaced with the
explicit import — it broke the repo's own no-wildcard rule. The KDoc states what `validateRealms()`
actually does today (a duplicate-realm-id check), not what it might grow into.

**OrgPolicy.** Kept in `funktor:auth`. Moving it to saas would force **auth → saas**; today they are
siblings over core/rest and saas is the more specific module. The org types genuinely shared with saas
(`OrgId`, `OrgMembership`, `SelectedOrg`) already live lower, in `ultra:security`. A KDoc section now
records this so the question is not re-opened.

`SignupOrgBehavior` was **completely unused** — zero references outside its own declaration — so it is
deleted, along with the `onSignup` parameter of `OrgPolicy.Required` (which becomes a `data object`).

**SessionJwtClaims.** Both KDocs described the feature in the present tense as though wired. It is not:
`withSessionId`/`sessionIdClaim()` have no callers and no production code registers a `SessionStore`.
The storage half IS complete (revoke, revokeAllForUser, listForUser, Null/Vault/Cached, TTL-index
pruning, device fields). KDocs corrected; the remaining work and your five questions are captured in
`.claude/tasks/20260728-session-revocation-wiring.md`.
