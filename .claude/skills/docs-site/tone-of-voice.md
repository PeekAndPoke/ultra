# Docs tone of voice — the two-voice system

The docs site uses two distinct voices. The dialogue between them is what gives the site warmth:
Voice 1 is slightly too polished, and Voice 2 keeps it real.

## Voice 1 — the scientist

Clear, neutral, factual. Describes what things do without claiming they are better than the
alternatives. May slightly overstate or gloss over a trade-off — but only to set up Voice 2's
honest correction.

## Voice 2 — the artist

Modestly humorous, self-ironic, honest. Appears in `<Aside>` components with „quoted text". Comments
on Voice 1's claims, admits trade-offs, acknowledges limitations, and tells the real story behind a
decision. A bit cartoonish; appreciates details.

## Why

The creator does not want to brag. The site is a figurehead and should reflect someone who builds
solid tools and lets the work speak for itself. **We do not try to convince anyone. Facts speak for
themselves, or they don't.**

## Rules

- Never use "done right", "no compromises", "no surprises", or similar superlatives.
- Remove "no X required" patterns when they accumulate — one is fine, three is preachy.
- Voice 2 appears only where it adds something. Not on every page, not after every section.
- Voice 2 is at its best when it honestly admits a weakness, a trade-off, or a messy origin story.
- Library overview pages: use the `<Aside>` component (block mode with „quotes").
- Landing page cards: italic quote text at the bottom of the card.
- Technical reference pages (operators, API): mostly Voice 1. Voice 2 only for a genuine gotcha.

## Page structure — the motivation section

Every library overview page opens with a motivation section **before** explaining the library itself:

1. Explain why the underlying *concept* matters (immutability, type safety, reactive state, …).
2. Do NOT claim the library is the best solution — only that the problem is real.
3. Voice 2 adds the honest origin story: what specific need led to building this.

Pattern: *"Why X matters"* → Voice 2 aside → *"What [Library] does"* → code example.

## Writing style generally

Prefer compelling, runnable code examples over javadoc-style prose. The goal is documentation nobody
skips.
