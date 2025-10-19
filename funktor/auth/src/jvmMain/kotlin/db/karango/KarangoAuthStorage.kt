package de.peekandpoke.funktor.auth.db.karango

import de.peekandpoke.funktor.auth.AuthStorage
import de.peekandpoke.ultra.common.datetime.Kronos

class KarangoAuthStorage(
    kronos: Lazy<Kronos>,
    authRecordsRepo: Lazy<KarangoAuthRecordsRepo>,
) : AuthStorage {
    override val kronos by kronos
    override val authRecordsRepo by authRecordsRepo
}
