package de.peekandpoke.funktor.rest.docs

import de.peekandpoke.funktor.rest.ApiRoute
import de.peekandpoke.funktor.rest.RestDslMarkerConfig
import de.peekandpoke.ultra.common.TypedKey

data class EndpointDocs(
    val name: String?,
    val description: String?,
) {
    companion object {
        val default = EndpointDocs(
            name = null,
            description = null,
        )
    }

    class Builder {
        var name: String? = null
        var description: String? = null

        internal fun build() = EndpointDocs(
            name = name,
            description = description,
        )
    }
}

val EndpointDocsKey = TypedKey<EndpointDocs>("EndpointDocs")

val ApiRoute<*>.docs get() = attributes[EndpointDocsKey] ?: EndpointDocs.default

@RestDslMarkerConfig
fun <RESPONSE> ApiRoute.Plain<RESPONSE>.docs(
    block: EndpointDocs.Builder.(ApiRoute.Plain<RESPONSE>) -> Unit,
): ApiRoute.Plain<RESPONSE> {
    return withAttribute(EndpointDocsKey, EndpointDocs.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <PARAMS> ApiRoute.Sse<PARAMS>.docs(
    block: EndpointDocs.Builder.(ApiRoute.Sse<PARAMS>) -> Unit,
): ApiRoute.Sse<PARAMS> {
    return withAttribute(EndpointDocsKey, EndpointDocs.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <PARAMS, RESPONSE> ApiRoute.WithParams<PARAMS, RESPONSE>.docs(
    block: EndpointDocs.Builder.(ApiRoute.WithParams<PARAMS, RESPONSE>) -> Unit,
): ApiRoute.WithParams<PARAMS, RESPONSE> {
    return withAttribute(EndpointDocsKey, EndpointDocs.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <BODY, RESPONSE> ApiRoute.WithBody<BODY, RESPONSE>.docs(
    block: EndpointDocs.Builder.(ApiRoute.WithBody<BODY, RESPONSE>) -> Unit,
): ApiRoute.WithBody<BODY, RESPONSE> {
    return withAttribute(EndpointDocsKey, EndpointDocs.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <PARAMS, BODY, RESPONSE> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.docs(
    block: EndpointDocs.Builder.(ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>) -> Unit,
): ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE> {
    return withAttribute(EndpointDocsKey, EndpointDocs.Builder().also { it.block(this) }.build())
}
