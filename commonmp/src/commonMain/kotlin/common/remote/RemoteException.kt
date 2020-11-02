package de.peekandpoke.ultra.common.remote

/**
 * Exception type for handling http exceptions
 */
class RemoteException(val response: RemoteResponse) : Throwable()
