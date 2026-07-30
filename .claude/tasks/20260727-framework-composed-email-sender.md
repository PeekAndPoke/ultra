# The framework composes the email sender chain; the app supplies only a provider

**Status:** DONE 2026-07-27 (implemented + gate passed same day)
**Plan:** `/Users/gerk/.claude/plans/indexed-stargazing-spring.md`
**Supersedes:** the guard design in `.claude/tasks-archive/2026-07/20260726-email-e2e-test-seam.md`
**Security-critical:** yes — this is what makes "a test run never reaches a real provider" true.

## Why

An app had to hand-assemble the whole decorator stack (`funktor-demo/.../kontainer.kt:80-95`):
`provider.applyDevConfig(config, devConfig).withHooks(log) { onAfterSend(storing); … }`. Every link is
something the framework wants true of every app, yet each was opt-in by convention — and the review
gate a day earlier found the consequence: omit `.applyDevConfig(...)` and the test suite mails real
people.

That was first patched by adding `EmailSender.canDeliver` so a module-level guard could tell a
delivering chain from a non-delivering one. It worked, but it made the framework *defensive* instead
of making the app's job *smaller*, and it put a member on a published interface that every decorator
has to remember to delegate — a rule that broke the same day it was written.

The user's direction: *"the user only needs to provide the final sender and the rest, the devConfig
and the hooks are applied always"*, and *"I do not like the instanceof check, will just break in
unforeseen ways"*.

## The design

**In test mode the framework substitutes a capturing sender.** That single decision removes the need
to ask anything about the sender — no `canDeliver`, no marker interface, no type check.

- `CapturedEmails` (new) holds the state. **No constructor dependencies**, therefore a GLOBAL
  singleton: one instance per blueprint, shared by every per-request kontainer. Moving the state off
  the sender is what makes the sender's own scope irrelevant, which is the whole scoping trap gone.
- `CapturingEmailSender(captured)` is now a thin writer.
- **One composition site**, the `Mailing` factory (`funktor/messaging/.../index_jvm.kt`): in test mode
  it builds a `CapturingEmailSender` and never touches `ConfiguredEmailSender.sender`, so the app's
  provider is not even constructed; otherwise `applyDevConfig` then `withHooks(storing + app hooks +
  debug log)`.
- `FunktorMessagingBuilder.useSender(devConfig) { provider }` — the app's whole contribution — plus
  `onAfterSend { }` for app hooks, which run in the same place as the framework's own.
- The provider lives behind **`ConfiguredEmailSender`**, which is deliberately NOT an `EmailSender`.
  So `EmailSender` is not a kontainer service at all: nothing can inject an undecorated sender, and
  the holder's `by lazy` builds the provider at most once per blueprint (the composed chain is rebuilt
  per request, so building it there would open one SES client per HTTP request).
- `devConfig` is a builder field read at composition time, so builder call order does not matter.

### What this gets for free

- **The bypass is covered twice over.** In test mode nothing the app registered is consulted at all;
  and because `EmailSender` is not a service, a stray `singleton(EmailSender::class) { … }` is inert
  in every mode. No inspection, nothing to lie about.
- **No credentials needed in tests.** Precisely: no *client construction*. Config plumbing
  (`${AWS_SES_SECRET_KEY}`) is unchanged and still required to boot.
- **The test app needs no email wiring at all.** `funktor/all`'s blueprint lost the capturing sender,
  the hooks wrapper and the `TestOutbox` holder; `PasswordResetEmailE2eSpec` needed no change.

### Deleted

`EmailSender.canDeliver` + 5 overrides · `suppressDeliveryInTestMode` · the `isTest` branch inside
`applyDevConfig` · `dynamic(EmailHooks::class)` (nothing injected it) · `TestOutbox`.

`funktor-demo`'s mail wiring: 16 lines → 1.

## Test evidence

- `FunktorMessagingBuilderSpec` (11) — dev config applied without the app calling `applyDevConfig`;
  the storing hook wired without the app asking; **the storing hook persists the ORIGINAL mail, not
  the dev-decorated one**; builder order irrelevant; `useSender` twice → last wins; the provider is
  constructed at most once across three kontainers; **captured mail visible across kontainers from
  one blueprint**.
- `MailingTestModeGuardSpec` (4) — the direct-registration bypass still does not deliver *and* is
  captured; test mode captures with no wiring; the provider is never constructed in test mode; outside
  test mode the provider is used and nothing is captured.
- `ApplyDevConfigSpec` (5) — dev behaviour only, now including the previously untested
  **`ignoreDomains` × destination-redirect ordering** (the redirect runs first, so mail to an ignored
  domain is redirected and still sent).
- `CapturedEmailsSpec` (8), `MailingOverridesSpec` (6, the dev banner had none), `EmailStoringSpec` (9) — including separate senders sharing one store, and 200 concurrent sends.
- Green after the gate: `:funktor:messaging` 51/51, `:funktor:all` 128/128 (unchanged),
  `:funktor:auth` 95/95, `:funktor-demo:server` 47/47, `:ultra:security`, `:ultra:common`.

### Mutation evidence

| Mutation | Result |
|---|---|
| Drop the `isTest` branch from the composition | ✅ reddens 4 (both guard tests + capture) |
| `Lazy<EmailSender>` → `EmailSender` | ✅ reddens exactly "provider never constructed" |
| Give `CapturedEmails` a ctor dependency (→ per-kontainer) | ✅ reddens exactly the cross-kontainer test |
| Compose hooks INSIDE the overrides | ✅ reddens exactly "persists the ORIGINAL mail" |

## Notes / residual

- **`MailingTestModeGuardSpec`'s direct `singleton(EmailSender::class)` is the assertion.** It is
  commented as such. Rewriting it to use `useSender` would leave it green and prove nothing.
- `Mailing` is now `SemiDynamic` (per-kontainer) for every app, including ones with no mail
  configured, because it injects `StoringEmailHook`. It holds no state, so this is allocation only.
- A misconfigured `environment = "test"` in production now silently *captures* mail rather than
  discarding it — i.e. retains bodies in heap. The boot-time warning that would surface this is item 1
  of `.claude/tasks/20260727-messaging-followups.md`.
- `funktor-demo` has no test that triggers a mail flow, so its own composed chain is unproven at the
  app level. Deleting the old registration in the same commit is what prevents double-decoration.

## Review record (filled by /feature-review)

Gate run 2026-07-27 over the whole email implementation, not only the diff (the user asked to "fully
verify the current email impl").

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | findings | 18 — 12 fixed here, 6 recorded |
| 2. Domain expert | findings | 12 — 4 fixed here, 8 recorded |
| 3. Security | findings | 7 — 4 fixed here, 3 recorded |

Every finding was re-verified against the code before being accepted. Two independent reviewers found
the `EmailSender`-injection hole; two found the test-harness environment gap.

### Fixed — holes this change had introduced

- **`EmailSender` resolved to the BARE provider.** Before the redesign the app registered the fully
  composed chain under that type; after it, the raw `AwsSesSender`. An app injecting `EmailSender` —
  the obvious-looking type — would have got no dev overrides, no test capture, no storing hook, and
  would have hit real SES from CI. `MailingTestModeGuardSpec` did not catch it: it proves direct
  *registration* is ignored, not that direct *injection* is unsafe. Fixed by putting the provider
  behind `ConfiguredEmailSender`, which is NOT an `EmailSender` — so `EmailSender` is not a kontainer
  service at all and the bypass is unwritable rather than guarded. It also memoises the provider,
  which let the `Lazy<>` injection go.
- **The always-on debug log interpolated the whole mail**, body included. That was one app's line
  before; moving it into the framework applied it to every app. `LogCollector.Appender` has NO level
  filter, so it was persisted into insights records and served by a GUI route with no auth floor.
  Now logs recipients, subject and message id only.
- **A second `useSender` without a `devConfig` silently cleared the first one** — a merge or a
  conditional block would have turned off dev redirection and started mailing real customers from a
  dev box. `devConfig` is now only ever set, never cleared.
- **`CapturingEmailSender` held a `private var counter`** while its KDoc claimed no state: it restarts
  per kontainer, so two unrelated mails got `captured-1`, and that is the id `StoringEmailHook`
  persists. Moved into `CapturedEmails` under the existing lock.
- **`Email.hrefs()` still used the lazy `.*?`** — the exact bug class this changeset had just fixed in
  `EmailStoring`. Fixed in both now.
- **`FunktorMessagingBuilder.config`** was public with no reader anywhere. Removed.
- **My "builder call ORDER does not matter" test varied no order.** It made a single builder call and
  was a strictly weaker duplicate of the dev-config test — no implementation change could redden it.
  Replaced with the property actually at risk (a second `useSender` must not drop the dev config),
  plus a real app-hook test.
- **The e2e persisted-copy assertion could pass on an empty body**, and `stored.size shouldBe 1`
  depended on declaration order. Both fixed, with positive assertions added.
- Wrong comment about `CapturedEmails` scope (it is per-spec, not process-global); wildcard import in
  the demo's `kontainer.kt`.

### Fixed — pre-existing, found while verifying

- **The test harness never checked that the config it loaded says it is a test.** `createTestBed(env)`
  picks `application.<env>.conf` by FILENAME while `isTest` reads a field INSIDE it, and the whole
  suppression guarantee rests on those agreeing. A config copied from `application.dev.conf`, or a
  pipeline standardising on `"ci"`, would hand the suite a live provider. Now a `require` with an
  actionable message. Mutation-verified: setting the test config to `"ci"` fails every spec at boot
  with *"FIX: set it to 'test', otherwise the app does not know it is under test — mail would be sent
  for real instead of captured."*
- **`funktor:auth`'s test container ran messaging in PROD mode** (`AppConfig.of()` defaults to
  `"prod"`), safe only because it never registers a sender.
- **The dev banner never appeared on a realistic template.** `PrefixBody` matched the literal
  `"<body>"`, so `body { style = "..." }` — routine in mail — silently got no banner, making a dev-box
  mail indistinguishable from production. It also injected raw HTML into plain-text bodies. Now
  matches `<body` with any attributes, falls back to prepending for a fragment, and uses a text banner
  for text bodies. Six new tests; it had none.
- **`anonymizeLinks` was case-sensitive and http-only** — `HTTPS://`, a protocol-relative `//host/…`
  and a `myapp://` deep link all slipped through with their tokens.
- **`SecureRandom.getInstanceStrong()`** ran per request (the chain is rebuilt per kontainer) to
  generate a placeholder message id; on Linux that commonly blocks on entropy.
- **`docs-site/.../messaging.astro` taught the removed hand-composition verbatim** — following it now
  would double-decorate and persist every mail twice. Rewritten around `useSender`, with the
  test-capture section it never had.

### New coverage added

Mail with no storing policy is not persisted · `withoutContent` writes `"n/a"` · a throwing hook
breaks neither the send nor its siblings · an app hook sees the original mail · the dev banner (6) ·
anonymization of uppercase/protocol-relative/custom schemes.

`:funktor:messaging` 39 → 51 tests.

### Mutation evidence (this round)

| Mutation | Result |
|---|---|
| `useSender` clears `devConfig` again | ✅ reddens exactly the new dev-config test |
| Dev banner matches literal `<body>` again | ✅ reddens exactly the attributes test |
| Test config declares `environment = "ci"` | ✅ every spec fails at boot with the actionable message |

### Recorded, not fixed

`.claude/tasks/20260727-messaging-followups.md` (8 items, incl. the boot warning, `senderName` never
reaching the `From` header, anonymization not covering non-URL secrets, no `OnBeforeSend` hook for
activation-resend throttling, and zero provider tests) and
`.claude/tasks/20260727-redteam-messaging.md` (6 attack scenarios, led by the un-tenanted
`findByRefs`).
