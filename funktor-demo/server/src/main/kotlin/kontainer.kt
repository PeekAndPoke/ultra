package io.peekandpoke.funktor.demo.server

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.funktor.core.App
import de.peekandpoke.funktor.core.installKontainer
import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.funktor
import de.peekandpoke.funktor.insights.instrumentWithInsights
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.senders.ExampleDomainsIgnoringEmailSender
import de.peekandpoke.funktor.messaging.senders.aws.AwsSesSender
import de.peekandpoke.funktor.rest.auth.jwtUserProvider
import de.peekandpoke.karango.karango
import de.peekandpoke.monko.monko
import de.peekandpoke.ultra.kontainer.kontainer
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
    val awsSender: AwsSesSender by lazy {
        AwsSesSender.of(config = config.aws.ses)
    }

    singleton(EmailSender::class) {
        ExampleDomainsIgnoringEmailSender(
            wrapped = awsSender,
        )
    }

    // Apps
    singleton(ApiApp::class)

    // Modules
    module(AdminUserModule)
}
