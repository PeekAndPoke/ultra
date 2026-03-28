package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

@Serializable
data class AppLifecycleInfo(
    val startedAt: String,
    val uptimeSeconds: Long,
    val jvmVersion: String,
    val kotlinVersion: String,
)
