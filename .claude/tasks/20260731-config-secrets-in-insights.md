# Config secrets are written verbatim into every insights record

**Status:** FOUND, NOT FIXED — needs a decision, and it crosses three modules with other owners
**Security-critical:** yes
**Found:** 2026-07-31, while distilling the reference renderers into a Vue tab spec
**Related:** `.claude/tasks/20260731-depot-findings.md` (the `@JsonIgnore` constraint — now shown incomplete)

## What was observed

Not inferred. Read out of the bytes of a record written by the running app
(`funktor/all/src/jvmTest`, `InsightsRecordingSpec`, depot under `/tmp/funktor-all-insights*`):

```
app-config.config.funktor.auth.jwt.signingKey = "ka2fEBWhmjPFpaPhg5Iir6tAX1COT0lGNKbD3L5SqP1We…"
app-config.config.funktor.security.csrfSecret  = "CHANGE_ME"
```

`AppConfigCollector` serialises the **entire `AppConfig`** through `InsightsMapper` (Jackson), and
nothing removes these.

## What leaks, and what protects it today

| Field | Declared in | Protection |
|---|---|---|
| `funktor.auth.jwt.signingKey` | `ultra/security/src/commonMain/kotlin/jwt/JwtConfig.kt:8` | redacting `toString()` **only** |
| `funktor.security.csrfSecret` | `ultra/security/src/jvmMain/kotlin/UltraSecurityConfig.kt:5` | redacting `toString()` **only** |
| `arangodb.password` | `karango/core/src/main/kotlin/config/ArangoDbConfig.kt:13` | none |
| `mongodb.connectionString` | `monko/core/src/main/kotlin/MongoDbConfig.kt:4` | none — credentials are conventionally embedded in the URI |
| `keys: Map<String, String>` | `funktor/core/.../config/AppConfig.kt:170` | none — a bag reached by `getKeyOrNull(name)`, i.e. exactly where an app puts its own secrets |

The last three are not in the funktor:all test config, so they were not in the record I read. They are in
`FunktorDemoConfig` (`funktor-demo/server/src/main/kotlin/config.kt:17,18`), which **is** the `AppConfig`
that gets serialised.

### Why this survived review: the protection is the wrong mechanism and looks right

`JwtConfig` and `UltraSecurityConfig` both carry a hand-written redacting `toString()`, KDoc'd
*"Redacts [signingKey] to prevent accidental exposure in logs or error messages."*
`ultra/security/src/jvmTest/kotlin/UltraSecurityConfigSpec.kt:9` even pins it — **"toString must redact
csrfSecret"**, a passing test guarding a route the secret does not travel by.

**Jackson never calls `toString()`.** It reads properties. So an auditor — including me, when I wrote the
`@JsonIgnore` table in the depot-findings file — sees redaction and moves on. That table lists four
places as "the only thing keeping secrets out of records" and is missing every row above.

`@JsonIgnore` is used correctly elsewhere (`KtorConfig.kt:28,30`, `AwsSesConfig.kt:9`, `AwsS3Config.kt:8`,
`ultra/vault/domain.kt:43,47,52`), which is what makes the gaps look deliberate rather than forgotten.

## Severity: HIGH, not critical — and why

The insights GUI is **gone from the compile path** (verified: no `InsightsGui` reference outside
`reference/`), so the only reader today is `InsightsApi`, which is `authFloor = { isSuperUser() }`. A
superuser already holds every privilege the signing key would forge, so this is **not** a live privilege
escalation.

What remains is real:

1. **Plaintext at rest.** Every record on disk carries the key. Backups, container images, log shippers
   and mounted volumes all now contain material that forges a superuser JWT — `HMAC512`, symmetric, so
   possession is sufficient. "Someone read a file" becomes "someone is a superuser".
2. **It crosses the network** on every `getRecord`, into whatever proxy or cache sits in front.
3. **It is one config change from critical.** Any future "let support staff read insights" role, or a
   re-mounted GUI, converts this straight into anonymous authentication bypass. The exposure is bounded
   by a decision nobody wrote down.
4. **It compounds depot F1** (symlinks followed, root not canonicalised) — the depot is the store that
   now holds the key.

## Fix options — needs a decision

**1. `@get:JsonIgnore` on each field.** Matches the existing pattern. Two problems: `JwtConfig` lives in
`commonMain` with no Jackson on the classpath, so the annotation cannot simply be added there; and it is
opt-in, which has now failed silently four times. Also worth checking while there — `KtorConfig.Security`
uses `@get:JsonIgnore` with no `@JsonProperty`, which in Jackson normally suppresses **deserialization**
too, so `keyStorePassword` may not be loading from config at all.

**2. Redact inside `AppConfigCollector`, by field name.** Reuse the sensitive-name policy already built
for `HeaderLogging` (`token|secret|password|credential|key|signature`) over the serialised config tree.
Fails safe, needs no cooperation from config authors, works whatever serializer the write path uses
later, and lives entirely in `funktor/insights` — no other agent's module. Residual risk is a secret with
an innocuous name, which is the same trade the header deny-list already accepted deliberately.

**3. Allow-list the config tab instead.** Config is a bounded known set, unlike headers, so an allow-list
is defensible here where it was not there — at the cost of a tab that shows very little.

**4. Drop the `app-config` slice.** Simplest and safest; loses a genuinely useful tab.

**Recommendation: 2, plus 1 where it is cheap** (`ArangoDbConfig`, `UltraSecurityConfig` — both
JVM-only). 2 is the one that holds when someone adds a config class next month; 1 is defence in depth.
Whatever is chosen, it needs **a test that a known secret does not appear in a written record** — the
depot-findings file already demands that test for a different reason (moving off Jackson), and it would
have caught this on day one.

## Also fix regardless of which option wins

- The KDoc on `JwtConfig.toString()` and `UltraSecurityConfig.toString()` claims protection it does not
  provide. Either state the narrow truth ("logs and error messages only — Jackson and Slumber both
  serialise this field in full") or the sentence keeps misleading the next reader.
- `.claude/tasks/20260731-depot-findings.md`'s `@JsonIgnore` table needs the rows above, or it reads as
  a complete inventory.

## Not in scope

- Rotating the demo's committed test signing key. It is a test fixture in
  `funktor/all/src/jvmTest/resources/config/application.test.conf`, already public in the repo.
