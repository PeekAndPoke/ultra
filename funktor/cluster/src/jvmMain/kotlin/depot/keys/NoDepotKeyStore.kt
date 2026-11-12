package io.peekandpoke.funktor.cluster.depot.keys

import io.peekandpoke.ultra.depot.DepotException
import io.peekandpoke.ultra.depot.crypto.DepotKeyStore
import io.peekandpoke.ultra.depot.crypto.DepotSubjectKey

/** The key store of a cluster without a database backend: an encrypting repository cannot work with it. */
object NoDepotKeyStore : DepotKeyStore {
    override suspend fun getOrCreate(subject: String): DepotSubjectKey = fail()

    override suspend fun get(id: String): DepotSubjectKey? = fail()

    override suspend fun delete(subject: String): Boolean = fail()

    private fun fail(): Nothing =
        throw DepotException("No depot key store is configured: the cluster needs useKarango() or useMonko()")
}
