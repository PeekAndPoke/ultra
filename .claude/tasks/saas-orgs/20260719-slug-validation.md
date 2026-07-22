# Subdomain-safe slug validation (org + branch)

**Status:** IN PROGRESS (2026-07-19)
**Plan:** (saas foundation — org/branch slug is a future tenant subdomain)
**Security-critical:** low (input validation; the slug will later drive subdomain→org auto-select, so
a bad slug is a routing/tenant-selection concern more than an authz one).

## Spec

Slug must be a valid, lowercase **DNS label** because it may become a tenant subdomain
(`acme.b2b.example.com`) that auto-selects the org on login.

- [x] Single shared rule in `funktor/saas` **commonMain** (`domain/Slugs.kt`) so UI and server can't
      drift: lowercase `[a-z0-9-]`, no leading/trailing hyphen, length 2..63, not a reserved infra
      name (`www/api/app/admin/ops/b2b/b2b2c/mail/...`).
- [x] **Server** validation in `OrgsApi` — org slug on create (`400` with reason), branch slugs in
      `validateBranches`. Slug is immutable on update (no slug in `UpdateOrgRequest`).
- [x] **UI** validation via `accepts(validSlug())` + normalize-on-input (`Slugs.normalize`) on the
      slug field (shown only when creating). `validSlug()` delegates to the shared `Slugs`.
- [x] Pure unit coverage of the rule.

## Implementation notes

Layered so the charset/shape rule has ONE definition, shared by UI and server (mirrors how
`validEmail` wraps `ultra.common.isEmail`):

- `ultra/common` — `String.isSlug()` + `SlugRegex` (`regexes.kt`/`strings.kt`): the canonical
  DNS-label predicate (lowercase alnum + internal hyphens, length 1..63). Sits below both kraft and
  saas, so no backwards dependency and no drift.
- `kraft/core` `string_rules_extra.kt` — generic `validSlug()` form rule wrapping `isSlug()` (next
  to `validEmail`/`validUrlWithProtocol`). This is the reusable UI rule any app uses.
- `funktor/saas/src/commonMain/.../domain/Slugs.kt` — server contract: `normalize`, `validationError`
  (granular messages + **RESERVED** names + min-length 2, charset via `isSlug()`), `isValid`, plus a
  `normalizeSlug` top-level alias so the 4 existing jvm call sites need no change. Old jvmMain
  `domain/slugs.kt` deleted.
- `OrgEditPage.kt` — `accepts(validSlug())` (from kraft) + normalize-on-input via `Slugs.normalize`.
- **RESERVED list is a product decision** — kept minimal + explicit; confirm/extend with the user.
  It lives server-side only (Kraft's generic `validSlug` doesn't know it); the server rejects
  reserved slugs with a 400 the UI surfaces.

## Test evidence

- [x] `SlugsSpec` (commonTest, runs on jvm) — **8/8 green**
      (`./gradlew :funktor:saas:jvmTest --tests "io.peekandpoke.funktor.saas.domain.SlugsSpec"`).
- [x] `:funktor:saas:compileKotlinJvm` + `compileKotlinJs` + `:funktor-demo:ops-app:compileKotlinJs`
      green.
- [ ] **Endpoint e2e gap:** create/update rejecting bad slugs via `AppUnderTest` needs a super-user
      auth path in the demo test harness (no funktor-demo test authenticates a real user yet). This
      harness is a shared prerequisite with `20260719-cross-realm-authz-and-tests.md` — build it
      once, then add: create with reserved/invalid/duplicate slug ⇒ 400/409; valid ⇒ 200; branch
      with bad slug ⇒ 400. Until then, coverage is the pure `SlugsSpec` + the server calling the
      same `Slugs.validationError`.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |
