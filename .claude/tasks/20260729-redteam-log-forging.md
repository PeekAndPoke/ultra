# Red-team: log forging and log-pipeline abuse via `ultra/log`

**Status:** COLLECTED — do NOT execute as part of normal feature work
**Plan:** raised from `.claude/tasks/20260729-log-scan-findings.md` → L1
**Security-critical:** yes

Scenarios to attempt in a dedicated penetration-test session. Nothing here has been exploited; the
underlying defect (unsanitised `$message` in `LogAppender.format:61`) is confirmed by reading, and the
enabling condition (multi-line records are normal, because `Log.error(message, e)` folds the stack
trace in with `"\n"` at `Log.kt:42`) is confirmed by probe.

## Why this is worth attempting

A log line is `"${ts.toLocalDate()} ${ts.toLocalTime()} $level - $name - $message"`. Nothing escapes
or strips newlines from `message`, and the library itself emits multi-line records, so a SIEM or
line-oriented collector has no way to tell an injected line from a genuine one. Log integrity is the
evidence base for incident response — forging it degrades every downstream investigation.

Two properties make this sharper than the usual CWE-117:

- `LogAppender.format` renders a **variable-width** timestamp (L6), so a forged line does not even
  need to match a fixed shape convincingly.
- Continuation lines carry no prefix at all (L15), so collectors already treat unprefixed lines as
  events of unknown level — an attacker does not have to forge a prefix to be believed.

## Scenarios

1. **Forge a complete log entry.** Find a request path that logs attacker-controlled text (an email,
   a User-Agent, a URL path, a form field, a failed-login identifier). Submit a value containing
   `\n2026-01-01 00:00:00 ERROR - io.peekandpoke.SomethingTrusted - <claim>` and confirm the collector
   parses it as a separate event attributed to a logger the attacker does not control.
2. **Forge an authentication outcome.** Same mechanism, aimed at the auth logs specifically — inject a
   line that reads as a successful login for another principal, or as a failed login for a principal
   the attacker wants locked out. Check whether any lockout, alerting or anomaly rule keys off log
   text rather than off the datastore.
3. **Bury a real event.** Inject a large multi-line payload immediately before or after a genuine
   security-relevant action so the real line scrolls out of a bounded view or a rate-limited shipper.
4. **Break the collector.** Inject text that is syntactically significant to the downstream parser —
   JSON fragments into a JSON encoder, `\r` for CR-injection, ANSI escapes into a terminal-rendered
   viewer, extremely long single lines to hit a truncation limit.
5. **Poison the logger-name cache.** `LogAppender.loggerNameLookUp` never evicts (L19) and
   `UltraLogManager.log(level, message, loggerName)` is public with an arbitrary `loggerName`. If any
   call path derives a logger name from request data, drive unbounded growth and measure heap.
6. **Crash the appender through the name.** `formatLoggerName` throws on an empty non-final segment
   (L5). If any path lets request data reach `loggerName`, send `"a..b.C"` and check whether the
   throw silently suppresses delivery to every later appender for that event (L3) — i.e. use the
   crash to blind a specific downstream appender.
7. **Silence logging through the race.** L2 drops events under concurrent `add()` + `log()`. Look for
   a path where an attacker can influence appender registration (a feature toggle, a per-tenant sink)
   and check whether load can be used to drop security-relevant events.
8. **Exploit the missing level gate.** L4 means `LogLevel.OFF` still reaches appenders. If any code
   uses OFF believing it suppresses output, check whether secrets are being written that the author
   thought were disabled.

## First mitigations to evaluate (not yet decided)

- Strip or escape `\r`/`\n` in `format` before interpolating `message`, and render the stack trace
  through a dedicated, clearly-delimited channel instead of string concatenation.
- Carry the `Throwable` on `LogAppender.append` (L8) so structured backends stop parsing text.
- A fixed-width timestamp (L6) so a real line has a shape a forged one must match.
