package io.peekandpoke.funktor.demo.server.admin

import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.demo.common.AdminUserModel
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.password.PasswordHasher
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class AdminUsersRepo(
    driver: KarangoDriver,
    onAfterSaves: List<OnAfterSave>,
    timestamps: TimestampedHook,
) : EntityRepository<AdminUser>(
    name = "admin_users",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamps.onBeforeSave())
) {
    companion object {
        fun Storable<AdminUser>.asApiModel() = with(value) {
            AdminUserModel(
                id = _id,
                name = name,
                email = email,
                isSuperUser = isSuperUser,
            )
        }
    }

    interface OnAfterSave : Repository.Hooks.OnAfterSave<AdminUser>

    @Suppress("unused")
    class Fixtures(
        repo: AdminUsersRepo,
        private val authRecordStorage: AuthRecordStorage,
        private val passwordHasher: PasswordHasher,
    ) : RepoFixtureLoader<AdminUser>(repo = repo) {

        private val commonPassword = "S3cret123!"

        private suspend fun Stored<AdminUser>.createPassword(password: String = commonPassword) {
            authRecordStorage.create {
                AuthRecord.Password(
                    realm = AdminUserRealm.REALM,
                    ownerId = _id,
                    token = passwordHasher.hashAsString(password)
                )
            }
        }

        val karsten = singleFix {
            repo.insert(
                "karsten", AdminUser(
                    name = "Karsten",
                    email = "karsten.john.gerber@googlemail.com",
                    isSuperUser = true,
                )
            ).also { user -> user.createPassword() }
        }
    }

    override fun KarangoIndexBuilder<AdminUser>.buildIndexes() {
        persistentIndex {
            field { email }

            options {
                unique(true)
            }
        }
    }

    suspend fun findByEmail(email: String) = findFirst {
        FOR(repo) { user ->
            FILTER(user.email EQ email)

            LIMIT(1)

            RETURN(user)
        }
    }
}
