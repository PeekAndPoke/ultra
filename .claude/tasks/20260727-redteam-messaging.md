# Red-team: the messaging / sent-messages surface

**Status:** COLLECTED 2026-07-27 — NOT executed. A dedicated penetration-test session runs these.
**Feature:** `.claude/tasks/20260727-framework-composed-email-sender.md`
**Why:** mail is how password reset and (soon) account activation deliver their tokens, and every mail
is persisted. The store is the part with a read path.

## 1. Un-tenanted `findByRefs` (HIGHEST VALUE)

`SentMessage.Lookup.of` (`funktor/messaging/src/jvmMain/kotlin/storage/SentMessage.kt:29-39`)
automatically folds every `toAddresses` entry into the lookup key set, and `findByRefs` has no org
dimension and no caller-identity check.

Nothing exposes it today — the demo's `getSentMessages` passes `refs = emptySet()`, which both repos
short-circuit to an empty cursor. The scenario is therefore about the NEXT endpoint:

- Write (or find) a handler that forwards a user-supplied address into `findByRefs`, and read another
  tenant's stored mail: recipient, subject, timestamps, anonymized body.
- Confirm whether org isolation is enforceable at all on this API as it stands, or whether it needs an
  org dimension on `SentMessage`.
- Try the same with a `refs` value that is a `_id` from another collection.

## 2. Retention as an oracle

`CapturedEmails` retains up to 1000 full bodies when `config.ktor.isTest`. In a correctly configured
production app nothing writes to it.

- Stand up an instance misconfigured as `environment = "test"`, drive password resets, then obtain a
  heap dump by every route available (JMX, an actuator-style endpoint, an OOM artifact, a crash report
  shipped to an error tracker) and confirm how many live tokens fall out.
- Establish whether any deployment path can set `environment` from an env var an attacker influences.

## 3. Anonymization evasion

`EmailStoring.withAnonymizedContent` strips hrefs and bare URLs, case-insensitively, any scheme.
Attempt to get a live secret into `system_sent_messages` anyway:

- a URL wrapped across a line break in a plain-text body (known gap);
- a token that is not a URL at all — an OTP or activation code printed as text (known gap);
- a token in the SUBJECT, in `refs`, in `tags`, or inside an attachment — none pass through
  `modifyContent` (known gap);
- HTML entity or percent encoding of the scheme (`&#104;ttps://`, `https%3A//`);
- an `<a>` whose href is assembled by the mail client (`data-*` attributes, `<base href>`).

Then confirm who can actually read the store — the sent-messages inspector audience.

## 4. Mail-bombing via resend (blocks on activation)

There is no `OnBeforeSend` hook, so nothing in the chain can suppress or throttle a send.

- Once "resend activation email" exists, hit it in a loop against a victim address and measure how
  many mails leave. Repeat against an address that does not exist (does the neutral response still
  hold, and does it still cost a send?).
- Check whether `letTheBotsWait()`'s jitter is the only rate limit, and what it actually costs an
  attacker.

## 5. Enumeration through the send path

Complements §5 of `.claude/tasks/20260726-redteam-value-class-emailaddress.md` (timing). New angle:
now that hooks are awaited inside `send`, a persisted-mail write is on the request path for a KNOWN
address and absent for an unknown one.

- Measure whether the storing write widens the known/unknown timing gap enough to be usable, and
  whether an unreachable sent-messages database changes the response shape.

## 6. Provider-layer failure handling

`AwsSesSender` converts any `Throwable` into `EmailResult.ofError` — a failed send that still returns
a result object.

- Force provider failures (bad credentials, throttling, a missing `jakarta.activation` on the
  classpath) and confirm what the caller sees, what gets persisted, and whether a user-visible flow
  (password reset) reports success while no mail was sent.
