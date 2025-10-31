package io.peekandpoke.funktor.demo.server

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.funktor.core.App
import de.peekandpoke.funktor.core.installKontainer
import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.funktor
import de.peekandpoke.funktor.insights.instrumentWithInsights
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.senders.ignoreExampleDomains
import de.peekandpoke.funktor.messaging.senders.sendgrid.SendgridSender
import de.peekandpoke.funktor.messaging.senders.withHooks
import de.peekandpoke.funktor.messaging.senders.withOverrides
import de.peekandpoke.funktor.messaging.storage.StoringEmailHook
import de.peekandpoke.funktor.rest.auth.jwtUserProvider
import de.peekandpoke.karango.karango
import de.peekandpoke.monko.monko
import de.peekandpoke.ultra.common.modifyIf
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.log.Log
import io.ktor.server.routing.*
import io.peekandpoke.funktor.demo.server.admin.AdminUserModule
import io.peekandpoke.funktor.demo.server.api.ApiApp
import java.io.File

data class KeysConfig(val config: Config)

fun Route.installWwwKontainer(app: App<FunktorDemoConfig>, insights: InsightsConfig?) {
    installKontainer {
        app.kontainers.create {
            // Insights config
            insights?.let { with { insights } }
        }
    }

    instrumentWithInsights(insights)
}

fun Route.installApiKontainer(app: App<FunktorDemoConfig>, insights: InsightsConfig?) {
    installKontainer { call ->
        app.kontainers.create {
            // user record provider
            with { call.jwtUserProvider() }
            // Insights config
            insights?.let { with { insights } }
        }
    }

    instrumentWithInsights(insights)
}

fun createBlueprint(config: FunktorDemoConfig) = kontainer {
    // Mount all KtorFx things
    funktor(
        config = config,
        rest = {
            jwt(config.auth.jwt)
        },
        logging = {
            useKarango()
        },
        cluster = {
            useKarango()
        },
        messaging = {
            useKarango()
        },
        auth = {
            useMonko()
        }
    )

    // Mount ArangoDb
    karango(config = config.arangodb)

    // Mount MongoDb
    monko(config = config.mongodb)

    // Keys config
    instance(
        KeysConfig(
            ConfigFactory.parseFile(File("./config/keys.env.conf"))
        )
    )

    // Mailing
//    val mailSender: AwsSesSender by lazy { AwsSesSender.of(config = config.aws.ses) }
    val mailSender: SendgridSender by lazy { SendgridSender.of(config = config.sendgrid) }

    singleton(EmailSender::class) { log: Log, storing: StoringEmailHook ->
        mailSender
            .ignoreExampleDomains()
            .withHooks(log) {
                onAfterSend(storing)

                onAfterSend { email, result ->
                    log.info("Email sent: $email, result: $result")
                }
            }.modifyIf(config.ktor.isDevelopment) {
                withOverrides {
                    developmentMode(config, toEmail = "")
                }
            }
    }

    // Apps
    singleton(ApiApp::class)

    // Modules
    module(AdminUserModule)
}
