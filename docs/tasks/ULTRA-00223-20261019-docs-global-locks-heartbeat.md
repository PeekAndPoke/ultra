# ULTRA-00223: Docs: global locks expire by heartbeat

> **Status (2026-10-19): NOT STARTED.** write after ULTRA-00224 (global locks max hold) lands (it adds the `maxHold` parameter).
> Settled 2026-10-19: lock timing relies on NTP (state it as a requirement)
>
> **Part of:** ULTRA-00260 (Docs backlog epic)

**Plan:** follow-up of ULTRA-00219 (global locks heartbeat) (public API changed)
**Security-critical:** no

## What changed that the docs must say

- `docs-site/src/pages/ultra/funktor/cluster.astro` + the LLM mirror (`docs-site/src/data/llms/funktor.md`): a lock lives as
  long as its holder's heartbeat — `ttl` (60 s) and `heartbeatInterval` (ttl/3) on `VaultGlobalLocksProvider`; no maximum
  lifetime; a lost lock is logged, the handler runs on.
- Operating constraints (from the provider KDoc): ttl > worst DB stall + clock skew + one heartbeat interval; ttl and
  heartbeatInterval uniform across the cluster (change them in two deploys); `ttl <= 15 min`; a write that protects an
  invariant still needs a conditional write; bound a handler with `withTimeout` — a handler that never returns holds its
  lock until the server stops or `funktor:cluster:release-locks` frees it.
- Upgrade caveat: during a rolling deploy, three OLD-server paths can still delete a live token lock (an old peer's beacon
  rule against a freshly booted new server, an old server's boot wipe, an old holder's release by key).
- API breaks: `GlobalLocksProvider.releaseExpired()` is new and abstract; `VaultGlobalLocksProvider.repository` is a
  `VaultGlobalLocksProvider.Repo`; the listing's `GlobalLockEntryModel.lastHeartbeat`.

## History

- 2026-10-21: numbered ULTRA-00223 (was `docs/tasks/20261019-docs-global-locks-heartbeat.md`).
