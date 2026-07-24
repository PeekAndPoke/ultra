package io.peekandpoke.funktor.demo.server.b2b

import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.demo.common.B2bUserModel
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
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class B2bUsersRepo(
    driver: KarangoDriver,
    onAfterSaves: List<OnAfterSave>,
    timestamps: TimestampedHook,
) : EntityRepository<B2bUser>(
    name = "b2b_users",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamps.onBeforeSave())
) {
    companion object {
        suspend fun Storable<B2bUser>.asApiModel() = with(resolve()) {
            B2bUserModel(
                id = _id,
                name = name,
                email = email,
            )
        }
    }

    interface OnAfterSave : Repository.Hooks.OnAfterSave<B2bUser>

    @Suppress("unused")
    class Fixtures(
        repo: B2bUsersRepo,
        private val authRecordStorage: AuthRecordStorage,
        private val passwordHasher: PasswordHasher,
        private val orgs: OrgsStorage,
        private val orgMembers: OrgMembersStorage,
    ) : RepoFixtureLoader<B2bUser>(repo = repo) {

        private val commonPassword = "S3cret123!"

        private suspend fun Stored<B2bUser>.createPassword(password: String = commonPassword) {
            authRecordStorage.create {
                AuthRecord.Password(
                    realm = B2bRealm.REALM,
                    ownerId = _id,
                    token = passwordHasher.hashAsString(password)
                )
            }
        }

        // 0 orgs → "no organisation access" on login
        val noOrg = singleFix {
            repo.insert(
                "b2b-noorg", B2bUser(name = "No Org User", email = "noorg@b2b.test")
            ).also { it.createPassword() }
        }

        // 1 org → auto-selected on login
        val singleOrg = singleFix {
            val acme = orgs.ensureBySlug("acme", "Acme Inc")
            repo.insert(
                "b2b-single", B2bUser(name = "Single Org User", email = "single@b2b.test")
            ).also {
                it.createPassword()
                orgMembers.add(org = acme, userId = it._id, roles = setOf("admin"))
            }
        }

        // n orgs → org-selection step on login
        val multiOrg = singleFix {
            val acme = orgs.ensureBySlug("acme", "Acme Inc")
            val globex = orgs.ensureBySlug("globex", "Globex Corporation")
            repo.insert(
                "b2b-multi", B2bUser(name = "Multi Org User", email = "multi@b2b.test")
            ).also {
                it.createPassword()
                orgMembers.add(org = acme, userId = it._id, roles = setOf("admin"))
                orgMembers.add(org = globex, userId = it._id, roles = setOf("member"))
            }
        }

        // acme's OWNER — exercises the owner-only ownership gating in the member-management API.
        val ownerOrg = singleFix {
            val acme = orgs.ensureBySlug("acme", "Acme Inc")
            repo.insert(
                "b2b-owner", B2bUser(name = "Owner User", email = "owner@b2b.test")
            ).also {
                it.createPassword()
                orgMembers.add(org = acme, userId = it._id, roles = setOf("owner"))
            }
        }
    }

    override fun KarangoIndexBuilder<B2bUser>.buildIndexes() {
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
