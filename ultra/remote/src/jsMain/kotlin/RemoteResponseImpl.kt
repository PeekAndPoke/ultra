package io.peekandpoke.ultra.remote

import org.w3c.fetch.Response

/**
 * JS-platform implementation of [RemoteResponse] backed by a browser Fetch API [Response].
 */
class RemoteResponseImpl(
    override val request: RemoteRequest,
    override val body: String,
    private val response: Response
) : RemoteResponse {

    override val status: Int get() = response.status.toInt()

    override val statusText: String get() = response.statusText

    override val ok: Boolean get() = response.ok
}
