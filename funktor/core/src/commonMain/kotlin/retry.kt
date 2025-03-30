package de.peekandpoke.ktorfx.core

import de.peekandpoke.ultra.common.maths.Ease
import de.peekandpoke.ultra.common.maths.Ease.bindFromTo
import de.peekandpoke.ultra.common.maths.EaseFn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// TODO move to ultra::common
object Retry {

    suspend fun <T> retry(
        attempts: Int = 10,
        delays: ClosedRange<Duration> = 100.milliseconds..1000.milliseconds,
        easeFn: EaseFn = Ease.In.quad,
        block: suspend () -> T,
    ): T? = retry(
        attempts = attempts,
        firstDelay = delays.start,
        lastDelay = delays.endInclusive,
        easeFn = easeFn,
        block = block,
    )

    suspend fun <T> retry(
        attempts: Int = 10,
        firstDelay: Duration = 100.milliseconds,
        lastDelay: Duration = 1000.milliseconds,
        easeFn: EaseFn = Ease.In.quad,
        block: suspend () -> T,
    ): T? {
        val result = flow {
            val r = block()
            emit(r)
        }.retry(
            attempts = attempts,
            firstDelay = firstDelay,
            lastDelay = lastDelay,
            easeFn = easeFn,
        ).firstOrNull()

        return result
    }

    fun <T> Flow<T>.retry(
        attempts: Int = 10,
        firstDelay: Duration = 100.milliseconds,
        lastDelay: Duration = firstDelay * 10,
        easeFn: EaseFn = Ease.In.quad,
    ): Flow<T> {
        val ease = easeFn.bindFromTo(
            from = firstDelay.inWholeMilliseconds.toDouble(),
            to = lastDelay.inWholeMilliseconds.toDouble(),
        )

        return retryWhen { cause, attempt ->
            if (attempt >= attempts) {
                false
            } else {
                val progress = easeFn((attempt + 1) / attempts.toDouble())
                // Calculate the delay
                val delay = ease(progress = progress)

                // println("Retrying in $delay ms")

                // delay
                delay(delay.milliseconds)
                // return
                true
            }
        }
    }
}

