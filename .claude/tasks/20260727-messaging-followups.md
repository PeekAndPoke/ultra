# Messaging follow-ups from the email review gate

**Status:** TODO — collected 2026-07-27, none started
**Source:** the 3-agent gate on `.claude/tasks/20260727-framework-composed-email-sender.md`
**Security-critical:** items 1 and 2 are; the rest are correctness or ergonomics.

Everything here was CONFIRMED against the code during the gate and deliberately not fixed in that
change, either because it is pre-existing and separable or because it belongs with account
activation. Ordered by value.

## 1. Boot-time warning when mail is silently not going anywhere

Three distinct configurations make mail vanish with no signal at all, and the sender reports
`success = true` in every one:

- **`environment = "test"` in production** — the module substitutes a capturing sender, so mail is
  now not merely discarded but RETAINED: up to 1000 full bodies, including live reset tokens, in a
  long-lived JVM heap with no TTL. One heap dump (an OOM artifact, a crash report shipped to an error
  tracker) yields them all.
- **`environment = "dev"` with no `devOverrides`** — `application.dev.conf` still does
  `include "dev.overrides.conf"`, a **gitignored** file. Without it `applyDevConfig` is a no-op and a
  local run mails real addresses from the shared database. This is the *same* gitignored-file
  dependency that caused the original incident, one environment over.
- **Production with no `useSender`** — falls back to `NullEmailSender`, which discards everything and
  reports success.

Per the standing rule that a check aborting or warning at boot must state the concrete FIX, not just
what is wrong. The messaging module knows all three at composition time.

## 2. Anonymization covers LINKS, not SECRETS — and only the body

`EmailStoring.withAnonymizedContent` is the control keeping live tokens out of `system_sent_messages`,
which the sent-messages inspector shows to support staff. It now strips hrefs and bare URLs
(case-insensitively, any scheme). It does NOT cover:

- **A token that is not a URL.** A 6-digit activation code or OTP printed as text is stored verbatim.
  This lands directly on the activation feature if it uses a code rather than a link.
- **A URL split across lines.** `bareUrlRegex` is whitespace-bounded, so a plain-text mail wrapped at
  72 columns leaves the token fragment behind as a bare word.
- **Anything outside `body`.** `StoringEmailHook` persists `subject`, `refs`, `tags` and attachments
  raw, never through `modifyContent`. Nothing leaks today (`AuthRealm` puts only `user._id` and the
  address into refs), but the policy offers no protection if a future mail puts a code in the subject
  or a token-bearing PDF in an attachment.

Suggested direction: an explicit `WithContent.redacting(secret)` taking the value the caller already
holds, rather than trying to pattern-match secrets. Rename or re-document so callers know the scope.

## 3. `senderName` never reaches the `From` header

`AuthRealm.DefaultMessaging` takes `senderName` and uses it ONLY in the body sign-off
(`AuthRealm.kt:89,127`). `Email.source` is the bare address, and both providers pass it through as
one (`AwsSesSender.kt:85,139`; `SendgridSender.kt:80`). Every auth mail from every realm arrives with
no display name — a measurable deliverability and trust penalty, and four realms configure a value
that does nothing.

Needs a first-class `Email.sourceName: String?` mapped per provider, NOT string concatenation: SES's
`.source()` accepts `"Name <addr>"` but SendGrid's `Email` takes name and address separately.

## 4. Sender identity is a code literal, and one of them is wrong

All four demo realms hardcode `"treore@jointhebase.co"` — a personal-looking address on a domain
unrelated to the app, duplicated 4×, with nothing tying it to the SES verified identity or DKIM
domain. Changing the sending domain, or having staging send from a different one, is a four-file code
change.

The narrow fix: `funktorMessaging` supplies a config-sourced DEFAULT source address, applied when
`Email.source` is blank; realms keep display name and application name and may override the address.
Do not centralise branding — a realm genuinely is a branding boundary.

**Constraint worth recording rather than fixing:** `AuthRealm.Messaging` is a realm-level `val` with
no org parameter, and password reset happens BEFORE org selection, so for an `OrgPolicy.Required`
realm the org is not known at send time. Per-org white-labelled sender identity is not expressible in
this shape at all. That is fine now; it is the thing that must change first when B2B2C wants
tenant-branded mail.

## 5. No `OnBeforeSend` hook — activation resend has nowhere to put a throttle

`EmailHooks` has only `OnAfterSend`. There is no point in the chain at which a send can be
suppressed, so suppression lists, per-address throttling and send-idempotency cannot be expressed.
"Resend activation email" is a textbook mail-bombing vector — hit in a loop against a victim address
— and there is currently no place to put the defence. Needed BEFORE activation resend ships.

## 6. The provider layer has zero tests

`AwsSesSender` and `SendgridSender` have no test at all, and test mode now guarantees they are never
constructed — so the whole provider layer is unreachable from any test. `prepareWithAttachments` is
the only consumer of the `com.sun.mail:jakarta.mail` dependency; if a consumer excludes or downgrades
`jakarta.activation`, `javax.activation.DataHandler` throws `NoClassDefFoundError`, which
`catch (error: Throwable)` converts into a failed-but-successful-looking send.

A pure unit test over `prepareWithAttachments` (no SES client needed) asserting the MIME parts,
recipients and attachment would also pin the jakarta.mail contract the dependency comment argues for.

## 7. `funktor-demo` has no test that triggers a mail flow

Its own composed chain — the `config.devOverrides?.mailing` plumbing, the SES config path, the four
realm identities — is unverified end to end. The framework-level tests cover the composition; nothing
covers the demo's use of it.

## 8. Smaller, confirmed

- **An app can override `Mailing` itself** and lose the whole composition. The design doc's "THE
  composition site, and deliberately the only one" is true of everything except that. Either pin it
  with a test or document it as unsupported.
- **`MailingOverride.None`** has no callers (dead).
- **`RealmId` does not use `isForbiddenInId`**, though the relocated KDoc in `ultra:common` now claims
  every id type does. `RealmId` uses a stricter allowlist, so this is a doc inaccuracy, not a hole —
  and it is the concrete case behind the denylist-vs-allowlist question parked in
  `.claude/tasks/20260724-value-class-ids-migration.md` Step 5.

## Out of scope here, recorded elsewhere

- `LogCollector.Appender` (funktor:insights) has NO minimum level, and the insights GUI routes have no
  auth floor — so any DEBUG line is persisted to disk and served unauthenticated. The messaging change
  no longer feeds it mail bodies, but the underlying exposure is real and belongs to insights.
- `SentMessage.Lookup` folds recipient addresses into `refs`, and `findByRefs` is un-tenanted →
  recorded as a red-team scenario in `.claude/tasks/20260727-redteam-messaging.md`.
