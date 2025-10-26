package de.peekandpoke.funktor.cluster.devtools

import de.peekandpoke.ultra.common.remote.EmptyApiResponse
import de.peekandpoke.ultra.common.remote.RemoteResponse
import de.peekandpoke.ultra.common.remote.ResponseInterceptor
import kotlinx.serialization.json.Json

class DevtoolsApiResponseInterceptor : ResponseInterceptor {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val serializer = EmptyApiResponse.serializer()

    override suspend fun intercept(response: RemoteResponse): RemoteResponse {

        try {
            // try to deserialize the ApiResponse without data
            val apiResponse = json.decodeFromString(serializer, response.body)

            apiResponse.insights?.apply {
                DevtoolsState.RequestHistory.add(this)
            }

        } catch (e: Throwable) {
            println("Could not intercept response: ${e.stackTraceToString()}")
        }

        return response
    }
}
