# Six Hats Analysis: Making peekandpoke.io Visually Unique

**Date:** 2026-03-28
**Rounds:** 3

---

## White Hat — Facts & Context

- **Current state**: The docs site uses only basic CSS hover transitions. No animations, no WebGL, no scroll effects.
- **Proven reference**: karsten-gerber.de runs a Three.js metallic hexagon background with normal-mapped lighting, mouse
  tracking, idle Bézier wandering, and gyroscope on mobile — production-tested and performant.
- **Tech stack**: Astro + Tailwind CSS + Shiki. No animation libraries. Astro's island architecture allows heavy
  client-side components without penalizing page load.
- **Design language**: Dark theme (#0e0d0b), warm amber/gold accents (#d4a03a, #e8b84a), frosted glass nav, generous
  spacing.
- **Audience**: Kotlin developers evaluating libraries. They value clarity and speed. Every Kotlin ecosystem docs site (
  Ktor, Arrow, Exposed) looks like default markdown.

---

## Discussion Summary

### Round 1 — Opening Positions

#### Red Hat — Feelings & Intuition

- The hexagon background from karsten-gerber.de creates a "magnetic" pull — that same pull on docs would be incredible
- **"Spectacle at the entrance, silence in the library"** — the emotional line between landing page and docs is
  razor-sharp
- Code blocks that feel alive (shimmer on hover) would create a smile; scroll-jacking would create fury
- Frosted glass nav with a golden glow on scroll would feel premium

#### Black Hat — Risks & Problems

- 300ms extra time-to-interactive is catastrophic for docs pages opened 20 times in a tab bar
- Mobile is 30% of traffic — WebGL degrades unpredictably on mid-range Android
- "Unique" can signal "unserious" — developers benchmark against Kotlin/Ktor official docs
- Every custom animation is code a solo maintainer must debug when browsers ship breaking changes

#### Yellow Hat — Benefits & Value

- Most Kotlin docs are static markdown dumps — even subtle animations put the site in the top 5%
- The Three.js hexagon is a proven asset with near-zero R&D risk
- Astro's island architecture allows heavy visuals without penalizing page load or Lighthouse scores
- No Kotlin ecosystem player is doing this — first-mover advantage is cheap to capture

#### Green Hat — Creative Ideas

- 10 ideas from typewriter code reveals to a "Molten Kotlin" WebGL fluid hero
- Code block "X-ray" showing what DSL compiles to — visual flair serving understanding
- Terminal-mode easter egg (hidden, opt-in, zero cost to critical path)
- Interactive dependency constellation as navigation

#### Blue Hat — Process & Next Steps

- Sequence: CSS-only first → vanilla JS → Three.js, each layer validating the previous
- Critical decisions needed: performance budget (max LCP increase), mobile cutoff, A/B validation
- Pick ONE effect, ship it, measure bounce rate delta before layering more

### Round 2 — Key Debates

The sharpest exchange was between Red Hat and Black Hat on the "unserious" risk. Black argued that heavy visual effects
signal "side project with good CSS." Red pushed back hard: the sites that feel unserious are the ones with *gratuitous*
animation everywhere — a single stunning hero with quiet docs pages does the opposite, signaling craft. Yellow sided
with Red, noting that "doing nothing is also a risk" when every competing docs site looks identical.

Green Hat's original 10 ideas got ruthlessly pruned by the debate. Black Hat killed particles, parallax, and the molten
hero outright — they signal "portfolio site," not "production tooling." But Green adapted and produced two new ideas
that survived all criticism: a **golden thread navigation** (CSS scroll-timeline trace through sidebar sections) and a *
*code block warm glow** (two lines of CSS, zero performance cost). These emerged as the creative sweet spot — effects
developers notice only when they're gone.

The most productive convergence was Red Hat's insight that "magic comes from coherence, not individual tricks." This
shifted the entire group from debating which single effect to ship toward designing a **coherent ambient mood**: hexagon
texture, warm glow nav, quiet code shimmer, all working together. Blue Hat immediately restructured the execution plan
around this insight, organizing into three tiers gated by performance measurements.

### Round 3 — Final Verdicts

| Hat    | Final Verdict                                                                                                                                                                         |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Red    | Ship the CSS-only warmth layer first — hexagon texture, code glow, terminal easter egg — because craft whispered is more memorable than spectacle shouted.                            |
| Black  | Lock the performance budget first, ship one CSS-only effect, and redirect every saved hour into documentation content — a complete docs site *is* the most memorable visual identity. |
| Yellow | CSS warm glow everywhere first — it is the highest return on the lowest investment, and consistency compounds while spectacle fades.                                                  |
| Green  | The signature move is golden thread navigation combined with warm glow code blocks — together they create a visual identity no other docs site has.                                   |
| Blue   | CSS hexagonal mesh on landing page only, warm navigation accents site-wide, hard performance gates defined before implementation begins.                                              |

---

## Synthesis

The debate produced a clear consensus with one productive tension. All five hats agreed on three things: (1) the landing
page should carry the visual weight while docs pages stay calm, (2) CSS-only effects should ship first as the
foundation, and (3) a hard performance budget must be defined before any animation code is written. The tension was
between Red Hat's desire for "coherent atmosphere" and Black Hat's insistence on "one effect, measure, then decide." The
resolution came from Green Hat's refined list — many of the strongest ideas (warm glow, golden thread, code block
shimmer) are CSS-only and can ship as a coherent package without conflicting with Black Hat's performance constraints.

The most important insight came from Red Hat in Round 2: **differentiation through atmosphere, not animation.** The goal
isn't to add a flashy effect — it's to make every page feel like someone cared about every detail. That warmth
transfers: visitors who feel the site is crafted assume the libraries are crafted too. This reframing moved the
discussion from "which WebGL trick do we add" to "how do we make every page feel alive."

Black Hat's final point deserves weight: visual polish on incomplete docs signals style over substance. The visual work
should enhance documentation that already exists, not substitute for documentation that doesn't.

---

## The 10 Ideas (Normal → Highly Creative)

### 1. Code Block Warm Glow

Soft amber `box-shadow` on hover for all code snippets. Two lines of CSS. Makes every snippet feel hand-forged.
**Effort:** Trivial | **Impact:** Subtle but site-wide | **Risk:** Zero

### 2. Enhanced Card Hover States

Library cards lift slightly (`translateY(-2px)`), border brightens, subtle scale. Staggered entrance animations on page
load with `@keyframes` + `animation-delay`.
**Effort:** Small | **Impact:** Landing page polish | **Risk:** Zero

### 3. Frosted Glass Nav with Golden Glow

The existing `backdrop-blur` nav gets a faint amber `box-shadow` that intensifies on scroll. Pure CSS using `scroll()`
timeline or a tiny scroll listener.
**Effort:** Small | **Impact:** Premium feel on every page | **Risk:** Zero

### 4. Section Transition Wipes

Smooth `IntersectionObserver`-driven fade+translate on landing page sections. Each section group slides in as you
scroll. No scroll-jacking — just entrance choreography.
**Effort:** Small | **Impact:** Landing page feels alive | **Risk:** Minimal

### 5. Golden Thread Navigation

An animated SVG line in the docs sidebar that traces your scroll position through section markers — like a circuit trace
lighting up in amber. CSS `scroll-timeline` or lightweight JS.
**Effort:** Medium | **Impact:** Unique visual identity, functional | **Risk:** Low

### 6. Typewriter Code Hero

The landing page hero types out a real Kotlin DSL snippet character by character with a blinking cursor, then settles
into static code. CSS `@keyframes` with `steps()`.
**Effort:** Medium | **Impact:** Memorable first impression | **Risk:** Low

### 7. CSS Hexagonal Mesh Background

A faint, pure-CSS honeycomb pattern on the landing page (repeating SVG or gradient). Reinforces the "interconnected
modules" metaphor without loading Three.js.
**Effort:** Medium | **Impact:** Visual identity anchor | **Risk:** Low

### 8. Three.js Metallic Hexagon Background (Landing Page Only)

Port the proven normal-mapped hexagon mesh from karsten-gerber.de. Mouse-following amber point light. Static fallback on
mobile. Confined to the landing page hero via Astro island.
**Effort:** Large (but code exists) | **Impact:** Showstopper | **Risk:** Medium (performance, mobile)

### 9. Terminal Easter Egg

Hidden activation (Konami code or triple-backtick). The page re-renders as a retro green-on-black terminal. Content
stays identical, typography goes monospace, links become `> cd /kraft`. Memorable and shareable.
**Effort:** Large | **Impact:** Developer delight, social sharing | **Risk:** Low (hidden, opt-in)

### 10. Code Block X-Ray

Hover a Kotlin DSL code sample and a translucent overlay appears showing what it compiles to — generated HTML, SQL, or
serialized JSON. Educational spectacle that makes the library's power visible.
**Effort:** Very large | **Impact:** Marketing as documentation | **Risk:** Medium (content maintenance)

---

## Recommended Actions

1. **Ship ideas 1-4 as a coherent CSS-only "warmth" package.** Code glow, card hover, nav glow, section transitions.
   This alone differentiates the site from every Kotlin docs competitor.

2. **Prototype idea 5 (golden thread navigation)** as the signature visual identity element. If it works, it becomes the
   thing people remember.

3. **Port idea 8 (Three.js hexagon) to the landing page hero** using Astro island isolation. The code already exists on
   karsten-gerber.de — adapt colors to amber/gold palette with static fallback on mobile.

4. **Keep idea 9 (terminal easter egg) as a backlog delight** — build it when you want a fun break from docs work.

5. **Defer idea 10 (code X-ray) until a specific library page needs it** — it's most powerful as a one-off showcase for
   Karango's query DSL or Kraft's HTML DSL, not as a generic feature.
