// Copies the REAL insights responses the funktor/all e2e specs dump into this directory.
//
//   INSIGHTS_WIRE_SAMPLES_DIR=/some/dir console/wt-gradle.sh :funktor:all:jvmTest
//   node funktor/codegen/vue-verify/fixtures/refresh.mjs /some/dir
//
// Each response is parsed and re-serialised (2-space indent). Three things are rewritten, because they
// identify the machine that ran the build; everything else is what the server sent:
//
//   - the runtime slice's `systemProperties` keep the keys below and lose the rest (paths, user name);
//   - the envelope's `insights.server`, the build machine's hostname, becomes SERVER;
//   - every per-worktree test database name (`ultra_wt_…`) becomes DATABASE, consistently everywhere.
//
// Never hand-edit the JSON files here; re-run this instead.

import { readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const KEPT_PROPERTIES = ['file.encoding', 'java.version', 'java.vendor', 'java.vm.name', 'os.arch', 'os.name']
const SERVER = 'build-host'
const DATABASE = 'funktor_test'

const source = process.argv[2]
if (!source) throw new Error('usage: node refresh.mjs <INSIGHTS_WIRE_SAMPLES_DIR>')

const here = fileURLToPath(new URL('.', import.meta.url))
const files = readdirSync(source)

/** The current record of InsightsRecordingSpec's request, as InsightsKontainerSliceSpec served it -- exactly one. */
const slimCandidates = files.filter(
    (name) => /^record-response-\d{4}-\d{2}-\d{2}T.*\.json$/.test(name) && !name.includes('2026-10-01T12:00:00'),
)
if (slimCandidates.length !== 1) {
    throw new Error(`expected exactly one current record in ${source}, found ${slimCandidates.length}: use a fresh directory`)
}

const targets = {
    'record-slim.json': slimCandidates[0],
    // InsightsKontainerSliceSpec's frozen FAT_RECORD, as the API serves it
    'record-fat.json': 'record-response-2026-10-01T12:00:00.000000000.json',
    // InsightsVaultBindValuesSpec: a session-token lookup on Arango and on Mongo, profiling and explain on
    'record-vault.json': 'record-response-vault-two-backends.json',
    'graph-kontainer.json': 'kontainer-graph.json',
    'graph-database.json': 'database-graph-two-backends.json',
    'refusal-graph.json': 'refusal-kontainer-graph.json',
}

function anonymise(response) {
    if (response.insights && typeof response.insights.server === 'string') response.insights.server = SERVER

    for (const slice of response.data?.collectors ?? []) {
        const properties = slice.key === 'runtime' ? slice.data?.systemProperties : undefined
        if (properties === undefined) continue

        slice.data.systemProperties = Object.fromEntries(
            Object.entries(properties).filter(([key]) => KEPT_PROPERTIES.includes(key)),
        )
    }
    return response
}

for (const [target, name] of Object.entries(targets)) {
    if (!files.includes(name)) throw new Error(`${source} has no sample for ${target} (${name})`)

    const text = readFileSync(join(source, name), 'utf8').replaceAll(/ultra_wt_[A-Za-z0-9_]+/g, DATABASE)
    writeFileSync(join(here, target), JSON.stringify(anonymise(JSON.parse(text)), null, 2) + '\n')

    console.log(`${target} <- ${name}`)
}
