# ULTRA-00310: Kraft: the message bus delivers no stale message to a new handler

> **Status (2026-11-11): DONE.** Review gate PASS (round 1 clean). The maintainer on 2026-11-11: "yes do it with a ticket" (after the explanation of the replay). Found by the streams gate (ULTRA-00307, round 2, domain finding 7).
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

**Security-critical:** no

## Why this exists

`MessagesHandler` (`kraft/core/src/jsMain/kotlin/messages/MessagesHandler.kt`) tells a new subscriber the last message
ever sent on the component's bus, as the `StreamSource` it replaced did. `onMessage<M> { }` subscribes, so a handler
registered after messages have flowed is called at once with an old one:

- it acts on a past event as if it just happened (a late `FormController` would add one already-mounted field again,
  and call `stop()` on a message long done);
- it sees only that single last message, never the ones before it: a half-true picture;
- the bus keeps that message, and through it the sending component, alive until the next message.

Handlers registered in `init { }` (the usual case) are unaffected: the replay hands them `null`, which they ignore.
`FormController` is created lazily during render, and `FormController.observer` can be added any time, so the late
case is real.

## The change

The bus keeps no last message: a new subscriber is told `null` ("no message", which the `Stream` contract allows and
every `onMessage` ignores) and from then on every message sent after it registered. `stream()` reads `null`.

## Acceptance

- A spec: a handler registered after a message was sent is not called with it, and gets the next one; red on the old
  bus.
- Kraft core and core-tests green.

## Review record

Round 1, 2026-11-11, level 1 (`reviewer-high`), blind; charters 1 (implementation) and 2 (domain). The security
charter was left out by risk: no auth, tenancy or data boundary. **Clean: 0 CRITICAL, 0 MAJOR.**

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | `MessagesHandler.kt:49-53` | No test of a handler throwing during `send` (impl 1) | fix | Spec case: the second handler still gets the message |
| 1 | MINOR | `MessagesHandler.kt:23,32` | "Told `null` first" and "`stream()` reads `null`" untested (impl 2) | fix | The late-handler case records `[null]`, then `[null, "y"]`, and reads `stream()`; the throw guard records values |
| 1 | MINOR | `MessagesHandler.kt:45` | No test of a handler subscribing during a dispatch (impl 3) | fix | Spec case: it gets `[null, "y"]`, not the message in flight |
| 1 | NIT | `MessagesHandlerSpec` | The motivating case runs on the bus, not through a component (impl NIT) | reject | `onMessage` is a null-filtering wrapper over the bus; the bus is what changed |

Domain: no findings; nothing in the repo relied on the replay (every `formController()` is built before its fields
mount; `formObserver()` has no users); the `Stream` contract holds; the bus is the only event-like stream. Kraft
core-tests `MessagesHandlerSpec` 6/6.

## History

- 2026-11-11: created on the maintainer's request.
- 2026-11-11: implemented: `MessagesHandler` keeps no message; `stream()` is `null`, a subscriber is told `null`
  first. `MessagesHandlerSpec`: the late handler case red on the old bus ("x" replayed); the throw guard case now
  throws on the initial `null`. Kraft core-tests 589/589, core 216/216. Review gate next.
- 2026-11-11: review gate PASS (round 1 clean); its MINORs fixed; DONE.
- 2026-11-11: DONE.
