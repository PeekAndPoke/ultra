package de.peekandpoke.funktor.rest.security

import de.peekandpoke.funktor.rest.ApiRoute
import de.peekandpoke.funktor.rest.RestDslMarkerConfig
import de.peekandpoke.funktor.rest.RestSecurityRuleMarker
import de.peekandpoke.ultra.common.TypedKey

data class EndpointSecurity(
    val allowsSensitiveData: Boolean,
) {
    companion object {
        val default = EndpointSecurity(
            allowsSensitiveData = false,
        )
    }

    class Builder {
        private var allowsSensitiveData: Boolean = false

        @RestSecurityRuleMarker()
        fun allowsSensitiveData() {
            allowsSensitiveData = true
        }

        internal fun build() = EndpointSecurity(
            allowsSensitiveData = allowsSensitiveData,
        )
    }
}

val EndpointSecurityKey = TypedKey<EndpointSecurity>("EndpointSecurity")

val ApiRoute<*>.security get() = attributes[EndpointSecurityKey] ?: EndpointSecurity.default

@RestDslMarkerConfig
fun <RESPONSE> ApiRoute.Plain<RESPONSE>.security(
    block: EndpointSecurity.Builder.(ApiRoute.Plain<RESPONSE>) -> Unit,
): ApiRoute.Plain<RESPONSE> {
    return withAttribute(EndpointSecurityKey, EndpointSecurity.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <PARAMS, RESPONSE> ApiRoute.WithParams<PARAMS, RESPONSE>.security(
    block: EndpointSecurity.Builder.(ApiRoute.WithParams<PARAMS, RESPONSE>) -> Unit,
): ApiRoute.WithParams<PARAMS, RESPONSE> {
    return withAttribute(EndpointSecurityKey, EndpointSecurity.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <BODY, RESPONSE> ApiRoute.WithBody<BODY, RESPONSE>.security(
    block: EndpointSecurity.Builder.(ApiRoute.WithBody<BODY, RESPONSE>) -> Unit,
): ApiRoute.WithBody<BODY, RESPONSE> {
    return withAttribute(EndpointSecurityKey, EndpointSecurity.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <PARAMS, BODY, RESPONSE> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.security(
    block: EndpointSecurity.Builder.(ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>) -> Unit,
): ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE> {
    return withAttribute(EndpointSecurityKey, EndpointSecurity.Builder().also { it.block(this) }.build())
}

