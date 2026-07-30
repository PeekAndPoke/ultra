/**
 * Server-Sent Events over `fetch`, with a spec-faithful frame parser.
 *
 * HAND-WRITTEN AND CHECKED IN — not generated.
 *
 * **Why not `EventSource`?** It cannot send an `Authorization` header — that is not in the spec, and
 * there is no workaround short of putting the token in the query string, where it lands in access
 * logs. `ApiRoute.Sse` routes pass through the same auth floor as every other route, so an
 * authenticated stream is simply unreachable via `EventSource`. Reading the stream through `fetch`
 * costs this file and buys ordinary header-based auth.
 *
 * Two deliberate differences from `EventSource`:
 *
 * - **No automatic reconnect.** `EventSource` silently retries; that is the wrong default for a
 *   generated client, where a dropped stream after an auth change should surface. The server's
 *   suggested backoff is passed through as [SseEvent.retry] so callers can reconnect themselves.
 * - **A non-2xx response throws** [SseError], unlike the request path in `http.ts`, which returns
 *   non-2xx as a normal envelope. There is no stream to hand back in that case, and an async
 *   generator has nowhere to put an envelope.
 *
 * Note that `ApiRoute.Sse` declares `responseType: TypeRef<Unit>` — SSE routes carry no typed event
 * payload — so events are delivered as raw `data` strings and callers parse them with whatever schema
 * applies.
 */
import { type SdkConfig } from './client.ts'
import { type UrlParam, buildUrl } from './http.ts'

/** One dispatched event. */
export interface SseEvent {
    /** The `event:` field, or `'message'` when the server sent none — the SSE default. */
    event: string
    /** The `data:` field. Multiple `data:` lines in one frame are joined with newlines. */
    data: string
    /** The most recent `id:` seen, which per spec persists across subsequent events. */
    id?: string
    /** The most recent `retry:` seen, in milliseconds — the server's suggested reconnect delay. */
    retry?: number
}

/**
 * Thrown when the stream could not be opened.
 *
 * The fields are assigned explicitly rather than declared as constructor parameter properties: those
 * are not erasable syntax, so they break every type-stripping toolchain — Node's own TypeScript
 * support, and esbuild under `verbatimModuleSyntax`. Nothing in this runtime may use them.
 */
export class SseError extends Error {
    readonly status: number
    readonly statusText: string
    /** Size of the response body in characters. */
    readonly bodyLength: number
    /**
     * The raw body — **only when `SdkConfig.debug` is set**, otherwise `undefined`.
     *
     * Same rule as `ApiProtocolError`: errors escape the application into Sentry and console logs,
     * and Sentry serialises an Error's own enumerable properties. A failed stream response is
     * commonly a proxy error page naming internal hosts, or an auth error echoing a token.
     */
    readonly body?: string

    constructor(status: number, statusText: string, bodyLength: number, body?: string) {
        super(
            `SSE request failed: ${status} ${statusText} (${bodyLength} characters). ` +
                `Body withheld; set debug: true on SdkConfig to include it.`,
        )

        this.name = 'SseError'
        this.status = status
        this.statusText = statusText
        this.bodyLength = bodyLength

        if (body !== undefined) {
            this.body = body
        }
    }
}

/**
 * Incremental parser for the `text/event-stream` wire format.
 *
 * Exposed separately from [sseStream] because it is the part with edge cases worth testing directly:
 * frames split across chunk boundaries, all three line terminators, comments, and frames that carry
 * no data and must not be dispatched.
 */
export class SseParser {
    private buffer = ''
    private data: string[] = []
    private eventType = ''
    private lastEventId: string | undefined
    private retry: number | undefined

    /** Feeds a decoded chunk in and returns whatever events completed within it. */
    push(chunk: string): SseEvent[] {
        this.buffer += chunk

        const events: SseEvent[] = []
        const terminator = /\r\n|\n|\r/g

        let consumed = 0
        let match: RegExpExecArray | null

        while ((match = terminator.exec(this.buffer)) !== null) {
            // A lone CR at the very end may be the first half of a CRLF split across chunks.
            if (match[0] === '\r' && match.index + 1 === this.buffer.length) {
                break
            }

            const line = this.buffer.slice(consumed, match.index)
            consumed = match.index + match[0].length

            const event = this.consumeLine(line)

            if (event !== null) {
                events.push(event)
            }
        }

        this.buffer = this.buffer.slice(consumed)

        return events
    }

    private consumeLine(line: string): SseEvent | null {
        // A blank line ends the frame.
        if (line === '') {
            return this.dispatch()
        }

        // A line starting with a colon is a comment — servers send them as keep-alives.
        if (line.startsWith(':')) {
            return null
        }

        const colon = line.indexOf(':')
        const field = colon === -1 ? line : line.slice(0, colon)
        const raw = colon === -1 ? '' : line.slice(colon + 1)
        // Exactly one leading space is part of the framing, not of the value.
        const value = raw.startsWith(' ') ? raw.slice(1) : raw

        switch (field) {
            case 'event':
                this.eventType = value
                break
            case 'data':
                this.data.push(value)
                break
            case 'id':
                // Per spec an id containing NUL is ignored rather than treated as a reset.
                if (!value.includes(String.fromCharCode(0))) {
                    this.lastEventId = value
                }
                break
            case 'retry':
                if (/^\d+$/.test(value)) {
                    this.retry = Number(value)
                }
                break
            default:
                // Unknown fields are ignored.
                break
        }

        return null
    }

    private dispatch(): SseEvent | null {
        // A frame with no data is not dispatched, but it does reset the event type.
        if (this.data.length === 0) {
            this.eventType = ''
            return null
        }

        const event: SseEvent = {
            event: this.eventType === '' ? 'message' : this.eventType,
            data: this.data.join('\n'),
        }

        if (this.lastEventId !== undefined) {
            event.id = this.lastEventId
        }

        if (this.retry !== undefined) {
            event.retry = this.retry
        }

        this.data = []
        this.eventType = ''

        return event
    }
}

/**
 * Opens the stream for a generated endpoint.
 *
 * The counterpart of `request` in `client.ts`, and the one line every generated SSE member reduces
 * to. It takes the whole [SdkConfig] for symmetry with `request`, though only `baseUrl` is used —
 * **an SSE stream does not go through `config.transport`.** `sseStream` calls `fetch` directly,
 * because the response is consumed as a byte stream rather than as a string, which the transport
 * interface has no way to express. Auth is therefore NOT inherited from a transport wrapper here:
 * pass it per call via [SseOptions.headers].
 */
export function stream(
    config: SdkConfig,
    pattern: string,
    params: { path?: Record<string, UrlParam>; query?: Record<string, UrlParam> } = {},
    options: SseOptions = {},
): AsyncGenerator<SseEvent> {
    return sseStream(
        buildUrl(config.baseUrl, pattern, params.path ?? {}, params.query ?? {}),
        // The config's flag governs both paths, so a caller does not have to remember it twice; an
        // explicit per-call `debug` still wins.
        { debug: config.debug, ...options },
    )
}

/** Options for [sseStream]. */
export interface SseOptions {
    /** Extra request headers — this is where `Authorization` goes. */
    headers?: Record<string, string>
    /** Aborts the stream. Ending the `for await` loop also closes it. */
    signal?: AbortSignal
    /** The `fetch` to use — inject one to test. */
    fetchImpl?: typeof fetch
    /** Attach the raw body to a thrown [SseError]. Off by default; see `SdkConfig.debug`. */
    debug?: boolean
}

/**
 * Opens [url] as an event stream and yields events until the server closes it or the caller stops
 * consuming.
 *
 * ```ts
 * for await (const event of sseStream(url, { headers: { Authorization: `Bearer ${token}` } })) {
 *     console.log(event.event, JSON.parse(event.data))
 * }
 * ```
 *
 * @throws SseError when the stream could not be opened
 */
export async function* sseStream(url: string, options: SseOptions = {}): AsyncGenerator<SseEvent> {
    const fetchImpl = options.fetchImpl ?? globalThis.fetch

    const response = await fetchImpl(url, {
        method: 'GET',
        headers: {
            Accept: 'text/event-stream',
            'Cache-Control': 'no-cache',
            ...options.headers,
        },
        signal: options.signal,
    })

    if (!response.ok || response.body === null) {
        const text = await response.text()

        throw new SseError(response.status, response.statusText, text.length, options.debug ? text : undefined)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    const parser = new SseParser()

    try {
        while (true) {
            const { done, value } = await reader.read()

            if (done) {
                break
            }

            for (const event of parser.push(decoder.decode(value, { stream: true }))) {
                yield event
            }
        }
    } finally {
        // Reached on `break`/`return` out of the caller's loop too — releases the connection.
        await reader.cancel().catch(() => undefined)
    }
}
