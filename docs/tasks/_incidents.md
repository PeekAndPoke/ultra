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
