# Docs collector — messaging: framework-composed sender, test capture, id-character predicate

**Status:** TODO
**Plan:** none — follow-up to `.claude/tasks/20260726-email-e2e-test-seam.md`
**Security-critical:** no (documentation only — but it documents a security guard)

Per the standing rule that every funktor/kraft/ultra change gets a documentation follow-up. Nothing
here is urgent; it is a collector so the prose pass has a list instead of a git log.

## What changed and needs documenting

### 1. `funktorMessaging(config, builder)` — BREAKING signature change

Was `funktorMessaging(builder)`. The module needs the `AppConfig` to decide whether mail may leave the
process; taking it explicitly matches `funktorCore(config, info)` and `funktorRest(config) { }` and
makes every call site supply it at compile time rather than discovering at boot that no `AppConfig`
happened to be registered. Three call sites in-repo, all updated. External consumers must add the
argument.

### 2. `FunktorMessagingBuilder.useSender(devConfig) { provider }` — the headline change

An app supplies ONLY its provider. Dev config, test-mode capture, the storing hook and the debug log
are applied by the module. `funktor-demo`'s mail wiring went from 16 lines to 1.

Three things a doc must say, because each is non-obvious and each has a real reason:

- **[provider] is a lambda, not a value.** It is invoked at most once per blueprint, and never at all
  in test mode. `AwsSesSender.of(...)` opens an SDK client, so both matter.
- **The provider cannot be a kontainer service.** Registering it under its own type would make it a
  second candidate for every `EmailSender` injection point and the blueprint would fail validation as
  ambiguous. Construct it inside the lambda.
- **`devConfig` is passed in** because it lives on the app's own config class, not on `AppConfig` —
  the framework cannot reach it. Same for provider credentials.

### 3. Test mode captures; nothing opts in

`funktorMessaging` substitutes a `CapturingEmailSender` writing into the `CapturedEmails` service
whenever `config.ktor.isTest`. A test resolves `CapturedEmails` from the kontainer — no registration,
no holder type, no cast. `funktor/all/src/jvmTest/kotlin/PasswordResetEmailE2eSpec.kt` is the worked
example, and its blueprint has no email wiring at all.

Worth stating explicitly in the docs: **`CapturedEmails` must never gain a constructor dependency.**
That is what keeps it a global singleton shared across per-request kontainers; give it one and every
capture-based test silently reads an empty list.

Also worth stating: this is what makes the guarantee unconditional. An app that registers
`singleton(EmailSender::class) { ... }` directly, bypassing `useSender`, is simply not consulted in
test mode — there is no type check to defeat.

### 4. `Email.hrefs()`

Extracts the links from a mail body with `kotlinx.html`'s attribute escaping undone — how a test
follows the token in an auth mail. Note WHY it is needed: the stored copy cannot serve, because
`EmailStoring.withAnonymizedContent` strips every link before persisting.

### 5. `EmailStoring.anonymizeLinks` now strips bare URLs too

Behaviour change: previously `href="…"` only, so plain-text bodies kept their links (and their
tokens). Any consumer relying on bare URLs surviving into `system_sent_messages` will see them
replaced with `#anonymized`.

### 6. `isForbiddenInId` moved to `ultra:common`

Was `internal` in `ultra:security`; now public in `ultra/common/.../strings.kt` next to `isEmail` /
`isSlug`. Mention it in the id/value-class docs — and note the open denylist-vs-allowlist question
recorded in `.claude/tasks/20260724-value-class-ids-migration.md` Step 5 before writing it up as
guidance.

### 7. `EmailAddress` rejects control characters

One more line in the type's documented invariants, plus the extended rollout audit query in
`.claude/tasks/20260726-value-class-emailaddress.md`.

### 8. Dependency note

`funktor:messaging` no longer pulls `org.apache.commons:commons-email`; it declares
`com.sun.mail:jakarta.mail:1.6.8` directly (CVE-2025-7962 floor). Only relevant to consumers pinning
mail libraries themselves.

## Where

- `docs-site/src/pages/ultra/*` for the reference prose.
- `docs-site/src/data/llms/*.md` templates for the LLM mirror — never `docs-site/public/`.
- See `.claude/skills/docs-site/`.
