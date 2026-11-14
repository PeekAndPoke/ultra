# ULTRA-00326: Docs: ultra/cache after ULTRA-00159 (refresh, soft size bounds, clock, estimator)

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00260 (Epic: Docs backlog)

## Why this exists

ULTRA-00159 (cache scan findings) changes behaviour and corrects claims that the public docs still make. The docs
are written when the cache batch is merged and settled; the maintainer (2026-11-14) wants one docs sweep after the
foundation modules and Slumber are done, and this ticket is the cache's part of it.

## The work

Pages `docs-site/src/pages/ultra/cache/*` and the mirror `docs-site/src/data/llms/cache.md`, which move together.

- **False today:** "only one refresh per key at a time" (`refresh.astro:74`, `llms/cache.md:123`): a write frees the
  key, so a second refresh can run beside the first.
- **To add, from ULTRA-00159's changelog text and KDoc:**
  - `maxEntries` and `maxMemoryUsage` are soft bounds: keys read in every maintenance pass are not evicted by them
    (maintainer: "Since this cache is called FastCache it is ok, when it is not 100% accurate").
  - a constantly read key expires on time under `expireAfterWrite` and the refresh's hard TTL; a value is served at
    most ttl plus two loop periods.
  - TTLs run on a monotonic clock.
  - refresh: a failed load keeps the stale value and is retried until the hard TTL; a dropped result is silent; the
    write-back case of the identity compare; a background refresh counts as an access until ULTRA-00321.
  - the size estimator: what the default counts, how far it can be off per platform, what it costs, and how to plug
    in an own `ObjectSizeEstimator`.
  - the maintenance loop holds its cache weakly: a cache nobody references stops.

## Acceptance

- Every statement on the cache pages is true against the merged code; `pnpm astro build` is green.

## History

- 2026-11-14: created on the maintainer's word ("We need to anyway do a docs-sweep ... so yes").
