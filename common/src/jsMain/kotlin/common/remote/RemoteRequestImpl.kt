package de.peekandpoke.ultra.common.remote

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.RequestMode
import org.w3c.fetch.RequestRedirect

@Suppress("Detekt.TooManyFunctions")
class RemoteRequestImpl(
    private val baseUrl: String,
    override val requestInterceptors: List<RequestInterceptor>,
    override val responseInterceptors: List<ResponseInterceptor>
) : RemoteRequest {
    private var headers: Headers? = undefined
    private var referrer: String? = undefined
    private var referrerPolicy: dynamic = undefined
    private var mode: RequestMode? = undefined
    private var credentials: RequestCredentials? = undefined
    private var cache: RequestCache? = undefined
    private var redirect: RequestRedirect? = undefined
    private var integrity: String? = undefined
    private var keepalive: Boolean? = undefined

    init {
        requestInterceptors.forEach {
            it.intercept(this@RemoteRequestImpl)
        }
    }

    /**
     * builds a request, sends it to the server, awaits the response (async), creates a flow of it and attaches the defined errorHandler
     */
    private fun execute(url: String, init: RequestInit): Flow<RemoteResponse> = flow {

        polyfillFetch()

        val response = window.fetch("$baseUrl/${url.trimStart('/')}", init).await()

        val remoteResponse: RemoteResponse = RemoteResponseImpl(
            request = this@RemoteRequestImpl,
            body = response.text().await(),
            response = response
        )

        if (response.ok) {
            emit(remoteResponse)
        } else {
            throw RemoteException(remoteResponse)
        }
    }
        // We apply all response interceptors
        .run {
            responseInterceptors.fold(this) { flow, interceptor ->
                flow
                    .catch {
                        if (it is RemoteException) {
                            interceptor.intercept(it.response)
                        }
                        // we rethrow the exception
                        throw it
                    }
                    .map {
                        interceptor.intercept(it)
                    }
            }
        }

    /**
     * builds a [RequestInit] without a body from the template using [method]
     *
     * @param method the http method to use (GET, POST, etc.)
     */
    private fun buildInit(method: String): RequestInit {

        return RequestInit(
            method = method,
            headers = headers,
            referrer = referrer,
            referrerPolicy = referrerPolicy,
            mode = mode,
            credentials = credentials,
            cache = cache,
            redirect = redirect,
            integrity = integrity,
            keepalive = keepalive
        )
    }

    /**
     * builds a [RequestInit] with a body from the template using [method]
     *
     * @param method the http method to use (GET, POST, etc.)
     * @param body content of the request
     * @param contentType content-type of the request body (default: application/json)
     */
    private fun buildInit(method: String, contentType: String = "application/json", body: String): RequestInit {
        addHeader("Content-Type", contentType)
        return RequestInit(
            method = method,
            headers = headers,
            referrer = referrer,
            referrerPolicy = referrerPolicy,
            mode = mode,
            credentials = credentials,
            cache = cache,
            redirect = redirect,
            integrity = integrity,
            keepalive = keepalive,
            body = body
        )
    }

    // Methods

    /**
     * issues a get request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    override fun get(uri: String): Flow<RemoteResponse> = execute(uri, buildInit("GET"))

    /**
     * issues a head request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    override fun head(uri: String): Flow<RemoteResponse> = execute(uri, buildInit("HEAD"))

    /**
     * issues a post request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    override fun post(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(uri, buildInit("POST", contentType, body))

    /**
     * issues a put request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    override fun put(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(uri, buildInit("PUT", contentType, body))

    /**
     * issues a delete request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    override fun delete(uri: String, contentType: String, body: String?): Flow<RemoteResponse> {
        return if (body != null) execute(uri, buildInit("DELETE", contentType, body))
        else execute(uri, buildInit("DELETE"))
    }

    /**
     * issues a options request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    override fun options(uri: String): Flow<RemoteResponse> = execute(uri, buildInit("OPTIONS"))

    /**
     * issues a patch request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    override fun patch(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(uri, buildInit("PATCH", contentType, body))

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
        addHeader("Authorization", "Basic ${btoa("$username:$password")}")

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
        if (headers == null || headers == undefined) headers = Headers()
        headers!!.append(name, value)
    }
}
