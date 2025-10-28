package de.peekandpoke.funktor.testing

import de.peekandpoke.funktor.core.App
import de.peekandpoke.funktor.core.AppArgs
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.config.AppConfig.Companion.typed
import de.peekandpoke.funktor.core.provideApp
import de.peekandpoke.funktor.core.provideKontainer
import de.peekandpoke.ultra.kontainer.Kontainer
import io.ktor.server.config.*
import io.ktor.server.testing.*

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
             * NOTICE: DIRTY-HACK work-around to make the [backend] available tp [App.Definition.module]
             * @see App.Definition.module()
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
