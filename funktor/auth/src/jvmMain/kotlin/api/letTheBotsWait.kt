package io.peekandpoke.funktor.auth.api

import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/** Slows a request down by a random delay — evens the timing between hit/miss on public auth routes. */
internal suspend fun letTheBotsWait() {
    delay(Random.nextLong(250, 500).milliseconds)
}
