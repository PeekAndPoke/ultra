package de.peekandpoke.funktor.auth

import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.karango.karango
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class AuthRealmSpec : FreeSpec() {

    private suspend fun createKontainer(block: KontainerBuilder.() -> Unit = {}): Kontainer {
        return createAuthTestContainer(
            configureKontainer = {
                karango(ArangoDbConfig.forUnitTests)

                block()
            },
            configureAuth = { useKarango() }
        )
    }

    init {
        "Registering a single realm" {
            val kontainer = createKontainer {
                dynamic(KarangoTestAppUsersRepo::class)
                dynamic(TestAppUserRealm::class)
            }

            val realms = kontainer.funktorAuth.realms

            withClue("One realm must be registered") {
                realms shouldHaveSize 1
            }

            val testUserRealm = realms.first()

            withClue("Realm must have the correct id and providers") {
                testUserRealm.id shouldBe TestAppUserRealm.REALM
                testUserRealm.providers shouldHaveSize 1
            }
        }

        "Registering multiple realms" {

        }
    }
}
