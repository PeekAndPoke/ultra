package io.peekandpoke.funktor.cluster.backgroundjobs.api

import io.ktor.server.routing.*
import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobArchivedModel
import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobQueuedModel
import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobResultModel
import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobRetryPolicyModel
import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsApiClient
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobArchived
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobExecutionResult
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobQueued
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobRetryPolicy
import io.peekandpoke.funktor.cluster.cluster
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.jsonPrinter
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.vault.Stored

class BackgroundJobsApi(converter: OutgoingConverter) : ApiRoutes("background-jobs", converter) {

    data class PagingParam(
        val page: Int = 1,
        val epp: Int = 20,
    )

    data class JobIdParam(
        val id: String,
    )

    val listQueued = BackgroundJobsApiClient.ListQueued.mount(PagingParam::class) {
        docs {
            name = "Lists queued jobs"
        }.codeGen {
            funcName = "listQueued"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            val result = cluster.backgroundJobs.listQueuedJobs(page = params.page, epp = params.epp)

            val paged = Paged(
                items = result.map { asApiModel(it) },
                page = params.page,
                epp = params.epp,
                fullItemCount = result.fullCount,
            )

            ApiResponse.ok(paged)
        }
    }

    val getQueued = BackgroundJobsApiClient.GetQueued.mount(JobIdParam::class) {
        docs {
            name = "Get queued job"
        }.codeGen {
            funcName = "getQueued"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            val result = cluster.backgroundJobs.getQueuedJob(id = params.id)

            ApiResponse.okOrNotFound(
                result?.let { asApiModel(it) }
            )
        }
    }

    val listArchived = BackgroundJobsApiClient.ListArchived.mount(PagingParam::class) {
        docs {
            name = "List archived jobs"
        }.codeGen {
            funcName = "listArchived"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            val result = cluster.backgroundJobs
                .listArchivedJobs(page = params.page, epp = params.epp)

            val paged = Paged(
                items = result.map { asApiModel(it) },
                page = params.page,
                epp = params.epp,
                fullItemCount = result.fullCount,
            )

            ApiResponse.ok(paged)
        }
    }

    val getArchived = BackgroundJobsApiClient.GetArchived.mount(JobIdParam::class) {
        docs {
            name = "Get archived job"
        }.codeGen {
            funcName = "getArchived"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            val result = cluster.backgroundJobs.getArchivedJob(id = params.id)

            ApiResponse.okOrNotFound(
                result?.let { asApiModel(it) }
            )
        }
    }

    private fun RoutingContext.asApiModel(job: Stored<BackgroundJobQueued>) = with(job.value) {
        BackgroundJobQueuedModel(
            id = job._key,
            type = type,
            data = jsonPrinter.prettyPrint(data),
            dataHash = dataHash,
            retryPolicy = asApiModel(retryPolicy),
            createdAt = createdAt,
            dueAt = dueAt,
            state = state.toString(),
            results = results.map { asApiModel(it) }
        )
    }

    private fun RoutingContext.asApiModel(job: Stored<BackgroundJobArchived>) = with(job.value) {
        BackgroundJobArchivedModel(
            id = job._key,
            type = type,
            data = jsonPrinter.prettyPrint(data),
            dataHash = dataHash,
            retryPolicy = asApiModel(retryPolicy),
            results = results.map { asApiModel(it) },
            createdAt = createdAt,
            archivedAt = archivedAt,
        )
    }

    private fun RoutingContext.asApiModel(result: BackgroundJobExecutionResult): BackgroundJobResultModel =
        when (result) {
            is BackgroundJobExecutionResult.Success -> BackgroundJobResultModel.Success(
                data = jsonPrinter.prettyPrint(result.data),
                serverId = result.serverId,
                startedAt = result.startedAt,
                endedAt = result.endedAt,
                cpuProfile = result.cpuProfile,
            )

            is BackgroundJobExecutionResult.Failed -> BackgroundJobResultModel.Failed(
                data = jsonPrinter.prettyPrint(result.data),
                serverId = result.serverId,
                startedAt = result.startedAt,
                endedAt = result.endedAt,
                cpuProfile = result.cpuProfile,
            )
        }

    private fun asApiModel(retryPolicy: BackgroundJobRetryPolicy): BackgroundJobRetryPolicyModel = when (retryPolicy) {
        is BackgroundJobRetryPolicy.None -> BackgroundJobRetryPolicyModel.None

        is BackgroundJobRetryPolicy.LinearDelay -> BackgroundJobRetryPolicyModel.LinearDelay(
            delayInMs = retryPolicy.delayInMs,
            maxTries = retryPolicy.maxTries,
        )
    }
}
