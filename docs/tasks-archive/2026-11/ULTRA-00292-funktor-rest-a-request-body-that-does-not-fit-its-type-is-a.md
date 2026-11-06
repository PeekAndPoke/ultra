# ULTRA-00292: funktor-rest: a request body that does not fit its type is a 400, and its content stays out of production answers

> **Status (2026-11-06): DONE.** gate PASS after round 1.** Approved by the maintainer 2026-11-06. Open for the maintainer: 1.2 (Slumber's date codecs) and whether a red-team ticket is wanted.
**Security-critical:** yes (cleartext passwords reached the log and the answer)

## Why this exists

Found in the ULTRA-00289 gate. A JSON body missing a required field (or with a wrong type, or not JSON at all)
makes Slumber throw `AwakerException`; funktor-rest does not translate it, so the answer is a **500**. Then:

- `ApiStatusPages` puts `cause.message` into the answer, **also in production**. Slumber's message carries
  `Input value: …`, the whole body: an activation request missing `displayName` echoes its cleartext password.
- `logInternalError` logs the throwable, so the same input lands in the server log, password included.
- A body that is not JSON at all: kotlinx's parse error quotes the input (`JSON input: …`), same two leaks.

Every body-bearing endpoint has this. It predates ULTRA-00289.

## Decisions (maintainer, 2026-11-06)

"It should be a 4xx error and the input should only be returned if the server is NOT a prod environment."

## The work

- `SlumberRestCodec.awakeBody` / `deserialize`: an `AwakerException`, or a body that does not parse as JSON, becomes
  `CouldNotConvertException` (mapped to 400). Its message names the type and the failing path only, never a value;
  the original exception is its cause, so a non-production answer (the stack trace) still shows the input.
- `ApiStatusPages`: the 400 answer carries the safe message in production, the stack trace outside it (as today).
  A 500 answer in production no longer carries `cause.message` at all: an unknown exception's message may hold
  input, and fail-closed is the rule `exposesStackTraces` already follows.
- No 400 is logged as an internal error.

## Implementation notes

- `SlumberRestCodec.awakeBody` catches `AwakerException` and throws `CouldNotConvertException` with the first line
  of Slumber's message (the path), capped at 300 chars; `parseBody` does the same for kotlinx's
  `SerializationException`. Slumber names the OBJECT a field is missing from (`'root'`), not the field.
- `ApiStatusPages.errorDetail(config, cause, internal)` is the one place that decides what an error answer says:
  the stack trace outside production; in production the message of a client error, nothing of an internal one.
- **Not changed:** the URI-param converters (`javatime.kt`, `mpdatetime.kt`) quote the bad value in their
  `CouldNotConvertException`, so a production 400 still echoes a path or query value. That value is in the URL,
  hence already in every access log; not a body secret. A value class's own `require` message also still reaches
  the answer, as before: it is the developer's text.
- **Not mutation-covered:** that the 500 branch passes `internal = true`. The test apps run in `test`, where both
  branches answer the stack trace; `errorDetail` itself is pinned by `ApiStatusPagesSpec`.

## Test evidence

- `SlumberRestCodecSpec`: missing field, wrong-typed field, not JSON; the marker secret never in the message, but in
  the cause's stack trace. `ApiStatusPagesSpec`: `errorDetail` in prod, unknown and dev. `RequestBodyErrorsSpec`
  (funktor-all): the three bodies through the real sign-in endpoint answer 400. `AccountActivationEmailE2eSpec`:
  an activation without `displayName` answers 400 and its link survives.
- Green: funktor-rest 120/0, funktor-all 249/0.
- Mutation-checked (2026-11-06), each red in its test: `AwakerException` not translated (codec spec ×2,
  `RequestBodyErrorsSpec` ×2, the activation e2e); non-JSON not translated (codec spec, `RequestBodyErrorsSpec`);
  the whole Slumber message kept (codec spec); a prod 500 says its message (`ApiStatusPagesSpec`).
  Round-1 fixes: map paths kept (codec spec, the map row); the whole message on a wrong type (codec spec ×2).
  funktor-rest 121/0 after them.

## Acceptance

- [x] A body missing a required field, a wrong-typed field, a non-JSON body: 400 on both body-bearing route kinds,
      for every rejection Slumber raises as `AwakerException`. Not yet for the date codecs that throw on their own
      (round 1, 1.2): maintainer decision.
- [x] In production, the answer holds no input value (asserted with a marker value in the body); outside it, it may.
- [x] Nothing of the body reaches the log.
- [x] A 500 in production carries no `cause.message`.
- [x] Mutation-checked (security-relevant, codec).

## Review record (the ledger)

### Round 1: level 1 (`reviewer-high`, all three charters): CLEAN (0 CRITICAL, 0 MAJOR)

| # | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1.1 | MINOR (security) | `MapAwaker.kt:64-65`, `SlumberRestCodec.kt:134` | A map's raw key is part of Slumber's path, so a production 400 echoes it: `{"attrs":{"<secret>":null}}` | fixed | Verified. A key with dots cannot be told from field names, so a path through a map (`[KEY]`/`[VAL]` in it) is dropped: "a value inside a map is missing or of the wrong type". Spec + mutant U1 |
| 1.2 | MINOR (domain) | `MpLocalDateCodec.kt:27`, `MpZonedDateTimeCodec.kt:25`, `MpLocalDateTimeCodec`, `ZonedDateTimeCodec.kt:30` | These awakers throw NPE, ClassCastException or ZoneRulesException on malformed input, not `AwakerException`: still a 500 | maintainer-decision | Verified by the reviewer against the code. No body secret leaks (a prod 500 says nothing now; at most a zone id in the log). The fix belongs in Slumber (battle-tested; `as? String`, validate the zone as `ZoneIdCodec` does), and changes what a corrupt DB value does to a nullable field. Widening the codec's catch is wrong: a 500 from a DB-backed awaker must stay one |
| 1.3 | MINOR (security) | `ApiStatusPages.kt:60,96` | `withCause(..., internal = false)` as a default fails open: dropping `internal = true` compiles and no test goes red | fixed | The parameter has no default now; both call sites name it |
| 1.4 | NIT | `SlumberRestCodecSpec.kt` | The wrong-typed test cannot go red for the leak: the secret was a sibling of the failing node | fixed | The secret sits inside the failing node; mutant U2 |
| 1.5 | NIT | `SlumberRestCodec.kt:107-125` | The `awakeBody` KDoc still says only constructor failures are translated | fixed | KDoc names `AwakerException` and `safeAwakerMessage` |
| 1.6 | NIT | various | Residuals: a stdlib exception in an `init` (e.g. `toInt()`) would quote input through the rejected-value branch; the prod 500 text holds the URI, query included; no red-team ticket; a deeply nested array may overflow `JsonUtil.unwrap` (pre-existing) | recorded | No such `init` exists today; the URI is in every access log; the red-team ticket is the maintainer's call (a new ticket); the overflow predates this |

**Probed and CLEAN in round 1:** both body routes (`routing.kt:267,299`) go through the codec, SSE/plain/params routes
read no body, nothing else decodes request input with `RestCodec`; every `AwakerException` throw site (only
`reportNullError` and `RedactedAwaker`) is caught; unknown or missing polymorphic discriminators, unknown enums and
out-of-range numbers all end as `AwakerException`, so 400; `AwakerException` extends `Throwable`, and its catch sits
before the `Throwable` one; not-JSON is `JsonDecodingException`, caught, fixed text; nothing logs the body (the 400
branch does not log, insights record no body); nothing reads a 500's `cause.message`; `errorDetail(null, …)` fails
closed; code style.

Round-1 fixes are MINOR and NIT only, so they do not loop.

**Verdict:** gate PASS (1.2 waits for the maintainer; it leaks nothing).

## History

- 2026-11-06: created on the maintainer's approval.
- 2026-11-06: built, round 1 CLEAN, its MINORs fixed; DONE. No docs follow-up: no public API changed beyond the
  behaviour the CHANGELOG names.
- 2026-11-06: DONE.
