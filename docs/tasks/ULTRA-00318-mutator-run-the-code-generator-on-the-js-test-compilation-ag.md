# ULTRA-00318: Mutator: run the code generator on the JS test compilation again, once Kotest survives a second KSP round

> **Status (2026-11-13): PAUSED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

## Why this exists

ULTRA-00144 moved the Mutator specs to `mutator/core/src/sharedTest` so they run on JS. To get there, the mutator
KSP processor was taken off `kspJsTest`: next to Kotest's own processor it forces a second KSP round, and Kotest's
processor then fails in `finish()` with "PSI has changed since creation" (Kotest 6.2.6 and 6.2.7, KSP 2.3.12;
6.2.7 probed 2026-11-13). The JS tests compile the mutators the JVM run generates. So no JS compilation in the repo
runs the mutator processor. The maintainer (2026-11-13): "yes we accept this ... mutator is anyway not in use
anywhere at the moment".

## The work (when a newer Kotest is out)

- [ ] In `mutator/core/build.gradle.kts`: add `add("kspJsTest", project(":mutator:ksp"))` back and delete the
      `srcDir` of `jsTest` that points at the JVM run's output. Run `:mutator:core:clean :mutator:core:jsBrowserTest`.
- [ ] If it is green: the JS and JVM outputs are generated separately again; remove the fixture-package rule from
      the build comment and `mutator/core/CLAUDE.md`.
- [ ] The trigger is a Kotest bump: note this ticket next to `kotest_version` in `buildSrc/src/main/kotlin/Deps.kt`.

## History

- 2026-11-13: created on archiving ULTRA-00144; paused until a Kotest after 6.2.7.
