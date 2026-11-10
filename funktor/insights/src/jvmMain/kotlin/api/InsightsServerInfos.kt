package io.peekandpoke.funktor.insights.api

import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.core.model.AppVersion

/** The running server's [InsightsServerInfo]: its git version and its ktor application id and environment. */
fun InsightsServerInfo.Companion.of(version: AppVersion, ktor: KtorConfig) = InsightsServerInfo(
    git = version.describeGit(),
    gitDesc = version.gitDesc,
    applicationId = ktor.application.id,
    environment = ktor.deployment.environment,
)
