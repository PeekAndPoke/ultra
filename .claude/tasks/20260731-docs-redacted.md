# DOCS — `Redacted<T>` (and where `ultra:common` gets documented at all)

**Status:** TODO — created 2026-07-31 on archiving `.claude/tasks-archive/2026-07/20260731-redacted-and-jackson-removal.md`
**Plan:** `.claude/tasks-archive/2026-07/20260731-redacted-and-jackson-removal.md`
**Security-critical:** no (documentation), but it documents a security mechanism — accuracy matters more
than usual, because a reader who misunderstands the round-trip rule can create a real leak.

## Why this task exists

`Redacted<T>` is new **public API** in `ultra:common` (`io.peekandpoke.ultra.common.model`), plus a
built-in Slumber codec that is registered by default. Per CLAUDE.md, public-API changes get a docs
follow-up on archive. It is settled: it shipped, the config secrets already depend on it, and the JWT
work was built on top of it.

**Use a current example, not `JwtConfig.signingKey`.** That field is gone — commit `9fe2a21a`
replaced it with `JwtConfig.keys: List<JwtSigningKey>`, where the `Redacted<String>` now sits on
`JwtSigningKey.secret`. The type itself did not change; only where it is used did. Good live
examples to draw from: `ultra/security/src/commonMain/kotlin/jwt/JwtSigningKey.kt` (a scalar secret
inside a list element, which is also the interesting HOCON case) and
`funktor/messaging/.../SendgridConfig.kt` (`apiKey`). For the whole-subtree form, see
`AppConfig.keys: Map<String, Redacted<String>>` in `funktor/core/src/jvmMain/kotlin/config/AppConfig.kt`.

## The scoping problem to solve FIRST

**There is no `ultra:common` docs page and no `common.md` LLM mirror.** Current pages:
`cache, datetime, funktor, i18n, karango, kontainer, kraft, maths, monko, mutator, slumber, streams,
tools, vault` — `common` is absent from both `docs-site/src/pages/ultra/` and
`docs-site/src/data/llms/`.

So decide before writing:

1. **New `ultra/common` page** — correct home by module, but `ultra:common` is a grab-bag and a page for
   it invites documenting the whole grab-bag. Scope it deliberately if chosen.
2. **Put it on the `slumber` page** — wrong by module, but right by *use*: the type is inert without a
   codec, and the Slumber codec is what makes it work server-side. Risks implying it is a Slumber type.
3. **Both, one canonical** — full treatment on one page, a cross-reference from the other. Most likely
   the right answer; pick which is canonical rather than duplicating.

Note `.claude/tasks-archive/2026-07/20260731-slumber-as-declared-wire-shape.md` §6 settles that the whole `Slumber`
annotation nest moves to `ultra:common` (`io.peekandpoke.ultra.common.slumber`). If that lands first,
option 1 gets stronger — `ultra:common` would then hold the slumber-facing *contract*, which is worth a
page of its own.

## What the docs must say

- What it is: read in, never written out. Serializing always yields `Redacted.PLACEHOLDER`.
- **The round trip is broken on purpose**, and what that means in practice — a `Redacted<String>` that
  goes out and comes back holds the literal placeholder, while a `Redacted<SomeObject>` throws. This is
  the single most important thing on the page; a reader who misses it can restore a config whose signing
  key is a public constant.
- Wrapping a whole **subtree**, not just a leaf: `Redacted<AwsConfig>`.
- Reading the value: `.value`, and that doing so is unremarkable by design.
- **The rule that keeps it honest: adding a serializer to a project means adding a codec for this type**,
  or it falls through to generic object handling and emits `{"value": …}`.
- HOCON/config loading works — the awaker takes the RAW node and awakens `T` from it, so a config file
  writes `signingKey = "abc"`, not `signingKey { value = "abc" }`.

## What NOT to put on the docs page

Per the docs-vs-skills split in CLAUDE.md, these are "what will bite me" and belong in a skill (or stay
in the task/KDoc), not on the docs site:

- Why it is not a `value class` (both Slumber's value-class slumberer and Jackson inline past a custom
  codec — measured).
- Why it is not a `data class` (generated `toString`/`copy`/`component1` each leak).
- That it fails closed if the Slumber codec is removed.
- Slumber branch-ordering requirements.

The KDoc on `Redacted.kt` already carries all of this and is the right place for it.

## Deliverables

- [ ] Decide the page location (above).
- [ ] Docs page content.
- [ ] LLM mirror updated in `docs-site/src/data/llms/` — **the pair must move together**. If a new
      `common.md` is created, add it to `llms.txt` and the sidebar/nav per `.claude/skills/docs-site/`.
- [ ] `pnpm run build` from `docs-site/` — zero errors, page count checked.

## Related

- `.claude/tasks/20260731-redteam-redacted.md` — attack scenarios, collected not executed.
- `.claude/tasks/20260728-docs-sync-remaining.md` — existing docs-drift backlog; check for overlap
  before starting so this does not duplicate a page someone is already rewriting.
