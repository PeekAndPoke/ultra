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
import { ApiError, ApiProtocolError, request, sdkConfig, unwrap } from './generated/runtime/client.ts'
import { buildUrl, fetchTransport } from './generated/runtime/http.ts'
import type { HttpRequest, HttpTransport } from './generated/runtime/http.ts'
import { SseParser, sseStream, stream } from './generated/runtime/sse.ts'
import { ApiAcl } from './generated/runtime/acl.ts'
import { route } from './generated/runtime/route.ts'
import { AuthSession, authTransport, inMemorySession, localStorageSession } from './generated/runtime/auth.ts'
import type { SignedIn } from './generated/runtime/auth.ts'
import { applySignIn, completeSignIn } from './generated/runtime/login.ts'
import { startAutoRefresh } from './generated/runtime/refresh.ts'
import type { SignInResult } from './generated/runtime/login.ts'
// THROUGH THE BARREL on purpose — this is what makes `tsc` compile index.ts, and `export *`
// is only safe if no two emitted modules export the same name. A collision is a compile error
// here rather than a silent hole in a consumer's build.
import { FxDemoClient } from './generated/index.ts'
import { mountAll, navItems, routes } from './generated/mount.ts'
import type { SdkRoute } from './generated/mount.ts'
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

/**
 * Awaits [fn] and returns the thrown error's `name`, or `null` when it resolved.
 *
 * Returns the NAME rather than a boolean so a check cannot pass on the wrong error — an assertion
 * that something merely threw is satisfied by a typo in the test itself.
 */
async function rejectedAsync(fn: () => Promise<unknown>): Promise<string | null> {
    try {
        await fn()
        return null
    } catch (e) {
        return (e as Error).name
    }
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

    // `credentials` — the one thing an auth WRAPPER cannot supply, because it only sees the
    // HttpRequest and `fetch` reads credentials from its own init. Cookie sessions need it.
    report(
        !('credentials' in (seen ?? {})),
        'fetchTransport: omits credentials entirely when unset, leaving fetch its own default',
        String(seen !== undefined && 'credentials' in seen),
    )

    await fetchTransport(fake).send({ ...request, credentials: 'include' })
    report(seen?.credentials === 'include', "fetchTransport: passes credentials through when set")

    await fetchTransport(fake).send({ ...request, credentials: 'omit' })
    report(seen?.credentials === 'omit', 'fetchTransport: and passes the other values too')
}

/**
 * `request` is what every generated endpoint member reduces to, so a bug here is a bug in every
 * endpoint at once. The transport is a stub: this checks what the client DOES with a response, which
 * is the part the generator depends on.
 */
async function checkRequest(report: Report): Promise<void> {
    /** A transport that records the request and answers [status] with [body]. */
    function stub(status: number, body: string): { transport: HttpTransport; seen: () => HttpRequest } {
        let captured: HttpRequest | undefined

        return {
            transport: {
                send: (req) => {
                    captured = req
                    return Promise.resolve({ status, statusText: '', body })
                },
            },
            seen: () => captured!,
        }
    }

    const envelope = (data: string) => `{"status":{"value":200,"description":"OK"},"data":${data}}`

    // 1. The happy path: URL built from pattern + params, payload validated, envelope returned.
    {
        const { transport, seen } = stub(200, envelope('{"name":"Ada","bio":null}'))

        const result = await request(
            { baseUrl: 'http://x', transport },
            'GET',
            '/api/speakers/{id}',
            FxSpeaker,
            { path: { id: 'a b' }, query: { q: 'x', empty: '' } },
        )

        report(seen().url === 'http://x/api/speakers/a%20b?q=x', 'request: builds the URL from path and query')
        report(seen().method === 'GET', 'request: sends the declared method')
        report(equal(result.data, { name: 'Ada', bio: null }), 'request: returns the validated payload')
        report(seen().body === undefined, 'request: sends no body when none was given')
    }

    // 2. A body is JSON-encoded and declares its content type; a bodiless request must NOT declare one.
    //    Both halves are asserted: checking only the with-body half is satisfied by sending
    //    Content-Type unconditionally, which is the bug the "only" in the label is about.
    {
        const withBody = stub(200, envelope('null'))

        await request(
            { baseUrl: 'http://x', transport: withBody.transport },
            'POST',
            '/api/speakers',
            FxSpeaker.nullable(),
            { body: { name: 'Ada', bio: null } },
        )

        report(withBody.seen().body === '{"name":"Ada","bio":null}', 'request: JSON-encodes the body')
        report(
            withBody.seen().headers['Content-Type'] === 'application/json',
            'request: declares Content-Type when there is a body',
        )

        const bodiless = stub(200, envelope('null'))

        await request(
            { baseUrl: 'http://x', transport: bodiless.transport },
            'GET',
            '/api/speakers',
            FxSpeaker.nullable(),
        )

        report(
            bodiless.seen().headers['Content-Type'] === undefined,
            'request: sends no Content-Type when there is no body',
        )
    }

    // 3. THE contract: a non-2xx envelope is returned, never thrown. If this ever regresses, every
    //    generated call site that branches on isSuccess silently becomes unreachable.
    {
        const { transport } = stub(404, '{"status":{"value":404,"description":"Not Found"},"data":null}')

        const result = await request({ baseUrl: 'http://x', transport }, 'GET', '/api/x', FxSpeaker.nullable())

        report(result.status.value === 404, 'request: returns a non-2xx envelope instead of throwing')
        report(!isSuccess(result), 'request: the returned non-2xx envelope reports isSuccess false')
    }

    // 4. A body that is not an envelope must be loud. A proxy's HTML 502 is the common case, and the
    //    silent alternative is a client that returns undefined and blames the caller.
    {
        const { transport } = stub(502, '<html>Bad Gateway</html>')

        const failed = await rejectedAsync(() =>
            request({ baseUrl: 'http://x', transport }, 'GET', '/api/x', FxSpeaker),
        )

        report(failed === 'ApiProtocolError', 'request: throws ApiProtocolError when the body is not JSON')
    }

    // 5. Valid JSON that does not match the generated schema means the SDK is stale — the single most
    //    important thing this module exists to catch, and it must not be waved through.
    {
        const { transport } = stub(200, envelope('{"name":42}'))

        const failed = await rejectedAsync(() =>
            request({ baseUrl: 'http://x', transport }, 'GET', '/api/x', FxSpeaker),
        )

        report(failed === 'ApiProtocolError', 'request: throws ApiProtocolError when the payload is stale')
    }

    // 5b. DATA PROTECTION. An error escapes the application — a global handler passes it to Sentry or
    //     console.error, and Sentry serialises an Error's own enumerable properties. The stale-SDK
    //     path fires on an ordinary successful response, so a retained body is real user data leaving
    //     the origin. These checks assert the payload is NOT reachable, by any of the routes a
    //     reporter would take.
    {
        const secret = 'ada.lovelace@example.com'

        const { transport } = stub(200, envelope(`{"name":"Ada","bio":{"email":"${secret}"}}`))

        let caught: ApiProtocolError | undefined

        try {
            await request({ baseUrl: 'http://x', transport }, 'GET', '/api/x', FxSpeaker)
        } catch (e) {
            caught = e as ApiProtocolError
        }

        report(caught?.name === 'ApiProtocolError', 'privacy: a stale payload still throws')

        report(caught?.body === undefined, 'privacy: the raw body is NOT retained by default')

        report(
            caught !== undefined && !caught.message.includes(secret),
            'privacy: the message does not quote the payload',
        )

        // The route a reporter actually takes. JSON.stringify over an Error walks own enumerable
        // properties, which is exactly what Sentry does.
        report(
            caught !== undefined && !JSON.stringify({ ...caught }).includes(secret),
            'privacy: serialising the error for a reporter does not carry the payload',
        )

        // ...while still saying enough to identify the drift.
        report(
            caught?.issues.some((issue) => issue.includes('bio')) === true,
            'privacy: the drifted FIELD is still named, which is what diagnoses a stale SDK',
            caught?.issues.join(' | '),
        )

        report(caught?.bodyLength !== undefined && caught.bodyLength > 0, 'privacy: the body size is kept')
    }

    // 5c. ...and the opt-out works, so a developer can still reproduce locally.
    {
        const secret = 'grace.hopper@example.com'

        const { transport } = stub(200, envelope(`{"name":"Grace","bio":{"email":"${secret}"}}`))

        const failed = await rejectedAsync(() =>
            request({ baseUrl: 'http://x', transport, debug: true }, 'GET', '/api/x', FxSpeaker),
        )

        report(failed === 'ApiProtocolError', 'privacy: debug mode still throws the same error')

        let caught: ApiProtocolError | undefined

        try {
            await request({ baseUrl: 'http://x', transport, debug: true }, 'GET', '/api/x', FxSpeaker)
        } catch (e) {
            caught = e as ApiProtocolError
        }

        report(
            caught?.body?.includes(secret) === true,
            'privacy: debug: true DOES attach the body, so the flag is not decorative',
        )
    }

    // 6. unwrap: throws on non-2xx, and keeps the envelope reachable on the error.
    {
        const notFound = { status: { value: 404, description: 'Not Found' }, data: null }

        let caught: unknown

        try {
            unwrap(notFound)
        } catch (e) {
            caught = e
        }

        report(caught instanceof ApiError, 'unwrap: throws ApiError on a non-2xx envelope')
        report(
            (caught as ApiError)?.response?.status?.value === 404,
            'unwrap: keeps the envelope on the error, so messages survive the throw',
        )

        report(
            unwrap({ status: { value: 200, description: 'OK' }, data: { name: 'Ada', bio: null } })?.name === 'Ada',
            'unwrap: returns the payload on 2xx',
        )

        // Nullable on success too — noContent() and okOrNotFound() both send null.
        report(
            unwrap({ status: { value: 204, description: 'No Content' }, data: null }) === null,
            'unwrap: returns null for a 2xx envelope with no data',
        )
    }
}

/**
 * The end-to-end check: a GENERATED client, constructed and called for real.
 *
 * Everything else here tests hand-written runtime. This is the only place emitted client code is
 * executed, and it exercises the whole chain at once — the class shape, its imports resolving, the
 * schema expression the emitter chose, `request`, and the envelope parse. A Kotlin assertion over the
 * emitted string can only compare it to a string written by the same hand that emitted it.
 */
async function checkGeneratedClient(report: Report): Promise<void> {
    let sent: HttpRequest | undefined

    const transport: HttpTransport = {
        send: (req) => {
            sent = req

            // One stub, answering each route with a payload its own schema accepts — otherwise a
            // parse failure reads as a client bug when it is really a fixture bug.
            //
            // Matched most-specific-first and anchored on the full path: `/speakers` and
            // `/speakers/{id}` return DIFFERENT shapes, and a substring test picked the wrong one.
            const speaker = '{"name":"Ada","bio":null}'

            const talk =
                '{"id":"t-1","title":"Hello","status":"ACTIVE","speakers":[],"tags":[],' +
                '"meta":{},"seats":42,"durationMs":1234,"rating":null,"featured":true}'

            const instant = '{"ts":1785406530000,"timezone":"UTC","human":"2026-07-30T10:15:30.000Z"}'

            const path = new URL(req.url).pathname

            const payload =
                path === '/api/fx/speakers' ? `[${speaker}]`
                : path.startsWith('/api/fx/speakers/') ? talk
                : path.startsWith('/api/fx/talks/') ? talk
                : instant

            return Promise.resolve({
                status: 200,
                statusText: 'OK',
                body: `{"status":{"value":200,"description":"OK"},"data":${payload}}`,
            })
        },
    }

    const client = new FxDemoClient({ baseUrl: 'http://x', transport })

    // 1. The aggregate really constructs its groups, and a member call reaches the transport.
    const speakers = await client.talks.listSpeakers()

    report(sent?.url === 'http://x/api/fx/speakers', 'client: the generated member builds its URL')
    report(sent?.method === 'GET', 'client: the generated member sends its declared method')
    report(
        equal(speakers.data, [{ name: 'Ada', bio: null }]),
        'client: the generated schema parses the payload',
    )

    // 2. A claimed type as the payload — imported from its runtime module, not from models.ts. If the
    //    emitter had imported it from models.ts this file would not even load.
    const time = await client.status.serverTime()

    report(time.data?.timezone === 'UTC', 'client: a claimed payload type parses through the client')
    report(sent?.method === 'GET' && sent?.url === 'http://x/api/fx/time', 'client: second group is wired')

    // 3. A parameterised member: the path param fills the placeholder, the query params append, and
    //    an omitted optional one is left out entirely rather than sent empty.
    await client.talks.getTalk({ id: 'a b', page: 2 })

    report(
        sent?.url === 'http://x/api/fx/talks/a%20b?page=2',
        'client: path params fill the pattern and query params append',
        sent?.url,
    )

    await client.talks.getTalk({ id: 't-1' })

    report(
        sent?.url === 'http://x/api/fx/talks/t-1',
        'client: an omitted optional param is not sent at all',
        sent?.url,
    )

    // 3b. A request body: JSON-encoded and sent, alongside a path parameter.
    await client.talks.importNodes({ id: 't-9' }, [{ name: 'root', children: [], parent: null }])

    report(
        sent?.url === 'http://x/api/fx/talks/t-9/nodes' && sent?.method === 'PUT',
        'client: a body route still builds its URL and method',
        sent?.url,
    )

    report(
        sent?.body === '[{"name":"root","children":[],"parent":null}]',
        'client: the body is JSON-encoded and sent',
        sent?.body,
    )

    report(
        sent?.headers['Content-Type'] === 'application/json',
        'client: a body route declares its content type',
    )

    // 3c. A STREAM. Executed, not merely type-checked: the member has no response schema at all, so
    //     the only thing that can confirm it is usable is consuming it.
    {
        const frames =
            'event: tick\ndata: {"n":1}\n\n' +
            'event: tick\ndata: {"n":2}\n\n'

        let requested: string | undefined

        const fakeFetch: typeof fetch = (url) => {
            requested = String(url)

            return Promise.resolve(
                new Response(frames, { status: 200, headers: { 'Content-Type': 'text/event-stream' } }),
            )
        }

        const seen: Array<{ event: string; data: string }> = []

        for await (const event of client.status.watch({ room: 'r 1' }, { fetchImpl: fakeFetch })) {
            seen.push({ event: event.event, data: event.data })
        }

        report(
            requested === 'http://x/api/fx/watch/r%201',
            'client: a stream builds and encodes its URL',
            requested,
        )

        report(
            equal(seen, [
                { event: 'tick', data: '{"n":1}' },
                { event: 'tick', data: '{"n":2}' },
            ]),
            'client: a stream yields the parsed events',
        )

        // The decision this records: events carry RAW data strings, because the payload type is not on
        // the route. If that ever changes, this line stops compiling and the choice gets revisited.
        report(
            typeof seen[0]?.data === 'string',
            'client: stream event data is an unparsed string, per the untyped-SSE decision',
        )
    }

    // 3d. CANCELLATION. `client.ts` documents `signal` as the way to cancel on unmount, and until
    //     2026-07-31 no generated member could pass one — the option existed and was unreachable.
    {
        const controller = new AbortController()

        let seenSignal: AbortSignal | undefined

        const transport: HttpTransport = {
            send: (req) => {
                seenSignal = req.signal

                // Per-route payload, or the schema rejects it and the failure reads as a client bug.
                const data = req.url.includes('/talks/')
                    ? '{"id":"t-1","title":"Hello","status":"ACTIVE","speakers":[],"tags":[],' +
                      '"meta":{},"seats":42,"durationMs":1234,"rating":null,"featured":true}'
                    : '[]'

                return Promise.resolve({
                    status: 200,
                    statusText: 'OK',
                    body: `{"status":{"value":200,"description":"OK"},"data":${data}}`,
                })
            },
        }

        const cancellable = new FxDemoClient({ baseUrl: 'http://x', transport })

        await cancellable.talks.listSpeakers({ signal: controller.signal })

        report(
            seenSignal === controller.signal,
            "client: a caller's AbortSignal reaches the transport (no-params member)",
        )

        // A member WITH params takes a different emit branch — the options spread lands inside the
        // built object rather than being the whole object. Both branches must forward the signal.
        seenSignal = undefined

        await cancellable.talks.getTalk({ id: 't-1' }, { signal: controller.signal })

        report(
            seenSignal === controller.signal,
            "client: a caller's AbortSignal reaches the transport (member with params)",
        )

        // ...and the generated path/query still arrive when options are also passed. The spread is
        // last, so this is the check that a caller cannot clobber the route the generator derived.
        let seenUrl: string | undefined

        const recording: HttpTransport = {
            send: (req) => {
                seenUrl = req.url
                return Promise.resolve({
                    status: 200,
                    statusText: 'OK',
                    body:
                        '{"status":{"value":200,"description":"OK"},"data":' +
                        '{"id":"t-1","title":"Hello","status":"ACTIVE","speakers":[],"tags":[],' +
                        '"meta":{},"seats":42,"durationMs":1234,"rating":null,"featured":true}}',
                })
            },
        }

        await new FxDemoClient({ baseUrl: 'http://x', transport: recording })
            .talks.getTalk({ id: 't-1', page: 3 }, { signal: controller.signal })

        report(
            seenUrl === 'http://x/api/fx/talks/t-1?page=3',
            'client: passing options does not displace the generated path and query',
            seenUrl,
        )
    }

    // 4. NEGATIVE TYPE CHECKS. Calling a member correctly proves the signature EXISTS; it does not
    //    prove the signature is ENFORCED — emitting every parameter as optional passes every check
    //    above. `@ts-expect-error` inverts that: tsc fails when the line STOPS erroring, so these
    //    break the moment the emitted types get looser.

    // @ts-expect-error `id` has no Kotlin default, so omitting it must not compile.
    void client.talks.getTalk({ page: 1 })

    // @ts-expect-error `order` is a union of the enum's constants, not an arbitrary string.
    void client.talks.getTalk({ id: 't-1', order: 'SIDEWAYS' })

    // @ts-expect-error a path param is a string; passing an object would stringify to "[object Object]".
    void client.talks.getTalk({ id: { nope: true } })

    // @ts-expect-error an unknown parameter is a typo, not something to send silently.
    void client.talks.getTalk({ id: 't-1', pge: 2 })

    // @ts-expect-error the body is typed, so a wrong shape must not compile.
    void client.talks.importNodes({ id: 't-9' }, [{ nope: true }])

    // @ts-expect-error a body route still requires its params argument.
    void client.talks.importNodes([{ name: 'root', children: [], parent: null }])

    // @ts-expect-error a stream still requires its path params.
    void client.status.watch({})

    report(true, 'client: the emitted signature rejects wrong calls (7 @ts-expect-error sites)')

    // 5. THE reason members are arrow-function class fields. A prototype method type-checks here and
    //    throws at run time, and this is the Vue-composable idiom, so it would break in real use.
    const { listSpeakers } = client.talks

    try {
        const destructured = await listSpeakers()

        report(
            equal(destructured.data, [{ name: 'Ada', bio: null }]),
            'client: a destructured member still works (arrow field, not prototype method)',
        )
    } catch (e) {
        report(false, 'client: a destructured member still works', (e as Error).message.split('\n')[0])
    }
}

/**
 * Route identity and the access lookup.
 *
 * The reason this is not covered by `checkGeneratedClient`: a positive call proves a member is
 * CALLABLE after the wrap, not that its signature survived intact. The negative sites below prove
 * that, and they fail loudly — tsc reports TS2578 when a `@ts-expect-error` stops erroring.
 *
 * Measured while mutation-testing, so nobody re-derives it: widening `route()`'s bound to
 * `(...args: any[]) => any` changes NOTHING here, because `F` is inferred from the argument either
 * way. The bound is a strictness choice, not a correctness one.
 */
async function checkRouteAndAcl(report: Report): Promise<void> {
    // A transport answering only what this group's one real call needs.
    const transport: HttpTransport = {
        send: () => Promise.resolve({
            status: 200,
            statusText: 'OK',
            body: '{"status":{"value":200,"description":"OK"},"data":[{"name":"Ada","bio":null}]}',
        }),
    }

    const client = new FxDemoClient({ baseUrl: 'http://x', transport })

    // 1. A member carries its own identity, and it is the PATTERN, not a filled-in URL — that is
    //    what the access matrix is keyed by.
    report(client.talks.getTalk.method === 'GET', 'route: member carries its method')
    report(
        client.talks.getTalk.uri === '/api/fx/talks/{id}',
        'route: member carries its uri PATTERN, placeholders intact',
        client.talks.getTalk.uri,
    )
    report(client.status.latest.method === 'POST', 'route: a non-GET member carries its own method')
    report(
        client.status.watch.uri === '/api/fx/watch/{room}',
        'route: a STREAM member is wrapped too',
        client.status.watch.uri,
    )

    // 2. The wrap is transparent: the member still calls, and still returns its validated payload.
    try {
        const speakers = await client.talks.listSpeakers()

        report(
            equal(speakers.data, [{ name: 'Ada', bio: null }]),
            'route: a wrapped member still performs its call',
        )
    } catch (e) {
        report(false, 'route: a wrapped member still performs its call', (e as Error).message.split('\n')[0])
    }

    // 3. And it still survives destructuring — `Object.assign` mutates the per-instance arrow field,
    //    so there is no prototype to lose.
    const { getTalk } = client.talks
    report(getTalk.uri === '/api/fx/talks/{id}', 'route: metadata survives destructuring')

    // 4. The ACL, against a matrix using the same keys the generator emits.
    const acl = new ApiAcl({
        entries: [
            { method: 'GET', uri: '/api/fx/talks/{id}', level: 'Granted' },
            { method: 'GET', uri: '/api/fx/speakers', level: 'Partial' },
            { method: 'POST', uri: '/api/fx/status', level: 'Denied' },
        ],
    })

    const grid: Array<[string, boolean, boolean, boolean, boolean]> = [
        // label, canAccess, canFullyAccess, canPartiallyAccess, isDenied
        ['Granted', true, true, false, false],
        ['Partial', true, false, true, false],
        ['Denied', false, false, false, true],
    ]
    const refs = [client.talks.getTalk, client.talks.listSpeakers, client.status.latest]

    grid.forEach(([label, canAccess, canFully, canPartially, denied], i) => {
        const r = refs[i]!
        report(acl.canAccess(r) === canAccess, `acl: ${label} canAccess`)
        report(acl.canFullyAccess(r) === canFully, `acl: ${label} canFullyAccess`)
        report(acl.canPartiallyAccess(r) === canPartially, `acl: ${label} canPartiallyAccess`)
        report(acl.isDenied(r) === denied, `acl: ${label} isDenied`)
    })

    // 5. Absence is how denial is transmitted — the server OMITS denied rows, so a route the matrix
    //    never mentions must read Denied. Dropping the `?? 'Denied'` fallback would grant it.
    report(
        acl.getAccessLevel(client.talks.importNodes) === 'Denied',
        'acl: a route absent from the matrix is Denied, not undefined',
        String(acl.getAccessLevel(client.talks.importNodes)),
    )
    report(ApiAcl.empty.isDenied(client.talks.getTalk), 'acl: the empty ACL denies everything')

    // 6. A near-miss must NOT match. Same uri, different method — and a filled-in URL rather than
    //    the pattern, which is the mistake a caller would make by hand.
    const nearMiss = new ApiAcl({
        entries: [
            { method: 'POST', uri: '/api/fx/talks/{id}', level: 'Granted' },
            { method: 'GET', uri: '/api/fx/talks/t-1', level: 'Granted' },
        ],
    })
    report(nearMiss.isDenied(client.talks.getTalk), 'acl: method is part of the key')

    // 7. The ACL methods are arrow fields too, so composables can destructure them.
    const { canAccess } = acl
    report(canAccess(client.talks.getTalk), 'acl: a destructured predicate still works')

    // 8. PUBLICNESS. The reason it exists: the matrix endpoint is itself authenticated, so a
    //    logged-out visitor has none — and without this the ACL denies `signIn` and the user can
    //    never reach a state where a matrix could be fetched.
    report(client.status.signIn.isPublic, 'route: a public endpoint is marked isPublic')
    report(!client.talks.getTalk.isPublic, 'route: a normal endpoint is not')

    const anon = ApiAcl.empty

    report(
        anon.canAccess(client.status.signIn),
        'acl: an ANONYMOUS visitor with no matrix can still reach a public route',
    )
    report(
        !anon.canAccess(client.talks.getTalk),
        'acl: and is still denied everything else',
    )
    report(
        !anon.canFullyAccess(client.status.signIn),
        'acl: canFullyAccess does NOT short-circuit — the strict predicate must not be the loose one',
    )
    report(
        anon.getAccessLevel(client.status.signIn) === 'Denied',
        'acl: getAccessLevel still reports the raw matrix answer, unaffected by isPublic',
    )

    // A public route the matrix DOES mention keeps the matrix's answer for the strict predicate.
    const withMatrix = new ApiAcl({
        entries: [{ method: 'POST', uri: '/api/fx/signin', level: 'Granted' }],
    })
    report(
        withMatrix.canFullyAccess(client.status.signIn),
        'acl: a public route present in the matrix is fully accessible',
    )

    // 9. Route metadata is frozen, so `readonly` is STRUCTURAL, not merely type-level. Without the
    //    freeze the assignment below type-errors and then silently succeeds at run time, repointing
    //    a member's identity — and the access lookup keys off exactly that.
    let threw = false
    try {
        // @ts-expect-error readonly at the type level; frozen at run time. Both must hold.
        client.talks.getTalk.uri = '/api/fx/somewhere-else'
    } catch {
        threw = true
    }

    report(threw, 'route: metadata is frozen, so nothing can repoint a member at run time')
    report(
        client.talks.getTalk.uri === '/api/fx/talks/{id}',
        'route: the uri survived the attempt to rewrite it',
        client.talks.getTalk.uri,
    )

    // 10. NEGATIVE TYPE CHECKS. Only sites that are NOT already covered by `checkGeneratedClient` —
    //    that block runs against the same wrapped client, so repeating its parameter checks here
    //    would prove nothing and cannot fail independently.

    // @ts-expect-error HttpMethod is a closed union, so a typo cannot survive.
    void route('TRACE', '/x', () => undefined)

    // @ts-expect-error a bare function is not a RouteRef.
    void acl.canAccess(() => undefined)

    // @ts-expect-error the matrix level is a closed union.
    void new ApiAcl({ entries: [{ method: 'GET', uri: '/x', level: 'Maybe' }] })

    // The PAYLOAD type through the wrap. The positive check above compares values at run time, which
    // passes just as well if `data` degraded to `any` — only this fails if it did.
    const typed = await client.talks.listSpeakers()
    // @ts-expect-error the awaited payload keeps its element type; `nope` is not on FxSpeaker.
    void typed.data?.[0]?.nope

    report(true, 'route/acl: the emitted types reject wrong use (4 @ts-expect-error sites)')
}

/** A bearer sign-in payload, shaped exactly like the generated `AuthSignInResponseSuccess`. */
function bearer(token: string, expiresAtMs?: number): SignedIn<{ roles?: string[] }> {
    return {
        session: { _type: 'bearer', token },
        permissions: { roles: ['ops'] },
        expiresAt: expiresAtMs === undefined ? null : { ts: expiresAtMs },
        userId: 'u-1',
    }
}

/** A cookie sign-in payload — note there is NO token, deliberately. */
const cookiePayload: SignedIn<{ roles?: string[] }> = {
    session: { _type: 'cookie' },
    permissions: { roles: ['ops'] },
    expiresAt: { ts: 1_800_000_000_000 },
    userId: 'u-1',
}

/**
 * The auth session runtime.
 *
 * Executed, not merely type-checked: every interesting property is runtime behaviour — whether a
 * listener sees the first frame, whether the header is on the wire, whether a reload restores.
 */
async function checkAuthSession(report: Report): Promise<void> {
    const store = inMemorySession()
    const session = new AuthSession<{ roles?: string[] }>(store)

    report(!session.state().isLoggedIn, 'auth: a fresh session is logged out')

    const seen: boolean[] = []
    const stop = session.subscribe((s) => seen.push(s.isLoggedIn))

    report(equal(seen, [false]), 'auth: subscribe fires at once with the current state')

    session.signedIn(bearer('tok-1', 1_800_000_000_000))

    report(session.state().isLoggedIn, 'auth: signedIn logs in')
    report(session.state().token === 'tok-1', 'auth: a bearer session exposes its token')
    report(equal(session.state().permissions, { roles: ['ops'] }), 'auth: permissions come from the RESPONSE')
    report(session.state().expiresAt === 1_800_000_000_000, 'auth: expiresAt is read from `.ts`')
    report(session.state().userId === 'u-1', 'auth: and the user id')
    report(equal(seen, [false, true]), 'auth: subscribers are notified')

    // THE reason the whole payload is persisted rather than just a token: nothing decodes a JWT any
    // more, so after a reload the expiry and permissions would otherwise be gone, the session would
    // never schedule a refresh, and it would present as "randomly logged out".
    const reloaded = new AuthSession<{ roles?: string[] }>(store)

    report(reloaded.state().isLoggedIn, 'auth: a new session restores from storage')
    report(reloaded.state().expiresAt === 1_800_000_000_000, 'auth: and its EXPIRY survives the reload')
    report(equal(reloaded.state().permissions, { roles: ['ops'] }), 'auth: and its permissions')

    report(session.isExpiring(0, 1_700_000_000_000) === false, 'auth: not expiring long before exp')
    report(session.isExpiring(0, 1_900_000_000_000) === true, 'auth: expiring after exp')
    report(
        new AuthSession(inMemorySession()).isExpiring(60_000) === false,
        'auth: a logged-out session is never "expiring"',
    )

    const noExp = new AuthSession<{ roles?: string[] }>(inMemorySession())
    noExp.signedIn(bearer('tok-2'))
    report(
        noExp.isExpiring(Number.MAX_SAFE_INTEGER) === false,
        'auth: a session with no expiry is never "expiring" — `exp` is optional in RFC 7519',
    )

    stop()
    session.signedIn(bearer('tok-3'))
    report(equal(seen, [false, true]), 'auth: unsubscribe stops notifications')

    session.signOut()
    report(!session.state().isLoggedIn, 'auth: signOut logs out')
    report(store.read() === null, 'auth: signOut clears storage')

    // COOKIE mode: no token, still logged in. A token-shaped API could not express this.
    const cookieSession = new AuthSession<{ roles?: string[] }>(inMemorySession())
    cookieSession.signedIn(cookiePayload)

    report(cookieSession.state().isLoggedIn, 'auth: a cookie session is logged in')
    report(cookieSession.state().token === null, 'auth: and carries NO token')
    report(cookieSession.state().carrier?._type === 'cookie', 'auth: the carrier says which mode')

    for (const junk of ['', 'not json', '{}', '{"session":null}', '{"session":{"_type":"nope"}}']) {
        const bad = inMemorySession()
        bad.write(junk)
        try {
            report(!new AuthSession(bad).state().isLoggedIn, `auth: corrupt storage '${junk}' restores logged-out`)
        } catch (e) {
            report(false, `auth: corrupt storage '${junk}' restores logged-out`, (e as Error).message)
        }
    }

    let sent: HttpRequest | undefined
    const inner: HttpTransport = {
        send: (req) => {
            sent = req
            return Promise.resolve({ status: 200, statusText: 'OK', body: '{}' })
        },
    }

    const live = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const wrapped = authTransport(inner, live)

    await wrapped.send({ method: 'GET', url: 'http://x/a', headers: {} })
    report(sent?.headers['Authorization'] === undefined, 'auth: logged out means no header')
    report(sent?.credentials === undefined, 'auth: and no credentials')

    live.signedIn(bearer('tok-123'))
    await wrapped.send({ method: 'GET', url: 'http://x/a', headers: {} })
    report(sent?.headers['Authorization'] === 'Bearer tok-123', 'auth: bearer attaches the header')
    report(sent?.credentials === undefined, 'auth: bearer does NOT set credentials')

    live.signOut()
    await wrapped.send({ method: 'GET', url: 'http://x/a', headers: {} })
    report(sent?.headers['Authorization'] === undefined, 'auth: signOut takes effect immediately')

    live.signedIn(cookiePayload)
    await wrapped.send({ method: 'GET', url: 'http://x/a', headers: {} })
    report(sent?.credentials === 'include', 'auth: COOKIE mode permits the cookie to travel')
    report(
        sent?.headers['Authorization'] === undefined,
        'auth: and attaches no header — JS cannot read the cookie, so there is nothing to attach',
    )

    live.signedIn(bearer('tok-9'))
    await wrapped.send({
        method: 'GET',
        url: 'http://x/a',
        headers: { Authorization: 'Bearer explicit' },
    })
    report(
        sent?.headers['Authorization'] === 'Bearer explicit',
        'auth: an explicit Authorization header is never overwritten',
    )

    const viaLocal = localStorageSession('funktor.test.session')
    viaLocal.write('persisted')
    report(viaLocal.read() === 'persisted', 'auth: localStorage strategy round-trips (or falls back)')
    viaLocal.clear()
    report(viaLocal.read() === null, 'auth: and clears')
}

/** An envelope carrying [data] at [status]. */
function envelope<T>(status: number, data: T | null, message?: string) {
    return {
        status: { value: status, description: 'x' },
        data,
        messages: message === undefined ? null : [{ type: 'error' as const, text: message, ts: null }],
    }
}

/**
 * The sign-in flow.
 *
 * The point of the module: `AuthSignInResponse` has three branches and TWO OF THEM ARE NOT FAILURES.
 * Treating `activation-required` as a bad password is the obvious bug and it is silent.
 */
async function checkLoginFlow(report: Report): Promise<void> {
    const success: SignInResult<{ roles?: string[] }> = {
        _type: 'success',
        session: { _type: 'bearer', token: 'tok-1' },
        permissions: { roles: ['ops'] },
        expiresAt: { ts: 1_800_000_000_000 },
        userId: 'u-1',
    }

    // 1. Success stores the session and reports it.
    const s1 = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const ok = applySignIn(s1, envelope(200, success))

    report(ok._type === 'signed-in', 'login: success is signed-in')
    report(s1.state().isLoggedIn, 'login: and the session is stored')
    report(
        ok._type === 'signed-in' && ok.session.token === 'tok-1',
        'login: the outcome carries the resulting session state',
    )

    // 2. The two NON-FAILURE branches must NOT create a session — their tokens grant one next step.
    const s2 = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const org = applySignIn(s2, envelope(200, {
        _type: 'org-selection-required' as const,
        selectionToken: 'sel-1',
        organisations: [{ id: 'o-1', name: 'Acme' }],
    }))

    report(org._type === 'org-selection-required', 'login: org selection is NOT a failure')
    report(
        org._type === 'org-selection-required' && org.selectionToken === 'sel-1',
        'login: and carries its single-use token',
    )
    report(!s2.state().isLoggedIn, 'login: org selection does NOT log the user in')

    const s3 = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const act = applySignIn(s3, envelope(200, { _type: 'activation-required' as const, resendToken: 'r-1' }))

    report(act._type === 'activation-required', 'login: activation required is NOT a failure')
    report(!s3.state().isLoggedIn, 'login: and does NOT log the user in')

    // 3. Rejection is ordinary control flow, not an exception — matching `request`'s contract.
    const s4 = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const bad = applySignIn(s4, envelope(401, null, 'Wrong credentials'))

    report(bad._type === 'rejected', 'login: a non-2xx is rejected, not thrown')
    report(bad._type === 'rejected' && bad.message === 'Wrong credentials', 'login: with the server message')
    report(bad._type === 'rejected' && bad.status === 401, 'login: and the status')
    report(!s4.state().isLoggedIn, 'login: a rejection leaves the session alone')

    // A non-2xx carrying a SUCCESS-SHAPED body must still be rejected. Found by mutation: every
    // rejection test above passes `data: null`, so the null guard caught them and the STATUS check
    // was never exercised on its own. A server answering 401 with a body would have logged the user
    // in.
    const s4b = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const liar = applySignIn(s4b, envelope(401, success))

    report(liar._type === 'rejected', 'login: a non-2xx is rejected even when it carries a body')
    report(!s4b.state().isLoggedIn, 'login: and such a response creates no session')

    // A 2xx with no data is still no session — a proxy or an empty envelope.
    const s5 = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const empty = applySignIn(s5, envelope(200, null))
    report(empty._type === 'rejected', 'login: a 2xx with null data is rejected, not a crash')

    // A rejection with no messages must not invent one.
    const s6 = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const quiet = applySignIn(s6, envelope(500, null))
    report(quiet._type === 'rejected' && quiet.message === null, 'login: no message means null, not ""')

    // 4. The async form, which is what an app actually writes.
    const s7 = new AuthSession<{ roles?: string[] }>(inMemorySession())
    const awaited = await completeSignIn(s7, Promise.resolve(envelope(200, success)))

    report(awaited._type === 'signed-in', 'login: completeSignIn awaits the call')
    report(s7.state().isLoggedIn, 'login: and applies it')

    // 5. A COOKIE-mode success carries no token and still logs in.
    const s8 = new AuthSession<{ roles?: string[] }>(inMemorySession())
    applySignIn(s8, envelope(200, {
        _type: 'success' as const,
        session: { _type: 'cookie' as const },
        permissions: { roles: ['ops'] },
        expiresAt: { ts: 1_800_000_000_000 },
        userId: 'u-1',
    }))

    report(s8.state().isLoggedIn && s8.state().token === null, 'login: a cookie-mode success logs in with no token')
}

/**
 * Auto-refresh.
 *
 * Driven by an INJECTED clock and timer, so this executes the real scheduling logic without waiting.
 * A test that slept would either be slow or flaky; this is neither.
 */
async function checkAutoRefresh(report: Report): Promise<void> {
    /** A controllable timer: `fire()` runs whatever is currently scheduled. */
    function fakeTimers() {
        let pending: (() => void) | null = null
        let cleared = 0

        return {
            setTimer: (fn: () => void) => {
                pending = fn
                return 1
            },
            clearTimer: () => {
                cleared += 1
                pending = null
            },
            fire: () => {
                const fn = pending
                pending = null
                fn?.()
            },
            cleared: () => cleared,
            armed: () => pending !== null,
        }
    }

    const success = (token: string) => ({
        status: { value: 200, description: 'OK' },
        data: {
            _type: 'success' as const,
            session: { _type: 'bearer' as const, token },
            permissions: { roles: ['ops'] },
            expiresAt: { ts: 2_000_000_000_000 },
            userId: 'u-1',
        },
        messages: null,
    })

    // 1. Not expiring — no refresh, but still re-armed.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'a' }, expiresAt: { ts: 2_000_000_000_000 } })

        let calls = 0
        startAutoRefresh(s, () => { calls += 1; return Promise.resolve(success('b')) }, {
            leadMs: 1_000, now: () => 1_000_000_000_000, setTimer: t.setTimer, clearTimer: t.clearTimer,
        })

        t.fire()
        report(calls === 0, 'refresh: a session far from expiry is not refreshed')
        report(t.armed(), 'refresh: and the schedule is re-armed anyway')
    }

    // 2. Expiring — refreshes, and the new token lands in the session.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'old' }, expiresAt: { ts: 1_000_000_060_000 } })

        startAutoRefresh(s, () => Promise.resolve(success('new')), {
            leadMs: 120_000, now: () => 1_000_000_000_000, setTimer: t.setTimer, clearTimer: t.clearTimer,
        })

        t.fire()
        await Promise.resolve()
        await Promise.resolve()
        await Promise.resolve()

        report(s.state().token === 'new', 'refresh: an expiring session is refreshed and updated')
    }

    // 3. A session with NO expiry is never refreshed — `exp` is optional, and such a token does not
    //    expire server-side either.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'a' } })

        let calls = 0
        startAutoRefresh(s, () => { calls += 1; return Promise.resolve(success('b')) }, {
            leadMs: Number.MAX_SAFE_INTEGER, setTimer: t.setTimer, clearTimer: t.clearTimer,
        })

        t.fire()
        report(calls === 0, 'refresh: a session with no expiry is never refreshed')
    }

    // 4. A rejected refresh reports but does NOT sign the user out — that is app policy.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'old' }, expiresAt: { ts: 1_000_000_060_000 } })

        let failed = 0
        startAutoRefresh(
            s,
            () => Promise.resolve({ status: { value: 401, description: 'no' }, data: null, messages: null }),
            {
                leadMs: 120_000, now: () => 1_000_000_000_000,
                setTimer: t.setTimer, clearTimer: t.clearTimer,
                onFailed: () => { failed += 1 },
            },
        )

        t.fire()
        await Promise.resolve()
        await Promise.resolve()
        await Promise.resolve()

        report(failed === 1, 'refresh: a rejected refresh calls onFailed')
        report(s.state().isLoggedIn, 'refresh: and does NOT sign the user out — that is app policy')
    }

    // 5. A THROWN refresh (network down) must not kill the session or the schedule.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'old' }, expiresAt: { ts: 1_000_000_060_000 } })

        startAutoRefresh(s, () => Promise.reject(new Error('offline')), {
            leadMs: 120_000, now: () => 1_000_000_000_000, setTimer: t.setTimer, clearTimer: t.clearTimer,
        })

        t.fire()
        await Promise.resolve()
        await Promise.resolve()
        await Promise.resolve()

        report(s.state().isLoggedIn, 'refresh: a network failure does not log the user out')
        report(t.armed(), 'refresh: and the schedule survives it')
    }

    // 6. Refreshes must NOT overlap. Found by mutation: every scenario above fires the timer once,
    //    so dropping the in-flight guard changed nothing. Two racing refreshes would have one write
    //    a stale session last.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'old' }, expiresAt: { ts: 1_000_000_060_000 } })

        let calls = 0
        // A HOLDER, not a `let`: TypeScript narrows a bare local to `null` because it cannot see the
        // assignment happening inside the Promise executor, so `release?.(...)` reads as uncallable.
        const box: { release: ((v: ReturnType<typeof success>) => void) | null } = { release: null }

        startAutoRefresh(s, () => {
            calls += 1
            return new Promise<ReturnType<typeof success>>((resolve) => { box.release = resolve })
        }, {
            leadMs: 120_000, now: () => 1_000_000_000_000, setTimer: t.setTimer, clearTimer: t.clearTimer,
        })

        t.fire()
        await Promise.resolve()
        t.fire()
        await Promise.resolve()

        report(calls === 1, 'refresh: a second tick during an in-flight refresh does not start another')

        box.release?.(success('new'))
        await Promise.resolve()
        await Promise.resolve()
        await Promise.resolve()
        report(s.state().token === 'new', 'refresh: and the in-flight one still lands')
    }

    // 7. A refresh that resolves AFTER stop() must not touch the session. Found by mutation: the
    //    earlier stop test could not see this, because clearing the timer alone already prevented
    //    any further tick.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'old' }, expiresAt: { ts: 1_000_000_060_000 } })

        const box: { release: ((v: ReturnType<typeof success>) => void) | null } = { release: null }
        const stop = startAutoRefresh(s, () =>
            new Promise<ReturnType<typeof success>>((resolve) => { box.release = resolve }), {
            leadMs: 120_000, now: () => 1_000_000_000_000, setTimer: t.setTimer, clearTimer: t.clearTimer,
        })

        t.fire()
        await Promise.resolve()
        stop()
        box.release?.(success('late'))
        await Promise.resolve()
        await Promise.resolve()
        await Promise.resolve()

        report(s.state().token === 'old', 'refresh: a refresh resolving after stop() is discarded')
    }

    // 8. A thrown refresh must be CAUGHT, not merely survivable. Found by mutation: removing the
    //    catch left every assertion green, because an unhandled rejection does not fail a check.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'old' }, expiresAt: { ts: 1_000_000_060_000 } })

        let unhandled = 0
        const onUnhandled = () => { unhandled += 1 }
        process.on('unhandledRejection', onUnhandled)

        startAutoRefresh(s, () => Promise.reject(new Error('offline')), {
            leadMs: 120_000, now: () => 1_000_000_000_000, setTimer: t.setTimer, clearTimer: t.clearTimer,
        })

        t.fire()
        await new Promise((r) => setTimeout(r, 10))
        process.off('unhandledRejection', onUnhandled)

        report(unhandled === 0, 'refresh: a thrown refresh is CAUGHT, not left unhandled')
    }

    // 9. stop() really stops.
    {
        const t = fakeTimers()
        const s = new AuthSession<{ roles?: string[] }>(inMemorySession())
        s.signedIn({ session: { _type: 'bearer', token: 'old' }, expiresAt: { ts: 1_000_000_060_000 } })

        let calls = 0
        const stop = startAutoRefresh(s, () => { calls += 1; return Promise.resolve(success('new')) }, {
            leadMs: 120_000, now: () => 1_000_000_000_000, setTimer: t.setTimer, clearTimer: t.clearTimer,
        })

        stop()
        report(t.cleared() === 1, 'refresh: stop clears the pending timer')

        t.fire()
        report(calls === 0, 'refresh: and nothing runs afterwards')
    }
}

/**
 * The aggregation registry's rendered output.
 *
 * Executed, not just compiled. The interesting failure is a route table that type-checks and then
 * fails to LOAD — a lazy `import()` naming a component nobody emitted resolves at compile time
 * against a path string and blows up at run time, in the consuming app, on navigation.
 */
async function checkMount(report: Report): Promise<void> {
    // 1. Sorted by path, so the file is stable across runs. `--check` compares content, and an
    //    unstable order would report drift that is not real.
    report(
        equal(routes.map((r) => r.path), ['/insights', '/insights/details', '/login']),
        'mount: routes are path-sorted and stable',
        routes.map((r) => r.path).join(', '),
    )

    // 2. requiresAuth is carried per route — the router guard's whole input.
    const login = routes.find((r) => r.path === '/login')
    const insights = routes.find((r) => r.path === '/insights')

    report(login?.meta.requiresAuth === false, 'mount: a public page is not gated')
    report(insights?.meta.requiresAuth === true, 'mount: a protected page is')

    // 3. THE check. Every component must resolve to the RIGHT module.
    //
    //    Asserting merely "not undefined" is too weak — it passes for `() => Promise.resolve(null)`,
    //    i.e. for an emitter that stopped importing anything at all. Measured: that mutant survived
    //    until this compared identities. The fixture stubs each export their own `name`.
    const expectedNames: Record<string, string> = {
        '/login': 'LoginPage',
        '/insights': 'InsightsPage',
        '/insights/details': 'InsightsDetails',
    }

    for (const route of routes) {
        try {
            const loaded = (await route.component()) as { name?: string } | null
            const want = expectedNames[route.path]

            report(
                loaded?.name === want,
                `mount: ${route.path} lazily loads ITS OWN component`,
                `got ${String(loaded?.name)}, want ${String(want)}`,
            )
        } catch (e) {
            report(false, `mount: ${route.path} lazily loads ITS OWN component`, (e as Error).message)
        }
    }

    // 4. Nav is a SUBSET, ordered independently of the route table.
    report(
        equal(navItems.map((n) => n.label), ['Insights', 'Sign in']),
        'mount: nav is in declared order, not path order',
        navItems.map((n) => n.label).join(', '),
    )
    report(
        navItems.every((n) => routes.some((r) => r.path === n.path)),
        'mount: every nav entry points at a real route',
    )
    report(
        !navItems.some((n) => n.path === '/insights/details'),
        'mount: a route with no nav entry stays out of the menu',
    )
    report(navItems.find((n) => n.label === 'Insights')?.icon === 'gauge', 'mount: nav carries its icon')
    report(navItems.find((n) => n.label === 'Sign in')?.icon === null, 'mount: and null when it has none')

    // 5. mountAll over a stand-in target. A real vue-router `Router` satisfies MountTarget
    //    structurally — that is what keeps this module, and this harness, free of Vue.
    const added: SdkRoute[] = []
    mountAll({ addRoute: (route) => added.push(route) })

    report(added.length === routes.length, 'mount: mountAll adds every route')
    report(
        equal(added.map((r) => r.path), routes.map((r) => r.path)),
        'mount: and preserves their order',
    )

    // 6. Selective mounting is possible, i.e. the bulk helper is not a funnel.
    const some: SdkRoute[] = []
    const target = { addRoute: (route: SdkRoute) => some.push(route) }
    for (const route of routes.filter((r) => r.meta.requiresAuth)) target.addRoute(route)

    report(some.length === 2, 'mount: routes can be filtered and mounted individually')
}

/**
 * The stream path's own `credentials`.
 *
 * Separate from the transport checks because a stream does NOT go through `HttpTransport` — it calls
 * `fetch` directly — so nothing the transport learns applies to it. For a cookie session this is the
 * only way a stream authenticates at all: there is no header a caller could set instead.
 */
async function checkSseCredentials(report: Report): Promise<void> {
    let seen: RequestInit | undefined

    const fake: typeof fetch = (_url, init) => {
        seen = init
        return Promise.resolve(
            new Response('data: hi\n\n', {
                status: 200,
                headers: { 'Content-Type': 'text/event-stream' },
            }),
        )
    }

    const drain = async (options: Record<string, unknown>) => {
        for await (const _ of sseStream('http://x/stream', { fetchImpl: fake, ...options })) break
    }

    await drain({})
    report(
        !('credentials' in (seen ?? {})),
        'sse: omits credentials entirely when unset',
        String(seen !== undefined && 'credentials' in seen),
    )

    await drain({ credentials: 'include' })
    report(seen?.credentials === 'include', 'sse: passes credentials through when set')

    // Through `stream(config, ...)`, which is what a generated member calls.
    const config = sdkConfig('http://x', fetchTransport(fake))
    for await (const _e of stream(config, '/s', {}, { fetchImpl: fake, credentials: 'include' })) break
    report(seen?.credentials === 'include', 'sse: and survives the generated `stream()` wrapper')
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
        ['request', checkRequest],
        ['generatedClient', checkGeneratedClient],
        ['sse', checkSseParser],
        ['sseCredentials', checkSseCredentials],
        ['routeAndAcl', checkRouteAndAcl],
        ['authSession', checkAuthSession],
        ['mount', checkMount],
        ['loginFlow', checkLoginFlow],
        ['autoRefresh', checkAutoRefresh],
    ]

    for (const [name, check] of groups) {
        try {
            await check(report)
        } catch (e) {
            report(false, `${name}: threw`, (e as Error).message.split('\n')[0])
        }
    }
}
