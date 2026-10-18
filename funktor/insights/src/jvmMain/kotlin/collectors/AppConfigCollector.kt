package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.InsightsCodec
import io.peekandpoke.ultra.slumber.slumber

class AppConfigCollector(
    codec: InsightsCodec,
    appConfig: AppConfig? = null,
    appInfo: AppInfo? = null,
) : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "app-config"
    }

    /**
     * Lazy, not eager: constructing this collector used to Jackson-serialise the entire [AppConfig]
     * there and then. `CollectorKeyUniquenessCheck` instantiates every collector at boot, so an app that
     * had insights DISABLED still materialised its whole config — signing key included — in memory at
     * start-up. Now that only happens when a record is actually written.
     */
    val static: Data by lazy {
        Data(
            info = when {
                appInfo != null -> codec.slumber(appInfo) ?: "not available"
                else -> "not available"
            },
            // Slumber, so `Redacted<T>` is honoured by RedactedSlumberer and the signing key, the CSRF
            // secret and the database password leave as `***redacted***`. Jackson had no idea the type
            // existed and wrote `{"value": "<the secret>"}`, which is why the name-based ConfigRedaction
            // had to exist until now.
            config = when {
                appConfig != null -> codec.slumber(appConfig) ?: "not available"
                else -> "not available"
            }
        )
    }

    /** VUE-REF: `reference/collectors/AppConfigCollector.kt` */
    data class Data(val info: Any, val config: Any) : InsightsCollectorData

    override fun finish(call: ApplicationCall): Data = static
}
