// Single source of truth for versions, URLs, and Maven coordinates.
// Update these when releasing — all pages reference this file.

// Versions
export const ultraVersion = '0.108.0';
export const kraftVersion = '0.108.0';
export const kotlinVersion = '2.3.10';
export const fomanticVersion = '2.8.8';

// Maven coordinates
// Each product line publishes under its OWN group id — see gradle.properties.
// Do NOT glue a compound artifact name onto ultraGroup; that coordinate does not exist.
export const ultraGroup = 'io.peekandpoke.ultra';
export const kraftGroup = 'io.peekandpoke.kraft';
export const funktorGroup = 'io.peekandpoke.funktor';
export const karangoGroup = 'io.peekandpoke.karango';
export const monkoGroup = 'io.peekandpoke.monko';
export const mutatorGroup = 'io.peekandpoke.mutator';

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
    datetime: `${githubRepo}/tree/master/ultra/datetime`,
    cache: `${githubRepo}/tree/master/ultra/cache`,
    maths: `${githubRepo}/tree/master/ultra/maths`,
    karango: `${githubRepo}/tree/master/karango`,
    funktor: `${githubRepo}/tree/master/funktor`,
    monko: `${githubRepo}/tree/master/monko`,
};

// Category color tints — Hilbert light, detail-page bg, sidebar border, card hover glow
// Each category gets a subtle visual identity across landing and detail pages
export const categoryTints = {
    tools: {
        light: '#c8dca8',
        bg: '#0c0e0b',
        border: 'rgba(160,190,120,0.08)',
        glow: 'rgba(160,190,120,0.15)',
        glowSm: 'rgba(160,190,120,0.08)'
    },
    kraft: {
        light: '#ffb8a8',
        bg: '#110d0c',
        border: 'rgba(220,140,120,0.08)',
        glow: 'rgba(220,140,120,0.15)',
        glowSm: 'rgba(220,140,120,0.08)'
    },
    funktor: {
        light: '#c8a8e8',
        bg: '#0e0c11',
        border: 'rgba(170,140,200,0.08)',
        glow: 'rgba(170,140,200,0.15)',
        glowSm: 'rgba(170,140,200,0.08)'
    },
} as const;

// Maps URL prefixes to their category tint
export const pathToCategory: Record<string, keyof typeof categoryTints> = {
    '/ultra/kontainer/': 'tools',
    '/ultra/slumber/': 'tools',
    '/ultra/mutator/': 'tools',
    '/ultra/streams/': 'tools',
    '/ultra/datetime/': 'tools',
    '/ultra/cache/': 'tools',
    '/ultra/maths/': 'tools',
    '/ultra/kraft/': 'kraft',
    '/ultra/funktor/': 'funktor',
    '/ultra/karango/': 'funktor',
    '/ultra/monko/': 'funktor',
};

// Helpers for dependency strings used in code examples
export const dep = {
    kontainer: `${ultraGroup}:kontainer:${ultraVersion}`,
    slumber: `${ultraGroup}:slumber:${ultraVersion}`,
    streams: `${ultraGroup}:streams:${ultraVersion}`,
    datetime: `${ultraGroup}:datetime:${ultraVersion}`,
    cache: `${ultraGroup}:cache:${ultraVersion}`,
    i18n: `${ultraGroup}:i18n:${ultraVersion}`,
    maths: `${ultraGroup}:maths:${ultraVersion}`,
    kraftCore: `${kraftGroup}:core:${kraftVersion}`,
    kraftSemanticUi: `${kraftGroup}:semanticui:${kraftVersion}`,
    kraftAddonDatetime: `${kraftGroup}:addons-datetime:${kraftVersion}`,
    kraftAddonChartJs: `${kraftGroup}:addons-chartjs:${kraftVersion}`,
    kraftAddonPrismJs: `${kraftGroup}:addons-prismjs:${kraftVersion}`,
    kraftAddonMarked: `${kraftGroup}:addons-marked:${kraftVersion}`,
    kraftAddonSignaturePad: `${kraftGroup}:addons-signaturepad:${kraftVersion}`,
    kraftAddonPixiJs: `${kraftGroup}:addons-pixijs:${kraftVersion}`,
    kraftAddonPdfJs: `${kraftGroup}:addons-pdfjs:${kraftVersion}`,
    kraftAddonJwtDecode: `${kraftGroup}:addons-jwtdecode:${kraftVersion}`,
    kraftAddonAvatars: `${kraftGroup}:addons-avatars:${kraftVersion}`,
    kraftAddonBrowserDetect: `${kraftGroup}:addons-browserdetect:${kraftVersion}`,
    kraftAddonNxCompile: `${kraftGroup}:addons-nxcompile:${kraftVersion}`,
    kraftAddonSourceMappedStacktrace: `${kraftGroup}:addons-sourcemappedstacktrace:${kraftVersion}`,
    kraftAddonThreeJs: `${kraftGroup}:addons-threejs:${kraftVersion}`,
    vault: `${ultraGroup}:vault:${ultraVersion}`,
    mutatorCore: `${mutatorGroup}:core:${ultraVersion}`,
    mutatorKsp: `${mutatorGroup}:ksp:${ultraVersion}`,
    karangoCore: `${karangoGroup}:core:${ultraVersion}`,
    karangoKsp: `${karangoGroup}:ksp:${ultraVersion}`,
    monkoCore: `${monkoGroup}:core:${ultraVersion}`,
    monkoKsp: `${monkoGroup}:ksp:${ultraVersion}`,
    funktorAll: `${funktorGroup}:all:${ultraVersion}`,
    funktorCore: `${funktorGroup}:core:${ultraVersion}`,
    funktorRest: `${funktorGroup}:rest:${ultraVersion}`,
    funktorAuth: `${funktorGroup}:auth:${ultraVersion}`,
    funktorCluster: `${funktorGroup}:cluster:${ultraVersion}`,
    funktorMessaging: `${funktorGroup}:messaging:${ultraVersion}`,
    funktorInsights: `${funktorGroup}:insights:${ultraVersion}`,
    funktorLogging: `${funktorGroup}:logging:${ultraVersion}`,
    funktorStaticWeb: `${funktorGroup}:staticweb:${ultraVersion}`,
    funktorTesting: `${funktorGroup}:testing:${ultraVersion}`,
};
