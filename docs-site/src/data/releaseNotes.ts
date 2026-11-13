// The release notes are written once, in `docs/release-notes/<version>.md` of the repository, and rendered here.
// One file per release; `README.md` there is the index for GitHub and says how a note is written.
import type {AstroComponentFactory} from 'astro/runtime/server/index.js';

interface NoteModule {
    frontmatter: { version: string; date: string; summary: string };
    Content: AstroComponentFactory;
}

const modules = import.meta.glob<NoteModule>('../../../docs/release-notes/[0-9]*.md', {eager: true});

/** Newest first: by the numbers of the version, not as text (0.118.0 is after 0.99.0). */
function compareVersions(a: string, b: string): number {
    const pa = a.split('.').map(Number);
    const pb = b.split('.').map(Number);

    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
        const diff = (pb[i] ?? 0) - (pa[i] ?? 0);
        if (diff !== 0) return diff;
    }

    return 0;
}

export const releaseNotes = Object.values(modules)
    .map((m) => ({...m.frontmatter, date: String(m.frontmatter.date).slice(0, 10), Content: m.Content}))
    .sort((a, b) => compareVersions(a.version, b.version));
