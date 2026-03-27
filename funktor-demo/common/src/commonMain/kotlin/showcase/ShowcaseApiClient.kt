package io.peekandpoke.funktor.demo.common.showcase

import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

class ShowcaseApiClient(config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/showcase"

        // Core showcase endpoints
        val GetLifecycleHooks = TypedApiEndpoint.Get(
            uri = "$BASE/core/lifecycle-hooks",
            response = LifecycleHookInfo.serializer().apiList(),
        )

        val GetConfigInfo = TypedApiEndpoint.Get(
            uri = "$BASE/core/config-info",
            response = ConfigInfoEntry.serializer().apiList(),
        )

        val GetCliCommands = TypedApiEndpoint.Get(
            uri = "$BASE/core/cli-commands",
            response = CliCommandInfo.serializer().apiList(),
        )

        val GetFixtures = TypedApiEndpoint.Get(
            uri = "$BASE/core/fixtures-info",
            response = FixtureInfo.serializer().apiList(),
        )

        val GetRepairs = TypedApiEndpoint.Get(
            uri = "$BASE/core/repairs-info",
            response = RepairInfo.serializer().apiList(),
        )

        val PostRetryDemo = TypedApiEndpoint.Post(
            uri = "$BASE/core/retry-demo",
            body = RetryDemoRequest.serializer(),
            response = RetryDemoResponse.serializer().api(),
        )

        // REST showcase endpoints
        val GetAllEndpoints = TypedApiEndpoint.Get(
            uri = "$BASE/rest/all-endpoints",
            response = EndpointInfo.serializer().apiList(),
        )

        val GetPlain = TypedApiEndpoint.Get(
            uri = "$BASE/rest/plain",
            response = ServerTimeResponse.serializer().api(),
        )

        val GetEcho = TypedApiEndpoint.Get(
            uri = "$BASE/rest/echo/{message}",
            response = EchoResponse.serializer().api(),
        )

        val PostTransform = TypedApiEndpoint.Post(
            uri = "$BASE/rest/transform",
            body = TransformRequest.serializer(),
            response = TransformResponse.serializer().api(),
        )

        val PutItem = TypedApiEndpoint.Put(
            uri = "$BASE/rest/items/{id}",
            body = UpdateItemRequest.serializer(),
            response = ItemResponse.serializer().api(),
        )
    }

    // Core showcase calls

    fun getLifecycleHooks(): Flow<ApiResponse<List<LifecycleHookInfo>>> = call(
        GetLifecycleHooks()
    )

    fun getConfigInfo(): Flow<ApiResponse<List<ConfigInfoEntry>>> = call(
        GetConfigInfo()
    )

    fun getCliCommands(): Flow<ApiResponse<List<CliCommandInfo>>> = call(
        GetCliCommands()
    )

    fun getFixtures(): Flow<ApiResponse<List<FixtureInfo>>> = call(
        GetFixtures()
    )

    fun getRepairs(): Flow<ApiResponse<List<RepairInfo>>> = call(
        GetRepairs()
    )

    fun postRetryDemo(request: RetryDemoRequest): Flow<ApiResponse<RetryDemoResponse>> = call(
        PostRetryDemo(body = request)
    )

    // REST showcase calls

    fun getAllEndpoints(): Flow<ApiResponse<List<EndpointInfo>>> = call(
        GetAllEndpoints()
    )

    fun getPlain(): Flow<ApiResponse<ServerTimeResponse>> = call(
        GetPlain()
    )

    fun getEcho(message: String): Flow<ApiResponse<EchoResponse>> = call(
        GetEcho("message" to message)
    )

    fun postTransform(request: TransformRequest): Flow<ApiResponse<TransformResponse>> = call(
        PostTransform(body = request)
    )

    fun putItem(id: String, request: UpdateItemRequest): Flow<ApiResponse<ItemResponse>> = call(
        PutItem("id" to id, body = request)
    )
}
