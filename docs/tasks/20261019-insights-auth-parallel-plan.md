# Plan: insights, then auth, in parallel worktrees

**Status:** IN PROGRESS — maintainer go 2026-10-19: **two tracks at a time**; D3, D8, D9 decided (below)
**Plan:** none — maintainer request 2026-10-19 ("finish the insights first, then auth"; "consider multiple agents on
worktrees … which parts can be developed independently and are easy to merge")
**Security-critical:** the auth tracks are; each gets its own task file and red-team follow-up

## What constrains the split

- **Memory, not cores.** 16 cores, 61 GB, ~9 GB free with the IDE (12 GB), the Kotlin daemon (7 GB) and the other
  projects' Gradle daemons. One worktree build with tests takes 3–6 GB → at most **2–3 builds at once**. The
  agent-fleet skill caps concurrent heavy (opus) agents at ~2–3, reviewers included.
- **The test databases are shared.** `ArangoDbConfig.forUnitTests` is `_system`
  (`karango/core/src/main/kotlin/config/ArangoDbConfig.kt:21`), `MongoDbConfig.forUnitTests` is `test`
  (`monko/core/src/main/kotlin/MongoDbConfig.kt:11`). Two worktrees running DB-backed suites wipe each other's
  collections — the same class of damage as today's overlapping builds, at the database instead of the build cache.
- **Every build rewrites tracked files**: `docs/kraft/examples/**` (`distributeJsExample`, `buildSrc/.../Docs.kt`) and
  `kotlin-js-store/yarn.lock` (`yarnLockAutoReplace`). Committed from several branches, they conflict on every merge.
- **Shared docs** — `CLAUDE.md`, skills, ULTRA-00028 (v1 roadmap), `CHANGELOG.md`, `BUILD-LOCK.md` — change on master only.

## Wave 0 — coordinator, on master, before any worktree starts

- [x] **E1** `auth-increments` merged into `master` (`f61c8731`, tree identical, local — not pushed).
- [x] **E2** Per-worktree test databases: `forUnitTests` honours `ULTRA_TEST_ARANGO_DB` / `ULTRA_TEST_MONGO_DB`
      (defaults unchanged), the same for funktor-demo's `application.test.conf`; a setup script creates the Arango
      databases per worktree (Karango never creates a database). `console/worktree-new.sh`, `console/wt-gradle.sh`.
      Verified 2026-10-19: `:funktor:cluster:jvmTest` + `:funktor-demo:server:test` with probe values wrote their
      collections into the probe databases on both backends (the override reaches the forked test JVMs).
- [x] **E3** Generated files: take `distributeJsExample` out of `build` into an explicit task run at release (D9). Verified: an example's `build` leaves `docs/` untouched; the release skill has the step.
- [x] **E4** (`428435c0`, ran alongside Wave 1; touched only task docs no track owns) Task-doc housekeeping in one commit: archive what is done but still open (auth-hardening parts 1–4,
      account-activation backend, activation-resend, apiacl naming, value-class EmailAddress, ktor-client
      unification, …) and fix the stale claims the inventory found (TAB-SPECS `createdAt`/`isSystem`, the Kraft doc's
      "gate has not run", `insights.astro` documenting the deleted `InsightsGui`). Every worktree then starts from
      accurate docs, and nobody edits them in parallel.
- [x] **E5** One full suite at a time across all worktrees: `console/wt-gradle.sh --full-suite` (`flock /tmp/ultra-full-suite.lock`).
      Module-level builds run freely in their own worktree.

## The tracks

| Track | Closes | Touches | Must not collide with |
|---|---|---|---|
| **T1 insights-vue** | `20260802-insights-vue-tabs.md` gate blockers (S1 at the source, I4, TAB-SPECS from a current record, D5/D6), then `20260824-insights-graphs-and-static-slices.md` Parts C+D (vis-network, tab buttons) | `funktor/codegen/src/main/resources/ts/{insights,ui}`, `InsightsTsContributor` + spec, `monko/core` (placeholder-ised query for S1), `funktor/insights/reference/TAB-SPECS.md`, `funktor-demo/sdkgen-app` | T2 only through the wire contract |
| **T2 insights-backend** | graphs task Parts A+B: the two graph endpoints, the slim kontainer slice (wire break, old records still render), e2e | `funktor/insights` (api, collectors, models), new spec files in `funktor/all` | T1 (contract), T4 (`InsightsApi.kt` — so T4 waits) |
| **T3 locks-heartbeat** | `20261019-global-locks-heartbeat.md` (agreed; draft code and tests exist) | `funktor/cluster/.../locks`, `funktor/inspect` lock model + list page, new e2e specs | nothing |
| **T4 insights-kraft** | `20260824-insights-kraft-gui.md` | `funktor/insights` commonMain (API + DTOs), new pages in `funktor/inspect/src/jsMain`, the two loose ends | T2 (`InsightsApi.kt`) → starts after T2 lands |
| **T5 auth-errors** | `error-disclosure/*` (generic production messages, account enumeration, env classification, fixtures gate) | `funktor/rest` (`ApiStatusPages`), `funktor/core` (`KtorConfig`, fixtures), `funktor/auth` (`AuthLoginApi`, `AuthUserApi`, `EmailAndPasswordAuth` messages) | password track (`EmailAndPasswordAuth.kt`) → that one goes after |
| **T6 auth-sso-verification** | ULTRA-00118 (sso email verification) | `funktor/auth` SSO providers | T5 only if both touch `AuthLoginApi` |
| **T7 auth-sessions** | ULTRA-00136 (session revocation wiring) | `funktor/auth` (`AuthRealm`, JWT claims, `SessionStore`) | T5/T6 in `AuthRealm` → after them |
| **insights → `20261019-insights-production-gate.md`** (graphs 404 when disabled: decided 2026-10-19; the rest open) | a production gate for insights: nothing refuses `enabled = true` in production (no environment allow-list, no `RefuseInsightsInProduction`; scenario 11 of ULTRA-00058 (redteam error disclosure) presumes one). Residue of the archived ULTRA-00057 (insights gui auth gate) | `funktor/insights` | T2 → after it lands, same area |
| **found by T1's gate → in `20261019-insights-production-gate.md` (to decide)** | `RuntimeCollector.kt:58` records ALL JVM system properties, unredacted, on every request (anonymous ones too) — a secret passed as `-D…` reaches the depot and the Runtime tab | `funktor/insights` | goes with the insights production gate |
| **auth, no task yet** | `RefreshToken` is a GET that mints a token (`AuthApiClient.kt:95`) and bodiless mutating routes bypass the Content-Type gate (`ApiRoutes.kt:307`). Residuals recorded only in the archived `20260802-rest-content-type-enforcement.md` | `funktor/auth`, `funktor/rest` | T5 |
| **found by T3, no task yet** | `KarangoDriver` runs every query in `async(Dispatchers.IO)` (`karango/core/src/main/kotlin/vault/KarangoDriver.kt:120,144`), so a saturated IO pool stalls ANY query — the lock heartbeat's dedicated dispatcher cannot help on Karango (measured, T3's skipped test `KarangoGlobalLockProviderSpec.kt:58`) | `karango/core` | none |
| **found by T3, no task yet** | `ultra:cache` `FastCacheOnEvictionSpec` "onEviction fires when expireAfterAccess evicts" (linuxX64) failed once under full-suite load, green alone — a timing flake that can turn a full suite red | `ultra/cache` | none |
| **found by T3's gate, no task yet** | The demo's PUBLIC `getActiveLocks` (`funktor-demo/server/src/main/kotlin/api/showcase/ClusterShowcaseApi.kt:34,145-163`, `authFloor = { public() }`) shows anonymous callers lock keys (org keys, entity ids) and `serverId`s, which carry hostname, port and MAC addresses (`GlobalServerId.kt:12-20`, `NetworkUtils.kt:44-56`). The group is public on purpose; decide whether the server ids should be | `funktor-demo` | none |
| **found by the activation-resend review, no task yet** | Monko's auth-record TTL index never culls: `expiresAt` is a `Long` of epoch seconds (`funktor/auth/src/jvmMain/kotlin/domain/AuthRecord.kt:26,35,91,112,134`), and a MongoDB TTL index (`db/monko/MonkoAuthRecordsRepo.kt:56-59`) only expires BSON Date fields — sessions, recovery/verification/resend tokens pile up on Mongo, contrary to the comment there (Karango's TTL takes numeric seconds and works). Persistence: fix + mutation-checked test on Mongo. Clock (maintainer 2026-10-19): where a TTL index decides expiry, compute from the DB clock (a server-side `$$NOW` / `DATE_NOW()`), not the app server's | `funktor/auth` (Monko) | T5–T7 (same module) |
| later | password hardening (after T5), JWT rotation operability (independent, small — a filler), CSP + token TTL, SDK auth Vue binding + SSE (after T1: same TS tree), route-check follow-ups | | |

**Wave 1** (two at a time, maintainer 2026-10-19) — T2 and T3 first (T3 is drafted and frees its slot soon;
T1 profits from T2's new kontainer shape landing first), then T1. **Wave 2** — T4 once T2 has landed, T5 as the first auth track (insights keep priority for
slots). Then T6, T7 and the rest as slots free up. Docs follow-ups land only after their code has settled.

**Progress (2026-10-19):** T3 DONE (gate PASS, merged `d9ffbb32`). T2 DONE (gate PASS, merged `d9ffbb32`). T1 DONE
(gate PASS in round 2, merged `a578ef0a`; MINORs → `20261019-insights-vue-round2-minors.md`; the vue-tabs task stays open
for its inventory gaps). Activation hardening DONE (gate PASS in round 4, `reviewer-top`; MINORs fixed + delta check; merged `ed8d3b38`;
archived `docs/tasks-archive/2026-10/20261019-activation-hardening.md`; docs folded into the email-first task). `insights-minors` DONE (review clean, merged `348c66bc`; follow-ups →
`20261019-insights-minors-followups.md`). T4 insights-kraft started, then PAUSED 2026-10-19 (budget precaution; WIP `24e745bf` on its branch). **Stable point
first:** finish activation hardening, then release 0.114.0; everything else waits until after it. First auth task after the release: `20261019-email-first-signup.md` (maintainer
2026-10-19; closes the one-click pre-hijack the hardening leaves). Also after the release: answer Mission-Control's
interview on how other repos' agents learn Kraft/Funktor (llms/*.md as entry point, what is current vs stale, a
per-library "mistakes agents make" section, common Kraft UI errors from the example bumps) — low priority. With it, in the same reply round: Mission-Control's RFC on the shared review-loop blueprint
(`/opt/dev/peekandpoke/missioncontrol/blueprints/skills/review-loop/SKILL.md`: usefulness and adoption, changes marked core vs
ultra-only, the 7 open questions; do NOT adopt before the maintainer decides).

## Working rules for a track agent

- Branch `wt/<track>` from `master`; worktree `/opt/dev/peekandpoke/ultra-wt/<track>`. Commit on the branch, never push.
- Edit your track's area and your task file only. Never the shared docs, never generated files.
- Build your own worktree only; the full suite only through the E5 slot; DB env from E2.
- No sub-agents. The coordinator runs each track's `/feature-review` gate (reviewers read the worktree); the track
  agent applies the confirmed fixes. Gates are staggered so no more than ~3 heavy agents run at once.
- Rebase onto `master` when told that another track has landed.

## Merge protocol — coordinator

1. Track gate PASS, module tests green in its worktree.
2. Rebase the branch onto `master` in its worktree; re-run its module tests.
3. On `master` in the main worktree, holding `BUILD-LOCK.md`: `git merge --no-ff wt/<track>`, full suite, docs-site
   build if docs changed. Then archive the task and add the docs follow-up if public API changed.
4. Tell the other tracks to rebase.

## Decisions needed (each one only blocks its own track)

- **D1** (T4) Kraft insights client: endpoint declarations + DTOs in commonMain, the `GlobalLocksApiClient` pattern
  (recommended), or a hand-written Kraft client.
- **D2** (T4) Graphs in Kraft: a vis-network Kraft addon, or no graphs in the Kraft port for now.
- **D3** (T1) Vue component tests: **add vitest + happy-dom via pnpm** (maintainer 2026-10-19), versions looked up
  online and pinned.
- **D4** query-vars (`20260731-query-vars-in-insights.md`) — not blocking: both UIs keep not rendering `vars`.
- **D5** (T5) Error disclosure: choose A/B/C (ULTRA-00055 (exception disclosure architecture) §3 has
  a recommendation).
- **D6** (T6) SSO email verification policy A/B/C.
- **D7** (T7) Session revocation: the doc's five design questions.
- **D8** Branch `js-bundle-size`: **left unmerged** (maintainer 2026-10-19).
- **D9** (E3) **`distributeJsExample` moves out of `build`** (maintainer 2026-10-19).
