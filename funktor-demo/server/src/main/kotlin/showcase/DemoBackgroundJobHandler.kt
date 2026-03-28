package io.peekandpoke.funktor.demo.server.showcase

import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import io.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobQueued
import io.peekandpoke.ultra.reflection.kType
import kotlinx.coroutines.delay

class DemoBackgroundJobHandler(
    backgroundJobs: Lazy<BackgroundJobs>,
) : BackgroundJobs.Handler<DemoBackgroundJobHandler.Input, DemoBackgroundJobHandler.Output>(
    backgroundJobs,
    kType(),
) {
    data class Input(
        val text: String,
        val execDelayMs: Long = 500,
        val shouldFail: Boolean = false,
    )

    data class Output(
        val message: String,
        val input: Input,
    )

    override val jobType: String = "demo-showcase-job"

    override suspend fun execute(job: BackgroundJobQueued, data: Input): Output {
        delay(data.execDelayMs)

        if (data.shouldFail) {
            error("Demo job intentionally failed: ${data.text}")
        }

        return Output(
            message = "Demo job completed: ${data.text}",
            input = data,
        )
    }
}
