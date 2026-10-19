package io.peekandpoke.funktor

import io.peekandpoke.funktor.auth.AccountRetirement
import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.AuthUserAdapter
import io.peekandpoke.funktor.auth.FunktorAuthBuilder
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.auth.provider.GithubSsoAuth
import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.jvm
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.hours

//  The user store, once per backend  ////////////////////////////////////////////////////////////////////////
//  Both carry a UNIQUE index on `email`, so every retirement in these specs also proves the renamed key
//  never collides — with a later sign-up of the same address, or with an earlier retirement of it.

/** What [ActivationHardeningRealm] needs from its user repository; implemented once per backend. */
interface HardeningUsers {
    suspend fun byId(id: String): Stored<TestUser>?
    suspend fun byEmail(email: EmailAddress): Stored<TestUser>?
    suspend fun create(user: TestUser): Stored<TestUser>
    suspend fun update(user: Stored<TestUser>): Stored<TestUser>
}

class HardeningKaUsersRepo(driver: KarangoDriver) : HardeningUsers, EntityRepository<TestUser>(
    name = "activation_hardening_e2e_ka_users",
    storedType = kType(),
    driver = driver,
) {
    override fun KarangoIndexBuilder<TestUser>.buildIndexes() {
        persistentIndex {
            field { email }
            options { unique(true) }
        }
    }

    override suspend fun byId(id: String) = findById(id)

    override suspend fun byEmail(email: EmailAddress) = findFirst {
        FOR(repo) { u ->
            FILTER(u.email EQ email)
            LIMIT(1)
            RETURN(u)
        }
    }

    override suspend fun create(user: TestUser) = insert(user)

    override suspend fun update(user: Stored<TestUser>) = save(user)
}

class HardeningMoUsersRepo(driver: MonkoDriver) : HardeningUsers, MonkoRepository<TestUser>(
    name = "activation_hardening_e2e_mo_users",
    storedType = kType(),
    driver = driver,
) {
    override fun MonkoIndexBuilder<TestUser>.buildIndexes() {
        uniqueIndex {
            field { it.email }
        }
    }

    override suspend fun byId(id: String) = findById(id)

    override suspend fun byEmail(email: EmailAddress) = find { u ->
        filter(u.email eq email)
        limit(1)
    }.firstOrNull()

    override suspend fun create(user: TestUser) = insert(user)

    override suspend fun update(user: Stored<TestUser>) = save(user)
}

//  A GitHub that answers from the "code"  /////////////////////////////////////////////////////////////////////

/**
 * Fake GitHub API. The OAuth code IS the answer: `verified:<email>` or `unverified:<email>` — the address
 * GitHub reports, and whether `/user/emails` lists it as the verified primary.
 */
object FakeGithub : GithubSsoAuth.RemoteClient {

    override suspend fun getAccessToken(code: String): JsonObject = buildJsonObject {
        put("token_type", "bearer")
        put("access_token", code)
    }

    override suspend fun getUser(access: JsonObject): JsonObject = buildJsonObject {
        put("email", access.code().substringAfter(":"))
    }

    override suspend fun getEmails(access: JsonObject): JsonArray = buildJsonArray {
        add(
            buildJsonObject {
                put("email", access.code().substringAfter(":"))
                put("primary", true)
                put("verified", access.code().startsWith("verified:"))
            }
        )
    }

    private fun JsonObject.code() = getValue("access_token").jsonPrimitive.content

    fun verified(email: String) = "verified:$email"

    fun unverified(email: String) = "unverified:$email"
}

/**
 * Test hook inside [ActivationHardeningRealm]'s `retire()`, AFTER the framework claimed the account and stripped its
 * credentials and BEFORE the rename: a spec runs a concurrent activation there, or makes the retire fail part-way.
 */
object RetireGate {
    @Volatile
    var beforeRename: suspend () -> Unit = {}

    /**
     * Runs right after a retire's CLAIM landed, before anything is stripped — the window in which an activation
     * that already read the (then unclaimed) marker would try to commit.
     */
    @Volatile
    var afterRetireClaim: suspend () -> Unit = {}
}

/** The real adapter, with [RetireGate.afterRetireClaim] run after a successful retire claim. */
class RetireClaimGatedAdapter(private val inner: AuthRecordStorage.Adapter) : AuthRecordStorage.Adapter by inner {
    override suspend fun <T : AuthRecord> replaceRecordIfUnclaimed(stored: Stored<T>): Stored<T>? =
        inner.replaceRecordIfUnclaimed(stored).also { replaced ->
            val claim = (stored.value() as? AuthRecord.PendingActivation)?.claimedBy
            if (replaced != null && claim == AuthRecord.PendingActivation.CLAIMED_BY_RETIRE) RetireGate.afterRetireClaim()
        }
}

//  The realm  //////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Production token config on purpose: the specs drive a clock they can advance instead of shortening the
 * delays and lifetimes, so what they pin is what ships.
 */
class ActivationHardeningRealm(
    deps: Lazy<AuthSystem.Deps>,
    users: Lazy<HardeningUsers>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
) : AuthRealm<TestUser> {
    companion object {
        val REALM = RealmId("activation-hardening")
    }

    override val deps: AuthSystem.Deps by deps
    private val store: HardeningUsers by users
    private val emailAndPassword: EmailAndPasswordAuth.Factory by emailAndPassword

    override val id: RealmId = REALM

    override val messaging: AuthRealm.Messaging<TestUser> = AuthRealm.DefaultMessaging(
        senderEmail = "test@example.com",
        senderName = "Test",
        applicationName = "Activation Hardening",
        realm = this,
    )

    override val providers by lazy {
        listOf(
            emailAndPassword(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://example.com/auth"),
                capabilities = setOf(Capability.SignIn, Capability.SignUp),
            ),
            GithubSsoAuth(
                capabilities = setOf(Capability.SignIn, Capability.SignUp),
                clientId = "e2e",
                clientSecret = "e2e",
                remoteClient = lazy { FakeGithub },
            ),
        )
    }

    override val users = object : AuthUserAdapter<TestUser> {
        override suspend fun loadById(id: UserId) = store.byId(id.value)

        override suspend fun loadByEmail(email: EmailAddress) = store.byEmail(email)

        override suspend fun createForSignup(params: AuthUserAdapter.CreateUserForSignupParams) =
            store.create(TestUser(name = params.displayName, email = params.email))

        override suspend fun retire(user: Stored<TestUser>, retirement: AccountRetirement): Stored<TestUser> {
            RetireGate.beforeRename()
            // `name` defaults to the address's local part: it goes with the address.
            return store.update(user.modify { it.copy(email = retirement.rename(it.email), name = "retired") })
        }

        override suspend fun serialize(user: Stored<TestUser>): JsonObject =
            Json.encodeToJsonElement(TestUser.serializer(), user.resolve()).jsonObject
    }

    override suspend fun generateJwt(user: Stored<TestUser>, selectedOrg: SelectedOrg?): String {
        val value = user.resolve()

        return deps.jwtGenerator.createJwt(
            user = JwtUserData(id = UserId(user._id), desc = value.name, type = TestUser.USER_TYPE, email = value.email),
            permissions = UserPermissions(),
        ) {
            // The SYSTEM clock: the JWT verifier does not know about the specs' advanced clock.
            withExpiresAt(Kronos.systemUtc.instantNow().plus(1.hours).jvm)
        }
    }
}

//  One app per backend  //////////////////////////////////////////////////////////////////////////////////////

private fun createHardeningBlueprint(
    config: FunktorAllTestConfig,
    authBackend: FunktorAuthBuilder.() -> Unit,
    users: KontainerBuilder.() -> Unit,
) = kontainer {
    funktor(
        config = config,
        rest = { jwt() },
        logging = { useKarango() },
        cluster = { useKarango() },
        messaging = { useKarango() },
        saas = { useKarango() },
        auth = authBackend,
    )

    karango(config = ArangoDbConfig.forUnitTests)
    monko(config = MongoDbConfig.forUnitTests)

    // A clock the specs can move: seven days of pending lifetime and hours of resend delay, in milliseconds.
    instance(Kronos::class, Kronos.systemUtc.mutable())

    dynamic(ActivationHardeningRealm::class)
    // The real Vault adapter of the configured backend, wrapped so a spec can act right after a retire's claim.
    dynamic(AuthRecordStorage.Adapter::class) { repo: AuthRecordStorage.Adapter.Vault.Repo ->
        RetireClaimGatedAdapter(AuthRecordStorage.Adapter.Vault(repo))
    }
    users()
}

val activationHardeningKarangoApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config ->
        AppKontainers(
            createHardeningBlueprint(config, authBackend = { useKarango() }) {
                dynamic(HardeningKaUsersRepo::class)
            }
        )
    },
)

val activationHardeningMonkoApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config ->
        AppKontainers(
            createHardeningBlueprint(config, authBackend = { useMonko() }) {
                dynamic(HardeningMoUsersRepo::class)
            }
        )
    },
)
