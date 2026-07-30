package io.peekandpoke.funktor.saas.api

import io.peekandpoke.funktor.saas.model.BranchModel
import io.peekandpoke.funktor.saas.model.OrgModel
import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Post
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Put
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Body for creating an organisation. */
@Serializable
data class CreateOrgRequest(
    val slug: String,
    val name: String,
    val status: OrgStatus = OrgStatus.Active,
    val branches: List<BranchModel> = emptyList(),
)

/** Body for updating an organisation. The slug is immutable and therefore not part of the request. */
@Serializable
data class UpdateOrgRequest(
    val name: String,
    val status: OrgStatus,
    val branches: List<BranchModel> = emptyList(),
)

/** Typed client for the organisation CRUD endpoints. */
class OrgsApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/api/orgs"

        val List = Get(
            uri = base,
            response = OrgModel.serializer().apiList(),
        )

        val Get = Get(
            uri = "$base/{id}",
            response = OrgModel.serializer().api(),
        )

        val Create = Post(
            uri = base,
            body = CreateOrgRequest.serializer(),
            response = OrgModel.serializer().api(),
        )

        val Update = Put(
            uri = "$base/{id}",
            body = UpdateOrgRequest.serializer(),
            response = OrgModel.serializer().api(),
        )
    }

    fun list(): Flow<ApiResponse<List<OrgModel>>> = call(
        List()
    )

    fun get(id: String): Flow<ApiResponse<OrgModel>> = call(
        Get("id" to id)
    )

    fun create(request: CreateOrgRequest): Flow<ApiResponse<OrgModel>> = call(
        Create(body = request)
    )

    fun update(id: String, request: UpdateOrgRequest): Flow<ApiResponse<OrgModel>> = call(
        Update("id" to id, body = request)
    )
}
