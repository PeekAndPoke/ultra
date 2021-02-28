package de.peekandpoke.ultra.common.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.w3c.fetch.Response

/**
 * factory method to create a [RemoteRequest]
 */
actual fun remote(
    baseUrl: String,
    requestInterceptors: List<RequestInterceptor>,
    responseInterceptors: List<ResponseInterceptor>
): RemoteRequest = RemoteRequestImpl(
    baseUrl,
    requestInterceptors,
    responseInterceptors
)

/**
 * extracts the body from the given [Response]
 */
fun Flow<RemoteResponse>.body() = this.map { it.body }

/**
 * Helper function for receiving the response body and mapping it
 */
inline fun <R> Flow<RemoteResponse>.body(crossinline transform: suspend (String) -> R): Flow<R> {
    return body().map { transform(it) }
}

/**
 * defines, how to handle an error that occurred during a http request.
 *
 * @param handler function that describes, how to handle a thrown [RemoteException]
 */
fun Flow<Response>.onError(handler: (RemoteException) -> Unit) = this.catch {
    when (it) {
        is RemoteException -> handler(it)
        else -> throw it
    }
}

external fun btoa(decoded: String): String
