# Kraft Ecosystem & Tailwind Strategy

**Date:** 2026-04-03
**Scope:** Two strategic initiatives — JS library ecosystem + Tailwind CSS integration
**Method:** Six Thinking Hats analysis (3 rounds)

---

## Six Hats Analysis

### White Hat — Facts & Context

- Kraft has 11 addons wrapping JS libraries via `@JsModule` + `external` declarations, with Kraft `Component`
  integration
- FomanticUI DSL accumulates CSS classes in `MutableList<String>` via property chaining (`ui.blue.button`), lives in
  `commonMain`
- Dukat is deprecated; **Karakum** (JetBrains) generates Kotlin externals from TypeScript `.d.ts` files
- Tailwind is utility-first (`flex`, `p-4`, `text-blue-500`) — fundamentally different from component-based CSS like
  FomanticUI
- Kraft's Konva addon has 100+ generated external classes — large-scale JS wrapping is proven
- React's most-used libraries: React Router, Redux/Zustand, Tanstack Query, Axios, Formik, date-fns, Framer Motion,
  Radix UI, Headless UI, i18next

---

### Round 1 — Opening Positions

#### Red Hat — Feelings & Intuition

- Wrapping React libraries feels like a trap — emotional payoff fades fast when you hit type mismatches between React's
  mental model and Kraft's component lifecycle
- Tailwind DSL has the strongest "wow, I want to use this" gut reaction — autocomplete for utility classes would feel
  magical
- FomanticUI users would feel abandoned — that emotional cost is real
- Stale autocomplete is worse than no autocomplete

#### Black Hat — Risks & Problems

- **Maintenance trap**: every upstream JS library major release forces wrapper updates with a solo maintainer
- **Semantic mismatch**: React libraries assume React lifecycle, hooks, virtual DOM reconciliation — fighting Kraft's
  rendering model
- Karakum generates quantity not quality — generated APIs need hand-curation on top, doubling the work
- Tailwind's 500+ utility classes with responsive/state variants create combinatorial explosion for a type-safe DSL
- **False demand**: which React libraries do Kraft users actually need?

#### Yellow Hat — Benefits & Value

- Konva addon validates wrapping at scale — this is a proven pattern, not speculation
- Karakum backed by JetBrains reduces maintenance burden and improves type accuracy
- Each addon creates compounding network effect — developers who adopt for one library discover others
- Tailwind DSL builds directly on proven FomanticUI property-chaining pattern
- Together, these transform Kraft from "interesting framework" to "credible React alternative"

#### Green Hat — Creative Ideas

- **React compatibility shim**: render React components inside Kraft's DOM tree directly — wrap React's rendering
  contract once and unlock the entire ecosystem
- **Addon protocol** (lifecycle hooks, state bridge, DOM slot) instead of bespoke wrappers per library
- **FomanticUI API emitting Tailwind classes**: reimplement the semantic DSL to output utility classes — zero migration
  cost for existing users
- **Build-time DSL generation**: parse `tailwind.config.js` at build time so the DSL always matches the project's
  design tokens
- **Headless component library** with pluggable styling backends (Tailwind, FomanticUI, raw CSS)

#### Blue Hat — Process & Priorities

- Initiative 1 (ecosystem) must come first — Tailwind integration itself needs JS tooling wrappers
- Milestone 2 (React component interop) is the single highest-risk validation gate
- Proposed sequence: date-fns → React component interop → Tailwind DSL MVP → Full integration
- Key decision: does Kraft embed React components or reimplement them in pure Kraft?

---

### Round 2 — Key Debates

The sharpest tension emerged between Yellow Hat's optimism about the React shim ("build one bridge, unlock thousands")
and Black Hat's structural critique ("a single-point-of-failure bridging incompatible rendering models"). Yellow
pointed to Konva as proof that large-scale wrapping works; Black countered that Konva is a canvas library with clean
imperative semantics — it doesn't fight Kraft's model the way React component libraries would. Red sided with Black
on gut feeling: "wrapping React's rendering contract once feels like famous last words."

Green Hat's proposal to reimplement FomanticUI's API with Tailwind output drew the strongest positive consensus. Red
called it "the thing that genuinely excites me — it respects users' investment." But Black challenged the semantic
assumptions: "`ui.blue.button` implies component semantics; `flex p-4 text-blue-500` implies utility composition. The
abstraction will leak." Green evolved the idea in response: instead of mapping FomanticUI's API directly, build a
**design-token DSL** (`ui.button { primary; size.large }`) that treats Tailwind as a compilation target, not a type
system. This keeps the type surface small while letting the output layer change.

The biggest shift came from Red Hat pushing back on Blue's sequencing: "don't let the riskiest gate (React interop)
hold hostage the initiative with the strongest emotional pull (Tailwind)." Blue absorbed this and revised to parallel
tracks with a hard timebox on the React shim. Green's "filter heuristic" — wrap declarative/stateless JS libs, reject
hook-dependent ones — gave Black a concrete scoping tool that reduced the perceived risk surface.

---

### Round 3 — Final Verdicts

| Hat    | Final Verdict                                                                                                                      |
|--------|------------------------------------------------------------------------------------------------------------------------------------|
| Red    | Lead with what sparks joy (Tailwind behind familiar components), defer what sparks dread (React shim). Trust the gut.              |
| Black  | Sequence, do not parallelize. PoC the shim first with a hard two-week kill switch. The biggest risk is walking both paths halfway. |
| Yellow | Define the addon protocol, prove it with one real React wrapper, and the entire ecosystem unlock follows.                          |
| Green  | Decouple the DSL from the renderer. Build the pluggable backend interface before writing a single Tailwind class or React bridge.  |
| Blue   | Run parallel tracks with a hard timebox. Tailwind migration is the strategic necessity; React interop is the tactical opportunity. |

---

## Synthesis

Three rounds of debate produced a clear consensus on architecture and a productive tension on sequencing. The
strongest idea to emerge was Green Hat's **pluggable rendering backend**: decouple Kraft's component DSL from its CSS
output layer. This single abstraction resolves the FomanticUI abandonment fear (existing API stays stable), enables
Tailwind (new output target), and creates the foundation for future styling backends. Every hat endorsed this
direction, though they disagreed on when to build it relative to the React interop experiment.

On the React ecosystem question, the discussion converged on a **scoping filter**: wrap declarative, stateless JS
libraries (chart libs, canvas renderers, animation engines, utility libraries like date-fns) where the JS API surface
maps cleanly to Kotlin externals. Reject hook-dependent React component libraries that fight Kraft's rendering model.
The React compatibility shim is worth a time-boxed experiment — but it is a tactical opportunity, not the strategic
foundation.

The unresolved tension is between Red/Black's caution ("sequence, don't parallelize — you have one maintainer") and
Blue/Yellow's ambition ("parallel tracks with a hard kill switch"). The pragmatic resolution: **Tailwind backend
starts immediately** (it builds on proven infrastructure and has the strongest user-facing impact), while the React
shim gets a **time-boxed spike** (2-4 weeks, one library, hard go/no-go).

---

## Recommended Actions

### Phase 1: Pluggable Styling Backend (4-6 weeks)

**Goal:** Decouple Kraft's component DSL from CSS output. Ship FomanticUI + Tailwind as dual backends.

1. **Define the styling backend interface** — an abstraction that the existing `SemanticTag` DSL delegates to. The
   interface maps semantic tokens (primary, secondary, size.large, etc.) to CSS class strings.

2. **Implement the FomanticUI backend** — extract the current class-accumulation logic into the new interface.
   Existing behavior is preserved exactly. Zero breaking changes.

3. **Implement the Tailwind backend** — map the same semantic tokens to Tailwind utility classes. Start with the
   most-used components: Button, Container, Grid, Form, Input, Message, Segment, Card, Menu.

4. **Application-level backend selection** — the user chooses FomanticUI or Tailwind at app initialization.
   Components render with the selected backend's classes.

5. **Funktor integration** — expose backend selection in Funktor's server-side UI configuration so the admin UI
   and any server-rendered pages can switch between FomanticUI and Tailwind.

### Phase 2: JS Library Ecosystem (parallel, 2-4 week spike)

**Goal:** Validate React component interop. Expand the addon catalog with safe picks.

1. **Karakum pipeline setup** — establish a repeatable workflow: TypeScript `.d.ts` → Karakum → Kotlin externals →
   hand-curated Kraft addon. Document the process.

2. **Safe addon candidates** (declarative, stateless, no React hooks):
    - **date-fns** — date formatting/manipulation (pure functions, no DOM)
    - **Axios / ky** — HTTP client (no component model)
    - **Framer Motion** — animation (declarative API)
    - **Recharts** — charts (declarative, React-based but simple component model)
    - **i18next** — internationalization (pure functions + context)

3. **React shim PoC** (time-boxed, 2 weeks) — attempt to render one React component (e.g., Recharts `<LineChart>`)
   inside Kraft's DOM tree via Preact's compatibility layer. Hard go/no-go: if it requires hook workarounds or
   breaks Kraft's lifecycle, kill it.

4. **Addon protocol specification** — if the shim works, formalize the contract: lifecycle hooks, state bridge,
   DOM slot mounting. If not, document the findings and proceed with Karakum-based external wrappers for safe
   candidates only.

### Phase 3: Advanced Tailwind (after Phase 1 validated)

1. **Build-time DSL generation** — parse `tailwind.config.js` to generate type-safe Kotlin properties matching the
   project's actual design tokens (custom colors, spacing, breakpoints).

2. **Form field components** — Tailwind-styled form fields (text input, select, checkbox, radio, textarea) with
   Kraft's existing form validation and draft/commit pattern.

3. **Headless component library** — extract component logic (modal, dropdown, tabs, accordion) into a
   framework-agnostic layer. FomanticUI and Tailwind backends provide styling only.

### Key Decisions to Make

| Decision                           | Options                                       | Recommendation                                      |
|------------------------------------|-----------------------------------------------|-----------------------------------------------------|
| Tailwind DSL location              | commonMain (like FomanticUI) or jsMain only   | commonMain — enables SSR later                      |
| Backend selection granularity      | Per-app or per-component                      | Per-app (simpler), per-component later              |
| React shim approach                | Preact compat layer or direct React embedding | Preact compat (already a dependency)                |
| Karakum vs. hand-written externals | Generated-first or manual-first               | Generated + curated (Karakum baseline, hand-polish) |
| Tailwind version to target         | v3 (stable) or v4 (new, CSS-first)            | v4 — forward-looking                                |

### Key Risks

| Risk                                    | Mitigation                                            |
|-----------------------------------------|-------------------------------------------------------|
| React shim is a single-point-of-failure | Time-boxed PoC with hard kill switch                  |
| Tailwind combinatorial explosion        | Design-token abstraction, not raw utility mapping     |
| Solo maintainer bandwidth               | Phase 1 first (highest impact), Phase 2 opportunistic |
| FomanticUI users feel abandoned         | Pluggable backend preserves their API exactly         |
| Generated externals are awkward         | Karakum baseline + hand-curation pass                 |
