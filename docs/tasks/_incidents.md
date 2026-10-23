# Incidents

Capture the incident when it happens: one line in this file. Fix the code and the tests at once; a rule
waits for the weekly meeting, which also prunes. Damage that can't be undone (lost work or data, an
exposed secret, a push or a publish) gets a stopgap at once, preferably a check or a deny-list request,
else one line marked PROVISIONAL until <meeting date>; the meeting keeps or drops it, and unconfirmed it
lapses.

After each meeting the decided lines move to a dated record. A review escape (a CRITICAL or MAJOR a round
found) is one class of incident; this file replaces the review-loop escape ledger (MC-00028).

| date | what happened | what let it through | what it cost | immediate fix | proposed rule |
|------|---------------|---------------------|--------------|---------------|---------------|
| 2026-10-17 | ULTRA-00205 (kronos advanceby collapse), review round 1 MAJOR: `advanceBy { }` on an already-read node (the `total.isInitialized()` arm, `ultra/datetime/src/commonMain/kotlin/Kronos.kt`) had no test; `ZERO`, double-count and `settled` mutants survived | the coordinator's own mutant list skipped that arm | one review round; nothing shipped | test added, mutants run | Standard 2 gains "a branch on cached or lazy state (`isInitialized()`, a memo hit) gets one mutant per arm" (from the escape ledger) |
| 2026-10-18 | ULTRA-00206 (kraft chartjs live updates), review round 1 MAJOR: the merge wrote into the caller's mount-time config, because Chart.js keeps the config object it is given (`initConfig`/`initData`) and writes into it | implementer and plan both assumed the library copies | one review round; nothing shipped | the component creates and merges from copies it owns; spec + mutant | the domain charter in `/feature-review` gains "when code hands objects to a third-party library, who owns them afterwards — does the library keep and mutate them?" (from the escape ledger) |
