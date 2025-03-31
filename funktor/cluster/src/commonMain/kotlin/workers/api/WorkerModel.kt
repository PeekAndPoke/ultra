package de.peekandpoke.funktor.cluster.workers.api

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class WorkerModel(
    val id: String,
    val runs: List<Run>,
) {
    @Serializable
    data class Run(
        val serverId: String,
        val begin: MpInstant,
        val end: MpInstant,
        val result: Result,
    ) {
        val duration get() = end - begin

        @Serializable
        sealed class Result {
            @Serializable
            @SerialName("success")
            data object Success : Result()

            @Serializable
            @SerialName("failure")
            data class Failure(
                val message: String,
                val stack: String,
            ) : Result() {
                companion object {
                    fun of(throwable: Throwable) = Failure(
                        message = throwable.message ?: "n/a",
                        stack = throwable.stackTraceToString(),
                    )
                }
            }
        }
    }

    val successRate by lazy {
        if (runs.isEmpty()) {
            1.0
        } else {
            runs.count { it.result is Run.Result.Success } / runs.size.toDouble()
        }
    }

    val shortenedId by lazy {
        val parts = id.split(".")

        "${parts.firstOrNull()}...${parts.lastOrNull()}"
    }

    fun getRunsPer(duration: Duration): Double {
        if (runs.isEmpty()) {
            return 0.0
        }

        if (runs.size < 2) {
            return 1.0
        }

        val min = runs.minOf { it.begin }
        val max = runs.maxOf { it.begin }

        return (duration.inWholeMilliseconds.toDouble() * runs.size) / (max - min).inWholeMilliseconds.toDouble()
    }
}

