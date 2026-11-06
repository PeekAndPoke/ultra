# ULTRA-00289: Funktor auth activation, round 2: overlapping links, the name at activation, a weak-password answer, org realms

> **Status (2026-11-06): DONE.** gate PASS after round 3.** Plan confirmed by the maintainer on 2026-11-05 ("Go"). The round-2 MINOR list fixed on the maintainer's word of 2026-11-06 ("Fix them all"), confirmed CLEAN by round 3, whose MINORs are fixed too. Follow-ups exist: ULTRA-00292 (funktor-rest, the password in the log) and ULTRA-00293 (SSO names, 2.5).
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)

**Security-critical:** yes (the activation link sets the password and signs in; ULTRA-00231).

## Why this exists

Created from the ULTRA-00231 gate, round 1 (2026-11-04). Its two MINORs, 1.9 and 1.10, are parts C and D below. On
2026-11-05 the maintainer folded in the gate's two parked decisions, 1.12 and 1.13 (parts A and B).

- **1.12:** after the growing delay, anyone who signs the address up again rotates the activation link. That kills
  the link a real user may be typing a password into.
- **1.13:** a pre-registering attacker chose the display name, and it survived into the victim's account.
- **1.9:** a weak password is shown as a dead link. `AuthLoginApi.activateAccount` answers every `AuthError` the same
  way: 400, `{success: false}`, no info.
- **1.10:** org realms. `OrgSelectionRequired` is wasted, and with 0 orgs the page claims "You can sign in now".

## Decisions (maintainer, 2026-11-05)

1. Older activation links stay alive for a while after a re-send. That while is a per-realm setting with a sensible
   default; 30 minutes was named.
2. No profile fields before activation. The display name is a mandatory field on the activation page, "for now".
3. A new operation on `AuthUserAdapter` writes the name at activation, and it is required: no default.
4. Pending accounts from 0.114–0.117 that carry an attacker's name need no special handling, as there are no live
   apps.
5. ULTRA-00289 (1.9, 1.10) is folded in here.
6. A dedicated answer for a weak password.
7. Org realms: n orgs go to the login page's org picker; 0 orgs say "active" without "sign in now".
8. Breaking API changes are fine; nothing is online.

## The plan

### A. Older links overlap

- `RealmTokenConfig.activationLinkOverlap: Duration = 30.minutes`. It must not be negative. 0 means today's
  behaviour: only the newest link lives.
- On a re-send, the new token is created first, as today. Then each older `EmailVerificationToken` of the owner
  changes:
  - an expired one is removed;
  - a live one gets `expiresAt = min(its own, now + overlap)`.
  A link is never extended. This replaces `removeEmailVerificationTokens(exceptId)` on that path.
- The rest stays as it is:
  - Using any link removes all the others (`activateAccount`).
  - The cancel link is untouched.
  - Rows stay bounded: the growing delays allow about three live links, all inside the first 30 minutes.

### B. The name at activation

- `AuthSignUpRequest.EmailAndPassword.displayName` is removed. The pending account gets the default derived from
  the address (`CreateUserForSignupParams.of(email)`), which tells nothing the mail does not.
- `AuthActivateAccountRequest.displayName: String` is new and required.
- The server checks it before the token is touched, next to the password policy:
  - trimmed;
  - 1 to 100 characters;
  - no control characters.
- `AuthUserAdapter.activate(user, params: ActivateUserParams): Stored<USER>` is new and required.
  - `ActivateUserParams(displayName)` is a params class, so a later profile field does not break the interface
    again.
  - A realm without sign-up throws `AuthError.notSupported()`, as `createForSignup` does.
- **The order in the activation POST:**
  1. Check the policy and the name.
  2. Consume the token.
  3. Write the password.
  4. Win the marker.
  5. Remove the other passwords and the other links.
  6. `users.activate(...)`.
  7. Sign in.

  The name is written only after the activation has won. If the adapter throws, the account is active and keeps the
  default name; the POST logs it and answers 500 (or 400 for an `AuthError`: the same body and page outcome as a dead link, though a dead link is a 200). A
  pending account with a dead link would be worse.
- An activation through a reset or SSO asks for no name. Those accounts keep the default derived from the address.
- **The frontend:**
  - `ActivateAccountPage` gets a mandatory name field.
  - The sign-up form in `LoginController` drops its name field.
  - The TS SDK runtime (`login.ts`, `verifyRuntime.ts`) and the Vue demo's `LoginView.vue` follow.
- **Implementations to update:**
  - `TestUserRealm`;
  - `ActivationHardeningE2eApps`;
  - the test realm in `funktor/auth/src/jvmTest/kotlin/index_jvmTest.kt`;
  - the four funktor-demo realms (`AdminUserRealm`, `B2bRealm`, `B2b2cRealm`, `OperatorRealm`).

### C. A dedicated answer for refused input

- `AuthActivateAccountResponse.failure: Failure?`, with `WeakPassword` and `InvalidDisplayName`. Every refusal of the
  link stays one answer with no reason (a 200 `{success: false}`, as built; an `AuthError` is a 400), so a used link,
  an unknown link and an expired link remain indistinguishable.
- Both checks run before the token is looked at, so a reason tells nothing about the token.
- `ActivateAccountPage` shows the policy message or a field error and keeps the form. The link stays usable, as the
  token was not touched.

### D. Org realms

- **n orgs:** `AuthState.activateAccount` hands `OrgSelectionRequired` to `pendingOrgSelection`, and the page routes
  to the login page's picker.
- **0 orgs, or sign-in switched off** (`signIn == null`): the page says the account is active, without "sign in now".

### Not here

- Storing tokens hashed: ULTRA-00291.
- Session revocation: ULTRA-00136.

## Implementation notes

Deviations from the plan, and decisions taken while building:

- **The input checks live in `AuthRealm.activate`**, which gives the reason. The realm owns the policy, and the name
  rule applies to every provider. The provider checks the policy again before the token (2.4), for a realm that
  calls it directly; that answers a plain dead-link `null`.
- **A refusal of the input is a 200**, `{success: false, failure}`, the same status as a dead link. Before, a weak
  password was a 400 (`AuthError`). One status for every answer the realm gives keeps the page's handling in one
  place.
- **Expired older links are not removed at a re-send** (the plan said they would be). They stay bounded by the
  growing delays, and activation and retire remove them all. A re-send only caps the live ones, with a REPLACE, so a
  link an activation removed meanwhile cannot come back.
- **The name rule is `AuthDisplayName` in commonMain**, so the page and the server cannot disagree. On the
  maintainer's word ("only allow visible characters and characters that are not 'stripped'. Have better error
  messages"), it works on code points and refuses rather than strips. `refusalOf` returns a `Refusal`: the problem
  (`Blank`, `TooLong`, `LineBreak`, `Invisible`, `NotDisplayable`, `StartsWithMark`, `TooManyMarks`), the code point
  and where it sits; the page's field and its server-refusal banner show `Refusal.message`. Kept: joiners between
  visible characters or after an Indic virama, one variation selector after a base, an ideographic one only after a
  CJK ideograph. Beyond the BMP common code has no categories: listed ranges are refused, the rest taken as visible.
- **`TestUserRealm` runs with `activationLinkOverlap = 0`**, so `AccountActivationEmailE2eSpec` pins the 0 path
  end to end. The hardening app runs the 30-minute default.
- **`ActivateAccountPage` edits the current state, not the rendered one** (`editChoice`). With two fields, typing
  the password before a re-render undid the name. The browser spec found it.
- **Hardening hooks:** `OrgGate` turns the hardening realm into an org realm; `ActivateGate` makes the name write
  fail.
- **Found outside this flow (funktor-rest), fixed in ULTRA-00292:** a request body that misses a required field
  answered 500, not 400. Slumber's `AwakerException` is not translated by `SlumberRestCodec.awakeBody`. Its message carries the
  whole input map, and `ApiStatusPages` logs it (`logInternalError`) and echoes `cause.message` even in production.
  So a malformed sign-in, reset or activation body puts the **cleartext password** into the server log. It predates
  this change, and every endpoint has it. The e2e row "A refused NAME…" pins only what this flow needs: refused, and
  the link survives. Now ULTRA-00292, which pins the 400 there.
- **The TS SDK runtime and the Vue demo needed no change** (the plan said they would follow). `login.ts` is the
  sign-in flow only; neither it, `verifyRuntime.ts` nor `LoginView.vue` has a sign-up form, an activation page or a
  `displayName`. The generated request types follow on the next regeneration.
- **Not touched:** the generated SDK under `funktor-demo/sdkgen-app/src/funktorsdk/` was already stale before this
  change (it still has `activation-required`). It is generated output, not regenerated here.

## Mutation checks (2026-11-05)

One mutant at a time, auth and funktor-all JVM suites (523 tests) or the auth browser suite (16), `cp` backup,
`cmp`-verified restore. Each went red in the test it targets, on both backends where storage is involved.

- mutation-checked: the cap drops its `> cap` filter, so it extends → red ("never EXTENDS", Karango + Monko)
- mutation-checked: the cap writes nothing → red ("OLDER link stays usable…", past the overlap, both backends)
- mutation-checked: the overlap is ignored, every re-send removes → red (the overlap e2e on both backends, plus 3 unit
  fakes)
- mutation-checked: overlap 0 caps instead of removing → red (unit: "with NO overlap…"). End to end this mutant is
  equivalent, since a link capped to "now" is already dead; only a dead row stays until activation removes it.
- mutation-checked: the name check is skipped → red (realm unit test, "A refused NAME…" e2e)
- mutation-checked: the name check runs after the token → red (same two)
- mutation-checked: `users.activate` is skipped → red ("A refused NAME…" e2e; "If writing the display name FAILS…",
  both backends)
- mutation-checked: the password policy check is skipped → red (realm unit test, "A WEAK password…" e2e)
- mutation-checked: `AuthState` drops the org selection → red (page spec, org handoff)
- mutation-checked: the page does not navigate to the picker → red (page spec, org handoff)

The round-1 fixes, the same way (2026-11-05):

- mutation-checked: `Failure.WeakPassword` gets its `@SerialName` back → red ("A WEAK password…": the kotlinx decode
  fails, "does not contain element with name 'WeakPassword'", the very bug of 1.1)
- mutation-checked: the login page reads any pending selection → red (page spec: the picker shows instead of
  "Sign In")
- mutation-checked: the take is not one-shot → red (same)
- mutation-checked: `logout()` keeps the selection → red (page spec: the selection is still there)
- mutation-checked: format characters allowed → red (`AuthDisplayNameSpec`, the realm unit test)
- mutation-checked: tag characters allowed → red (`AuthDisplayNameSpec`)
- mutation-checked: blank letters allowed → red (`AuthDisplayNameSpec`)
- mutation-checked: the joiners refused too → red (`AuthDisplayNameSpec`, the Persian ZWNJ name)
- not mutation-checked: "sign-up ignores an old client's `displayName`". The tolerance lives in Slumber's
  `DataClassAwaker`, not in this change; the e2e row guards it.

The round-2 MINOR batch and the round-3 fixes, the same way (2026-11-06; auth JVM 282, funktor-all 246, browser 23):

- mutation-checked: no supplementary ranges → red (`AuthDisplayNameSpec`: the tag block, the message rows)
- mutation-checked: a run of variation selectors allowed → red (`AuthDisplayNameSpec`)
- mutation-checked: joiner neighbours unchecked → red (`AuthDisplayNameSpec`: joiners only, the message rows)
- mutation-checked: a leading mark allowed → red (`AuthDisplayNameSpec`: marks only)
- mutation-checked: an unpaired low surrogate passes → red (`AuthDisplayNameSpec`)
- mutation-checked: noncharacters pass → red (`AuthDisplayNameSpec`)
- mutation-checked: the position not named → red (`AuthDisplayNameSpec`, the message test)
- mutation-checked: the page shows only `DESCRIPTION` → red (page spec "…the page says WHY")
- mutation-checked: the submit posts the rendered state → red (page spec, the password manager: the empty password)
- mutation-checked: the provider's policy check dropped → red (provider unit test: the token lookup is reached)
- mutation-checked: the name not written → red (the email-first e2e on Karango AND Monko)
- mutation-checked: U+2065 allowed → red; an ideographic selector on any letter → red; no virama allowance → red (the
  Bengali name refused); no mark cap → red; lone surrogates displayable → red (all `AuthDisplayNameSpec`)

## Review record (the ledger)

### Round 1: level 1 (`reviewer-high` ×3), blind, uncommitted on `d6786fe5`: NOT CLEAN (2 MAJOR)

Implementation & code style: 1 MAJOR, 3 MINOR, 6 NIT. Domain: CLEAN, 2 MINOR, 4 NIT. Security: 1 MAJOR, 1 MINOR. Overlaps
merged; each finding was checked against the code before acting.

| # | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1.1 | MAJOR | `AuthActivateAccountResponse.kt:24,28` | `Failure` carried `@SerialName`s. Slumber (server) writes `Enum.name` and ignores them; kotlinx (browser) reads by them. A refusal never reached the page: decode error → "could not reach the server", and a retry loop (impl) | fix | Verified: `EnumCodec` KDoc says SerialName is not honoured; the browser `ApiClient` decodes with kotlinx. The `@SerialName`s are gone (as `AuthProviderModel.Capability`). The `activate` helper of `AccountActivationEmailE2eSpec` now also decodes the raw body with kotlinx (the demo apps' codec config) and compares `success` and `failure`; the hardening spec does not, which is enough, as the server codec is the same on both backends. No test crossed the two codecs before |
| 1.2 | MAJOR (security), MINOR (impl, domain) | `LoginController.kt:433-435` | The login page seeded its picker from ANY pending selection; `logout()` does not clear it. A selection A left behind surfaced for B on the next visit, and B could finish signing in as A within 5 min. Reopens the ULTRA-00068 bystander case | fix | Verified: `logout()` left `pendingOrgSelection`; the mount read was unconditional. Now `AuthState.takeActivationOrgSelection()` hands over only an activation's selection, once (any other write resets the flag), and `logout()` clears the selection. Page spec: after the handoff, leaving and reopening the login page shows "Sign In"; a logout leaves no selection |
| 1.3 | MINOR | `AuthDisplayName.kt:7-8,24-29` | The rule let through bidi marks, zero-width spaces, word joiners, BOM, tag characters (hidden text) and blank-rendering letters; the KDoc claimed more than it refused (security; NIT in impl, domain) | fix | Verified. Now refuses every BMP `CharCategory.FORMAT` except ZWNJ/ZWJ (corrected in round 2: the check is per UTF-16 unit, so nothing beyond the BMP; see 2.1), the tag block U+E0000–E007F, and the Hangul fillers and blank braille cell. New `AuthDisplayNameSpec` (commonTest, so JVM and JS) with accepted names in other scripts and emoji. Trade-off: the subdivision flag emoji (England, Scotland, Wales) use tag characters and are refused |
| 1.4 | MINOR | `AccountActivationEmailE2eSpec.kt` | Acceptance "sign-up ignores a `displayName` sent by an old client" had no test (domain, impl) | fix | The refused-name e2e now signs up with a raw body carrying `displayName` and asserts the address-derived default |
| 1.5 | MINOR | ticket | Acceptance C asked for a 400 page spec; the notes chose a 200. Header stale (impl, domain) | fix | Ticket updated |
| 1.6 | NIT | `AuthRealm.kt:349-354` | `catch (Throwable)` logged a `CancellationException` as a failed name write (impl, domain) | fix | Rethrown first, as `retirePendingAccount` does |
| 1.7 | NIT | `AuthRealm.kt:349-354`, ticket | An `AuthError` from the adapter answers 400, not the 500 the ticket said (domain) | fix | Ticket wording corrected; the behaviour is acceptable (the page shows "link not usable"), reachable only by a misconfigured realm |
| 1.8 | NIT | `AuthRealm.kt:339-346` | The input checks run before the provider can say "not supported": a weak password to the GitHub provider gets `WeakPassword`, a strong one 400 (domain) | accept | Tells nothing about a token or an account; aiming the activation POST at an SSO provider is caller error |
| 1.9 | NIT | `TestUserRealm.kt:82` | KDoc said only the delays deviate (impl) | fix | Reworded |
| 1.10 | NIT | `ActivateAccountPage.kt:7` | Import order (impl) | fix | Sorted |
| 1.11 | NIT | `ActivationHardeningE2eApps.kt:226` | Missing blank line (impl) | fix | Added |
| 1.12 | NIT | `ActivateAccountPageSpec.kt:179-211` | The refused-then-retry case re-posts unchanged input; a corrected name is not checked in the body (impl) | fixed (maintainer 2026-11-06): page spec, a corrected name on the next POST | Not a defect: after a refusal the state is a `Choose` again, so the same `editChoice` path applies (corrected in round 2: no spec edits a field after a refusal) |

### Round 2: level 2 (`reviewer-xhigh` ×3), two-phase, uncommitted on `d6786fe5`: CLEAN (0 CRITICAL, 0 MAJOR)

Phase 1: implementation 3 MINOR + 3 NIT, domain 3 MINOR + 4 NIT, security 3 MINOR; all three CLEAN. All three disclosed
that the ticket's Review record came with their first read of the ticket, so phase 1 was not fully blind; each
formed its findings from the code. Phase 2: every round-1 row ACCEPTED by all three charters, none refuted. One
Reason text was corrected (1.1: only the `AccountActivationEmailE2eSpec` helper crosses the codecs). Each MINOR
below was checked against the code and holds. Per the loop, open MINORs go to the maintainer as a list.

| # | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2.1 | MINOR (all three) | `AuthDisplayName.kt:7-9,33-42`, `CHANGELOG.md:63` | The rule checks one UTF-16 unit at a time, so no supplementary-plane character is classified: format controls (U+1D173–1D17A, U+1BCA0–1BCA3, U+13430–1343F) render "Ad"+U+1D173+"min" as "Admin"; a run of variation selectors U+E0100–E01EF smuggles hidden bytes past the tag check; unpaired surrogates pass. The KDoc and CHANGELOG claim "every invisible format character" | fixed (maintainer 2026-11-06) | Verified (`Char.category` of a surrogate half is `SURROGATE`). Proposed: decode code points; refuse unpaired surrogates and a range list of default-ignorables beyond the BMP; allow ONE variation selector after a base character (emoji VS16, CJK ideographic variants), refuse a run |
| 2.2 | MINOR (impl, domain) | `AuthDisplayName.kt:19-25` | A name of only joiners, variation selectors or combining marks (U+200D, U+FE0F, U+034F) is accepted and renders blank | fixed (maintainer 2026-11-06) | Verified. Proposed: require one character outside ZWJ/ZWNJ, Mn, Me, Cf, Zs |
| 2.3 | MINOR (domain) | `AuthDisplayName.kt:16`, `index_js.kt:13-16` | `DESCRIPTION` names only the length; a name pasted with a trailing U+200E or a soft hyphen is refused under "1 to 100 characters", and the mandatory field blocks the user with no hint | fixed (maintainer 2026-11-06): refused with a message naming the character and its place, not stripped | Verified (`trim()` keeps Cf). Proposed: strip format characters at the ends like whitespace, and name the content rule in the message |
| 2.4 | MINOR (security), NIT (domain) | `EmailAndPasswordAuth.kt:535-563` | The provider's `activateAccount` writes the password without its own policy check; `setPassword` (:614) and the reset (:721) keep theirs. A realm overriding `activate` that calls the provider directly stores a weak password | fixed (maintainer 2026-11-06) | Verified; the only caller today is `AuthRealm.activate` (:346). Proposed: re-add the check before the token lookup (no oracle; the realm still produces the reason) |
| 2.5 | MINOR (security) | `GithubSsoAuth.kt:257`, `GoogleSsoAuth.kt:199` | Older than this change: SSO sign-up names skip `AuthDisplayName` (only trimmed), and a GitHub `"name": null` becomes the literal name `"null"` (`JsonNull.content`) | out-of-scope → ULTRA-00293 (maintainer 2026-11-06: the local part as the fallback) | Verified. Not this change's: the rule is scoped to the activation page by decision 2 |
| 2.6 | MINOR (impl) | `CHANGELOG.md:58-84` | The new abstract `EmailAndPasswordAuth.Services.capEmailVerificationTokens` (public, via `Factory.invoke(services=…)`) is not named as breaking | fixed (maintainer 2026-11-06) | Verified; the ULTRA-00231 entry named `removePasswordRecords` |
| 2.7 | NIT (impl) | `ActivateAccountPage.kt:287,133` | The submit posts the render-time state; a password manager that fills and clicks in one task posts the stale password (refused, form kept, second click works). An edit made while the POST is in flight is overwritten by the refusal | fixed (maintainer 2026-11-06) | Self-recovering. Proposed: submit the current `displayState` |
| 2.8 | NIT (impl) | `ActivationHardeningE2eSpec.kt:168-194` | The written name is read back on Karango only (`TestUserRealm`), never on Monko | fixed (maintainer 2026-11-06) | The write is realm adapter code, not framework storage |
| 2.9 | NIT (impl, domain) | `CHANGELOG.md:18,75-76` | The `getEmails`-abstract note is in the ULTRA-00118 entry and again in ULTRA-00289's | fixed (maintainer 2026-11-06) | Keep it in ULTRA-00118 |
| 2.10 | NIT (domain), withdrawn in phase 2 | `AuthDisplayName.kt:37` | U+180E MONGOLIAN VOWEL SEPARATOR (Cf) is refused; traditional Mongolian spelling needs it, as ZWNJ/ZWJ are needed elsewhere | withdrawn | Proposed: add it to the exceptions |

**Probed and CLEAN in round 2** (in addition to round 1's list): the one-shot org handoff (every write resets the
flag, logout clears, the login page reads only the take, no redirect bypasses it); the cross-codec wire for
`Failure`; a 400 or 500 decoding into the page; two live links submitted together (one winner, the loser removes its
own password); `Duration.INFINITE` as an overlap; a failed re-send mail now leaving the older link alive; Karango's TTL
index culling capped rows; real names in Persian, Indic, Sinhala, Hebrew with niqqud, Khmer, and emoji ZWJ sequences
accepted on JVM and JS alike.

**Probed and CLEAN** (all three charters, round 1):
- The order in `AuthRealm.activate`, and what each failure leaves behind.
- The overlap never extends a link and cannot revive a removed one: `replaceIfUnclaimed` never inserts, and a missing
  `claimedBy` matches null on both backends.
- Interleaved re-sends are pre-existing (ULTRA-00130), and the cap improves them.
- A refusal reason is no oracle: both checks run before the token, and used, unknown and expired links stay one answer.
- The pre-registration name channel is closed: sign-up has no name, and the auth mails do not render one.
- A failing name write leaves an active, consistent account, and its log line has no password.
- `getEmails` abstract fails closed; a null still means "not verified".
- No migration is needed: no stored shape changed.
- Code style: imports, KDoc links, branding.

**Recorded, not this ticket's to fix:** a request body that misses a required field answered 500, and the logged
`AwakerException` carried the input, cleartext password included (funktor-rest-wide). ULTRA-00292.

### Round 3: level 1 (`reviewer-high`, all three charters), confirmation of the MINOR batch: CLEAN (0 CRITICAL, 0 MAJOR)

| # | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3.1 | MINOR (security, domain) | `AuthDisplayName.kt:110` | U+2065, a reserved default-ignorable (category UNASSIGNED, not FORMAT), passes and renders as nothing: "Ad"+U+2065+"min" | fixed | Verified; permanently reserved, so not table drift. In the blank set, spec row |
| 3.2 | MINOR (security) | `AuthDisplayName.kt:95-96` | One ideographic variation selector after EVERY letter carries a hidden byte per letter (26 in a 78-char name) | fixed | Verified. U+E0100–E01EF only after a CJK ideograph; FE00–FE0F still after any base (16 values, needed for emoji) |
| 3.3 | MINOR (domain) | `AuthDisplayName.kt:90-93` | Older Bengali/Malayalam end a word with virama + ZWJ; refused as "invisible, please delete it", which changes the glyph | fixed | Verified. A joiner may follow an Indic virama with nothing visible after it |
| 3.4 | NIT | `AuthDisplayName.kt:74,55` | A broken surrogate is refused with "(?)" | fixed | The lone half stays in the code point list, so the message names it and its place |
| 3.5 | NIT | `AuthDisplayName.kt:42` | "characters" counts UTF-16 chars | fixed | The message says an emoji counts as two or more |
| 3.6 | NIT | `AuthDisplayName.kt:99` | Marks on one letter are not capped ("a" + 99 × U+0301) | fixed | UAX #15's stream-safe limit: more than 30 in a row is `TooManyMarks` |
| 3.7 | NIT | this ticket | Implementation notes and the ledger still describe round 2 | fixed | This update |

**Probed and CLEAN in round 3:** code-point decoding and pair arithmetic; check order (invisible before mark, so
U+17B4/17B5 are refused); noncharacters in every plane; every supplementary format control of Unicode 16 listed;
joiner chains, a selector after a space, names of only joiners, selectors or marks; the rainbow and pirate flags,
skin-tone ZWJ sequences, keycaps, Persian ZWNJ, Sinhala "Sri" and CJK variation sequences accepted; `trim()` alike on
JVM and JS; the page's field rule and banner; 2.4 no oracle; 2.6/2.9 in the CHANGELOG; 2.7 reads the state at
validation time and a refusal keeps edits made during the POST; 2.8 on both backends; 1.12 cannot pass vacuously.

Round-3 fixes are MINOR and NIT only, so they do not loop (`.claude/skills/review-loop/`); they are mutation-checked
below.

**Verdict:** gate PASS (round 3 clean; follow-ups ULTRA-00292 and ULTRA-00293 exist).

## Acceptance

- [x] **A:** e2e on both backends.
  - [x] Inside the overlap, an older link still activates, and then the newer one is dead.
  - [x] After the overlap, the older link is dead.
  - [x] A link is never extended.
  - [x] With overlap 0, only the newest link lives.
- [x] **B:** e2e.
  - [x] Activation writes the given name.
  - [x] Sign-up ignores a `displayName` sent by an old client.
  - [x] A blank, overlong or control-character name is refused before the token: the link still works afterwards.
  - [x] A throwing adapter leaves the account active.
- [x] **C:** e2e for both reasons, plus a page spec for a refused answer (a 200 with `failure`, see the
      implementation notes), and a check that the browser's codec reads the reason the server's codec wrote.
- [x] **D:** e2e on an org realm with sign-up, with 0, 1 and n orgs, plus a page spec for the picker handoff.
- [x] Mutation checks on A, B and C, the security-relevant paths.
- [x] CHANGELOG `## Unreleased` names the breaking API. ULTRA-00290 (docs) is updated with the settled API.
- [x] ULTRA-00127 (red team) has the new scenarios: a stale link inside the overlap, and the name field as a phishing
      carrier.

## History

- 2026-11-04: created from the ULTRA-00231 gate (1.9, 1.10).
- 2026-11-05: 1.12 and 1.13 folded in on the maintainer's decisions; rewritten as the plan above.
- 2026-11-06: the round-2 MINOR list fixed on the maintainer's word, round 3 CLEAN, its MINORs fixed and
  mutation-checked; follow-ups ULTRA-00292, ULTRA-00293; DONE. Docs: ULTRA-00290, updated with the settled API
  (written last, as ULTRA-00291 and ULTRA-00293 still move the same surface).
- 2026-11-06: DONE.
