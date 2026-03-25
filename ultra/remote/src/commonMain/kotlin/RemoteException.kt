package de.peekandpoke.ultra.remote

/**
 * Exception type for handling http exceptions
 */
class RemoteException(val response: RemoteResponse) : Throwable()
