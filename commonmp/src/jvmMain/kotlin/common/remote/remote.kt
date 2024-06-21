package de.peekandpoke.ultra.common.remote

import de.peekandpoke.ultra.common.toBase64
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * factory method to create a [RemoteRequest]
 */
actual fun createRequest(
    baseUrl: String,
    requestInterceptors: List<RequestInterceptor>,
    responseInterceptors: List<ResponseInterceptor>,
    client: HttpClient?,
): RemoteRequest = RemoteRequestImpl(
    baseUrl = baseUrl,
    client = client,
    requestInterceptors = requestInterceptors,
    responseInterceptors = responseInterceptors,
)

class RemoteResponseImpl(
    override val request: RemoteRequest,
    private val response: HttpResponse,
    private val responseBody: String,
) : RemoteResponse {
    override val body: String get() = responseBody

    override val status: Int get() = response.status.value

    override val statusText: String get() = response.status.description

    override val ok: Boolean get() = response.status.isSuccess()

    override val is4xx: Boolean get() = status in 400..499

    override val is5xx: Boolean get() = status in 500..599
}

class RemoteRequestImpl(
    private val baseUrl: String,
    override val requestInterceptors: List<RequestInterceptor>,
    override val responseInterceptors: List<ResponseInterceptor>,
    client: HttpClient?,
) : RemoteRequest {

    private val client = client!!

    private val headers: HeadersBuilder = HeadersBuilder()

    init {
        requestInterceptors.forEach {
            it.intercept(this@RemoteRequestImpl)
        }
    }

    private fun execute(
        method: HttpMethod,
        uri: String,
        contentType: String? = null,
        body: String? = null,
    ): Flow<RemoteResponse> {
        val remote = this

        return flow {
            val url = "$baseUrl/${uri.trimStart('/')}"

            val response: HttpResponse = client.request(url) {
                val request = this
                request.method = method
                request.headers.appendAll(remote.headers.build())

                contentType?.let {
                    request.contentType(ContentType.parse(contentType))
                }

                body?.let {
                    request.setBody(body)
                }
            }

            val result = RemoteResponseImpl(
                request = remote,
                response = response,
                responseBody = response.bodyAsText()
            )

            // TODO: call response interceptors

            emit(result)
        }
    }

    /**
     * issues a get request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    override fun get(uri: String): Flow<RemoteResponse> = execute(HttpMethod.Get, uri)

    /**
     * issues a head request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    override fun head(uri: String): Flow<RemoteResponse> = execute(HttpMethod.Head, uri)

    /**
     * issues a post request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    override fun post(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(HttpMethod.Post, uri, contentType, body)

    /**
     * issues a put request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    override fun put(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(HttpMethod.Put, uri, contentType, body)

    /**
     * issues a delete request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    override fun delete(uri: String, contentType: String, body: String?): Flow<RemoteResponse> =
        execute(HttpMethod.Delete, uri, contentType, body)

    /**
     * issues a options request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    override fun options(uri: String): Flow<RemoteResponse> =
        execute(HttpMethod.Options, uri)

    /**
     * issues a patch request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    override fun patch(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(HttpMethod.Patch, uri, contentType, body)

    // Headers

    /**
     * adds the given http header to the request
     *
     * @param name name of the http header to add
     * @param value value of the header field
     */
    override fun header(name: String, value: String) = addHeader(name, value)

    /**
     * adds the basic [Authorization](https://developer.mozilla.org/de/docs/Web/HTTP/Headers/Authorization)
     * header for the given username and password
     *
     * @param username name of the user
     * @param password password of the user
     */
    override fun basicAuth(username: String, password: String) =
        addHeader("Authorization", "Basic ${"$username:$password".toBase64()}")

    /**
     * adds the given [Cache-Control](https://developer.mozilla.org/de/docs/Web/HTTP/Headers/Cache-Control)
     * value to the http headers
     *
     * @param value cache-control value
     */
    override fun cacheControl(value: String) = addHeader("Cache-Control", value)

    /**
     * adds the given [Accept](https://developer.mozilla.org/de/docs/Web/HTTP/Headers/Accept)
     * value to the http headers, e.g "application/pdf"
     *
     * @param value media type to accept
     */
    override fun accept(value: String) = addHeader("Accept", value)

    /**
     * checks if a [Headers] object has been initialized, does so if not and attaches the given http header
     *
     * @param name name of the http header to add
     * @param value value of the header field
     */
    private fun addHeader(name: String, value: String): RemoteRequest = apply {
        headers.append(name, value)
    }
}
