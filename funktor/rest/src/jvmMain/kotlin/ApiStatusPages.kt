package de.peekandpoke.funktor.rest

import de.peekandpoke.funktor.core.broker.CouldNotConvertException
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.kontainerOrNull
import de.peekandpoke.funktor.core.logInternalError
import de.peekandpoke.funktor.core.logNotFound
import de.peekandpoke.ultra.common.remote.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.random.Random

object ApiStatusPages {

    fun isClientError(cause: Throwable): Boolean {
        return cause is ChannelWriteException ||
                cause is CancellationException ||
                cause is ClosedByteChannelException ||
                (cause is IOException && cause.message == "Ping timeout")
    }

    fun Route.installApiStatusPages() {

        val plugin = createRouteScopedPlugin("API-Status-Pages") {

            fun <T> ApiResponse<T>.withCause(call: ApplicationCall, cause: Throwable): ApiResponse<T> {
                val config = call.kontainerOrNull?.getOrNull(AppConfig::class)

                return when (config?.ktor?.isProduction) {
                    true -> this.withError(cause.message ?: "")
                    else -> this.withError(cause.stackTraceToString())
                }
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
                                ApiResponse.badRequest<Any>().withCause(call, cause)
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
                                    .withCause(call, cause)
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

