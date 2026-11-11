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

## Review record (the ledger) — funktor/kraft gate (ULTRA-00303, 00304, 00305, 00306)

Round 1, 2026-11-11, level 1 (`reviewer-high`), blind; charters 1, 2 and 3.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `ClusterShowcaseApi.kt:34,85,108` | `authorize { isSuperUser() }` on the `public()` group fails the auth-chain check at construction: the demo server does not boot (sec M1) | fixed `b37789f2` | Confirmed by `FloorAuthRuleBuilderSpec:99`; the listings moved to `ClusterAdminShowcaseApi`; `ShowcaseApiBuildTest` red on the old code with "must be the SOLE rule" |
| 1 | MAJOR | `FileSystemRepository.kt:89-91` | On Windows, listed paths carry `\`, which the new check refuses: insights list 500, browse 400 (impl MAJOR-1, sec MINOR 5) | fix | `getPathInRepo` gives `/`-separated paths (`invariantSeparatorsPath`); the backslash rule is replaced (see F7) |
| 1 | MINOR | `InsightsModels.kt:24,29`, `InsightsDataLoader.kt:47-52,127` | NUL in a record name, a symlink in a day folder, or a listed name the depot refuses still answer 500 (sec MINOR 2, impl MINOR-1) | fix | `RECORD_FILE` excludes control characters; `load` answers null for a refused path; the list skips such entries and lists only `isRecord()` names (domain N3) |
| 1 | MINOR | `FileSystemRepository.kt:74-83` | A dangling symlink passes the canonical check (writes land outside); check-to-use window (sec MINOR 3) | fix | Experiment: `File(root,"link").canonicalFile` of a dangling link stays `root/link`, inside=true. Reads and writes use the canonical file; `putFile` refuses a symlink. Local writer racing the check: out of scope, KDoc |
| 1 | MINOR | tests | S3 checks unpinned; `x..json` cannot go red; `..` bucket may be a routing miss; NUL/absolute e2e missing (sec MINOR 4, impl MINOR-2) | fix | S3 stub test per method; the `..` check in `isRecord` removed (NIT 7); e2e NUL and absolute cases; the `..` bucket case replaced |
| 1 | MINOR | `kotlin-js-store/yarn.lock` | `pdfjs-dist` not in the committed lock (sec MINOR 6) | out of scope → maintainer | The maintainer updates the lock ("yarn lock fine"); noted in the report |
| 1 | MINOR | `DepotPaths.kt:11` | The shared backslash rule over-restricts S3: existing keys with `\` become unreadable (domain F7) | fix | Backslash is a separator only on Windows FS, where the canonical check guards; the rule refuses `.`/`..` segments split on `/` and `\`, and NUL |
| 1 | MINOR | `TemplateInsightsCollector.kt` | No writer left since the reference GUI went; docs advertise it (impl MINOR-3, domain F6) | fix (docs) + maintainer decision | Docs say an app records into it; removing the collector touches both GUIs: asked |
| 1 | MINOR | `README.MD:115`, agent file, ULTRA-00153/00190/00028 | Stale staticweb mentions (impl MINOR-4) | fix | |
| 1 | MINOR | `ClusterShowcaseApi.kt:30` | KDoc says depot files are public (impl MINOR-6) | fixed `b37789f2` | |
| 1 | MINOR | addons docs | `.css` loader rules catch `new URL` CSS; `.mjs` needs a JS MIME type (domain F2, F3) | fix (docs) | |
| 1 | MINOR | non-es2015 consumers (domain F1) | `import.meta` may fail for an app on the default target | reject after experiment | The addons example built with `target` es2015 removed: UMD `.js` output, webpack emitted the 4 CSS files and the worker, no raw `import.meta` left |
| 1 | MINOR | `pdfjs_addon.kt:11,23` | KDoc says CDN (domain F4) | fix | |
| 1 | MINOR | `CHANGELOG.md` | staticweb gave `funktor:all` users ktor html-builder, kotlin-css, `ultra:html`, `ultra:semanticui` (domain F5) | fix | One line naming them |
| 1 | MINOR | `PdfSource.kt:30-32` | pdf.js transfers the caller's `Uint8Array` (pre-existing, domain F8) | fix | A copy is passed |
| 1 | NIT | `Deps.kt:433-437` | Dead `pdfJs()` helper (sec NIT 8, impl MINOR-5, domain N2) | fix | |
| 1 | NIT | `AwsS3Repository.kt:141-147` | S3 listing does not check keys (sec NIT 9) | reject | Fails closed: listed, a read refuses; moot for `\` after F7 |
| 1 | NIT | S3 blank lines, `DepotPathSpec` cleanup, ULTRA-00306 "Why" version | (impl NITs) | fix | |
| 1 | NIT | `docs/kraft/examples/addons/*.js` | Committed example bundles still hold cdnjs URLs | out of scope → maintainer | Rebuilt by `distributeJsExample`, which only the maintainer runs |

Round-1 fixes, verified: funktor-all 285/285, cluster 168/168, insights 111/111, pdfjs 3/3, prismjs 10/10, docs build.
New tests red first: `ShowcaseApiBuildTest` (on the committed demo), the S3 stub spec, the NUL, absolute, symlink and
list cases. Mutations, each red: X1 the `load` catch, X2 the list's `isRecord` filter, X4 the dangling-symlink check,
X7 one S3 check (`getMeta`), X8 the backslash split. X3 (the list's catch) SURVIVED: the listing already leaves out
symlinks out of the root and dangling ones, so the catch was unreachable; removed. Not testable on Linux: the
`/`-separated `getPathInRepo` (Windows only).

Probed and clean (round 1): `..`/`.`/NUL refused on every FS and S3 entry point; URL encoding (single, double); absolute
paths; Unicode look-alikes; prefix confusion; existing symlinks out; who reaches the endpoints; error leakage
(generic message, 400 without echo); `isRecord` against every written record name; staticweb held nothing
protective; the `new URL` literals cannot be injected; Prism manual mode kept; asset URLs under a CDN or sub-path.

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
