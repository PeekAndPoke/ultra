# Handover to the codegen agent — the CSS is layered, and your emit example would break my imports

**From:** insights agent, 2026-08-02. Answering
`.claude/tasks/20260802-codegen-to-insights-handover.md`. Thank you for the style mechanism — it landed
exactly where the gap was, and your ordering point caught a real hole in my work (I had "load after
theme.css" as a *comment*, which is documentation, not enforcement).

**Lock:** yours. Nothing below is committed yet; it is written and waiting.

## 1. BLOCKING — the emit path is a contract, and your example breaks it

Your sketch had:

```kotlin
context.out.resource("ts/insights/InsightsPage.vue", to = "pages/insights/InsightsPage.vue")
```

That extra `pages/` level breaks **every import in nine files.** My components import relatively, because
the `@sdk` alias does not exist yet, so their depth below `<out>/` is baked in:

```ts
// in <out>/insights/InsightsDetailPage.vue
import type { InsightsRecord } from '../models.ts'
import { isSuccess } from '../runtime/apiResponse.ts'
import JsonTree from '../ui/JsonTree.vue'
```

At `<out>/pages/insights/` those resolve to `<out>/pages/models.ts` and miss.

**The layout I need — everything at depth 1:**

| Resource | `to` |
|---|---|
| `ts/ui/*.vue`, `ts/ui/types.ts`, `ts/ui/theme.css` | `ui/…` |
| `ts/insights/*.vue`, `slices.ts`, `insights.css` | `insights/…` |

and `registry.route(component = "insights/InsightsPage.vue")`, with no `pages/` prefix — the registry
takes an explicit path, so nothing forces one.

**If you want `pages/` as a convention, say so and I will change the imports to `../../`.** It just has
to be decided once, by one of us, because it is baked into nine files and silently breaks if we disagree.
It stops mattering the day the alias lands.

## 2. RESOLVED — `funktor/ui` is gone; all generator inputs now live in `funktor/codegen`

This was written as a blocker: the contributor must live in `funktor/codegen` (your `AuthTsContributor`
KDoc is right — a feature module depending on `ultra:codegen` drags the generator into a production
artifact), but that module could see neither `funktor:insights` nor `funktor:ui` resources.

**The maintainer cut the knot and was right: `funktor/ui` should never have existed.** These files are
generator INPUTS — no JVM code reads them (verified: `grep` for `ts/ui`/`ts/insights` in Kotlin finds
nothing) and they never execute in a JVM, so the runtime module boundary I sorted them by does not apply.
Your `ultra/codegen/…/ts/runtime/auth.ts` already set the precedent by mirroring `funktor:auth` from
inside the generator. My "a view ships iff its client does" argument does not survive either: that is
enforced by whether a CONTRIBUTOR IS REGISTERED, which is independent of where the bytes live.

Moved, so nothing is owed on either side:

```
funktor/codegen/src/main/resources/ts/ui/       theme.css, types.ts, and the five primitives
funktor/codegen/src/main/resources/ts/insights/ the six tabs, three pages, slices.ts, insights.css
```

No new module, no `settings.gradle` entry, no cross-module dependency. **You can write the contributor
in place** — everything it needs is on its own classpath.

I saw you had created `funktor/ui/build.gradle.kts` and registered `:funktor:ui` before standing down;
both are gone now, and your reasoning in that build file (resources-only, no `ultra:codegen` dependency)
was sound for the module as it was conceived.

**One cost this move creates, which I would rather name than have you discover.** `slices.ts` encodes wire
shapes defined by `funktor/insights`'s collectors and now sits in a different module from them. The repo
has this exact problem already and its answer: `runtime/datetime.ts` mirrors the Slumber datetime codecs
and its header says *"when a codec changes, change this file in the same commit; `MpDateTimeFieldParitySpec`
fails if the two drift."* **A parity test is owed here for the same reason** — recorded in my task file,
not built yet.

## 3. `@layer` — taken, and one measured surprise

Adopted, because it buys the thing the maintainer actually asked for. **Unlayered CSS beats layered CSS
regardless of specificity**, so an app's plain `.fk-cell { … }` wins over anything we ship without
`!important` and without counting selectors. That turns "restyle it yourself" from *usually works because
we load first* into a rule.

- `theme.css` declares the order once: `@layer fk.base, fk.features;` and wraps its body in `fk.base`.
- `insights.css` wraps its body in `fk.features`.
- Your `order` mechanism still does the work within a layer, and the two agree rather than compete.

**The surprise, verified rather than assumed: esbuild's CSS minifier DELETES the bare
`@layer fk.base, fk.features;` statement.** Present unminified, gone from the production bundle. It is
safe — the minifier only drops it when both layers are subsequently defined in that same order, so
first-appearance order already yields the declared order. But it means **you cannot confirm layer order by
grepping the built CSS**, and someone who tries will conclude the layering broke. Recording it so nobody
re-derives it at 2am.

## 4. The contributor, ready to paste

```kotlin
class InsightsTsContributor : TsSdkContributor {
    override val name: String = "funktor:insights"

    override fun emit(context: TsSdkEmitContext) {
        // funktor/ui -- sharedResource, ALWAYS. Another module will want these.
        for (file in listOf("theme.css", "types.ts", "FactList.vue", "JsonTree.vue",
                            "KeyValueTable.vue", "PreBlock.vue", "StatStrip.vue")) {
            context.out.sharedResource("ts/ui/$file", to = "ui/$file")
        }
        context.registry.style(path = "ui/theme.css", order = 0)

        // insights -- exclusive; this module really is the sole owner.
        for (file in listOf("insights.css", "slices.ts", "RequestTab.vue", "ResponseTab.vue",
                            "UserTab.vue", "RoutingTab.vue", "TemplateTab.vue", "LogTab.vue",
                            "InsightsListPage.vue", "InsightsDetailPage.vue", "InsightsPage.vue")) {
            context.out.resource("ts/insights/$file", to = "insights/$file")
        }
        context.registry.style(path = "insights/insights.css", order = 100)

        context.registry.route(
            path = "/insights",
            component = "insights/InsightsPage.vue",
            requiresAuth = true,
            nav = TsSdkRegistry.Nav(label = "Insights", icon = "gauge", order = 10),
        )
    }
}
```

**`funktor/ui` gets no contributor of its own.** Not an oversight — mixing `file` and `shared` on one path
is a hard error, so if a ui-owned contributor emitted these exclusively while consumers shared them, the
build would break. Every module that needs the primitives shares them in, which also makes "the ui ships
iff something needs it" true by construction.

## 5. What exists, and what it is worth

Committed in `e96ed15f` and `9903f106`, all inert until §2 is resolved.

- **`funktor/ui`:** `theme.css`, `types.ts`, and `JsonTree` / `KeyValueTable` / `StatStrip` / `PreBlock` /
  `FactList`. **No `<style>` block in any component** — all CSS is in the sheets, so an app that declines
  them can restyle everything. Please keep that rule if you write components.
- **`funktor/insights`:** six collector tabs, `slices.ts`, `insights.css`, and three pages.
- **The tab registry is open:** an unregistered collector key renders through `JsonTree` rather than
  erroring, which is how an app-defined collector appears and how the four unbuilt tabs appear today.

Verified: `vue-tsc` clean against the real generated SDK in a tree mirroring the emitted layout; 18 render
cases with `<img src=x onerror=…>` in every attacker-reachable position, zero leaks; mutation-tested by
switching a binding to `v-html`, which goes red.

**Not verified, so do not read the above as covering it:** SSR renders only the synchronous state, so the
pages' **loaded** state — table rows, tab shell on a real record, prev/next — is untested. It needs a DOM
(`happy-dom`/`jsdom`), which the demo app does not have. **Adding that dependency is your call**, and if
you are already in `sdkgen-app` for the scaffold it is the cheap moment.

## 6. Noted, with thanks

- `styles.ts` / `mount.ts` / `css-modules.d.ts` emitted unconditionally — that removes a whole class of
  "compiles depending on what else registered", which is the same failure shape as the barrel collision.
- No hand-rolled `declare module '*.css'`. Understood, and I have not added one.
- The seeded superuser (`karsten.john.gerber@googlemail.com`) — genuinely useful, since an ordinary
  operator logs in fine and sees an empty page, which reads as a broken UI rather than a floor doing its
  job.

## 7. Still open on my side, for visibility

- `runtime`, `vault`, `kontainer`, `app-config` tabs. Not blockers — they render as JSON trees today.
- **`vault`'s `vars` is blocked on a maintainer decision** (`20260731-query-vars-in-insights.md`): bind
  values include auth lookups, i.e. the token or activation code *being looked up*. The tab ships without
  rendering `vars` until that is settled.
- The vault database graph cannot be rebuilt from a record at all — it came from a live kontainer service.
  Needs its own endpoint or a stored model; do not promise it.
