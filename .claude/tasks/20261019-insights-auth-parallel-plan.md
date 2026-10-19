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
- **Shared docs** — `CLAUDE.md`, skills, `v1-roadmap.md`, `CHANGELOG.md`, `BUILD-LOCK.md` — change on master only.

## Wave 0 — coordinator, on master, before any worktree starts

- [x] **E1** `auth-increments` merged into `master` (`f61c8731`, tree identical, local — not pushed).
- [ ] **E2** Per-worktree test databases: `forUnitTests` honours `ULTRA_TEST_ARANGO_DB` / `ULTRA_TEST_MONGO_DB`
      (defaults unchanged), the same for funktor-demo's `application.test.conf`; a setup script creates the Arango
      databases per worktree (Karango never creates a database).
- [ ] **E3** Generated files: take `distributeJsExample` out of `build` into an explicit task run at release (D9).
- [ ] **E4** (may run alongside Wave 1; touches only task docs no track owns) Task-doc housekeeping in one commit: archive what is done but still open (auth-hardening parts 1–4,
      account-activation backend, activation-resend, apiacl naming, value-class EmailAddress, ktor-client
      unification, …) and fix the stale claims the inventory found (TAB-SPECS `createdAt`/`isSystem`, the Kraft doc's
      "gate has not run", `insights.astro` documenting the deleted `InsightsGui`). Every worktree then starts from
      accurate docs, and nobody edits them in parallel.
- [ ] **E5** One full suite at a time across all worktrees: `flock /tmp/ultra-full-suite.lock ./gradlew clean build`.
      Module-level builds run freely in their own worktree.

## The tracks

| Track | Closes | Touches | Must not collide with |
|---|---|---|---|
| **T1 insights-vue** | `20260802-insights-vue-tabs.md` gate blockers (S1 at the source, I4, TAB-SPECS from a current record, D5/D6), then `20260824-insights-graphs-and-static-slices.md` Parts C+D (vis-network, tab buttons) | `funktor/codegen/src/main/resources/ts/{insights,ui}`, `InsightsTsContributor` + spec, `monko/core` (placeholder-ised query for S1), `funktor/insights/reference/TAB-SPECS.md`, `funktor-demo/sdkgen-app` | T2 only through the wire contract |
| **T2 insights-backend** | graphs task Parts A+B: the two graph endpoints, the slim kontainer slice (wire break, old records still render), e2e | `funktor/insights` (api, collectors, models), new spec files in `funktor/all` | T1 (contract), T4 (`InsightsApi.kt` — so T4 waits) |
| **T3 locks-heartbeat** | `20261019-global-locks-heartbeat.md` (agreed; draft code and tests exist) | `funktor/cluster/.../locks`, `funktor/inspect` lock model + list page, new e2e specs | nothing |
| **T4 insights-kraft** | `20260824-insights-kraft-gui.md` | `funktor/insights` commonMain (API + DTOs), new pages in `funktor/inspect/src/jsMain`, the two loose ends | T2 (`InsightsApi.kt`) → starts after T2 lands |
| **T5 auth-errors** | `error-disclosure/*` (generic production messages, account enumeration, env classification, fixtures gate) | `funktor/rest` (`ApiStatusPages`), `funktor/core` (`KtorConfig`, fixtures), `funktor/auth` (`AuthLoginApi`, `AuthUserApi`, `EmailAndPasswordAuth` messages) | password track (`EmailAndPasswordAuth.kt`) → that one goes after |
| **T6 auth-sso-verification** | `20260726-sso-email-verification.md` | `funktor/auth` SSO providers | T5 only if both touch `AuthLoginApi` |
| **T7 auth-sessions** | `20260728-session-revocation-wiring.md` | `funktor/auth` (`AuthRealm`, JWT claims, `SessionStore`) | T5/T6 in `AuthRealm` → after them |
| later | password hardening (after T5), JWT rotation operability (independent, small — a filler), CSP + token TTL, SDK auth Vue binding + SSE (after T1: same TS tree), route-check follow-ups | | |

**Wave 1** (two at a time, maintainer 2026-10-19) — T2 and T3 first (T3 is drafted and frees its slot soon;
T1 profits from T2's new kontainer shape landing first), then T1. **Wave 2** — T4 once T2 has landed, T5 as the first auth track (insights keep priority for
slots). Then T6, T7 and the rest as slots free up. Docs follow-ups land only after their code has settled.

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
- **D5** (T5) Error disclosure: choose A/B/C (`error-disclosure/20260720-exception-disclosure-architecture.md` §3 has
  a recommendation).
- **D6** (T6) SSO email verification policy A/B/C.
- **D7** (T7) Session revocation: the doc's five design questions.
- **D8** Branch `js-bundle-size`: **left unmerged** (maintainer 2026-10-19).
- **D9** (E3) **`distributeJsExample` moves out of `build`** (maintainer 2026-10-19).
