package io.peekandpoke.funktor.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.uri
import io.ktor.server.routing.Route
import io.ktor.util.cio.ChannelWriteException
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.ClosedByteChannelException
import io.peekandpoke.funktor.core.broker.CouldNotConvertException
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.kontainerOrNull
import io.peekandpoke.funktor.core.logInternalError
import io.peekandpoke.funktor.core.logNotFound
import io.peekandpoke.ultra.remote.ApiResponse
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.random.Random

/** Ktor status page handlers that map exceptions to typed [ApiResponse] errors. */
object ApiStatusPages {

    /**
     * Whether error responses may carry a full stack trace, which is only acceptable outside of
     * production.
     *
     * This deliberately asks for `isNotProduction` rather than negating `isProduction`, so that it
     * fails closed: when there is no kontainer or no [AppConfig] the environment is unknown, and an
     * unknown environment must be treated as production rather than leaking internals.
     */
    fun exposesStackTraces(config: AppConfig?): Boolean {
        return config?.ktor?.isNotProduction == true
    }

    /**
     * What an error answer may say of its [cause]: outside production the stack trace; in production the message
     * of a client error (written to be shown, see `SlumberRestCodec`), and nothing of an [internal] one, whose
     * message may quote the request's input.
     */
    fun errorDetail(config: AppConfig?, cause: Throwable, internal: Boolean): String? = when {
        exposesStackTraces(config) -> cause.stackTraceToString()
        internal -> null
        else -> cause.message ?: ""
    }

    fun isClientError(cause: Throwable): Boolean {
        return cause is ChannelWriteException ||
                cause is CancellationException ||
                cause is ClosedByteChannelException ||
                (cause is IOException && cause.message == "Ping timeout")
    }

    fun Route.installApiStatusPages() {

        val plugin = createRouteScopedPlugin("API-Status-Pages") {

            fun <T> ApiResponse<T>.withCause(call: ApplicationCall, cause: Throwable, internal: Boolean) =
                when (val detail = errorDetail(call.kontainerOrNull?.getOrNull(AppConfig::class), cause, internal)) {
                    null -> this
                    else -> this.withError(detail)
                }

            on(CallFailed) { call, cause ->
                try {
                    when {
                        // The client closed the connection. We ignore this and do not log anything
                        isClientError(cause) -> {
                            // we do nothing
                            call.respond(HttpStatusCode.NoContent, null)
                        }

                        cause is CouldNotConvertException -> {
                            call.apiRespond(
                                ApiResponse.badRequest<Any>().withCause(call, cause, internal = false)
                            )
                        }

                        cause is NotFoundException -> {
                            logNotFoundWithHint(call, cause)

                            call.apiRespond(
                                ApiResponse.notFound<Any>()
                                    .withError("The Resource '${call.request.uri}' was not found")
                            )
                        }

                        else -> {
                            call.logInternalError(cause)

                            call.apiRespond(
                                ApiResponse.internalServerError<Any>()
                                    .withError("The request '${call.request.uri}' cause an internal server error")
                                    .withCause(call, cause, internal = true)
                            )
                        }
                    }
                } catch (_: Throwable) {
                    // ignore it
                }
            }
        }

        install(plugin)
    }

    private val attackBlackList = mapOf<String, (ApplicationCall) -> Boolean>(
        "uri starts with /wp-admin" to {
            it.request.uri.startsWith("wp-admin") || it.request.uri.startsWith("/wp-admin")
        },
        "uri ends with .php" to {
            it.request.uri.endsWith(".php")
        },
        "uri contains .well-known" to {
            it.request.uri.contains(".well-known")
        },
    )

    suspend fun logNotFoundWithHint(call: ApplicationCall, cause: Throwable?) {
        val attack = attackBlackList.entries.firstOrNull { (_, v) -> v(call) }

        // If we detect an attack uri we let the attacker wait a bit
        attack?.let { delay(500 + Random.nextLong(-100, 100)) }

        call.logNotFound(cause = cause, hint = attack?.key?.let { "Attack detected: $it" })
    }
}
