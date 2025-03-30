package de.peekandpoke.ktorfx.core.metrics

import kotlin.time.Duration

interface RequestMetricsProvider {

    fun getRequestDuration(): Duration

    fun getRequestDurationInMs(): Double = getRequestDuration().inWholeNanoseconds / 1_000_000.0

    fun getRequestDetailsUri(): String?
}
