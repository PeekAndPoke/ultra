/**
 * The detail page in its LOADED state, through the real generated client: the overview (D5, D6), the
 * tab shell over a real record, and where the tabs get their graph source.
 */
import { afterEach, describe, expect, it, vi } from 'vitest'
import InsightsDetailPage from '../src/funktorsdk/insights/InsightsDetailPage.vue'
import { clientOver, click, KONTAINER_GRAPH_PATH, mount, recorded, recordedGraphs, recordingTransport, sliceOf } from './support.ts'
import type { Mounted } from './support.ts'

vi.mock('vis-network/standalone', () => ({ Network: class { destroy(): void {} } }))

let mounted: Mounted | null = null

afterEach(() => {
    mounted?.unmount()
    mounted = null
})

type RecordResponse = ReturnType<typeof recorded.slim>

function recordPath(response: RecordResponse): string {
    return `/_/funktor/insights/records/${response.data.ref.bucket}/${response.data.ref.file}`
}

async function open(response: RecordResponse, graphs?: Parameters<typeof mount>[2]) {
    const { transport, requests } = recordingTransport({
        [recordPath(response)]: { status: 200, body: response },
        [KONTAINER_GRAPH_PATH]: { status: 200, body: recorded.kontainerGraph() },
    })

    mounted = await mount(
        InsightsDetailPage,
        { client: clientOver(transport), bucket: response.data.ref.bucket, file: response.data.ref.file },
        graphs,
    )

    return { host: mounted.host, requests }
}

function fact(host: HTMLElement, key: string): string | undefined {
    const term = [...host.querySelectorAll('dt')].find((it) => it.textContent?.trim() === key)
    return term?.nextElementSibling?.textContent?.trim()
}

describe('the detail page, loaded', () => {
    it('shows the headline, with the status reason phrase from the response slice (D6)', async () => {
        const response = recorded.slim()
        const { host } = await open(response)

        expect(fact(host, 'Request')).toBe(`${response.data.method} ${response.data.path}`)
        expect(fact(host, 'Status')).toBe(`${response.data.status} ${sliceOf(response, 'response').status.description}`)
    })

    it('offers a tab per collector, and every one of them renders', async () => {
        const response = recorded.slim()
        const { host } = await open(response)

        const tabs = [...host.querySelectorAll('.fk-tabs__tab')] as HTMLButtonElement[]
        expect(tabs).toHaveLength(response.data.collectors.length)

        for (const tab of tabs) {
            tab.click()
            await new Promise((resolve) => setTimeout(resolve, 0))
            expect(host.querySelector('.fk-tabs__panel')?.textContent?.length, tab.textContent ?? '').toBeGreaterThan(0)
        }
    })

    it('says file descriptors were not measured in the Overview strip too, not only in the tab (D5)', async () => {
        const response = recorded.slim()
        const runtime = sliceOf(response, 'runtime')
        runtime.openFileDescriptors = 0
        runtime.maxFileDescriptors = 0

        const { host } = await open(response)

        // The first tab is `user`, so the runtime TAB is not rendered: this can only be the Overview's strip.
        const overview = [...host.querySelectorAll('.fk-section')].find(
            (section) => section.querySelector('.fk-section__title')?.textContent === 'Runtime',
        )
        expect(host.querySelector('.fk-tabs__tab--active')?.textContent?.trim()).toBe('User')
        expect(overview?.querySelector('.fk-strip')?.textContent).toContain('not measured on this platform')
    })

    it('the tabs use the graph source an ancestor provides, so a page fetches each graph once', async () => {
        const page = recordedGraphs()
        const { host, requests } = await open(recorded.slim(), page.graphs)

        ;([...host.querySelectorAll('.fk-tabs__tab')] as HTMLButtonElement[]).find((tab) => tab.textContent?.trim() === 'Kontainer')!.click()
        await new Promise((resolve) => setTimeout(resolve, 0))
        await click(host, 'Show instances Graph')

        expect(page.requests).toEqual([KONTAINER_GRAPH_PATH])
        expect(requests).not.toContain(KONTAINER_GRAPH_PATH)
    })

    it('mounted on its own, the page provides a source over its own client', async () => {
        const { host, requests } = await open(recorded.slim())

        ;([...host.querySelectorAll('.fk-tabs__tab')] as HTMLButtonElement[]).find((tab) => tab.textContent?.trim() === 'Kontainer')!.click()
        await new Promise((resolve) => setTimeout(resolve, 0))
        await click(host, 'Show instances Graph')

        expect(requests.filter((path) => path === KONTAINER_GRAPH_PATH)).toHaveLength(1)
    })
})
