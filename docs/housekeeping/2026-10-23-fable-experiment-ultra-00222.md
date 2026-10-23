# Fable experiment, ultra (MC-00026), 2026-10-23

One blind round on Opus 5.5 at max effort (`reviewer-xhigh` with effort `max`) of ULTRA-00222 at `8d3e01dd`
(master `d9016c81`): the same three round-3 phase-1 briefs, diff and spec Fable had, no ledger, read access
limited to a detached worktree at `8d3e01dd` and a clean scratch copy. Scored against Fable's phase-1
hand-backs, not its reconcile. n = 1.

| Fable's phase-1 finding | Fable | Opus 5.5 max |
|---|---|---|
| Password sign-in in flight across SSO / reset / retire gets a session (`EmailAndPasswordAuth.kt:305-325`) | MAJOR (security) | **found**, MAJOR, by security AND implementation; same fix (re-read the password `_id`) |
| An SSO claim that stops part-way has no exit but the same verified SSO (`AuthRealm.kt:451`) | MAJOR (domain), MINOR (implementation) | **found**, MAJOR (domain); implementation found the resume branch untested (MINOR) |
| `resolveSsoAccount` stale user across a retire (`AuthRealm.kt:551-552`) | aside in the security MAJOR | **found**, MINOR (security) |
| resend × retire: stray mail, false "activated" page | MINOR (security) | found (implementation MINOR 3); security listed it as already in the red-team ticket |
| `AuthError` is a `Throwable`; the sweep's nets catch `Exception` | MINOR | missed |
| Sign-up writes the password before the marker | MINOR | missed |
| Epoch back-date overloads `withTimestamps`' "unset" | MINOR | missed (noted as CLEAN) |
| KDocs say expiry is enforced at sign-in | MINOR | missed |
| `OnAfterSave` listeners never see the conditional writes | MINOR (docs note) | missed |

Opus found beyond Fable's phase 1: reset completion on an already-retired row writes a Password row and answers
success (Fable found it only in round 4); the SSO resume branch has no test and a mutant there survives (claimed,
not run). False alarms: none found on reading.

Tokens (subagent totals): Opus 5.5 max 945,573 (implementation 373,859, domain 305,555, security 266,159);
Fable phase 1 812,692 (350,809, 260,471, 201,412).

**Verdict for ultra:** on this change Opus 5.5 max matched Fable on both MAJORs, blind, at about 16% more tokens;
Fable's edge was breadth in MINORs (5 missed). It does not show the Fable exception earning its place on MAJORs.
