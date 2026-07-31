package io.peekandpoke.funktor.insights.collectors

import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.insights.ConfigRedaction
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.InsightsMapper

class AppConfigCollector(
    mapper: InsightsMapper,
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
                // AppInfo is version metadata — nothing sensitive, and the git branch is the point
                appInfo != null -> mapper.convertValue<Map<*, *>>(appInfo)
                else -> "not available"
            },
            config = when {
                // INTERIM redaction — see ConfigRedaction, and delete both when `Redacted<T>` lands.
                // Without this the JWT signing key, the CSRF secret and the database password are
                // written verbatim into every record.
                appConfig != null -> ConfigRedaction.redact(mapper.convertValue<Map<*, *>>(appConfig))
                    ?: "not available"

                else -> "not available"
            }
        )
    }

    /** VUE-REF: `reference/collectors/AppConfigCollector.kt` */
    data class Data(val info: Any, val config: Any) : InsightsCollectorData

    override fun finish(call: ApplicationCall): Data = static
}
