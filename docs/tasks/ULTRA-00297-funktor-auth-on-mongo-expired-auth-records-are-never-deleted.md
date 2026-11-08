# ULTRA-00297: funktor-auth on Mongo: expired auth records are never deleted (the TTL index needs a BSON date)

> **Status (2026-11-08): DESIGNED.** The maintainer, 2026-11-08: "we need a ticket for each … How can we implement
> auto-expiry here on Mongo?" One decision open (the clock, below).
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)

**Plan:** none. Found by the activation-resend review; listed only in the backlog of ULTRA-00221 (insights auth parallel plan).
**Security-critical:** no for access (expiry is enforced in code); yes for persistence, so mutation-checked.

## Why this exists

`AuthRecord.expiresAt` is a `Long` of epoch seconds (`funktor/auth/src/jvmMain/kotlin/domain/AuthRecord.kt:26,37`).
`MonkoAuthRecordsRepo` puts a TTL index on it (`db/monko/MonkoAuthRecordsRepo.kt:75-78`), but a MongoDB TTL index only
expires documents whose field is a BSON **Date** (or an array of them); a number is ignored. So on Mongo, sessions,
recovery, verification and resend tokens, and unactivated sign-ups pile up forever. Karango's TTL index takes numeric
seconds and works (`db/karango/KarangoAuthRecordsRepo.kt:84`).

Nothing is refused wrongly today: `AuthRecordStorage.hasNotExpired()` (`AuthRecordStorage.kt:344`) is the expiry boundary
on both backends. What is missing is the clean-up: the collection only grows, and expired one-shot token hashes stay
in it.

## How it can work on Mongo

- **A (recommended): Monko writes a Date shadow of the field.** For example, `ttlIndex { epochSeconds { it.expiresAt } }`:
  on every insert, save and replace, Monko adds a hidden field (e.g. `_ttl_expiresAt`) holding the same instant as a
  BSON Date, and builds the TTL index on that field with `expireAfterSeconds = 0`. The domain model stays one `Long`,
  shared with Karango, and every Monko user with a numeric expiry gets it for free.
  - It must be written on **every** write, not only on insert. `EmailAndPasswordAuth.kt:247` shortens `expiresAt` on
    a replace; the shadow has to follow.
  - Awaking must ignore the hidden field; the codec probably skips unknown keys already, but a test has to show it.
- B: change `expiresAt` to an `MpInstant` and make Monko store `MpInstant` as a BSON Date. This changes how every
  instant is stored on Mongo and breaks Karango's TTL, which needs numbers. Not recommended.
- C: a periodic sweep (`deleteMany { expiresAt < now }`) under a cluster lock. It works on both databases without
  index tricks, but it adds a scheduled job and a lock. It's the fallback if A fails.

## Decide before implementing

- [ ] **The clock.** The maintainer, 2026-10-19: "where a TTL index decides expiry, compute from the DB clock (a
      server-side `$$NOW` / `DATE_NOW()`), not the app server's." Two readings:
  - **A1:** the shadow is `expiresAt` as stored, an instant the app computed with its own clock; Mongo's TTL monitor
    then compares it with the database clock. One write, simple. Clock skew only moves the clean-up, because
    `hasNotExpired()` stays the boundary.
  - **A2:** the shadow is computed in the database: an update pipeline sets `$$NOW + (expiresAt - appNow)` in the same
    write. It follows the decision literally, but every Monko write path needs a pipeline variant.

  Recommendation: **A1**, because the TTL here is clean-up and not the boundary. If A2 is wanted, Karango gets the same
  treatment in this ticket, since its `expiresAt` also comes from the app clock today.

## The work

- [ ] Monko: the epoch-seconds TTL variant (the shadow on insert, save and replace, the index on the shadow), with KDoc.
- [ ] `MonkoAuthRecordsRepo` uses it, and the stale comment at `:71-74` goes.
- [ ] Rows written before the change have no shadow field. Nothing is live, so either do nothing and say so in the
      CHANGELOG, or backfill once with `updateMany` and `$toDate`. Decide while implementing; record which.

## Acceptance

- [ ] Monko spec on a real Mongo: a record with a past `expiresAt` is removed by the TTL monitor. The monitor runs
      every 60 s, so the test either waits it out with a bounded `eventually`, or asserts the index and the shadow's
      BSON type, plus one slow test that waits.
- [ ] A replace that shortens `expiresAt` moves the shadow too.
- [ ] Awaking a document with the shadow field works.
- [ ] Auth e2e on both backends (`MatrixTest2d`): an expired session record is gone after the monitor ran.
- [ ] Mutation-checked: drop the shadow write, shadow only on insert, index on the `Long` again. Each turns a test red.

## History

- 2026-11-08: created.
