# Auth-hardening quartet — index

**Status:** IN PROGRESS — parts 1–2 DONE, parts 3–4 + docs TODO (as of 2026-07-22)
**Plan:** none — this is the umbrella/index for the quartet
**Security-critical:** yes

Structural default-deny for the REST auth layer, done as an ordered chain. Each part has a hard
dependency on the previous one, so land them in order.

## The parts, in order

| # | Task | What it does | Status |
|---|------|--------------|--------|
| 1 | `20260722-authorize-rule-builder.md` | `authorize {}` becomes a builder-accumulator — kills the last-expression-wins footgun where only the final rule expression counted | **DONE** (2026-07-22, review loop ended round 4, zero findings) |
| 2 | `20260722-apiroutes-auth-floor.md` | Mandatory `authFloor` floor on `ApiRoutes` (renamed from `defaultAuth`, part 4) — structural default-deny; a route with no rule is denied, not public | **DONE** (2026-07-22, review loop ended round 3, zero findings) |
| 3 | `20260722-two-phase-auth-consistent-params.md` | Two-phase auth eval + pluggable `RouteBootCheck`/`RouteParamsGuard` + saas org-isolation (`OrgAware`/`OrgAwareParam`) — no entity loads before auth passes, structural cross-org isolation | **DONE** (2026-07-23, review loop: round-1 reworked on 2 HIGH direction findings, round-2 confirmed mechanism airtight, edge findings fixed) |
| 4 | `20260722-stored-param-migration.md` | Migrate framework APIs from in-handler `findById` to `Stored` params. Hard dependency on part 3 — without it the IDOR oracle goes live | **TODO** (designed + agreed) — UNBLOCKED |
| — | `20260722-docs-auth-dsl-and-floor.md` | Docs follow-up (collector) for the DSL rewrite + floor + part-3 checks/guards. Write the prose after 1–4 land | **TODO** (collector) |
| — | `20260722-route-check-followups.md` | Part-3 spin-offs: `validateUriPattern`→`RouteBootCheck`, entity-`OrgAware` linter, branch-level isolation | **TODO** |
| — | `20260723-redteam-org-isolation.md` | Red-team attack scenarios for the org-isolation (security-critical follow-up) | **TODO** (collected, not executed) |

Dependency chain: **1 → 2 → 3 → 4**, docs last. Part 3 done → part 4 unblocked.

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

Full protocol text lives in `20260722-authorize-rule-builder.md`.
