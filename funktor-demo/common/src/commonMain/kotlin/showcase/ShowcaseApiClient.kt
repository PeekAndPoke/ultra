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
        val PostRetryDemo = TypedApiEndpoint.Post(
            uri = "$BASE/core/retry-demo",
            body = RetryDemoRequest.serializer(),
            response = RetryDemoResponse.serializer().api(),
        )

        // REST showcase endpoints
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

        // Auth showcase endpoints
        val GetAuthRuleChecks = TypedApiEndpoint.Get(
            uri = "$BASE/auth-rules/check",
            response = AuthRuleCheckResult.serializer().apiList(),
        )

        // Cluster showcase endpoints
        val PostQueueJob = TypedApiEndpoint.Post(
            uri = "$BASE/cluster/jobs/queue",
            body = QueueJobRequest.serializer(),
            response = QueueJobResponse.serializer().api(),
        )

        val GetQueuedJobs = TypedApiEndpoint.Get(
            uri = "$BASE/cluster/jobs/queued",
            response = JobInfo.serializer().apiList(),
        )

        val GetArchivedJobs = TypedApiEndpoint.Get(
            uri = "$BASE/cluster/jobs/archived",
            response = JobInfo.serializer().apiList(),
        )

        val GetDepotRepos = TypedApiEndpoint.Get(
            uri = "$BASE/cluster/depot/repos",
            response = DepotRepoInfo.serializer().apiList(),
        )

        val GetDepotFiles = TypedApiEndpoint.Get(
            uri = "$BASE/cluster/depot/files/{repo}",
            response = DepotFileInfo.serializer().apiList(),
        )

        val PostDepotUpload = TypedApiEndpoint.Post(
            uri = "$BASE/cluster/depot/upload",
            body = DepotUploadRequest.serializer(),
            response = DepotUploadResponse.serializer().api(),
        )

        val GetStorageEntries = TypedApiEndpoint.Get(
            uri = "$BASE/cluster/storage/data/list",
            response = StorageEntry.serializer().apiList(),
        )

        val PostStorageSave = TypedApiEndpoint.Post(
            uri = "$BASE/cluster/storage/data/save",
            body = StorageSaveRequest.serializer(),
            response = StorageEntry.serializer().api(),
        )

        val GetActiveLocks = TypedApiEndpoint.Get(
            uri = "$BASE/cluster/locks/list",
            response = LockInfo.serializer().apiList(),
        )

        val PostAcquireLock = TypedApiEndpoint.Post(
            uri = "$BASE/cluster/locks/acquire",
            body = LockAcquireRequest.serializer(),
            response = LockAcquireResponse.serializer().api(),
        )

        val GetWorkers = TypedApiEndpoint.Get(
            uri = "$BASE/cluster/workers/list",
            response = WorkerInfo.serializer().apiList(),
        )

        // Messaging showcase endpoints
        val PostSendTestEmail = TypedApiEndpoint.Post(
            uri = "$BASE/messaging/send",
            body = SendTestEmailRequest.serializer(),
            response = SendTestEmailResponse.serializer().api(),
        )

        val GetSentMessages = TypedApiEndpoint.Get(
            uri = "$BASE/messaging/sent",
            response = SentMessageInfo.serializer().apiList(),
        )

        val GetEmailSenderInfo = TypedApiEndpoint.Get(
            uri = "$BASE/messaging/sender-info",
            response = EmailSenderInfo.serializer().api(),
        )

        // SSE showcase endpoints
        val SseClock = TypedApiEndpoint.Sse(
            uri = "$BASE/realtime/sse/clock",
        )

        val SseMetrics = TypedApiEndpoint.Sse(
            uri = "$BASE/realtime/sse/metrics",
        )
    }

    // Core showcase calls

    fun postRetryDemo(request: RetryDemoRequest): Flow<ApiResponse<RetryDemoResponse>> = call(
        PostRetryDemo(body = request)
    )

    // REST showcase calls

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

    // Auth showcase calls

    fun getAuthRuleChecks(): Flow<ApiResponse<List<AuthRuleCheckResult>>> = call(
        GetAuthRuleChecks()
    )

    // Cluster showcase calls

    fun queueJob(request: QueueJobRequest): Flow<ApiResponse<QueueJobResponse>> = call(
        PostQueueJob(body = request)
    )

    fun getQueuedJobs(): Flow<ApiResponse<List<JobInfo>>> = call(
        GetQueuedJobs()
    )

    fun getArchivedJobs(): Flow<ApiResponse<List<JobInfo>>> = call(
        GetArchivedJobs()
    )

    fun getDepotRepos(): Flow<ApiResponse<List<DepotRepoInfo>>> = call(
        GetDepotRepos()
    )

    fun getDepotFiles(repo: String): Flow<ApiResponse<List<DepotFileInfo>>> = call(
        GetDepotFiles("repo" to repo)
    )

    fun uploadToDepot(request: DepotUploadRequest): Flow<ApiResponse<DepotUploadResponse>> = call(
        PostDepotUpload(body = request)
    )

    fun getStorageEntries(): Flow<ApiResponse<List<StorageEntry>>> = call(
        GetStorageEntries()
    )

    fun saveStorageEntry(request: StorageSaveRequest): Flow<ApiResponse<StorageEntry>> = call(
        PostStorageSave(body = request)
    )

    fun getActiveLocks(): Flow<ApiResponse<List<LockInfo>>> = call(
        GetActiveLocks()
    )

    fun acquireLock(request: LockAcquireRequest): Flow<ApiResponse<LockAcquireResponse>> = call(
        PostAcquireLock(body = request)
    )

    fun getWorkers(): Flow<ApiResponse<List<WorkerInfo>>> = call(
        GetWorkers()
    )

    // Messaging showcase calls

    fun sendTestEmail(request: SendTestEmailRequest): Flow<ApiResponse<SendTestEmailResponse>> = call(
        PostSendTestEmail(body = request)
    )

    fun getSentMessages(): Flow<ApiResponse<List<SentMessageInfo>>> = call(
        GetSentMessages()
    )

    fun getEmailSenderInfo(): Flow<ApiResponse<EmailSenderInfo>> = call(
        GetEmailSenderInfo()
    )
}
