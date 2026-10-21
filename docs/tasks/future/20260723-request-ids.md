# Request IDs — per-request correlation ids through kontainer + logs

**Status:** FUTURE — not scheduled.
**Area:** funktor core (request pipeline, kontainer wiring, logging).

## Idea (user, 2026-07-23)

Attach a **random request id** to each (per-request) kontainer, and additionally **pick up upstream
ids** from proxies/CDNs — e.g. Cloudflare `cf-ray`, or standard `X-Request-Id` /
`traceparent` — keeping a **set of ids** per request (our own + all upstream ones). Add the ids to
every log line produced while handling the request.

## Sketch

- A small value object registered in the request kontainer, e.g.
  `RequestIds(own: String, upstream: Set<String>)` — generated/collected in the funktor request
  pipeline before anything else runs, injectable everywhere (services can just take it as a dep).
- Generation: random, collision-resistant, cheap (e.g. 128-bit random, base62). NOT a security
  token — no need for the secure generator, but must not be guessable enough to invite confusion.
- Upstream pickup: configurable header allowlist (`cf-ray`, `X-Request-Id`, `X-Amzn-Trace-Id`,
  `traceparent`, ...). Treat as untrusted input: length-cap and charset-sanitize before storing or
  logging (log-injection guard).
- Logging: the ultra `Log` for the request scope carries the ids (either an MDC-like context or a
  wrapped per-request `Log` instance from the kontainer) so every line is correlated.
- Response: echo our own id as a response header (`X-Request-Id`) so users/support can reference it.
- Later consumers: funktor:insights/logging UIs can filter by request id; the entity-mutation
  tracking idea ([[20260723-entity-mutation-tracking]]) attaches these ids to mutation records.

## Open questions (for the design pass)

- Where exactly in the pipeline does the kontainer get the instance (funktor request lifecycle hook)?
- One `Log` decoration vs a context mechanism — kraft/funktor logging has no MDC today.
- Do background jobs/workers get their own "request" ids (job ids) for the same correlation story?
