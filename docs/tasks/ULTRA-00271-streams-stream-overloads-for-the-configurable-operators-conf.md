# ULTRA-00271: Streams: stream overloads for the configurable operators (config is a stream)

> **Status (2026-10-29): DESIGNED.** The maintainer's rule 2026-10-29 (answer "Rule + stream overloads"): config values are streams; existing operators get stream overloads beside the constant ones. Follows ULTRA-00264 and ULTRA-00267.

**Security-critical:** no

## Why this exists

The maintainer, in Klang-Research's session (relayed 2026-10-28): "The streams should be like patterns ... when there
is something to be configured on a stream the config value itself needs to be a stream ..." ("like patterns": sprudel,
where every argument of a pattern function is itself a pattern). Confirmed for Ultra on 2026-10-29: a rule for new
operators, and stream overloads for the existing ones, beside the constant forms, so nothing breaks.
ULTRA-00264 (`smooth`'s law) and ULTRA-00267 (`sampled`'s interval) are the first.

## Decide before implementing

1. **Where the rule is written down.** There is no streams skill yet (ULTRA-00134 plans the library know-how skills).
   Recommendation: a module `ultra/streams/CLAUDE.md` with the rule (module facts live there, as in
   `ultra/kontainer/CLAUDE.md`); a CLAUDE.md change, so it waits for the maintainer's OK of the before/after.
2. **Semantics of a config change** per operator, decided case by case and written in each KDoc:
   - `smooth(rest, near)`: a new `near` applies from the next frame; a new `rest` applies to the next start from `null`.
   - `quantize(size, offset)`: a change re-snaps the current value at once.
   - `debounce(delay)`: a change applies to the next value; a pending one keeps its delay.
   - `history(capacity)`: a smaller capacity drops the oldest at once; a larger one keeps what there is.
   - `fallbackTo(fallback)`: a change re-emits when the source is `null`.
   - `ticker(interval)`: a change applies from the next frame.
   - `distinct(strict)`: stays a constant (a mode, not a value over time). Recommendation: yes.
3. **The constant overloads** delegate to the stream ones through `steady(x)`, as `smooth` does since ULTRA-00264.

## The work

1. One commit per operator: the stream overload, the constant one delegating, specs (commonTest, `HandClock` where
   time matters) for a config change while listening, and that the config stream is listened to only while someone
   listens.
2. Docs: each operator's entry on the streams pages and the LLM mirror, plus one line on the rule in
   `streams/extending` ("a configurable operator takes its config as a stream").
3. CHANGELOG.

## Acceptance

- Every operator of decision 2 except `distinct` has a stream overload; all constant call sites compile unchanged.
- `:ultra:streams:allTests` green on JVM, JS and native.

## History

- 2026-10-29: created on the maintainer's answer "Rule + stream overloads".
