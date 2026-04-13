---
name: release
description: Use when someone asks to release a new version, cut a release, bump the version, publish to Maven Central, or tag a release. Covers the full repo-wide version bump and release workflow.
disable-model-invocation: true
argument-hint: [ target version, e.g. 0.107.0 or 1.0.0 ]
---

## What This Skill Does

Executes a full release for the PeekAndPoke monorepo. The canonical version lives in `gradle.properties` (
`VERSION_NAME`); a release additionally requires bumping the docs-site constants and the README dependency snippet,
verifying the build, and optionally publishing to Maven Central + tagging.

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

4. **`.claude/plans/v1-roadmap.md`** — header line `**Current version:** $CURRENT → **Target:** 1.0.0` and the
   `**Released:**` bullet if present.

**Sanity check after edits:** run
`grep -rn "$CURRENT" gradle.properties docs-site/src/data/site.ts README.MD .claude/plans/v1-roadmap.md` and confirm
zero hits.

## Verification (mandatory — blocks publish)

1. **Full Gradle build.** `./gradlew clean build` — runs across all ~79 modules. Must be green. No warnings count as
   green per the Shipping Checklist.

2. **Docs-site build.** `cd docs-site && pnpm astro build`. After building, verify the new version landed in the
   generated LLM files:
   ```
   grep "Version: $1" docs-site/dist/llms.txt
   grep -c "$1" docs-site/dist/llms-full.txt   # expect ≥ 12
   grep -c "$CURRENT" docs-site/dist/llms.txt docs-site/dist/llms-full.txt docs-site/dist/llms/*.md
   ```
   The last grep must return all zeroes — no stale version strings.

3. **funktor-demo smoke** (optional but recommended for major/minor releases): `./gradlew :funktor-demo:server:run`
   against both ArangoDB and MongoDB configurations. Skip for patch releases unless the user asks.

If any step fails, stop. Do NOT proceed to publish. Report the failure and wait for the user.

## Commit + tag

Once verification passes:

1. `git diff` — show the bump diff to the user for final review.
2. `git add -A && git commit -m "Release $1"` — single atomic commit, version-bump only.
3. `git tag -a v$1 -m "Release $1"` — annotated tag.
4. **Do NOT push** yet. Ask the user to confirm before `git push && git push --tags`.

## Publish to Maven Central

Prerequisites (one-time per dev machine): Maven Central credentials in `~/.gradle/gradle.properties` —
`mavenCentralUsername`, `mavenCentralPassword`, `signing.password`. See `publish.md` at the repo root.

1. Ask the user for explicit go-ahead before publishing. Publishing to Maven Central is **irreversible** — artifacts
   cannot be overwritten.
2. `./gradlew publishAllPublicationsToMavenCentralRepository` — publishes all 5 library roots (ultra, kraft, karango,
   monko, funktor).
3. Verify appearance at https://central.sonatype.com/namespace/io.peekandpoke.ultra (may take 10–30 min to index).

## Post-publish

Once the new version is live on Maven Central, the three external Kraft example projects (`kraft-example-helloworld`,
`kraft-example-router`, `kraft-example-remote`) need their dependency versions bumped. That's a separate repo, not in
this monorepo. Use the `update-kraft-examples` skill — it handles those projects end-to-end.

## Shipping Checklist (mirrors `.claude/plans/v1-roadmap.md`)

Run through this before publishing any v1.x / major release. For patch releases (0.x.y → 0.x.(y+1) where only bug fixes
landed), skip items marked *(major/minor only)*.

- [ ] Clean working tree on the release branch
- [ ] All CRITICAL/HIGH audit findings resolved — check `.claude/plans-archive/*issues*.md` *(major/minor only)*
- [ ] Every shippable module has test coverage ≥25% *(major/minor only)*
- [ ] Zero unresolved TODOs without `// TODO(post-v1)` justification *(major/minor only)*
- [ ] Every public API has KDoc *(major only)*
- [ ] `./gradlew clean build` green, no warnings
- [ ] `cd docs-site && pnpm astro build` green; LLM mirrors show the new version
- [ ] Every library has a README
- [ ] CHANGELOG.md updated with release notes *(major/minor only)*
- [ ] Root README points at the new version
- [ ] `gradle.properties`, `docs-site/src/data/site.ts`, `README.MD`, `.claude/plans/v1-roadmap.md` all bumped
- [ ] funktor-demo runs end-to-end on both ArangoDB and MongoDB *(major/minor only)*
- [ ] Release commit + annotated git tag created (not yet pushed)
- [ ] User has given explicit go-ahead for `git push --tags`
- [ ] User has given explicit go-ahead for `publishAllPublicationsToMavenCentralRepository`

## Output Format

At the end of a successful release, report to the user:

```
## Release $1 ready

**Bumped:**
- gradle.properties: VERSION_NAME=$1
- docs-site/src/data/site.ts: ultraVersion, kraftVersion
- README.MD: 7 dependency strings
- .claude/plans/v1-roadmap.md: header

**Verified:**
- ./gradlew clean build — green
- docs-site astro build — green, LLM mirrors carry $1

**Committed:**
- Release commit: <short sha>
- Annotated tag: v$1

**Next (needs your go-ahead):**
1. git push && git push --tags
2. ./gradlew publishAllPublicationsToMavenCentralRepository
```

## Guardrails — what NOT to do

- **Never push or publish without explicit user confirmation.** Publishing to Maven Central is irreversible.
- **Never edit `docs-site/public/`** for version bumps — that directory is no longer authoritative for LLM mirrors.
  Everything routes through `src/data/site.ts`.
- **Never edit `docs-site/src/data/llms/*`** to change versions — those templates use `{{ultraVersion}}` /
  `{{kraftVersion}}` placeholders on purpose. Only edit them to update actual content.
- **Never skip the build verification step.** A failing build after version bump means something references the old
  version in a way you didn't anticipate.
- **Don't amend the release commit** once the tag exists — create a follow-up commit if changes are needed.
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
