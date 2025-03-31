package de.peekandpoke.funktor.cluster.backgroundjobs.example

import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import de.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobQueued
import de.peekandpoke.ultra.common.reflection.kType
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName

class ExampleBackgroundJobHandler01(
    backgroundJobs: Lazy<BackgroundJobs>,
) :
    BackgroundJobs.Handler<ExampleBackgroundJobHandler01.Input, ExampleBackgroundJobHandler01.Output>(
        backgroundJobs,
        kType()
    ) {

    data class Input(
        val execDelayMs: Long = 500,
        val text: String,
        val behaviour: Behaviour = Behaviour.Succeed,
    ) {
        sealed class Behaviour {
            @SerialName("succeed")
            data object Succeed : Behaviour()

            @SerialName("succeed-after-nth-failure")
            data class SucceedAfterNthFailure(val failures: Int) : Behaviour()

            @SerialName("fail")
            data object Fail : Behaviour()
        }
    }

    data class Output(
        val msg: String,
        val input: Input,
    )

    override val jobType: String = "example-job-01"

    override suspend fun execute(job: BackgroundJobQueued, data: Input): Output {

        println("========================= Executing ExampleBackgroundJobHandler01 =========================")

        delay(data.execDelayMs)

        return when (val b = data.behaviour) {
            is Input.Behaviour.Succeed -> {
                Output(msg = "Executed $jobType", input = data)
            }

            is Input.Behaviour.SucceedAfterNthFailure -> when (job.results.size < b.failures) {
                true -> throw RuntimeException("Job failed")
                else -> Output(msg = "Executed $jobType", input = data)
            }

            is Input.Behaviour.Fail -> {
                throw RuntimeException("Job failed")
            }
        }
    }
}
