# ULTRA-00300: Database driver exception messages carry query input into logs and insights

> **Status (2026-11-10): DESIGNED.** The maintainer, 2026-11-10: "keep a ticket for that, we need to review the
> security implications". Step one is that review; the fix is decided after it.
>
> **Part of:** ULTRA-00253 (Epic: Insights)

**Plan:** none. Found in ULTRA-00228 (insights minors and follow-ups), where it was the one open question.
**Security-critical:** yes (secret disclosure). Red team: a scenario in ULTRA-00161 once the review names the paths.

## Why this exists

ULTRA-00228 made Monko's query printer bind a document keyed by a password hash as one value, so the query text in an
insights record no longer shows the hash. The driver does not do the same for its own errors. When Mongo rejects a
query, for example `unknown operator: $2b$10$…` for a hash used as a key, the exception message quotes that input.
If the app lets the exception escape:
- the app's error log writes the message, and the stack trace outside production;
- the insights `log` slice records the formatted log line (`collectors/LogCollector.kt`, `LogAppender.format`);
- outside production the error answer carries the stack trace (`ApiStatusPages.errorDetail`, `funktor/rest`).

The same may hold for ArangoDB (an AQL error quoting a bind value) and for other drivers.

## Decide before implementing (the security review)

- [ ] Which paths carry a driver exception message: the app log, the insights `log` slice, the error answer, the
      logging module's persisted log (`funktor/logging`, Karango or Monko backed), and anything else. Production and
      development separately: in production insights is off (ULTRA-00225), but the app log is not.
- [ ] Which inputs reach a driver message in practice: Mongo's `unknown operator`, a duplicate-key error (it quotes
      the key value, so a unique index on a token hash or an email would print it), AQL parse and bind errors.
- [ ] Who can trigger it: an anonymous caller who controls the input (a lookup by a hash it supplies) is worse than a
      superuser.
- [ ] Then pick: catch-and-rewrap at the repository boundary, a redacting log formatter for known driver exceptions,
      or treat the log as sensitive and keep it out of insights. Record the reason.

## The work

To be written after the review.

## Acceptance

- [ ] E2e: a query that the driver rejects with the input in its message leaves no trace of that input in the app
      log capture, the insights record, or the error answer, on both backends where both apply.
- [ ] Mutation-checked.

## History

- 2026-11-10: created.
