package io.peekandpoke.funktor.testing

import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.peekandpoke.funktor.core.App
import io.peekandpoke.funktor.core.AppArgs
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.AppConfig.Companion.typed
import io.peekandpoke.funktor.core.provideApp
import io.peekandpoke.funktor.core.provideKontainer
import io.peekandpoke.ultra.kontainer.Kontainer

/** Encapsulates a running test application: the [App], its system [Kontainer], and the Ktor [TestApplication]. */
class TestBed<C : AppConfig>(
    val app: App<C>,
    val kontainer: Kontainer,
    val engine: TestApplication,
) {
    companion object {
        fun <C : AppConfig> App.Definition<C>.createTestBed(env: String = "test"): TestBed<C> {

            val rawConfig = AppConfig.loadEnv(env = env)
            val typedConfig = rawConfig.typed(configType)

            val app = App(
                args = AppArgs(emptyList()),
                config = typedConfig,
                kontainers = kontainers(typedConfig),
            )

            val kontainer = app.kontainers.system()

            /**
             * TODO: fix this
             * NOTICE: DIRTY-HACK work-around to make the [app] available tp [App.Definition.module]
             * @see App.Definition.module
             */
            App.Definition.testApp = app

            val testApplication = TestApplication {
                // NOTICE: this is currently called too late...
                application {
                    attributes.provideApp(app)
                    attributes.provideKontainer(kontainer)
                }

                environment {
                    config = HoconApplicationConfig(rawConfig)
                }
            }

            return TestBed(
                app = app,
                kontainer = kontainer,
                engine = testApplication,
            )
        }
    }
}
