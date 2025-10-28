package de.peekandpoke.funktor.rest.docs

import de.peekandpoke.funktor.rest.ApiRoute
import de.peekandpoke.funktor.rest.RestDslMarkerConfig
import de.peekandpoke.ultra.common.TypedKey

data class CodeGenHints(
    val funcName: String?,
    val tags: List<String>,
) {
    companion object {
        val empty = CodeGenHints(
            funcName = null,
            tags = emptyList(),
        )
    }

    class Builder {
        var funcName: String? = null

        private val tags = mutableListOf<String>()

        internal fun build() = CodeGenHints(
            funcName = funcName,
            tags = tags.toList()
        )

        fun tag(tag: String) {
            tags.add(tag)
        }
    }
}

val CodeGenHintsKey = TypedKey<CodeGenHints>("CodeGenHints")

val ApiRoute<*>.codeGen get() = attributes[CodeGenHintsKey] ?: CodeGenHints.empty

@RestDslMarkerConfig
fun <RESPONSE> ApiRoute.Plain<RESPONSE>.codeGen(
    block: CodeGenHints.Builder.(ApiRoute.Plain<RESPONSE>) -> Unit,
): ApiRoute.Plain<RESPONSE> {
    return withAttribute(CodeGenHintsKey, CodeGenHints.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <PARAMS> ApiRoute.Sse<PARAMS>.codeGen(
    block: CodeGenHints.Builder.(ApiRoute.Sse<PARAMS>) -> Unit,
): ApiRoute.Sse<PARAMS> {
    return withAttribute(CodeGenHintsKey, CodeGenHints.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <PARAMS, RESPONSE> ApiRoute.WithParams<PARAMS, RESPONSE>.codeGen(
    block: CodeGenHints.Builder.(ApiRoute.WithParams<PARAMS, RESPONSE>) -> Unit,
): ApiRoute.WithParams<PARAMS, RESPONSE> {
    return withAttribute(CodeGenHintsKey, CodeGenHints.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <BODY, RESPONSE> ApiRoute.WithBody<BODY, RESPONSE>.codeGen(
    block: CodeGenHints.Builder.(ApiRoute.WithBody<BODY, RESPONSE>) -> Unit,
): ApiRoute.WithBody<BODY, RESPONSE> {
    return withAttribute(CodeGenHintsKey, CodeGenHints.Builder().also { it.block(this) }.build())
}

@RestDslMarkerConfig
fun <PARAMS, BODY, RESPONSE> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.codeGen(
    block: CodeGenHints.Builder.(ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>) -> Unit,
): ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE> {
    return withAttribute(CodeGenHintsKey, CodeGenHints.Builder().also { it.block(this) }.build())
}
