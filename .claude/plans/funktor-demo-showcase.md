# Funktor-Demo Showcase: FunktorConf

## Context

The funktor-demo currently only has technical showcase pages (retry, echo, cluster ops) but no real-world domain
demonstrating CRUD, forms, validation, or list/detail patterns. We need a showcase application that:

- Demonstrates funktor's full capabilities with a realistic domain
- Can be referenced 1:1 in funktor documentation (same domain in docs and demo)
- Includes both an admin-app (existing) and a user-app/website (new, later wave)

A Six Hats analysis was conducted. All five hats converged on **FunktorConf** — a conference/event management platform —
as the best domain choice.

## Why FunktorConf

- **Emotional texture**: Countdowns, live Q&A, real-time check-ins make demos feel alive
- **Natural Monko repo fit**: Events need auth, messaging, background jobs, storage, SSE — these are expected features,
  not forced
- **Bounded sub-domains**: Speakers, schedule, attendees, Q&A are each small CRUD surfaces
- **Universally understood**: No domain expertise needed to follow along
- **Scope control**: Unlike "blog" (where everyone knows what "finished" looks like), event platforms have no fixed
  mental model — we control the narrative

## Monko Repo Mapping Scorecard

| Monko Repo                     | FunktorConf Feature                                        | Natural Fit? |
|--------------------------------|------------------------------------------------------------|--------------|
| MonkoAuthRecordsRepo           | Speaker/organizer login, role-based access                 | Yes          |
| MonkoSentMessagesRepo          | Attendee notifications, speaker confirmations              | Yes          |
| MonkoBackgroundJobsQueueRepo   | Badge generation, bulk email blasts, report generation     | Yes          |
| MonkoBackgroundJobsArchiveRepo | Job history (completed badge runs, sent newsletters)       | Yes          |
| MonkoRandomCacheRepository     | Cached schedule views, speaker lists                       | Yes          |
| MonkoRandomDataRepository      | Event configuration, feature flags                         | Moderate     |
| MonkoGlobalLocksRepo           | Concurrent schedule editing, seat reservation locks        | Moderate     |
| MonkoServerBeaconRepo          | Server health (infrastructure — keep in existing showcase) | Low          |
| MonkoWorkerHistoryRepo         | Scheduled tasks: reminder emails, cleanup jobs             | Yes          |
| MonkoLogRepository             | Audit trail: check-ins, schedule changes                   | Yes          |

**Honest split**: Repos with "Low" fit stay in the existing technical showcase pages rather than being forced into the
event domain.

## Wave Plan

### Wave 1 — Domain Models + CRUD (admin-app) ← START HERE

**Goal**: Prove forms, validation, list/detail, routing with real entities
**Screen budget**: ~6 screens max

Entities:

- **Event** — name, description, dates, venue, status (draft/published/archived)
- **Speaker** — name, bio, photo URL, talk title, talk abstract
- **Attendee** — name, email, ticket type, check-in status

Each entity gets: list page, detail/edit page (with form + validation)

Files to create/modify:

- `common/src/commonMain/kotlin/funktorconf/` — shared models (Event, Speaker, Attendee)
- `server/src/main/kotlin/funktorconf/` — Monko repos, API routes, kontainer module
- `adminapp/src/jsMain/kotlin/pages/funktorconf/` — list + edit pages per entity
- `adminapp/src/jsMain/kotlin/nav.kt` — new FunktorConf nav section
- `common/src/commonMain/kotlin/funktorconf/FunktorConfApiClient.kt` — typed API client

### Wave 2 — Cross-Cutting Concerns

Wire into Wave 1 entities:

- Auth: role-based access (organizer vs. viewer)
- Messaging: notification on speaker confirmation, attendee registration
- Logging: audit trail for entity changes

### Wave 3 — Cluster Features

- Background jobs: badge PDF generation, bulk email for event updates
- Workers: scheduled reminder emails before event
- Storage: speaker slide uploads, event assets
- Locks: concurrent schedule editing protection
- Cache: cached public schedule view

### Wave 4 — Public User-App (website)

A read-only public-facing app consuming the same API:

- Event landing page with schedule
- Speaker profiles
- Live Q&A feed (SSE)
- Attendee self-registration

This demonstrates the common-module sharing story (same models, different UI).

## Relationship to Docs

Each wave should produce matching funktor doc page drafts. Before coding each entity/feature, identify which doc page
will reference it:

- Monko repos docs → Event/Speaker/Attendee repository code
- Forms & validation docs → Event/Speaker edit forms
- Routing docs → Admin navigation + public routes
- Auth docs → Role-based access in admin
- Messaging docs → Notification setup
- Cluster docs → Background job + worker examples

## What NOT to Do

- Don't remove existing technical showcase pages — they serve infrastructure proof
- Don't force every Monko repo into the event domain — be honest about fit
- Don't build rich text editing, complex image upload, or other scope-creep features in Wave 1
- Don't start coding before the repo-mapping scorecard is validated

## Verification

- After Wave 1: Can a docs page walk a reader through the showcase code and it makes sense?
- After Wave 2: Does every naturally-fitting Monko repo have a touchpoint in the domain?
- After Wave 4: Does the user-app consume the same API and share models via common module?
