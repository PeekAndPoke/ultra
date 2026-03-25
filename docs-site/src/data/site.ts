// Single source of truth for versions, URLs, and Maven coordinates.
// Update these when releasing — all pages reference this file.

// Versions
export const ultraVersion = '0.104.1';
export const kraftVersion = '0.104.1';
export const kotlinVersion = '2.3.10';
export const fomanticVersion = '2.8.8';

// Maven coordinates
export const ultraGroup = 'io.peekandpoke.ultra';
export const kraftGroup = 'io.peekandpoke.kraft';

// URLs
export const githubRepo = 'https://github.com/PeekAndPoke/ultra';
export const mavenCentralUrl = 'https://central.sonatype.com/namespace/io.peekandpoke.ultra';
export const fomanticCdnUrl = `https://cdn.jsdelivr.net/npm/fomantic-ui@${fomanticVersion}/dist/semantic.min.css`;

// Example project repos
export const kraftExamples = {
    helloworld: 'https://github.com/PeekAndPoke/kraft-example-helloworld',
    router: 'https://github.com/PeekAndPoke/kraft-example-router',
    remote: 'https://github.com/PeekAndPoke/kraft-example-remote',
};

// Live example paths (served from public/kraft-examples via symlinks)
export const kraftLiveExamples = {
    helloWorld: '/kraft-examples/hello-world/index.html',
    addons: '/kraft-examples/addons/index.html',
    fomanticui: '/kraft-examples/fomanticui/index.html',
};

// GitHub source links for each library
export const githubSource = {
    kraft: `${githubRepo}/tree/master/kraft`,
    kontainer: `${githubRepo}/tree/master/ultra/kontainer`,
    slumber: `${githubRepo}/tree/master/ultra/slumber`,
    mutator: `${githubRepo}/tree/master/mutator`,
    streams: `${githubRepo}/tree/master/ultra/streams`,
    karango: `${githubRepo}/tree/master/karango`,
};

// Helpers for dependency strings used in code examples
export const dep = {
    kontainer: `${ultraGroup}:kontainer:${ultraVersion}`,
    slumber: `${ultraGroup}:slumber:${ultraVersion}`,
    streams: `${ultraGroup}:streams:${ultraVersion}`,
    kraftCore: `${kraftGroup}:core:${kraftVersion}`,
    kraftSemanticUi: `${kraftGroup}:semanticui:${kraftVersion}`,
    kraftAddonChartJs: `${kraftGroup}:addons-chartjs:${kraftVersion}`,
    kraftAddonPrismJs: `${kraftGroup}:addons-prismjs:${kraftVersion}`,
    mutatorCore: `${ultraGroup}:mutator-core:${ultraVersion}`,
    mutatorKsp: `${ultraGroup}:mutator-ksp:${ultraVersion}`,
    karangoCore: `${ultraGroup}:karango-core:${ultraVersion}`,
    karangoKsp: `${ultraGroup}:karango-ksp:${ultraVersion}`,
};
