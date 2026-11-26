# ULTRA-00326: Docs: ultra/cache after ULTRA-00159 (refresh, soft size bounds, clock, estimator)

> **Status (2026-11-26): IN PROGRESS.** Pages and mirror written on `wt/cache-log`; `pnpm astro build` green.
> Waits for the cache code review running in parallel: a code change there may need a docs change here.
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
  - `maxMemoryUsage`: an entry whose own estimate exceeds the budget is evicted alone at the next pass, never
    counted, the other entries stay; such a value is never cached, also a small one whose estimate is inflated
    (sampling errors multiply per nested level). Only a pending write spares it, and that write is sized anew.
  - the lambda form of an own estimator (`ObjectSizeEstimator` is a `fun interface`):
    `maxMemoryUsage(bytes) { obj -> ... }`. It is called once for the key and once for the value of each write,
    each as `Any?`.
- **False after ULTRA-00159:** "JS charges every number 1 byte" (`eviction.astro:114`, `llms/cache.md:109`): it is 8
  bytes now.

## Written (2026-11-26)

Every statement checked against `ultra/cache/src` (`FastCache.kt`, `ObjectSizeEstimator.kt`, the platform actuals) on
`wt/cache-log` at b885baf1/3f0e9229. Figures (0.03 ms, 15 to 35 ms, 7.6, 0.63, about 3 800, 48 B per map entry) are the
KDoc's measured values, not re-measured.

| Item | Page | Mirror `llms/cache.md` |
|---|---|---|
| "only one refresh per key" | already corrected by ULTRA-00356 (`refresh.astro:79-82`), kept | :216 (unchanged) |
| soft bounds | already there (`eviction.astro:88`) | :88 |
| hot key expires on time (write TTL, hard TTL) | `eviction.astro:65`, `refresh.astro:71` | :75, :224 |
| ttl plus two loop periods; monotonic clock; `timeSource`; TTL and loop delay fallbacks | `eviction.astro:37-45` | :61-64 |
| refresh: retry until hard TTL, failure line | `refresh.astro:96-99` | :226 |
| refresh: dropped result, write-back case, equal values for primitives | `refresh.astro:84-88` | :218-221 |
| refresh counts as an access (open, ULTRA-00321; public text says "an open question, may change") | `refresh.astro:90-92` | :222-223 |
| refresh: zero ttl, hardTtl not above ttl | `refresh.astro:74-75` | :224-225 |
| loop holds its cache weakly | already there (`eviction.astro:345`) | :281 |
| `maxMemoryUsage`: sized on writes, key and value; over-budget entry alone, only a pending write spares it | `eviction.astro:92-106` | :97-112 |
| estimator: what it counts, cap, sampling, never throws, cost bound and what it excludes | `eviction.astro:108-142` | :114-131 |
| which way it errs, per platform (table) | `eviction.astro:144-223` | :133-145 |
| not a memory bound for attacker-shaped data | `eviction.astro:226-230` | :147-149 |
| own estimator: `fun interface`, lambda, contract, `fallbackSize`, `addBehaviour`, limits, untrusted data | `eviction.astro:232-271` | :151-176 |
| tuning `EstimatorConfig` (all seven fields, `SamplePick`), odd settings | `eviction.astro:273-296` | :178-189 |
| JS numbers 8 B (was "1 byte"); "recursively" and "is an interface" removed | `eviction.astro` table, JS row | JS row |

Not written: the ULTRA-00159 follow-up items outside this list (`clear()` is not an eviction, `NullableCache` cycles).

## Acceptance

- Every statement on the cache pages is true against the merged code; `pnpm astro build` is green.

## History

- 2026-11-14: created on the maintainer's word ("We need to anyway do a docs-sweep ... so yes").
- 2026-11-25: the ULTRA-00356 truth audit (merged 9636ac3a) fixed the false spots this ticket lists where the code is
  settled; the new documentation it asks for is still open.
- 2026-11-26: pages (`eviction.astro`, `refresh.astro`) and the mirror written against the code: estimator
  section, own estimator, refresh semantics, TTL fallbacks; build green. IN PROGRESS until the parallel code review settles.
