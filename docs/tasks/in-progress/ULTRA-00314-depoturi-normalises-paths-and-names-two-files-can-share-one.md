# ULTRA-00314: DepotUri normalises paths and names: two files can share one uri

> **Status (2026-11-12): IN PROGRESS.**

## Why this exists

Found in the review of ULTRA-00311 (security, round 2), 2026-11-12. `DepotUri(repo, path)` trimmed the path,
dropped a leading `/` and folded `\` and runs of `/` into one `/`; a repository name's `/` became `-`. Paths
that are different files on Linux and S3 (`shared/report.pdf` and `shared/report.pdf ` with a blank at the end,
`shared\report.pdf`) got one uri. An app that stores `item.uri` and later serves `depot.getContent(uri)` hands
out another file than the one the uri was made from.

## The work (the maintainer, 2026-11-12: "Tackle the normalized DepotUri right away")

- [x] `DepotUri` keeps the path as it is; `parse` reads back exactly what `uri` printed.
- [x] A repository name a uri cannot carry (blank, a `/` or `\`, blanks around it) is refused by
      `DepotUri(...)` (IllegalArgumentException) and by `DepotFacade` (DepotException); `DepotUri.isRepoName`.
- [x] Tests: paths that differ have uris that differ and read back; refused names; mutation-checked.
- [x] CHANGELOG (breaking: a uri stored tidied names the tidied path).

## Test evidence

`ultra/depot/src/test/kotlin/DepotUriSpec.kt`, `EncryptingDepotRepositorySpec.kt` (the facade's name check).
Reviewed with ULTRA-00311 step 3, round 3.

## History

- 2026-11-12: created on the maintainer's word and built.
