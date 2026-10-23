# ULTRA-00238: Gradle version catalog instead of buildSrc Deps.kt (pilot)

> **Status (2026-10-23): DESIGNED.** Starts after the current work. Ultra pilots it; raise it with Mission Control as
> a cross-project topic afterwards, with what the pilot showed (maintainer, 2026-10-23: "Let us pilot it here. We can
> raise this to MC later").

## Why this exists

The maintainer, after ULTRA-00235 (kraft-examples) used one `gradle/libs.versions.toml`: "the libs.versions.toml seems
to be way more ergonomic than the buildSrc approach". Gains: Gradle's standard format with IntelliJ completion;
Renovate and Dependabot read it, so "is there a newer version?" can come from a tool; a version bump no longer
recompiles `buildSrc` and invalidates every build script's classpath; examples and apps share one layout.

Today: `buildSrc/src/main/kotlin/Deps.kt`, 626 lines, about 148 entries, used by 70 build scripts.

## Decide before implementing

- **npm dependencies** (`Deps.Npm`, the Kraft addons' `npm("chart.js", "4.5.1")`): a catalog holds Maven coordinates
  only. Proposal: their versions under `[versions]`, read as `npm("chart.js", libs.versions.chartjs.get())`.
- **Helpers with logic** (the `full(scope)` functions, `Deps.kt:174`, `:199`, `:222`, `:238`, `:329`): plain lists
  become `[bundles]`; mixed ones (platform, ksp) become explicit lines per module.
- **`buildSrc` stays**: `I18nPlugin`, `ExtractExampleCodePlugin` and `Docs.kt` are build logic, not versions. Only
  `Deps.kt` goes. Moving the plugins to `build-logic` is a separate question.
- Release skill and lockstep: `VERSION_NAME` stays in `gradle.properties`; the skills that name `Deps.kt`
  (`update-kraft-examples` step 1, the release skill) move to the catalog.

## The work

Catalog and `Deps.kt` coexist, so a module group at a time, each followed by a compile and its tests:

- [ ] `gradle/libs.versions.toml` with every entry of `Deps.kt`, versions checked unchanged (no bumps in this ticket).
- [ ] ultra/*, then mutator, karango, monko, then kraft (npm), then funktor and funktor-demo.
- [ ] Delete `Deps.kt`; skills updated.

## Acceptance

- [ ] No build script references `Deps`; `Deps.kt` deleted.
- [ ] Resolved dependency versions identical before and after (compare `dependencies` output for a sample of modules).
- [ ] Full suite green: `console/wt-gradle.sh --full-suite`.
- [ ] Notes for the MC topic: what moved cleanly, what didn't, the effort.

## History

- 2026-10-23: created (maintainer's go on a pilot).
