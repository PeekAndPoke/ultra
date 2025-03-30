package de.peekandpoke.ktorfx.logging

import de.peekandpoke.ktorfx.core.kontainerOrNull
import de.peekandpoke.ktorfx.core.methodAndUrl
import de.peekandpoke.ultra.logging.Log
import io.ktor.server.application.*
import io.ktor.server.plugins.*

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

fun ApplicationCall.logInternalError(cause: Throwable? = null) {
    logInternalError(null, cause)
}

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
