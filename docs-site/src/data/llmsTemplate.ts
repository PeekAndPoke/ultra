// Renders an LLM mirror file with version placeholders substituted.
//
// Source files under `src/data/llms/` use `{{ultraVersion}}` / `{{kraftVersion}}` markers
// that get replaced at build time with the values from `site.ts`. This keeps the LLM
// mirrors in sync with the canonical version constants without requiring them to live
// under `public/` (where they couldn't import from TypeScript).
//
// Vite's `?raw` imports inline the file contents at build time so the bundled output
// doesn't need runtime filesystem access.

import llmsTxt from './llms/llms.txt?raw';
import cacheMd from './llms/cache.md?raw';
import datetimeMd from './llms/datetime.md?raw';
import funktorMd from './llms/funktor.md?raw';
import i18nMd from './llms/i18n.md?raw';
import karangoMd from './llms/karango.md?raw';
import kontainerMd from './llms/kontainer.md?raw';
import kraftMd from './llms/kraft.md?raw';
import mathsMd from './llms/maths.md?raw';
import monkoMd from './llms/monko.md?raw';
import mutatorMd from './llms/mutator.md?raw';
import slumberMd from './llms/slumber.md?raw';
import streamsMd from './llms/streams.md?raw';
import vaultMd from './llms/vault.md?raw';

import {kotlinVersion, kraftVersion, ultraVersion} from './site';

const templates: Record<string, string> = {
    'llms.txt': llmsTxt,
    'cache.md': cacheMd,
    'datetime.md': datetimeMd,
    'funktor.md': funktorMd,
    'i18n.md': i18nMd,
    'karango.md': karangoMd,
    'kontainer.md': kontainerMd,
    'kraft.md': kraftMd,
    'maths.md': mathsMd,
    'monko.md': monkoMd,
    'mutator.md': mutatorMd,
    'slumber.md': slumberMd,
    'streams.md': streamsMd,
    'vault.md': vaultMd,
};

// The per-library mirrors, in the order they appear in llms-full.txt.
// Adding a library here is all that is needed for it to show up in the full file.
const fullOrder = [
    'kontainer', 'slumber', 'mutator', 'streams', 'datetime', 'cache', 'maths', 'i18n',
    'kraft',
    'funktor',
    'vault', 'karango', 'monko',
] as const;

/**
 * Builds `llms-full.txt` by CONCATENATING the per-library mirrors.
 *
 * It used to be a hand-maintained file duplicating every mirror, which meant a third copy of the
 * same content to keep in sync — and it had drifted badly: 8 of 13 libraries, no Funktor at all,
 * and a header still claiming "six libraries". Generating it removes that copy entirely.
 */
function renderLlmsFull(): string {
    const header = [
        '# PeekAndPoke Ultra — Complete Documentation',
        '',
        '> Every library mirror concatenated into one file. Generated — do not hand-edit.',
        `> Version: ${ultraVersion} | Kraft: ${kraftVersion} | Kotlin: ${kotlinVersion}`,
        '> GitHub: https://github.com/PeekAndPoke/ultra',
        '> Maven Central: https://central.sonatype.com/search?q=io.peekandpoke',
        '',
        '## Contents',
        '',
        ...fullOrder.map((slug) => `- ${slug} — https://peekandpoke.io/llms/${slug}.md`),
        '',
    ].join('\n');

    const bodies = fullOrder.map((slug) => {
        const raw = templates[`${slug}.md`];
        if (raw === undefined) {
            throw new Error(`llms-full.txt: no mirror registered for '${slug}'`);
        }
        return ['', '---', '', raw.trim(), ''].join('\n');
    });

    return [header, ...bodies].join('\n');
}

export function renderLlmsTemplate(filename: string): string {
    if (filename === 'llms-full.txt') {
        return renderLlmsFull()
            .replaceAll('{{ultraVersion}}', ultraVersion)
            .replaceAll('{{kraftVersion}}', kraftVersion);
    }

    const template = templates[filename];
    if (template === undefined) {
        throw new Error(`Unknown llms template: ${filename}`);
    }
    return template
        .replaceAll('{{ultraVersion}}', ultraVersion)
        .replaceAll('{{kraftVersion}}', kraftVersion);
}
