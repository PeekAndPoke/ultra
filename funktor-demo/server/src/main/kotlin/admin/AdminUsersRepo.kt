package io.peekandpoke.funktor.demo.server.admin

import de.peekandpoke.funktor.auth.AuthRecordStorage
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.peekandpoke.funktor.demo.common.AdminUserModel

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

    class Fixtures(
        repo: AdminUsersRepo,
        private val authRecordStorage: AuthRecordStorage,
        private val passwordHasher: PasswordHasher,
    ) : RepoFixtureLoader<AdminUser>(repo = repo) {

        private val commonPassword = "S3cret123!"

        private suspend fun Stored<AdminUser>.createPassword(password: String = commonPassword) {
            authRecordStorage.createRecord {
                AuthRecord.Password(
                    realm = AdminUserRealm.REALM,
                    ownerId = _id,
                    hash = passwordHasher.hash(password)
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

    interface OnAfterSave : Repository.Hooks.OnAfterSave<AdminUser>

    override fun IndexBuilder<AdminUser>.buildIndexes() {
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
