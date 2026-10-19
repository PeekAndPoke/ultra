/**
 * The kontainer readers on both slice shapes, on real data. A misread here would not throw: it would show
 * a wrong table, so each shape's distinguishing fields are pinned.
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
    runtimeCells,
} from '../src/funktorsdk/insights/slices.ts'
import { recorded, sliceOf } from './support.ts'

describe('readKontainer', () => {
    it('reads a SLIM record: instantiated services only, no definitions, the fingerprint kept', () => {
        const raw = sliceOf(recorded.slim(), 'kontainer')
        const slice = readKontainer(raw)!

        expect(slice.shape).toBe('slim')
        expect(slice.graphFingerprint).toBe(raw.graphFingerprint)
        expect(slice.services).toHaveLength(raw.services.length)
        expect(slice.services.every((service) => service.definition === null && service.instances.length > 0)).toBe(true)

        const first = raw.services[0]
        const read = slice.services.find((service) => service.cls === first.cls)!
        expect(read.type).toBe(first.type)
        expect(read.instances[0].cls).toBe(first.instances[0].cls)
        expect(read.instances[0].createdAtMillis).toBe(first.instances[0].createdAt.ts)
    })

    it('reads an OLD (fat) record: every service, {fqn} and {location} unwrapped, definitions inline', () => {
        const raw = sliceOf(recorded.fat(), 'kontainer')
        const slice = readKontainer(raw)!

        expect(slice.shape).toBe('full')
        expect(slice.graphFingerprint).toBeNull()
        expect(slice.services).toHaveLength(raw.info.services.length)

        for (const service of raw.info.services) {
            const read = slice.services.find((it) => it.cls === service.cls.fqn)!
            expect(read.definition!.creates).toBe(service.definition.creates.fqn)
            expect(read.definition!.codeLocation).toBe(service.definition.codeLocation.location)
        }
    })

    it('the graphFingerprint KEY marks the slim shape, even when its value is null', () => {
        const raw = sliceOf(recorded.slim(), 'kontainer')
        raw.graphFingerprint = null

        expect(readKontainer(raw)?.shape).toBe('slim')
    })

    it('a slice with neither marker is unrecognised, not guessed', () => {
        const raw = sliceOf(recorded.slim(), 'kontainer')
        delete raw.graphFingerprint

        expect(readKontainer(raw)).toBeNull()
    })

    it('only a full record has counts: a slim one has none, and a stray field is not read as one', () => {
        const fat = sliceOf(recorded.fat(), 'kontainer')
        expect(readKontainer(fat)).toMatchObject({ numOld: fat.numOld, numTotal: fat.numTotal })

        const slim = sliceOf(recorded.slim(), 'kontainer')
        expect(Object.keys(slim).sort()).toEqual(['graphFingerprint', 'services'])
        expect(readKontainer({ ...slim, numOld: 3, numTotal: 7 })).toMatchObject({ numOld: null, numTotal: null })
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

    it('a DynamicOverride gets what a full record wrote for it, not the blueprint whose injects did not run', () => {
        const slice = readKontainer(sliceOf(recorded.slim(), 'kontainer'))!
        const graph = readKontainerGraph(recorded.kontainerGraph().data)!
        const joined = joinKontainerGraph(slice, graph, true)

        const overrides = joined.filter((service) => service.type === 'DynamicOverride')
        expect(overrides.map((service) => service.cls)).toEqual(
            expect.arrayContaining(['io.peekandpoke.ultra.security.user.UserProvider', 'io.peekandpoke.funktor.core.model.InsightsConfig']),
        )

        for (const service of overrides) {
            const blueprint = graph.services.find((it) => it.cls === service.cls)!.definition
            expect(service.definition).toEqual({
                creates: service.instances[0].cls,
                injectionType: 'Dynamic',
                injects: [],
                codeLocation: null,
                overwrites: blueprint,
            })
        }

        // ...which is what the full record of the same service really says
        const v1 = readKontainer(sliceOf(recorded.fat(), 'kontainer'))!.services
            .find((service) => service.cls === 'io.peekandpoke.ultra.security.user.UserProvider')!.definition!
        const v2 = overrides.find((service) => service.cls === 'io.peekandpoke.ultra.security.user.UserProvider')!.definition!

        expect(v1.creates).toBe(v2.creates)
        expect(v1.injectionType).toBe(v2.injectionType)
        expect(v1.injects).toEqual(v2.injects)
        expect(v1.overwrites!.creates).toBe(v2.overwrites!.creates)
        expect(v1.overwrites!.injectionType).toBe(v2.overwrites!.injectionType)
    })

    it('an override shows NONE of its blueprint\'s injects, even when the blueprint has some', () => {
        // Derived: the real graph, with UserProvider's blueprint given the injects of a real service. The two
        // real overrides' blueprints inject nothing, so the real data alone cannot tell "[]" from "the blueprint's".
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

        expect(graphMatch(readKontainer(sliceOf(recorded.fat(), 'kontainer'))!, graph)).toBe('same')
    })
})

describe('the other readers', () => {
    it('an instant is millis from Slumber\'s object, and seconds x 1000 from a Jackson-era double', () => {
        expect(readInstantMillis({ ts: 1792419655680, timezone: 'UTC', human: '...' })).toBe(1792419655680)
        expect(readInstantMillis(1785496992.7)).toBe(1785496992700)
    })

    it('vault entries of the current collector are not legacy, and keep their placeholder text', () => {
        const slice = readVault(sliceOf(recorded.vault(), 'vault'))!

        expect(slice.entries.map((entry) => entry.queryLanguage).sort()).toEqual(['aql', 'mongo-bound'])
        expect(slice.entries.every((entry) => !entry.legacy && entry.query !== null && entry.varsCount === 1)).toBe(true)
    })

    it('a Mongo entry under the OLD label json has an UNKNOWN bind count, not the 0 it was recorded with', () => {
        // What the collector wrote for Monko 2026-08-24..2026-10-19: text dropped, `vars` empty, so 0.
        const raw = sliceOf(recorded.vault(), 'vault')
        const mongo = raw.entries.find((entry: { queryLanguage: string }) => entry.queryLanguage === 'mongo-bound')
        Object.assign(mongo, { queryLanguage: 'json', query: null, varsCount: 0 })

        const entry = readVault(raw)!.entries.find((it) => it.queryLanguage === 'json')!

        expect(entry.varsCount).toBeNull()
        expect(readVault(sliceOf(recorded.vault(), 'vault'))!.entries.every((it) => it.varsCount === 1)).toBe(true)
    })

    it('...and so has a PRE-collector one, which kept its text and an empty vars', () => {
        // The raw profiler entry Jackson wrote before VaultCollector.Data: Monko's text, values inline, `vars: {}`.
        const raw = { entries: [{ queryLanguage: 'json', query: '{"filter": {"token": "x"}}', vars: {}, totalNs: 1 }] }

        const entry = readVault(raw)!.entries[0]

        expect(entry.legacy).toBe(true)
        expect(entry.varsCount).toBeNull()
    })

    it('the file descriptor cell carries the "not measured" caveat when both counts are 0', () => {
        const raw = sliceOf(recorded.slim(), 'runtime')
        const measured = runtimeCells(readRuntime(raw)!).find((cell) => cell.label === 'File descriptors')!
        expect(measured.hint).toBe(`max: ${raw.maxFileDescriptors}`)

        const unmeasured = runtimeCells(readRuntime({ ...raw, openFileDescriptors: 0, maxFileDescriptors: 0 })!)
        expect(unmeasured.find((cell) => cell.label === 'File descriptors')!.hint).toBe('not measured on this platform')
    })
})
