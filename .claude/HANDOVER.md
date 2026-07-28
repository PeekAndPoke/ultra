# Handover — 2026-07-27/28, `auth-increments`

Written to move a session between machines. **Delete or overwrite it when it stops being true** — a
stale handover is worse than none.

## Resume in three steps

1. On the other machine: `git fetch && git checkout auth-increments && git pull`
   Everything through **`2ae8036f`** is pushed. Nothing is uncommitted; the working tree was clean.
2. Check the environment prerequisites below (this is the part that usually bites).
3. Open a session in the repo root and paste the "opening line" at the bottom.

## Where we are

**Home base is the b2b SaaS showcase (`funktor-demo`).** Everything below is a detour off it. The
org-membership arc is complete and committed; its deferred leaves are the invite/accept consent flow,
the operator cross-realm view, red-team execution, and a docs prose pass.

The last three sessions ran one arc: **email → account activation → activation resend + frontend.**

- **Email test seam** (archived). The framework composes the whole sender chain at one site; in test
  mode it substitutes a capturing sender and never touches the app's provider. An app supplies only
  `useSender(devConfig) { ... }`. Two invariants that must not be broken are recorded in
  `.claude/tasks-archive/2026-07/20260727-framework-composed-email-sender.md`.
- **Account activation** — `.claude/tasks/20260727-account-activation.md` (BACKEND DONE, gate passed).
  "Not activated" is the PRESENCE of an `AuthRecord.PendingActivation` marker. Deliberately not a
  field on `AuthUser` (the only safe default locks out every existing account) and deliberately not
  "an unexpired verification token exists" (expired rows are filtered from every lookup, so that check
  erases itself after 24h — mutation-verified).
- **Resend + frontend** — `.claude/tasks/20260727-activation-resend-and-frontend.md` (gate passed,
  and the gate reshaped it). The first cut let resend take a bare email on an anonymous route, which
  would have turned sign-up from a one-shot into a sustained mail faucet. Now authorized by a
  single-use `AuthRecord.ActivationResendToken` minted only on the path that proves the password.
  **Read that task's Review record before touching this area** — it explains why several
  obvious-looking "improvements" are wrong.

One rule from that arc is easy to break by accident, so it is written on
`AuthRecord.PendingActivation` itself: **clearing the marker requires proving the mailbox AND
invalidating every earlier password.** A password reset does both. SSO sign-in proves the mailbox but
leaves the attacker's password intact — clearing there reopens the pre-hijack chain. Two reviewers
asked for exactly that change; it was rejected with reasons.

## The immediate open question

**i18n S7 — localized emails.** `.claude/tasks/i18n/20260722-i18n-s7-emails.md` is TODO/UNBLOCKED and
fully designed (whole-template-per-locale, Markdown + front-matter subjects, app-outer override
precedence, HTML-escaping as a hard acceptance criterion). Refreshed 2026-07-27 for what changed since
it was written: there are now THREE auth emails, `LanguageSettings` still has zero consumers, the two
mail e2e specs assert subject strings that move into templates, and the anonymizer must keep seeing a
real href once links come from templates.

**The user was asked to choose and has not answered yet:**

- **(a) S7 proper** — codegen mode, checker mode, Gradle plugin surface, a Markdown dependency. Wide
  blast radius.
- **(b) A vertical slice** — convert only the activation email (newest, smallest), proving the whole
  pipeline end to end at a fraction of the scope; the other two then follow mechanically.

Recommendation given: (b) if showcase momentum matters, (a) if the i18n machinery is the point.

## Open items, priority order

1. **i18n S7** — above.
2. `.claude/tasks/20260727-signup-mail-throttle.md` — anonymous sign-up is an unthrottled outbound-mail
   primitive. **Deferred by explicit user decision**; four options written up with trade-offs. Note
   that option B (per-user cooldown) specifically does NOT work for sign-up.
3. `.claude/tasks/20260727-messaging-followups.md` — 8 items; top three are the boot warning,
   `senderName` never reaching the `From` header, and anonymization not covering non-URL secrets.
4. `.claude/tasks/20260727-activation-resend-cooldown-atomicity.md` — check-then-act on the cooldown.
   LOW only because the single-use token sits in front of it. Needs a concurrency test.
5. **Policy decision the user still owes:** `.claude/tasks/20260726-sso-email-verification.md` — SSO
   signup links to an existing account without checking `email_verified`. Three options; couples to
   activation.
6. **Value-class ids Step 5 (`Slug`)** — `.claude/tasks/20260724-value-class-ids-migration.md`. Carries
   an open question: `isForbiddenInId` is a DENYLIST while `RealmId` uses an ALLOWLIST, and publishing
   the denylist as the shared id predicate steers the next id type toward the weaker pattern. Decide
   before writing it; the user then codifies the standing rule (agent drafts).
7. **Red-team tasks — COLLECTED, never executed during feature work:**
   `20260727-redteam-account-activation.md` (9 scenarios), `20260727-redteam-messaging.md`,
   `20260725-redteam-value-class-{orgid,userid}.md`, `20260726-redteam-value-class-emailaddress.md`.
8. **Dead surface awaiting the user's call:** `UserPermissions.hasAnyOrganisation`, `canAccessOrg`,
   `ownerIdsOf`; `MailingOverride.None`.
9. Doc collectors: `.claude/tasks/20260727-docs-messaging-test-seam.md`;
   `.claude/future-plans/20260726-vault-test-gap-sweep.md`.

## Environment prerequisites on the other machine

- **ArangoDB and MongoDB running.** Dev and test databases are separated: `funktor-demo-dev` /
  `funktor-demo-test` on both backends. **The Arango `funktor-demo-test` database must exist** or the
  suite fails at boot.
- `pnpm`, never `npm`.
- Sanity check before starting work — all three should be green:
  `./gradlew :funktor:auth:jvmTest :funktor:all:jvmTest :funktor-demo:server:test` then
  `./gradlew assemble`.
  Current baselines: `funktor:all` **137** tests, `EmailAndPasswordAuthSpec` **26**,
  `AccountActivationEmailE2eSpec` **7**, `AuthRecordStorage{Karango,Monko}Spec` **7 each**.

## Things a fresh session will not know

`CLAUDE.md` carries the project rules and is loaded automatically. These are the ones that are NOT in
it and that live in machine-local agent memory, so they do **not** travel:

- **Verification quirks.** kotest ignores `--tests`, so confirm a spec actually ran via
  `build/test-results/**/TEST-*.xml`. Avoid `--rerun-tasks` (kapt flakiness). MPP modules use
  `:jvmTest`; `funktor-demo:server` uses `:test`.
- **Never emit `\uXXXX` escapes or raw control characters in an edit** — they come out as raw bytes
  every time, in file writes and in match strings alike. Write `Char(0xNN)` instead, and additionally
  pin the property that makes a code point special or the test passes for the whole rejected class.
- **`shouldBe` is untyped**, so `valueClass shouldBe "literal"` rots silently.
- **Mutation-test every security-relevant change.** This session it caught four vacuous or
  wrong-reason tests, including one written in the same session — a "single-use token" e2e that was
  also satisfied by the cooldown, so deleting the token consumption left the suite green.
- **Back up files with `cp` before mutation testing, never restore with `git checkout`** — the file
  usually has other uncommitted work in it.
- **Kontainer:** a singleton that transitively injects a `dynamic` becomes `SemiDynamic`, i.e. one
  instance per request. Shared-state services must take zero constructor dependencies.
- **Adversarially verify every review finding against the code before accepting it.** Reviewers have
  been confidently wrong this session (e.g. "the demo doesn't mount the reset-password route" — it
  does), and so has the coordinator.
- **Escalate design-direction forks** with a recommendation plus options rather than deciding
  silently; **flag dead surface** rather than deleting it unilaterally.
- Before adding or bumping any dependency, **look up the newest version online** — never from memory.

## Opening line to paste on the other machine

> Read `.claude/HANDOVER.md`. We're on `auth-increments` at `2ae8036f`. Last arc was account
> activation + resend, both gated. The open question is i18n S7 (localized emails): full stage, or a
> vertical slice converting just the activation email first. Confirm the test baselines still pass,
> then let's pick up there.
