# Email end-to-end test seam — capture outgoing mail and follow the token in it

**Status:** DONE 2026-07-27 (implemented 2026-07-26, gate passed 2026-07-27)

> **SUPERSEDED 2026-07-27 (same day) by `.claude/tasks/20260727-framework-composed-email-sender.md`.**
> The seam itself stands, but two things described below no longer exist. `EmailSender.canDeliver`
> was removed: instead of asking a chain whether it delivers, the framework now substitutes a
> capturing sender in test mode, so there is nothing to ask. And the app no longer composes the
> decorator chain at all — `useSender { provider }` is its whole contribution — which also retired
> the `TestOutbox` holder and `suppressDeliveryInTestMode`. Read the doc below for WHY the guard
> exists; read the newer one for how it works now.
**Plan:** none — prerequisite for account activation (`.claude/tasks/20260726-sso-email-verification.md`)
**Security-critical:** yes — one half is a fail-closed guard on whether a test run can reach a real
email provider; the other half is the first test that asserts a password-reset mail is sent at all.



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `40a57e17` | 2026-07-27 | 22 | feat(messaging): compose the email chain in the framework; capture mail in tests |

### Files changed (22)

**docs-site/src**
- `docs-site/src/pages/ultra/funktor/messaging.astro`

**funktor-demo/server**
- `funktor-demo/server/src/main/kotlin/kontainer.kt`

**funktor/all**
- `funktor/all/src/jvmMain/kotlin/funktor.kt`
- `funktor/all/src/jvmTest/kotlin/FunktorApiSpec.kt`
- `funktor/all/src/jvmTest/kotlin/PasswordResetEmailE2eSpec.kt`

**funktor/auth**
- `funktor/auth/src/jvmTest/kotlin/index_jvmTest.kt`

**funktor/messaging**
- `funktor/messaging/src/jvmMain/kotlin/index_jvm.kt`
- `funktor/messaging/src/jvmMain/kotlin/overrides.kt`
- `funktor/messaging/src/jvmMain/kotlin/senders/CapturedEmails.kt`
- `funktor/messaging/src/jvmMain/kotlin/senders/CapturingEmailSender.kt`
- `funktor/messaging/src/jvmMain/kotlin/senders/ConfiguredEmailSender.kt`
- `funktor/messaging/src/jvmMain/kotlin/senders/ExampleDomainsIgnoringEmailSender.kt`
- `funktor/messaging/src/jvmMain/kotlin/senders/senders.kt`
- `funktor/messaging/src/jvmMain/kotlin/storage/EmailStoring.kt`
- `funktor/messaging/src/jvmTest/kotlin/FunktorMessagingBuilderSpec.kt`
- `funktor/messaging/src/jvmTest/kotlin/MailingOverridesSpec.kt`
- `funktor/messaging/src/jvmTest/kotlin/MailingTestModeGuardSpec.kt`
- `funktor/messaging/src/jvmTest/kotlin/index_jvmTest.kt`
- `funktor/messaging/src/jvmTest/kotlin/senders/ApplyDevConfigSpec.kt`
- `funktor/messaging/src/jvmTest/kotlin/senders/CapturedEmailsSpec.kt`
- `funktor/messaging/src/jvmTest/kotlin/storage/EmailStoringSpec.kt`

**funktor/testing**
- `funktor/testing/src/jvmMain/kotlin/setup.kt`

## Why

Two problems, one fix.

1. **Test mode could send real email.** `applyDevConfig` checked `config.ktor.isTest` INSIDE the
   `devConfig ?: return this` bail-out, so mail suppression in tests depended on the developer having
   a `dev.overrides.conf` — a **gitignored** file. Removing `include "dev.overrides.conf"` from
   `application.test.conf` earlier this session (part of separating the test databases) made that
   latent design smell reachable: a machine without the file, or CI, would hand the real
   `AwsSesSender` to the test suite.
2. **No email flow was ever tested through the email.** Every piece of password recovery had unit
   tests and the flow as a whole was only assumed to work, because nothing asserted that a mail is
   sent at all — let alone that the token inside it is the one the server accepts. `SentMessagesStorage`
   cannot serve as the oracle: auth mails are stored with `EmailStoring.withAnonymizedContent`, which
   rewrites every `href` to `#anonymized` before persisting. That is correct for a database and
   useless for a test.

Password reset is the proving ground on purpose — it is the one email flow that already works end to
end (real producer, real mail, real consumer), so a red test means the seam is wrong, not the feature.

## Spec

- [x] `funktor/messaging/.../senders/senders.kt` — hoist `if (config.ktor.isTest) return NullEmailSender()`
      ABOVE the `devConfig` null-check. Test mode never sends, unconditionally.
- [x] `funktor/messaging/.../senders/CapturingEmailSender.kt` — an in-memory `EmailSender` that
      records and delivers nothing. Drop-oldest bound at `MAX_CAPTURED = 1000`, synchronized (ktor
      test requests are not single-threaded), `clear()` / `capturedTo()` / `lastTo()`.
- [x] `Email.hrefs()` in the same file — the links in a mail body, with `kotlinx.html`'s attribute
      escaping undone in ONE pass (`&amp;` decoded separately from the rest would turn `&amp;lt;`
      into markup).
- [x] Registered in the `funktor:all` test blueprint, reachable as `FunktorApiSpec.capturedEmails`.
- [x] `PasswordResetEmailE2eSpec` — the whole flow, driven through the HTTP API.

## Implementation notes

- **Registered ONLY under `EmailSender`** (`funktor/all/src/jvmTest/kotlin/index_testJvm.kt`).
  Adding a second registration under the concrete type gives `SimpleMailing(sender: EmailSender)` two
  assignable candidates and kontainer rejects the entire blueprint as ambiguous — this was a real
  failure, not a hypothetical. Hence the documented cast in `FunktorApiSpec.capturedEmails`.
- **Zero constructor dependencies, deliberately.** Kontainer promotes a singleton that injects a
  dynamic service to `SemiDynamic` — one instance per kontainer, i.e. per request. Mails captured
  while handling a request would then land on an instance the test can never see. KDoc'd on the class.
- **`linkParam(name)` was written and then deleted.** The reset link is
  `/auth/{provider}/reset-password/{token}` — the token is a PATH segment, not a query parameter, so
  a query-param helper would have shipped with no caller. (Same shape as the dead vault helpers the
  user flagged; not repeating it here.)
- The `isTest` early return drops the `ignoreExampleDomains` / `withOverrides` wrappers, which is
  behaviour-neutral: they only rewrite recipients on a sender that already discards everything.

## Test evidence

- [x] `funktor/messaging/src/jvmTest/kotlin/senders/ApplyDevConfigSpec.kt` — 5 tests. Pins test-mode
      suppression with AND without a dev config, `disableEmails`, the untouched-in-production case,
      and that dev-mode destination redirection still works (i.e. the new early return did not
      swallow dev mode).
- [x] `funktor/messaging/src/jvmTest/kotlin/senders/CapturingEmailSenderSpec.kt` — 6 tests. Capture
      order, `clear`, the drop-oldest bound, cc/bcc matching, and href entity-decoding.
- [x] `funktor/all/src/jvmTest/kotlin/PasswordResetEmailE2eSpec.kt` — 3 tests: the full walk
      (signup → reset request → read the mail → follow the link → set password → sign in with the new
      one → old one rejected → token is single-use); no mail for an unknown address (the half of
      "no account enumeration" that no response-body assertion can see); and case-insensitive reset
      that mails the canonical address.
- [x] `funktor/messaging/src/jvmTest/kotlin/storage/EmailStoringSpec.kt` — 7 tests over
      `withAnonymizedContent`, which had none. Added in review; see below.
- [x] `funktor/messaging/src/jvmTest/kotlin/MailingTestModeGuardSpec.kt` — 3 tests booting the real
      messaging module, proving the guard holds for an app that never calls `applyDevConfig`.
- [x] After the review round: `:funktor:messaging:jvmTest` 35/35, `:funktor:all:jvmTest` 128/128,
      `:ultra:security:jvmTest`, `:ultra:common:jvmTest` + `:ultra:common:jsTest`, and a whole-project
      `assemble` — all green.

### Mutation evidence

| Mutation | Result |
|---|---|
| Put the `isTest` check back inside the `devConfig` bail-out | ✅ reddens `ApplyDevConfigSpec` |
| `recoverAccountInitPasswordReset` returns before sending the mail | ✅ reddens 2 e2e tests — and **nothing else in the 128-test suite noticed**, which is the gap this closes |
| Token written into the URL with `setRaw` (no percent-encoding) | ❌ **did not redden at first** — fixed, see below |
| Remove `synchronized` from `CapturingEmailSender` | ✅ reddens the new concurrency test, 3 runs out of 3 |
| Drop the guard from the `Mailing` factory | ✅ reddens `MailingTestModeGuardSpec` |
| Revert `anonymizeLinks` to href-only | ✅ reddens 3 `EmailStoringSpec` tests |

The third mutation is the one worth recording. The original assertion decoded the path segment and
checked the token was accepted, which passes even for an unencoded token: undoing an encoding that
never happened is a no-op on base64. The defect is real, though — the token routinely contains `/`,
which would split it across path segments so the frontend route never matches, leaving a dead link
that every server-side test still reports as healthy. The spec now asserts the segment contains no
`/` and is exactly the percent-encoding of the token it decodes to; re-running the mutation reddens it.

## Dependency correction (done here)

`funktor:messaging` declared `commons-email` with the comment "sending simple email via smtp", and
there is no SMTP sender — so it looked unused, and the user agreed to drop it (decision: mail will
always go out through provider APIs, no SMTP).

**Dropping it broke the build**, which corrected the diagnosis: no file imports
`org.apache.commons.mail`, but commons-email was the only thing pulling in `com.sun.mail:jakarta.mail`,
which `AwsSesSender` compiles against directly (`javax.mail.*`, `javax.activation.*`) to build the
MIME message for `SendRawEmail`. The dependency was mislabelled, not unused.

Resolution: `commons-email` is gone; `com.sun.mail:jakarta.mail:1.6.7` is now declared directly — the
exact version that was already resolving transitively, so this is a de-transitivisation, not a bump.
`Deps.JavaLibs.JakartaMail` replaces `ApacheCommons.email`.

- Follow-up: jakarta.mail 1.6.x still exposes the `javax.*` namespace. Moving to 2.x
  (`jakarta.*`, `org.eclipse.angus:angus-mail`) means rewriting the imports in `AwsSesSender`.
- `Deps.JavaLibs.ApacheCommons.cli` turned out to be unreferenced too (no build file, no source).
  Removed on the user's call, which empties the `ApacheCommons` object — so it is gone entirely.

## Follow-ups (not done here)

- The same seam is not yet available to `funktor-demo:server` tests; its blueprint builds the sender
  inline in `kontainer.kt:85`. Wire it when a demo-level email test is needed.

## Review record (filled by /feature-review)

Round 1, 2026-07-27, over the working-tree diff vs `0f29d239`.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | findings | 5 (2 MEDIUM, 3 LOW) — all confirmed, all fixed |
| 2. Domain expert | findings | 7 (3 MEDIUM, 4 LOW) — all confirmed; 5 fixed, 2 were design calls |
| 3. Security | findings | 5 (3 MEDIUM, 2 LOW) — all confirmed; 4 fixed, 1 deferred |

Reviewers 2 and 3 independently found the same top issue (the guard being opt-in), and **disagreed**
on where `CapturingEmailSender` belongs. Every finding was re-verified against the code before being
accepted; none were dropped as unconfirmed.

### Fixed — defects

- **KDoc showed the broken wiring.** The class doc's "register it like this" snippet was the exact
  double-registration that fails kontainer validation with "Parameter 'sender' is ambiguous" — the
  error hit during implementation, and the snippet was never updated to match the fix.
- **Nothing tested the locking**, which is the one property the class exists to guarantee. All six
  tests passed with every `synchronized` removed. Added a 200-way concurrent send; mutation-verified
  red 3/3 with the lock removed.
- **Catch-all `else -> "'"` in the entity decoder.** `#39`/`apos` are unreachable (kotlinx.html emits
  only `<`, `>`, `&`, `"`), so the branch was dead — and adding one alternative later, say `nbsp`,
  would have silently decoded it to `'`. Now `else -> it.value`, and the dead alternatives are gone.
- **Tests 2 and 3 depended on test 1** creating the account. Run alone, test 3 failed with "expected
  not null", which reads as "case-insensitive reset is broken". Signup moved to `beforeSpec`.
- **A global assertion on shared state.** `emails.captured shouldBe emptyList()` reads every spec's
  mail; now scoped to the address, which is also the more precise statement of the property.
- **`emails.clear()` after signup threw away evidence.** Replaced with an assertion that signup mails
  nothing — a deliberate tripwire that goes red when activation lands.
- **CVE-2025-7962** (SMTP injection via CR/LF, CVSS 6.0) affects `com.sun.mail:jakarta.mail < 1.6.8`.
  Verified independently: 1.6.8 is the patch, still ships the `javax.mail` namespace (no import
  rewrite), still pulls `jakarta.activation`. Bumped. Unreachable here — nothing uses the SMTP
  transport — but `implementation` puts it on every consumer's runtime classpath.
- **KDoc justified the retention bound with a "dev outbox"** that does not exist, inviting exactly the
  production-adjacent use that makes retaining live tokens a problem. Rewritten.

### Fixed — design decisions taken with the user

- **The guard was opt-in.** `applyDevConfig` is an extension nothing forces; it had ONE call site in
  the repo. An app registering a real sender without it mails real people from its test suite.
  Now enforced by the messaging module where it builds `Mailing` — an app cannot opt out by
  forgetting a line. `funktorMessaging(config, builder)` takes the config explicitly, matching
  `funktorCore` / `funktorRest`, so every call site supplies it at compile time.
- **`EmailSender.canDeliver`,** not the marker interface originally planned. The marker was
  implemented first and the e2e tests immediately caught why it cannot work: a decorator
  (`HooksEmailSender`) cannot inherit a marker from what it wraps, so wrapping the capturing sender in
  hooks made the chain look delivering and the guard swallowed it. Delivery is a property of the
  CHAIN: default `true` (fail-closed), `false` on the two leaves, and every decorator delegates to
  `wrapped.canDeliver`. Pinned in both directions, decorated and bare.
- **`EmailAddress` now gates on `isForbiddenInId`.** It was the one id-shaped value class that did
  not. `trim()` only strips the ends, so `victim@x.com\r\nbcc: attacker@evil.com` was constructible
  and flows to `InternetAddress` → raw MIME. Unreachable today (creation paths assert `isValidFormat`,
  a full-match regex), but format is deliberately not checked on the lookup path, so the safety rested
  on every future caller remembering. See the rollout note below.
- **`CapturingEmailSender` stays in `funktor:messaging`.** It is the only module serving both
  consumers — that module's own jvmTest needs it as a spy, and `funktor:all`'s tests need it across a
  boundary — and `NullEmailSender` is already a published sender that silently swallows mail. The
  security objection is answered by the KDoc rewrite and by it not being registered by default.

### Fixed — the best find of the gate (pre-existing)

`EmailStoring.withAnonymizedContent` — the control that keeps live tokens out of the database — had
**zero** coverage, because `StoringEmailHook` is only wired in `funktor-demo` and never in a test. It
also only rewrote `href="…"`, while the same policy is applied to `EmailBody.Text`, where a link is a
bare URL. A plain-text activation mail, or an HTML one that also prints the URL for copy-paste, would
have persisted its token verbatim into `system_sent_messages`.

- `anonymizeLinks` now strips bare `http(s)://…` as well as hrefs, and the href pattern no longer
  fails on a value spanning a newline. New `EmailStoringSpec` — 7 tests.
- The `funktor:all` test app now wires `withHooks { onAfterSend(storing) }` like a real app, reached
  via a `TestOutbox` holder (the sender cannot be registered under its own type — ambiguity again).
- `PasswordResetEmailE2eSpec` now asserts the PERSISTED copy does not contain the token that the
  captured mail does — comparing against the real token, so it cannot pass vacuously.

### Deferred, with the user's agreement

- **`isForbiddenInId` is a denylist; `RealmId` uses an allowlist.** Publishing the denylist as the
  shared id predicate points the next id type at the weaker pattern. Recorded for the value-class plan
  rather than changed here — it is about future id types, not this diff.
- **A misconfigured `environment = "test"` in production now silently discards mail** (including
  breach notifications) while reporting success. Worth a boot-time warning when the guard suppresses a
  configured sender; not done here.

**Red-team follow-up:** covered by the existing
`.claude/tasks/20260726-redteam-value-class-emailaddress.md` §5 (enumeration by timing — the work
asymmetry this seam now makes measurable) — no new red-team doc.
