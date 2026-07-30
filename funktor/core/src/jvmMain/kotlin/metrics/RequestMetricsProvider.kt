package io.peekandpoke.funktor.core.metrics

import kotlin.time.Duration

/** Provides timing information for the current HTTP request. */
interface RequestMetricsProvider {

    fun getRequestDuration(): Duration

    fun getRequestDurationInMs(): Double = getRequestDuration().inWholeNanoseconds / 1_000_000.0
}
