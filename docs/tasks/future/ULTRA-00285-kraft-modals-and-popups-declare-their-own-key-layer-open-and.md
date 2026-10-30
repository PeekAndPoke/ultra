# ULTRA-00285: Kraft: modals and popups declare their own key layer (open and close with them)

> **Status (2026-10-30): IDEA.** A request from Klang-Main-Dev (weekly meeting 2026-10-30, round 2.3).
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

## Why this exists

Klang's key manager (klang `workshop/src/jsMain/kotlin/keys/`, bound for Kraft as its own request later) works in
layers by priority. A Kraft modal or popup opens no layer, so whatever stands in front declares its own Esc at a
higher priority by hand (`KlangKeyPriorities`: a code tool's modal at Tool via `ToolButtonBar`). Klang wants the
layer to open and close with the modal or popup, declared, not guessed: a guard that read the open modals failed,
because the editor's hover docs are a popup that stays open while typing and it swallowed keys.

Checked against the code (2026-10-30):
- `ModalsManager` and `PopupsManager` are each a `Stream<List<Handle>>` of what is open
  (`kraft/core/src/jsMain/kotlin/modals/ModalsManager.kt:20`, `popups/PopupsManager.kt:26`), and each `Handle` has
  `onClose` (`ModalsManager.kt:58`, `PopupsManager.kt:118`). So "when does it open, when does it close" is there.
- What is missing is the declaration: nothing on a modal or popup says "I take the keys" (the hover-doc popup must
  not), so a listener on the stream has to guess, which is what failed.
- The content of a modal is a component; its own mount and unmount already bound its lifetime. Whether a key
  handler registered there is enough, or the handle should carry the layer, is the first thing to decide.

## Decide before implementing

- Where the declaration lives: a flag or layer on `Handle` at `show`, the content component's lifecycle, or both.
- Kraft has no key manager yet: the hook should not need one (a neutral `onOpen`/`onClose` or a layer marker that
  Klang's manager reads), until the key manager request arrives.

## The work

## Acceptance

## History

- 2026-10-30: created from Klang-Main-Dev's request; checked against `ModalsManager.kt` and `PopupsManager.kt`.
