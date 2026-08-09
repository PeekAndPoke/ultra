# Group generated API clients under `api/`

**Status:** DONE — 2026-08-09. `/feature-review` not run; see the review record.
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md`
**Security-critical:** no — emitted layout only, no change to what is served or who may call it.

## Why

Maintainer, 2026-08-09. The SDK root already groups by KIND — `runtime/`, `ui/`, `insights/` — and
client files were the one thing left loose in it. At ten features that is untidy; at a hundred it
buries `models.ts`, `mount.ts` and `styles.ts` among them.

Root before: 10 loose `*Client.ts` + 5 files + 3 dirs.
Root after: `api/ insights/ runtime/ ui/` + `models.ts index.ts mount.ts styles.ts css-modules.d.ts`.

## What made it more than a rename

**Every generated client imported root-relative** — `./models.ts`, `./runtime/client.ts`,
`./runtime/route.ts`, `./runtime/sse.ts`, and each claimed type's contributor-declared `importFrom`.
Moving a client one directory down makes all five resolve to files that do not exist.

`TsModulePaths.rootRelative(specifier, fromFile)` is the single adjustment point, applied to all of
them in `TsClientEmitter`. Derived from the emitted file's own path, so a future `v2/` or per-feature
directory is a changed path rather than a hunt for hardcoded `./`.

**This is arithmetic on a path, not a source transform.** The generator builds its own import lines
from a module string plus a name list, so the specifier is DATA. That is why it is sound here while
rewriting `@sdk/…` inside hand-written `.vue` source was rejected as unsound. A path alias would
remove the need entirely and is still the long-term answer.

## The fixture was the whole risk

`ts-verify` passed on the FIRST run — and proved nothing, because `TsFixtureGenerator` wrote its
client at the SDK root. The depth rewrite was completely unexercised by the real compiler.

Moved the fixture to `api/fxDemoClient.ts`, and only then was the mutation meaningful: disabling
`rootRelative` produces **TS2307 on all five specifiers**, including `./runtime/datetime.ts` — which
is the claimed-type `importFrom` path, so the contributor-declared case is covered too.

Third time this exact trap has bitten this feature (`ApiAccessLevel` barrel collision, the CSS
`css-modules.d.ts` case, now this): **a guard is only as good as the fixture that exercises it, and
a green first run on a new code path is the signal to check the fixture, not to move on.**

## Decisions

- **`models.ts` stays at root.** It is not a client, and `insights/` components import `../models.ts`.
- **`TsClientNames.clientFile` owns the directory** (`api/…`), so every consumer follows from one
  place — including `InsightsTsContributor`'s check that the feature name still yields the file its
  pages import.
- Three insights components changed: `../funktorInsightsClient.ts` → `../api/funktorInsightsClient.ts`.

## Test evidence

- [x] `TsModulePathsSpec` — depth 0/1/N, bare specifiers untouched, already-climbing untouched,
      backslash separators
- [x] ts-verify compiles a REAL client at depth 1 against a real `tsc`
- [x] Mutation: rewrite disabled → unit spec red AND ts-verify red with TS2307 ×5
- [x] `:ultra:codegen:check :funktor:codegen:test :funktor:rest:jvmTest` — 310 / 72 / 116, 0 failures
- [x] Compile sweep clean
- [x] Demo app regenerated (49 files), `vue-tsc` clean, **and `vite build` succeeds** — 167 modules,
      `InsightsPage` still emitted as its own lazy chunk, CSS bundled. Per the standing rule that a
      green typecheck is not proof an SDK is consumable.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |
