package io.peekandpoke.funktor.demo.server.operator

import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.demo.common.OperatorUserModel
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.password.PasswordHasher
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class OperatorUsersRepo(
    driver: KarangoDriver,
    onAfterSaves: List<OnAfterSave>,
    timestamps: TimestampedHook,
) : EntityRepository<OperatorUser>(
    name = "operator_users",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamps.onBeforeSave())
) {
    companion object {
        suspend fun Storable<OperatorUser>.asApiModel() = with(resolve()) {
            OperatorUserModel(
                id = UserId(_id),
                name = name,
                email = email,
                isSuperUser = isSuperUser,
            )
        }
    }

    interface OnAfterSave : Repository.Hooks.OnAfterSave<OperatorUser>

    @Suppress("unused")
    class Fixtures(
        repo: OperatorUsersRepo,
        private val authRecordStorage: AuthRecordStorage,
        private val passwordHasher: PasswordHasher,
    ) : RepoFixtureLoader<OperatorUser>(repo = repo) {

        private val commonPassword = "S3cret123!"

        private suspend fun Stored<OperatorUser>.createPassword(password: String = commonPassword) {
            authRecordStorage.create {
                AuthRecord.Password(
                    realm = OperatorRealm.REALM,
                    ownerId = UserId(_id),
                    token = passwordHasher.hashAsString(password)
                )
            }
        }

        val karsten = singleFix {
            repo.insert(
                "karsten-ops", OperatorUser(
                    name = "Karsten (Operator)",
                    email = EmailAddress("karsten.john.gerber@googlemail.com"),
                    isSuperUser = true,
                )
            ).also { user -> user.createPassword() }
        }
    }

    override fun KarangoIndexBuilder<OperatorUser>.buildIndexes() {
        persistentIndex {
            field { email }

            options {
                unique(true)
            }
        }
    }

    suspend fun findByEmail(email: EmailAddress) = findFirst {
        FOR(repo) { user ->
            FILTER(user.email EQ email)

            LIMIT(1)

            RETURN(user)
        }
    }
}
