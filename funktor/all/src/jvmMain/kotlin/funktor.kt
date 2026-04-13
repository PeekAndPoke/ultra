package io.peekandpoke.funktor

import io.peekandpoke.funktor.auth.FunktorAuthBuilder
import io.peekandpoke.funktor.auth.funktorAuth
import io.peekandpoke.funktor.cluster.FunktorClusterBuilder
import io.peekandpoke.funktor.cluster.funktorCluster
import io.peekandpoke.funktor.core.broker.funktorBroker
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.funktorCore
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.core.model.default
import io.peekandpoke.funktor.insights.funktorInsights
import io.peekandpoke.funktor.inspect.introspection.funktorIntrospection
import io.peekandpoke.funktor.logging.FunktorLoggingBuilder
import io.peekandpoke.funktor.logging.funktorLogging
import io.peekandpoke.funktor.messaging.FunktorMessagingBuilder
import io.peekandpoke.funktor.messaging.funktorMessaging
import io.peekandpoke.funktor.rest.FunktorRestBuilder
import io.peekandpoke.funktor.rest.funktorRest
import io.peekandpoke.funktor.staticweb.funktorStaticWeb
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

/** All-in-one Funktor kontainer module: registers core, auth, broker, rest, cluster, logging, messaging, and insights. */
val Funktor = module { params: FunktorParams ->
    funktorCore(params.config, params.appInfo)
    funktorAuth(params.auth)
    funktorBroker()
    funktorRest(params.config, params.rest)
    funktorCluster(params.cluster)
    funktorLogging(params.logging)
    funktorStaticWeb()
    funktorMessaging(params.messaging)
    funktorInsights()
    funktorIntrospection()
}

/** Convenience function for registering the full Funktor stack with config and builder lambdas. */
fun KontainerBuilder.funktor(
    config: AppConfig,
    appInfo: AppInfo = AppInfo.default(),
    rest: FunktorRestBuilder.() -> Unit = {},
    logging: FunktorLoggingBuilder.() -> Unit = {},
    cluster: FunktorClusterBuilder.() -> Unit = {},
    messaging: FunktorMessagingBuilder.() -> Unit = {},
    auth: FunktorAuthBuilder.() -> Unit = {},
) = module(
    Funktor,
    FunktorParams(
        appInfo = appInfo,
        config = config,
        rest = rest,
        logging = logging,
        cluster = cluster,
        messaging = messaging,
        auth = auth,
    )
)

/** Parameters bundle for the all-in-one [Funktor] module. */
@ConsistentCopyVisibility
data class FunktorParams internal constructor(
    val config: AppConfig,
    val appInfo: AppInfo,
    val rest: FunktorRestBuilder.() -> Unit,
    val logging: FunktorLoggingBuilder.() -> Unit,
    val cluster: FunktorClusterBuilder.() -> Unit,
    val messaging: FunktorMessagingBuilder.() -> Unit,
    val auth: FunktorAuthBuilder.() -> Unit,
)
