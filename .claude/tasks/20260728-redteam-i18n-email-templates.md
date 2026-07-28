# Red-team — auth email templates (rendered in code)

**Status:** COLLECTED — NOT EXECUTED
**Feature:** `.claude/tasks/20260728-auth-email-templates-in-code.md`
**Rule:** these are attack scenarios for a dedicated penetration-test session. Do NOT run them as
part of normal feature work.

## What changed, and why this file shrank

An earlier version of this collection targeted a Markdown-template pipeline: front-matter parsing,
`{{placeholder}}` substitution, regex-based HTML escaping, a URL scheme allowlist, and classpath
lookup by a locale derived from user input. **That design was dropped on 2026-07-28** in favour of
rendering each email in Kotlin with the kotlinx.html DSL.

The following scenarios are **retired because the mechanism no longer exists** — do not spend time on
them:

- HTML/attribute injection through substituted values, escaper breakout, single-quoted or unquoted
  attribute contexts — kotlinx.html escapes structurally; there is no substitution step.
- URL scheme allowlist bypass (`javascript:`, entity tricks, control-character splitting) — there is
  no sanitizer, because there is no attacker-authored URL context.
- Path traversal / resource disclosure via `language.messaging` — nothing builds a resource path from
  a locale any more; locales select a function from a `Map<Locale, …>`.
- Front-matter parse failures reaching send time — templates are compiled code.
- Mail-header injection through a substituted subject — subjects are Kotlin string literals with
  interpolated config values.

What remains is narrower and mostly concerns the seams AROUND the templates.

## Scenarios

### 1. Persist a live token (defeat the anonymizer)

Still the highest-value target. The activation and recovery mails carry live single-use tokens;
`EmailStoring.withAnonymizedContent` strips links from the PERSISTED copy via
`href="[^"]*"` plus a bare-URL regex (`funktor/messaging/src/jvmMain/kotlin/storage/EmailStoring.kt`).

- kotlinx.html always double-quotes attributes, so the `href` pass should always match — **verify
  that claim** rather than assuming it, including for attributes written via `attributes[...] = ...`.
- The bare-URL regex requires `//`. With a relative `FrontendUrls(baseUrl = "/auth")` the URL is
  `/auth/email-password/activate/<token>` — no `//`. If an app's custom template ALSO prints the URL
  as copy-paste text (a routine thing to do for token mails), does anything strip it?
- **An app-supplied template is the realistic vector.** `AuthEmailTemplates` lets an app replace any
  of the three renderings. Write an override that leaks its own token into storage — e.g. token in a
  `<base>` tag, in `srcset`, inside an HTML comment, percent-encoded, or split across two attributes.
- The SUBJECT now goes through the storing policy too (fixed in the gate). Attack the fix: is
  `modifyContent` enough for a subject, or can a token be encoded past both its regexes?
- **Attachments still bypass the policy entirely** — `StoringEmailHook` persists `email.attachments`
  verbatim. No auth mail has one today; an app template could attach an `.ics` or PDF carrying the
  link.

### 2. Abuse the template override seam

- `requireAuthEmailEnvelope` now pins the recipient (fixed in the gate): exactly one `to`, no cc, no
  bcc. Attack the fix — can a template still get the mail delivered somewhere else, e.g. via `source`,
  a provider-specific attribute, or an address that compares equal but routes differently?
- Can an app template mutate `Email.attributes` to change downstream sender behaviour (idempotency
  key collisions, provider-specific attributes)? `withIdempotencyKey` is consumed by `SendgridSender`;
  a constant key would collapse every activation mail into one delivery.
- A template that throws is now caught and degraded to a failed `EmailResult` (fixed in the gate) —
  **but that catch has no test**. Verify it actually holds on all three paths, especially resend,
  where the previous activation link is revoked before the render runs.

### 3. Locale as a lever

- `authEmailLocales` runs `Locale.parse` on `language.messaging`, free-form text a profile UI will
  eventually let users write. No path is built from it any more, but confirm nothing else
  interpolates it (logs, metrics labels, provider metadata).
- Does the chosen locale change timing, response, or error output in a way that reveals whether an
  account exists?
- Extremely long or adversarial language tags — any unbounded work in `Locale.parse` or in map
  lookup?

### 4. The send path itself (unchanged by this feature, still worth a pass)

- The activation-resend endpoint is authorized by a single-use `AuthRecord.ActivationResendToken`
  minted only on the path that proves the password. Read
  `.claude/tasks/20260727-activation-resend-and-frontend.md` first — several obvious attacks are
  already closed there, and its Review record explains why.
- `.claude/tasks/20260727-activation-resend-cooldown-atomicity.md` records a known check-then-act on
  the cooldown.

## Adjacent, pre-existing, NOT caused by this feature

`ultra/remote/src/commonMain/kotlin/helpers.kt:9` — `uriToParamsCache` is an unsynchronized
`mutableMapOf` mutated via `getOrPut` from concurrent request threads, and it sits on the
activation/reset link-building path. Deserves its own task.

## Probed during the 2026-07-28 gate and NOT reproducible

Recorded so a tester spends their time elsewhere. All verified against the shipped code:

- **Anonymizer bypass via link shape — failed.** `EmailStoring`'s two regexes were run against real
  kotlinx.html output for: the default `de` mail, a URL containing `&`, a URL containing `"`, a
  `myapp://` custom scheme, a protocol-relative URL, a bare URL in text position, and an attempted
  attribute-quote breakout. All clean. kotlinx-html-jvm 0.12.0's `HTMLStreamBuilder.onTagStart` writes
  every attribute double-quoted and escapes `"` inside it, so the `[^"]*` bound cannot be split, and
  `prettyPrint` never breaks a line inside an attribute. This is a structural improvement over the
  reverted design, where quoting was the template author's choice.
- **Token in a subject via the framework defaults — not reachable.** All subjects are
  `"${applicationName}: <constant>"`. The subject is now also run through the storing policy, so even
  an app template that interpolates a URL there no longer persists it.
- **No user-controlled data reaches the HTML body.** Every value in the DSL — `senderName`,
  `applicationName`, `senderEmail` — is app config; `activationUrl`/`resetUrl` are framework-built by
  `buildUri`. `displayName` is in no `Params`. **This is the single strongest property of the design.
  The day someone adds `displayName` for a "Hello $name!" greeting, re-open this file.**
- **`Locale.parse` on hostile input — no reachable sink.** It feeds map lookups only; the `lang`
  attribute comes from the MATCHED locale (a template key), never from user text. No resource path, no
  interpolation, no regex.
- **ReDoS / unbounded work — none found.** `Locale.parse` is a `split(limit = 2)` plus case folding.
  The chain is bounded at `2·preferred.size + 2`. Both `EmailStoring` regexes are linear.
- **`language.messaging` is not writable through any funktor endpoint today** — it is only read.
  Hostile-locale input needs an app-exposed profile setting first.
- **U+2028/U+2029/U+0085 in a subject** — both senders encode (SES sends JSON;
  `MimeMessage.setSubject(s, "UTF-8")` RFC-2047-encodes non-ASCII).
- **`.store()` cannot be chosen by a template** — `TypedAttributes.plus` is last-write-wins and
  `DefaultMessaging` writes after the template.
- **No `unsafe {}` anywhere in the auth/messaging email path.**

## Still open, recorded rather than fixed

- **Attachments bypass the storing policy.** `StoringEmailHook` persists `email.attachments` verbatim
  with no anonymization. No auth mail has one today, so this is latent — but an app template could
  attach an `.ics` or PDF carrying the activation link.
- **kotlinx.html 0.12.0 `escapeAppend` hatch:** a backslash immediately before `&` emits a RAW,
  unescaped `&`. It cannot produce a raw `<`, `>` or `"`, so it is not an XSS or anonymizer-breakout
  primitive, and nothing attacker-controlled reaches the DSL today. Re-check the day user data enters
  these templates.
- **The `try/catch` around `render()` has no test.** It is the control that stops an app template's
  exception from 500-ing sign-up after the account is written (and from handing
  `recoverAccountInitPasswordReset` an enumeration oracle). Verified by inspection only — a test needs
  a realm with full `AuthSystem.Deps` plus a deliberately throwing template.
