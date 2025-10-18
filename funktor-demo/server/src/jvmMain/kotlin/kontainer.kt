package io.peekandpoke.funktor.demo.server

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.funktor.core.App
import de.peekandpoke.funktor.core.installKontainer
import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.funktor
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.senders.ExampleDomainsIgnoringEmailSender
import de.peekandpoke.funktor.messaging.senders.aws.AwsSesSender
import de.peekandpoke.funktor.rest.auth.jwtUserProvider
import de.peekandpoke.karango.karango
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.security.jwt.JwtConfig
import de.peekandpoke.ultra.vault.profiling.DefaultQueryProfiler
import io.ktor.server.routing.*
import io.peekandpoke.funktor.demo.server.api.api.ApiApp
import java.io.File

data class KeysConfig(val config: Config)

fun Route.installApiKontainer(app: App<FunktorDemoConfig>, insights: InsightsConfig?) = installKontainer { call ->
    app.kontainers.create {
        // user record provider
        with { call.jwtUserProvider() }
        // Insights config
        insights?.let { with { insights } }
        // Database query profile
        if (app.config.arangodb.flags.enableProfiler) {
            with {
                DefaultQueryProfiler(explainQueries = app.config.arangodb.flags.enableExplain)
            }
        }
    }
}

fun createBlueprint(config: FunktorDemoConfig) = kontainer {
    // Mount all KtorFx things
    funktor(
        config = config,
        rest = {
            jwt(
                JwtConfig(
                    singingKey = config.auth.apiJwtSigningKey,
                    permissionsNs = "permissions",
                    userNs = "user",
                    issuer = "https://api.funktor-demo.io",
                    audience = "api.funktor-demo.io",
                )
            )
        },
        logging = {
            useKarango()
        },
        cluster = {
            useKarango()
        },
        messaging = {
            useKarango()
        }
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

//    module(ReaktorAuth)
//    // TODO: add config builder to ReaktorAuth() to enable karango storage
//    dynamic(AuthStorage::class, KarangoAuthStorage::class)
//    dynamic(KarangoAuthRecordsRepo::class)
//    dynamic(KarangoAuthRecordsRepo.Fixtures::class)

    // Mount Karango
    karango(config = config.arangodb)

    // Keys config
    instance(
        KeysConfig(
            ConfigFactory.parseFile(File("./config/keys.env.conf"))
        )
    )

    // Apps
    singleton(ApiApp::class)

    // Modules
//    module(AppUserModule)

    // AppUser services
}
