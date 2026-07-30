# SaaS Foundation — Multi-Tenancy + Document Tooling

**Status:** PLANNED 2026-07-17
**Goal:** Bring funktor to a state where multi-tenant SMB SaaS products (thebizz speedboats) can be
spun up quickly, and build a reusable document layer: Kotlin content data model + pluggable WYSIWYG
editor (kraft) + email/PDF renderers with app-specific extension points.
**Method:** Deep scan via 4-agent fleet (funktor module map, Opus auth/tenancy deep-dive,
templating building-block inventory, thebizz idea extraction) + coordinator synthesis.
**Relationship to `v1-roadmap.md`:** v1 hardening/release continues unchanged. This plan is the
product-platform work layered on top. Post-v1 backlog items #3 (account activation) and #5 (email
template editor) are absorbed here. `v1-email-auth-and-sessions.md` is a hard prerequisite (Track A).
**Update 2026-07-17:** Tracks A+B are superseded by the detailed plan
`20260717-auth-orgs-foundation.md` (Karsten confirmed: orgs+branches two-level tenancy on
`UserPermissions`, shared identity, 0/1/n login selection, no back-compat needed; auth is the
center-piece and goes first — all other tracks wait).

---

## Why these priorities (thebizz signal)

From 161 ideas across both pools (SMB/B2B weighted, shortlist = founder-interest signal):

| Shared capability | Ideas needing it | Covered today by |
|---|---|---|
| Document generation (PDF) | 25 | nothing (deps staged, unused) |
| Document editor / letters / templates | 30 | nothing |
| Multi-tenant B2B accounts | structurally most B2B ideas (Kanzlei/agency/Träger + their clients) | flat `organisations` set only |
| File upload + CSV/Excel ingestion | 47 | depot (storage only, no parsing) |
| Dashboards/reporting | 57 | kraft + chartjs addon ✅ |
| Forms intake | 34 | kraft forms ✅ |
| Email notifications/templates | recurring | funktor/messaging ✅ (senders), no templates |
| Billing/subscriptions | ~all (shape varies wildly) | nothing — **deferred, own plan** |
| Constraint solver (rostering/routing) | 53 (combinatorial pool) | out of scope — Python/OR-Tools boundary |

The shortlist (`thebizz/research/karsten-shortlist.md`) is dominated by German compliance/document
niches (XRechnung/ZUGFeRD e-invoices, warning letters, certificates, audit reports) — which makes
the document layer *groundwork*, not a feature. Note: ZUGFeRD = PDF/A-3 with embedded XML → PDF
engine choice must support that path eventually (see AD8).

---

## Key findings from the scan

Auth / tenancy:

- There is **no `UserCredentials` type**. The existing multi-tenancy notion is
  `UserPermissions.organisations: Set<String>` (`ultra/security/src/commonMain/kotlin/user/UserPermissions.kt:8`),
  JWT-carried (`jwt/builder.kt:25-50`), with a matching `forOrganisation()` auth rule
  (`funktor/rest/src/jvmMain/kotlin/auth/AuthRule.kt:65-76`) that **no route uses yet**.
- **Realm ≠ tenant.** `AuthRealm` is a static, build-time user-pool boundary (admin vs. customer),
  collected from the kontainer at startup (`funktor/auth/src/jvmMain/kotlin/index_jvm.kt:29-50`).
  Runtime tenant creation cannot be realm-based. Tenancy must be a new orthogonal dimension.
- Permissions are **flat** (roles/groups/permissions independent of organisations) — "admin in
  tenant A, viewer in tenant B" is inexpressible. `isSuperUser` bypasses every check.
- The realm-scoped `AuthRecord` storage (`FILTER(r.realm EQ realm)` on every query + compound
  index, `funktor/auth/.../db/karango/KarangoAuthRecordsRepo.kt`) is the one proven
  isolation-by-query pattern in the repo — the template for tenant scoping.
- Demo user store has a **globally unique email index** (`AdminUsersRepo.kt:75-82`).
- Sessions (`SessionStore` + records) are built & tested but **not wired**: no middleware, sign-in
  creates no session, `touch()` is a Phase-1 no-op. `AuthSystem.activate()` is stubbed.

Document layer:

- `funktor/messaging` is mature: `EmailBody.Html(block: HTML.() -> Unit)` kotlinx.html bridge, AWS
  SES + SendGrid senders, dev overrides, hooks, dual-backend sent-message storage. No template
  engine exists anywhere.
- `buildSrc/src/main/kotlin/Deps.kt` → `object Pdf` stages flying-saucer (+ iText5 backend) and
  Apache PDFBox 3.0.6, referenced by **zero modules**. `kraft/addons/pdfjs` is a viewer only.
- No WYSIWYG/rich-text/contenteditable code exists anywhere in kraft.
- Reusable editor building blocks: addon registry pattern (`kraft/core/.../addons/registry/`),
  forms abstraction split (core contract + semanticui components), drag & drop
  (`kraft/semanticui/.../dnd/`), modals/popups managers, messages pub-sub bus.
- House serialization idiom: `@Serializable sealed` + `@SerialName` per variant; `TypedAttributes`
  (`ultra/common/.../attributes.kt`) for type-safe extensible attribute bags; inline-CSS helper
  (`ultra/html/.../inline_style.kt`) — email HTML needs inlined styles.

Spin-up DX:

- **No starter/scaffold exists** — new projects hand-copy funktor-demo; the only minimal reference
  app is test code (`funktor/all/src/jvmTest/kotlin/`).
- **Docs promise a DX that doesn't exist**: `getting-started.astro` shows `funktorApp { server{} cli{} }`
  and `apiApp.mountAll(this)` — neither exists. The real bootstrap is ~11 manual wiring steps.
- Subdomain/host routing + CORS allow-lists are hand-rolled per project (`funktor-demo/server/.../server.kt:59-89`).
- Per-realm auth setup is heavy boilerplate (full `AuthRealm<USER>` impl per user population).
- No deployment story: no Dockerfile, no SPA→server bundling, no CI config in the demo.

---

## Architecture decisions (AD1–AD4 CONFIRMED 2026-07-17; rest PROPOSED — confirm before building)

- **AD1 — Tenant is orthogonal to realm.** ✅ CONFIRMED. Realms divide end-user vs. admin-user vs.
  global management interfaces. Tenancy is two-level: **Organisation** (chain) → **Branch** (site),
  carried by `UserPermissions.organisations`/`branches`. Detailed design:
  `20260717-auth-orgs-foundation.md`.
- **AD2 — Identity vs. membership.** ✅ CONFIRMED. One user account per email (global unique email
  stays); accounts can belong to multiple organisations via embedded `OrgMembership`.
- **AD3 — Single-selected-org per session (Model C), flat claims preserved.** ✅ CONFIRMED: login
  resolves 0/1/n accessible orgs (0 → no access, 1 → auto-select, n → selection step).
  `UserPermissions.organisations: Set` → `org: String?` (the one selected org) + `accessibleOrgs:
  Set`; `branches`/`roles`/`permissions` are the selected org's slice, derived from the DB at login.
  No separate org claim (rides in the permissions claim). No live switch — re-login to change org.
  Full design: `20260717-auth-orgs-foundation.md`.
- **AD4 — Tenant admin is a tenant role,** not `isSuperUser`. `isSuperUser` remains platform-only.
- **AD5 — Enforced tenant scoping at the data layer.** Entities opt in via a tenant field +
  a base-repo/filter hook that injects `tenant == current` automatically (modeled on the realm
  filtering in `KarangoAuthRecordsRepo`). Hand-written per-query filters are the failure mode to
  design away.
- **AD6 — Document model uses open polymorphism.** `Block` is a non-sealed `@Serializable`
  interface; core blocks ship in the library, apps register custom blocks via a single
  `DocumentRegistry` entry that binds serializer + renderer(s) + editor component in one call
  (kotlinx.serialization `SerializersModule` under the hood). This is the app-specific extension
  point for both rendering and editing.
- **AD7 — One Block→HTML core renderer, adapters on top.** Renderer lives in `commonMain`
  (kotlinx.html is multiplatform) so the browser preview in the editor and the server-side email
  render are the *same code*. Email adapter targets `EmailBody.Html`; PDF adapter feeds the HTML
  (+ inline CSS) into the PDF engine.
- **AD8 — PDF engine needs a licensing spike before wiring.** The staged `flying-saucer-itext5`
  depends on iText5 (**AGPL — unusable for closed commercial products**). Evaluate
  `openhtmltopdf` (PDFBox-based, maintained flying-saucer fork) and `flying-saucer-pdf-openpdf`
  (LGPL/MPL). ZUGFeRD/XRechnung (PDF/A-3 + embedded XML, e.g. via Mustang/PDFBox) comes later as a
  vertical addon on the same PDFBox base.
- **AD9 — Proposed module homes** (follow existing layout conventions):
  `funktor/saas` (orgs/tenancy + future SaaS features — see `20260717-auth-orgs-foundation.md`),
  `ultra/document` (block model + HTML renderer, commonMain), `ultra/document-pdf` (JVM PDF
  adapter), `kraft/addons/document-editor` (editor).

---

## Tracks A + B — SUPERSEDED (2026-07-17)

Both tracks are replaced by the detailed plan **`20260717-auth-orgs-foundation.md`**:
Phases O1–O4 (organisations/branches/membership/login-selection) cover the former Track B core;
Phase F covers the former Track A (all open user flows, org-aware). Items from Track B that are
**not** in the detailed plan and remain future work here: invitations (was B3), enforced
org-scoped data access (was B6), org-role AuthRule extensions beyond selection (was B7),
per-org email branding (was B9).

## Track C — Document model & renderers (`ultra/document`, `ultra/document-pdf`)

- [ ] **C1** Block model core set in `commonMain`: `Document`, `Block` (open polymorphic interface,
      AD6) with core blocks: Headline(level), Paragraph (rich-text spans: bold/italic/link),
      BulletList/NumberedList, Table (rows/cells, header row), Image (depot/URL ref), Divider,
      Spacer. Style/theme model: fonts, sizes, colors, spacing as a `DocumentTheme` (not per-block
      inline styling; blocks carry semantic attributes + `TypedAttributes` escape hatch).
- [ ] **C2** `DocumentRegistry`: single registration point binding block type → serializer
      (+ later: renderer, editor). Ships with core blocks pre-registered; apps add custom blocks.
- [ ] **C3** Merge fields / data binding: inline `Variable` span + `RenderContext` (map/typed
      lookup) resolved at render time — required for email templates ("Hello {{user.name}}") and
      letters.
- [ ] **C4** Block→HTML renderer in `commonMain` (kotlinx.html + inline CSS via `ultra/html`
      helpers, email-client-safe subset: tables-for-layout where needed, no external CSS).
      Renderer registry keyed by block type (extension point, AD6/AD7).
- [ ] **C5** Email adapter: `Document.toEmailBody(theme, context): EmailBody.Html` + wiring docs
      for `Mailing`. Golden-file tests for rendered HTML.
- [ ] **C6** Template storage: vault entity for stored documents/templates (name, document,
      version, updatedBy), both backends — this is what apps CRUD and the editor edits.
      Tenant-scoped once B6 exists (design the entity with the tenant field from day one).
- [ ] **C7** PDF spike (AD8): evaluate openhtmltopdf vs. flying-saucer-openpdf on license, PDF/A
      support, CSS fidelity, page-break control. Output: decision + updated `Deps.kt` (remove the
      staged iText5 artifact). Timebox: 1 day.
- [ ] **C8** Block→PDF adapter in `ultra/document-pdf` (JVM): page model (size, margins,
      header/footer with page numbers, page-break hints on blocks), via the C4 HTML output + chosen
      engine. Golden-file/visual tests.
- [ ] **C9** (later, vertical addon) ZUGFeRD/XRechnung support on the PDFBox base — first product
      that needs it drives it; keep out of the core.

## Track D — WYSIWYG editor (`kraft/addons/document-editor`)

- [ ] **D1** Editor contract: `BlockEditor` component interface + editor registry keyed by block
      type (mirrors C2; one `DocumentRegistry` call registers block + serializer + renderer +
      editor). Follow the forms pattern: core contract, semanticui-styled implementations.
- [ ] **D2** Editor MVP: block list with add/remove/reorder (reuse `kraft/semanticui/dnd`),
      per-block editors for the core set (headline, paragraph, lists, image picker, table editor,
      divider/spacer), block-type picker (popups manager), editor state via messages bus like
      `FormFieldComponent` ↔ controller.
- [ ] **D3** Live preview pane using the C4 `commonMain` HTML renderer client-side — same code as
      the server render, so WYSIWYG is honest by construction.
- [ ] **D4** Rich-text span editing inside Paragraph/Headline (bold/italic/link). Spike first:
      contenteditable-per-block with span-model sync vs. structured toolbar editing. Keep the span
      model (C1) the source of truth either way.
- [ ] **D5** Custom-block demo: register one app-specific block end-to-end (model + renderer +
      editor) in funktor-demo — this is the acceptance test for the extension-point story.
- [ ] **D6** Template management screens in funktor-demo: list/edit/preview/send-test-email
      against C6 storage. (Absorbs v1-roadmap post-v1 item #5 "Email template editor".)

## Track E — Spin-up DX (starter + deployment)

- [ ] **E1** Close the docs-DX gap by *implementing* the documented API: `funktorApp { server{}
      cli{} }` DSL + `ApiApp.mountAll(...)` (or consciously re-document the real API — decide;
      implementing is preferred since the docs describe the DX we want).
- [ ] **E2** Generic default `AuthRealm` ("email/password auth for entity X" without hand-writing
      JWT construction, serialization, signup creation) — kills the biggest per-project boilerplate.
- [ ] **E3** Config-driven host/subdomain + CORS helper (replaces hand-rolled
      `host("api.*".toRegex())` + hardcoded `allowHost` lists).
- [ ] **E4** `funktor-starter` template: minimal multi-tenant SaaS skeleton — server + common +
      kraft SPA, ONE db backend (align with v1-roadmap Track E1 decision), default realm (E2),
      tenants module (Track B), document editor page (Track D), fixtures, Dockerfile +
      docker-compose + SPA→server bundling task. "Clone → rename → runs" is the acceptance test.
- [ ] **E5** (defer) Project generator CLI (`funktor new <name>`) once E4 stabilizes as a template.

---

## Critical path & parallelization

```
20260717-auth-orgs-foundation.md (O1–O4, then F) ── the center-piece, goes FIRST (decided 2026-07-17)
C1–C5 (doc model+email) ─► C6 ──► C7–C8 (PDF)
D1–D3 need C1–C4          D4–D6 after MVP
E1–E3 anytime; E4 needs auth-orgs + D2 minimum
```

- Auth/orgs foundation first — all other tracks wait (Karsten, 2026-07-17). Track C is technically
  independent and could run in parallel if capacity allows, but is not scheduled yet.
- Track D starts once C1–C4 exist.
- Groundwork gates before features: **G-A** (auth-orgs acceptance gate — see the detailed plan),
  **G-D** (same document renders identically in editor preview, email, and PDF: C4+C8+D3),
  **G-S** (starter clones and runs in <15 min: E4).

## Open decisions for Karsten

1. ~~AD2 identity model~~ — ✅ ANSWERED 2026-07-17: shared identity + multi-org membership.
2. ~~AD3 JWT approach~~ — ✅ ANSWERED 2026-07-17: selected-org-per-session + re-issue on switch.
   (Smaller follow-up recommendations are listed in `20260717-auth-orgs-foundation.md`.)
3. **PDF engine** — decided after C7 spike; flag now: staged iText5 artifact is AGPL.
4. **Starter DB backend default** — ties into the open v1-roadmap Track E1 decision.
5. **E1** — implement the documented DSL vs. re-document reality.

## Explicitly deferred (observed needs, not scheduled)

- **Billing/subscriptions module** — universal across thebizz ideas but pricing shapes vary
  (flat, per-seat, per-solve, escrow, commission). Plan it when the first product locks a model.
- **File-ingestion toolkit** (CSV/Excel parsing helpers) — 47 ideas need uploads; depot stores
  files but nothing parses them. Candidate `funktor/ingest` later.
- **Audit-trail module** (tamper-evident compliance logs/exports) — recurring compliance theme;
  builds on funktor/logging.
- **Constraint-solver core** — the combinatorial pool's shared engine is Python/OR-Tools territory;
  products would call it as a service. Repo-boundary decision belongs to thebizz, not here.
- Depot encryption-at-rest + configurable log appender — already tracked as funktor TODOs.
