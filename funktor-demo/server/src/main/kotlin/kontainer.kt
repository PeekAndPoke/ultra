package io.peekandpoke.funktor.demo.server

import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.App
import io.peekandpoke.funktor.core.installKontainer
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.demo.server.admin.AdminUserModule
import io.peekandpoke.funktor.demo.server.api.ApiApp
import io.peekandpoke.funktor.demo.server.funktorconf.FunktorConfModule
import io.peekandpoke.funktor.demo.server.showcase.ShowcaseModule
import io.peekandpoke.funktor.funktor
import io.peekandpoke.funktor.insights.instrumentWithInsights
import io.peekandpoke.funktor.messaging.EmailSender
import io.peekandpoke.funktor.messaging.senders.applyDevConfig
import io.peekandpoke.funktor.messaging.senders.aws.AwsSesSender
import io.peekandpoke.funktor.messaging.senders.withHooks
import io.peekandpoke.funktor.messaging.storage.StoringEmailHook
import io.peekandpoke.funktor.rest.auth.currentUserProvider
import io.peekandpoke.karango.karango
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.Log

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
            with { call.currentUserProvider() }
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
            jwt()
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
            useKarango()
        }
    )

    // Mount ArangoDb
    karango(config = config.arangodb)

    // Mount MongoDb
    monko(config = config.mongodb)

    // Mailing
    val mailSender: AwsSesSender by lazy { AwsSesSender.of(config = config.aws.ses) }
//    val mailSender: SendgridSender by lazy { SendgridSender.of(config = config.sendgrid) }

    singleton(EmailSender::class) { log: Log, storing: StoringEmailHook ->
        mailSender
            .applyDevConfig(config = config, devConfig = config.devOverrides?.mailing)
            .withHooks(log) {
                onAfterSend(storing)

                onAfterSend { email, result ->
                    log.debug("Email sent: $email, result: $result")
                }
            }
    }

    // Apps
    singleton(ApiApp::class)

    // Modules
    module(AdminUserModule)
    module(ShowcaseModule)
    module(FunktorConfModule)
}
