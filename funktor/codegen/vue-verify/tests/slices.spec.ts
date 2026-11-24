/**
 * The kontainer readers on real data. A misread here would not throw: it would show a wrong table, so the
 * distinguishing fields are pinned.
 */
import { describe, expect, it } from 'vitest'
import {
    graphMatch,
    joinKontainerGraph,
    readInstantMillis,
    readKontainer,
    readKontainerGraph,
    readRuntime,
    readVault,
    valuesText,
    runtimeCells,
} from '../src/funktorsdk/insights/slices.ts'
import { recorded, sliceOf } from './support.ts'

describe('readKontainer', () => {
    it('reads a record: services with creations only, no definitions, the fingerprint kept', () => {
        const raw = sliceOf(recorded.slim(), 'kontainer')
        const slice = readKontainer(raw)!

        expect(slice.graphFingerprint).toBe(raw.graphFingerprint)
        expect(slice.services).toHaveLength(raw.services.length)
        expect(slice.services.every((service) => service.definition === null && service.count > 0)).toBe(true)

        // Every service, by cls: its count and both times, each from its own key
        for (const written of raw.services) {
            const read = slice.services.find((service) => service.cls === written.cls)!
            expect(read.type, written.cls).toBe(written.type)
            expect(read.count, written.cls).toBe(written.count)
            expect(read.firstCreatedAtMillis, written.cls).toBe(written.firstCreatedAt.ts)
            expect(read.lastCreatedAtMillis, written.cls).toBe(written.lastCreatedAt.ts)
        }
    })

    it('a service of a record written before the counters reads as one without creations: its instance list is not read', () => {
        const slice = readKontainer({
            graphFingerprint: 'fp',
            services: [{ cls: 'a.Old', type: 'Prototype', instances: [{ cls: 'a.Old', createdAt: { ts: 1792413528458 } }] }],
        })!

        expect(slice.services).toEqual([
            { cls: 'a.Old', type: 'Prototype', definition: null, count: 0, firstCreatedAtMillis: null, lastCreatedAtMillis: null },
        ])
    })

    it('the graphFingerprint KEY marks the slice, even when its value is null', () => {
        const raw = sliceOf(recorded.slim(), 'kontainer')
        raw.graphFingerprint = null

        expect(readKontainer(raw)).toMatchObject({ graphFingerprint: null, services: expect.any(Array) })
    })

    it('a slice with neither marker is unrecognised, not guessed', () => {
        const raw = sliceOf(recorded.slim(), 'kontainer')
        delete raw.graphFingerprint

        expect(readKontainer(raw)).toBeNull()
    })

    it('a slice written before 2026-10-19 (`info.services`) is not read: null, so the tab shows the raw tree', () => {
        expect(readKontainer({ numOld: 0, numTotal: 1, info: { services: [{ cls: { fqn: 'a.B' }, type: 'Singleton' }] } })).toBeNull()
    })
})

describe('the kontainer graph', () => {
    it('reads flat strings for every class and code location', () => {
        const raw: any = recorded.kontainerGraph().data
        const graph = readKontainerGraph(raw)!

        expect(graph.fingerprint).toBe(raw.fingerprint)
        const withInjects = raw.services.find((service: any) => service.definition.injects.length > 0)!
        const read = graph.services.find((service) => service.cls === withInjects.cls)!

        expect(read.definition!.codeLocation).toBe(withInjects.definition.codeLocation)
        expect(read.definition!.injects[0].classes).toEqual(withInjects.definition.injects[0].classes)
    })

    it('joins a slim record on cls: each service gets ITS definition from the graph, the type from the record', () => {
        const slice = readKontainer(sliceOf(recorded.slim(), 'kontainer'))!
        const graph = readKontainerGraph(recorded.kontainerGraph().data)!

        const joined = joinKontainerGraph(slice, graph, false)

        expect(joined).toHaveLength(slice.services.length)
        expect(joined.map((service) => service.type)).toEqual(slice.services.map((service) => service.type))

        // By cls, not by index or simple name: deep-equal to the graph's own entry for the same class.
        for (const service of joined.filter((it) => it.type !== 'DynamicOverride')) {
            expect(service.definition, service.cls ?? '').toEqual(graph.services.find((it) => it.cls === service.cls)!.definition)
        }
    })

    it('a DynamicOverride is handed in: no class named, no injects, over the blueprint whose injects did not run', () => {
        const slice = readKontainer(sliceOf(recorded.slim(), 'kontainer'))!
        const graph = readKontainerGraph(recorded.kontainerGraph().data)!
        const joined = joinKontainerGraph(slice, graph, true)

        const overrides = joined.filter((service) => service.type === 'DynamicOverride')
        // One since ULTRA-00301: InsightsConfig is no longer handed in per request
        expect(overrides.map((service) => service.cls)).toEqual(['io.peekandpoke.ultra.security.user.UserProvider'])
        expect(graph.services.find((it) => it.cls === 'io.peekandpoke.ultra.security.user.UserProvider')?.definition).not.toBeNull()

        for (const service of overrides) {
            const blueprint = graph.services.find((it) => it.cls === service.cls)!.definition
            expect(service.definition).toEqual({
                creates: null,
                injectionType: 'Dynamic',
                injects: [],
                codeLocation: null,
                overwrites: blueprint,
                handedIn: true,
            })
        }
    })

    it('an override shows NONE of its blueprint\'s injects, even when the blueprint has some', () => {
        // Derived: the real graph, with UserProvider's blueprint given the injects of a real service. The real
        // override's blueprint injects nothing, so the real data alone cannot tell "[]" from "the blueprint's".
        const raw: any = recorded.kontainerGraph().data
        const donor = raw.services.find((it: any) => it.cls === 'io.peekandpoke.funktor.auth.AuthRecordStorage')
        const blueprint = raw.services.find((it: any) => it.cls === 'io.peekandpoke.ultra.security.user.UserProvider')
        blueprint.definition.injects = donor.definition.injects
        expect(blueprint.definition.injects.length).toBeGreaterThan(0)

        const joined = joinKontainerGraph(readKontainer(sliceOf(recorded.slim(), 'kontainer'))!, readKontainerGraph(raw)!, true)
        const override = joined.find((it) => it.cls === 'io.peekandpoke.ultra.security.user.UserProvider')!

        expect(override.definition!.injects).toEqual([])
        expect(override.definition!.overwrites!.injects.length).toBe(donor.definition.injects.length)
    })

    it('joins by the FULL class name: a same-named class elsewhere in the graph is not taken', () => {
        // Derived: the real graph has no two services sharing a simple name, so a join by simple name would
        // pass on it. A decoy, same simple name, other package and definition, is put FIRST.
        const raw: any = recorded.kontainerGraph().data
        const slice = readKontainer(sliceOf(recorded.slim(), 'kontainer'))!
        const target = slice.services.find((it) => it.type !== 'DynamicOverride')!
        const simple = target.cls!.split('.').pop()
        const decoyDefinition = { ...raw.services[0].definition, creates: `decoy.${simple}`, codeLocation: 'decoy' }
        raw.services.unshift({ cls: `decoy.${simple}`, type: 'Singleton', definition: decoyDefinition })

        const graph = readKontainerGraph(raw)!
        const joined = joinKontainerGraph(slice, graph, false).find((it) => it.cls === target.cls)!

        expect(joined.definition).toEqual(graph.services.find((it) => it.cls === target.cls)!.definition)
        expect(joined.definition!.creates).not.toBe(`decoy.${simple}`)
    })

    it('says whether a record was written against the graph: same, other, or unknown when it names none', () => {
        const graph = readKontainerGraph(recorded.kontainerGraph().data)!

        expect(graphMatch(readKontainer(sliceOf(recorded.slim(), 'kontainer'))!, graph)).toBe('same')
        expect(graphMatch(readKontainer(sliceOf(recorded.vault(), 'kontainer'))!, graph)).toBe('other')

        const nameless = sliceOf(recorded.slim(), 'kontainer')
        nameless.graphFingerprint = null
        expect(graphMatch(readKontainer(nameless)!, graph)).toBe('unknown')

    })
})

describe('the other readers', () => {
    it('an instant is millis from Slumber\'s object; a bare number is not guessed into a unit', () => {
        expect(readInstantMillis({ ts: 1792419655680, timezone: 'UTC', human: '...' })).toBe(1792419655680)
        expect(readInstantMillis(1785496992.7)).toBeNull()
    })

    it('vault entries of the current collector are not legacy, and keep their placeholder text', () => {
        const slice = readVault(sliceOf(recorded.vault(), 'vault'))!

        expect(new Set(slice.entries.map((entry) => entry.queryLanguage))).toEqual(new Set(['aql', 'aql-raw', 'mongo-bound']))
        expect(slice.entries.every((entry) => !entry.legacy && entry.query !== null)).toBe(true)
    })

    it('an ordinary entry carries its values and plan; a withholding one neither, only the count (ULTRA-00171)', () => {
        const slice = readVault(sliceOf(recorded.vault(), 'vault'))!
        const ordinary = slice.entries.filter((entry) => !entry.valuesWithheld)
        const withheld = slice.entries.filter((entry) => entry.valuesWithheld)

        expect(ordinary.length).toBeGreaterThan(0)
        expect(withheld.length).toBeGreaterThanOrEqual(5)

        const lookup = ordinary.find((entry) => entry.queryLanguage === 'aql' && entry.query?.includes('token'))!
        expect(Object.values(lookup.values!)).toHaveLength(1)
        expect(lookup.plan).not.toBeNull()

        for (const entry of withheld) {
            expect(entry.values).toBeNull()
            expect(entry.plan).toBeNull()
            expect(entry.varsCount).toBeGreaterThanOrEqual(1)
        }
    })

    it('the values read as one pretty JSON document; a value cut at the limit stays text', () => {
        expect(valuesText({ v_1: '"abc"', p2: '{"a":[1,null]}', p3: '"xxx … (9000 chars)' })).toBe(
            JSON.stringify({ v_1: 'abc', p2: { a: [1, null] }, p3: '"xxx … (9000 chars)' }, null, 2),
        )
    })

    it('a value JavaScript would print differently is shown as the server wrote it: a Long above 2^53, a 1.0', () => {
        expect(valuesText({ id: '9007199254740993', ratio: '1.0', doc: '{"n":9007199254740993}', ok: '42' })).toBe(
            '{\n  "id": 9007199254740993,\n  "ratio": 1.0,\n  "doc": {"n":9007199254740993},\n  "ok": 42\n}',
        )
    })

    it('a value that looks like an internal marker is just a value: each is shown in its own slot', () => {
        // The old display swapped markers into the whole text, so a value spelling one took another's place
        const marker = `${String.fromCharCode(0)}verbatim:0`

        expect(valuesText({ a: JSON.stringify(marker), b: '9007199254740993' })).toBe(
            `{\n  "a": ${JSON.stringify(marker)},\n  "b": 9007199254740993\n}`,
        )
    })

    it('a value that is not a string is dropped, not guessed', () => {
        const entry = readVault({ entries: [{ queryLanguage: 'aql', query: 'q', values: { a: '1', b: 2 } }] })!.entries[0]

        expect(entry.values).toEqual({ a: '1' })
        expect(entry.valuesWithheld).toBe(false)
    })

    it('a Mongo entry under the OLD label json has an UNKNOWN bind count, not the 0 it was recorded with', () => {
        // What the collector wrote for Monko 2026-08-24..2026-10-19: text dropped, `vars` empty, so 0.
        const raw = sliceOf(recorded.vault(), 'vault')
        const mongo = raw.entries.find((entry: { queryLanguage: string }) => entry.queryLanguage === 'mongo-bound')
        Object.assign(mongo, { queryLanguage: 'json', query: null, varsCount: 0 })

        const entry = readVault(raw)!.entries.find((it) => it.queryLanguage === 'json')!

        expect(entry.varsCount).toBeNull()
        expect(readVault(sliceOf(recorded.vault(), 'vault'))!.entries.every((it) => it.varsCount !== null)).toBe(true)
    })

    it('...and so has a PRE-collector one, which kept its text and an empty vars', () => {
        // The raw profiler entry Jackson wrote before VaultCollector.Data: Monko's text, values inline, `vars: {}`.
        const raw = { entries: [{ queryLanguage: 'json', query: '{"filter": {"token": "x"}}', vars: {}, totalNs: 1 }] }

        const entry = readVault(raw)!.entries[0]

        expect(entry.legacy).toBe(true)
        expect(entry.varsCount).toBeNull()
    })

    it('a PRE-collector entry shows neither its values nor its plan: Jackson wrote them for every repository', () => {
        const raw = { entries: [{ queryLanguage: 'aql', query: 'q', vars: { v: 'secret' }, queryExplained: 'secret', values: { v: '"secret"' }, explain: 'secret' }] }

        const entry = readVault(raw)!.entries[0]

        expect(entry.legacy).toBe(true)
        expect([entry.query, entry.values, entry.plan]).toEqual([null, null, null])
    })

    it('the file descriptor cell carries the "not measured" caveat when both counts are 0', () => {
        const raw = sliceOf(recorded.slim(), 'runtime')
        const measured = runtimeCells(readRuntime(raw)!).find((cell) => cell.label === 'File descriptors')!
        expect(measured.hint).toBe(`max: ${raw.maxFileDescriptors}`)

        const unmeasured = runtimeCells(readRuntime({ ...raw, openFileDescriptors: 0, maxFileDescriptors: 0 })!)
        expect(unmeasured.find((cell) => cell.label === 'File descriptors')!.hint).toBe('not measured on this platform')
    })
})
