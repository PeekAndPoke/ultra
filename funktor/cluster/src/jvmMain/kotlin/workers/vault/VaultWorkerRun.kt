package io.peekandpoke.funktor.cluster.workers.vault

import io.peekandpoke.funktor.cluster.workers.domain.WorkerRun
import io.peekandpoke.funktor.inspect.cluster.workers.api.WorkerModel
import io.peekandpoke.ultra.vault.Vault

@Vault
data class VaultWorkerRun(
    /** The data about the run */
    val run: WorkerRun,
) {
    enum class Status {
        Success,
        Failure,
    }

    @Vault.Field
    val status: Status
        get() = when (run.result) {
            is WorkerModel.Run.Result.Success -> Status.Success
            is WorkerModel.Run.Result.Failure -> Status.Failure
        }
}
