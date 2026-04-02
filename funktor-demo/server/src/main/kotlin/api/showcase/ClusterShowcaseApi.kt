package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.cluster.cluster
import io.peekandpoke.funktor.cluster.depot.domain.DepotItem
import io.peekandpoke.funktor.cluster.storage.RandomDataStorage
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.demo.common.showcase.DepotFileInfo
import io.peekandpoke.funktor.demo.common.showcase.DepotRepoInfo
import io.peekandpoke.funktor.demo.common.showcase.DepotUploadResponse
import io.peekandpoke.funktor.demo.common.showcase.JobInfo
import io.peekandpoke.funktor.demo.common.showcase.LockAcquireResponse
import io.peekandpoke.funktor.demo.common.showcase.LockInfo
import io.peekandpoke.funktor.demo.common.showcase.QueueJobResponse
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.demo.common.showcase.StorageEntry
import io.peekandpoke.funktor.demo.common.showcase.WorkerInfo
import io.peekandpoke.funktor.demo.server.showcase.DemoBackgroundJobHandler
import io.peekandpoke.funktor.inspect.cluster.workers.api.WorkerModel
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class ClusterShowcaseApi : ApiRoutes("showcase-cluster") {

    data class RepoParams(val repo: String)

    // Background Jobs

    val queueJob = ShowcaseApiClient.PostQueueJob.mount {
        docs {
            name = "Queue a demo background job"
        }.codeGen {
            funcName = "queueJob"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            val handler = call.kontainer.get(DemoBackgroundJobHandler::class)

            handler.queue(
                data = DemoBackgroundJobHandler.Input(
                    text = body.text,
                    execDelayMs = body.delayMs,
                    shouldFail = body.shouldFail,
                )
            )

            ApiResponse.ok(QueueJobResponse(queued = true, message = "Job queued successfully"))
        }
    }

    val getQueuedJobs = ShowcaseApiClient.GetQueuedJobs.mount {
        docs {
            name = "List queued jobs"
        }.codeGen {
            funcName = "getQueuedJobs"
        }.authorize {
            public()
        }.handle {
            val jobs = cluster.backgroundJobs.listQueuedJobs(page = 1, epp = 20)

            val result = jobs.map {
                JobInfo(
                    id = it._key,
                    jobType = it.value.type,
                    status = "queued",
                    createdAt = it.value.createdAt?.toIsoString() ?: "?",
                )
            }

            ApiResponse.ok(result)
        }
    }

    val getArchivedJobs = ShowcaseApiClient.GetArchivedJobs.mount {
        docs {
            name = "List archived jobs"
        }.codeGen {
            funcName = "getArchivedJobs"
        }.authorize {
            public()
        }.handle {
            val jobs = cluster.backgroundJobs.listArchivedJobs(page = 1, epp = 20)

            val result = jobs.map {
                JobInfo(
                    id = it._key,
                    jobType = it.value.type,
                    status = if (it.value.didFinallySucceed()) "succeeded" else "failed",
                    createdAt = it.value.createdAt?.toIsoString() ?: "?",
                )
            }

            ApiResponse.ok(result)
        }
    }

    // File Depot

    val getDepotRepos = ShowcaseApiClient.GetDepotRepos.mount {
        docs {
            name = "List depot repositories"
        }.codeGen {
            funcName = "getDepotRepos"
        }.authorize {
            public()
        }.handle {
            val repos = cluster.depot.getRepos()

            val result = repos.map {
                DepotRepoInfo(
                    name = it.name,
                    type = it.type,
                    location = it.location,
                )
            }

            ApiResponse.ok(result)
        }
    }

    val getDepotFiles = ShowcaseApiClient.GetDepotFiles.mount(RepoParams::class) {
        docs {
            name = "List files in depot repository"
        }.codeGen {
            funcName = "getDepotFiles"
        }.authorize {
            public()
        }.handle { params ->
            val repo = cluster.depot.getRepo(params.repo)
            val items = repo?.listItems() ?: emptyList()

            val result = items.map { item ->
                DepotFileInfo(
                    name = item.name,
                    path = item.path,
                    isFolder = item is DepotItem.Folder,
                    sizeBytes = (item as? DepotItem.File)?.size,
                )
            }

            ApiResponse.ok(result)
        }
    }

    val uploadToDepot = ShowcaseApiClient.PostDepotUpload.mount {
        docs {
            name = "Upload file to depot"
        }.codeGen {
            funcName = "uploadToDepot"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            val repo = cluster.depot.getRepo(body.repo)

            if (repo == null) {
                ApiResponse.ok(DepotUploadResponse(success = false, path = body.path))
            } else {
                val content = java.util.Base64.getDecoder().decode(body.contentBase64)
                repo.putFile(body.path, content)
                ApiResponse.ok(DepotUploadResponse(success = true, path = body.path))
            }
        }
    }

    // Key-Value Storage

    val getStorageEntries = ShowcaseApiClient.GetStorageEntries.mount {
        docs {
            name = "List random data storage entries"
        }.codeGen {
            funcName = "getStorageEntries"
        }.authorize {
            public()
        }.handle {
            val entries = cluster.storage.randomData.list(search = "", page = 1, epp = 20)

            val result = entries.map {
                StorageEntry(
                    id = it._key,
                    category = it.value.category,
                    dataId = it.value.dataId,
                    createdAt = it.value.createdAt.toIsoString(),
                    updatedAt = it.value.updatedAt.toIsoString(),
                )
            }

            ApiResponse.ok(result)
        }
    }

    val saveStorageEntry = ShowcaseApiClient.PostStorageSave.mount {
        docs {
            name = "Save data to random data storage"
        }.codeGen {
            funcName = "saveStorageEntry"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            val category = RandomDataStorage.category<String>(body.category)
            val saved = cluster.storage.randomData.save(category, body.dataId, body.jsonValue)

            ApiResponse.ok(
                StorageEntry(
                    id = "${body.category}/${body.dataId}",
                    category = body.category,
                    dataId = body.dataId,
                    createdAt = saved.createdAt.toIsoString(),
                    updatedAt = saved.updatedAt.toIsoString(),
                )
            )
        }
    }

    // Distributed Locks

    val getActiveLocks = ShowcaseApiClient.GetActiveLocks.mount {
        docs {
            name = "List active global locks"
        }.codeGen {
            funcName = "getActiveLocks"
        }.authorize {
            public()
        }.handle {
            val locks = cluster.locks.global.list()

            val result = locks.map {
                LockInfo(
                    key = it.key,
                    serverId = it.serverId,
                    createdAt = it.created.toIsoString(),
                )
            }

            ApiResponse.ok(result)
        }
    }

    val acquireLock = ShowcaseApiClient.PostAcquireLock.mount {
        docs {
            name = "Acquire a demo lock"
        }.codeGen {
            funcName = "acquireLock"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            try {
                cluster.locks.global.lock(body.key, timeout = 5.seconds) {
                    delay(body.holdForMs)
                }
                ApiResponse.ok(
                    LockAcquireResponse(
                        acquired = true,
                        message = "Lock acquired and released after ${body.holdForMs}ms"
                    )
                )
            } catch (e: Exception) {
                ApiResponse.ok(LockAcquireResponse(acquired = false, message = "Failed: ${e.message}"))
            }
        }
    }

    // Workers

    val getWorkers = ShowcaseApiClient.GetWorkers.mount {
        docs {
            name = "List workers with stats"
        }.codeGen {
            funcName = "getWorkers"
        }.authorize {
            public()
        }.handle {
            val stats = cluster.workers.stats()

            val result = stats.map { worker ->
                val lastRun = worker.runs.lastOrNull()

                WorkerInfo(
                    id = worker.id,
                    lastRunAt = lastRun?.begin?.toIsoString(),
                    lastRunResult = lastRun?.result?.let { r ->
                        when (r) {
                            is WorkerModel.Run.Result.Success -> "success"
                            is WorkerModel.Run.Result.Failure -> "failure: ${r.message}"
                        }
                    },
                    totalRuns = worker.runs.size,
                )
            }

            ApiResponse.ok(result)
        }
    }
}
