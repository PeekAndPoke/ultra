package de.peekandpoke.funktor.core.config

class ConfigError(message: String, cause: Throwable? = null) : Throwable(message = message, cause = cause)
