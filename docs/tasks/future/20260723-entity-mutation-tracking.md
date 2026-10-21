# Automatic entity mutation tracking — vault hooks + diff worker

**Status:** FUTURE — not scheduled. Needs a proper design pass (GDPR question below is open).
**Area:** ultra:vault hooks, funktor worker infra, storage.

## Idea (user, 2026-07-23)

Automatic audit/history for entities:

1. **Capture via vault hooks:** whenever an entity is saved, store a **one-to-one copy** of the
   saved state into a mutation-tracking collection (cheap, synchronous, no diffing at write time).
2. **Diff worker:** a background worker later computes **diffs** between consecutive copies and
   compacts storage using **checkpoints / key frames** — a full snapshot every ~100 mutations (or
   similar), diffs in between (video-codec style: key frame + deltas).
3. **Attribution:** each mutation record carries the **request ids**
   ([[20260723-request-ids]] — our id + upstream ids like `cf-ray`) and the **acting user**
   (id/type/realm from the session), so every change answers "who, when, via which request".

## Sketch

- Vault already has a hooks mechanism (`ultra.vault.hooks`, e.g. `Timestamped`) — mutation capture
  becomes another hook (opt-in per repo or per entity type, e.g. a marker interface or repo config).
- Record shape: `entityId`, `collection`, seq/no., timestamp, full copy OR diff + kind
  (`KeyFrame`/`Delta`), `requestIds`, `actor` (user id + realm + maybe org), `checkpointRef`.
- Worker: funktor has worker infra (funktor:cluster/workers) — periodic compaction job computes
  diffs, writes key frames every N mutations, deletes the raw 1:1 copies it has compacted.
- Reads: reconstruct any historical state = nearest key frame + replay deltas. Powers audit UIs,
  "what changed" views (funktor:insights), and debugging.

## OPEN QUESTION (user): anonymization on account deletion / GDPR requests

The history contains full copies/diffs of personal data — deleting the live entity is not enough.
Options to weigh in the design pass:

1. **Cascade erasure:** on GDPR delete, hard-delete all mutation records for the entity (simple,
   loses audit trail entirely — may conflict with legal retention duties, e.g. billing).
2. **Crypto-shredding:** encrypt each entity's (or each user's) history with a per-subject key;
   GDPR delete = destroy the key → history becomes unreadable ciphertext while non-personal
   metadata (timestamps, seq, collection) stays for audit integrity. Elegant but adds key
   management (where do subject keys live? key deletion must be provable).
3. **Field-level redaction:** rewrite history records replacing PII fields with placeholders
   (needs to know which fields are PII → a `@Pii`-style annotation on entity fields; diffs make
   rewriting harder — a redacted key frame invalidates downstream deltas touching those fields).
4. **Retention windows:** auto-expire mutation history after N days/months regardless (reduces the
   problem but doesn't solve on-demand erasure).

Likely answer is a combination: retention window by default + crypto-shredding or
cascade-erasure for erasure requests; PII annotation needed either way for actor data inside
OTHER entities' histories (e.g. a user's name embedded in an org's mutation record — erasing the
user must also handle those). Also note: the `actor` attribution itself is personal data.

## Related

- [[20260723-request-ids]] — prerequisite for the request-id attribution.
- Existing precedent in-repo: `EmailStoring.withAnonymizedContent` (messaging) already anonymizes
  stored email content — same spirit, study its approach.
- Red-team/GDPR angle overlaps the auth backlog (user erasure lands as an `AuthUserAdapter` op).
