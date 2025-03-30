package de.peekandpoke.ktorfx

import de.peekandpoke.ktorfx.cluster.KtorFXClusterBuilder
import de.peekandpoke.ktorfx.cluster.ktorFxCluster
import de.peekandpoke.ktorfx.core.broker.ktorFxBroker
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.core.ktorFxCore
import de.peekandpoke.ktorfx.core.model.AppInfo
import de.peekandpoke.ktorfx.core.model.default
import de.peekandpoke.ktorfx.insights.ktorFxInsights
import de.peekandpoke.ktorfx.logging.KtorFXLoggingBuilder
import de.peekandpoke.ktorfx.logging.ktorFxLogging
import de.peekandpoke.ktorfx.messaging.KtorFXCMessagingBuilder
import de.peekandpoke.ktorfx.messaging.ktorFxMessaging
import de.peekandpoke.ktorfx.rest.KtorFXRestBuilder
import de.peekandpoke.ktorfx.rest.ktorFxRest
import de.peekandpoke.ktorfx.staticweb.ktorFxStaticWeb
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

// Kontainer module

fun KontainerBuilder.ktorFx(
    config: AppConfig,
    appInfo: AppInfo = AppInfo.default(),
    rest: KtorFXRestBuilder.() -> Unit = {},
    logging: KtorFXLoggingBuilder.() -> Unit = {},
    cluster: KtorFXClusterBuilder.() -> Unit = {},
    messaging: KtorFXCMessagingBuilder.() -> Unit = {},
) = module(
    KtorFX,
    KtorFXParams(
        appInfo = appInfo,
        config = config,
        rest = rest,
        logging = logging,
        cluster = cluster,
        messaging = messaging,
    )
)

@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
data class KtorFXParams internal constructor(
    val config: AppConfig,
    val appInfo: AppInfo,
    val rest: KtorFXRestBuilder.() -> Unit,
    val logging: KtorFXLoggingBuilder.() -> Unit,
    val cluster: KtorFXClusterBuilder.() -> Unit,
    val messaging: KtorFXCMessagingBuilder.() -> Unit,
)

/** Module definition */
val KtorFX = module { params: KtorFXParams ->
    ktorFxCore(params.config, params.appInfo)
    ktorFxBroker()
    ktorFxRest(params.rest)
    ktorFxCluster(params.cluster)
    ktorFxLogging(params.logging)
    ktorFxStaticWeb()
    ktorFxMessaging(params.messaging)
    ktorFxInsights()
}
