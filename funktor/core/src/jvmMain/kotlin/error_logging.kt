package io.peekandpoke.funktor.core

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.peekandpoke.ultra.log.Log

/** Logs a 404 not-found event with request details, optional hint, and cause. */
fun ApplicationCall.logNotFound(cause: Throwable? = null, hint: String? = null) {
    val content = listOfNotNull(
        "Not found '${request.methodAndUrl()}'",
        hint?.let { "-> Hint: $it" },
        "X-Forwarded-For: ${this.request.headers["X-Forwarded-For"] ?: ""}",
        "Referrer: ${this.request.headers["Referer"] ?: ""}",
        "IP: ${request.origin.remoteHost}",
        cause?.let { "Cause: ${it.message}" }
    ).joinToString("\n")

    when (val log = kontainerOrNull?.getOrNull(Log::class)) {
        null -> {
            application.log.info(content)
        }

        else -> {
            log.info(content)
        }
    }
}

/** Logs a 400 bad-request event with request details and cause. */
fun ApplicationCall.logBadRequest(cause: Throwable? = null) {
    val content = listOfNotNull(
        "Bad Request '${request.methodAndUrl()}'",
        "X-Forwarded-For: ${this.request.headers["X-Forwarded-For"] ?: ""}",
        "Referrer: ${this.request.headers["Referer"] ?: ""}",
        "IP: ${request.origin.remoteHost}",
        cause?.let { "Cause: ${it.message}" }
    ).joinToString("\n")

    when (val log = kontainerOrNull?.getOrNull(Log::class)) {
        null -> {
            application.log.info(content)
        }

        else -> {
            log.info(content)
        }
    }
}

/** Logs a 500 internal-error event with request details and cause. */
fun ApplicationCall.logInternalError(cause: Throwable? = null) {
    logInternalError(null, cause)
}

/** Logs a 500 internal-error event with optional message, request details, and cause. */
fun ApplicationCall.logInternalError(message: String? = null, cause: Throwable? = null) {
    val content = listOfNotNull(
        "Internal Error '${request.methodAndUrl()}'",
        message,
        "X-Forwarded-For: ${this.request.headers["X-Forwarded-For"] ?: ""}",
        "Referrer: ${this.request.headers["Referer"] ?: ""}",
        "IP: ${request.origin.remoteHost}",
    ).joinToString("\n")

    when (val log = kontainerOrNull?.getOrNull(Log::class)) {
        null -> {
            application.log.error(content, cause)
        }

        else -> {
            log.error(content + "\n" + (cause?.stackTraceToString() ?: ""))
        }
    }
}
