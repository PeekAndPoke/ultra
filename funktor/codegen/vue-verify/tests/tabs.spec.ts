/**
 * One component test per collector tab, each against a REAL recorded slice.
 *
 * `record-slim.json` is the request `InsightsRecordingSpec` records (lifecycle-hooks, as a superuser, on
 * the recording host); `record-vault.json` adds queries on both backends; `record-fat.json` is a record
 * written before the kontainer slice was slimmed. Expectations are read from the same data where they can
 * be, so the test checks the rendering rather than restating the fixture.
 */
import { afterEach, describe, expect, it, vi } from 'vitest'
import AppConfigTab from '../src/funktorsdk/insights/AppConfigTab.vue'
import KontainerTab from '../src/funktorsdk/insights/KontainerTab.vue'
import LogTab from '../src/funktorsdk/insights/LogTab.vue'
import RequestTab from '../src/funktorsdk/insights/RequestTab.vue'
import ResponseTab from '../src/funktorsdk/insights/ResponseTab.vue'
import RoutingTab from '../src/funktorsdk/insights/RoutingTab.vue'
import RuntimeTab from '../src/funktorsdk/insights/RuntimeTab.vue'
import TemplateTab from '../src/funktorsdk/insights/TemplateTab.vue'
import UserTab from '../src/funktorsdk/insights/UserTab.vue'
import VaultTab from '../src/funktorsdk/insights/VaultTab.vue'
import { FK_REDACTED } from '../src/funktorsdk/ui/types.ts'
import { mount, recorded, sliceOf } from './support.ts'
import type { Mounted } from './support.ts'

// The graphs are not this file's subject; keep the canvas library out of it.
vi.mock('vis-network/standalone', () => ({ Network: class {} }))

let mounted: Mounted | null = null

afterEach(() => {
    mounted?.unmount()
    mounted = null
})

async function render(component: object, data: unknown): Promise<HTMLElement> {
    mounted = await mount(component, { data })
    return mounted.host
}

/** The text of the `<dd>` next to the `<dt>` reading [key] in a FactList. */
function fact(host: HTMLElement, key: string): string | undefined {
    const term = [...host.querySelectorAll('dt')].find((it) => it.textContent?.trim() === key)
    return term?.nextElementSibling?.textContent?.trim()
}

function rawTreeOnly(host: HTMLElement): boolean {
    return host.querySelector('.fk-json') !== null && host.querySelector('.fk-section') === null
}

describe('request', () => {
    it('shows the method, the composed URL and every header, the credential one redacted', async () => {
        const slice = sliceOf(recorded.slim(), 'request')
        const host = await render(RequestTab, slice)

        expect(fact(host, 'Method')).toBe(slice.method.value)
        expect(fact(host, 'URL')).toBe(`${slice.scheme}://${slice.host}:${slice.port}${slice.uri}`)

        for (const name of Object.keys(slice.headers)) expect(host.textContent).toContain(name)
        expect(slice.headers.Authorization).toEqual([FK_REDACTED])
        expect(host.textContent).toContain(FK_REDACTED)
    })
})

describe('response', () => {
    it('shows the status with its reason phrase, and the headers', async () => {
        const slice = sliceOf(recorded.slim(), 'response')
        const host = await render(ResponseTab, slice)

        expect(fact(host, 'Status')).toBe(`${slice.status.value} ${slice.status.description}`)
        for (const [name, values] of Object.entries(slice.headers as Record<string, string[]>)) {
            expect(host.textContent).toContain(name)
            expect(host.textContent).toContain(values[0])
        }
    })
})

describe('user', () => {
    it('shows who made the request, its kind from the discriminator, and superuser', async () => {
        const slice = sliceOf(recorded.slim(), 'user')
        const host = await render(UserTab, slice)

        expect(fact(host, 'User id')).toBe(slice.user.userId)
        expect(fact(host, 'Email')).toBe(slice.user.email)
        expect(fact(host, 'Kind')).toBe(slice.user._type)
        expect(fact(host, 'Superuser')).toBe('yes')
    })
})

describe('routing', () => {
    it('shows the resolve trace verbatim in a pre', async () => {
        const slice = sliceOf(recorded.slim(), 'routing')
        const host = await render(RoutingTab, slice)

        expect(host.querySelector('pre')?.textContent).toBe(slice.trace)
    })
})

describe('template', () => {
    it('treats a null render time as no view rendered, the normal case for an API call', async () => {
        const slice = sliceOf(recorded.slim(), 'template')
        expect(slice.timeNs).toBeNull()

        const host = await render(TemplateTab, slice)

        expect(host.textContent).toContain('No view was rendered')
    })
})

describe('log', () => {
    it('says so when the request logged nothing', async () => {
        const slice = sliceOf(recorded.slim(), 'log')
        expect(slice.entries).toEqual([])

        const host = await render(LogTab, slice)

        expect(host.textContent).toContain('No log entries')
    })
})

describe('runtime', () => {
    it('shows the seven cells and the system properties', async () => {
        const slice = sliceOf(recorded.slim(), 'runtime')
        const host = await render(RuntimeTab, slice)

        const cells = [...host.querySelectorAll('.fk-strip__cell')]
        expect(cells).toHaveLength(7)
        expect(host.textContent).toContain(slice.jvmVersion)
        expect(host.textContent).toContain(`${Math.round(slice.maxMem / (1024 * 1024))} MB`)
        for (const name of Object.keys(slice.systemProperties)) expect(host.textContent).toContain(name)
    })
})

describe('vault', () => {
    it('says no queries ran when profiling recorded none', async () => {
        const host = await render(VaultTab, sliceOf(recorded.slim(), 'vault'))

        expect(host.textContent).toContain('No database queries ran for this request')
    })

    it('shows one card per query with its placeholder text, on BOTH backends', async () => {
        const slice = sliceOf(recorded.vault(), 'vault')
        const host = await render(VaultTab, slice)

        const cards = [...host.querySelectorAll('.fk-query')]
        expect(cards).toHaveLength(slice.entries.length)

        const languages = slice.entries.map((entry: { queryLanguage: string }) => entry.queryLanguage)
        expect(languages).toEqual(expect.arrayContaining(['aql', 'json']))

        // Mongo's text used to be dropped because it inlined the values; it is placeholders now (S1).
        const texts = [...host.querySelectorAll('pre')].map((pre) => pre.textContent)
        for (const entry of slice.entries) expect(texts).toContain(entry.query)
        expect(texts.join('\n')).toContain('"token": "@p1"')
    })
})

describe('kontainer', () => {
    it('a SLIM record says which shape it is, lists only instantiated services and does not show the counts', async () => {
        const slice = sliceOf(recorded.slim(), 'kontainer')
        const host = await render(KontainerTab, slice)

        expect(fact(host, 'Record shape')).toContain('slim')
        expect(fact(host, 'Kontainers (young / old / total)')).toBe('not measured')
        expect(fact(host, 'Graph fingerprint')).toBe(slice.graphFingerprint)
        expect(host.querySelectorAll('.fk-services tbody tr')).toHaveLength(slice.services.length)
        expect(host.textContent).toContain('Injects and definitions are not part of this record')
    })

    it('tells process-wide singletons from this request\'s own by TYPE, not by creation time', async () => {
        const slice = sliceOf(recorded.slim(), 'kontainer')
        const host = await render(KontainerTab, slice)

        const singletons = slice.services.filter((service: { type: string }) => service.type === 'Singleton').length
        expect(singletons).toBeGreaterThan(0)
        expect(singletons).toBeLessThan(slice.services.length)

        const marks = [...host.querySelectorAll('.fk-services tbody td:nth-child(2) .fk-muted')].map((it) => it.textContent?.trim())
        expect(marks.filter((it) => it === 'process-wide')).toHaveLength(singletons)
        expect(marks.filter((it) => it === "this request's")).toHaveLength(slice.services.length - singletons)
        expect(fact(host, 'Services')).toBe(
            `${slice.services.length} instantiated: ${slice.services.length - singletons} this request's own, ${singletons} process-wide singletons`,
        )
    })

    it('an OLD (fat) record still renders: shape said, definitions and code locations inline', async () => {
        const slice = sliceOf(recorded.fat(), 'kontainer')
        const host = await render(KontainerTab, slice)

        expect(fact(host, 'Record shape')).toContain('full')
        expect(host.querySelectorAll('.fk-services tbody tr')).toHaveLength(slice.info.services.length)

        for (const service of slice.info.services) {
            expect(host.textContent).toContain(service.definition.codeLocation.location)
        }
    })

    it('a kontainer slice of neither shape falls back to the raw tree', async () => {
        const host = await render(KontainerTab, { numOld: 0, numTotal: 0, services: [] })

        expect(rawTreeOnly(host)).toBe(true)
    })
})

describe('app-config', () => {
    it('shows both trees with the secrets caveat, and the signing key arrives redacted', async () => {
        const slice = sliceOf(recorded.slim(), 'app-config')
        const host = await render(AppConfigTab, slice)

        expect(host.textContent).toContain('a secret held in a plain')
        expect(host.querySelectorAll('.fk-json').length).toBeGreaterThanOrEqual(2)
        expect(JSON.stringify(slice.config)).toContain(FK_REDACTED)
    })
})
