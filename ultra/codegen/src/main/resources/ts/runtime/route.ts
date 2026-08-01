/**
 * Route identity — what a generated client member knows about itself.
 *
 * A generated member is normally just a function, which means it cannot answer "which endpoint am
 * I?". That question has one important caller: the per-user access matrix
 * (`UserApiAccessMatrix`, queried through `ApiAcl` in `runtime/acl.ts`) is keyed by METHOD and URI
 * PATTERN, so a frontend can only ask "may this user call this?" if the member carries both.
 *
 * The answer is a callable object: a function with properties attached. It stays callable, and it
 * describes itself.
 *
 * ```ts
 * await api.getEvent({ id: 'abc' })   // still a normal call
 * api.getEvent.method                  // 'GET'
 * acl.canAccess(api.getEvent)          // and now queryable
 * ```
 */

/**
 * The HTTP methods a generated route can use.
 *
 * A CLOSED union, so `member.method` is worth reading and a typo cannot survive. Widening it is a
 * deliberate act: `TsClientSpec.Endpoint` refuses any other method at GENERATION time, naming the
 * route, rather than emitting TypeScript that fails to compile inside output nobody can edit.
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD' | 'OPTIONS'

/**
 * What identifies an endpoint in the access matrix.
 *
 * [uri] is the route PATTERN, placeholders included — `/funktor-conf/events/{id}`, never a filled-in
 * URL. That is the form the server puts in the matrix, so it is the form that matches.
 */
export interface RouteRef {
    readonly method: HttpMethod
    readonly uri: string
}

/** A callable endpoint that also describes itself. */
export type Route<F extends (...args: never[]) => unknown> = F & RouteRef

/**
 * Attaches a route's identity to the call that performs it.
 *
 * `F` is INFERRED from [call], so the wrap is transparent: parameter types and the awaited payload
 * type both survive it. That does not depend on the bound — `(...args: any[]) => any` and `Function`
 * preserve inference just as well, measured, not assumed. `(...args: never[]) => unknown` is chosen
 * because it is the strict form: it admits any function by contravariance without introducing `any`
 * into a file every generated SDK carries.
 *
 * `Object.assign` mutates [call] rather than wrapping it. That is safe HERE, and only here, because
 * generated members are arrow-function class FIELDS: each instance builds its own function object,
 * so there is no shared prototype to contaminate. It is also what keeps `const { getEvent } = api`
 * working, which is the Vue-composable idiom this SDK is consumed through.
 */
export function route<F extends (...args: never[]) => unknown>(
    method: HttpMethod,
    uri: string,
    call: F,
): Route<F> {
    return Object.assign(call, { method, uri })
}
