package io.peekandpoke.funktor

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.cluster.FunktorClusterApiFeature
import io.peekandpoke.funktor.cluster.locks.GlobalLocksProvider
import io.peekandpoke.funktor.cluster.locks.VaultGlobalLocksProvider
import io.peekandpoke.funktor.cluster.locks.karango.KarangoGlobalLocksRepo
import io.peekandpoke.funktor.cluster.locks.monko.MonkoGlobalLocksRepo
import io.peekandpoke.funktor.core.App
import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.insights.InsightsFileRepository
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.funktor.inspect.cluster.locks.api.GlobalLockEntryModel
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.funktor.testing.AppUnderTest
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.security.jwt.JwtGenerator
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The default test blueprint, with the cluster (and so the global locks) on Monko instead of Karango. */
private fun createMonkoClusterBlueprint(config: FunktorAllTestConfig) = kontainer {
    funktor(
        config = config,
        rest = { jwt() },
        logging = { useKarango() },
        cluster = { useMonko() },
        messaging = { useKarango() },
        saas = { useKarango() },
        auth = { useKarango() },
    )

    karango(config = ArangoDbConfig.forUnitTests)
    monko(config = MongoDbConfig.forUnitTests)

    dynamic(InsightsRepository::class) { InsightsFileRepository(insightsDepotDir) }

    module(TestUserModule)
}

private val monkoClusterTestApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config -> AppKontainers(createMonkoClusterBlueprint(config)) },
)

/** The global locks listing on Karango, the default test app's backend. */
class KarangoGlobalLocksE2eSpec : GlobalLocksE2eSpec(testApp, KarangoGlobalLocksRepo::class)

/** The global locks listing on Monko. */
class MonkoGlobalLocksE2eSpec : GlobalLocksE2eSpec(monkoClusterTestApp, MonkoGlobalLocksRepo::class)

/**
 * End-to-end: a lock held through the app's [GlobalLocksProvider] is listed by `listGlobalLocks` with its last
 * heartbeat, and without its token.
 */
abstract class GlobalLocksE2eSpec(
    app: App.Definition<FunktorAllTestConfig>,
    private val backend: KClass<out VaultGlobalLocksProvider.Repo>,
) : AppSpec<FunktorAllTestConfig>(app) {

    private val api by service(FunktorClusterApiFeature::class)
    private val locks by service(GlobalLocksProvider::class)
    private val jwt by service(JwtGenerator::class)

    private val superUserToken: String by lazy {
        jwt.createJwt(
            user = JwtUserData(
                id = UserId("e2e-locks"),
                desc = "e2e",
                type = TestUser.USER_TYPE,
                email = EmailAddress("e2e-locks@test.com"),
            ),
            permissions = UserPermissions(isSuperUser = true),
        )
    }

    private suspend fun AppUnderTest<FunktorAllTestConfig>.listLocks(
        assertions: suspend (body: String, listed: List<GlobalLockEntryModel>) -> Unit,
    ) {
        authenticate(superUserToken) {
            request(api.globalLocks.listGlobalLocks) {
                status shouldBe HttpStatusCode.OK
                assertions(bodyAsText(), apiResponseData<List<GlobalLockEntryModel>>()!!)
            }
        }
    }

    init {
        "A lock held through the app's provider is listed with its last heartbeat, and without its token" {
            val key = "e2e-listed-lock"
            val repository = (locks as VaultGlobalLocksProvider).repository
            // The backend this spec claims to run on.
            repository::class shouldBe backend

            val holding = CompletableDeferred<Unit>()
            val done = CompletableDeferred<Unit>()

            // The first request starts the app, whose start releases every lock this server holds: take it after.
            apiApp {
                listLocks { _, listed -> listed.filter { it.key == key }.shouldBeEmpty() }
            }

            coroutineScope {
                val holder = async {
                    locks.lock(key, timeout = 5.seconds) { holding.complete(Unit); done.await() }
                }

                holding.await()

                val held = repository.findById(key)!!.resolve()
                // The holder's next beat is 20 s away: beat once now, at a moment distinct from `created`.
                val beat = held.created.plus(5.seconds)
                repository.heartbeat(key = key, token = held.token, heartbeatAt = beat, expiresAt = beat.plus(15.minutes)) shouldBe true

                apiApp {
                    listLocks { body, listed ->
                        body shouldNotContain held.token

                        listed.single { it.key == key }.let {
                            it.serverId shouldBe held.serverId
                            it.created.toEpochMillis() shouldBe held.created.toEpochMillis()
                            it.lastHeartbeat.toEpochMillis() shouldBe beat.toEpochMillis()
                            it.expires.toEpochMillis() shouldBe beat.plus(15.minutes).toEpochMillis()
                        }
                    }
                }

                done.complete(Unit)
                holder.await()
            }

            // Released by its holder.
            apiApp {
                listLocks { _, listed -> listed.filter { it.key == key }.shouldBeEmpty() }
            }
        }
    }
}
