package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

// Background Jobs
@Serializable
data class QueueJobRequest(
    val text: String = "Demo job",
    val delayMs: Long = 500,
    val shouldFail: Boolean = false,
)

@Serializable
data class QueueJobResponse(
    val queued: Boolean,
    val message: String,
)

@Serializable
data class JobInfo(
    val id: String,
    val jobType: String,
    val status: String,
    val createdAt: String,
)

// File Depot
@Serializable
data class DepotRepoInfo(
    val name: String,
    val type: String,
    val location: String,
)

@Serializable
data class DepotFileInfo(
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val sizeBytes: Long? = null,
)

@Serializable
data class DepotUploadRequest(
    val repo: String,
    val path: String,
    val contentBase64: String,
)

@Serializable
data class DepotUploadResponse(
    val success: Boolean,
    val path: String,
)

// Key-Value Storage
@Serializable
data class StorageSaveRequest(
    val category: String,
    val dataId: String,
    val jsonValue: String,
)

@Serializable
data class StorageEntry(
    val id: String,
    val category: String,
    val dataId: String,
    val createdAt: String,
    val updatedAt: String,
)

// Distributed Locks
@Serializable
data class LockAcquireRequest(
    val key: String = "demo-lock",
    val holdForMs: Long = 3000,
)

@Serializable
data class LockAcquireResponse(
    val acquired: Boolean,
    val message: String,
)

@Serializable
data class LockInfo(
    val key: String,
    val serverId: String,
    val createdAt: String,
)

// Workers
@Serializable
data class WorkerInfo(
    val id: String,
    val lastRunAt: String?,
    val lastRunResult: String?,
    val totalRuns: Int,
)
