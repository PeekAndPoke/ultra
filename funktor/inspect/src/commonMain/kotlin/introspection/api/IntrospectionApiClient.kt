package io.peekandpoke.funktor.inspect.introspection.api

import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Post
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

class IntrospectionApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/funktor/introspection"

        val GetLifecycleHooks = Get(
            uri = "$base/lifecycle-hooks",
            response = LifecycleHookInfo.serializer().apiList(),
        )

        val GetConfigInfo = Get(
            uri = "$base/config-info",
            response = ConfigInfoEntry.serializer().apiList(),
        )

        val GetCliCommands = Get(
            uri = "$base/cli-commands",
            response = CliCommandInfo.serializer().apiList(),
        )

        val GetFixtures = Get(
            uri = "$base/fixtures",
            response = FixtureInfo.serializer().apiList(),
        )

        val GetRepairs = Get(
            uri = "$base/repairs",
            response = RepairInfo.serializer().apiList(),
        )

        val GetAllEndpoints = Get(
            uri = "$base/endpoints",
            response = EndpointInfo.serializer().apiList(),
        )

        val GetAuthRealms = Get(
            uri = "$base/auth-realms",
            response = AuthRealmInfo.serializer().apiList(),
        )

        val PostValidatePassword = Post(
            uri = "$base/auth-realms/validate-password",
            body = PasswordValidationRequest.serializer(),
            response = PasswordValidationResponse.serializer().api(),
        )

        val GetAppLifecycle = Get(
            uri = "$base/system",
            response = AppLifecycleInfo.serializer().api(),
        )
    }

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

    fun getAllEndpoints(): Flow<ApiResponse<List<EndpointInfo>>> = call(
        GetAllEndpoints()
    )

    fun getAuthRealms(): Flow<ApiResponse<List<AuthRealmInfo>>> = call(
        GetAuthRealms()
    )

    fun validatePassword(request: PasswordValidationRequest): Flow<ApiResponse<PasswordValidationResponse>> = call(
        PostValidatePassword(body = request)
    )

    fun getAppLifecycle(): Flow<ApiResponse<AppLifecycleInfo>> = call(
        GetAppLifecycle()
    )
}
