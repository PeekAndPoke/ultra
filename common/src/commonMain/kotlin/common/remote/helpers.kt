package de.peekandpoke.ultra.common.remote

import de.peekandpoke.ultra.common.encodeUriComponent
import de.peekandpoke.ultra.common.model.Paged
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlin.jvm.JvmName

private val uriToParamsCache = mutableMapOf<String, Map<String, UriParamBuilder.Value>>()

@Suppress("RegExpRedundantEscape")
private val placeholderRegex = "\\{([^}]*)\\}".toRegex()

/**
 * Creates a remote request
 */
fun createRequest(
    config: ApiClient.Config,
) = createRequest(
    baseUrl = config.baseUrl,
    requestInterceptors = config.requestInterceptors,
    responseInterceptors = config.responseInterceptors,
    client = config.client,
)

/**
 * Wraps the given serializer with an ApiResponse serializer
 */
fun <T> KSerializer<T>.api(): KSerializer<ApiResponse<T>> =
    ApiResponse.serializer(this)

/**
 * Wraps the given serializer as list and with an ApiResponse serializer
 */
fun <T> KSerializer<T>.apiList(): KSerializer<ApiResponse<List<T>>> =
    ApiResponse.serializer(ListSerializer(this))

/**
 * Wraps the given serializer as [Paged] and with an ApiResponse serializer
 */
fun <T> KSerializer<T>.apiPaged(): KSerializer<ApiResponse<Paged<T>>> =
    ApiResponse.serializer(Paged.serializer(this))

/**
 * Builds an uri from the given [pattern] without any parameters.
 */
fun buildUri(pattern: String): String {
    return buildUri(pattern = pattern, params = emptyMap<String, UriParamBuilder.Value>())
}

/**
 * Builds an uri from the given [pattern] and params added by the [builder].
 */
fun buildUri(pattern: String, builder: UriParamBuilder.() -> Unit): String {
    return buildUri(pattern, UriParamBuilder.uriParams(builder))
}

/**
 * Builds an uri from the given [pattern] and [params].
 */
fun buildUri(pattern: String, vararg params: Pair<String, String?>): String {
    return buildUri(pattern, params.toMap())
}

/**
 * Builds an uri from the given [pattern] and [params].
 */
@JvmName("buildUri_map")
fun buildUri(pattern: String, params: Map<String, String?> = emptyMap()): String {

    return buildUri(
        pattern = pattern,
        params = UriParamBuilder.of(params)
    )
}

/**
 * Builds an uri from the given [pattern] and [params].
 *
 * It tries to replace all parameters names with value by replacing
 * {PARAM_NAME} with its string value.
 *
 * All parameters that are not found in the uri [pattern] will be appended
 * as query parameters.
 *
 * Parameters that have a null value or a blank string will NOT be included
 */
@JvmName("buildUri_values")
fun buildUri(pattern: String, params: Map<String, UriParamBuilder.Value> = emptyMap()): String {

    val urlParams = uriToParamsCache.getOrPut(pattern) {
        placeholderRegex
            .findAll(pattern)
            .map { it.groupValues[1] }
            .associateWith { UriParamBuilder.StrValue("") }
    }

    val cleaned = urlParams.plus(
        params
            .filter { (_, v) -> !v.encoded.isNullOrBlank() }
    )

    if (cleaned.isEmpty()) {
        return pattern
    }

    // We replace all param placeholders in the uri
    // And we collect all the params that are not part of the uri
    val paramsNotInUri = mutableMapOf<String, UriParamBuilder.Value>()

    val uriReplaced = cleaned.entries.fold(pattern) { acc, (k, v) ->
        val part = "{$k}"
        if (acc.contains(part)) {
            when (val value = v.encoded) {
                null -> acc
                else -> acc.replace(part, value)
            }
        } else {
            paramsNotInUri[k] = v
            acc
        }
    }

    return when (paramsNotInUri.isEmpty()) {
        true -> uriReplaced

        else -> "$uriReplaced?" + paramsNotInUri
            .map { (k, v) -> k.encodeUriComponent() + "=" + (v.encoded ?: "") }
            .joinToString("&")
    }
}
