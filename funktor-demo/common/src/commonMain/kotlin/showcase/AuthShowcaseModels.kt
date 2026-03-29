package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

@Serializable
data class AuthRuleCheckResult(
    val ruleName: String,
    val description: String,
    val accessGranted: Boolean,
)
