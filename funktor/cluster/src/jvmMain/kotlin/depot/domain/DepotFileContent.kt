package de.peekandpoke.ktorfx.cluster.depot.domain

/**
 * Definition of the content of a depot file
 */
interface DepotFileContent {
    /** Returns the content of the file as a [ByteArray] */
    suspend fun getContentBytes(): ByteArray?
}
