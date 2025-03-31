package de.peekandpoke.funktor.cluster.backgroundjobs.example

import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import de.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobQueued
import de.peekandpoke.ultra.common.reflection.kType
import kotlinx.coroutines.delay

class ExampleBackgroundJobHandler02(
    backgroundJobs: Lazy<BackgroundJobs>,
) :
    BackgroundJobs.Handler<ExampleBackgroundJobHandler02.Input, ExampleBackgroundJobHandler02.Output>(
        backgroundJobs,
        kType()
    ) {

    data class Input(
        val execDelayMs: Long = 500,
        val text: String,
    )

    data class Output(
        val msg: String,
        val input: Input,
    )

    override val jobType: String = "example-job-02"

    override suspend fun execute(job: BackgroundJobQueued, data: Input): Output {

        delay(data.execDelayMs)

        return Output(
            msg = "Executed $jobType",
            input = data,
        )
    }
}
