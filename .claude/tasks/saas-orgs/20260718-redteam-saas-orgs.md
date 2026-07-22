# Red-team: SaaS organisations (Phase O1)

**Status:** COLLECTED — do NOT execute during feature work; run in a dedicated pentest session
**Feature task:** `20260717-saas-organisation-storage.md`
**Scope:** `funktor/saas` — `Organisation`/`Branch` entity, `OrgsStorage`, `OrgsApiFeature`,
`ensureOrganisation` startup hook.

## Attack scenarios to attempt

### Access control on the CRUD API (`OrgsApi`, all `isSuperUser()`)
- [ ] Non-super-user JWT (regular member) calls list/get/create/update — must be denied, not just
      hidden. Confirm 401/403, no data leak in the error body.
- [ ] Anonymous calls to every endpoint — must be 401 (list+create confirmed in tests; verify get/update).
- [ ] API-key caller with crafted permissions — can it reach org CRUD?

### IDOR / id handling
- [ ] `GET/PUT /api/orgs/{id}` with a `_key` for an org in a different collection or a forged
      `collection/key` — does `findById`/`DOCUMENT(repo, id)` leak or cross repos? Test id values
      containing `/`, `..`, AQL/Mongo metacharacters.
- [ ] Enumerate orgs by guessing `_key`s (are keys sequential/predictable?).

### Tenant isolation (becomes critical once O2 membership scoping lands)
- [ ] After O2: a tenant admin of org A attempts read/update of org B via id — must fail.
- [ ] Branch-id spoofing: create/update an org with branch ids colliding with another org's.

### Data-integrity / storage
- [ ] Slug uniqueness under concurrency (cross-JVM) — two creates with the same slug; confirm the
      unique index rejects the loser and no partial/duplicate rows.
- [ ] `ensureBySlug` race: many concurrent callers (simulate multi-JVM boot) — exactly one org, no
      exception surfaced (the catch→re-read path holds).
- [ ] Oversized/hostile payloads: huge `branches` list, very long slug/name, unicode/control chars.

### Startup hook
- [ ] `ensureOrganisation` mis-config (empty slug, duplicate default across realms) — does boot fail
      safe or create garbage? Confirm an `AppStartException` path vs silent bad state.

## Notes
- Current CRUD is platform-super-user only; per-tenant authorization does not exist yet, so the
  cross-org isolation items are pre-registered for after O2/O3 add memberships + org-scoped rules.
