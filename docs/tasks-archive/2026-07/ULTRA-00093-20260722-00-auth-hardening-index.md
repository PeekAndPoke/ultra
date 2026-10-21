# ULTRA-00093: Auth-hardening quartet — index

**Status:** DONE (quartet complete 2026-07-23; archived 2026-10-19). Parts 1–4 are all DONE and archived next to this file in
`docs/tasks-archive/2026-07/`. The follow-ups in the last four table rows are NOT done and stay open as ordinary tasks in
`docs/tasks/` — they no longer have an umbrella.
**Plan:** none — this is the umbrella/index for the quartet
**Security-critical:** yes

Structural default-deny for the REST auth layer, done as an ordered chain. Each part has a hard
dependency on the previous one, so land them in order.

## The parts, in order

| # | Task | What it does | Status |
|---|------|--------------|--------|
| 1 | ULTRA-00088 (authorize rule builder) | `authorize {}` becomes a builder-accumulator — kills the last-expression-wins footgun where only the final rule expression counted | **DONE** (2026-07-22, review loop ended round 4, zero findings) |
| 2 | ULTRA-00087 (apiroutes auth floor) | Mandatory `authFloor` floor on `ApiRoutes` (renamed from `defaultAuth`, part 4) — structural default-deny; a route with no rule is denied, not public | **DONE** (2026-07-22, review loop ended round 3, zero findings) |
| 3 | ULTRA-00090 (two phase auth consistent params) | Two-phase auth eval + pluggable `RouteBootCheck`/`RouteParamsGuard` + saas org-isolation (`OrgAware`/`OrgAwareParam`) — no entity loads before auth passes, structural cross-org isolation | **DONE** (2026-07-23, review loop: round-1 reworked on 2 HIGH direction findings, round-2 confirmed mechanism airtight, edge findings fixed) |
| 4 | ULTRA-00089 (stored param migration) | Migrate framework APIs from in-handler `findById` to `Stored` params + entity-based param naming + `defaultAuth`→`authFloor` rename | **DONE** (2026-07-23, review loop ended round 3, zero findings) |
| — | ULTRA-00091 (docs auth dsl and floor) | Docs follow-up (collector) for the DSL rewrite + floor + part-3 checks/guards. Write the prose after 1–4 land | **PARTIAL** (checked 2026-10-19: floor + DSL are documented, two-phase eval / `RouteBootCheck` / `ConsistentParam` / `Stored` params are not) |
| — | ULTRA-00094 (route check followups) | Part-3 spin-offs: `validateUriPattern`→`RouteBootCheck`, entity-`OrgAware` linter, branch-level isolation | **TODO** |
| — | `20260723-security-open-questions.md` | Open design questions on owner-in-URI, body-embedded ids, cross-org operations | **OPEN QUESTIONS** (not in this index until 2026-10-19) |
| — | ULTRA-00095 (redteam org isolation) | Red-team attack scenarios for the org-isolation (security-critical follow-up) | **TODO** (collected, not executed) |

Dependency chain: **1 → 2 → 3 → 4**, docs last — the four parts are done; only the follow-up rows above remain.

## Shared review protocol (user directive, 2026-07-22 — applies to all four)

Deep review **LOOP**, not a single pass:

1. Full 3-agent gate (impl+style, domain, security — all `opus`) on the complete diff; every finding
   adversarially verified by the coordinator before acceptance.
2. Fix ALL confirmed findings — no severity threshold; confirmed LOWs get fixed too.
3. Re-run the FULL gate on the updated diff (fresh reviewers, whole diff — not just the fix delta).
4. Repeat 2–3 until a round produces ZERO confirmed findings. **One pass is not enough.**
5. **ESCALATION:** if a round surfaces something fundamentally wrong with the agreed *direction* —
   not a fixable finding but "the design judgement itself was wrong" — STOP and consult the user on
   direction before continuing.

Full protocol text lives in ULTRA-00088 (authorize rule builder).

## History

- 2026-10-21: numbered ULTRA-00093 (was `docs/tasks-archive/2026-07/20260722-00-auth-hardening-index.md`).
