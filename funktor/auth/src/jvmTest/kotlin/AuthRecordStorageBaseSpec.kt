package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

abstract class AuthRecordStorageBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()

    abstract fun FunktorAuthBuilder.configureAuth()

    suspend fun createKontainer(): Kontainer {
        return createAuthTestContainer(
            configureKontainer = { configureKontainer() },
            configureAuth = { configureAuth() },
        )
    }

    init {
        "Creating a passwords and reloading the latest one must work" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            authRecords
                .create(AuthRecord.Password(realm = "realm", ownerId = "owner1", token = "pw1"))

            // ensure they get a different timestamp
            delay(1.milliseconds)

            val owner1pw2 = authRecords
                .create(AuthRecord.Password(realm = "realm", ownerId = "owner1", token = "pw2"))

            delay(2.milliseconds)

            authRecords
                .create(AuthRecord.Password(realm = "realm", ownerId = "owner2", token = "pw1"))

            // ensure they get a different timestamp
            delay(1.milliseconds)

            val owner2pw2 = authRecords
                .create(AuthRecord.Password(realm = "realm", ownerId = "owner2", token = "pw2"))

            withClue("Loading latest password of owner1") {
                val loaded = authRecords
                    .findLatestRecordBy(AuthRecord.Password, "realm", "owner1")

                loaded.shouldNotBeNull()
                loaded._id shouldBe owner1pw2._id
                loaded.value.realm shouldBe owner1pw2.value.realm
                loaded.value.ownerId shouldBe owner1pw2.value.ownerId
                loaded.value.token shouldBe owner1pw2.value.token
            }

            withClue("Loading latest password of owner2") {
                val loaded = authRecords
                    .findLatestRecordBy(AuthRecord.Password, "realm", "owner2")

                loaded.shouldNotBeNull()
                loaded._id shouldBe owner2pw2._id
                loaded.value.realm shouldBe owner2pw2.value.realm
                loaded.value.ownerId shouldBe owner2pw2.value.ownerId
                loaded.value.token shouldBe owner2pw2.value.token
            }

            withClue("Loading from a realm that does not have password stored") {
                val loaded = authRecords
                    .findLatestRecordBy(AuthRecord.Password, "UNKNOWN", "owner1")

                loaded.shouldBeNull()
            }

            withClue("Loading for an owner that does not have password stored") {
                val loaded = authRecords
                    .findLatestRecordBy(AuthRecord.Password, "realm", "UNKNOWN")

                loaded.shouldBeNull()
            }
        }

        "Creating and loading a password reset token must work" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()
            val kronos = kontainer.get(Kronos::class)

            val token1 = authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = "realm",
                    ownerId = "owner1",
                    token = "token1",
                    expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds(),
                )
            )

            val token2 = authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = "realm",
                    ownerId = "owner2",
                    token = "token2",
                    expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds(),
                )
            )

            withClue("Loading password reset token of owner1") {
                val loaded = authRecords
                    .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = "realm", token = "token1")

                loaded.shouldNotBeNull()
                loaded._id shouldBe token1._id
                loaded.value.realm shouldBe token1.value.realm
                loaded.value.ownerId shouldBe token1.value.ownerId
                loaded.value.token shouldBe token1.value.token
                loaded.value.expiresAt shouldBe token1.value.expiresAt
            }

            withClue("Loading password reset token of owner2") {
                val loaded = authRecords
                    .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = "realm", token = "token2")

                loaded.shouldNotBeNull()
                loaded._id shouldBe token2._id
                loaded.value.realm shouldBe token2.value.realm
                loaded.value.ownerId shouldBe token2.value.ownerId
                loaded.value.token shouldBe token2.value.token
                loaded.value.expiresAt shouldBe token2.value.expiresAt
            }
        }

        "Creating a password reset token that has already expired" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()
            val kronos = kontainer.get(Kronos::class)

            authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = "realm",
                    ownerId = "owner1",
                    token = "token1",
                    expiresAt = kronos.instantNow().minus(1.hours).toEpochSeconds(),
                )
            )

            val loaded = authRecords
                .findLatestRecordBy(AuthRecord.PasswordRecoveryToken, "realm", "owner1")

            loaded.shouldBeNull()
        }
    }
}
