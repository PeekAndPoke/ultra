# ULTRA-00313: Karango: remove(idOrKey) swallows every database error as 'nothing removed'

> **Status (2026-11-12): DESIGNED.**

## Why this exists

Found in the review of ULTRA-00311 (depot encryption), 2026-11-12. `EntityRepository.remove(idOrKey)`
(`karango/core/src/main/kotlin/vault/EntityRepository.kt:330-338`) catches every `KarangoQueryException` and returns
`RemoveResult(count = 0)`. `KarangoDriver` turns every `ArangoDBException` into that exception: connection loss,
timeouts, failover, write-write conflicts, and "document not found" (1202), which is the one case the catch is
there for. A caller cannot tell "nothing to remove" from "the removal failed". Monko's `deleteOne` lets errors
through.

## The work

- [ ] `remove(idOrKey)` answers count 0 only for error 1202; every other error is thrown.
- [ ] Find the callers that rely on the swallowing (grep `remove(` on Karango repositories) and decide each.
- [ ] A test on Karango that a failed removal throws; mutation-checked.
- [ ] CHANGELOG: behaviour change.

## History

- 2026-11-12: created on the maintainer's word ("yes to the two new tickets").
