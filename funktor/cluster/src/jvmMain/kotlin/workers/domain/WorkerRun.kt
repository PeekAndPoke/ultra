package de.peekandpoke.funktor.cluster.workers.domain

import de.peekandpoke.funktor.cluster.workers.api.WorkerModel

data class WorkerRun(
    /** The id of the worker */
    val workerId: String,
    /** The id of the server executing the worker */
    val serverId: String,
    /** The start of the run in epoch millis */
    val beginTs: Long,
    /** The end of the run in epoch millis */
    val endTs: Long,
    /** The result of the run */
    val result: WorkerModel.Run.Result,
)
