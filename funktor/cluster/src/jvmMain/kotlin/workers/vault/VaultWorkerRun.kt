package de.peekandpoke.funktor.cluster.workers.vault

import de.peekandpoke.funktor.cluster.workers.api.WorkerModel
import de.peekandpoke.funktor.cluster.workers.domain.WorkerRun
import de.peekandpoke.ultra.vault.Vault

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
