package io.peekandpoke.funktor.inspect.cluster.devtools

import io.peekandpoke.ultra.remote.EmptyApiResponse
import io.peekandpoke.ultra.remote.RemoteResponse
import kotlinx.serialization.json.Json

/**
 * Records the [io.peekandpoke.ultra.remote.ApiResponse.Insights] of every API response into
 * [DevtoolsState.RequestHistory] so the dev-tools request list can display them.
 *
 * Registered as an `onResponse` observer on the [io.peekandpoke.ultra.remote.ApiClient.Config]
 * (applied with `onEach` on the response flow).
 */
object DevtoolsResponseObserver {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val serializer = EmptyApiResponse.serializer()

    /** Decodes the envelope (without data) and records its insights. Never throws. */
    fun record(response: RemoteResponse) {
        try {
            val apiResponse = json.decodeFromString(serializer, response.body)

            apiResponse.insights?.apply {
                DevtoolsState.RequestHistory.add(this)
            }
        } catch (e: Throwable) {
            println("Could not observe response: ${e.stackTraceToString()}")
        }
    }
}
