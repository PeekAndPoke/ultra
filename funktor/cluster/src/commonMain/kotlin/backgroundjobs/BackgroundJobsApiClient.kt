package de.peekandpoke.ktorfx.cluster.backgroundjobs

import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint.Get
import de.peekandpoke.ultra.common.remote.api
import de.peekandpoke.ultra.common.remote.apiPaged
import de.peekandpoke.ultra.common.remote.call
import kotlinx.coroutines.flow.Flow

class BackgroundJobsApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/background-jobs"

        val ListQueued = Get(
            "$base/queued",
            response = BackgroundJobQueuedModel.serializer().apiPaged(),
        )

        val GetQueued = Get(
            "$base/queued/{id}",
            response = BackgroundJobQueuedModel.serializer().api(),
        )

        val ListArchived = Get(
            "$base/archived",
            response = BackgroundJobArchivedModel.serializer().apiPaged(),
        )

        val GetArchived = Get(
            "$base/archived/{id}",
            response = BackgroundJobArchivedModel.serializer().api(),
        )
    }

    fun listQueued(page: Int, epp: Int): Flow<ApiResponse<Paged<BackgroundJobQueuedModel>>> =
        call(
            ListQueued(
                "page" to page.toString(),
                "epp" to epp.toString()
            )
        )

    fun getQueued(id: String): Flow<ApiResponse<BackgroundJobQueuedModel>> =
        call(
            GetQueued("id" to id)
        )

    fun listArchived(page: Int, epp: Int): Flow<ApiResponse<Paged<BackgroundJobArchivedModel>>> =
        call(
            ListArchived(
                "page" to page.toString(),
                "epp" to epp.toString()
            )
        )

    fun getArchived(id: String): Flow<ApiResponse<BackgroundJobArchivedModel>> =
        call(
            GetArchived("id" to id)
        )
}
