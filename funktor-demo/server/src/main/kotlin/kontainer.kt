package io.peekandpoke.funktor.demo.server

import io.ktor.server.routing.Route
import io.peekandpoke.funktor.core.App
import io.peekandpoke.funktor.core.installKontainer
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.demo.server.admin.AdminUserModule
import io.peekandpoke.funktor.demo.server.api.ApiApp
import io.peekandpoke.funktor.demo.server.b2b.B2bModule
import io.peekandpoke.funktor.demo.server.b2b2c.B2b2cModule
import io.peekandpoke.funktor.demo.server.operator.OperatorModule
import io.peekandpoke.funktor.demo.server.funktorconf.FunktorConfModule
import io.peekandpoke.funktor.demo.server.showcase.ShowcaseModule
import io.peekandpoke.funktor.funktor
import io.peekandpoke.funktor.insights.instrumentWithInsights
import io.peekandpoke.funktor.messaging.senders.aws.AwsSesSender
import io.peekandpoke.funktor.rest.auth.currentUserProvider
import io.peekandpoke.karango.karango
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.funktor.codegen.funktorCodegen

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
            // The ONLY thing this app supplies. Dev overrides, test-mode capture, persisting what
            // was sent and the debug log are applied by funktor:messaging itself.
            useSender(devConfig = config.devOverrides?.mailing) { AwsSesSender.of(config.aws.ses) }
//          useSender(devConfig = config.devOverrides?.mailing) { SendgridSender.of(config.sendgrid) }
        },
        auth = {
            useKarango()
        },
        saas = {
            useKarango()
        }
    )

    // The TypeScript SDK generator. Dev-time only — it adds a CLI command (`sdk:ts:generate`) and
    // contributes nothing to request handling.
    funktorCodegen()

    // Mount ArangoDb
    karango(config = config.arangodb)

    // Mount MongoDb
    monko(config = config.mongodb)

    // Apps
    singleton(ApiApp::class)

    // Modules
    module(OperatorModule)
    module(B2bModule)
    module(B2b2cModule)

    // TO be removed ... old stuff
    module(AdminUserModule)
    module(ShowcaseModule)
    module(FunktorConfModule)
}
