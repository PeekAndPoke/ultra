# ULTRA-00321: Cache: a background refresh must not count as an access (expireAfterAccess, LRU)

> **Status (2026-11-14): DESIGNED.**
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

**Plan:** ULTRA-00316 (close the open epics), track B. Found by the reviews of ULTRA-00159 (cache scan findings).

## Why this exists

`FastCache` with `refreshAfterWrite` reloads a value in the background. That reload is recorded like a read by a
caller: it renews `expireAfterAccess` and moves the key to the young end of the LRU order. A key that nobody reads
any more but that keeps being refreshed is therefore never idle and is evicted last.

The maintainer (2026-11-14): "I guess it should not count as access."

## Decide before implementing

- Confirm the rule: only a caller's `get`/`getOrPut` counts as an access; a refresh and its write count as neither an
  access nor (for `expireAfterAccess`) a renewal.
- Whether a refresh may still renew `expireAfterWrite` (it is a write) or whether the hard TTL of the refresh
  behaviour is the only bound.

## The work

- Tests first: a refreshed but unread key expires after `expireAfterAccess`; a refreshed but unread key is evicted
  before a read one when the cache is full.
- Change the tracking in `ultra/cache/src/commonMain/kotlin/FastCache.kt`; mutation check on the new tests.
- Docs: the cache page and its LLM mirror say what counts as an access.

## Acceptance

- Both tests green on JVM and JS, red without the change.
- CHANGELOG names the behaviour change (no compile signal).

## History

- 2026-11-14: created on the maintainer's word ("yes followup ticket").
