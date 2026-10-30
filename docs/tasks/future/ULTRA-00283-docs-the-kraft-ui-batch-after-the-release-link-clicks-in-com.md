# ULTRA-00283: Docs: the Kraft UI batch after the release (link clicks in components, custom strategies, dnd dependency)

> **Status (2026-10-30): IDEA.** The DOCS follow-up of the UI batch (archived 2026-10-30: ULTRA-00243, 00247, 00263, 00265, 00266, 00268, 00272-00278). Written against settled code: after the release.

## Why this exists

The batch's pages were updated with the code (routing, link-clicks, testing, utilities, addons, drag-and-drop,
streams combining, and their LLM mirrors). Left for after the release, when the API has settled:

1. **link-clicks page:** a recipe for a component that stops click propagation and keeps the router:
   `Router.takeLinkClick(event)`; `RouterStrategy.linkClicked` for a custom strategy; `Router.pageOf(uri)` as the one
   rule of "the same page" (guards, RouterComponent, FadingModal).
2. **drag-and-drop page:** the Gradle dependency of `kraft-dnd` for an app without Semantic UI (`site.ts` has
   `kraftDnd`, unused), and the grip pattern next to the equip-grid sample.
3. **testing page:** pointer helpers once ULTRA-00279 exists.

## Decide before implementing

## The work

## Acceptance

## History

- 2026-10-30: created.
