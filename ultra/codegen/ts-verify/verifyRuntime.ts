/**
 * Behavioural checks for the HAND-WRITTEN runtime (`runtime/http.ts`, `apiResponse.ts`, `sse.ts`).
 *
 * The generated code is verified against real Slumber output by `verify.ts`. The runtime cannot be
 * checked that way — it is not derived from anything — so it is checked the ordinary way instead:
 * against the behaviour its Kotlin counterparts are documented to have. `tsc --noEmit` covers these
 * files too, because the fixture generator copies them into `generated/`.
 *
 * The imports are STATIC on purpose. A dynamic import would leave the call sites unchecked, and half
 * the value here is proving the runtime's declared types are usable from generated code.
 */
import { apiResponse, isSuccess } from './generated/runtime/apiResponse.ts'
import { buildUrl, fetchTransport } from './generated/runtime/http.ts'
import type { HttpRequest } from './generated/runtime/http.ts'
import { SseParser } from './generated/runtime/sse.ts'
import { FxSpeaker } from './generated/talk.ts'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

type Report = (ok: boolean, label: string, detail?: string) => void

/** Runs [fn], returning whether it threw. */
function rejected(fn: () => unknown): boolean {
    try {
        fn()
        return false
    } catch {
        return true
    }
}

function equal(actual: unknown, expected: unknown): boolean {
    return JSON.stringify(actual) === JSON.stringify(expected)
}

function checkApiResponse(report: Report, generatedDir: string): void {
    const schema = apiResponse(FxSpeaker)

    const sample: unknown = JSON.parse(readFileSync(join(generatedDir, 'apiResponse.sample.json'), 'utf8'))

    try {
        const parsed = schema.parse(sample)

        report(equal(parsed.data, { name: 'Ada', bio: null }), 'apiResponse: unwraps the real payload')
        report(parsed.status.value === 200, 'apiResponse: reads the status object')
        report(parsed.messages?.length === 1, 'apiResponse: reads the messages')
        report(isSuccess(parsed), 'apiResponse: isSuccess is true for 200')
    } catch (e) {
        report(false, 'apiResponse: parses a real envelope', (e as Error).message.split('\n')[0])
    }

    // The payload must be validated, not waved through — an envelope schema that ignored its argument
    // would pass every test above.
    report(
        rejected(() => schema.parse({ status: { value: 200, description: 'OK' }, data: { name: 42 } })),
        'apiResponse: rejects a payload that does not match the data schema',
    )

    report(
        rejected(() => schema.parse({ data: null })),
        'apiResponse: rejects an envelope without a status',
    )

    // `messages` and `insights` carry Kotlin defaults, so a server may omit them entirely.
    report(
        !rejected(() => schema.parse({ status: { value: 204, description: 'No Content' }, data: null })),
        'apiResponse: accepts an envelope without messages or insights',
    )

    report(
        !isSuccess({ status: { value: 404, description: 'Not Found' }, data: null }),
        'apiResponse: isSuccess is false for 404',
    )
}

function checkBuildUrl(report: Report): void {
    report(
        buildUrl('http://x/', '/api/talks/{id}', { id: 'a b' }) === 'http://x/api/talks/a%20b',
        'buildUrl: fills and encodes path params, and joins without a double slash',
    )

    report(
        buildUrl('http://x', '/api/talks', {}, { search: 'a&b', page: 2 }) ===
            'http://x/api/talks?search=a%26b&page=2',
        'buildUrl: appends and encodes query params',
    )

    // Mirrors TypedRouteRenderer, which drops null and empty values rather than sending them empty.
    report(
        buildUrl('http://x', '/api/talks', {}, { search: '', page: null, epp: undefined, ok: false }) ===
            'http://x/api/talks?ok=false',
        'buildUrl: omits null, undefined and empty query params but keeps false',
    )

    report(
        rejected(() => buildUrl('http://x', '/api/talks/{id}')),
        'buildUrl: throws on an unfilled path placeholder',
    )
}

async function checkTransport(report: Report): Promise<void> {
    let seen: RequestInit | undefined

    const fake: typeof fetch = (_url, init) => {
        seen = init
        return Promise.resolve(new Response('{"ok":true}', { status: 404, statusText: 'Not Found' }))
    }

    const request: HttpRequest = {
        method: 'POST',
        url: 'http://x/api',
        headers: { 'Content-Type': 'application/json' },
        body: '{}',
    }

    const response = await fetchTransport(fake).send(request)

    // The whole reason fetch is the default: a 404 comes back as data, exactly as the Kotlin client
    // surfaces non-2xx as a decoded envelope rather than throwing.
    report(response.status === 404, 'fetchTransport: returns non-2xx instead of throwing')
    report(response.body === '{"ok":true}', 'fetchTransport: reads the body of a non-2xx response')
    report(seen?.method === 'POST' && seen?.body === '{}', 'fetchTransport: passes method and body through')
}

function checkSseParser(report: Report): void {
    const simple = new SseParser().push('data: hello\n\n')

    report(
        equal(simple, [{ event: 'message', data: 'hello' }]),
        'sse: dispatches a frame and defaults the event type to "message"',
    )

    // Frames arriving split across reads is the normal case, not an edge case.
    const split = new SseParser()

    report(
        split.push('event: tick\ndata: a').length === 0 &&
            equal(split.push('\ndata: b\n\n'), [{ event: 'tick', data: 'a\nb' }]),
        'sse: joins a frame split across chunks and joins multiple data lines',
    )

    // A CRLF split down the middle must stay ONE terminator. Read as two, the LF starting the next
    // chunk looks like a blank line and cuts the frame in half — so this needs a frame that continues
    // past the split. Ending it right there instead would pass either way: the spurious blank line
    // dispatches nothing, because a frame with no data is dropped.
    const crlf = new SseParser()

    report(
        crlf.push('data: a\r').length === 0 &&
            equal(crlf.push('\ndata: b\n\n'), [{ event: 'message', data: 'a\nb' }]),
        'sse: treats a CRLF split across chunks as one terminator',
    )

    report(
        equal(new SseParser().push(': keep-alive\n\ndata: real\n\n'), [{ event: 'message', data: 'real' }]),
        'sse: ignores comments and does not dispatch a frame with no data',
    )

    report(
        equal(new SseParser().push('id: 7\nretry: 500\ndata: x\n\ndata: y\n\n'), [
            { event: 'message', data: 'x', id: '7', retry: 500 },
            { event: 'message', data: 'y', id: '7', retry: 500 },
        ]),
        'sse: carries id and retry forward to later events, per spec',
    )

    report(
        equal(new SseParser().push('event: tick\ndata: a\n\ndata: b\n\n'), [
            { event: 'tick', data: 'a' },
            { event: 'message', data: 'b' },
        ]),
        'sse: resets the event type after each frame',
    )

    report(
        equal(new SseParser().push('data:no-space\n\n'), [{ event: 'message', data: 'no-space' }]),
        'sse: strips at most one space after the colon',
    )
}

/**
 * Runs every runtime check, reporting through [report].
 *
 * Each group is isolated: a check that THROWS is reported as a failure and the rest still run. Without
 * that, a single regression aborts the process and every later check silently goes unrun — which is
 * exactly what happened while mutation-testing this file.
 */
export async function verifyRuntime(report: Report, generatedDir: string): Promise<void> {
    const groups: Array<[string, (report: Report) => void | Promise<void>]> = [
        ['apiResponse', (r) => checkApiResponse(r, generatedDir)],
        ['buildUrl', checkBuildUrl],
        ['transport', checkTransport],
        ['sse', checkSseParser],
    ]

    for (const [name, check] of groups) {
        try {
            await check(report)
        } catch (e) {
            report(false, `${name}: threw`, (e as Error).message.split('\n')[0])
        }
    }
}
