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
import llmsFullTxt from './llms/llms-full.txt?raw';
import cacheMd from './llms/cache.md?raw';
import datetimeMd from './llms/datetime.md?raw';
import funktorMd from './llms/funktor.md?raw';
import karangoMd from './llms/karango.md?raw';
import kontainerMd from './llms/kontainer.md?raw';
import kraftMd from './llms/kraft.md?raw';
import monkoMd from './llms/monko.md?raw';
import mutatorMd from './llms/mutator.md?raw';
import slumberMd from './llms/slumber.md?raw';
import streamsMd from './llms/streams.md?raw';
import vaultMd from './llms/vault.md?raw';

import {kraftVersion, ultraVersion} from './site';

const templates: Record<string, string> = {
    'llms.txt': llmsTxt,
    'llms-full.txt': llmsFullTxt,
    'cache.md': cacheMd,
    'datetime.md': datetimeMd,
    'funktor.md': funktorMd,
    'karango.md': karangoMd,
    'kontainer.md': kontainerMd,
    'kraft.md': kraftMd,
    'monko.md': monkoMd,
    'mutator.md': mutatorMd,
    'slumber.md': slumberMd,
    'streams.md': streamsMd,
    'vault.md': vaultMd,
};

export function renderLlmsTemplate(filename: string): string {
    const template = templates[filename];
    if (template === undefined) {
        throw new Error(`Unknown llms template: ${filename}`);
    }
    return template
        .replaceAll('{{ultraVersion}}', ultraVersion)
        .replaceAll('{{kraftVersion}}', kraftVersion);
}
