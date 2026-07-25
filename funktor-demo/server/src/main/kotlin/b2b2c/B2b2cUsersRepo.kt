package io.peekandpoke.funktor.demo.server.b2b2c

import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.demo.common.B2b2cUserModel
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.password.PasswordHasher
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class B2b2cUsersRepo(
    driver: KarangoDriver,
    onAfterSaves: List<OnAfterSave>,
    timestamps: TimestampedHook,
) : EntityRepository<B2b2cUser>(
    name = "b2b2c_users",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamps.onBeforeSave())
) {
    companion object {
        suspend fun Storable<B2b2cUser>.asApiModel() = with(resolve()) {
            B2b2cUserModel(
                id = UserId(_id),
                name = name,
                email = email,
            )
        }
    }

    interface OnAfterSave : Repository.Hooks.OnAfterSave<B2b2cUser>

    @Suppress("unused")
    class Fixtures(
        repo: B2b2cUsersRepo,
        private val authRecordStorage: AuthRecordStorage,
        private val passwordHasher: PasswordHasher,
        private val orgs: OrgsStorage,
        private val orgMembers: OrgMembersStorage,
    ) : RepoFixtureLoader<B2b2cUser>(repo = repo) {

        private val commonPassword = "S3cret123!"

        private suspend fun Stored<B2b2cUser>.createPassword(password: String = commonPassword) {
            authRecordStorage.create {
                AuthRecord.Password(
                    realm = B2b2cRealm.REALM,
                    ownerId = UserId(_id),
                    token = passwordHasher.hashAsString(password)
                )
            }
        }

        // 0 orgs → "no organisation access" on login
        val noOrg = singleFix {
            repo.insert(
                "b2b2c-noorg", B2b2cUser(name = "No Org End-User", email = "noorg@b2b2c.test")
            ).also { it.createPassword() }
        }

        // Role vocabulary: end-users deliberately do NOT reuse the b2b account roles
        // ("admin"/"member") — both realms resolve the SAME orgs, and the role strings land in the
        // JWT roles claim, so a shared vocabulary would make the two populations indistinguishable
        // to any role-gated surface (see 20260719-cross-realm-authz-and-tests.md, role collision).
        // 1 org → auto-selected on login
        val singleOrg = singleFix {
            val acme = orgs.ensureBySlug("acme", "Acme Inc")
            repo.insert(
                "b2b2c-single", B2b2cUser(name = "Single Org End-User", email = "single@b2b2c.test")
            ).also {
                it.createPassword()
                orgMembers.add(org = acme, userId = UserId(it._id), roles = setOf("end-user"))
            }
        }

        // n orgs → org-selection step on login
        val multiOrg = singleFix {
            val acme = orgs.ensureBySlug("acme", "Acme Inc")
            val globex = orgs.ensureBySlug("globex", "Globex Corporation")
            repo.insert(
                "b2b2c-multi", B2b2cUser(name = "Multi Org End-User", email = "multi@b2b2c.test")
            ).also {
                it.createPassword()
                orgMembers.add(org = acme, userId = UserId(it._id), roles = setOf("end-user"))
                orgMembers.add(org = globex, userId = UserId(it._id), roles = setOf("end-user"))
            }
        }
    }

    override fun KarangoIndexBuilder<B2b2cUser>.buildIndexes() {
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
