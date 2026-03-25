package de.peekandpoke.funktor.cluster.backgroundjobs

import de.peekandpoke.ultra.model.Paged
import de.peekandpoke.ultra.remote.ApiClient
import de.peekandpoke.ultra.remote.ApiResponse
import de.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import de.peekandpoke.ultra.remote.api
import de.peekandpoke.ultra.remote.apiPaged
import de.peekandpoke.ultra.remote.call
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
