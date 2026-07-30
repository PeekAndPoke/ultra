# Auth email templates, rendered in code

**Status:** DONE (2026-07-28) — gate PASS after fixes; all tests green + `assemble` clean
**Plan:** `.claude/tasks/i18n/20260722-i18n-s7-emails.md` → S7, **design replaced** (see below)
**Supersedes:** `.claude/tasks-archive/2026-07/20260728-i18n-s7-activation-email-slice.md` (the Markdown-template slice — built, gated, reverted)
**Security-critical:** yes (the activation mail carries a live single-use token) → red-team follow-up

## Why the Markdown design was dropped

S7 was designed around whole-Markdown-templates-per-locale with `{{placeholder}}` substitution. That
slice was built and gated on 2026-07-28. **The gate is what killed it.** The user's objection — "this
will lead to a real email being sent out with `Hello ###FIRSTNAME###!` at some point" — is not
hypothetical: Reviewer 1 found that `senderName` was substituted into the body with **no assertion
anywhere**, so deleting it from the args map ships a literal `{{senderName}}` to every recipient with
the whole suite green. Leaving unknown placeholders untouched was a deliberate design choice, which
means the failure mode was by design.

Removing the mechanism removes six confirmed findings rather than patching them:

| Confirmed finding (gate, 2026-07-28) | Fate |
|---|---|
| HIGH — template-authored attributes double-escaped, `&` → `&amp;amp;` (verified empirically) | gone: kotlinx.html escapes structurally |
| HIGH — a `myapp://` deep link silently became `href="#"` | gone: no URL sanitizer needed |
| HIGH — malformed template threw at SEND time, 500-ing sign-up after the account was written | gone: a template that does not compile does not ship |
| MEDIUM — `UrlSanitizer` skipped for single-quoted / spaced / unquoted attributes | gone: no attribute-context guessing |
| MEDIUM — activation token reachable from the subject; persisted subject is never anonymized | gone: the subject is a Kotlin string |
| MEDIUM — path-traversal guard needed on a user-controlled locale in a resource path | gone: no name-based lookup |

It also drops the commonmark dependency, a hand-rolled front-matter parser, and ~340 lines of
regex-based escaping. And it RESTORES a guarantee the Markdown version weakened: kotlinx.html always
double-quotes attributes, so `EmailStoring`'s `href="[^"]*"` anonymizer cannot be defeated by a
template author's choice of quote character.

**Accepted cost:** a `render()` function cannot be handed to a translator or a TMS the way a `.md`
file can. Auth mails are developer-written here, and the file-based approach can come back later if
that changes.

## Design

1. **`EmailTemplate<PARAMS>`** (`funktor:messaging`) — `fun render(params: PARAMS, preferred: List<Locale>): Email`.
   The template builds the whole `Email` (source, destination, subject, body) and nothing
   downstream of it. `DefaultMessaging` re-asserts the envelope via `requireAuthEmailEnvelope` — a
   template must not be able to cc a live activation token to a second mailbox (gate finding #3).
2. **`LocalizedEmailTemplate<PARAMS>`** — one shared fallback walker so every template gets identical
   `de-CH → de → en` semantics instead of three hand-rolled chains that drift. Holds
   `renderers: Map<Locale, (PARAMS, Locale) -> Email>` plus a `fallbackLocale` it guarantees. The
   renderer is HANDED the matched locale rather than naming its own — otherwise the map key and the
   rendering are two statements of the same fact, free to disagree (gate finding #2).
3. **`render` takes a LIST, not `(locale, fallback)`.** The realm default sits BETWEEN the user's
   language and the framework's `en` — modelling it as an either/or is the HIGH finding two reviewers
   raised against the previous design, and it survives a redesign if not handled deliberately.
4. **Each template declares its own nested `Params`**, so adding a parameter later is a local change
   and a missing one is a compile error rather than a `{{placeholder}}` in someone's inbox.
5. **`AuthEmailTemplates`** — a holder passed into `DefaultMessaging`, with
   `.accountActivation` / `.passwordChanged` / `.passwordRecovery`. No lookup by name. Apps override
   by supplying their own subclass; the framework ships defaults.
6. **`.store(...)` stays at the call site.** Templates return an `Email`; `DefaultMessaging` applies
   `EmailStoring.withAnonymizedContent(refs, tags)`. An app template must NOT be able to choose the
   storing policy — that is what keeps live tokens out of the support inspector.
7. **`EmailLayout` is an injectable `fun interface`**, `render(locale, content)`, passed into each
   `Default*` template and on into `authEmail`. Branding — colours, frame, header, footer — is the
   common reason to touch these mails, and it should not require reimplementing three renderings.
   `AuthEmailTemplates.default(layout)` hands one layout to all three, so an app cannot brand two and
   silently ship framework chrome on the third. It takes the LOCALE because a footer (imprint,
   unsubscribe) has to be translated; a layout blind to the locale can only put an English footer
   under a German body. `EmailLayout.default` emits `<html lang>` + `<head>` with a charset
   declaration, which closes two review findings for free.

## Spec

- [x] `EmailTemplate<PARAMS>` + `LocalizedEmailTemplate<PARAMS>` —
      `funktor/messaging/src/jvmMain/kotlin/templates/EmailTemplate.kt`
- [x] `AuthEmailTemplates` holder + three template types with nested `Params`, framework defaults in
      `en` + `de` — `funktor/auth/src/jvmMain/kotlin/emails/`
- [x] `DefaultMessaging(… , templates: AuthEmailTemplates = AuthEmailTemplates())` — defaulted; all
      six existing call sites compile untouched
- [x] Locale preference `user.language.messaging` → `realm.defaultLanguage` → template fallback —
      `authEmailLocales` in `emails/AuthEmailTemplates.kt`, extracted from `DefaultMessaging` so the
      rule is unit-testable rather than trapped in a private method
- [x] `validate()` at wiring time — `AuthEmailTemplates.init`
- [x] All THREE emails converted
- [x] Markdown machinery, specs, `.md` resources and the commonmark dependency deleted; `Deps.kt`
      reverted to its pre-slice state
- [x] **`EmailLayout` is an injectable `fun interface`**, threaded into each `Default*` template and
      through `authEmail`, with `AuthEmailTemplates.default(layout)` handing one layout to all three.
      Added after the first cut made it a static `object` — which silently removed an app's only way
      to brand its mail, a regression on the design it replaced.

## Test evidence

- [x] `LocalizedEmailTemplateSpec` (14) — exact hit, region → base, regional beats base, preference
      order walked, each preference exhausted with its base before the next, fallback, non-English
      fallback, `validate()` accept + reject, loud `render()` failure
- [x] `AuthEmailTemplatesSpec` (19) — both languages for all three mails, exactly one link and the
      base64 token intact, password-changed carries NO link, sender name present, `lang` + charset,
      locale-preference rule, tag normalization (`"DE_ch"`), blank setting ignored, custom layout
      reaching all three mails
- [x] Non-ASCII survives (`Grüßen`)
- [x] `AccountActivationEmailE2eSpec` (8) keeps the German test, unchanged by the rewrite
- [x] Anonymizer still sees a real href — pre-existing e2e assertions still pass and still fail when
      anonymization is removed (mutation 4)
- [x] **Mutation-tested — all six caught** (three more after the review fixes, see the Review record):

  | Mutation | Caught by |
  |---|---|
  | `rendererFor` ignores the preference order | `LocalizedEmailTemplateSpec` (5), `AuthEmailTemplatesSpec` (7), **and the German e2e** |
  | `authEmailLocales` drops the realm default | `AuthEmailTemplatesSpec` (4) |
  | `validate()` not called at wiring time | `AuthEmailTemplatesSpec` — the reject test |
  | `withAnonymizedContent` → `withContent` | `AccountActivationEmailE2eSpec` |
  | layout drops `lang` + charset | `AuthEmailTemplatesSpec` |
  | one template ignores its injected layout | `AuthEmailTemplatesSpec` — the custom-layout test |

- [x] Green (2026-07-28): `./gradlew :funktor:messaging:jvmTest :funktor:auth:jvmTest
      :funktor:all:jvmTest :funktor-demo:server:test` then `./gradlew assemble`

| Suite | Baseline | After |
|---|---|---|
| `funktor:messaging:jvmTest` | 51 | **65** (+14) |
| `funktor:auth:jvmTest` | 107 | **133** (+26) |
| `funktor:all:jvmTest` | 137 | **138** (+1) |
| `funktor-demo:server:test` | 47 | 47 |

New specs: `LocalizedEmailTemplateSpec` 14, `AuthEmailTemplatesSpec` 19, `AuthEmailEnvelopeSpec` 5,
`DefaultMessagingLocaleWiringSpec` 2; `AccountActivationEmailE2eSpec` 7 → 8.

## Implementation notes

- Only TWO implementations of `AuthRealm.Messaging` exist (`DefaultMessaging`, and `TestMessaging` in
  `funktor/auth/src/jvmTest/kotlin/index_jvmTest.kt:106`), and all six `DefaultMessaging(...)` call
  sites pass only the four original named params — so a defaulted `templates` parameter is
  source-compatible everywhere.
- `htmlEmailBody` in `funktor/messaging/src/jvmMain/kotlin/EmailHelpers.kt:17` is deliberately NOT
  reused: it is a member of the `EmailHelpers` *interface*, so it is unreachable from `EmailLayout`'s
  `fun interface` lambda. `EmailBody.Html { }` is called directly, which is correct.
- `MessageResolver.chain` (`ultra/i18n`) is a property on a class that needs catalogs, not a reusable
  free function — hence a small chain walker here rather than reuse.

## Review record (/feature-review, 2026-07-28)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | APPROVE WITH FIXES | 4 MEDIUM, 4 LOW |
| 2. Domain expert | findings, none blocking | 2 MEDIUM, 5 LOW |
| 3. Security | CHANGES REQUESTED | 2 MEDIUM, 2 LOW |

**Gate: PASS** — every finding was verified against the code before being accepted, then fixed.

### Fixed

| # | Sev | Finding | Fix |
|---|---|---|---|
| 1 | MED | `render()` throws OUTSIDE the "return a failed `EmailResult`" protocol every caller is written against. On resend it throws *after* the previous link is revoked -> unrecoverable lockout; on `recoverAccountInitPasswordReset` it creates an **account-enumeration oracle** (500 for addresses that exist, neutral 200 for ones that do not). Reachable because templates are app-overridable. | `sendAuthEmail` wraps the render in `try/catch`, logs, returns `EmailResult.ofError` |
| 2 | MED | The rendering locale was stated TWICE — as the `renderers` map key and again as a literal inside each renderer — free to disagree. Copy `renderDe` to `renderFr`, forget the literal, and French copy ships as `lang="de"` under a German footer: the mixed-language mail this design exists to prevent. | the walker HANDS the matched locale to the renderer; `rendererFor` returns `Pair<Locale, ...>` |
| 3 | MED | A template can address the mail anywhere. An app template adding a cc "so support can see onboarding mails" puts a live activation token — a working credential — into a shared mailbox. | `requireAuthEmailEnvelope`, extracted so it is unit-testable, called inside the try |
| 4 | MED | The persisted **subject** was never anonymized. Templates are app-overridable now, so a subject interpolating `activationUrl` writes a live token into the support inspector *and* into every log appender. | `StoringEmailHook` runs `storing.modifyContent` over the subject too |
| 5 | MED | The documented override path does NOT compile for an external consumer: `funktor:messaging` and `kotlinx.html` were `implementation`-scoped while the new extension points expose their types. | both promoted to `api`, with the reason recorded |
| 6 | MED | `AuthRealm.defaultLanguage` — new public API — had NO coverage of its only consumer. Hardcoding `Locale("en")` at the call site left the entire suite green. This is the exact bug class the redesign was justified on. | `preferredLocales` made `internal` and takes the user value; `DefaultMessagingLocaleWiringSpec` pins it (mutation-verified) |
| 7 | MED | `validate()` demanded an exact `fallbackLocale` key while `rendererFor` widens region->base, so a template declaring `de-CH` and shipping `de` failed to boot despite working fine. | `validate()` now checks the invariant the lookup actually needs |
| 8 | LOW | `AuthEmailTemplates.default(layout)` and a per-template override were mutually exclusive — an app doing both silently shipped framework chrome on the two it did not name. | `layout` moved to the FIRST constructor parameter; `default()` kept as a thin alias |
| 9 | LOW | German copy: the recovery mail was titled **"Konto wiederherstellen"**, which in German means restoring a *deleted* account — a security mail that misdescribes itself, and exactly the shape users are trained to read as phishing. Plus a du/Sie register clash, a comma German Grußformeln do not take, "Achtung!" on a mail the user requested, and two calques. | -> "Passwort zurücksetzen"; "Viele Grüße" (no comma) via a shared `authEmailSignature`; "festzulegen"; "folgenden Link" |
| 10 | LOW | No `<!DOCTYPE html>`, so every standalone render path (forward-as-inline, save-as-HTML, the persisted body) parses in quirks mode — which is where the charset guess the `<head>` exists for goes wrong. Unstyled, it renders as Times New Roman full-bleed in Outlook. | doctype prefixed; minimal inline body style |
| 11 | LOW | KDoc claimed `validate()` runs at boot; realms are kontainer-`dynamic`, i.e. rebuilt per request. | comment corrected to what is true, with the singleton fix noted |
| 12 | LOW | Stale Markdown-pipeline comment in the German e2e; 12 lines of duplicated KDoc on a one-line delegate; six copies of the sign-off; task-doc count and `htmlEmailBody` inaccuracies; import ordering. | all fixed |

### Declined, with reasons

- **`EmailLayout.render` returning `EmailBody` rather than `EmailBody.Html`**, to make a future text
  alternative non-breaking. Declined: multipart needs an `Email` model change regardless, so widening
  this return type does not actually avoid that break — it only loosens the contract today.
- **A `de-CH` user receives `Grüße` rather than Swiss `Grüsse`** (Swiss orthography has no `ß`).
  Correct observation, but it is the intended cost of `de-CH -> de` fallback, not a defect.
- **Attachments are not anonymized.** Real, but out of scope — an auth `Email` never has one.
  Recorded in the red-team collection instead.

### Known test gaps (stated, not hidden)

- The `try/catch` around render (#1) is verified by inspection only. A test needs a realm with full
  `AuthSystem.Deps` plus a deliberately throwing template, and `MinimalTestRealm.deps` is `error(...)`.
- `StoringEmailHook`'s subject anonymization (#4) has no direct test — there is no `StoringEmailHook`
  spec at all, and framework subjects carry no URL, so an e2e assertion would be vacuous. The
  underlying `modifyContent` is covered by `EmailStoringSpec`.

### Mutations run after the fixes — all caught

| Mutation | Caught by |
|---|---|
| renderer handed `fallbackLocale` instead of the matched locale | `LocalizedEmailTemplateSpec`, plus 4 in `AuthEmailTemplatesSpec` |
| `requireAuthEmailEnvelope` neutered to `require(true)` | `AuthEmailEnvelopeSpec` (4) |
| `DefaultMessaging` hardcodes `Locale("en")` instead of the realm default | `DefaultMessagingLocaleWiringSpec` (2) |

**Red-team follow-up:** `.claude/tasks/20260728-redteam-i18n-email-templates.md` — rewritten for this
design (2026-07-28); the retired Markdown/substitution scenarios are listed there so a tester does not
re-run them.
