# Ship shareable Claude skills alongside the framework (Kraft, ultra, funktor)

**Status:** IDEA — not yet scheduled (captured 2026-07-23, user request)
**Type:** developer-experience / distribution

## The idea

Ship Claude **skills** as part of the framework packages (Kraft, ultra, AND funktor), so that any
project depending on them gets framework-aware Claude assistance + review heuristics out of the box —
the same way we ship code + docs. A skill encodes the framework's idioms and safety practices, so
Claude (and especially `/feature-review`) can flag deviations.

## Why it's valuable

Some correctness/security properties can't be fully enforced structurally, only nudged. A skill
turns "good practice" into a review heuristic Claude applies automatically. Concrete example from the
auth-hardening work:

- A skill can detect that an **org-owned entity should implement `OrgAware`** (it has an
  `org`/`orgId`/`Ref<Organisation>` field but forgot the interface), or that a **user-owned entity
  should implement `UserAware`** (orders, payments, profile) — the exact gap the boot check CANNOT
  catch (an unmarked entity). Not a 100% guarantee, but a strong good-practice net.
- Complements the structural backstops: the boot check + the entity-`OrgAware` **linter**
  (`auth-hardening/20260722-route-check-followups.md` item 2) enforce at build time; the **skill**
  aids at authoring/review time. Different failure surfaces, both wanted.

More broadly, skills can teach the current idioms so reviews stop accepting stale patterns: the
block-style `authorize {}` DSL, the mandatory `ApiRoutes` floor, two-phase auth, `RouteBootCheck`/
`RouteParamsGuard`, Kraft component/state conventions, ultra vault `Ref`/`Stored` usage, etc.

## Sketch (to think through when scheduled)

- **Packaging:** how does a skill travel with a published library? A `skills/` dir in the artifact,
  a convention Claude Code discovers from a dependency, or a docs-site-hosted skill bundle. Decide
  the distribution mechanism.
- **Scope per package:** `kraft` (UI/component/state review), `ultra` (vault/kontainer/slumber
  idioms), `funktor` (auth DSL + floor + org/user isolation + REST route conventions).
- **Review integration:** feed these into `/feature-review` so the domain/security reviewers apply
  framework-specific heuristics (e.g. "flag an entity with an org field that isn't `OrgAware`").
- **Versioning:** the skill must track the framework version (idioms change — cf. the docs collector
  that had to catch stale `or`/`and` DSL examples).

## Cross-references

- `auth-hardening/20260722-route-check-followups.md` (item 2: the entity-`OrgAware` linter — the
  structural counterpart) and item 4 (`UserAware` isolation).
- The docs collectors (`docs-site/src/data/llms/*`) — the LLM-readable mirrors are the closest
  existing thing; a skill is the interactive/review-time complement.
