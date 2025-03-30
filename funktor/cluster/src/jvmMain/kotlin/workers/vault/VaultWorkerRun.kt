package de.peekandpoke.ktorfx.cluster.workers.vault

import de.peekandpoke.karango.Karango
import de.peekandpoke.ktorfx.cluster.workers.api.WorkerModel
import de.peekandpoke.ktorfx.cluster.workers.domain.WorkerRun
import de.peekandpoke.ultra.slumber.Slumber

@Karango
data class VaultWorkerRun(
    /** The data about the run */
    val run: WorkerRun,
) {
    enum class Status {
        Success,
        Failure,
    }

    @Slumber.Field
    val status: Status
        get() = when (run.result) {
            is WorkerModel.Run.Result.Success -> Status.Success
            is WorkerModel.Run.Result.Failure -> Status.Failure
        }
}
