# Six Hats Analysis: Kraft Examples, Interactive Demos & Site Visual Identity

**Date:** 2026-03-24
**Rounds:** 3

---

## White Hat — Facts & Context

- 10 pages total, 8 Kraft doc pages with ~1950 lines of content and 84 static code blocks
- Kraft is a Kotlin SPA framework using Preact, with SemanticUI DSL, reactive state, routing, forms, 12 addons
- Pre-built compiled examples already exist in `kraft/examples/` (hello-world, fomanticui, addons) with working HTML+JS
  output in `docs/kraft/examples/`
- The site uses Astro (static HTML output), warm amber/gold dark theme, Tailwind CSS
- Kotlin Playground (play.kotlinlang.org) only supports JVM Kotlin — **not** Kotlin/JS. No off-the-shelf browser
  playground exists for Kraft
- Published on Maven Central as `io.peekandpoke.ultra`, repo on GitHub

---

## Discussion Summary

### Round 1 — Opening Positions

#### Red Hat — Feelings & Intuition

- 84 static code blocks for a **UI framework** feels fundamentally contradictory — "show me it running"
- The amber/gold theme is cozy but risks feeling sleepy rather than energetic
- The gap between reading code and being able to try it is where excitement dies
- Pre-built examples exist but are buried — show the running thing FIRST, then the code

#### Black Hat — Risks & Problems

- No viable technical path for in-browser Kotlin/JS editing — no playground exists
- Building a custom compiler pipeline would be months of work, disproportionate to project scale
- AI-generated imagery dates fast and heavy images hurt page load for developer audiences
- Embedding 84 iframes without lazy loading/error handling is a performance catastrophe
- Stale examples (out of sync with Kraft API) would be worse than no examples

#### Yellow Hat — Benefits & Value

- The compiled examples already exist — embedding them is integration, not research
- Being the first Kotlin/JS framework with embedded runnable demos would be genuinely novel
- 84 code blocks is already more substance than most Kotlin framework docs
- Every addon (ChartJS, PDF viewer, signature pad) is inherently demo-friendly
- Even 10-15 strategically placed live embeds would transform site perception

#### Green Hat — Creative Ideas

- **Recipe card format**: Problem / Kraft Solution / Live Result — storytelling arc per example
- **postMessage parameter scrubbing**: bypass the missing playground entirely by sending messages to embedded iframes
  that change parameters (colors, speeds, layouts) without any compilation
- **Dogfood the framework**: rebuild the docs site in Kraft itself — the site becomes the demo
- **`<LiveExample>` Astro component**: reusable wrapper with parameter sidebar + iframe
- Drop mascot characters — let running code be the visual identity
- Amber crystal visual motif achievable with CSS effects (glass morphism, glow, depth)

#### Blue Hat — Process & Next Steps

- Embed existing compiled examples via iframes first (lowest cost, highest signal)
- Recipe card layout pattern is a content restructure, not infrastructure work
- Visual refresh should be CSS-only — no custom artwork dependencies
- CI check: if a compiled example fails to load, the build breaks
- Validate with one prototype page before committing timelines

### Round 2 — Key Debates

The most heated exchange was between Red and Black on **dogfooding**. Green proposed rebuilding the docs site in Kraft
itself — Red loved this as the ultimate trust signal ("if you won't use your own framework for your own site, why should
I?"), while Black called it "architectural self-sabotage" since a Kraft bug would break the documentation. This tension
remained unresolved but productive: it highlighted that Kraft's reliability IS the underlying question the docs need to
answer, regardless of approach.

Yellow and Black clashed on the **iframe approach's complexity**. Yellow called it "80% of the value at 5% of the cost,"
while Black insisted "integration at scale IS research" and demanded a single-page prototype before any timeline. Blue
mediated by proposing a proof-of-concept: embed 3 iframes on one page, measure performance on mobile, then decide. Green
contributed a creative resolution to the "not interactive" critique: use `postMessage` to let the doc page send
parameter changes to the embedded iframe, creating scrubbing/tweaking without any compilation step.

On **visual identity**, all hats converged away from AI-generated artwork as a primary strategy. Black warned about
maintenance burden and dating; Green dropped the mascot idea; Blue constrained the visual refresh to CSS-only effects.
The emerging consensus: the running demos themselves ARE the visual identity. A framework that shows live, interactive
UI examples doesn't need stock art to feel premium.

### Round 3 — Final Verdicts

| Hat    | Final Verdict                                                                                                                                    |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Red    | Ship one live embedded example this week, because trust is built by showing, not describing.                                                     |
| Black  | Prototype one real page with 8-10 iframes under real conditions before committing to anything.                                                   |
| Yellow | Ship two live demo iframes on the two highest-traffic pages within one week — that converts docs from telling to showing.                        |
| Green  | Ship one reusable `<LiveExample>` component with parameter controls — ten strategic placements will outperform eighty-four static code blocks.   |
| Blue   | Ship three pages with live-result-first iframes this week; that resolves the credibility gap and makes every subsequent improvement incremental. |

---

## Synthesis

The strongest consensus across all three rounds: **the pre-built compiled examples are an underexploited asset that
should be embedded as live demos immediately**. Every hat agreed on this — they only differed on scope (1 page vs 3 vs
10) and process (prototype first vs ship fast). The iframe approach won because it uses what already exists, requires no
new tooling, and delivers the emotional "alive" feeling that Red identified as the core gap.

The most creative insight came from Green's `postMessage` idea: you don't need a Kotlin/JS playground to make examples
interactive. By building a `<LiveExample>` Astro component that wraps an iframe with parameter controls (sliders, color
pickers, toggles), visitors can tweak running demos without any compilation. This bypasses the hard technical blocker (
no Kotlin/JS playground) with an elegant alternative that's arguably better for the use case.

On visual identity, the group converged on an unexpected answer: **the running demos ARE the visual identity**. Rather
than investing in AI-generated artwork that dates fast, the site should invest in making examples visually impressive —
beautiful forms, animated charts, interactive components. CSS effects (golden glow on active demos, glass morphism on
cards, subtle amber lighting) can reinforce the warm theme without external art assets. AI image generation remains an
option for hero sections and library "cover art" but is not the primary differentiator.

The dogfooding debate (rebuild docs in Kraft) remains an unresolved tension with strong arguments on both sides. It's
best treated as a separate, future initiative — not mixed into the current improvement sprint.

## Recommended Actions

### Phase 1: Proof of Concept (this week)

1. **Build a `<LiveExample>` Astro component** — wraps an iframe (lazy-loaded) with the Kraft example URL, plus optional
   parameter sidebar
2. **Embed on 3 high-traffic pages** — Kraft landing (`/ultra/kraft/`), Getting Started, and one addon page (ChartJS or
   forms)
3. **Layout: live result ABOVE code** — recipe card pattern: show the running app first, then the Kotlin code that
   produces it
4. **Measure**: page load time, iframe load behavior on mobile

### Phase 2: Scale & Structure (weeks 2-3)

5. **Identify the 10-15 best examples** from `kraft/examples/` that map to doc pages — not all 84 code blocks need a
   live demo, just the hero moments
6. **Add postMessage interactivity** to 2-3 examples — parameter scrubbing (slider changes chart data, toggle switches
   theme, etc.)
7. **CI check**: build step that verifies embedded example URLs return 200

### Phase 3: Visual Polish (weeks 4-5)

8. **CSS-only visual effects** — golden glow on live demo frames, glass morphism on cards, subtle depth/shadow
9. **AI-generated hero images** — one per library section (Kraft, Mutator, etc.) using a consistent style. Tools:
   Midjourney or DALL-E for abstract/architectural golden-amber imagery. Keep to hero sections only, not inline.
10. **Consider a "Kraft showcase" page** — a single page that IS a Kraft app (embedded full-page), demonstrating the
    framework's full capabilities

### Backlog (future)

- Dogfooding: evaluate rebuilding the docs site (or a section) in Kraft itself
- `llms-full.txt` auto-generation at build time
- Custom Kotlin/JS playground (only if postMessage approach proves insufficient)

---

## Progress (April 4, 2026) — examples restructured

### hello-world thinned out

`kraft/examples/hello-world` was doing too much — `MainPage.kt` alone was 449 lines of forms, modals, popups,
context menus, DateTime fields, select fields, textareas, DataLoader, localStorage persistence, and a color
playground. All of that belongs in the fomanticui example.

**Now:** 4 files, ~100 lines total. Focused showcase of the core Kraft concepts a new user needs:

- App bootstrap
- `PureComponent` and `Component<Props>`
- `by value()` reactive state
- Props + `onClick` events
- `subscribingTo(stream)` auto-unsubscribe

Files: `index.kt`, `MainPage.kt`, `CounterComponent.kt`, `TickerComponent.kt`.

### addons example — fully updated to the new AddonRegistry

Every addon page in `kraft/examples/addons` now demonstrates the new `AddonRegistry` pattern with lazy loading
via dynamic imports. CodeBlock-documented examples per addon: marked, signaturepad, jwtdecode, browserdetect,
avatars, nxcompile, sourcemappedstacktrace, prismjs, chartjs, pdfjs, pixijs.

### Breakout game — new pixijs demo

`kraft/examples/addons/src/jsMain/kotlin/pixijs/BreakoutExample.kt` — a playable Breakout game with 5x10 bricks,
paddle + ball physics, keyboard controls. Lazy-loaded pixi.js (~300KB) only fetches when the page is visited.
Strongest "alive" demo in the examples — exactly the kind of embedded live thing the synthesis recommended.

### ES2015 target enabled in all three example apps

`hello-world`, `fomanticui`, and `addons` all compile with `target.set("es2015")` plus the required
`fullySpecified: false` webpack rule. See `.claude/plans/es2015-migration.md`.

---

### New Example Ideas (from the discussion)

These examples would showcase Kraft's unique strengths beyond what's currently documented:

| Example                     | Why it's compelling                                               | Page            |
|-----------------------------|-------------------------------------------------------------------|-----------------|
| **Live counter with state** | Simplest possible interactive demo — "see state work"             | Getting Started |
| **Real-time chart**         | ChartJS addon + ticker stream = animated data visualization       | Addons          |
| **Form with validation**    | Draft/commit pattern + error states — highly visual               | Forms           |
| **Theme switcher**          | postMessage scrubbing: toggle SemanticUI inverted mode live       | SemanticUI DSL  |
| **Drag & drop list**        | DnD addon — tactile, satisfying, hard to fake with static code    | Addons          |
| **Auth flow**               | Login → protected route → redirect — shows middleware in action   | Routing         |
| **Todo app**                | The classic, but in Kraft — component composition + state + forms | Components      |
| **Responsive layout**       | Grid that adapts — show mobile/desktop side by side               | SemanticUI DSL  |
| **Modal + toast workflow**  | Click button → modal → confirm → toast notification               | SemanticUI DSL  |
| **PDF viewer**              | Embed a PDF in the docs page itself — meta and impressive         | Addons          |
