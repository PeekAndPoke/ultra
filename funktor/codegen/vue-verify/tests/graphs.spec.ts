/**
 * The graphs: fetched lazily, the kontainer graph cached by fingerprint, drawn from the right data, and never
 * able to take a tab down. `vis-network` is replaced by a recorder here; `graphs-real-library.spec.ts` runs the real one.
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import KontainerTab from '../src/funktorsdk/insights/KontainerTab.vue'
import VaultTab from '../src/funktorsdk/insights/VaultTab.vue'
import { databaseNetwork, kontainerNetwork, readDatabaseGraph } from '../src/funktorsdk/insights/graphs.ts'
import { joinKontainerGraph, readKontainer, readKontainerGraph } from '../src/funktorsdk/insights/slices.ts'
import type { KontainerGraphData, KontainerSlice } from '../src/funktorsdk/insights/slices.ts'
import {
    click,
    DATABASE_GRAPH_PATH,
    KONTAINER_GRAPH_PATH,
    mount,
    recorded,
    recordedGraphs,
    sequencedKontainerGraphs,
    sliceOf,
} from './support.ts'
import type { Mounted } from './support.ts'

const drawn = vi.hoisted(() => ({ networks: [] as { data: { nodes: any[]; edges: any[] }; options: any }[] }))

vi.mock('vis-network/standalone', () => ({
    Network: class {
        constructor(_container: HTMLElement, data: { nodes: any[]; edges: any[] }, options: unknown) {
            drawn.networks.push({ data, options })
        }

        destroy(): void {}
    },
}))

let mounted: Mounted[] = []

beforeEach(() => {
    drawn.networks.length = 0
})

afterEach(() => {
    mounted.forEach((it) => it.unmount())
    mounted = []
})

async function render(component: object, data: unknown, graphs?: Parameters<typeof mount>[2]): Promise<HTMLElement> {
    const it = await mount(component, { data }, graphs)
    mounted.push(it)
    return it.host
}

function tableRows(host: HTMLElement): number {
    return host.querySelectorAll('.fk-services tbody tr').length
}

const slimSlice = (): KontainerSlice => readKontainer(sliceOf(recorded.slim(), 'kontainer'))!
const graphData = (): KontainerGraphData => readKontainerGraph(recorded.kontainerGraph().data)!

describe('the kontainer graphs', () => {
    it('are NOT fetched until a graph button is clicked', async () => {
        const { graphs, requests } = recordedGraphs()
        await render(KontainerTab, sliceOf(recorded.slim(), 'kontainer'), graphs)

        expect(requests).toEqual([])
    })

    it('fetch the graph on the first click and draw the instantiated services from it', async () => {
        const { graphs, requests } = recordedGraphs()
        const slice = sliceOf(recorded.slim(), 'kontainer')
        const host = await render(KontainerTab, slice, graphs)

        await click(host, 'Show instances Graph')

        expect(requests).toEqual([KONTAINER_GRAPH_PATH])
        expect(drawn.networks).toHaveLength(1)
        expect(drawn.networks[0].data.nodes).toHaveLength(slice.services.length)
        expect(drawn.networks[0].data.edges.length).toBeGreaterThan(0)
        expect(drawn.networks[0].options.physics.solver).toBe('forceAtlas2Based')

        // and the table gained what only the graph has
        expect(host.textContent).not.toContain('Injects and definitions are not part of this record')
        expect(host.textContent).toContain('insights_module.kt')
    })

    it('are fetched ONCE per fingerprint, however many tabs, graphs and records of it ask', async () => {
        const { graphs, requests } = recordedGraphs()
        const first = await render(KontainerTab, sliceOf(recorded.slim(), 'kontainer'), graphs)

        await click(first, 'Show instances Graph')
        await click(first, 'Show Full Graph')

        // another record of the same graph, opened on the same page: the same source
        const second = await render(KontainerTab, sliceOf(recorded.slim(), 'kontainer'), graphs)
        await click(second, 'Show Full Graph')

        expect(requests).toEqual([KONTAINER_GRAPH_PATH])
    })

    it('a record naming ANOTHER fingerprint asks once more; the server still differing, that answer is kept', async () => {
        const { graphs, requests } = recordedGraphs()
        const first = await render(KontainerTab, sliceOf(recorded.slim(), 'kontainer'), graphs)
        await click(first, 'Show instances Graph')

        // record-vault.json was written by another app, so it names another fingerprint
        const other = await render(KontainerTab, sliceOf(recorded.vault(), 'kontainer'), graphs)
        await click(other, 'Show Full Graph')

        expect(requests).toEqual([KONTAINER_GRAPH_PATH, KONTAINER_GRAPH_PATH])
        expect(other.textContent).toContain('written against another kontainer graph')
        expect(other.textContent).toContain('Asked for it at')

        // That record's full graph is its own services plus the server's graph's.
        const own = sliceOf(recorded.vault(), 'kontainer').services.map((service: { cls: string }) => service.cls)
        const union = new Set([...own, ...graphData().services.map((service) => service.cls)])
        expect(drawn.networks.at(-1)!.data.nodes).toHaveLength(union.size)
        expect(drawn.networks.at(-1)!.options.physics.solver).toBe('barnesHut')

        // ...and a third record naming it does not ask again
        const again = await render(KontainerTab, sliceOf(recorded.vault(), 'kontainer'), graphs)
        await click(again, 'Show instances Graph')
        expect(requests).toHaveLength(2)
        expect(again.textContent).toContain('written against another kontainer graph')
    })

    it('the graph a fetch brought is cached under ITS fingerprint, and a record naming none takes it', async () => {
        const { graphs, requests } = recordedGraphs()

        // asked for another fingerprint, the server answers its own
        const other = await render(KontainerTab, sliceOf(recorded.vault(), 'kontainer'), graphs)
        await click(other, 'Show instances Graph')

        const same = await render(KontainerTab, sliceOf(recorded.slim(), 'kontainer'), graphs)
        await click(same, 'Show instances Graph')

        const none = sliceOf(recorded.slim(), 'kontainer')
        none.graphFingerprint = null
        const unnamed = await render(KontainerTab, none, graphs)
        await click(unnamed, 'Show instances Graph')

        expect(requests).toEqual([KONTAINER_GRAPH_PATH])
        expect(same.textContent).not.toContain('written against another kontainer graph')
        expect(unnamed.textContent).toContain('cannot be verified')
    })

    it('after a redeploy, a record of the NEW build gets the new graph, not the one cached before it', async () => {
        const before = recorded.kontainerGraph()
        before.data.fingerprint = 'graph-before-the-deploy'
        const { graphs, requests } = sequencedKontainerGraphs([before, recorded.kontainerGraph()])

        const old = sliceOf(recorded.slim(), 'kontainer')
        old.graphFingerprint = 'graph-before-the-deploy'
        const oldHost = await render(KontainerTab, old, graphs)
        await click(oldHost, 'Show instances Graph')
        expect(oldHost.textContent).not.toContain('written against another kontainer graph')

        const fresh = await render(KontainerTab, sliceOf(recorded.slim(), 'kontainer'), graphs)
        await click(fresh, 'Show instances Graph')

        expect(requests).toEqual([KONTAINER_GRAPH_PATH, KONTAINER_GRAPH_PATH])
        expect(fresh.textContent).not.toContain('written against another kontainer graph')

        // both answers stay cached under their own fingerprints
        const oldAgain = await render(KontainerTab, old, graphs)
        await click(oldAgain, 'Show instances Graph')
        expect(requests).toHaveLength(2)
        expect(oldAgain.textContent).not.toContain('written against another kontainer graph')
    })

    it('a 401 leaves the tab rendering, with the refusal said', async () => {
        const { graphs } = recordedGraphs({ [KONTAINER_GRAPH_PATH]: { status: 401, body: recorded.refusal() } })
        const slice = sliceOf(recorded.slim(), 'kontainer')
        const host = await render(KontainerTab, slice, graphs)

        await click(host, 'Show instances Graph')

        expect(host.querySelector('.fk-notice--error')?.textContent).toContain('401')
        expect(drawn.networks).toHaveLength(0)
        expect(tableRows(host)).toBe(slice.services.length)
    })

    it('a missing endpoint -- an older server answering with an HTML 404 -- leaves the tab rendering', async () => {
        const { graphs } = recordedGraphs({ [KONTAINER_GRAPH_PATH]: { status: 404, body: '<html>Not Found</html>' } })
        const slice = sliceOf(recorded.slim(), 'kontainer')
        const host = await render(KontainerTab, slice, graphs)

        await click(host, 'Show instances Graph')

        expect(host.querySelector('.fk-notice--error')?.textContent).toContain('not available')
        expect(tableRows(host)).toBe(slice.services.length)
    })

    it('a FAILED fetch is not kept: the next click asks again', async () => {
        const { graphs, requests } = recordedGraphs({ [KONTAINER_GRAPH_PATH]: { status: 401, body: recorded.refusal() } })
        const host = await render(KontainerTab, sliceOf(recorded.slim(), 'kontainer'), graphs)

        await click(host, 'Show instances Graph')
        await click(host, 'Show instances Graph') // hide
        await click(host, 'Show instances Graph')

        expect(requests).toEqual([KONTAINER_GRAPH_PATH, KONTAINER_GRAPH_PATH])
    })

    it('a record written against another graph says so', async () => {
        // record-vault.json was written by another app (blueprint), so its fingerprint differs
        const slice = sliceOf(recorded.vault(), 'kontainer')
        expect(slice.graphFingerprint).not.toBe(recorded.kontainerGraph().data.fingerprint)

        const { graphs } = recordedGraphs()
        const host = await render(KontainerTab, slice, graphs)
        await click(host, 'Show instances Graph')

        expect(host.textContent).toContain('written against another kontainer graph')
    })

    it('a record that names NO graph says its match cannot be verified', async () => {
        const slice = sliceOf(recorded.slim(), 'kontainer')
        slice.graphFingerprint = null

        const { graphs } = recordedGraphs()
        const host = await render(KontainerTab, slice, graphs)
        await click(host, 'Show instances Graph')

        expect(host.textContent).toContain('cannot be verified')
        expect(host.textContent).toContain('fetched at')
        expect(host.textContent).not.toContain('written against another kontainer graph')
    })

    it('...and a record of the SAME graph does not', async () => {
        const slice = sliceOf(recorded.slim(), 'kontainer')
        expect(slice.graphFingerprint).toBe(recorded.kontainerGraph().data.fingerprint)

        const { graphs } = recordedGraphs()
        const host = await render(KontainerTab, slice, graphs)
        await click(host, 'Show instances Graph')

        expect(host.textContent).not.toContain('written against another kontainer graph')
    })

    it('an OLD (fat) record draws from the record itself, with no request', async () => {
        const { graphs, requests } = recordedGraphs()
        const slice = sliceOf(recorded.fat(), 'kontainer')
        const host = await render(KontainerTab, slice, graphs)

        await click(host, 'Show Full Graph')

        expect(requests).toEqual([])
        expect(drawn.networks[0].data.nodes).toHaveLength(slice.info.services.length)
    })

    it('without a graph source a slim record says why there is no graph, and still lists its services', async () => {
        const slice = sliceOf(recorded.slim(), 'kontainer')
        const host = await render(KontainerTab, slice)

        expect(host.textContent).toContain('No API client here')
        expect(tableRows(host)).toBe(slice.services.length)
    })
})

describe('the kontainer network, built from the real graph', () => {
    const services = () => joinKontainerGraph(slimSlice(), graphData(), true)

    it('draws every edge between two nodes, and counts the ones it cannot draw', () => {
        const network = kontainerNetwork(services(), 'full')
        const ids = new Set(network.nodes.map((node) => node.id))

        for (const edge of network.edges) {
            expect(ids.has(edge.from) && ids.has(edge.to)).toBe(true)
        }

        // Recomputed independently: injected classes that are no service key (a DECLARED type, e.g.
        // AppConfig while the key is the app's own config class).
        const keys = new Set(services().map((service) => service.cls))
        const dangling = services()
            .flatMap((service) => service.definition?.injects ?? [])
            .flatMap((inject) => inject.classes)
            .filter((cls) => !keys.has(cls)).length

        expect(network.undrawn).toBe(dangling)
        expect(dangling).toBeGreaterThan(0)
    })

    it('dashes and lightens exactly the Lazy injections', () => {
        const network = kontainerNetwork(services(), 'full')

        // Counted from the data, not the network: each drawable injection, by its provision type.
        const keys = new Set(services().map((service) => service.cls))
        const drawable = (lazy: boolean) =>
            services()
                .filter((service) => service.cls !== null)
                .flatMap((service) => service.definition?.injects ?? [])
                .filter((inject) => (inject.provisionType === 'Lazy') === lazy)
                .flatMap((inject) => inject.classes)
                .filter((cls) => keys.has(cls)).length

        expect(drawable(true)).toBeGreaterThan(0)
        expect(drawable(false)).toBeGreaterThan(0)
        expect(network.edges.filter((edge) => edge.dashes)).toHaveLength(drawable(true))
        expect(network.edges.filter((edge) => !edge.dashes)).toHaveLength(drawable(false))
        for (const edge of network.edges) expect(edge.value).toBe(edge.dashes ? 1 : 2)
    })

    it('draws NO injection edge out of a DynamicOverride, in either graph -- its blueprint injects did not run', () => {
        for (const kind of ['instances', 'full'] as const) {
            const joined = joinKontainerGraph(slimSlice(), graphData(), kind === 'full')
            const network = kontainerNetwork(joined, kind)
            const overrideIds = network.nodes.filter((node) => node.label.endsWith('\nDynamicOverride')).map((node) => node.id)

            expect(overrideIds, kind).toHaveLength(2)
            expect(network.edges.filter((edge) => overrideIds.includes(edge.from)), kind).toEqual([])
        }
    })

    it('colours by the RECORD\'s type, so a DynamicOverride shows as one', () => {
        const joined = joinKontainerGraph(slimSlice(), graphData(), false)
        const override = joined.find((service) => service.type === 'DynamicOverride')!
        const inGraph = graphData().services.find((service) => service.cls === override.cls)!

        expect(inGraph.type).toBe('Dynamic')

        const node = kontainerNetwork(joined, 'instances').nodes.find((it) => it.label.endsWith('\nDynamicOverride'))
        expect(node?.color).toBe('#DC143C')
    })
})

describe('the database graph', () => {
    it('is NOT fetched until asked for, then drawn: one node per repository', async () => {
        const { graphs, requests } = recordedGraphs()
        const host = await render(VaultTab, sliceOf(recorded.vault(), 'vault'), graphs)

        expect(requests).toEqual([])

        await click(host, 'Show Database Graph')

        expect(requests).toEqual([DATABASE_GRAPH_PATH])
        expect(drawn.networks[0].data.nodes).toHaveLength(recorded.databaseGraph().data.repos.length)
        expect(host.textContent).toContain("the server's schema as fetched at")
    })

    it('"Database Graph Data" shows the raw model, from the same single fetch', async () => {
        const { graphs, requests } = recordedGraphs()
        const host = await render(VaultTab, sliceOf(recorded.vault(), 'vault'), graphs)

        await click(host, 'Show Database Graph')

        const details = host.querySelector('details')!
        details.open = true
        details.dispatchEvent(new Event('toggle'))
        await new Promise((resolve) => setTimeout(resolve, 0))

        expect(requests).toEqual([DATABASE_GRAPH_PATH])
        expect(details.textContent).toContain('repos')
    })

    it('a 401 leaves every query card rendering', async () => {
        const { graphs } = recordedGraphs({ [DATABASE_GRAPH_PATH]: { status: 401, body: recorded.refusal() } })
        const slice = sliceOf(recorded.vault(), 'vault')
        const host = await render(VaultTab, slice, graphs)

        await click(host, 'Show Database Graph')

        expect(host.querySelector('.fk-notice--error')?.textContent).toContain('401')
        expect(host.querySelectorAll('.fk-query')).toHaveLength(slice.entries.length)
    })

    it('draws the cross-backend reference, from the referenced repository to the referencing one', () => {
        const graph = readDatabaseGraph(recorded.databaseGraph().data)!
        const network = databaseNetwork(graph)

        const id = (name: string) => network.nodes.find((node) => node.label.startsWith(`${name}\n`))!.id
        const edge = network.edges.find((it) => it.from === id('insights_graph_e2e_authors') && it.to === id('insights_graph_e2e_books'))

        expect(edge?.label).toBe('1')
        expect(edge?.dashes).toBe(false)
    })
})
