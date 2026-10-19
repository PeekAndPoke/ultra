/**
 * Mounting and data helpers for the component tests.
 *
 * Every record and graph comes from `fixtures/`, which `fixtures/refresh.mjs` copies out of what the
 * funktor/all e2e specs really served. Requests go through the REAL generated client and its zod
 * validation; only the transport underneath is replaced.
 */
import { createApp, h, nextTick } from 'vue'
import type { Component } from 'vue'
import { FunktorInsightsClient } from '../src/funktorsdk/api/funktorInsightsClient.ts'
import { createGraphSource, provideInsightsGraphs } from '../src/funktorsdk/insights/graphs.ts'
import type { InsightsGraphSource } from '../src/funktorsdk/insights/graphs.ts'
import { sdkConfig } from '../src/funktorsdk/runtime/client.ts'
import type { HttpRequest, HttpResponse, HttpTransport } from '../src/funktorsdk/runtime/http.ts'
import fat from '../fixtures/record-fat.json'
import databaseGraph from '../fixtures/graph-database.json'
import kontainerGraph from '../fixtures/graph-kontainer.json'
import refusal from '../fixtures/refusal-graph.json'
import slim from '../fixtures/record-slim.json'
import vault from '../fixtures/record-vault.json'

/** The payload of the `v-html` guard: harmless as text, an executed handler as markup. */
export const PAYLOAD = '<img src=x onerror=alert(1)>'

export const KONTAINER_GRAPH_PATH = '/_/funktor/insights/graphs/kontainer'
export const DATABASE_GRAPH_PATH = '/_/funktor/insights/graphs/database'

/** The recorded API responses, deep-copied so a test may change one without touching the next. */
export const recorded = {
    slim: () => structuredClone(slim),
    fat: () => structuredClone(fat),
    vault: () => structuredClone(vault),
    kontainerGraph: () => structuredClone(kontainerGraph),
    databaseGraph: () => structuredClone(databaseGraph),
    refusal: () => structuredClone(refusal),
}

/** The `data` of one collector slice in a recorded record response. */
export function sliceOf(response: { data: { collectors: { key: string; data: unknown }[] } }, key: string): any {
    const slice = response.data.collectors.find((it) => it.key === key)
    if (slice === undefined) throw new Error(`the recorded record has no '${key}' slice`)
    return slice.data
}

export interface Answer {
    status: number
    /** An object is sent as JSON; a string as is, which is how a non-envelope error page arrives. */
    body: unknown
}

/** A transport answering by path, recording every path it was asked for. Unknown paths get an HTML 404. */
export function recordingTransport(answers: Record<string, Answer>): { transport: HttpTransport; requests: string[] } {
    const requests: string[] = []

    const transport: HttpTransport = {
        async send(request: HttpRequest): Promise<HttpResponse> {
            // Decoded: a record's file name travels percent-encoded (`:` in its timestamp).
            const path = decodeURIComponent(new URL(request.url).pathname)
            requests.push(path)

            const answer = Object.hasOwn(answers, path) ? answers[path] : undefined
            if (answer === undefined) return { status: 404, statusText: 'Not Found', body: '<html><body>Not Found</body></html>' }

            return {
                status: answer.status,
                statusText: '',
                body: typeof answer.body === 'string' ? answer.body : JSON.stringify(answer.body),
            }
        },
    }

    return { transport, requests }
}

export function clientOver(transport: HttpTransport): FunktorInsightsClient {
    return new FunktorInsightsClient(sdkConfig('http://insights.test', transport))
}

/** A graph source over the real client, answering both graph endpoints from the recorded responses. */
export function recordedGraphs(overrides: Record<string, Answer> = {}): { graphs: InsightsGraphSource; requests: string[] } {
    const { transport, requests } = recordingTransport({
        [KONTAINER_GRAPH_PATH]: { status: 200, body: recorded.kontainerGraph() },
        [DATABASE_GRAPH_PATH]: { status: 200, body: recorded.databaseGraph() },
        ...overrides,
    })

    const client = clientOver(transport)
    return { graphs: createGraphSource(() => client), requests }
}

/** A graph source whose kontainer endpoint answers [bodies] in turn, the last one from then on: a redeploy. */
export function sequencedKontainerGraphs(bodies: unknown[]): { graphs: InsightsGraphSource; requests: string[] } {
    const requests: string[] = []

    const transport: HttpTransport = {
        async send(request: HttpRequest): Promise<HttpResponse> {
            const path = decodeURIComponent(new URL(request.url).pathname)
            if (path !== KONTAINER_GRAPH_PATH) throw new Error(`unexpected request: ${path}`)

            const body = bodies[Math.min(requests.length, bodies.length - 1)]
            requests.push(path)
            return { status: 200, statusText: '', body: JSON.stringify(body) }
        },
    }

    const client = clientOver(transport)
    return { graphs: createGraphSource(() => client), requests }
}

export interface Mounted {
    host: HTMLElement
    unmount(): void
}

/** Mounts [component] in the document, under a provided graph source when one is given. */
export async function mount(component: Component, props: Record<string, unknown>, graphs?: InsightsGraphSource): Promise<Mounted> {
    const host = document.createElement('div')
    document.body.appendChild(host)

    const app = createApp({
        setup() {
            if (graphs !== undefined) provideInsightsGraphs(graphs)
            return () => h(component, props)
        },
    })

    app.config.errorHandler = (error) => {
        throw error
    }
    app.mount(host)
    await settle()

    return {
        host,
        unmount() {
            app.unmount()
            host.remove()
        },
    }
}

/** Lets pending promises, dynamic imports and re-renders finish. */
export async function settle(): Promise<void> {
    for (let i = 0; i < 20; i++) {
        await new Promise((resolve) => setTimeout(resolve, 0))
        await nextTick()
    }
}

export function buttonNamed(host: HTMLElement, text: string): HTMLButtonElement {
    const button = [...host.querySelectorAll('button')].find((it) => it.textContent?.trim() === text)
    if (button === undefined) throw new Error(`no button '${text}' in: ${host.textContent?.slice(0, 300)}`)
    return button
}

export async function click(host: HTMLElement, text: string): Promise<void> {
    buttonNamed(host, text).click()
    await settle()
}
