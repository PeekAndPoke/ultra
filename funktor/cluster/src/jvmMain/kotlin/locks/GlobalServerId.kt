package io.peekandpoke.funktor.cluster.locks

import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.ultra.common.network.NetworkUtils

class GlobalServerId(config: AppConfig) {
    companion object {
        fun withoutConfig() = GlobalServerId(AppConfig.empty)
    }

    private val id: String = listOf(
        config.ktor.application.id,
        config.ktor.deployment.environment,
        config.ktor.deployment.port.toString(),
        NetworkUtils.getHostNameOrDefault(),
        NetworkUtils.getNetworkFingerPrint(),
    ).joinToString("_") {
        it.lowercase().replace("[^a-zA-Z0-9]+".toRegex(), "-")
    }

    fun getId() = id
}
