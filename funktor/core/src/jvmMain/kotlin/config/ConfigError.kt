package io.peekandpoke.funktor.core.config

/** Thrown when the application configuration cannot be loaded or parsed. */
class ConfigError(message: String, cause: Throwable? = null) : Throwable(message = message, cause = cause)
