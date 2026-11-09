package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.core.lifecycle.AppStartException
import io.peekandpoke.funktor.core.model.InsightsConfig

/**
 * Whether this environment may run insights (ULTRA-00225): only a development one, `dev`, `test` or `qa*`
 * ([KtorConfig.isDevelopment]).
 *
 * An allow-list on purpose. A record holds request headers, user records, config and queries, so every other name
 * counts as production: `prod` and `live`, but also `staging`, a typo, or an unresolved `${ENV}`.
 */
internal val KtorConfig.allowsInsights: Boolean get() = isDevelopment

/** True when [this] turns insights on and [ktor]'s environment allows it. */
internal fun InsightsConfig?.isActiveIn(ktor: KtorConfig): Boolean = this?.enabled == true && ktor.allowsInsights

/**
 * Refuses to boot when [config] turns insights on in an environment that does not allow it. Loud on purpose:
 * quietly switching it off would hide a production config that asks for it.
 */
internal fun refuseInsightsOutsideDevelopment(config: InsightsConfig?, ktor: KtorConfig) {
    if (config?.enabled != true || ktor.allowsInsights) return

    throw AppStartException(
        "Insights is enabled, but the environment '${ktor.deployment.environment}' is not a development one " +
                "(dev, test, qa*). Insights records headers, users, config and queries, so it never runs in " +
                "production. Pass a disabled InsightsConfig to instrumentWithInsights in this environment."
    )
}
