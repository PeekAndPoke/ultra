package io.peekandpoke.ultra.remote

/**
 * Represents the response received from a remote HTTP request.
 *
 * Platform-specific implementations wrap the underlying HTTP response
 * and expose a uniform API for reading status, body text, and error classification.
 */
interface RemoteResponse {
    /** The originating [RemoteRequest]. */
    val request: RemoteRequest

    /** The response body as a string. */
    val body: String

    /** The numeric HTTP status code (e.g. 200, 404). */
    val status: Int

    /** The textual HTTP status reason phrase. */
    val statusText: String

    /** `true` when the response indicates a successful request (typically 2xx). */
    val ok: Boolean

    /** `true` when the status code is in the 4xx (Client Error) range. */
    val is4xx: Boolean get() = status in 400..499

    /** `true` when the status code is in the 5xx (Server Error) range. */
    val is5xx: Boolean get() = status in 500..599
}
