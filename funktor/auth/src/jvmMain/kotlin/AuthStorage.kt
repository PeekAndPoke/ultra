package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.slumber.Polymorphic
import de.peekandpoke.ultra.vault.Stored
import kotlin.time.Duration

interface AuthStorage {
    interface AuthRecordsRepo {
        suspend fun <T : AuthRecord> insert(record: T): Stored<T>

        suspend fun findLatestBy(
            realm: String,
            type: String,
            owner: String,
        ): Stored<AuthRecord>?
    }

    val kronos: Kronos
    val authRecordsRepo: AuthRecordsRepo

    fun calcExpiresAt(durationFromNow: Duration) = kronos.instantNow().plus(durationFromNow).toEpochSeconds()

    suspend fun <T : AuthRecord> createRecord(
        create: AuthStorage.() -> T,
    ): Stored<T> {
        return authRecordsRepo.insert(
            create(this)
        )
    }
}

suspend inline fun <reified T : AuthRecord> AuthStorage.findLatestRecordBy(
    type: Polymorphic.TypedChild<T>,
    realm: String,
    owner: String,
): Stored<T>? {
    return authRecordsRepo.findLatestBy(
        type = type.identifier,
        realm = realm,
        owner = owner,
    )?.castTyped()
}
