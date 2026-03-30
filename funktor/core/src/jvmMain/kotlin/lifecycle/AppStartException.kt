package io.peekandpoke.funktor.core.lifecycle

/**
 * Exception that signals a fatal error during app startup.
 *
 * When an [AppLifeCycleHooks.OnAppStarting] hook throws this exception,
 * it will NOT be caught by the lifecycle builder — it propagates up and prevents the app from starting.
 */
class AppStartException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
