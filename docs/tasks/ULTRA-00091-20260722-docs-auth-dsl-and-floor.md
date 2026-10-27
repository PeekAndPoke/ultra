# ULTRA-00091: Docs follow-up: authorize DSL + ApiRoutes floor + part-3 checks (auth-hardening quartet)

> **Status (2026-10-19): PAUSED.** PARTIAL — rechecked against `docs-site/` on 2026-10-19. The floor and the `authorize {}` DSL ARE documented
> (commit `e2d6553b`, 2026-07-28). The part-3 mechanism and part 4 are NOT. COLLECTOR for the remaining documentation pass
> (do not write the prose here, capture what is left + anchors). Created 2026-07-22 per the "framework change → doc task"
> standing rule.
>
> **Part of:** ULTRA-00260 (Docs backlog epic)

**Type:** documentation
**Triggering changes:** `52512bd4` (part 1: block-style authorize DSL), `2b6d274d` (part 2: mandatory ApiRoutes floor), and
parts 3–4, which landed 2026-07-23 (two-phase auth + pluggable route checks + org-isolation + opt-in `ConsistentParam`;
`findById` → `Stored` route params). All four parts are DONE and archived in `docs/tasks-archive/2026-07/`.
**Overlap:** ULTRA-00135 (docs sync remaining) §2 lists the same part-3 gap ("`ConsistentParam` / caller-binding, two-phase auth
evaluation, `RouteBootCheck`, `RouteParamsGuard`"). One docs pass should close both; decide which doc owns it then.
**Settled?** Yes for the extension points — `RouteBootCheck.kt`, `RouteParamsGuard.kt` and `auth/AuthPhase.kt` have had no
commit since 2026-07-23. The moving part is org isolation: ULTRA-00094 (route check followups) item 5 may add a
`CrossOrgParam` opt-out beside `OrgAwareParam`, so keep the org-isolation prose to what is already on `saas.astro`.

## Already documented — do not redo (audited 2026-10-19)

| Topic | Where |
|---|---|
| Mandatory `authFloor`, append-only, validated at group construction | `docs-site/src/pages/ultra/funktor/rest.astro:136-147`; LLM mirror template `docs-site/src/data/llms/funktor.md:89-100` |
| `authorize {}` = statements and an AND chain, block `forAny {}` / `forAll {}`, no infix `and`/`or`, rules table incl. `forUserType`, `public()` / `forbidden()` as the SOLE rule, empty combinator rejected, "split mixed audiences into separate groups" | `rest.astro:149-210`; mirror `funktor.md:102-144` |
| Org isolation: `OrgAware` / `OrgAwareParam`, boot-forced, request-time guard, the canonical-`_id` contract | `docs-site/src/pages/ultra/funktor/saas.astro:49-` ; mirror `funktor.md:494-` |

## Still undocumented (audited 2026-10-19)

1. **Two-phase evaluation.** Caller-only rules (the floor, permission checks) run in PHASE 1, BEFORE param conversion
   (`findById`); param-dependent rules run in PHASE 2. It kills pre-auth DB reads and the 404-vs-401 existence oracle, and a
   hidden phase-2 failure answers a 404 byte-identical to a genuine miss. Worth a phase-1 / phase-2 diagram. Anchors:
   `funktor/rest/.../auth/AuthPhase.kt` (`isCallerOnly`, `flattenTopLevelAnds`), the module KDoc at the top of `routing.kt`
   and `dispatchPhase2`, `ApiRoute.phase1Denials` / `checkParamPhase`. Today only passing mentions exist
   (`auth.astro:264`, `funktor.md:302` name `RouteParamsGuard`s as something `estimateAccess` cannot see).
2. **`RouteBootCheck` and `RouteParamsGuard`** — the two extension points. `ValidateRoutesOnAppStarting` is a thin runner over
   `getAll(RouteBootCheck)`; a module adds a domain check with its own concrete types (saas contributes
   `OrgIsolationBootCheck` + `OrgIsolationGuard`). A guard that denies answers 404. Anchors:
   `funktor/rest/.../RouteBootCheck.kt`, `RouteParamsGuard.kt`, `ConverterCompatBootCheck.kt`, `AuthChainBootCheck.kt`.
3. **`ConsistentParam`** — PURE OPT-IN, for non-org referential consistency only, and explicitly not a tenant boundary
   (org isolation is the separate, forced mechanism). Anchor: `funktor/core/.../broker/ConsistentParam.kt`.
4. **`Stored<T>` route params** replacing handler-side `findById` (part 4). The ctor field keeps the name of the URI segment
   (`id`); a binding miss is a 404 with the same status the handler's `okOrNotFound` produced; binding runs only after
   phase 1; name param classes after the entity (`OrgParam`, `EventParam`), not a generic `IdParam`. `rest.astro` and
   `core.astro` never say that a `Stored<T>` field of a params class is resolved from the URI — the only place it appears is
   the org-isolation snippet (`saas.astro:52-63`), which does not explain the binding. Anchors: `OrgsApi.kt:26`,
   `FunktorConfApi.kt:21-23`, `funktor/all/src/jvmTest/kotlin/StoredParamMigrationE2eSpec.kt`.
5. **Two small gaps in pages that are otherwise done.** `rest.astro` "API features" (`:229`) shows a feature bundling two
   groups but never says why (one group per audience, since the floor is append-only); `auth.astro` has no pointer to the floor
   as the default-deny story. For consumers migrating their own `ApiRoutes`: before = `ApiRoutes("my")` + a per-route
   `authorize { isSuperUser() }`; after = `ApiRoutes("my", authFloor = { isSuperUser() })`, per-route blocks only to
   strengthen; public + protected routes become two groups.

## Do NOT

- Do not edit `docs-site/public/` LLM mirrors — edit the `docs-site/src/data/llms/*.md` templates.
- Do not write the prose in THIS file — this is the collector; the doc pass writes the pages.
- Keep it compact (CLAUDE.md "Keep docs compact"): one concept per page, an example only where it earns its place.

## History

- 2026-10-21: numbered ULTRA-00091 (was `docs/tasks/20260722-docs-auth-dsl-and-floor.md`).
