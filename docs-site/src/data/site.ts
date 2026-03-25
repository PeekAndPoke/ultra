// Single source of truth for versions, URLs, and Maven coordinates.
// Update these when releasing — all pages reference this file.

// Versions
export const ultraVersion = '0.102.0';
export const kraftVersion = '0.102.0';
export const kotlinVersion = '2.3.10';
export const fomanticVersion = '2.8.8';

// Maven coordinates
export const ultraGroup = 'io.peekandpoke.ultra';
export const kraftGroup = 'io.peekandpoke.kraft';

// URLs
export const githubRepo = 'https://github.com/PeekAndPoke/ultra';
export const mavenCentralUrl = 'https://central.sonatype.com/namespace/io.peekandpoke.ultra';
export const fomanticCdnUrl = `https://cdn.jsdelivr.net/npm/fomantic-ui@${fomanticVersion}/dist/semantic.min.css`;

// Helpers for dependency strings used in code examples
export const dep = {
    kontainer: `${ultraGroup}:kontainer:${ultraVersion}`,
    slumber: `${ultraGroup}:slumber:${ultraVersion}`,
    streams: `${ultraGroup}:streams:${ultraVersion}`,
    kraftCore: `${kraftGroup}:core:${kraftVersion}`,
    kraftSemanticUi: `${kraftGroup}:semanticui:${kraftVersion}`,
    kraftAddonChartJs: `${kraftGroup}:addons-chartjs:${kraftVersion}`,
    kraftAddonPrismJs: `${kraftGroup}:addons-prismjs:${kraftVersion}`,
};
