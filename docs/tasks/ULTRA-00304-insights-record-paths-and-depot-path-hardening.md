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

### Round 2, 2026-11-11, level 2 (`reviewer-xhigh`), two-phase; charters 1, 2 and 3

Implementation and security disclosed that their phase-1 read took in this ledger (a whole-file read), domain saw one
row via a grep. Result: **NOT clean — 1 MAJOR** (charter 1). Security and domain: 0 CRITICAL, 0 MAJOR. Every round-1
fix holds; three reached only part of what they needed. Two rounds without a clean one: **safety valve, the maintainer
is asked** (review-loop).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MAJOR | `ClusterShowcaseApi.kt:157`, `ShowcaseApiBuildTest` | No e2e test pins the superuser floor on the demo's depot listings (impl M1; security MINOR-2) | fix, not yet run | `ShowcaseDepotAuthTest` (AppSpec): anonymous 401, b2b user 401, admin-realm superuser 200. The demo suite stops at boot on `${AWS_SES_ACCESS_KEY}` here, and the maintainer's demo server is running |
| 2 | MINOR | `InsightsRecordPathSpec.kt:64-81` | The `x..json`/backslash/absolute cases cannot go red; "red first" for the absolute case was wrong (impl m1, security NIT-3) | fix | Real files planted (`a\b.json`, `etc/passwd.json`); `x..json` planted and expected 200; `InsightsRecordRefSpec` unit table pins the backslash and control-character clauses. Ledger correction: the absolute case was never red |
| 2 | MINOR | `CHANGELOG.md:286-287` | "as is a name with `..`" is wrong (impl m2, gap of row 5) | fix | |
| 2 | MINOR | `InsightsRecordPathSpec.kt:109-135` | The list test has no positive assertion, depends on depot size, never cleans up (impl m3) | fix | Day `records-2999-12-31`, asserts `/listed`, cleans up; an `afterSpec` removes every planted folder |
| 2 | MINOR | `InsightsDataLoader.kt:66-82,110` | next/previous over unfiltered siblings; day folders by prefix only (impl m4, domain 3 + NIT; gap of row 3) | fix | Siblings and day folders use the record rule (`isRecordBucket`) |
| 2 | MINOR | `broker/Routes.kt:20-21` | KDoc names the deleted `InsightsGuiRoutes` (impl m5) | fix | `mountPoint` now unused: maintainer decision |
| 2 | MINOR | `insights.astro:89-98` | Runtime/Response/Vault collector descriptions wrong (impl m6, domain 7) | fix | |
| 2 | MINOR | `FileSystemRepository.kt` | Four FS methods have no per-method test (security MINOR-1) | fix | `FileSystemRepositoryPathSpec`: all six methods, traversal and symlink out; `removeFile` leaves the outside file |
| 2 | MINOR | `FileSystemRepository.kt:179-183` | `removeFile` through a symlink inside the root deletes the target (domain 1) | fix | The link itself is removed; spec case |
| 2 | MINOR | `FileSystemRepository.kt:90,112` | Refusing symlinks that lead out of the root drops mounted storage (domain 2) | maintainer decision | Implemented as "refuse" (the ticket's open item); CHANGELOG gives the migration line |
| 2 | MINOR | `FileSystemRepository.kt:75,112` | `canonicalFile` per listed entry (domain 4) | fix | Only symlinks are resolved; other entries of a resolved folder are inside by construction |
| 2 | MINOR | docs, CHANGELOG | A bundle on a CDN needs CORS for the worker and `worker-src blob:` (domain 5) | fix (docs) | |
| 2 | NIT | `DepotPaths.kt:4` | `DepotInvalidPathException.path` unread, holds attacker input (impl n1, security NIT-4) | fix | Removed |
| 2 | NIT | `addons.astro:405` | "loaded on first use" wrong for the eager default (impl n2, domain 8) | fix | |
| 2 | NIT | `vue-verify/fixtures/graph-kontainer.json` | Frozen fixture names staticweb classes (impl n3) | reject | A recorded graph of an old app; harmless |
| 2 | — | ledger row "putFile refuses a symlink" | Wording (security, impl, domain) | corrected | Only a dangling symlink, or one leading out, is refused; a live link inside is written through to its target |

Round-2 fixes, verified: funktor-all 286/286, cluster 181/181, insights 114/114, Kraft core-tests 589/589, docs build.
Mutations, each red: Y1 the siblings filter, Y2 removing the link not the target, Y3 the listing's symlink filter.
`ShowcaseDepotAuthTest` compiles; not run (demo boot key, demo server running).

### Round 3, 2026-11-11, level 3 (`reviewer-top`), two-phase; charters 1, 2 and 3

Run on the maintainer's word after the safety valve ("run the review now"). Phase 1 leaks: charter 1 saw two ledger
lines through a grep and the section heading, charter 2 one row (the `.mjs` docs row); neither built a finding on them.
Result: **NOT clean — 1 MAJOR** (charter 2, held in phase 2). Charters 1 and 3: 0 CRITICAL, 0 MAJOR.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3 | MAJOR | `FileSystemRepository.kt:179-187` | `removeFile("")` unlinks a root that is a symlink (the CHANGELOG's own migration); the link branch also removes links to folders (domain M1; impl M1 and security MINOR 1 as MINOR, plus a link outside the root reached through a link) | fix | Experiment: `File(root, "")` is the root and a symlink. The branch now removes only a link to a file whose folder lies inside the root, never the root; spec cases for a linked root (`""`, `"/"`) and a folder link. A defect of round 2's own fix |
| 3 | MINOR | `FileSystemRepository.kt:83-93` | Items reached through a link inside the root report the target's path (impl M2) | fix | IO stays on the canonical file; `FsFile`/`FsFolder` carry the path as asked for, listings the folder's own path; spec case |
| 3 | MINOR | `FileSystemRepository.kt:72` | Canonical root cached; a re-pointed root link refuses everything until restart (domain m2) | fix | Read per call |
| 3 | MINOR | `FileSystemRepository.kt:86,90` | `InvalidPathException` from `toPath()` escapes as a 500 (security MINOR 2) | fix | Mapped to `DepotInvalidPathException`; spec with `Char(0xD800)` |
| 3 | MINOR | `CHANGELOG.md`, `TAB-SPECS.md` | Old `template` slices "show as plain JSON"; both GUIs drop them (all three) | fix | "get no tab"; the CHANGELOG also names an app's own `template` collector (domain N2) |
| 3 | MINOR | `insights.astro:65-72` | The Vue snippet omits `provideSdkConfig` and `styles.ts`; the page throws (impl M4) | fix | Snippet from the demo's `main.ts` |
| 3 | MINOR | `InsightsDataLoaderSpec.kt:390-405` | The `is DepotItem.File` siblings clause is unpinned (impl M6) | fix | A folder `c.json` planted |
| 3 | MINOR | `CHANGELOG.md` | The pdf.js entry does not say what `librarySource` users lose (domain m4) | fix | Version/build/host choice gone, the modern build needs Safari 17.4+, an app's own `pdfjs-dist` is shared |
| 3 | MINOR | `PdfJs.kt:43-50` | `getDocument(params)` still hands over the caller's array (domain m5, gap of round-1 row) | fix | The copy moved into `getDocument` (a new parameter object); `PdfSource` relies on it; spec loads the same bytes twice |
| 3 | NIT | `InsightsModels.kt:24` | `isRecord()` public (domain N1) | fix | `internal` |
| 3 | NIT | `addons.astro:105`, `kraft.md:2306`, `pdfjs_addon.kt` | "loaded on first use" left in the table, mirror and KDoc (gap of round-2 row) | fix | |
| 3 | NIT | `ClusterShowcaseApi.kt:233` | The demo upload answers 500 for a refused path (security NIT 5, impl N5) | fix | 400, as DepotApi |
| 3 | NIT | `CHANGELOG.md` | "removeFile removes the link" listed as breaking: the released code did that already (impl N1) | fix | Line removed |
| 3 | NIT | `CHANGELOG.md` | The list's narrowing to records unmentioned (impl N2) | fix | |
| 3 | NIT | `FileSystemRepository.kt:157` | FS `getMeta` checks no path (impl N3) | fix | It resolves the path; spec case |
| 3 | NIT | `InsightsRecordPathSpec` | `records-2026-01-05` not cleaned; two containers of one name; a title counting NUL as an existing file (impl N4, security) | fix | |
| 3 | NIT | addon READMEs | APIs that do not exist and a wrong docs host (domain N5) | out of scope → follow-up | Predates this change |
| 3 | — | withdrawn in phase 2 | security MINOR 3 (`..` cases), the `invariantSeparatorsPath` surplus claim; impl M5, M7, N7; domain N3, N4, the es5 note | withdrawn | Reasons named in the reconciles |

Round-3 fixes, verified: cluster 186/186, insights 114/114, funktor-all 286/286, demo 52/52, pdfjs 4/4, docs build.
Mutations, each red: W1 the root/folder guard in `removeFile` (both new cases), W2 the `InvalidPathException`
mapping, W3 the path as asked, W4 the pdf.js copy, W5 `getMeta`'s check.

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
- 2026-11-11: the maintainer on the safety valve: the demo config now starts without the SES keys
  (`application.common.conf`, maintainer's change). The demo suite runs: 52/52, `ShowcaseDepotAuthTest` 4/4; mutation
  Z1 (the admin group's floor set to `public()`) red in three cases. Round 2's MAJOR is resolved.
- 2026-11-11, maintainer decisions: symlinks that lead out of a depot root stay refused ("following symlinks must
  not be allowed, that is correct"); `TemplateInsightsCollector` removed (with the View tab in both GUIs);
  `Routes.mountPoint` removed; the lock file carries `pdfjs-dist` and `@napi-rs/canvas`.
- 2026-11-11: round 3 of the gate waits until the maintainer's visual sweep of the Kraft insights pages settles, so
  it reviews the final pages once.
- 2026-11-11: round 3 (top level) not clean: one MAJOR, a defect of round 2's `removeFile` fix; fixed and mutation-checked. Third round without a clean one: back to the maintainer.
