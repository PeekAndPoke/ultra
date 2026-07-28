# i18n S7 vertical slice — the account-activation email, localized

**Status:** SUPERSEDED / NOT SHIPPED (2026-07-28) — the code was written, gated, and then reverted.
Replaced by `.claude/tasks/20260728-auth-email-templates-in-code.md`.

> ## ⚠ This slice was built and deliberately thrown away. Nothing here is in the codebase.
>
> The implementation below (Markdown templates + `{{placeholder}}` substitution + regex escaping)
> passed its own tests and then failed its review gate on design, not on defects-in-the-small.
> The user's call: a substitution engine eventually mails `Hello ###FIRSTNAME###!` to a real person —
> and Reviewer 1 had just proved that exact hole was live and untested for `senderName`.
>
> **The gate output is the valuable part of this file** and is why it is kept rather than deleted:
> the confirmed-findings list below is what justified the redesign, and the red-team collection was
> rewritten against it. The mutation-test table is also still instructive — every mutation was
> caught, which is precisely why "the tests pass" was not a good enough reason to keep the design.
>
> Do not use the Spec / Implementation notes below as instructions.
**Plan:** `.claude/tasks/i18n/20260722-i18n-s7-emails.md` → option (b), chosen by the user 2026-07-28
**Security-critical:** yes (HTML/attribute escaping of substituted values; a template now sits between
the framework and the `href` the anonymizer must keep seeing) → red-team follow-up required



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `853248f7` | 2026-07-28 | 15 | feat(auth): localized auth emails, rendered in code |

### Files changed (15)

**funktor/all**
- `funktor/all/src/jvmTest/kotlin/AccountActivationEmailE2eSpec.kt`
- `funktor/all/src/jvmTest/kotlin/TestUserRealm.kt`

**funktor/auth**
- `funktor/auth/build.gradle.kts`
- `funktor/auth/src/jvmMain/kotlin/AuthRealm.kt`
- `funktor/auth/src/jvmMain/kotlin/emails/AccountActivationEmailTemplate.kt`
- `funktor/auth/src/jvmMain/kotlin/emails/AuthEmailTemplates.kt`
- `funktor/auth/src/jvmMain/kotlin/emails/PasswordChangedEmailTemplate.kt`
- `funktor/auth/src/jvmMain/kotlin/emails/PasswordRecoveryEmailTemplate.kt`
- `funktor/auth/src/jvmTest/kotlin/emails/AuthEmailTemplatesSpec.kt`
- `funktor/auth/src/jvmTest/kotlin/index_jvmTest.kt`

**funktor/messaging**
- `funktor/messaging/build.gradle.kts`
- `funktor/messaging/src/jvmMain/kotlin/storage/StoringEmailHook.kt`
- `funktor/messaging/src/jvmMain/kotlin/templates/EmailLayout.kt`
- `funktor/messaging/src/jvmMain/kotlin/templates/EmailTemplate.kt`
- `funktor/messaging/src/jvmTest/kotlin/templates/LocalizedEmailTemplateSpec.kt`

## Why a slice and not S7 proper

The user chose (b) over the full stage. S7's blast radius is wide — codegen emails mode, checker
rules, a Gradle plugin surface, a Markdown dependency, all three auth mails at once. The activation
mail is the newest and smallest of the three and already has the most e2e coverage
(`AccountActivationEmailE2eSpec`, 7 tests), so it is the cheapest place to prove the whole pipeline:
template → Markdown → substitution → escaping → layout → locale resolution → anonymization.

`password-changed` and `account-recovery` then follow mechanically, against machinery that is real
rather than speculative.

**Explicitly deferred to the follow-up** (do NOT build these here):

- `password-changed.{en,de}.md`, `account-recovery.{en,de}.md`
- i18n checker rules for templates (`requiredLangs` parity, front-matter subject placeholders,
  the partial-override warning)
- `I18nPlugin` emails mode / build-time baking — the slice loads templates from the classpath
- Multipart text alternative (`EmailBody` models Text OR Html; multipart is a model change)

## Spec

- [x] `account-activation.en.md` + `account-activation.de.md`, Markdown body + front-matter `subject:`
      — `funktor/auth/src/jvmMain/resources/i18n/emails/`
- [x] Layout extracted out of `DefaultMessaging` — `EmailLayout.kt`
- [x] Template source + resolver with **APP-OUTER precedence** — `EmailTemplateResolver.kt:39`
- [x] Recipient locale resolution: `user.language.messaging` → realm default → `en`, the stored tag
      run through `Locale.parse` — `AuthRealm.kt:121` (`localeFor`), `AuthRealm.kt` `defaultLanguage`
- [x] Recipient locale reaches the render — see the deviation note below; the `Messaging` INTERFACE
      is deliberately unchanged
- [x] Substituted values escaped for text AND double-quoted-attribute context; `href`/`src`
      scheme-checked — `HtmlValueSubstitution.kt`, `UrlSanitizer.kt`
- [x] Single-pass substitution preserved — pinned by
      `HtmlValueSubstitutionSpec` "a value containing a placeholder is NOT re-scanned"

## Test evidence

- [x] Escaping adversarial tests — `HtmlValueSubstitutionSpec` (12), `UrlSanitizerSpec` (8)
- [x] Front-matter / template-parsing unit tests — `EmailTemplateSpec` (12)
- [x] Resolver tests incl. APP-OUTER and the traversal guard — `EmailTemplateResolverSpec` (9)
- [x] Renderer/Markdown integration — `EmailTemplateRendererSpec` (7)
- [x] Subject header-injection tests — `SubjectSubstitutionSpec` (7)
- [x] `AccountActivationEmailE2eSpec` renders in `en` AND `de`, no mixed-language mail, and the
      German link still activates (8 tests, was 7)
- [x] The anonymizer still sees a real href in the rendered template — the pre-existing positive
      assertions (`storedBody shouldContain "#anonymized"` + `shouldContain "Click the link below…"`)
      still pass against the templated body
- [x] **Mutation-tested — all six mutations caught:**

  | Mutation | Caught by |
  |---|---|
  | drop `"` → `&quot;` from `escapeHtml` | `HtmlValueSubstitutionSpec` (2) |
  | `UrlSanitizer.sanitize` returns its input | `UrlSanitizerSpec` (6) + `HtmlValueSubstitutionSpec` (2) |
  | `SubjectSubstitution.sanitize` returns its input | `SubjectSubstitutionSpec` (3) |
  | resolver flipped to locale-outer | `EmailTemplateResolverSpec` APP-OUTER test |
  | `percentEncodeUrls(true)` (mangles the token) | `EmailTemplateRendererSpec` **and 5 e2e tests** |
  | `localeFor` ignores `user.language` | the German e2e test, alone |

- [x] Full commands run + green (2026-07-28):
      `./gradlew :funktor:messaging:jvmTest :funktor:auth:jvmTest :funktor:all:jvmTest :funktor-demo:server:test`
      then `./gradlew assemble`

| Suite | Before | After |
|---|---|---|
| `funktor:messaging:jvmTest` | 51 | **106** (+55) |
| `funktor:auth:jvmTest` | 107 | 107 |
| `funktor:all:jvmTest` | 137 | **138** (+1) |
| `funktor-demo:server:test` | 47 | 47 |

## Baselines to hold (verified green on this machine 2026-07-28, before any change)

| Suite / spec | Count |
|---|---|
| `funktor:auth:jvmTest` | 107 |
| `funktor:all:jvmTest` | 137 |
| `funktor-demo:server:test` | 47 |
| `EmailAndPasswordAuthSpec` | 26 |
| `AccountActivationEmailE2eSpec` | 7 |
| `PasswordResetEmailE2eSpec` | 3 |
| `AuthRecordStorage{Karango,Monko}Spec` | 7 each |

The two mail e2e specs assert subject strings that move into templates. Update them **deliberately**
rather than loosening them until they pass, and keep `single()` on the href — an auth mail carrying
exactly one link is a property, not an accident.

## Implementation notes

Decisions made while building — keep short, link code as `path/File.kt:line`.

- **Markdown dependency:** `org.commonmark:commonmark` **0.29.0** — latest release, checked on Maven
  Central 2026-07-28 (published 2026-06-20), per the standing "look it up online" rule.
- **Substitute AFTER rendering Markdown, not before.** Rendering first means the template author
  controls all markup and every substituted value lands in a known HTML context that can be escaped.
  Substituting first would let a value carrying `[x](javascript:…)` or raw HTML become markup.
- **`percentEncodeUrls(false)` is set explicitly** on the `HtmlRenderer`
  (`EmailTemplateRenderer.kt:28`), although it is also commonmark's default. With it on, `{{` in a
  link destination becomes `%7B%7B`, the slot is never filled, and the mail goes out with a literal
  `%7B%7BactivationUrl%7D%7D` link — a failure invisible to every unit test. Mutation-verified: the
  e2e suite does catch it.
- **DEVIATION from the S7 spec line "recipient locale threaded through `Messaging`".** The
  `Messaging<USER>` INTERFACE is unchanged; `DefaultMessaging` derives the locale from the `user` it
  is already handed (`localeFor`). Threading a `locale` parameter would break every app that
  implements `Messaging` in exchange for nothing — the parameter's only possible source is
  `user.language`, which every implementation already has. Revisit only if a caller appears that
  knows a better locale than the user record does (e.g. mailing in the language of the admin who
  triggered an action).
- **Templates are classpath resources, not build-time-baked.** `src/jvmMain/resources/i18n/emails/`
  rather than the `src/<sourceSet>/i18n/` convention the yaml catalogs use, because baking is the
  `I18nPlugin` emails mode that this slice defers. When S7 proper adds it, the source location moves
  and `ClasspathEmailTemplateSource` becomes one source among several.
- **Path-traversal guard on the locale, and it is not theoretical.** `language.messaging` is
  free-form and a profile UI will eventually let users write it; `Locale.parse` normalises case and
  separator but does not restrict the character set. Both the resolver (drops unusable tags from the
  chain) and `ClasspathEmailTemplateSource` (regex-validates before touching a path) hold the line,
  and `EmailTemplateResolverSpec` asserts the crafted tag never reaches a source at all.
- **Subject sanitizing is separate from HTML escaping.** A subject is a header value, so the risk is
  CR/LF (header injection), not markup — and HTML-escaping it would put a literal `&amp;` in the
  recipient's inbox list. See `SubjectSubstitution.kt`.
- **`funktor:all`'s `TestUser` gained a `language` field**, because the realm used by the e2e specs
  had no way to express one. `LanguageSettings` now has its first consumer in the repo.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up:** `.claude/tasks/20260728-redteam-i18n-email-templates.md` (to be written at the
end of this task — collected, NOT executed)
