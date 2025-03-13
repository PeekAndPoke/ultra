package de.peekandpoke.ultra.common.remote

import org.w3c.fetch.Response

class RemoteResponseImpl(
    override val request: RemoteRequest,
    override val body: String,
    private val response: Response
) : RemoteResponse {

    override val status: Int get() = response.status.toInt()

    override val statusText: String get() = response.statusText

    override val ok: Boolean get() = response.ok
}
