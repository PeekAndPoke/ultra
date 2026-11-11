# ULTRA-00304: Insights record paths and depot path hardening

> **Status (2026-11-11): IN PROGRESS.** Implemented with the recommended decisions; mutation checks and the review gate next. Out-of-scope finding of ULTRA-00198's review gate; the maintainer on 2026-11-11:
> "yes we need to harden this. Does depot allow path traversal from user input? If so we need to fix this too."
>
> **Part of:** ULTRA-00253 (Epic: Insights)

**Security-critical:** yes (red team: ULTRA-00161, item 45)

## Why this exists

**Insights.** `InsightsApi` builds `InsightsRecordRef(bucket = params.bucket, file = params.file)` from the URL
(`funktor/insights/src/jvmMain/kotlin/api/InsightsApi.kt:80`) and `InsightsDataLoader.load` reads `"$bucket/$file"`
(`InsightsDataLoader.kt:45-49`). Any path inside the insights repository is read, not only
`records-<date>/<name>.json`; a `..` makes the FS repository throw `error("Invalid name")`, which answers 500.

**Depot, read 2026-11-11 (not yet pinned by tests):**
- `FileSystemRepository` (`funktor/cluster/src/jvmMain/kotlin/depot/repos/fs/FileSystemRepository.kt`) rejects any
  `..` in every method (`validateName`). An absolute path stays under the root: `File(root, "/etc/passwd")` is
  `<root>/etc/passwd` on Unix. Symlinks inside the root are followed. So no escape from the repo by path, as read.
- `AwsS3Repository`: one repo is one bucket; paths are S3 keys, taken literally by S3. To verify: that AWS SDK
  2.56.0 does not normalise a `../` key segment in the request path (path-style addressing would then leave the
  bucket).
- **The real gap:** a depot repo has no boundary inside it. Every caller that passes user input as a path exposes the
  whole repo; the depot cannot know which part a caller means.
- Found on the way:
  - `FileSystemRepository.listItems(path, limit)` ignores `path` and lists the root (`listItems().take(limit)`).
  - `listItems` throws for the whole folder when one entry's name contains `..` (`.onEach { it.path.validateName() }`).
  - The demo's public `ClusterShowcaseApi.getDepotFiles` (`funktor-demo/server/.../ClusterShowcaseApi.kt:99`,
    `authFloor = public()`) lists the root of any repo named in the request, the insights repo included
    (folder names only).

## Decide before implementing

1. Where the insights check lives: in `InsightsRecordRef` (a `bucket` matching `records-\d{4}-\d{2}-\d{2}` and a
   `file` matching `[^/\\]+\.json` with no `..`), checked by the loader. Recommendation: a `isValid()` on the ref,
   called by the loader; an invalid ref is "no such record" (404), never 500.
2. Depot: should `validateName` move up into `DepotRepository` as a shared `DepotPath` check (no `..` segment, no
   NUL, no backslash), so S3 and FS refuse the same paths? Recommendation: yes, with a clear exception type that the
   API maps to 400.
3. The demo's public depot listing: restrict to superuser, or drop. Recommendation: superuser.

## The work

1. Insights: the ref check, the loader answers null for an invalid ref; e2e test (`AppSpec`) on the detail endpoint
   with `..`, `%2e%2e`, an absolute path, a backslash, a non-`.json` file, a valid ref → 404, 404, 404, 404, 404, 200.
2. Depot: shared path check; FS e2e tests for `..`, absolute, NUL, symlink out of root (decide: refuse or allow);
   S3 key check by test against the SDK's request building (no live bucket).
3. `listItems(path, limit)` honours `path`; one bad name skips that entry instead of failing the folder.
4. The demo endpoint.
5. Mutation-check each check (security-relevant).

## Acceptance

- The e2e tests above, each red before its fix.
- `ULTRA-00161` item 45 can be run against it.

## History

- 2026-11-11: created from ULTRA-00198's gate; depot read for traversal.
- 2026-11-11: implemented, decisions as recommended:
  - `InsightsRecordRef.isRecord()` (`records-yyyy-mm-dd` / one segment `.json`, no `..`); the loader answers null
    (404) for anything else.
  - `String.validateDepotPath()` + `DepotInvalidPathException` (`depot/repos/DepotPaths.kt`): no `.`/`..`
    segment, backslash or NUL; used by both repositories. The FS repository also checks the canonical path stays
    under the canonical root (a symlink out is refused and not listed). A name like `a..b` is allowed now.
  - `DepotApi.browse` answers 400 for a refused path (was 500 with a stack trace in the messages).
  - `listItems(path, limit)` honours `path`; one bad entry no longer fails the folder listing.
  - Demo: `getDepotRepos`/`getDepotFiles` are superuser only (they listed repo locations and files publicly).
  - S3: the check runs before every SDK call; no test against the SDK's request building (no bucket here), the
    shared check is unit-tested.
  - Tests: `InsightsRecordPathSpec`, `DepotPathSpec` (funktor/all, e2e, both red before: 200 / 500 / a symlink
    listed), `FileSystemRepositorySpec` (funktor/cluster).
- 2026-11-11: a private companion on `InsightsRecordRef` broke Slumber's reflection (every record read was a 500);
  the regexes are top-level now. Green: funktor-all 283/283, insights 111/111, cluster, codegen 104/104.
  Mutations, each red: D1 `..` check, D2 backslash check, D3 canonical check, D4 listing filter, D5 limit path,
  I1 `isRecord` in the loader, I2 the 400 mapping. Review gate next (security-critical).
