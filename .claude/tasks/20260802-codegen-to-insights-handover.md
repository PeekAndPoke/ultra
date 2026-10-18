# Handover to the insights agent — you can ship your CSS now

**From:** codegen agent, 2026-08-02. **Commit:** `db785022`. **Lock:** taken by me while I build the
demo app scaffold; nothing I am touching is in `funktor/ui` or `funktor/insights`.

You wrote `funktor/ui/src/main/resources/ts/ui/theme.css` and
`funktor/insights/src/jvmMain/resources/ts/insights/insights.css` and there was no way to ship them.
There is now.

## The API

```kotlin
class InsightsTsContributor : TsSdkContributor {
    override val name: String = "funktor:insights"

    override fun emit(context: TsSdkEmitContext) {
        // 1. Emit the bytes. sharedResource, NOT resource — see below.
        context.out.sharedResource("ts/ui/theme.css", to = "ui/theme.css")
        context.out.resource("ts/insights/insights.css", to = "insights/insights.css")

        // 2. Register them, so something actually loads them.
        context.registry.style(path = "ui/theme.css", order = 0)
        context.registry.style(path = "insights/insights.css", order = 100)
    }
}
```

That generates `styles.ts`, which the app imports once:

```ts
import './funktorsdk/styles.ts'
```

## Four things that will bite otherwise

1. **`sharedResource` for anything another module might also want** — `theme.css` above.
   `out.resource` plans an EXCLUSIVE path, so the second contributor asking for `ui/theme.css` is a
   hard error. `sharedResource` dedupes identical content and still fails loudly on a real conflict.
   Same rule the runtime modules already follow.

2. **Registering is separate from emitting, and you need both.** Emit without register → the sheet
   ships and does nothing, silently. Register without emit → hard error at build time naming your
   contributor (that one I made loud on purpose).

3. **`style()` DEDUPLICATES, unlike `route()`.** Registering `ui/theme.css` at order 0 from both
   `funktor:ui` and `funktor:insights` is fine and expected — declare your dependency on the theme
   rather than assuming someone else registered it. Registering the same sheet at a DIFFERENT order
   is a hard error naming both contributors, because there is no non-arbitrary answer and the wrong
   one is silent.

4. **`order` is cascade position and it is semantic.** `theme.css` defines the custom properties
   `insights.css` consumes; loading them the other way round does not error, the overrides just stop
   applying. Contributors arrive from a DI container in no defined order, so it has to be declared.
   Convention: theme/base near 0, feature sheets in the hundreds. Ties break on path.

## One decision I deliberately left to you

**CSS `@layer`.** Import order is currently an implicit cascade. `@layer theme, components, features;`
declared once in `theme.css` would make it explicit, and a sheet's position would stop depending on
when it was imported. Cheap now, while the design system is five files old — expensive once apps have
written overrides against the current cascade. Your call; the `order` mechanism works either way.

## While I was in there

- **`mount.ts`, `styles.ts` and `css-modules.d.ts` are now emitted UNCONDITIONALLY**, even empty.
  They were conditional, which meant the app's hand-written `import { mountAll } from
  './funktorsdk/mount.ts'` compiled or not depending on whether anyone had registered a page.
- **`css-modules.d.ts` is why your CSS imports type-check at all.** A side-effect import of a `.css`
  is TS2882 without an ambient declaration. Do NOT add your own `declare module '*.css' { … }` —
  a bodied wildcard collides with `vite/client`'s (TS2300) and with this one. It is handled.

## What is still missing for your pages to be clickable

Registering a page is the same registry, and nobody does it yet:

```kotlin
context.out.resource("ts/insights/InsightsPage.vue", to = "pages/insights/InsightsPage.vue")
context.registry.route(
    path = "/insights",
    component = "pages/insights/InsightsPage.vue",
    requiresAuth = true,
    nav = TsSdkRegistry.Nav(label = "Insights", icon = "gauge", order = 10),
)
```

I am building the app side — router, auth, login against the `operators` realm, `mountAll`, and an
ACL-gated menu. **Once your contributor registers routes, the pages appear with no further app
changes.** That is the whole point of the registry, so we should not need to coordinate on it.

One thing worth knowing for your own testing: `InsightsApi` floors at `isSuperUser()`
(`funktor/insights/src/jvmMain/kotlin/api/InsightsApi.kt:21`), and the demo's seeded operator
`karsten.john.gerber@googlemail.com` / `S3cret123!` IS a superuser. An ordinary operator would log in
fine and see nothing.
