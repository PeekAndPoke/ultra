# API Endpoint Tests in funktor/all

## Status: IMPLEMENTED

All funktor API endpoints are now covered with integration tests. 92 tests across 6 specs, 0 failures.

## What was built

### Test infrastructure (funktor/all)

| File                                                 | Purpose                                                                                                |
|------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `build.gradle.kts`                                   | Added `kotlin("plugin.serialization")`, karango/monko test deps, KSP processors                        |
| `src/jvmTest/resources/config/application.test.conf` | Minimal HOCON config with JWT signing key                                                              |
| `src/jvmTest/kotlin/index_testJvm.kt`                | `FunktorAllTestConfig`, `createBlueprint`, `testApp`, `apiApp` helper                                  |
| `src/jvmTest/kotlin/server.kt`                       | Ktor module with JWT auth and API route mounting                                                       |
| `src/jvmTest/kotlin/TestUserRealm.kt`                | `@Vault @Serializable TestUser`, `TestUsersRepo`, `TestUserRealm` (admin-user realm), `TestUserModule` |
| `src/jvmTest/kotlin/FunktorApiSpec.kt`               | Base class with shared `superUserToken`, `regularUserToken`, `realm`, `usersRepo`                      |

### Test specs

| Spec                     | Tests | Endpoints     | Coverage                                                                         |
|--------------------------|-------|---------------|----------------------------------------------------------------------------------|
| `IntrospectionApiSpec`   | 30    | 10            | Anonymous + regularUser + superUser per endpoint, body assertions on 6 endpoints |
| `ClusterApiSpec`         | 30    | 15 (7 groups) | Anonymous + superUser, 404 for non-existent IDs                                  |
| `LoggingApiSpec`         | 12    | 4             | Anonymous + regularUser + superUser, body assertions on list/bulk                |
| `AuthApiSpec`            | 12    | 8             | getRealm body assertion, sign-up + sign-in happy path, error cases               |
| `ShowcaseApiTest` (demo) | 7     | 6             | REST endpoints with body assertions, retry demo, auth rule checks                |
| `DemoTest` (demo)        | 1     | 1             | Smoke test for getRealm                                                          |

### Key design decisions

- **Real ArangoDB** via `ArangoDbConfig.forUnitTests` (same as existing auth tests)
- **BeforeProjectListener removed** in AppSpec — replaced with `beforeSpec` + `dbInitialized` flag (Kotest 6.x
  compatibility)
- **JWT tokens generated via TestUserRealm** (admin-user realm) with actual DB user insertion
- **Unique emails** for sign-up tests using `System.currentTimeMillis()` to avoid cross-run collisions
- **Three auth tiers tested**: anonymous (401), regularUser (401), superUser (200) for isSuperUser endpoints

### Auth behavior discovered during testing

- Anonymous requests to `isSuperUser` endpoints return **401 Unauthorized** (not 403 Forbidden)
- `activateAccount` is a stub — always returns `200 OK` with `success=false` regardless of realm
- `setPassword` from anonymous returns 403 (identity mismatch check runs before realm check)
- `workers.get` returns 200 even for non-existent workers (API design choice)

## Future enhancements

- **Showcase cluster/messaging/SSE tests** — ClusterShowcaseApi (10 endpoints), MessagingShowcaseApi (3),
  SseShowcaseApi (2) are untested
- **ClusterApiSpec body assertions** — currently only checks status codes
- **Auth happy-path expansion** — setPassword, activateAccount, recoverAccount flows need happy-path tests
- **Configurable auth rules** — make `isSuperUser()` / `public()` configurable per test to reduce boilerplate
- **Non-superUser tests in ClusterApiSpec** — add regularUser tier for consistency with
  IntrospectionApiSpec/LoggingApiSpec
