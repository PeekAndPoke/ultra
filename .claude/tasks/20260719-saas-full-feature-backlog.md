# Full-SaaS Feature Backlog (org lifecycle, billing, operator surface)

**Status:** BACKLOG — RECORD ONLY (2026-07-19). Provisional working hypotheses, revisable. Nothing
here is scheduled; the auth-orgs foundation (`20260717-auth-orgs-foundation.md`) comes first.
**Purpose:** capture everything a full multi-tenant SaaS needs beyond the org/auth foundation, and
decide the core-vs-app boundary + extension-point strategy.

---

## 0. The framing question: core vs. app-specific (+ extension points)

The guiding split, applied throughout:

- **CORE** owns the *mechanism* and the *data spine* every SaaS needs identically: org lifecycle,
  ownership, invitations, the billing data model + Stripe/webhook plumbing + status tracking,
  billing emails, the operator surface primitives, and the extension mechanisms themselves.
- **APPS** own the *domain meaning*: which roles exist and what they permit, which plans exist and
  what features they unlock, domain entities, product-specific invoice line items and operator
  metrics, and extra signup fields.

**Extension-point patterns already established in funktor — reuse these, don't invent new ones:**
1. **Realm hooks** (the O3 pattern): app subclasses a realm and overrides `suspend fun` hooks
   (`getAccessibleOrgs`, `resolveSelectedOrg`, `createUserForSignup`, …). Add hooks for signup
   fields, role catalog, plan resolution.
2. **Kontainer-collected registries** (the `ApiFeature` / `OnAppStarting` pattern): apps register
   implementations as `singleton`/`dynamic`; core auto-collects `List<T>`. Use for: plan catalog,
   role catalog, billing-event handlers, operator tools, webhook handlers.
3. **Config-driven** (`AppConfig` subtype): Stripe keys, plan definitions, feature flags.
4. **Post-event hooks** (the `EmailHooks` pattern): `onPaymentSucceeded`, `onSubscriptionChanged`,
   `onOrgCreated`, `onOwnershipTransferred` — apps subscribe without touching core flow.
5. **Document templates** (the planned document layer, `20260717-saas-foundation.md` Track C/D):
   apps supply block-based invoice templates; core renders to PDF (incl. ZUGFeRD/XRechnung).

**Proposed module layout (provisional):**
- `funktor/saas` (exists) — orgs, branches, membership, ownership, invitations.
- `funktor/billing` (new) — plans, subscriptions, payments, invoices, Stripe adapter, webhooks,
  billing emails. Depends on `saas` + `messaging` + the document layer (invoices).
- `funktor/operator` (new) — operator realm + cross-tenant admin API primitives + impersonation.
- App layer — role/plan catalogs, invoice templates, operator metrics, domain.

---

## 1. Create your own org — minimal info

- **Minimal:** org **name** only (→ slug auto-derived + uniqueness check). The creating user becomes
  the first **owner**. Everything else deferred/optional.
- **Optional-at-creation / fill-later:** billing email, country + locale (tax/invoice), timezone,
  VAT id + legal name + address (needed before first invoice, not before first use).
- Already sketched: `SignupOrgBehavior.CreateOwnOrg` (O2). Wiring: on signup create `Organisation`
  + an `OrgMembership(owner)` for the user, in the realm's `createUserForSignup`/a create-org hook.
- **Open:** collect company/tax details lazily (a "complete your billing profile" gate before
  paid actions) vs. up front. Recommend lazy — lowest signup friction.

## 2. Ownership of org — multiple owners? transfer?

- **Ownership is a CORE concept** (it gates billing, member management, deletion), distinct from
  app roles. Model as a structural role/flag on `OrgMembership` (e.g. `owner`, plus `admin`).
- **Multiple owners: yes.** Support N owners.
- **Invariant (core-enforced):** an org always has ≥1 owner — can't remove/demote the last owner,
  can't leave as sole owner.
- **Transfer:** an explicit atomic action — promote member X to owner (+ optionally demote self).
  Sole-owner transfer = promote-then-demote in one step. Audited.
- **Actions:** `addOwner`, `removeOwner` (last-owner guard), `transferOwnership`. All owner-gated.
- **Open:** is "admin" (manage members/settings but not billing) a distinct core role? Likely yes —
  owner ⊃ admin. Keep owner+admin core; everything else app.

## 3. Invite users to org

- **`Invitation` entity** (core, in `funktor/saas`): orgId, invitee email, assigned roles,
  invitedBy, status (pending/accepted/revoked/expired), token, expiresAt.
- **Flow:** owner/admin invites by email → email w/ accept link (reuse the single-use token pattern
  like `PasswordRecoveryToken`) → accept: existing user joins / new user signs up then joins →
  `OrgMembership` created with the invited roles.
- **Features:** resend, revoke, role-at-invite, expiry; who-can-invite (owner/admin); **seat limits**
  (plan-derived — reject invite over seat cap); optional email-domain auto-join.
- **Open:** invite to a specific **branch** too (branch-scoped membership)? Ties to branch model.

## 4. App-specific roles per org — app-specific, not core

- **Agree.** Core provides only structural roles (`owner`, `admin`) for org/billing management.
  Domain roles (e.g. `accountant`, `reviewer`) are **app-defined**.
- **Already wired:** `OrgMembership.roles: Set<String>` (O2) carries them; `buildOrgPermissions`
  flattens them into the session. `AuthRealm.getKnownRoles()` is the registration point.
- **Extension point:** app registers its **role catalog** (role → permissions) via `getKnownRoles`
  / a kontainer-collected registry, so the operator UI + `ApiAccessMatrix` can display/enforce them.
- **Open:** should role→permission resolution be app-provided (recommended) so core stays domain-blind?

## 5. Payments / Stripe — what to store locally

**Principle: Stripe is the source of truth; we mirror references + status locally via webhooks.
Never store raw card/IBAN data (PCI/PSD2) — only Stripe tokens/ids + display metadata.**

- **On the org (a `BillingAccount`):** `stripeCustomerId`, billing email, tax/company details,
  default payment method, current subscription ref, account status (active/past_due/suspended).
- **Payment methods:** type (**card** / **SEPA direct debit**), Stripe PM id, display-only last4 /
  brand / expiry / IBAN-last4, SEPA mandate ref + status, isDefault. (Both CC + SEPA via Stripe
  PaymentIntents/SetupIntents; SEPA needs mandate acceptance + longer settlement/failure windows.)
- **Subscription:** plan ref, Stripe subscription id, status (trialing/active/past_due/canceled),
  current period start/end, cancelAtPeriodEnd, seats.
- **Payments/charges:** amount, currency, status (succeeded/pending/failed), Stripe payment-intent
  id, timestamp, linked invoice, failure reason.
- **Status tracking = Stripe webhooks** (core infra, must-have): signature-verified, **idempotent**,
  retry-safe. Handle `invoice.paid`, `invoice.payment_failed`, `invoice.upcoming`,
  `customer.subscription.updated/deleted`, `payment_intent.succeeded/failed`,
  `setup_intent.succeeded`, `charge.refunded`. Each updates the local mirror.
- **Emails (via `messaging`):** upcoming payment (`invoice.upcoming`), receipt/paid, **failed →
  dunning**, trial ending, card/mandate expiring, subscription canceled.
- **Open:** lean on **Stripe Billing** (subscriptions, proration, dunning, tax, hosted invoices) to
  minimize local logic — recommended — vs. build our own. See §6 for the invoice nuance.

## 6. Invoice creation

- **Two layers, both needed:**
  - **Billing/payment invoice** — mirror Stripe's invoice (status, amount, hosted PDF ref). Cheap.
  - **Legally-compliant invoice** — for EU/German B2B (VAT, sequential legal invoice number,
    **ZUGFeRD/XRechnung** e-invoice) we likely generate our **own** PDF via the planned document
    layer (`20260717-saas-foundation.md` C/PDF track). This *connects billing ↔ the document tooling*
    and is literally several thebizz product needs.
- **Data:** sequential invoice number (legally required, gapless per issuer), issue date, seller
  (operator) + buyer (org) details, line items, VAT/tax breakdown, totals, payment status, period.
- **Core vs app:** core = invoice data model + numbering + Stripe mirror + PDF pipeline hook; app =
  the invoice *template* (block-based) + any domain line items.
- **Open:** who is the legal invoice issuer — the operator (us) for all tenants, or per-app? Numbering
  scope follows that. Reverse-charge / VAT-id validation (VIES) for EU B2B.

## 7. Payment plans — monthly / yearly / one-time / usage

- **`Plan` model (core data, app-defined instances):** id, name, **interval** (month/year/one-time/
  metered), price(s) per currency, **featurePermissions** (→ `OrgPlan.featurePermissions`, already
  wired in O2!), trial period, seat model (flat vs per-seat), limits/quotas.
- **Stripe mapping:** each plan ↔ Stripe Product + Price(s). Mirror ids.
- **Already connected:** the plan the org buys → `OrgPlan.featurePermissions` → session permissions
  (O2). So "plan gates features" already works end-to-end once billing sets the org's plan.
- **Core vs app:** core = plan/subscription mechanics + Stripe; app = the plan *catalog* + feature sets.
- **Open:** upgrade/downgrade proration, plan changes mid-period, grandfathering old plans.

---

## What else comes to mind (additional must-haves)

**Billing depth:**
- **Metered / usage-based billing** (per-solve, per-document, per-API-call) — matches thebizz
  per-transaction ideas; report usage to Stripe.
- **Tax handling** — VAT, EU B2B reverse-charge, VAT-id (VIES) validation, Stripe Tax vs. custom.
  Load-bearing for German/EU targets.
- **Trials & free tier** (trial-without-card, trial length, expiry emails).
- **Coupons / discounts / promo codes.**
- **Dunning + grace period + suspend-on-nonpayment** (then org status → suspended → login gate).
- **Refunds / credits / credit notes** (credit notes are legally required in the EU).
- **Proration** on plan changes; **cancellation flow** (at-period-end vs immediate + retention).
- **Multi-currency** (EUR primary).
- **Billing portal** — reuse **Stripe Customer Portal** so tenants self-manage payment methods /
  invoices / cancellation without us building that UI (big time saver).
- **Billing contact ≠ owner** (separate billing email).

**Org lifecycle & compliance:**
- **Suspend / reactivate / archive / delete** org, with data **retention + export** (GDPR erasure).
- **Seats/quotas/limits enforcement** from the plan (core enforces the numeric caps; app defines them).
- **Audit trail** for org+billing events (plan change, invite, ownership transfer, payment) — ties to
  the earlier auth audit-trail backlog.
- **Per-org branding** (email sender identity, logo on invoices) — deferred saas-foundation item.

**Cross-cutting:**
- **Webhook infrastructure** (generic, signature-verified, idempotent, retried) — core, reused for
  Stripe and future providers.
- **Notification/email catalog** (the billing + lifecycle emails, templated via the document layer).

---

## Operator (us, the platform operator) admin surface

- **Dedicated operator realm** — separate from tenant realms, `OrgPolicy.None` (operators are NOT
  tenant-scoped; they see across all orgs). Distinct user store + strong auth (2FA-forced — see auth
  backlog).
- **Cross-tenant admin API** (core primitives, *bypasses* org-scoping → must be tightly gated +
  fully audited): list/search all orgs, view org + memberships + billing status, change/credit
  plan, issue refunds, suspend/reactivate/delete, resend invites/emails.
- **Impersonation** — operator logs in *as* a tenant user, time-boxed + fully audited (also on the
  auth backlog; belongs here).
- **Operator dashboards** — MRR, churn, active orgs, failed payments, signups, trials. Metric set is
  partly app-specific (extension point: apps register operator metrics/tools).
- **UI** — a separate operator admin app (or a section gated to the operator realm).
- **Core vs app:** core = operator realm + cross-tenant API primitives + impersonation + audit; app =
  product-specific metrics/tools registered into the operator surface.

---

## Open decisions to make before scheduling any of this

1. **Stripe Billing vs. own billing engine** — recommend Stripe Billing + Customer Portal + local
   mirror-via-webhooks (least code), with our own compliant invoice PDFs on top for EU e-invoicing.
2. **`funktor/billing` + `funktor/operator` as new modules** vs. folding into `funktor/saas`.
3. **Legal invoice issuer** = operator (single entity, all tenants) vs. per-app — sets numbering.
4. **How much billing is core vs. app** — recommend: data model + Stripe + webhooks + emails +
   invoice pipeline are core; plan catalog + invoice templates + operator metrics are app.
5. Sequencing vs. the auth backlog (`v1-email-auth-and-sessions.md` Phase F, 2FA, audit trail) — most
   of this depends on Phase F sessions + the document layer landing first.

## Cross-references

- Orgs/membership/ownership foundation, plan→permissions wiring: `20260717-auth-orgs-foundation.md`
  (O1/O2 done; invitations partially sketched).
- Document/PDF layer for invoices: `20260717-saas-foundation.md` (Tracks C/D).
- Auth completeness (2FA, audit trail, impersonation, GDPR) discussed in-session — overlaps §Operator
  and §Org-lifecycle here; capture into a dedicated auth-features plan when scheduled.
