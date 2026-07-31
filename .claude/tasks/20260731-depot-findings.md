# Depot — findings parked for the next visit

**Status:** COLLECTED — not scheduled
**Found by:** `/feature-review` of the insights REST API (2026-07-31) and the discussion after it
**Security-critical:** partly — F1 is remotely exploitable through an authenticated admin

Everything here is in `funktor/cluster/src/jvmMain/kotlin/depot/`. None of it was touched by the insights
work; it surfaced because the insights API is the first thing to read depot content over HTTP.

## F1 — symlinks are followed, and the root is not canonicalised

`repos/fs/FileSystemRepository.kt:76, 115, 139`

`root` is `File(dir).absoluteFile`, not `.canonicalFile`, and `getFile` / `getContent` / `listItems` do
no containment re-check after resolving. `validateName()` rejects `..`, so ordinary traversal is closed
(verified) — but a **symlink inside the depot resolves outside it** and the read goes straight through.

**Scenario.** An attacker who can write to the depot directory plants
`records-2026-07-31/x.json -> /proc/self/environ`, or the app's `application.conf`. The next superuser
to open that record receives the file contents over HTTP. This converts the accepted risk "depot files
are readable by anyone with filesystem access" into "any file the app user can read, exfiltrated
remotely through an authenticated admin surface".

**Fix direction:** `file.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())` before every
read, or open with `LinkOption.NOFOLLOW_LINKS`.

Also red-team item 22 in `.claude/tasks/20260730-redteam-insights-api.md`.

## F2 — `listItems(path, limit)` ignores `path`

`repos/fs/FileSystemRepository.kt:103`

```kotlin
override suspend fun listItems(path: String, limit: Int): List<DepotItem> {
    return listItems().take(limit)          // listItems() == listItems("") == the ROOT
}
```

The `path` argument is discarded, so `listItems("records-2026-07-31", 50)` returns 50 entries from the
repository **root** — the day folders — not the files in that day. The listing is unsorted, so the 50 are
arbitrary. `AwsS3Repository.kt:125` implements the same overload separately, so the two backends
disagree about what it means.

**Why it matters:** this is exactly the overload someone reaches for when optimising a large listing
(insights finding D-M3/S6). It returns the wrong directory's contents **silently** rather than failing.
`InsightsDataLoader` deliberately avoids it today.

**Fix direction:** honour `path`, and decide whether `limit` implies an ordering — a "limit" over an
unordered listing is not meaningful. Sorting by name is free and, for timestamp-named files, correct.

## F3 — the security comment overstates what the code checks

`repos/fs/FileSystemRepository.kt:15, 87, 113, 125, 137`

```kotlin
private fun String.isInvalidName() = contains("..")
```

Every call site is commented *"SECURITY. Prevent path traversal! check for any slashes and dot-dots"* —
but slashes are **not** checked, only dot-dots. The behaviour is correct (slashes alone cannot escape the
root, and paths legitimately contain them), so this is a comment defect, not a hole. It is worth fixing
because a reader auditing this trusts the comment.

## F4 — no ordering guarantee anywhere

`listItems` returns `dir.listFiles()` order, which is filesystem-dependent and unspecified. Every caller
that cares must sort. Worth deciding whether the interface should promise an order — insights sorts by
name, the inspect UI may sort differently.

## Related constraint discovered nearby — DO NOT move the insights write path off Jackson

Not a depot finding, but the same investigation, and it belongs somewhere durable.

Secrets are kept out of insights records and logs by **`@JsonIgnore`**:

| Where | What it hides |
|---|---|
| `funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt:28,30` | two keystore passwords |
| `funktor/messaging/src/jvmMain/kotlin/senders/aws/AwsSesConfig.kt:9` | AWS credentials |
| `funktor/cluster/src/jvmMain/kotlin/depot/repos/aws/AwsS3Config.kt:8` | AWS credentials |
| ~~`ultra/vault/src/jvmMain/kotlin/domain.kt:43,47,52`~~ | **NOT secrets** — `collection`, `asRef`, `asStored` are derived properties excluded to stop recursion, not credentials. Listed here in error; corrected 2026-07-31 |

`AppConfigCollector` serialises the **entire `AppConfig`** through `InsightsMapper` (Jackson), so those
annotations are the only thing standing between the app's signing keys and a stored insights record.

**This table is NOT the full inventory — it is the list of fields that are protected.** Several config
secrets have no `@JsonIgnore` at all and are written into every record verbatim, including the JWT
signing key. Verified against a real record on 2026-07-31; see
`.claude/tasks/20260731-config-secrets-in-insights.md`. Read that before relying on anything here.

**Slumber and kotlinx.serialization do not honour `@JsonIgnore`.** The insights *read* path moved to
kotlinx on 2026-07-31, which makes moving the *write* path look like tidy-up. It is not: doing so would
silently start writing signing keys and AWS credentials into every record — no error, no warning. If the
write path is ever migrated, each `@JsonIgnore` needs an equivalent in the new serializer **first**, and
a test that asserts a known secret does not appear in the written record.
