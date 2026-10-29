# ULTRA-00239: Kraft modals: Esc closes a modal as a dismissal, through a reusable key-binding mechanism

> **Status (2026-10-29): PAUSED.** Follow-up of ULTRA-00233 (every modal answers exactly once). Waits for ULTRA-00233 and for Klang's generalized key-binding feature.
>
> **Part of:** ULTRA-00257 (Kraft forms and components epic)

**Security-critical:** no

## Why this exists

ULTRA-00233 left Esc out: no Kraft modal handles it today, and adding it changes behaviour for every `FadingModal`
subclass. The maintainer (2026-10-24): file it as a follow-up, and implement it **through a reusable mechanism**,
because Klang is building a generalized key-binding feature. So Esc on a modal is one binding of that mechanism,
not a `keydown` listener hard-wired into `FadingModal`.

Klang's own modal subclass handles Esc today; it is the reference for the expected behaviour.

## The mechanism (Klang-Research, 2026-10-29)

Klang's key manager is KLANG-00325 (IN PROGRESS, epic KLANG-00332), in `klang/workshop/src/jsMain/kotlin/keys/`
(package `io.peekandpoke.klang.workshop.keys`), depending on Kraft and Ultra only. Commits `836e6c2d6` (manager),
`2fd36e596` (layers, YesNoModal on it), latest `18a8533ad`. Steps 1-2 done and reviewed (core, `OnKey` with hints,
layers, priorities, chord sequences, the Esc chain); step 3 (CodeMirror keys) in progress; the maintainer's Mac test
pending. Its step 5 is "the move into Kraft when it has proven itself; ULTRA-00239 built on it there".

Shape: actions, not keys (`KeyAction(id, title, keys, macKeys, scope, priority)`, rebindable), defined in bags
(`manager.define(bindings)`, duplicate ids throw); letters match by typed character, digits and shifted symbols by
`event.code`; start-up checks for clashes and keys Chrome keeps; `KraftApp.Builder.keys(...)` sets `KeysManager.key`
and a `KeyHintStage`; `Tag.OnKey(action, ...)` registers on mount; layers form a stack, only the top answers,
`Component.openKeyLayer()` in `init`; within a layer the highest priority, then the newest registration answers.
The modal plan agreed with the maintainer 2026-10-25: `FadingModal` opens a key layer in its init; Esc dismisses,
OkCancel answers Ctrl+Enter through actions.

## Decide before implementing

1. **The go for the move** is the maintainer's (KLANG-00325 step 5), after the Mac test. Until then this ticket waits.
2. **Open points for the move** (Klang's list): layers follow the component tree, not the screen stack (a popup from
   a modal lands in the root layer, so it waits while the modal is open; do popups and toasts open layers?); naming
   and shape (`KeysManager` adds document listeners in its constructor, which piles up per app as ULTRA-00268 found
   for `WindowController`; `Component.keys` is a generic name on every component); held keys never repeat (opt-in
   planned); the hint stage's clipping cases.
3. **Where in Kraft:** `kraft/core` (`io.peekandpoke.kraft.keys`), since modals in `kraft/semanticui` build on it.

## The work

1. Port KLANG-00325 from Klang's workshop (Apache-2.0) with its tests into `kraft/core/src/jsMain/kotlin/keys/`,
   applying the decisions of point 2; specs into `kraft/core-tests/src/jsTest/kotlin/keys/`.
2. `kraft/semanticui/.../modals/FadingModal.kt`: `openKeyLayer()` in init; actions `kraft.modal.dismiss` (Esc) and
   `kraft.modal.confirm` (Mod-Enter, OkCancel); an opt-out per modal (`dismissOnEsc = false`).
3. Specs as in Acceptance, in `kraft/semanticui/src/jsTest/kotlin/modals/`.
4. Docs: a keys page, the overlays page, LLM mirrors; CHANGELOG.

## History

- 2026-10-24: created as the Esc follow-up of ULTRA-00233 (maintainer: through a reusable mechanism, Klang is
  working on generalized key bindings).
- 2026-10-24: NOT SCHEDULED.
- 2026-10-29: plan written from Klang-Research's report on KLANG-00325; waits for the maintainer's go to move it into Kraft.
- 2026-10-29: maintainer: "We will not do this now. It is not stable yet and will get new features still. So this one is parked." (Klang's key manager stays in Klang's workshop.)
- 2026-10-29: PAUSED.
