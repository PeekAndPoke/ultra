# `TypedRouteRenderer` double-encodes query parameter values

**Status:** TODO
**Plan:** none — standalone defect, found during the Phase 2 review of the TypeScript SDK generator.
**Security-critical:** no.

## The defect

`TypedRouteRenderer.render` encodes each parameter value and then hands the result to `toUri`, which
encodes it **again**:

- `funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:43`
  `converter.convert(...).encodeURLQueryComponent(encodeFull = true)`
- `funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:60` → `toUri(queryParams)`
- `ultra/common/src/jvmMain/kotlin/strings.kt:33` — `URLEncoder.encode(v, "UTF-8")`

So a query value goes through two encodings. `?at=2026-07-30T10:15:30Z` renders as

```
at=2026%252D07%252D30T10%253A17%253A30Z
```

where `%25` is the re-encoded `%`. A server parsing that receives the literal text `%3A` rather than
`:`, so `MpInstant.parse` — and anything else strict — fails.

**Path parameters are NOT affected**: they are substituted into the pattern before `toUri` runs, so
they are encoded once.

## How it was found

While reviewing the generated SDK's `buildUrl`, whose KDoc claimed to mirror `TypedRouteRenderer`.
It does not, and should not: `buildUrl` is a faithful port of the Kotlin API CLIENT's builder
(`ultra/remote/src/commonMain/kotlin/helpers.kt:87` + `UriParamBuilder.kt`), which encodes once.
The generated TypeScript client is therefore **correct** and `TypedRouteRenderer` is the outlier.

That KDoc has been corrected (`3eff7c41`). This task is the defect it uncovered.

## Why it has not bitten

`TypedRouteRenderer` is a server-side LINK renderer — it builds URLs for templates and redirects, not
for API calls. Values used there are typically identifier-shaped (`[A-Za-z0-9_-]`), which survives
double encoding unchanged. It breaks for anything containing a reserved character: timestamps,
emails, paths, search strings.

## Spec

- [ ] Confirm the double encode by execution, with a value containing `:` — do not trust this write-up.
- [ ] Decide which layer should encode. `toUri` encoding is the more defensible place, since it also
      handles the keys; the caller-side `encodeURLQueryComponent` then becomes the bug.
- [ ] Check every caller of `toUri` before changing it — other call sites may pass UNencoded values
      and depend on it encoding.
- [ ] Regression test with a reserved character, asserting a single encoding round-trips.
- [ ] Check whether `CsrfProtection` tokens pass through this path; they are base64 and may contain
      `+` and `=`.

## Test evidence

- [ ] Unit/behaviour tests
- [ ] Full test command(s) run + green: `...`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |
