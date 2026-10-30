# ULTRA-00273: Kraft routing: a malformed percent-escape makes route matching throw

> **Status (2026-10-30): IN PROGRESS.** Fixed 2026-10-30 (the maintainer: bugs into this release): route matching decodes with a fallback to the raw text (`RouteBase.kt` `decodedOrRaw`); `RouteMatchingSpec` (3 rows). Review gate next.
>
> **Part of:** ULTRA-00250 (Epic: Route auth and error disclosure)

**Security-critical:** no

## Why this exists

`kraft/core/src/commonMain/kotlin/routing/RouteBase.kt:68` and `:78` decode with JS `decodeURIComponent`, which
throws URIError on a lone `%` or `%E0`. A crafted link `#/edit/1?q=%`, a typed hash or an initial URL then throws out of
route matching (and, with a leave guard mounted, out of `isLeaving` on every navigation). Fails closed: nothing
navigates and no guard is bypassed.

## The work

1. A decode that falls back to the raw text on URIError.
2. A spec: a malformed query or param resolves the route with the raw text.

## History

- 2026-10-29: created from the review gate of the UI batch.
- 2026-10-30: IN PROGRESS.
- 2026-10-30: fixed; spec added and mutation-checked.
