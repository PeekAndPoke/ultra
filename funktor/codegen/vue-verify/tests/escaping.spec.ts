/**
 * The `v-html` guard, as a test: a slice containing `<img src=x onerror=alert(1)>` renders as TEXT.
 *
 * Every string in a slice was recorded verbatim from a request, most of them unauthenticated. Each case
 * plants the payload in a REAL recorded slice at a position an attacker reaches, and asserts two things:
 * no `<img>` reached the DOM, and the payload IS on the page as text. The second one matters -- a tab
 * that silently dropped the field would pass the first by showing nothing.
 *
 * Mutation-checked: switching `PreBlock`'s `{{ text }}` to `v-html` fails the routing, log and vault cases.
 */
import { afterEach, describe, expect, it, vi } from 'vitest'
import AppConfigTab from '../src/funktorsdk/insights/AppConfigTab.vue'
import KontainerTab from '../src/funktorsdk/insights/KontainerTab.vue'
import LogTab from '../src/funktorsdk/insights/LogTab.vue'
import RequestTab from '../src/funktorsdk/insights/RequestTab.vue'
import ResponseTab from '../src/funktorsdk/insights/ResponseTab.vue'
import RoutingTab from '../src/funktorsdk/insights/RoutingTab.vue'
import RuntimeTab from '../src/funktorsdk/insights/RuntimeTab.vue'
import UserTab from '../src/funktorsdk/insights/UserTab.vue'
import VaultTab from '../src/funktorsdk/insights/VaultTab.vue'
import { mount, PAYLOAD, recorded, sliceOf } from './support.ts'
import type { Mounted } from './support.ts'

vi.mock('vis-network/standalone', () => ({ Network: class {} }))

let mounted: Mounted | null = null

afterEach(() => {
    mounted?.unmount()
    mounted = null
})

/**
 * Renders [component] over [data] and checks the payload arrived as text, never as an element, EXACTLY
 * [times] times: a tab dropping one planted field must fail, which a lower bound lets through.
 */
async function expectEscaped(component: object, data: unknown, times = 1): Promise<void> {
    mounted = await mount(component, { data })
    const host = mounted.host

    expect(host.querySelector('img'), 'the payload reached the DOM as markup').toBeNull()

    const shown = host.textContent?.split(PAYLOAD).length ?? 1
    expect(shown - 1, 'the payload is not shown once per planted position -- a field was dropped or doubled').toBe(times)
}

describe('a recorded <img onerror> renders as text', () => {
    it('request: header name, header value, query parameter and path', async () => {
        const slice = sliceOf(recorded.slim(), 'request')
        slice.headers['User-Agent'] = [PAYLOAD]
        slice.headers[PAYLOAD] = ['x']
        slice.queryParams = { q: [PAYLOAD] }
        slice.uri = `/search/${PAYLOAD}`

        // 5, not 4: the raw tree shows `uri`, a top-level string, while it keeps the maps collapsed.
        await expectEscaped(RequestTab, slice, 5)
    })

    it('response: a header value', async () => {
        const slice = sliceOf(recorded.slim(), 'response')
        slice.headers['Set-Cookie'] = [PAYLOAD]

        await expectEscaped(ResponseTab, slice)
    })

    it('user: email and description', async () => {
        const slice = sliceOf(recorded.slim(), 'user')
        slice.user.email = PAYLOAD
        slice.user.desc = PAYLOAD

        await expectEscaped(UserTab, slice, 2)
    })

    it('routing: the pre-formatted trace -- the "render it as markup" shortcut', async () => {
        const slice = sliceOf(recorded.slim(), 'routing')
        slice.trace = `${slice.trace}\n${PAYLOAD}`

        await expectEscaped(RoutingTab, slice)
    })

    it('log: a formatted log line', async () => {
        const slice = sliceOf(recorded.slim(), 'log')
        slice.entries = [{ level: 'ERROR', text: `failed for ${PAYLOAD}` }]

        await expectEscaped(LogTab, slice)
    })

    it('runtime: a system property name and value', async () => {
        const slice = sliceOf(recorded.slim(), 'runtime')
        slice.systemProperties[PAYLOAD] = PAYLOAD

        await expectEscaped(RuntimeTab, slice, 2)
    })

    it('vault: the query text and the connection', async () => {
        const slice = sliceOf(recorded.vault(), 'vault')
        slice.entries[0].query = `${slice.entries[0].query} ${PAYLOAD}`
        slice.entries[0].connection = PAYLOAD

        await expectEscaped(VaultTab, slice, 2)
    })

    it('kontainer: a class name and a code location', async () => {
        const slice = sliceOf(recorded.fat(), 'kontainer')
        slice.info.services[0].cls = { fqn: `io.example.${PAYLOAD}` }
        slice.info.services[0].definition.codeLocation = { location: PAYLOAD }

        await expectEscaped(KontainerTab, slice, 2)
    })

    it('app-config: a config value', async () => {
        const slice = sliceOf(recorded.slim(), 'app-config')
        slice.info = { ...slice.info, note: PAYLOAD }

        await expectEscaped(AppConfigTab, slice)
    })
})
