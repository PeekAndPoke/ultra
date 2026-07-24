package io.peekandpoke.funktor.demo.common.b2b

import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Delete
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Put
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

/**
 * Typed client for the b2b org member-management endpoints. The org is carried in the URL (the
 * tenant boundary — `OrgAwareParam` on the server binds it to the caller's selected session org);
 * `{member}` is the `OrgMember` row id.
 */
class B2bMembersApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/api/b2b/orgs/{org}/members"

        val List = Get(
            uri = base,
            response = OrgMemberModel.serializer().apiList(),
        )

        val ChangeRoles = Put(
            uri = "$base/{member}/roles",
            body = ChangeMemberRolesRequest.serializer(),
            response = OrgMemberModel.serializer().api(),
        )

        val Remove = Delete(
            uri = "$base/{member}",
            response = OrgMemberModel.serializer().api(),
        )
    }

    fun list(org: String): Flow<ApiResponse<List<OrgMemberModel>>> = call(
        List("org" to org)
    )

    fun changeRoles(org: String, member: String, request: ChangeMemberRolesRequest): Flow<ApiResponse<OrgMemberModel>> = call(
        ChangeRoles("org" to org, "member" to member, body = request)
    )

    fun remove(org: String, member: String): Flow<ApiResponse<OrgMemberModel>> = call(
        Remove("org" to org, "member" to member)
    )
}
