# Vault: startup hook-consistency checks / auto-attach hooks

**Status:** FUTURE — not scheduled.
**Area:** ultra:vault (repositories, hooks), app startup validation.

## Idea (user, 2026-07-23)

Today an entity can implement a hook-marker interface (e.g. `Timestamped` from
`ultra.vault.hooks`) while the repo silently **forgets to mount the matching hook** — timestamps
just never get written, with no error. Two escalation levels:

1. **Fail-fast startup check:** on app start (natural place: alongside
   `Database.ensureRepositories()`), for every registered repository inspect the entity type
   (`storedType` is already reified on `EntityRepository`): if the entity implements a known
   hook-marker interface but the repo does not have the corresponding hook mounted → **fail startup**
   with a precise message (repo name, entity type, missing hook).
2. **Better — auto-attach:** when such a marker is detected, **automatically attach the hook** to
   the repo instead of failing. The marker interface *is* the intent; requiring a second manual
   registration is redundant ceremony and the source of the bug class.

## Sketch

- A registry of `marker interface → hook factory` pairs (e.g. `Timestamped::class → TimestampedHook`),
  extensible so future hooks join the mechanism — notably the mutation-tracking hook
  ([[20260723-entity-mutation-tracking]]) and any future `@Pii`/anonymization hooks.
- Detection via the repo's stored type: `storedType.classifier` implements marker?
- Auto-attach fits the framework's "no magic, but sane defaults" stance IF it is discoverable:
  log at startup what was auto-attached, and keep an explicit opt-out (e.g. repo flag) for the
  rare case a repo genuinely wants the marker without the hook.
- Explicitly-mounted hooks stay untouched — auto-attach only fills gaps, never duplicates
  (idempotence: skip if a hook of that type is already present).
- Failure mode choice per hook type: some hooks may be auto-attachable (Timestamped), others may
  need config and can only fail-fast (e.g. mutation tracking needing a target collection).

## Open questions

- Where does vault learn all repos at startup — `ensureRepositories()` walk is the candidate.
- Hooks with constructor deps (kontainer-resolved) — auto-attach needs access to the kontainer or a
  hook factory registered there.
- Should the check also catch the inverse (hook mounted but entity does NOT implement the marker) —
  probably a warning, not a failure.

## Related

- [[20260723-entity-mutation-tracking]] — first big beneficiary of auto-attach.
- Known trap in this area: `kontainer-semidynamic-singleton-trap` (memory) — hook factories with
  ctor deps must mind kontainer semantics.
