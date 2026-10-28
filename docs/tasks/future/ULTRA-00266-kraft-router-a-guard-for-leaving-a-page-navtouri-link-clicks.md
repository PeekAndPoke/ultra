# ULTRA-00266: Kraft router: a guard for leaving a page (navToUri, link clicks, Back)

> **Status (2026-10-28): IDEA.** Request from Klang (Klang-Research, 2026-10-28), from its look editor (KLANG-00382). Not scheduled; Klang keeps unapplied drafts in memory and arms beforeunload meanwhile.
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-28: Klang's look editor (klang `src/jsMain/kotlin/pages/LooksPage.kt`, `eb719ce71`, KLANG-00382)
holds drafts not yet applied; its design says "leaving with a draft not applied asks first". Kraft has no way to hold
a navigation away from a page. Klang's workaround: drafts kept in memory and shown again on return, plus a
`beforeunload` listener for reload and tab close.

Checked against the code, 2026-10-28 (`kraft/core/src/jsMain/kotlin/routing/`):

- **Holds:** middlewares are those of the *target* route, run in `Router.navigateToWindowUri` after the address has
  changed (`Router.kt:475-506`), and can only `Proceed` or `Redirect` (`middleware.kt:29-40`). A redirect back adds a
  history entry and the address flashes.
- **Holds:** `BackNavigationTrap` (`BackNavigationTrap.kt`, `trapBackNavigation`) answers only `popstate`, so not
  `navToUri` and not intercepted link clicks.
- **Found on the way, the hard part:** most navigations reach the router *after* the address moved. `navToUri`
  (`Router.kt:329`) goes through the strategy: the hash strategy sets `location.hash` and learns of it from
  `hashchange` (`:84-90`, `:108-114`); the path strategy pushes first, then navigates (`:131-138`); `popstate` and
  `hashchange` from Back or a typed fragment are after the fact too. A guard has to run before the strategy writes
  for `navToUri`, `replaceUri` and intercepted clicks, and for popstate/hashchange put the old address back without a
  new entry.

## Decide before implementing

1. **The API.** Klang's idea: a component-scoped guard, living and dying with its component as `trapBackNavigation`
   does: `guardLeaving { target -> LeaveResult.Proceed | Stop | Ask(suspend () -> Boolean) }`, asked before any
   navigation away from the current route. `Ask` lets the page show its own modal and go on to the target on yes.
   Recommendation: that shape; several guards are asked in mount order, the first non-Proceed wins.
2. **One mechanism with `BackNavigationTrap`, or two.** Recommendation: the guard subsumes the trap's "ask on Back"
   use; the trap keeps its "Back closes a modal" use. Decide once ULTRA-00203 (trap fires on fragment navigation) and
   ULTRA-00263 (replaceUri under hash routing) are settled, since all three touch the same listeners.
3. **`beforeunload`.** Optional flag: armed while the guard would say Stop or Ask. Browsers show only their own text.
4. **What counts as leaving:** a change of path or query (as `popstateListener` does at `Router.kt:163`), not a
   fragment. Recommendation: same rule.

## The work

1. Guard registry on the `Router`; `navToUri` / `replaceUri` / link interception ask it before the strategy writes.
2. popstate and hashchange: ask, and on Stop or a "no" put the previous address back with `replaceState`.
3. `Component.guardLeaving { }`, removed on unmount.
4. TestBed specs for both strategies: Stop holds each entry point and leaves the address unchanged; Ask continues on
   yes, stays on no; unmount removes the guard.
5. Docs: routing page and LLM mirror.

## Acceptance

- Each entry point (`navToUri`, a link click, Back, a typed hash) is held by a Stop guard, with the address and
  `history.length` unchanged; RED without the guard.
- `Ask` with "yes" lands on the target with one history entry.

## History

- 2026-10-28: created from Klang's request; checked against the code the same day.
