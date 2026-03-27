package io.peekandpoke.ultra.remote

/**
 * Exception thrown when an HTTP request receives a non-OK response.
 *
 * Carries the full [RemoteResponse] so callers can inspect status, body, etc.
 */
class RemoteException(val response: RemoteResponse) : Throwable()
