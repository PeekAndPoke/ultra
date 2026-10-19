/**
 * The kontainer readers on both slice shapes, on real data. A misread here would not throw: it would show
 * a wrong table, so each shape's distinguishing fields are pinned.
 */
import { describe, expect, it } from 'vitest'
import {
    instanceCountsMeasured,
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

    it('the counts are "not measured" when they are funktor\'s constant 0', () => {
        expect(instanceCountsMeasured(readKontainer(sliceOf(recorded.slim(), 'kontainer'))!)).toBe(false)
        expect(instanceCountsMeasured({ ...readKontainer(sliceOf(recorded.slim(), 'kontainer'))!, numTotal: 3 })).toBe(true)
    })
})

describe('the kontainer graph', () => {
    it('reads flat strings for every class and code location', () => {
        const raw = recorded.kontainerGraph().data
        const graph = readKontainerGraph(raw)!

        expect(graph.fingerprint).toBe(raw.fingerprint)
        const withInjects = raw.services.find((service: any) => service.definition.injects.length > 0)!
        const read = graph.services.find((service) => service.cls === withInjects.cls)!

        expect(read.definition!.codeLocation).toBe(withInjects.definition.codeLocation)
        expect(read.definition!.injects[0].classes).toEqual(withInjects.definition.injects[0].classes)
    })

    it('joins a slim record on cls: definitions from the graph, the type from the record', () => {
        const slice = readKontainer(sliceOf(recorded.slim(), 'kontainer'))!
        const graph = readKontainerGraph(recorded.kontainerGraph().data)!

        const joined = joinKontainerGraph(slice, graph, false)

        expect(joined).toHaveLength(slice.services.length)
        expect(joined.every((service) => service.definition !== null)).toBe(true)
        expect(joined.map((service) => service.type)).toEqual(slice.services.map((service) => service.type))
    })
})

describe('the other readers', () => {
    it('an instant is millis from Slumber\'s object, and seconds x 1000 from a Jackson-era double', () => {
        expect(readInstantMillis({ ts: 1792419655680, timezone: 'UTC', human: '...' })).toBe(1792419655680)
        expect(readInstantMillis(1785496992.7)).toBe(1785496992700)
    })

    it('vault entries of the current collector are not legacy, and keep their placeholder text', () => {
        const slice = readVault(sliceOf(recorded.vault(), 'vault'))!

        expect(slice.entries.every((entry) => !entry.legacy && entry.query !== null && entry.varsCount === 1)).toBe(true)
    })

    it('the file descriptor cell carries the "not measured" caveat when both counts are 0', () => {
        const raw = sliceOf(recorded.slim(), 'runtime')
        const measured = runtimeCells(readRuntime(raw)!).find((cell) => cell.label === 'File descriptors')!
        expect(measured.hint).toBe(`max: ${raw.maxFileDescriptors}`)

        const unmeasured = runtimeCells(readRuntime({ ...raw, openFileDescriptors: 0, maxFileDescriptors: 0 })!)
        expect(unmeasured.find((cell) => cell.label === 'File descriptors')!.hint).toBe('not measured on this platform')
    })
})
