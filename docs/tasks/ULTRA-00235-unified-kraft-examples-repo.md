# ULTRA-00235: Unified kraft examples repo

> **Status (2026-10-23): NOT STARTED.** Scheduled right after the Rules Retro work items (maintainer, 2026-10-23).

## Why this exists

The maintainer (2026-10-23): "how about we combine the kraft examples into one repo with different launch commands.
This make maintainance easier and is also open for more examples in the future without the need for a new repo."

Today there are three repos (`kraft-example-helloworld`, `kraft-example-router`, `kraft-example-remote`), each with
its own copy of `buildSrc`, the wrapper and `gradle.properties` (each also has both `README.md` and `README.MD`). Every
release bumps all three (`.claude/skills/update-kraft-examples/`). They exist to prove the PUBLISHED Maven artifacts
work; `kraft/examples/` in this repo builds against source and cannot show that.

## Decide before implementing

- Repo name (proposal: `kraft-examples`) — the maintainer creates it on GitHub.
- The old three: archive on GitHub with a one-line README pointing to the new repo (proposal), not delete; outside
  links exist.
- Each example stays self-contained (no dependency on a sibling), so `cp -r router my-app` plus a small
  `settings.gradle.kts` still gives a starter. Yes / no.

## The work

- One Gradle build: subprojects `helloworld/`, `router/`, `remote/`; one shared `buildSrc`; the Kraft/Ultra version
  in one place.
- Launch per example, e.g. `./gradlew :router:jsBrowserDevelopmentRun --continuous`; the README lists one command
  per example and how to add a new one (a folder plus one line in `settings.gradle.kts`).
- Built locally next to the old repos; never pushed (the maintainer pushes).
- Links: `docs-site/src/data/site.ts:27-29`, `docs-site/src/data/llms/kraft.md:91-95`.
- `.claude/skills/update-kraft-examples/` rewritten for one repo; the release skill's post-publish step follows.

## Acceptance

- [ ] Each example builds and runs from the combined repo against the current published version.
- [ ] One version bump updates all examples.
- [ ] Docs-site links and the LLM mirror point to the new repo; `pnpm astro build` green.
- [ ] `update-kraft-examples` covers the combined repo only.
- [ ] Old repos' pointer READMEs prepared for the maintainer to push and archive.

## History

- 2026-10-23: created.
