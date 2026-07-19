# Fixtures must only install on recognised development environments

**Status:** TODO
**Plan:** none — from `.claude/tasks/20260720-error-response-disclosure-audit.md` (finding 4)
**Security-critical:** yes

## Spec

`funktor/core/src/jvmMain/kotlin/fixtures/fixtures_module.kt:16-26`:

```kotlin
if (config.ktor.isProduction) {
    singleton(FixtureInstaller::class, NullFixtureInstaller::class)
} else {
    singleton(FixtureInstaller::class, SimpleFixtureInstaller::class)
    dynamic(InstallFixturesCliCommand::class)
    dynamic(ListFixturesCliCommand::class)
}
```

Because `isProduction` is an allow-list of exactly `live`/`prod`/`production`, **any** unrecognised
environment string takes the `else` branch: `staging`, `prd`, `production-eu`, or an unresolved
`${ENV}` from a templating bug. On such a host, `SimpleFixtureInstaller` and
`InstallFixturesCliCommand` are registered, so seed fixtures become loadable.

The consequence here is worse than the disclosure findings: fixtures can overwrite or wipe real
data. Invert the gate so only explicitly-recognised development environments get fixtures.

- [ ] Gate reads `if (config.ktor.isDevelopment) { install } else { NullFixtureInstaller }`
- [ ] An unrecognised environment string gets `NullFixtureInstaller` and no CLI commands
- [ ] `InstallFixturesCliCommand` / `ListFixturesCliCommand` are registered only on the same
      allow-listed environments
- [ ] Verify no other module gates behaviour on `!isProduction` in the destructive direction

## Implementation notes

- Depends on / pairs with `.claude/tasks/20260720-env-classification-allowlist.md`. That task makes
  `isDevelopment` itself trustworthy; this task changes which flag is consulted. Either can land
  first, but both are needed for the gate to be sound.
- `IntrospectionApi.getFixtures` (`funktor/inspect/.../IntrospectionApi.kt`) enumerates fixtures and
  is superuser-gated, so it is not an additional exposure — but it will start returning empty on
  affected hosts once this lands, which is the intended outcome.
- Consider whether `FixtureInstaller` should also hard-refuse at call time when the environment is
  not development, so a mis-registration cannot be exploited later. Defence in depth.

## Test evidence

- [ ] Kontainer test: build with `environment = "prod"` / `"staging"` / `"${ENV}"` / `""` and assert
      `FixtureInstaller` resolves to `NullFixtureInstaller` and the CLI commands are absent
- [ ] Kontainer test: `environment = "dev"` / `"test"` / `"qa"` resolves to `SimpleFixtureInstaller`
- [ ] End-to-end: not required (module wiring only, no storage touched by the gate itself)
- [ ] Full test command(s) run + green: `./gradlew :funktor:core:jvmTest`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
