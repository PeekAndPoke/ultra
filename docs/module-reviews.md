# Module reviews

Which module was reviewed when, how, and by which model. Ultra is the foundation of other projects, so every module
is reviewed again and again, with the sharpest model available (maintainer, 2026-11-14).

**How to read it**

- **Whole-module review:** the module was read as a whole. Until 2026-11-14 these were *scans* (agents reading for
  defects, no charters). A *deep review* reads the whole module under the three charters.
- **Change review:** the review gate of one feature or fix. It says nothing about the code the change did not touch.
- **Charters:** C = implementation and code style, D = domain expert, S = security.
- **Model:** the highest reviewer level of the gate. `reviewer-opus-high` (Opus, effort high), `reviewer-fable-medium`,
  `-high`, `-xhigh` (Fable). Older names: `reviewer-high`, `-xhigh`, `-top` (Opus). "not recorded" means the ticket
  does not say.
- **Red team:** attack scenarios run against the module. "collected" means a ticket of scenarios exists and was not run.

**How to keep it**

- When a gate closes or a scan, deep review or red-team session ends: update the module's row in "Status" and add
  one line to "Log", newest first within the module. Coordinator only, on master.
- A row is a claim about a ticket: write only what the ticket's ledger records.
- Next to plan: the oldest whole-module review, and "never" first, from the foundation upwards (ULTRA-00316).

## Status

Last updated 2026-11-14.

### ultra

| Module | Whole-module review | Last change review | Charters | Model | Red team | Open |
|---|---|---|---|---|---|---|
| common | 2026-07-29 scan, ULTRA-00143, model not recorded | 2026-07-20, ULTRA-00071, Placeholders injection fix | C D S | Opus | none | findings of ULTRA-00143 paused |
| reflection | 2026-07-29 scan, ULTRA-00148, model not recorded | none | | | none | findings of ULTRA-00148 paused |
| log | 2026-07-30 scan, ULTRA-00149, model not recorded | 2026-11-13, ULTRA-00158, level filtering and LogEvent, round 1 NOT CLEAN | C D S | `reviewer-opus-high` | planned (ULTRA-00316) | gate of ULTRA-00158 open |
| cache | 2026-07-30 scan, ULTRA-00159, model not recorded | 2026-11-14, ULTRA-00159, scan fixes and hot-key expiry, round 3 running | C D S | `reviewer-fable-xhigh` | planned (ULTRA-00316) | round 3; five real defects to fix |
| datetime | never | 2026-11-14, ULTRA-00211 and ULTRA-00210, zoned date-time keeps its instant, Mp enums; delta round 3 NOT CLEAN, fix running | C D S | `reviewer-fable-medium` | planned (ULTRA-00316) | batch 2 not merged |
| slumber | 2026-07-31 scan, ULTRA-00163, model not recorded, adjudication unfinished | 2026-11-06, ULTRA-00294, date awakers | C D S | `reviewer-high` | none | **deep review next** (ULTRA-00316) |
| kontainer | never | none | | | none | ULTRA-00319 designed |
| streams | 2026-11-11 operator audit, ULTRA-00307, model not recorded | 2026-11-11, ULTRA-00307, operator fixes, 2 rounds, clean | C D | `reviewer-xhigh` | none | |
| vault | 2026-07-28 scan, ULTRA-00140, model not recorded | 2026-07-20, ULTRA-00061, hook scope | C D S | not recorded | none | findings of ULTRA-00140 |
| depot, depot-s3 | never | 2026-11-13, ULTRA-00311, metadata, encryption and erasure, 4 rounds, clean | not recorded | `reviewer-xhigh` | collected | |
| security | never | 2026-11-06, ULTRA-00067, secret generator, clean | C D S | `reviewer-high` | none | |
| maths | never | 2026-10-26, ULTRA-00242, smoothing | C D | `reviewer-high` | none | |
| i18n | never | 2026-07-21, ULTRA-00076 and ULTRA-00070, runtime core and forms | C D S | Opus | none | |
| codegen | never | 2026-07-30, ULTRA-00155, generics emission | C D | not recorded | none | |
| remote | never | 2026-07-19, ULTRA-00049, Ktor client unification | C D S | Opus | none | |
| fixture, model, html, meta, semanticui | never | none recorded | | | none | |

### kraft

| Module | Whole-module review | Last change review | Charters | Model | Red team | Open |
|---|---|---|---|---|---|---|
| core | never | 2026-10-30, ULTRA-00266, router guard, 9 rounds | not recorded | `reviewer-top` | none | |
| semanticui | never | 2026-07-24, ULTRA-00099, org membership UI, 3 rounds | not recorded | not recorded | none | |
| testing | never | 2026-10-24, ULTRA-00240, TestBed | C | `reviewer-high` | none | |
| addons | never | 2026-11-10, ULTRA-00198, insights Kraft GUI and visnetwork, 3 rounds | not recorded | `reviewer-top` | none | |

### funktor

| Module | Whole-module review | Last change review | Charters | Model | Red team | Open |
|---|---|---|---|---|---|---|
| core | never | none recorded as its own scope | | | collected | |
| rest | never | 2026-11-06, ULTRA-00292, request body errors, clean | C D S | `reviewer-high` | collected | |
| auth | never | 2026-11-06, ULTRA-00289, 00291, 00293, 00296 | C D S | `reviewer-xhigh` | collected | |
| saas | never | 2026-07-24, ULTRA-00099 | C D S | Opus | collected | |
| cluster | never | 2026-11-13, ULTRA-00311 (depot part); 2026-10-19, ULTRA-00219, global lock | C D S | `reviewer-xhigh` | collected | |
| messaging | never | none recorded | | | collected (ULTRA-00125) | |
| insights | never | 2026-11-10, ULTRA-00301 and ULTRA-00171 | not recorded | not recorded | collected | |
| inspect | never | 2026-07-31, ULTRA-00169, Redacted and observer | C D S | Opus | none | |
| logging | never | 2026-11-13, ULTRA-00158 (database appenders, with `ultra/log`) | C D S | `reviewer-opus-high` | none | gate of ULTRA-00158 open |
| codegen | never | 2026-08-09, ULTRA-00195, client grouping | C D S | not recorded | none | |
| testing, all | never | none recorded as their own scope | | | none | |

### karango, monko, mutator, the rest

| Module | Whole-module review | Last change review | Charters | Model | Red team | Open |
|---|---|---|---|---|---|---|
| karango/core | never | 2026-07-28, ULTRA-00139, ensureIndexes | C D S | Opus | none | |
| karango/ksp | never | 2026-07-24, ULTRA-00107, value class filter | C D S | Opus | none | |
| monko/core, monko/ksp | never | 2026-07-24, ULTRA-00108, value class filter | C D S | Opus | none | |
| mutator/core | never | 2026-11-14, ULTRA-00234, ListMutator, 3 rounds, clean | C D S | `reviewer-fable-xhigh` | none | parked: ULTRA-00322 |
| mutator/ksp | never | 2026-11-13, ULTRA-00144, build and KSP, clean | C D | `reviewer-opus-high` | none | parked: ULTRA-00322 |
| docs-site | never | 2026-11-11, ULTRA-00303 | not recorded | not recorded | none | |
| build, tooling | never | 2026-07-21, ULTRA-00073, i18n Gradle plugin | C D S | Opus | none | |

**What the table shows (2026-11-14):** no module has had a whole-module review under the three charters. Seven had a
scan or audit (common, reflection, log, cache, slumber, vault, streams), none of them with a recorded model. Fable
has reviewed three changes so far (ULTRA-00176, ULTRA-00234, ULTRA-00211). No red-team session has been run.

## Log

One line per review event, by module, then date. The lines up to 2026-11-14 were collected from the tickets by an
agent on that day and were not re-checked one by one; a line is only as good as the ticket's ledger. In this
column set I = implementation and code style (C above), D = domain, S = security.

| Module | Date | Ticket | Kind | Scope | Charters | Model or reviewer level | Rounds | Verdict |
|---|---|---|---|---|---|---|---|---|
| ultra/cache | 2026-07-30 | ULTRA-00159 | Whole-module scan | all of `ultra/cache` (FastCache, ObjectSizeEstimator, ValueSortedMap, NullableCache, Cache), 4 read-only agents, findings coordinator-verified | n/a (scan) | not recorded | 1 pass | findings backlog; ticket PAUSED |
| ultra/cache | 2026-11-13 | ULTRA-00159 (via ULTRA-00316 progress) | Feature review gate | fixes of the scan findings (Track B, worktree `wt/cache-log`) | I + D + S (round 2: impl and domain CLEAN, security NOT CLEAN) | not recorded | 2 | NOT CLEAN: hot key outlives write TTL and hard TTL (probe-confirmed); stopped, waiting for maintainer |
| ultra/common | 2026-07-20 | ULTRA-00071 | Feature review gate (security-critical) | Placeholders second-order injection fix | I, D (correctness/API), S | Opus x3 | 1 | PASS |
| ultra/common | 2026-07-29 | ULTRA-00143 | Whole-module scan | all 49 main files documented, 7 agents | n/a (scan) | not recorded | 1 pass | findings backlog, 8 defects fixed + mutation-checked; ticket PAUSED |
| ultra/log | 2026-07-29/30 | ULTRA-00149 | Whole-module scan | all 9 files of `ultra/log`, 4 agents, 20 findings | n/a (scan) | not recorded | 1 pass | mechanical fixes done, 5 design questions open; PAUSED |
| ultra/log | 2026-07-30 | ULTRA-00150 | Red team (collected only) | log forging / log-pipeline abuse scenarios | n/a | n/a | not executed | NOT STARTED (collected) |
| ultra/log | 2026-11-13 | ULTRA-00158 | Feature review gate | log level filtering and LogEvent (shipped unreviewed in 0.109.0) | I + D + S | `opus-high` (level 1) | 1 | NOT CLEAN (1 MAJOR, same finding in all three charters); MAJOR fixed on master (`c218d1a6`), gate continues after open decisions |
| ultra/reflection | 2026-07-29 | ULTRA-00148 | Whole-module scan | all 8 main files documented, 6 agents, 33 findings | n/a (scan) | not recorded | 1 pass | first two fix rounds done; PAUSED |
| ultra/slumber | 2026-07-24 | ULTRA-00105 | Feature review gate | `@JvmInline value class` serialization support | I, D (serialization), S | not recorded | 2 | loop closed, round 2 clean |
| ultra/slumber | 2026-07-31 | ULTRA-00163 | Whole-module scan | all of `ultra/slumber`, 8 read-only agents (KDoc added, findings adjudicated) | n/a (scan) | not recorded | 1 pass | scan complete, adjudication in progress, no fixes yet; PAUSED |
| ultra/slumber | 2026-08-01 | ULTRA-00170 | Feature review gate | `@Slumber.As` wire-shape feature (5 commits; also touches karango/monko/funktor) | three reviewers (charters not named in the header; reviewer 3 security-style) | Opus x3 | 1 | gate PASS (one condition on e2e coverage left on record) |
| ultra/slumber | 2026-11-06 | ULTRA-00294 (shared gate with ULTRA-00295) | Feature review gate | date awakers throw instead of null | I + D + S | `reviewer-high` x3 (level 1) | 1 | PASS |
| ultra/vault | 2026-07-20 | ULTRA-00061 | Feature review gate | vault after-save/after-delete hooks, structured concurrency | I, D, S | not recorded | 1 | CRITICAL found and fixed; gate passed, follow-ups filed |
| ultra/vault | 2026-07-28 | ULTRA-00140 | Whole-module scan | all 31 main files of `ultra/vault`, 12 sub-agents, one file each | n/a (scan) | not recorded | 1 pass | findings backlog, A/B/Q items fixed + mutation-checked; PAUSED (driver items deferred) |
| ultra/i18n | 2026-07-20 | ULTRA-00070 | Feature review gate | S1 runtime core | I, D (i18n), S | Opus x3 | 1 | PASS |
| ultra/i18n | 2026-07-21 | ULTRA-00072 | Feature review gate | S2 emitter core (tooling) | I, D (i18n), S (codegen security) | Opus x3 | 1 + re-check | NOT PASS, fixed, PASS |
| ultra/i18n | 2026-07-21 | ULTRA-00073 | Feature review gate | S3 i18n Gradle plugin (buildSrc) | I, D (gradle/i18n), S | Opus x3 | 1 | APPROVE after fixes |
| ultra/i18n | 2026-07-21 | ULTRA-00074 | Feature review gate | S4 kraft reactive i18n glue (kraft) | I, D (kraft/i18n), S | Opus x3 | 1 | PASS after fixes |
| ultra/i18n | 2026-07-21 | ULTRA-00076 | Feature review gate | S5 kraft forms i18n | 3 reviewers (charters not itemised) | Opus x3 | 1 | findings fixed, PASS |
| ultra/remote | 2026-07-19 | ULTRA-00049 | Feature review gate | HTTP client unified on Ktor (emit-not-throw change) | I, D, S | Opus/high x3 | 1 | findings fixed (two caller regressions found by domain), DONE |
| ultra/security | 2026-07-31 | ULTRA-00176 | Feature review gate | JWT `kid` key rotation (`JwtSignatureGate`, funktor auth chain) | I (implementation & correctness), D (JWT RFCs), S (adversarial) | Fable x3 (level not named) | 1 | PASS with findings, fixed in `493423b8` |
| ultra/security | 2026-07-31 | ULTRA-00172 | Red team (collected only) | own JWT verifier | n/a | n/a | not executed | NOT STARTED (collected) |
| ultra/security | 2026-11-06 | ULTRA-00067 | Feature review gate | CSPRNG secret/token generator | I + D + S | `reviewer-high` x3 (level 1) | 1 | CLEAN, gate PASS |
| ultra/streams | 2026-08-24 | ULTRA-00199 | Feature review gate | `switchMap`/`switchMapNotNull` | I, D, S | not recorded | 1 (+ fix) | FAIL (2 HIGH), fixed; DONE |
| ultra/streams | 2026-08-24 | ULTRA-00201 | Feature review gate | CutoffStream reentrancy leaks | I, D, S | not recorded | 1 (+ fix) | FAIL (1 HIGH), fixed; DONE |
| ultra/streams | 2026-08-24 | ULTRA-00202 | Docs review | docs page for switchMap (technical accuracy, docs craft; no S) | 2 charters (accuracy, docs craft) | not recorded | 1 | PASS with findings |
| ultra/streams | 2026-10-26 | ULTRA-00241 | Feature review gate | `filterIsInstanceOf(KClass)` | I only (scoped by risk) | R1 `reviewer-high`, R2 `reviewer-xhigh` | 2 | R1 1 MAJOR; R2 clean; PASS |
| ultra/streams | 2026-10-26 | ULTRA-00242 | Feature review gate | `smooth()`, Smoothing laws in `ultra/maths`, FrameClock | I + D (no S) | `reviewer-high` x2 then x1 | 2 | rounds 1 and 2 clean; PASS |
| ultra/streams | 2026-10-29 | ULTRA-00264 | Feature review gate | change a smooth stream's law; batch with 00267, 00269 and kraft pixijs/threejs addons 00245, 00246, 00262, 00270 | A impl, A domain, B impl, B domain, S | R1 `reviewer-high`, R2 `reviewer-xhigh` | 2 | R1 NOT CLEAN; R2 clean; PASS |
| ultra/streams | 2026-11-11 | ULTRA-00307 | Module audit + feature review gate | "streams operator audit" of all stream operators (maintainer's request), then the fixes 00271, 00302, 00307, 00308, 00309 | I + D (S not run) | R1 `reviewer-high`, R2 `reviewer-xhigh` | 2 | R2 clean; PASS (audit scope: operators, not recorded as a full-module scan ticket) |
| ultra/datetime | 2026-10-18 | ULTRA-00205 | Feature review gate | Kronos `advanceBy` collapse | I, D, S | R1 not named, R3/R4 `reviewer-xhigh` (tiered) | 4 | R1 1 MAJOR; R2-R4 clean; PASS |
| ultra/datetime | 2026-10-18 | ULTRA-00207 | Docs review | docs for Kronos (docs-site) | not recorded (doc-only) | R1 `reviewer-high`, R2 `reviewer-xhigh` | 2 | R2 0 CRITICAL/0 MAJOR |
| ultra/datetime | 2026-10-18 | ULTRA-00209 | Feature review gate | own zone engine, kotlinx-datetime 0.8 (JS timezones) | I, D, S (R1) | R1 `reviewer-high` x3; later rounds not itemised | not recorded | gate PASS (one pre-existing MAJOR got ULTRA-00211) |
| ultra/datetime | 2026-10-18/19 | ULTRA-00213 | Feature review gate | JS performance (zone offsets, Long) | I, D, S | R1 `reviewer-high` x3, R2 `reviewer-xhigh` x3 | 2 | R1 1 MAJOR; R2 clean; PASS |
| ultra/datetime | 2026-10-19 | ULTRA-00216 | Feature review gate | own epoch <-> local date-time conversion | I, D, S | R1 `reviewer-high` x3, R2 `reviewer-xhigh`, R3 `reviewer-top` | 3 | R1 1 CRITICAL + 1 MAJOR; R3 clean; PASS |
| ultra/datetime | 2026-11-13 | ULTRA-00217 (reported in ULTRA-00316) | Feature review gate | native zone cache growth with path variants (Track A batch 1) | impl/domain and security (two reviewers) | not recorded (gate "round 2 CLEAN, delta round 3 CLEAN") | 3 (delta) | CLEAN; merged `87ad3489` |
| ultra/datetime | 2026-11-13 | ULTRA-00215 | Feature review gate | `MpLocalTimeSlot.splitWithGaps` loop across midnight | I + D + S | R1 `reviewer-opus-high` (level 1); later not named | 3 (R1 not clean, R2 clean, delta R3 clean) | R1 1 MAJOR (security); PASS; merged `87ad3489` |
| ultra/maths | 2026-10-26 | ULTRA-00242 | (see ultra/streams) | Smoothing laws live in `ultra/maths` | (see above) | (see above) | 2 | PASS |
| ultra/depot | 2026-11-11..13 | ULTRA-00311 | Feature review gate | per-file metadata and encrypted files with per-file keys (also ultra/depot-s3, funktor/cluster); step 2 and step 3 reviewed separately | I + D + S | `reviewer-high` R1, `reviewer-xhigh` R2-R4 (R3 at maintainer's "mid level") | step 2: 3 rounds; step 3: 4 rounds | step 2 R1-R3 NOT CLEAN (MAJORs); step 3 R1-R3 NOT CLEAN, R4 CLEAN; DONE 2026-11-13 |
| ultra/depot | 2026-11-12 | ULTRA-00312 | Red team (collected only) | depot encryption and shredding | n/a | n/a | not executed | DESIGNED (collected) |
| ultra/depot | 2026-11-11 | ULTRA-00304 | Feature review gate | insights record paths and depot path hardening; shared ledger for ULTRA-00303, 00305, 00306 (staticweb removal, kraft addon assets, docs) | I + D + S | R1 `reviewer-high`, R2 `reviewer-xhigh`, R3 `reviewer-top` (level 3), plus maintainer's short final check | 3 + final check | gate PASS |
| ultra/depot | 2026-07-31 | ULTRA-00164 | (finding, not review) | depot findings (symlink escape) found by the insights REST feature review | n/a | n/a | n/a | collected |
| ultra/(cross) Redacted, remote, inspect | 2026-07-31 | ULTRA-00169 | Feature review gate | `Redacted<T>` and Jackson removal | I, D, S, one reviewer each | Opus/high x3 | 1 | findings confirmed and fixed (`5c4bc615`); incl. MAJOR-class H1, F1 |
| ultra/(cross) Redacted | 2026-07-31 | ULTRA-00175 | Red team (collected only) | `Redacted<T>` | n/a | n/a | not executed | NOT STARTED (collected) |
| ultra/(whole repo) | 2026-10-23 | MC-00026 note (housekeeping, ULTRA-00222 round 3 phase 1) | Model experiment, not a module review | one blind round, Opus 5.5 max vs Fable on activation hardening | I, D, S | `reviewer-xhigh` with effort max (Opus 5.5) vs Fable | 1 | Opus matched Fable on both MAJORs, Fable found 5 more MINORs |
| kraft/core | 2026-10-18 | ULTRA-00206 | Feature review gate | chartjs live updates (kraft addon chartjs) | I, D, S | R1-R2 `reviewer-high`/ xhigh-level (R3 `reviewer-xhigh`, tiered, all charters) | 3 | R1 1 MAJOR; R2, R3 clean; PASS |
| kraft/core | 2026-09-28 | ULTRA-00204 | Feature review gate | router click interception | not recorded | not recorded | 2 | R2 clean; PASS |
| kraft/core | 2026-10-24 | ULTRA-00233 | Design review + feature review gate | modals answer exactly once (`Handle<RESULT>`) | design: C (impl) and D by two reviewers; feature R1: I + D (no S) | `reviewer-high` x2 | 1 design + 1 feature | feature R1 clean; PASS |
| kraft/testing | 2026-10-24 | ULTRA-00240 | Feature review gate | TestBed unmounts, failing awaits, shared eventually | I only (scoped by risk) | `reviewer-high` x1 | 1 | clean; PASS |
| kraft/core (router) | 2026-10-29/30 | ULTRA-00266 | Feature review gate (UI batch) | router leave-guard + 00243, 00247, 00263, 00268, 00272-00278 (kraft testing, windowcontroller, responsive, urlParam, FadingModal) | impl+domain per group A/B, S | R1 `reviewer-high`, R2 `reviewer-xhigh`, R3-R6 `reviewer-top`, R7 `reviewer-xhigh`, R8-R9 `reviewer-top` | 9 | R2 not clean (MAJOR, safety valve), R9 CLEAN; gate PASS 2026-10-30 |
| kraft/core (dnd) | 2026-10-30 | ULTRA-00265 | Feature review gate | dnd pointer events, keyboard, pick-and-place | I + D + S | R1 `reviewer-high`, R2 `reviewer-xhigh`, R3-R5 `reviewer-high` | 5 | R5 CLEAN (4th clean in a row); PASS |
| kraft/core (message bus) | 2026-11-11 | ULTRA-00310 | Feature review gate | message bus delivers no stale message | I + D (S not run) | `reviewer-high` (level 1) | 1 | clean; PASS |
| kraft/core (modals/forms) | 2026-07-21 | ULTRA-00077 | Feature review gate | async/suspend form validation rules | I, D, S | Opus x3 | 1 | PASS (H6 etc. fixed) |
| kraft/core (forms) | 2026-07-22 | ULTRA-00078 | Red team (collected only) | async/suspend validation | n/a | n/a | not executed | collected |
| kraft/addons (pixijs) | 2026-10-18 | ULTRA-00214 | Feature review gate | pixijs addon contract tests | scoped by risk (one reviewer R1) | R1 `reviewer-high`, R2 `reviewer-xhigh` | 2 | R2 0 CRITICAL/0 MAJOR; PASS |
| kraft/addons (pixijs, threejs) | 2026-10-29 | ULTRA-00264 | Feature review gate | tickets 00245, 00246, 00262, 00270 gated in ULTRA-00264's record | see ultra/streams row | `reviewer-high` -> `reviewer-xhigh` | 2 | PASS |
| kraft/addons (insights GUI) | 2026-11-10 | ULTRA-00198 | Feature review gate | Insights Kraft (Kotlin/JS) GUI | I + D + S | R1 `reviewer-high`, R2 `reviewer-xhigh`, R3 `reviewer-top` (opus max) | 3 | R1, R2 NOT CLEAN (1 MAJOR each); R3 clean; PASS 2026-11-11 |
| kraft/semanticui | 2026-07-23/24 | ULTRA-00099 | Feature review gate | org membership management (kraft UI + funktor/saas) | 3-agent (impl/domain/security) | not recorded | 3 (R1 no CRITICAL/HIGH; R3 clean) | loop closed |
| funktor/auth | 2026-07-22 | ULTRA-00084 | Feature review gate | AuthUser seams refactor | I, D, S (security reviewer at higher effort) | `opus` x3 | R1 recorded (loop protocol) | round 1 findings fixed |
| funktor/auth | 2026-07-22 | ULTRA-00086 | Feature review gate | b2b2c realm + b2b2c-app + shared org hooks | I, D, S | Opus x3 | 1 | PASS |
| funktor/auth | 2026-07-22 | ULTRA-00063 | Feature review gate (catch-up) | b2b realm | I, D, S | Opus x3 | 1 | findings fixed |
| funktor/auth | 2026-07-22 | ULTRA-00062 | Feature review gate (catch-up) | OperatorApiFeature | I, D, S | Opus x3 | 1 | HIGH confirmed by D and S, fixed |
| funktor/auth | 2026-07-22 | ULTRA-00087 | Feature review gate (loop) | mandatory auth floor on ApiRoutes | I, D, S | Opus x3 per round | looped (count in ticket) | closed clean |
| funktor/auth | 2026-07-22 | ULTRA-00088 | Feature review gate (loop) | `authorize {}` rule builder | I, D, S | `opus` x3 per round | 4 | R4 all ZERO; PASS |
| funktor/auth | 2026-07-22 | ULTRA-00090 | Feature review gate | two-phase auth with consistent params | 3 reviewers | Opus x3 | round 1 stated | 2 HIGH, fixed |
| funktor/auth | 2026-07-23 | ULTRA-00089 | Feature review gate (loop) | Stored-param migration | I, D, S | not recorded (3-agent) | 3 | R3 clean |
| funktor/auth | 2026-07-24 | ULTRA-00104 | Feature review gate | add member to organisation | I, D, S | not recorded | 1 | PASS, loop closed |
| funktor/auth | 2026-07-25/26 | ULTRA-00113, 00115, 00119 | Feature review gate | value classes `UserId`, `OrgId`, `EmailAddress` (00119 with 00116 client JWT claims) | I, D, S | not recorded | 00119: 2 | PASS after fixes (00119 looped to zero findings) |
| funktor/auth | 2026-07-27 | ULTRA-00121, 00123, 00126 | Feature review gate | email e2e test seam; email sender chain; account activation | I, D, S | not recorded | 1 each | findings fixed (00126 one escalated, blocking) |
| funktor/auth | 2026-07-28 | ULTRA-00131 | Feature review gate | auth email templates in code | I, D, S | not recorded | 1 | APPROVE WITH FIXES / CHANGES REQUESTED (S) |
| funktor/auth | 2026-07-28 | ULTRA-00133 | Red team (collected only) | auth email templates | n/a | n/a | not executed | collected |
| funktor/auth | 2026-10-19..20 | ULTRA-00222 | Feature review gate (strict) | activation hardening (pre-registration takeover, zero-click activation, resend) | I + D + S | R1 `reviewer-high`, R2 `reviewer-xhigh`, R3-R4 `reviewer-top` (x3 each) | 4 | R1 1 MAJOR, R2 3 MAJOR, R3 2 MAJOR, R4 CLEAN; PASS 2026-10-20 |
| funktor/auth | 2026-10-19 | ULTRA-00129 | Feature review gate | activation resend + frontend | I + D + S (R2: `reviewer-xhigh` x3 two-phase) | `reviewer-xhigh` x3 | 1+ | gate FAIL (3 MAJOR); superseded by ULTRA-00231 |
| funktor/auth | 2026-11-04 | ULTRA-00231 | Feature review gate | email-first sign-up | I + D + S | `reviewer-high` x3 (level 1) | 1 | CLEAN; PASS |
| funktor/auth | 2026-11-04 | ULTRA-00118 | Feature review gate | SSO email-verification linking | I + D + S | R1 `reviewer-high`, R2 `reviewer-xhigh` | 2 | R1 NOT CLEAN (1 MAJOR); R2 clean; PASS |
| funktor/auth | 2026-11-06 | ULTRA-00066 (with 00288) | Feature review gate | password rehash on login (+ SSO minors) | I + D + S | R1 `reviewer-high` x3, R2 `reviewer-xhigh` x3 | 2 | R1 NOT CLEAN (1 MAJOR); R2 clean; PASS |
| funktor/auth | 2026-11-06 | ULTRA-00130 | Feature review gate | activation-resend cooldown atomicity | I + D + S | `reviewer-high` x3 | 1 | CLEAN; PASS |
| funktor/auth | 2026-11-06 | ULTRA-00289 | Feature review gate | activation page (weak password, org name) | I + D + S | R1 `reviewer-high`, R2 `reviewer-xhigh`, R3 `reviewer-high` (confirmation) | 3 | R1 NOT CLEAN (2 MAJOR); R2, R3 clean; PASS |
| funktor/auth | 2026-11-06 | ULTRA-00291 | Feature review gate | one-shot tokens stored hashed | I + D + S | `reviewer-high` | 1 | CLEAN; PASS |
| funktor/auth | 2026-11-06 | ULTRA-00293 | Feature review gate | SSO sign-up names meet AuthDisplayName | I + D + S | `reviewer-high` | 1 | CLEAN; PASS |
| funktor/auth | 2026-11-06 | ULTRA-00295 (shared gate with ULTRA-00294) | Feature review gate | org-selection token single use | I + D + S | `reviewer-high` x3 | 1 | PASS |
| funktor/auth | 2026-11-06 | ULTRA-00296 | Feature review gate | passwords max 256 characters | I + S (by risk) | `reviewer-high` | 1 | both CLEAN; PASS |
| funktor/auth | 2026-11-06 | ULTRA-00178 | Feature review gate | JWT rotation operability | I + S (by risk) | `reviewer-high` | 1 | both CLEAN; ticket PAUSED on decisions |
| funktor/auth | 2026-07-27 | ULTRA-00127 | Red team (collected only) | account activation | n/a | n/a | not executed | collected |
| funktor/auth | 2026-07-25/26 | ULTRA-00112, 00114, 00117 | Red team (collected only) | UserId, OrgId, EmailAddress | n/a | n/a | not executed | collected |
| funktor/auth | 2026-07-22 | ULTRA-00058 | Red team (collected only) | error disclosure, env gating, route auth | n/a | n/a | not executed | collected |
| funktor/saas | 2026-07-18 | ULTRA-00038 | Feature review gate | saas module + Organisation storage (Phase O1) | I, D, S | Opus/high x3 | 1 | PASS |
| funktor/saas | 2026-07-24 | ULTRA-00039, 00095 | Red team (collected only) | SaaS orgs; org-isolation guard | n/a | n/a | not executed | collected |
| funktor/saas | 2026-07-20 | ULTRA-00068 | Feature review gate | composable auth frontend (AuthLogin) | I, D, S | Opus/high x3 | 1 | findings fixed |
| funktor/rest | 2026-08-02 | ULTRA-00185 | Feature review gate | REST Content-Type enforcement | I, D, S | Opus x3 | 1 | findings fixed; DONE |
| funktor/rest | 2026-08-24 | ULTRA-00196 | Feature review gate | ACL-aware navigation for contributed pages | I, D, S | not recorded | 1 (FAIL -> fixed) | PASSED 2026-08-24 |
| funktor/rest | 2026-11-06 | ULTRA-00292 | Feature review gate | body that does not fit its type is a 400 | I + D + S | `reviewer-high` | 1 | CLEAN; PASS |
| funktor/cluster | 2026-10-19 | ULTRA-00219 | Feature review gate | global locks expire by heartbeat | I + D + S | R1 `reviewer-high`, R2 `reviewer-xhigh` x3, batch `reviewer-high` | 2 + batch | R1 2 MAJOR; R2 clean; PASS |
| funktor/cluster | 2026-07-27 | ULTRA-00125 | Red team (collected only) | messaging / sent-messages surface | n/a | n/a | not executed | collected |
| funktor/insights | 2026-07-31 | ULTRA-00160 | Feature review gate | insights split data/rendering, superuser REST API | I, D, S | not recorded | 2 | R1 gate FAIL (1 CRITICAL, 4 HIGH); R2 PASS with follow-ups |
| funktor/insights | 2026-08-24 | ULTRA-00197 | Feature review gate | insights graphs, static slices (parts A+B) | I + D + S | `reviewer-high` x3 (+ batch review `reviewer-high`) | 1 | clean; PASS (parts C+D open) |
| funktor/insights | 2026-10-19 | ULTRA-00190 | Feature review gate | insights Vue tabs (T1) | I + D + S | R1 `reviewer-high` x3, R2 `reviewer-xhigh` x3 | 2 | R1 NOT CLEAN (1 MAJOR); R2 clean; PASS |
| funktor/insights | 2026-10-19 | ULTRA-00226 | Feature review gate | Vue round-2 minors | not recorded | `reviewer-high` | 1 | clean; PASS |
| funktor/insights | 2026-11-09 | ULTRA-00225 | Feature review gate | insights production gate | I + S (by risk) | `reviewer-high` | 1 | both CLEAN; PASS |
| funktor/insights | 2026-11-09 | ULTRA-00228 | Feature review gate | insights follow-ups, log order | I + S (by risk) | `reviewer-high` | 1 | both CLEAN; PASS |
| funktor/insights | 2026-11-10 | ULTRA-00171 | Feature review gate | query bind values in insights | I + D + S | not recorded | 2 | PASS (round 2 clean) |
| funktor/insights | 2026-11-10 | ULTRA-00301 | Feature review gate | one switch for the whole app (InsightsConfig) | I + S | not recorded (round 1) | 1 | both CLEAN; PASS |
| funktor/insights | 2026-10-19 | ULTRA-00161 | Red team (collected only) | insights REST API and collector data | n/a | n/a | not executed | collected |
| funktor/codegen | 2026-07-29 | ULTRA-00146 | Feature review gate | TS SDK codegen (`ultra/codegen` + `funktor/codegen`) | I, D, S | not recorded | 1 | findings fixed |
| funktor/codegen | 2026-07-30 | ULTRA-00155 | Feature review gate | real TS generics (ultra/codegen) | I, D, adversarial | not recorded | 1 | findings confirmed (all reproduced) |
| funktor/codegen | 2026-07-30 | ULTRA-00156 | Feature review gate | REST contributor + generate CLI | I, D, S | not recorded | 1 | findings confirmed; DONE |
| funktor/codegen | 2026-08-01 | ULTRA-00182 | Feature review gate | route access control in the TS SDK | I, D, S | not recorded | 1 | I and D FAIL -> fixed, S PASS |
| funktor/codegen | 2026-08-02 | ULTRA-00153 | Feature review gate | Vue contributors registry, mountAll, CLI only | I, D, S | not recorded | 1 | FAIL -> fixed |
| funktor/codegen | 2026-08-02 | ULTRA-00166 | Feature review gate | SDK auth runtime (session) | I, D, S | not recorded | 1 | FAIL -> fixed (HIGH in all three) |
| funktor/codegen | 2026-08-02 | ULTRA-00183 | Feature review gate | public-route metadata in SDK | I, D, S | not recorded | 1 | PASS |
| funktor/codegen | 2026-08-02 | ULTRA-00191 | Feature review gate | CSS contribution + demo scaffold (reviewed 2026-08-09) | I, D, S | not recorded | 1 | FAIL -> fixed |
| funktor/codegen | 2026-08-09 | ULTRA-00195 | Feature review gate | group generated clients under `api/` | I, D, S | not recorded | 1 | PASS |
| funktor/codegen | 2026-08-02 | ULTRA-00187 | Red team (collected only) | TS SDK auth runtime | n/a | n/a | not executed | collected |
| karango/core | 2026-07-24 | ULTRA-00107 | Feature review gate | `@JvmInline value class` filter support (no production change) | I, D, S | opus/high (3-agent, shared record with 00108) | 1 | PASS |
| karango/core | 2026-07-28 | ULTRA-00139 | Feature review gate | `ensureIndexes`/`recreateIndexes` return index state (4-file diff) | I, D, S | opus x3 | 1 | findings fixed |
| monko/core | 2026-07-24 | ULTRA-00108 | Feature review gate | `@JvmInline value class` filter support (monko KSP fix) | I, D, S | opus/high x3 | 1 | PASS |
| mutator/core | 2026-11-13 | ULTRA-00144 | Feature review gate | mutator specs on JS/native; build + KSP | I (implementation and build), D (multiplatform, KSP, Kotest); S not run | `reviewer-opus-high` (level 1) | 1 | CLEAN; merged `ae0a23cc` |
| mutator/core | 2026-11-14 | ULTRA-00234 | Feature review gate | `ListMutator.subList`, row tokens, iterator contract | I, D, S | R1 not named; R2 domain `fable-medium` (others not named); R3 `reviewer-fable-xhigh` (level 3) | 3 | R1 and R2 NOT CLEAN (security MAJOR in R2); R3 CLEAN on all three; merged `d2a19b0c` |
| build/tooling | 2026-07-21 | ULTRA-00073 | Feature review gate | i18n Gradle plugin (buildSrc) | I, D, S | Opus x3 | 1 | APPROVE after fixes |
| build/tooling | 2026-10-18 | ULTRA-00208 | none | Kotlin 2.4.21 upgrade, release 0.111.0 | n/a | n/a | n/a | gate NOT RUN (a dependency upgrade) |
| docs-site | 2026-08-24 | ULTRA-00202 | Docs review | switchMap docs (see ultra/streams) | accuracy + docs craft | not recorded | 1 | PASS with findings |
| docs-site | 2026-10-18 | ULTRA-00207 | Docs review | Kronos docs (see ultra/datetime) | not recorded | `reviewer-high` -> `reviewer-xhigh` | 2 | clean |
| docs-site | 2026-11-11 | ULTRA-00303 | Docs review | insights pages for Kraft and Vue | with ULTRA-00304's ledger | R1-R3 as 00304 | 3 + final check | PASS |

Where a ticket header gave a model only as "reviewer-top (opus max)" or "Opus 5.5 max", that is the stated model. No ticket records a review by `reviewer-fable-medium/high` except ULTRA-00234 (fable-medium for the round-2 domain charter, `reviewer-fable-xhigh` for round 3). ULTRA-00236 (Fable on the top rung) was stopped by the maintainer on 2026-10-25.
