---
name: release
description: Use when someone asks to release a new version, cut a release, bump the version, publish to Maven Central, or tag a release. Covers the full repo-wide version bump and release workflow.
disable-model-invocation: true
argument-hint: [ target version, e.g. 0.107.0 or 1.0.0 ]
---

## What This Skill Does

Executes a full release for the PeekAndPoke monorepo. The canonical version lives in `gradle.properties` (
`VERSION_NAME`); a release additionally requires bumping the docs-site constants and the README dependency snippet,
verifying the docs site and a release commit. The maintainer runs the full build (with the example bundles); then the
agent commits the bundles and tags; then the maintainer publishes to Maven Central and pushes.

The operator (the human) must approve each destructive step explicitly. This skill prepares the changes — it does not
auto-publish or auto-push.

## Inputs

- `$1` — target version string, e.g. `0.107.0` (semver, no leading `v`). If omitted, ask the user.

## Preflight (run before anything else)

1. **Clean working tree.** Run `git status`. If there are uncommitted changes, stop and ask the user to commit or stash
   first — the release commit must contain only version-bump changes.
2. **On the right branch.** Confirm `git branch --show-current` is the release branch (usually `master`). If not, ask
   the user.
3. **Read the current version.** Grep `gradle.properties` for `VERSION_NAME=` and store as `$CURRENT`.
4. **Confirm the bump.** Show the user "`$CURRENT` → `$1`" and ask for go/no-go before editing files.

## Locations to bump (every release)

Every string below must move from `$CURRENT` to `$1`. Use the Edit tool, not sed, so each change is explicit:

1. **`gradle.properties`** — single line: `VERSION_NAME=$CURRENT` → `VERSION_NAME=$1`. This is the canonical source; all
   Gradle modules read from here.

2. **`docs-site/src/data/site.ts`** — two lines:
    - `export const ultraVersion = '$CURRENT';` → `'$1'`
    - `export const kraftVersion = '$CURRENT';` → `'$1'`
      These two constants drive:
    - Every `.astro` page's Maven dependency snippet (via `${ultraVersion}` / `${kraftVersion}` imports)
    - The `dep.*` map in the same file (consumed by Astro pages)
    - All LLM mirrors at `/llms.txt`, `/llms-full.txt`, `/llms/*.md` (served by endpoints under `src/pages/llms.txt.ts`,
      `src/pages/llms-full.txt.ts`, `src/pages/llms/[slug].md.ts` which read from `src/data/llms/` templates and
      substitute `{{ultraVersion}}` / `{{kraftVersion}}` at build time via `src/data/llmsTemplate.ts`)
      **You do NOT edit any file under `docs-site/public/` or `docs-site/src/data/llms/`** — they use placeholders and
      get rendered at build time.

3. **`README.MD`** — the Gradle dependency snippet near line ~123 has 7 hardcoded version strings (`mutator-core`,
   `mutator-ksp`, `kontainer`, `slumber`, `streams`, `karango-core`, `kraft:core`). Use `Edit` with `replace_all: true`
   on the old version string, scoped to this file.

4. **ULTRA-00028 (v1 roadmap)** (path: `console/tools/ticket/ticket.py find ULTRA-00028`) — header line `**Current version:** $CURRENT → **Target:** 1.0.0` and the
   `**Released:**` bullet if present.

5. **`CHANGELOG.md`** — the heading `## Unreleased` becomes `## $1`. If there is no `## Unreleased` section, stop and
   ask: a release without notes is the maintainer's call.

**Sanity check after edits:** run
`grep -rn "$CURRENT" gradle.properties docs-site/src/data/site.ts README.MD "$(console/tools/ticket/ticket.py find ULTRA-00028)"` and confirm
zero hits.

## Verification (blocks the release commit)

**No full Gradle build here.** The maintainer's publish build runs `clean build` with every test anyway (maintainer,
2026-10-24: "I anyway have to do the full build (which runs all tests) in order to get the artifacts for publishing").
That build is also where the Kraft example bundles get refreshed (see Build and publish).

1. **Docs-site build.** `cd docs-site && pnpm astro build`. As user claude, add `--outDir ../.build-claude/docs-site-dist`
   so the maintainer's `dist/` stays his. After building, verify the new version landed in the generated LLM files
   (`<dist>` is the output folder):
   ```
   grep "Version: $1" <dist>/llms.txt
   grep -c "$1" <dist>/llms-full.txt   # expect ≥ 12
   grep -c "$CURRENT" <dist>/llms.txt <dist>/llms-full.txt <dist>/llms/*.md
   ```
   The last grep must return all zeroes — no stale version strings.

2. **funktor-demo smoke** (optional but recommended for major/minor releases): `./gradlew :funktor-demo:server:run`
   against both ArangoDB and MongoDB configurations. Skip for patch releases unless the user asks.

If any step fails, stop. Report the failure and wait for the user.

## Release commit (no tag yet)

Once verification passes:

1. `git diff` — show the bump diff to the user for final review.
2. `git add` the bumped files by name, then `git commit -m "Release $1"` — single atomic commit, version-bump only.
3. **No tag yet, and do NOT push.** The tag comes after the maintainer's build (maintainer, 2026-10-26): a tag made
   before it would point at a commit nobody has built, and at the previous release's example bundles.

## Build, bundles, tag, publish

Prerequisites (one-time per dev machine): Maven Central credentials in `~/.gradle/gradle.properties` —
`mavenCentralUsername`, `mavenCentralPassword`, `signing.password`. See `publish.md` at the repo root.

**The maintainer builds and publishes, not the agent.** Publishing to Maven Central is **irreversible** — artifacts
cannot be overwritten.

1. **The maintainer:** `./gradlew clean build distributeJsExample` — every test, and the Kraft example bundles in
   `docs/kraft/examples/`. `distributeJsExample` is NOT part of `build` (since 2026-10-19), so it is named here. Must
   be green. If it fails: fix it in follow-up commits on top of the release commit (never amend), and the maintainer
   builds again.
2. **The agent, once told the build is green:** commit the refreshed bundles on their own,
   `build files: Kraft examples rebuilt for $1` (skip if `git status` shows none). **Agents never run
   `distributeJsExample` themselves:** it writes tracked files, and files written by user claude break the
   maintainer's next run ("Could not set file mode"; MC-00031, open until the maintainer decides).
3. **The agent:** `git tag -a v$1 -m "Release $1"` on that last commit (the bundles commit, else the release commit).
4. **The maintainer:** `./gradlew publishAllPublicationsToMavenCentralRepository` — publishes all 5 library roots
   (ultra, kraft, karango, monko, funktor) — then `git push && git push --tags`.
5. Once told it is published, verify appearance at https://central.sonatype.com/namespace/io.peekandpoke.ultra (may
   take 10–30 min to index).

## Post-publish

Once the new version is live on Maven Central, the Kraft examples repo (`kraft-examples`, a separate repo, not in
this monorepo) needs its versions bumped. Use the `update-kraft-examples` skill — it handles it end-to-end.

## Shipping Checklist (mirrors ULTRA-00028 (v1 roadmap))

Run through this before publishing any v1.x / major release. For patch releases (0.x.y → 0.x.(y+1) where only bug fixes
landed), skip items marked *(major/minor only)*.

- [ ] Clean working tree on the release branch
- [ ] All CRITICAL/HIGH audit findings resolved — check `docs/tasks-archive/**/*issues*.md` *(major/minor only)*
- [ ] Every shippable module has test coverage ≥25% *(major/minor only)*
- [ ] Zero unresolved TODOs without `// TODO(post-v1)` justification *(major/minor only)*
- [ ] Every public API has KDoc *(major only)*
- [ ] `cd docs-site && pnpm astro build` green; LLM mirrors show the new version
- [ ] The maintainer's `./gradlew clean build distributeJsExample` green, no warnings; bundles committed on their own
- [ ] Every library has a README
- [ ] CHANGELOG.md updated with release notes *(major/minor only)*
- [ ] Root README points at the new version
- [ ] `gradle.properties`, `docs-site/src/data/site.ts`, `README.MD`, ULTRA-00028 (v1 roadmap) all bumped
- [ ] funktor-demo runs end-to-end on both ArangoDB and MongoDB *(major/minor only)*
- [ ] `CHANGELOG.md`: `## Unreleased` → `## $1`
- [ ] Release commit created (no tag yet, not pushed); handed to the maintainer for the build
- [ ] After the green build: bundles committed, annotated tag `v$1` on that commit
- [ ] Handed to the maintainer for `publishAllPublicationsToMavenCentralRepository` and `git push && git push --tags`

## Output Format

At the end of a successful release, report to the user:

```
## Release $1 ready

**Bumped:**
- gradle.properties: VERSION_NAME=$1
- docs-site/src/data/site.ts: ultraVersion, kraftVersion
- README.MD: 7 dependency strings
- ULTRA-00028 (v1 roadmap): header
- CHANGELOG.md: ## $1

**Verified:**
- docs-site astro build — green, LLM mirrors carry $1

**Committed:**
- Release commit: <short sha> (no tag yet)

**Next:**
1. Yours: ./gradlew clean build distributeJsExample — tell me when it is green
2. Mine: commit the refreshed bundles, tag v$1
3. Yours: ./gradlew publishAllPublicationsToMavenCentralRepository, then git push && git push --tags
```

## Guardrails — what NOT to do

- **Never publish.** The maintainer (2026-10-18): "Let me do the publishing please." Prepare the release; the
  maintainer runs `publishAllPublicationsToMavenCentralRepository`. Publishing to Maven Central is irreversible.
- **Never push.** The maintainer (2026-10-19): "You merge all things that are done into master and i push."
- **Never edit `docs-site/public/`** for version bumps — that directory is no longer authoritative for LLM mirrors.
  Everything routes through `src/data/site.ts`.
- **Never edit `docs-site/src/data/llms/*`** to change versions — those templates use `{{ultraVersion}}` /
  `{{kraftVersion}}` placeholders on purpose. Only edit them to update actual content.
- **Never skip the docs-site verification.** A stale version string in the LLM mirrors means something references
  the old version in a way you didn't anticipate. The full Gradle build is the maintainer's, before publishing.
- **Don't amend the release commit** — create a follow-up commit if changes are needed; the tag goes on the last one.
- **Don't use `--no-verify` or skip signing** — Maven Central requires signed artifacts.

## Notes

- Gradle module versions all derive from `VERSION_NAME` in `gradle.properties` — there's no per-module version file to
  update.
- The `dep.*` map in `docs-site/src/data/site.ts` also uses `${ultraVersion}` / `${kraftVersion}` — no separate
  maintenance needed there.
- Group coordinates (`ULTRA_GROUP`, `KRAFT_GROUP`, etc.) in `gradle.properties` and `site.ts` (`ultraGroup`,
  `kraftGroup`) should not change during a normal release. Only touch them for a rename/fork.
- The llms-render machinery is documented at the top of `docs-site/src/data/llmsTemplate.ts` — read that if anything
  about the LLM mirror rendering is unclear.
