package de.peekandpoke.funktor

import de.peekandpoke.funktor.cluster.FunktorClusterBuilder
import de.peekandpoke.funktor.cluster.funktorCluster
import de.peekandpoke.funktor.core.broker.funktorBroker
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.funktorCore
import de.peekandpoke.funktor.core.model.AppInfo
import de.peekandpoke.funktor.core.model.default
import de.peekandpoke.funktor.insights.funktorInsights
import de.peekandpoke.funktor.logging.FunktorLoggingBuilder
import de.peekandpoke.funktor.logging.funktorLogging
import de.peekandpoke.funktor.messaging.FunktorCMessagingBuilder
import de.peekandpoke.funktor.messaging.funktorMessaging
import de.peekandpoke.funktor.rest.FunktorRestBuilder
import de.peekandpoke.funktor.rest.funktorRest
import de.peekandpoke.funktor.staticweb.funktorStaticWeb
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

// Kontainer module

fun KontainerBuilder.funktor(
    config: AppConfig,
    appInfo: AppInfo = AppInfo.default(),
    rest: FunktorRestBuilder.() -> Unit = {},
    logging: FunktorLoggingBuilder.() -> Unit = {},
    cluster: FunktorClusterBuilder.() -> Unit = {},
    messaging: FunktorCMessagingBuilder.() -> Unit = {},
) = module(
    Funktor,
    FunktorParams(
        appInfo = appInfo,
        config = config,
        rest = rest,
        logging = logging,
        cluster = cluster,
        messaging = messaging,
    )
)

@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
data class FunktorParams internal constructor(
    val config: AppConfig,
    val appInfo: AppInfo,
    val rest: FunktorRestBuilder.() -> Unit,
    val logging: FunktorLoggingBuilder.() -> Unit,
    val cluster: FunktorClusterBuilder.() -> Unit,
    val messaging: FunktorCMessagingBuilder.() -> Unit,
)

/** Module definition */
val Funktor = module { params: FunktorParams ->
    funktorCore(params.config, params.appInfo)
    funktorBroker()
    funktorRest(params.rest)
    funktorCluster(params.cluster)
    funktorLogging(params.logging)
    funktorStaticWeb()
    funktorMessaging(params.messaging)
    funktorInsights()
}
