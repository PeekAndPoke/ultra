# Fixtures must only install on recognised development environments

**Status:** TODO
**Plan:** none — from `.claude/tasks/20260720-error-response-disclosure-audit.md` (finding 4)
**Security-critical:** yes

## Spec

`funktor/core/src/jvmMain/kotlin/fixtures/fixtures_module.kt:14-27`:

```kotlin
internal val Funktor_Fixtures = module { config: AppConfig ->

    if (config.ktor.isProduction) {

        singleton(FixtureInstaller::class, NullFixtureInstaller::class)
    } else {

        singleton(FixtureInstaller::class, SimpleFixtureInstaller::class)

        // Cli commands
        dynamic(InstallFixturesCliCommand::class)
        dynamic(ListFixturesCliCommand::class)
    }
}
```

`config.ktor.isProduction` (`funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt:42`) is an
allow-list of exactly `live`/`prod`/`production`. Because the gate branches on it directly (not on
its negation `isNotProduction`), **any** unrecognised environment string — `staging`, `prd`,
`production-eu`, a trailing space, or an unresolved `${ENV}` from a templating bug — takes the
`else` branch: `SimpleFixtureInstaller` and both fixture CLI commands get registered.

- [ ] Gate reads `if (config.ktor.isDevelopment) { install } else { NullFixtureInstaller }`
- [ ] An unrecognised environment string gets `NullFixtureInstaller` and no CLI commands
- [ ] `InstallFixturesCliCommand` / `ListFixturesCliCommand` are registered only on the same
      allow-listed environments
- [ ] `SimpleFixtureInstaller` hard-refuses (throws) in `clear()`, `install()`, and
      `installSelected()` when `config.ktor.isDevelopment` is not true — defence in depth so a
      future mis-registration elsewhere can't silently run fixtures (see design below)
- [ ] Verify no other module gates destructive behaviour on `!isProduction` — confirmed: the only
      other consumers of `isNotProduction`/`!isProduction` in the repo are
      `funktor/rest/src/jvmMain/kotlin/respond.kt:57` and
      `funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt:33`, both disclosure-only (stack traces),
      not data-mutating. No other destructive gate exists today.

## What installing fixtures actually does

Read the whole subsystem (`funktor/core/src/jvmMain/kotlin/fixtures/`):

- `FixtureInstaller` (`FixtureInstaller.kt:100-127`) is the interface: `getLoaders()`, `clear()`,
  `install()`, `installSelected()`, and `clearAndInstall()` (default method: `clear()` then
  `install()`).
- `NullFixtureInstaller` (`NullFixtureInstaller.kt:8-31`) is a total no-op — empty loader list,
  empty results, on every method.
- `SimpleFixtureInstaller` (`SimpleFixtureInstaller.kt:10-94`) takes `allLoaders: List<FixtureLoader>`
  (kontainer auto-collects **every** `FixtureLoader` registered anywhere in the container — the
  same auto-resolution pattern used for `List<OnAppStarting>` etc., confirmed by
  `funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleKontainerWiringSpec.kt:11-14`). It sorts
  loaders by dependency (`Prioritizer`), then:
  - `clear()`: runs `prepare()` on every loader.
  - `install()`: runs `load()` then `finalize()` on every loader.
  - `clearAndInstall()`: `clear()` then `install()`.
- `RepoFixtureLoader<T>` (`RepoFixtureLoader.kt:170-172`) — the base class most concrete fixture
  loaders extend — implements `prepare()` as:
  ```kotlin
  override suspend fun prepare(result: FixtureLoader.MutableResult) {
      val removed = repo.removeAll()
      result.info("Cleared repo ${repo.name}. Removed ${removed.count} entries.")
  }
  ```
  i.e. **`prepare()` unconditionally wipes the entire backing collection**, not just fixture rows.
  `load()` (`RepoFixtureLoader.kt:175-206`) then batch/single-inserts the fixture definitions.
- `InstallFixturesCliCommand` (`cli/InstallFixturesCliCommand.kt:22-66`): calls
  `FixtureInstaller.clear()` once, then `clearAndInstall()` (which calls `clear()` again, then
  `install()`) — so `prepare()`/`removeAll()` runs on every registered repo before fixtures load.
  `ListFixturesCliCommand` (`cli/ListFixturesCliCommand.kt:17-27`) only prints `getLoaders()`, no
  mutation.

## Real blast radius — confirmed destructive, not insert-only

This is worse than "seed fixtures become loadable": fixture loaders in this codebase are
**unconditionally registered** in their owning modules, with no environment check of their own —
the *only* thing standing between them and `repo.removeAll()` is which `FixtureInstaller`
implementation `fixtures_module.kt` bound. Confirmed unconditional registrations:

| Loader | Wipes | Registered at |
|---|---|---|
| `KarangoAuthRecordsRepo.Fixtures` / `MonkoAuthRecordsRepo.Fixtures` (`RepoFixtureLoader<AuthRecord>`) | **all auth records** — every user's credentials/sessions across every realm | `funktor/auth/src/jvmMain/kotlin/index_jvm.kt:77,100` |
| `KarangoOrgsRepo.Fixtures` / `MonkoOrgsRepo.Fixtures` (`RepoFixtureLoader<Organisation>`) | **all organisations/tenants** | `funktor/saas/src/jvmMain/kotlin/index_jvm.kt:58,78` |
| `KarangoBackgroundJobsQueueRepo.Fixtures` / `MonkoBackgroundJobsQueueRepo.Fixtures` (`RepoFixtureLoader`) | queued background jobs | `funktor/cluster/src/jvmMain/kotlin/index_jvm.kt:192,304` |
| `MonkoGlobalLocksRepo.Fixtures` / `KarangoGlobalLocksRepo.Fixtures` (calls `repo.removeAll()` directly, e.g. `funktor/cluster/src/jvmMain/kotlin/locks/monko/MonkoGlobalLocksRepo.kt:16-19`) | cluster lock state | `index_jvm.kt:219,331` |
| `*ServerBeaconRepo.Fixtures` (same pattern) | cluster server-beacon state | `index_jvm.kt:236,348` |
| `SentMessagesFixtures` (`repo.clear()`) | sent-message log | `funktor/messaging/src/jvmMain/kotlin/index_jvm.kt:41` |
| `RandomDataStorageFixtures` / `RandomCacheStorageFixtures` (`storage.clear()`) | cache/random-data storage | `funktor/cluster/src/jvmMain/kotlin/index_jvm.kt:135,143` |
| `AdminUsersRepo.Fixtures`, `OperatorUsersRepo.Fixtures`, `EventsRepo.Fixtures`, `SpeakersRepo.Fixtures`, `AttendeesRepo.Fixtures` (`RepoFixtureLoader`) | demo-app domain data | `funktor-demo/server/src/main/kotlin/{admin,operator,funktorconf}/*.kt` |

So on any app that wires `funktor-auth` and/or `funktor-saas` (which this repo's own products do —
see the `ops-app`/`adminapp` work on this branch), an unrecognised-environment misfire of this gate
means `fixtures:install` **deletes every user account and every organisation** on that host, then
replaces them with a small canned dev dataset. This is not a hypothetical worst case — it is what
the code does today, confirmed by reading `RepoFixtureLoader.prepare()` and the unconditional
`Fixtures` registrations above. Severity: **high**, but see the trigger-path section below for the
precise (non-remote) attack surface this implies.

## How fixtures get triggered — CLI/test-harness only, not remote HTTP

- **CLI commands** (`InstallFixturesCliCommand`, `ListFixturesCliCommand`) are `CliktCommand`s run
  via `CliRunner` (`funktor/core/src/jvmMain/kotlin/cli/CliRunner.kt:65-76`), which is invoked from
  `CliRunner.create()` when the app process is launched with CLI args (`App` entry point, e.g.
  `funktor-demo/server/src/main/kotlin/main.kt:14` → `app.run(args)`; per the class doc comment
  "Executes CLI commands registered in the kontainer when the app is launched with `--cli`").
  Triggering `fixtures:install` requires **starting the JVM process with different arguments** —
  i.e. shell/container-exec access or deploy-pipeline access to the host, not an HTTP request to a
  running server.
- **`IntrospectionApi.getFixtures`** (`funktor/inspect/src/jvmMain/kotlin/introspection/api/IntrospectionApi.kt:105-125`)
  only calls `installer.getLoaders()` (a read-only listing) and is `superuser`-gated
  (`.authorize { isSuperUser() }`, line 110-111). It does not call `install()`, `clear()`, or
  `clearAndInstall()` anywhere. Grepping the whole repo for `FixtureInstaller` usage confirms the
  only call sites of the mutating methods are `InstallFixturesCliCommand` and the test harness
  below — there is no HTTP endpoint anywhere that installs or clears fixtures.
- **`AppSpec`** (`funktor/testing/src/jvmMain/kotlin/AppSpec.kt:130,191,205,226`) exposes
  `installAllFixturesBeforeSpec()` / `installFixturesBeforeSpec()` / `clearFixtures()`, used only
  from Kotest specs, not reachable from a running server.
- **No boot-time auto-install**: grepped for `fixtureInstaller`/`FixtureInstaller` across
  `AppLifeCycleHooks` and all module `index_jvm.kt` files — nothing calls `install()`/
  `clearAndInstall()` on application startup. Fixtures never run unless something explicitly asks
  for it.

**Conclusion on trigger path**: this is not remotely exploitable by a network attacker. It is a
**misconfiguration / operational-safety risk**: an operator or CI/CD pipeline running
`app --cli fixtures:install` against a host they believe is a disposable dev/staging instance, but
where the environment string failed to resolve to one of `dev`/`test`/`qa*` (typo, unset env var,
templating bug), would silently wipe live auth records and orgs instead of getting a loud failure.
Anyone who already has exec access to run that CLI command on a given host generally already has
broad access to that host's other secrets/config — so the marginal new capability this bug grants
is "quiet, unintended data loss on misconfigured hosts", not "privilege escalation for an outside
attacker". Frame findings and any red-team follow-up accordingly: this is a fail-safe/operational
hardening fix, not a remote-attacker patch — though the blast radius (all auth records, all orgs)
still justifies treating it as security-critical.

## Fix design

Invert the gate to a positive development allow-list, matching the existing (already-safe)
`isDevelopment` property:

```kotlin
internal val Funktor_Fixtures = module { config: AppConfig ->

    if (config.ktor.isDevelopment) {

        singleton(FixtureInstaller::class, SimpleFixtureInstaller::class)

        // Cli commands
        dynamic(InstallFixturesCliCommand::class)
        dynamic(ListFixturesCliCommand::class)
    } else {

        singleton(FixtureInstaller::class, NullFixtureInstaller::class)
    }
}
```

### Defence in depth: guard `SimpleFixtureInstaller` at call time

Recommended. Add a config dependency and refuse to run when not on a recognised development
environment, so a future mis-registration (e.g. someone adds
`singleton(FixtureInstaller::class, SimpleFixtureInstaller::class)` in an unrelated module, or a
future refactor of `fixtures_module.kt` reintroduces the same inversion bug) can't silently execute
against real data:

```kotlin
class SimpleFixtureInstaller(
    private val allLoaders: List<FixtureLoader>,
    private val log: Log,
    private val config: AppConfig,
) : FixtureInstaller {

    private fun ensureDevelopmentEnvironment() {
        check(config.ktor.isDevelopment) {
            "Refusing to run fixtures: environment '${config.ktor.deployment.environment}' " +
                "is not a recognised development environment"
        }
    }

    override suspend fun clear() {
        ensureDevelopmentEnvironment()
        // ... existing body
    }

    override suspend fun install(): FixtureInstaller.Result {
        ensureDevelopmentEnvironment()
        // ... existing body
    }

    override suspend fun installSelected(vararg loader: FixtureLoader): FixtureInstaller.Result {
        ensureDevelopmentEnvironment()
        // ... existing body
    }
}
```

`getLoaders()` can stay unguarded — it's read-only and already exposed (superuser-gated) via
`IntrospectionApi.getFixtures`. Adding the `AppConfig` constructor param requires updating every
`singleton(FixtureInstaller::class, SimpleFixtureInstaller::class)` call site — currently only
`fixtures_module.kt:21`, and any test that constructs `SimpleFixtureInstaller` directly (none found
in a repo-wide grep at analysis time — confirm again at implementation time since this changes the
constructor signature).

## Dependency on the env-classification task — corrected

The existing note here previously said this task and
`.claude/tasks/20260720-env-classification-allowlist.md` are mutually required for soundness.
**That is not accurate — verify at implementation time, but as read today this task is
independent.** The other task's scope (per its own spec) is limited to `isProduction` /
`isNotProduction` (`KtorConfig.kt:42,44`). It does **not** touch `isDevelopment`.

`isDevelopment` (`KtorConfig.kt:34-40`) is *already* a positive allow-list, not derived from
`isProduction`:

```kotlin
val isLocalDev: Boolean get() = deployment.environment.lowercase() == "dev"
val isTest: Boolean get() = deployment.environment.lowercase() == "test"
val isQa: Boolean get() = deployment.environment.lowercase().startsWith("qa")
val isDevelopment: Boolean get() = isLocalDev || isTest || isQa
```

This is exercised today by `funktor/core/src/jvmTest/kotlin/config/ktor/KtorConfigSpec.kt:46-65`
("development environments must be recognised" / "production must never also count as
development"), which already asserts `isDevelopment` is false for `live`/`prod`/`production` and
true only for `dev`/`test`/`qa*`. An unrecognised string (`staging`, `prd`, `""`, `"${ENV}"`) is
false for all three sub-checks, so `isDevelopment` is false for it today — independent of whatever
happens to `isProduction`/`isNotProduction`.

**This task can land before, after, or independently of the env-classification task.** The only
coupling is that both tasks touch `funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt` and
its spec file, so landing them close together avoids a rebase, but neither blocks the other's
correctness.

## Test evidence

- [ ] Kontainer test (new file, e.g.
      `funktor/core/src/jvmTest/kotlin/fixtures/FixturesModuleKontainerWiringSpec.kt`): build a
      kontainer with `funktorFixtures(AppConfig.of(ktor = KtorConfig(deployment =
      KtorConfig.Deployment(environment = "<env>"))))` (or `module(Funktor_Fixtures, config)`
      directly — `Funktor_Fixtures`/`funktorFixtures` are `internal`, visible from `jvmTest`) and
      assert which `FixtureInstaller::class` resolves and whether
      `InstallFixturesCliCommand`/`ListFixturesCliCommand` are present (`kontainer.getOrNull(...)`)
      for this table:

  | `environment` | expected `FixtureInstaller` | CLI commands registered |
  |---|---|---|
  | `"dev"` / `"DEV"` | `SimpleFixtureInstaller` | yes |
  | `"test"` | `SimpleFixtureInstaller` | yes |
  | `"qa"` / `"qa-eu"` | `SimpleFixtureInstaller` | yes |
  | `"prod"` (default) | `NullFixtureInstaller` | no |
  | `"live"` / `"production"` | `NullFixtureInstaller` | no |
  | `"staging"` / `"prd"` / `"production-eu"` | `NullFixtureInstaller` | no |
  | `""` / `"${ENV}"` | `NullFixtureInstaller` | no |

  Provide the dependencies `SimpleFixtureInstaller` needs to resolve (`List<FixtureLoader>` — empty
  is fine, `Log` — bind `NullLog`, per the pattern in
  `ultra/vault/src/jvmTest/kotlin/tools/DatabaseGraphBuilderSpec.kt:20`) if adding the `AppConfig`
  guard in this task; otherwise the existing constructor only needs `allLoaders`/`log`.
- [ ] Unit test on `SimpleFixtureInstaller` (if the call-time guard is added): `clear()`,
      `install()`, `installSelected()` each throw when `config.ktor.isDevelopment == false`, and
      succeed (no throw) when `true`.
- [ ] End-to-end: not required for the module-wiring change itself. If the call-time guard is
      added, an `AppSpec`-based test that boots with a non-development environment and asserts
      `fixtures:install` fails loudly instead of wiping repos would be valuable but is optional —
      the kontainer-level tests already cover the wiring; consider only if time allows.
- [ ] Full test command(s) run + green: `./gradlew :funktor:core:jvmTest`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
