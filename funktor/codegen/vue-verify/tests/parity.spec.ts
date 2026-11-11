/**
 * Parity with the old insights GUI (ULTRA-00190): highlighted code, tab icons, a sortable kontainer table with
 * full class names, and the git version and environment on both pages.
 */
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import CodeBlock, { MAX_HIGHLIGHT_CHARS } from '../src/funktorsdk/insights/CodeBlock.vue'
import InsightsDetailPage from '../src/funktorsdk/insights/InsightsDetailPage.vue'
import InsightsListPage from '../src/funktorsdk/insights/InsightsListPage.vue'
import KontainerTab from '../src/funktorsdk/insights/KontainerTab.vue'
import VaultTab from '../src/funktorsdk/insights/VaultTab.vue'
import { buildFacts, readBuildInfo, serverBuildInfo } from '../src/funktorsdk/insights/slices.ts'
import {
    clientOver,
    highlighted,
    KONTAINER_GRAPH_PATH,
    mount,
    PAYLOAD,
    recorded,
    RECORDS_PATH,
    SERVER_INFO_PATH,
    settle,
    sliceOf,
    recordingTransport,
} from './support.ts'
import type { Answer, Mounted } from './support.ts'

vi.mock('vis-network/standalone', () => ({ Network: class { destroy(): void {} } }))

let mounted: Mounted | null = null

afterEach(() => {
    mounted?.unmount()
    mounted = null
})

async function show(component: object, props: Record<string, unknown>): Promise<HTMLElement> {
    mounted = await mount(component, props)
    return mounted.host
}

function fact(host: HTMLElement, key: string): string | undefined {
    const term = [...host.querySelectorAll('dt')].find((it) => it.textContent?.trim() === key)
    return term?.nextElementSibling?.textContent?.trim()
}

function tokens(host: HTMLElement, type: string): string[] {
    return [...host.querySelectorAll(`.token.${type}`)].map((it) => it.textContent ?? '')
}

/*  CodeBlock  ----------------------------------------------------------------------------------------------  */

/** An element of the HOST page that Prism would highlight on load if it were not told to stay manual. */
let hostCode: HTMLElement

beforeAll(() => {
    // JavaScript, a grammar in Prism's own bundle: auto mode WOULD change this element
    hostCode = document.createElement('code')
    hostCode.className = 'language-javascript'
    hostCode.textContent = 'var host = 1'
    document.body.appendChild(hostCode)
})

describe('CodeBlock', () => {
    it('highlights AQL: keywords and the bind placeholders, the text unchanged', async () => {
        const text = 'FOR d IN `sessions` FILTER d.`token` == @v_1 LIMIT 0, 1 RETURN d'
        const host = await show(CodeBlock, { text, language: 'aql' })
        await highlighted()

        expect(tokens(host, 'keyword')).toEqual(['FOR', 'IN', 'FILTER', 'LIMIT', 'RETURN'])
        expect(tokens(host, 'variable')).toEqual(['@v_1'])
        expect(host.querySelector('pre')?.textContent).toBe(text)
    })

    it('highlights JSON: properties, strings, numbers', async () => {
        const text = '{\n  "token": "@p1:string",\n  "limit": 1\n}'
        const host = await show(CodeBlock, { text, language: 'json' })
        await highlighted()

        expect(tokens(host, 'property')).toEqual(['"token"', '"limit"'])
        expect(tokens(host, 'string')).toEqual(['"@p1:string"'])
        expect(tokens(host, 'number')).toEqual(['1'])
        expect(host.querySelector('pre')?.textContent).toBe(text)
    })

    it('renders markup in the text as text, highlighted or not: no v-html', async () => {
        for (const language of ['json', 'aql', null]) {
            const text = language === 'json' ? `{"x": "${PAYLOAD}"}` : `RETURN "${PAYLOAD}"`
            const host = await show(CodeBlock, { text, language })
            await highlighted()

            expect(host.querySelector('img'), String(language)).toBeNull()
            expect(host.querySelector('pre')?.textContent).toBe(text)

            mounted?.unmount()
            mounted = null
        }
    })

    it('an unknown language is plain text', async () => {
        const host = await show(CodeBlock, { text: 'Execution plan:\n Id NodeType', language: 'text' })

        expect(host.querySelectorAll('.token')).toHaveLength(0)
        expect(host.querySelector('pre')?.textContent).toBe('Execution plan:\n Id NodeType')
    })

    it('leaves the host page alone: Prism is loaded manual, and leaves no global behind', async () => {
        const host = await show(CodeBlock, { text: '{"a": 1}', language: 'json' })
        await highlighted()
        await new Promise((resolve) => setTimeout(resolve, 50))

        expect(tokens(host, 'property')).toEqual(['"a"'])
        expect(hostCode.innerHTML).toBe('var host = 1')
        expect('Prism' in globalThis).toBe(false)
    })

    it('text longer than the limit is not highlighted: greedy patterns cost quadratic time on crafted text', async () => {
        const text = `{"a": "${'x'.repeat(MAX_HIGHLIGHT_CHARS)}"}`
        const host = await show(CodeBlock, { text, language: 'json' })

        expect(host.querySelectorAll('.token')).toHaveLength(0)
        expect(host.querySelector('pre')?.textContent).toBe(text)
    })
})

describe('the vault tab highlights its queries', () => {
    it('an Arango lookup has its AQL keywords marked', async () => {
        const slice = sliceOf(recorded.vault(), 'vault')
        const host = await show(VaultTab, { data: slice })
        await highlighted()

        const index = slice.entries.findIndex(
            (entry: { queryLanguage: string, query: string }) => entry.queryLanguage === 'aql' && entry.query.includes('token'),
        )
        const card = host.querySelectorAll('.fk-query')[index]

        expect(tokens(card as HTMLElement, 'keyword')).toContain('FILTER')
    })
})

/*  The kontainer table  ---------------------------------------------------------------------------------  */

describe('the kontainer table', () => {
    function column(host: HTMLElement, index: number): string[] {
        return [...host.querySelectorAll('.fk-services tbody tr')].map((row) => row.children[index]?.textContent?.trim() ?? '')
    }

    function header(host: HTMLElement, label: string): HTMLElement {
        const th = [...host.querySelectorAll('.fk-services th')].find((it) => it.textContent?.includes(label))
        if (th === undefined) throw new Error(`no header ${label}`)
        return th as HTMLElement
    }

    it('shows each service by its FULL class name, so the browser can find it', async () => {
        const slice = sliceOf(recorded.slim(), 'kontainer')
        const host = await show(KontainerTab, { data: slice })

        const names = column(host, 0)
        expect(names.length).toBeGreaterThan(1)
        expect(new Set(names)).toEqual(new Set(slice.services.map((service: { cls: string }) => service.cls)))
        expect(names.every((name) => name.includes('.'))).toBe(true)
    })

    it('sorts by a column on click, reverses on the next, and says so in aria-sort', async () => {
        const host = await show(KontainerTab, { data: sliceOf(recorded.slim(), 'kontainer') })
        const byName = (a: string, b: string) => a.localeCompare(b)

        expect(header(host, 'Service').getAttribute('aria-sort')).toBe('none')

        header(host, 'Service').querySelector('button')!.click()
        await settle()
        const ascending = column(host, 0)
        expect(ascending).toEqual([...ascending].sort(byName))
        expect(header(host, 'Service').getAttribute('aria-sort')).toBe('ascending')

        header(host, 'Service').querySelector('button')!.click()
        await settle()
        expect(column(host, 0)).toEqual([...ascending].reverse())
        expect(header(host, 'Service').getAttribute('aria-sort')).toBe('descending')
    })

    it('sorts the type and definition columns by what they show', async () => {
        const host = await show(KontainerTab, { data: sliceOf(recorded.fat(), 'kontainer') })
        const byName = (a: string, b: string) => a.localeCompare(b)

        header(host, 'Type').querySelector('button')!.click()
        await settle()
        const types = [...host.querySelectorAll('.fk-services tbody tr')].map((row) => row.children[1]?.querySelector('.fk-label')?.textContent ?? '')
        expect(types).toEqual([...types].sort(byName))
        expect(new Set(types).size).toBeGreaterThan(1)

        header(host, 'Definition').querySelector('button')!.click()
        await settle()
        // The first line of each cell, without the code location under it
        const definitions = [...host.querySelectorAll('.fk-services tbody tr')].map((row) => {
            const first = row.children[4]?.querySelector(':scope > div')?.cloneNode(true) as HTMLElement | undefined
            first?.querySelectorAll('.fk-muted').forEach((it) => it.remove())
            return first?.textContent?.replace(/\s+/g, ' ').trim() ?? ''
        })
        expect(definitions).toEqual([...definitions].sort(byName))
    })

    it('sorts the instances column by count, not by text', async () => {
        const host = await show(KontainerTab, { data: sliceOf(recorded.fat(), 'kontainer') })

        header(host, 'Instances').querySelector('button')!.click()
        await settle()

        const counts = [...host.querySelectorAll('.fk-services tbody tr')]
            .map((row) => row.children[2]?.querySelectorAll(':scope > div:not(.fk-muted)').length ?? 0)
        expect(counts).toEqual([...counts].sort((a, b) => a - b))
        expect(new Set(counts).size).toBeGreaterThan(1)
        expect(header(host, 'Service').getAttribute('aria-sort')).toBe('none')
    })
})

/*  Tab icons and the record's build  --------------------------------------------------------------------  */

type RecordResponse = ReturnType<typeof recorded.slim>

async function openRecord(response: RecordResponse): Promise<HTMLElement> {
    const path = `/_/funktor/insights/records/${response.data.ref.bucket}/${response.data.ref.file}`
    const { transport } = recordingTransport({
        [path]: { status: 200, body: response },
        [KONTAINER_GRAPH_PATH]: { status: 200, body: recorded.kontainerGraph() },
    })

    return show(InsightsDetailPage, { client: clientOver(transport), bucket: response.data.ref.bucket, file: response.data.ref.file })
}

describe('the detail page', () => {
    it('every known tab has its icon; an app-defined collector has none', async () => {
        const response = recorded.slim()
        ;(response.data.collectors as Array<{ key: string, data: unknown }>).push({ key: 'custom-collector', data: { any: 1 } })

        const host = await openRecord(response)
        const tabs = [...host.querySelectorAll('.fk-tabs__tab')]

        // The Overview, then one per collector
        expect(tabs).toHaveLength(response.data.collectors.length + 1)
        for (const tab of tabs) {
            const custom = tab.textContent?.trim() === 'custom-collector'
            expect(tab.querySelectorAll('svg').length, tab.textContent ?? '').toBe(custom ? 0 : 1)
            if (!custom) expect(tab.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true')
        }
    })

    it("an older record's slice of a removed collector gets no tab", async () => {
        const response = recorded.slim()
        ;(response.data.collectors as Array<{ key: string, data: unknown }>).push({ key: 'template', data: { timeNs: null } })

        const host = await openRecord(response)
        const labels = [...host.querySelectorAll('.fk-tabs__tab')].map((tab) => tab.textContent?.trim())

        expect(labels).not.toContain('template')
        // The Overview, then the collectors without the removed one
        expect(labels).toHaveLength(response.data.collectors.length)
    })

    it("the Overview shows the record's OWN git version, application and environment", async () => {
        const response = recorded.slim()
        const config = sliceOf(response, 'app-config')
        Object.assign(config.info.version, { gitBranch: 'main', gitRev: 'abc1234', gitDesc: 'v1.2.0-3-gabc1234' })

        const host = await openRecord(response)

        expect(fact(host, 'Git')).toBe('main-abc1234 (v1.2.0-3-gabc1234)')
        expect(fact(host, 'Application')).toBe(config.config.ktor.application.id)
        expect(fact(host, 'Environment')).toBe(config.config.ktor.deployment.environment)
    })

    it('a build without git info says n/a, not "n/a-n/a"', async () => {
        const host = await openRecord(recorded.slim())

        expect(sliceOf(recorded.slim(), 'app-config').info.version.gitBranch).toBe('n/a')
        expect(fact(host, 'Git')).toBe('n/a')
    })
})

/*  The list page header  -------------------------------------------------------------------------------  */

function emptyList(): Answer {
    return { status: 200, body: { ...recorded.serverInfo(), data: { items: [], page: 1, epp: 20, fullItemCount: 0 } } }
}

async function openList(server: Answer): Promise<{ host: HTMLElement, requests: string[] }> {
    const { transport, requests } = recordingTransport({ [RECORDS_PATH]: emptyList(), [SERVER_INFO_PATH]: server })
    const host = await show(InsightsListPage, { client: clientOver(transport) })
    return { host, requests }
}

describe('the list page', () => {
    it("shows the RUNNING server's git version, application and environment, fetched once", async () => {
        const info = recorded.serverInfo()
        Object.assign(info.data, { git: 'main-abc1234', gitDesc: 'v1.2.0-3-gabc1234' })

        const { host, requests } = await openList({ status: 200, body: info })

        expect(fact(host, 'Git')).toBe('main-abc1234 (v1.2.0-3-gabc1234)')
        expect(fact(host, 'Application')).toBe(info.data.applicationId)
        expect(fact(host, 'Environment')).toBe(info.data.environment)
        expect(requests.filter((path) => path === SERVER_INFO_PATH)).toHaveLength(1)
    })

    it('without the server info (an older server, a refusal) the header is left out and the list works', async () => {
        const { host } = await openList({ status: 404, body: '<html>Not Found</html>' })

        expect(host.querySelector('.fk-server')).toBeNull()
        expect(host.textContent).toContain('No records')
    })
})

describe('the build readers', () => {
    it('n/a and empty are missing values, so FactList shows its placeholder once', () => {
        const server = recorded.serverInfo().data

        expect(serverBuildInfo(server)).toEqual({
            git: null,
            gitDesc: null,
            applicationId: server.applicationId,
            environment: server.environment,
        })
        expect(readBuildInfo(null)).toBeNull()
        expect(buildFacts({ git: 'main-1', gitDesc: null, applicationId: null, environment: 'qa' })).toEqual([
            { key: 'Git', value: 'main-1' },
            { key: 'Application', value: null },
            { key: 'Environment', value: 'qa' },
        ])
    })
})
