# ULTRA-00235: Unified kraft examples repo

> **Status (2026-10-23): IN PROGRESS.** Built and committed locally; waits for the maintainer to create the GitHub repo and push.

## Why this exists

The maintainer (2026-10-23): "how about we combine the kraft examples into one repo with different launch commands.
This make maintainance easier and is also open for more examples in the future without the need for a new repo."

Today there are three repos (`kraft-example-helloworld`, `kraft-example-router`, `kraft-example-remote`), each with
its own copy of `buildSrc`, the wrapper and `gradle.properties` (each also has both `README.md` and `README.MD`). Every
release bumps all three (`.claude/skills/update-kraft-examples/`). They exist to prove the PUBLISHED Maven artifacts
work; `kraft/examples/` in this repo builds against source and cannot show that.

## Decisions (maintainer, 2026-10-23)

- Repo name: `kraft-examples`. The maintainer creates it on GitHub.
- Each example stays self-contained (no dependency on a sibling).
- The maintainer archives the old three repos once the new one is online.

## The work

- One Gradle build: subprojects `helloworld/`, `router/`, `remote/`; one shared `buildSrc`; the Kraft/Ultra version
  in one place.
- Launch per example, e.g. `./gradlew :router:jsBrowserDevelopmentRun --continuous`; the README lists one command
  per example and how to add a new one (a folder plus one line in `settings.gradle.kts`).
- Built locally next to the old repos; never pushed (the maintainer pushes).
- Links: `docs-site/src/data/site.ts:27-29`, `docs-site/src/data/llms/kraft.md:91-95`.
- `.claude/skills/update-kraft-examples/` rewritten for one repo; the release skill's post-publish step follows.

## Implementation notes

- `/opt/dev/peekandpoke/kraft-examples`, commit c8105c1: the three sources at their 0.114.0 commits, one Gradle build,
  versions in `gradle/libs.versions.toml` (replaces the copied `buildSrc/Deps.kt`, most of which no example used).
  Plugins declared once in the root `build.gradle.kts` (`apply false`); each example keeps its own repositories,
  dependencies and dev-server port (55000, 55001, 55002).
- Dropped: the remote example's `mavenLocal()` (the examples prove the published artifacts), the duplicate
  `README.MD` files, and `.idea` (the README lists the run commands).

## Acceptance

- [x] Each example builds from the combined repo against the current published version (`./gradlew build` green, 0.114.0). Not opened in a browser.
- [x] One version bump updates all examples (`gradle/libs.versions.toml`).
- [x] Docs-site links and the LLM mirror point to the new repo; `pnpm astro build` green (114 pages, no old link in `dist`).
- [x] `update-kraft-examples` covers the combined repo only; `/release` points to it.
- [x] Old repos' pointer READMEs committed locally (helloworld 3abd046, router 53615ef, remote b28b047) for the maintainer to push before archiving.
- [ ] The maintainer creates `PeekAndPoke/kraft-examples` on GitHub and pushes `/opt/dev/peekandpoke/kraft-examples` (c8105c1).

## History

- 2026-10-23: created.
- 2026-10-23: the maintainer's three decisions; started.
- 2026-10-23: IN PROGRESS.
- 2026-10-23: combined repo built and committed locally; docs links, LLM mirror and skills updated.
