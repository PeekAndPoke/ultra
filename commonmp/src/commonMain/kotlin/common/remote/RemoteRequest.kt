package de.peekandpoke.ultra.common.remote

import kotlinx.coroutines.flow.Flow

interface RemoteRequest {

    val requestInterceptors: List<RequestInterceptor>
    val responseInterceptors: List<ResponseInterceptor>

    ////  REQUESTS  ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * issues a get request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    fun get(uri: String = ""): Flow<RemoteResponse>

    /**
     * issues a head request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    fun head(uri: String = ""): Flow<RemoteResponse>

    /**
     * issues a post request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    fun post(uri: String = "", contentType: String = "application/json", body: String): Flow<RemoteResponse>

    /**
     * issues a put request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    fun put(uri: String = "", contentType: String = "application/json", body: String): Flow<RemoteResponse>

    /**
     * issues a delete request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    fun delete(uri: String = "", contentType: String = "application/json", body: String? = null): Flow<RemoteResponse>

    /**
     * issues a connect request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    fun connect(uri: String = ""): Flow<RemoteResponse>

    /**
     * issues a options request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     */
    fun options(uri: String = ""): Flow<RemoteResponse>

    /**
     * issues a patch request returning a flow of it's response
     *
     * @param uri endpoint url which getting appended to the baseUrl with `/`
     * @param contentType content-type of the given body
     * @param body content to send in the body of the request
     */
    fun patch(uri: String = "", contentType: String = "application/json", body: String): Flow<RemoteResponse>

    ////  HEADERS  /////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * adds the given http header to the request
     *
     * @param name name of the http header to add
     * @param value value of the header field
     */
    fun header(name: String, value: String): RemoteRequest

    /**
     * adds the basic [Authorization](https://developer.mozilla.org/de/docs/Web/HTTP/Headers/Authorization)
     * header for the given username and password
     *
     * @param username name of the user
     * @param password password of the user
     */
    fun basicAuth(username: String, password: String): RemoteRequest

    /**
     * adds the given [Cache-Control](https://developer.mozilla.org/de/docs/Web/HTTP/Headers/Cache-Control)
     * value to the http headers
     *
     * @param value cache-control value
     */
    fun cacheControl(value: String): RemoteRequest

    /**
     * adds the given [Accept](https://developer.mozilla.org/de/docs/Web/HTTP/Headers/Accept)
     * value to the http headers, e.g "application/pdf"
     *
     * @param value media type to accept
     */
    fun accept(value: String): RemoteRequest

    /**
     * adds a header to accept JSON as response
     */
    fun acceptJson(): RemoteRequest = accept("application/json")
}
