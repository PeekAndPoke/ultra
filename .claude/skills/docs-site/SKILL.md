---
name: docs-site
description: Use when someone asks to add a library to the docs site, create documentation pages, update the docs site, add a new docs section, or work on peekandpoke.io. Also use when someone mentions "docs-site", "documentation website", or "add to the website".
argument-hint: "[ library name or task description ]"
---

## What This Skill Does

Creates and updates documentation pages on the PeekAndPoke docs site (Astro-based, deployed to peekandpoke.io). Handles
adding new libraries, creating pages, updating navigation, and maintaining consistency across the site.

## Project Structure

```
docs-site/
├── src/
│   ├── pages/           # File-based routing → URLs
│   │   ├── index.astro  # Home page (/)
│   │   └── ultra/
│   │       ├── index.astro      # Library listing (/ultra/)
│   │       └── [library]/       # One directory per library
│   │           ├── index.astro  # Overview page
│   │           └── *.astro      # Sub-pages
│   ├── layouts/
│   │   ├── BaseLayout.astro     # Root HTML wrapper
│   │   └── DocsLayout.astro     # Docs page with sidebar
│   ├── components/
│   │   └── Nav.astro            # Top navigation bar
│   ├── data/
│   │   ├── site.ts              # CONSTANTS — versions, URLs, Maven coords
│   │   └── *-sidebar.ts         # Sidebar nav config per library
│   └── styles/
│       └── global.css           # Theme, typography, code blocks
├── astro.config.mjs
└── package.json                 # Use pnpm, NOT npm
```

## Constants File (CRITICAL)

**`src/data/site.ts`** is the single source of truth for all versions, URLs, and Maven coordinates. NEVER hardcode these
values in pages.

Read this file before creating any page. Use the exported constants:

- `ultraVersion`, `kraftVersion`, `kotlinVersion` — version numbers
- `ultraGroup`, `kraftGroup` — Maven group IDs
- `dep.kontainer`, `dep.slumber`, `dep.streams`, etc. — full dependency strings for code examples
- `githubRepo`, `mavenCentralUrl` — external URLs

When adding a new library, add its dependency string to `site.ts` first.

## Design System

**Color theme:** Warm amber/gold brand palette on dark surfaces.

- Brand accent: `text-brand-400` (gold), `bg-brand-400/10` (subtle gold bg)
- Text: `text-white` (headings), `text-gray-400` (body), `text-gray-500` (muted)
- Surfaces: `bg-white/[0.02]` (cards), `border-white/10` (borders)
- Code inline: `code` tags auto-styled as gold on dark via global.css

**Fonts:** Inter (sans), JetBrains Mono (code) — loaded via Google Fonts in BaseLayout.

**Code blocks:** Use Astro's `<Code>` component with `lang="kotlin"` and `theme="vitesse-dark"`.

## Steps: Adding a New Library

1. **Read `src/data/site.ts`** — check if the library's dependency is already there. If not, add it.

2. **Create sidebar config** — `src/data/[library]-sidebar.ts`:
   ```typescript
   export const [library]Sidebar = [
       {href: '/ultra/[library]/', label: 'Why [Library]', section: 'Overview'},
       {href: '/ultra/[library]/getting-started', label: 'Getting Started', section: 'Overview'},
       // ... more pages grouped by section
   ];
   ```

3. **Create pages** — `src/pages/ultra/[library]/`. Every page follows this pattern:
   ```astro
   ---
   import DocsLayout from '../../../layouts/DocsLayout.astro';
   import { [library]Sidebar } from '../../../data/[library]-sidebar';
   import { Code } from 'astro:components';
   import { dep, mavenCentralUrl } from '../../../data/site';
   ---

   <DocsLayout title="Page Title" sidebar={[library]Sidebar} description="Brief description">
     <!-- Content here -->
   </DocsLayout>
   ```

4. **Update home pages** — add the library to both:
    - `src/pages/index.astro` — libraries array with `ready: true`
    - `src/pages/ultra/index.astro` — libs array with `ready: true`

5. **Update navigation** — add to `src/components/Nav.astro` navLinks array.

6. **Build and verify** — run `pnpm run build` from the `docs-site/` directory. Check page count and zero errors.

## Page Content Guidelines

- **Keep pages short and focused.** One concept per page. Split if a page covers more than 3-4 topics.
- **Lead with a code example**, not prose. Show what the library does in the first 10 lines.
- **Subtitle pattern:** Use `<p class="text-xl text-gray-400 !mt-2 !mb-8">` after h1 for the page subtitle.
- **Feature grids:** Use `<div class="not-prose grid grid-cols-1 sm:grid-cols-2 gap-4 my-8">` with card divs.
- **Tables:** Use `<div class="not-prose my-8"><table class="w-full text-sm text-left">` wrapper.
- **Navigation links:** End each page with a link to the next page using the brand button style.
- **Code examples:** Use `<Code code={...} lang="kotlin" theme="vitesse-dark" />`. For string interpolation in code, use
  `${'$'}` to escape.
- **Dependencies in code examples:** Always use constants from `site.ts`, e.g., `${dep.kontainer}`.
- **External links:** Use `target="_blank" rel="noopener"` on external links.

## Notes

- Use **pnpm** for all package management, never npm.
- Use "PeekAndPoke" or "peekandpoke" for branding, never "peek&poke".
- The `_drafts/` directory contains unpublished pages — don't modify unless asked.
- Sidebar sections group items visually: use 'Overview', 'Core', 'Usage', 'Advanced' as section names.
- The build command is `pnpm run build` from the `docs-site/` directory. Dev server is `pnpm run dev`.
