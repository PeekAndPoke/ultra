// Copies the REAL insights responses the funktor/all e2e specs dump into this directory.
//
//   INSIGHTS_WIRE_SAMPLES_DIR=/some/dir console/wt-gradle.sh :funktor:all:jvmTest
//   node funktor/codegen/vue-verify/fixtures/refresh.mjs /some/dir
//
// The only change made to them: the runtime slice's `systemProperties` keep the keys below and lose the
// rest, which hold the build machine's paths and user name. Everything else is byte-for-byte what the
// server sent. Never hand-edit the JSON files here; re-run this instead.

import { readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const KEPT_PROPERTIES = ['file.encoding', 'java.version', 'java.vendor', 'java.vm.name', 'os.arch', 'os.name']

const source = process.argv[2]
if (!source) throw new Error('usage: node refresh.mjs <INSIGHTS_WIRE_SAMPLES_DIR>')

const here = new URL('.', import.meta.url).pathname
const files = readdirSync(source)

/** The current record InsightsRecordingSpec's request produces, as InsightsKontainerSliceSpec served it. */
const slim = files.find((name) => /^record-response-\d{4}-\d{2}-\d{2}T.*\.json$/.test(name) && !name.includes('2026-10-01T12:00:00'))

const targets = {
    'record-slim.json': slim,
    // InsightsKontainerSliceSpec's frozen FAT_RECORD, as the API serves it
    'record-fat.json': 'record-response-2026-10-01T12:00:00.000000000.json',
    // InsightsVaultBindValuesSpec: a session-token lookup on Arango and on Mongo, profiling and explain on
    'record-vault.json': 'record-response-vault-two-backends.json',
    'graph-kontainer.json': 'kontainer-graph.json',
    'graph-database.json': 'database-graph-two-backends.json',
    'refusal-graph.json': 'refusal-kontainer-graph.json',
}

function trimRuntime(response) {
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
    if (!name || !files.includes(name)) throw new Error(`${source} has no sample for ${target} (${name})`)

    const response = trimRuntime(JSON.parse(readFileSync(join(source, name), 'utf8')))
    writeFileSync(join(here, target), JSON.stringify(response, null, 2) + '\n')

    console.log(`${target} <- ${name}`)
}
