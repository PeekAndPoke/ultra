# ULTRA-00239: Kraft modals: Esc closes a modal as a dismissal, through a reusable key-binding mechanism

> **Status (2026-10-24): NOT SCHEDULED.** Follow-up of ULTRA-00233 (every modal answers exactly once). Waits for ULTRA-00233 and for Klang's generalized key-binding feature.
>
> **Part of:** ULTRA-00257 (Kraft forms and components epic)

**Security-critical:** no

## Why this exists

ULTRA-00233 left Esc out: no Kraft modal handles it today, and adding it changes behaviour for every `FadingModal`
subclass. The maintainer (2026-10-24): file it as a follow-up, and implement it **through a reusable mechanism**,
because Klang is building a generalized key-binding feature. So Esc on a modal is one binding of that mechanism,
not a `keydown` listener hard-wired into `FadingModal`.

Klang's own modal subclass handles Esc today; it is the reference for the expected behaviour.

## Decide before implementing

- **Where the mechanism lives:** take Klang's key-binding feature into Kraft (core or an addon) once it has settled
  there, or design Kraft's own with Klang as its first user. Ask Klang (through Mission Control) for its state first.
- **Scoping:** only the topmost modal reacts to Esc; a binding registered by a modal is removed when it unmounts.
- **Opt-out:** a modal that must not be dismissed by Esc (an unsaved form) can switch it off.

## The work

- [ ] Agree the mechanism with Klang's key-binding work.
- [ ] Esc on the topmost `FadingModal` calls `close()` (a dismissal: `onResult(null)`), through that mechanism.
- [ ] Docs: the overlays page and the LLM mirror.

## Acceptance

- [ ] A browser test: `keyDown("Escape")` dismisses the topmost of two stacked modals only, and delivers
  `onResult(null)` once (a synthetic key event reaches Kraft's listeners, ULTRA-00233's friction list).
- [ ] A modal with Esc switched off stays open.
- [ ] No listener is left behind after the modal unmounts.

## History

- 2026-10-24: created as the Esc follow-up of ULTRA-00233 (maintainer: through a reusable mechanism, Klang is
  working on generalized key bindings).
- 2026-10-24: NOT SCHEDULED.
