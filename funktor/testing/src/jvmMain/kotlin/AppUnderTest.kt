package de.peekandpoke.funktor.testing

import de.peekandpoke.funktor.core.broker.TypedRoute
import de.peekandpoke.funktor.core.broker.typedRouteRenderer
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.websocket.WebsocketClientModule
import de.peekandpoke.funktor.core.websocket.WsClientMessage
import de.peekandpoke.funktor.core.websocket.WsServerMessage
import de.peekandpoke.funktor.rest.ApiRoute
import de.peekandpoke.funktor.rest.codec.RestCodec
import de.peekandpoke.funktor.rest.restCodec
import de.peekandpoke.ultra.common.model.EmptyObject
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.kontainer.Kontainer
import io.kotest.assertions.withClue
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.test.fail

class AppUnderTest<C : AppConfig>(
    testBed: Lazy<TestBed<C>>,
    val host: String,
) {
    inner class AuthenticationScope(
        private val token: String?,
        private val setupRequest: HttpRequestBuilder.() -> Unit,
    ) {
        suspend fun request(
            method: HttpMethod,
            url: String,
            setup: HttpRequestBuilder.() -> Unit = {},
            assertions: suspend HttpResponse.() -> Unit,
        ) {
            val request = client.prepareRequest(url) {
                this.method = method
                configureRequest()
                setup()
            }

            val response: HttpResponse = request.execute()

            withClue(
                """
                    Request: $method $url 
                    
                    ResponseBody:
                    ${response.bodyAsText()}
                """.trimIndent()
            ) {
                response.assertions()
            }
        }

        suspend fun <RESPONSE> request(
            route: ApiRoute.Plain<RESPONSE>,
            setup: HttpRequestBuilder.() -> Unit = {},
            assertions: suspend HttpResponse.() -> Unit,
        ) {
            val response: HttpResponse = client.request(route.route().url) {
                this.method = route.method
                configureRequest()
                setup()
            }

            withClue(
                """
                    Request: ${route.method.value} ${route.route().url}
                     
                    ResponseBody:
                    ${response.bodyAsText()}
                """.trimIndent()
            ) {
                response.assertions()
            }
        }

        /**
         * Call this route with the given [params] and checks the given [assertions].
         *
         * The request can be additionally [setup].
         */
        suspend operator fun <PARAM : Any, RESPONSE> ApiRoute.WithParams<PARAM, RESPONSE>.invoke(
            params: PARAM,
            setup: HttpRequestBuilder.() -> Unit = {},
            assertions: suspend HttpResponse.() -> Unit,
        ) {
            request(route = this, params = params, setup = setup, assertions = assertions)
        }

        /**
         * Call this route with the given [params] and checks the given [assertions].
         *
         * The request can be additionally [setup].
         */
        suspend operator fun <PARAM : Any, BODY, RESPONSE> ApiRoute.WithBodyAndParams<PARAM, BODY, RESPONSE>.invoke(
            params: PARAM,
            body: BODY,
            setup: HttpRequestBuilder.() -> Unit = {},
            assertions: suspend HttpResponse.() -> Unit,
        ) {
            request(route = this, params = params, body = body, setup = setup, assertions = assertions)
        }

        /**
         * Call this route with the given [params] and checks the given [assertions].
         *
         * The request can be additionally [setup].
         */
        suspend fun <PARAM : Any, RESPONSE> request(
            route: ApiRoute.WithParams<PARAM, RESPONSE>,
            params: PARAM,
            setup: HttpRequestBuilder.() -> Unit = {},
            assertions: suspend HttpResponse.() -> Unit,
        ) {
            val requestClue = """
                Request: ${route.method.value} ${route.route(params).url}
            """.trimIndent()

            val request = client.prepareRequest(route.route(params).url) {
                this.method = route.method
                configureRequest()
                setup()
            }

            val response: HttpResponse = request.execute()

            withClue(
                """
                    $requestClue 

                    ResponseBody:
                    ${response.bodyAsText()}
                """.trimIndent()
            ) {
                response.assertions()
            }
        }

        suspend fun <BODY, RESPONSE> request(
            route: ApiRoute.WithBody<BODY, RESPONSE>,
            body: BODY,
            setup: HttpRequestBuilder.() -> Unit = {},
            assertions: suspend HttpResponse.() -> Unit,
        ) {
            val response: HttpResponse = client.request(route.route().url) {
                this.method = route.method
                setBody(restCodec.serialize(body) ?: "")
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                configureRequest()
                setup()
            }

            withClue(
                """
                    Request: ${route.method.value} ${route.route().url} 

                    Body:
                    $body

                    ResponseBody:
                    {${response.bodyAsText()}}
                """.trimIndent()
            ) {
                response.assertions()
            }
        }

        suspend fun <PARAM : Any, BODY, RESPONSE> request(
            route: ApiRoute.WithBodyAndParams<PARAM, BODY, RESPONSE>,
            params: PARAM,
            body: BODY,
            setup: HttpRequestBuilder.() -> Unit = {},
            assertions: suspend HttpResponse.() -> Unit,
        ) {
            val response = client.request(route.route(params).url) {
                this.method = route.method
                setBody(restCodec.serialize(body) ?: "")
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                configureRequest()
                setup()
            }

            withClue(
                """
                    Request: ${route.method.value} ${route.route(params).url} 

                    Body:
                    $body

                    ResponseBody:
                    ${response.bodyAsText()}
                """.trimIndent()
            ) {
                response.assertions()
            }
        }

        suspend fun websocket(uri: String, block: suspend WebsocketConversation.() -> Unit) {
            val wsClient = client.config {
                install(WebSockets) {}
            }

            wsClient.webSocket(urlString = uri, request = setupRequest) {
                WebsocketConversation(
                    authToken = token,
                    restCodec = restCodec,
                    session = this,
                ).block()
            }
        }

        private fun HttpRequestBuilder.configureRequest() {
            setupRequest()
        }
    }

    class WebsocketConversation(
        val authToken: String?,
        val restCodec: RestCodec,
        val session: DefaultClientWebSocketSession,
    ) {
        fun send(type: String) = send(type, EmptyObject())

        fun <T> send(message: WsClientMessage<T>): WsClientMessage<T> = message.also {
            runBlocking {
                session.outgoing.send(
                    Frame.Text(restCodec.serialize(it)!!)
                )
            }
        }

        fun <T> send(type: String, data: T): WsClientMessage<T> = send(
            WsClientMessage.withUuid(
                type = type,
                token = authToken ?: "",
                data = data,
            )
        )

        fun <T : Any> send(definition: WebsocketClientModule.Sends<T>, data: T) = send(
            type = definition.type,
            data = data,
        )

        fun send(definition: WebsocketClientModule.Sends<EmptyObject>) = send(
            type = definition.type,
            data = EmptyObject(),
        )

        inline fun <reified T> receiveAllDataAvailableInto(target: MutableList<T>) {
            val received = receiveAllDataAvailable<T>()

            synchronized(target) {
                target.addAll(received)
            }
        }

        @Suppress("UNUSED_PARAMETER")
        inline fun <reified T : Any> receiveAllDataAvailable(receives: WebsocketClientModule.Receives<T>): List<T> =
            receiveAllDataAvailable()

        inline fun <reified T> receiveAllDataAvailable(): List<T> {

            val type = kType<T>().wrapWith<WsServerMessage.Data<T>>()
            val items = mutableListOf<T>()
            var done = false

            while (!done) {

                val received = session.incoming.tryReceive()

                when (val frame = received.getOrNull()) {
                    null, is Frame.Close -> {
                        done = true
                    }

                    is Frame.Text -> {
                        try {
                            val text = frame.readText()

                            restCodec.deserialize(type, text)?.let {
                                items.add(it.data)
                            }
                        } catch (_: Throwable) {
                            // noop
                        }
                    }

                    else -> {
                        // noop
                    }
                }
            }

            return items
        }

        fun receiveAck(): WsServerMessage.Ack =
            receiveForType(type = kType())

        fun receiveNack(): WsServerMessage.Nack =
            receiveForType(type = kType())

        fun receiveLog(): WsServerMessage.Log =
            receiveForType(type = kType())

        @Suppress("UNUSED_PARAMETER")
        inline fun <reified T : Any> receiveData(sends: WebsocketClientModule.Receives<T>) =
            receiveData<T>()

        inline fun <reified T> receiveData(): T =
            receiveForType<WsServerMessage.Data<T>>(type = kType()).data

        fun <T> receiveForType(type: TypeRef<T>): T {
            return runBlocking {
                val received = session.incoming.receive()

                val text = (received as Frame.Text).readText()

                try {
                    @Suppress("UNCHECKED_CAST")
                    restCodec.deserialize(type, text) as T
                } catch (e: Throwable) {
                    fail(
                        "Could not deserialize to type '$type'\nReceived:\n$text" +
                                "\n\nError: ${e.message}\n${e.printStackTrace()}"
                    )
                }
            }
        }
    }

    private val testBed: TestBed<C> by testBed

    private val client get() = testBed.engine.client

    val kontainer: Kontainer by lazy { this.testBed.kontainer }

    val restCodec by lazy { kontainer.restCodec }

    inline fun authenticate(token: String?, body: AuthenticationScope.() -> Unit) {
        AuthenticationScope(
            token = token,
            setupRequest = {
                header(HttpHeaders.Host, host)
                header(HttpHeaders.Authorization, "Bearer $token")
            },
        ).body()
    }

    inline fun anonymous(body: AuthenticationScope.() -> Unit) {
        AuthenticationScope(
            token = null,
            setupRequest = {
                header(HttpHeaders.Host, host)
            },
        ).body()
    }

    suspend inline fun <reified T> HttpResponse.apiResponse(handler: ApiResponse<T>.() -> Unit) {
        apiResponse<T>().apply(handler)
    }

    suspend inline fun <reified T> HttpResponse.apiResponse(): ApiResponse<T> {
        return apiResponseOrNull<T>()
            ?: fail("Could not awake ApiResponse as '${T::class}' from response:\n\n${bodyAsText()}")
    }

    suspend inline fun <reified T> HttpResponse.apiResponseOrNull(): ApiResponse<T>? {

        val type = ApiResponse::class.createType(
            nullable = true,
            arguments = listOf(
                KTypeProjection.invariant(kType<T>().nullable.type)
            )
        )

        val body = bodyAsText()

        val result = try {
            @Suppress("UNCHECKED_CAST")
            restCodec.deserialize(type, body) as? ApiResponse<T>
        } catch (_: Throwable) {
            null
        }

        return result
    }

    suspend inline fun <reified T> HttpResponse.apiResponseData(): T? {
        val response = apiResponse<T>()

        return response.data
    }

    private val <P : Any> TypedRoute.Bound<P>.url get() = kontainer.typedRouteRenderer.render(this)
}
