package de.peekandpoke.ultra.common.remote

interface RemoteResponse {
    val request: RemoteRequest
    val body: String
    val status: Int
    val statusText: String
    val ok: Boolean

    val is4xx: Boolean get() = status in 400..499
    val is5xx: Boolean get() = status in 500..599
}
