package de.peekandpoke.ultra.common.remote

import de.peekandpoke.ultra.common.model.Message
import kotlinx.serialization.Serializable

/**
 * Standard format for an api response
 */
@Serializable
data class ApiResponse<out T>(
    /** The status code */
    val status: HttpStatusCode,
    /** The response data */
    val data: T?,
    /** Messages to be sent along */
    val messages: List<Message>? = null,
    /** Insights */
    val insights: Insights? = null,
) {
    @Serializable
    data class Insights(
        val ts: Long,
        val method: String,
        val url: String,
        val server: String,
        val status: HttpStatusCode,
        val durationMs: Double?,
        val detailsUri: String?,
    )

    @Suppress("NOTHING_TO_INLINE", "Detekt.TooManyFunctions")
    companion object {

        // 1XX /////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * [100] Creates an [ApiResponse] with [HttpStatusCode.Continue]
         */
        inline fun <T> continueResponse(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Continue, data)

        /**
         * [101] Creates an [ApiResponse] with [HttpStatusCode.SwitchingProtocols]
         */
        inline fun <T> switchingProtocols(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.SwitchingProtocols, data)

        /**
         * [102] Creates an [ApiResponse] with [HttpStatusCode.Processing]
         */
        inline fun <T> processing(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Processing, data)

        /**
         * [103] Creates an [ApiResponse] with [HttpStatusCode.EarlyHints]
         */
        inline fun <T> earlyHints(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.EarlyHints, data)

        // 2XX /////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * [200] Creates an [ApiResponse] with [HttpStatusCode.OK]
         */
        inline fun <T> ok(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.OK, data)

        /**
         * [200 | 404] Creates an [ApiResponse] with [HttpStatusCode.OK] or [HttpStatusCode.NotFound] when data is null
         */
        inline fun <T> okOrNotFound(data: T?): ApiResponse<T> = when (data) {
            null -> notFound()
            else -> ok(data)
        }

        /**
         * [201] Creates an [ApiResponse] with [HttpStatusCode.Created]
         */
        inline fun <T> created(data: T): ApiResponse<T> = ApiResponse(HttpStatusCode.Created, data)

        /**
         * [200 | 201] Creates an [ApiResponse] with [HttpStatusCode.OK] or [HttpStatusCode.Created]
         *
         * Code depending on [created].
         */
        inline fun <T> okOrCreated(created: Boolean, data: T) = when (created) {
            true -> created(data)
            false -> ok(data)
        }

        /**
         * [202] Creates an [ApiResponse] with [HttpStatusCode.Accepted]
         */
        inline fun <T> accepted(data: T): ApiResponse<T> = ApiResponse(HttpStatusCode.Accepted, data)

        /**
         * [203] Creates an [ApiResponse] with [HttpStatusCode.NonAuthoritativeInformation]
         */
        inline fun <T> nonAuthoritativeInformation(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.NonAuthoritativeInformation, data)

        /**
         * [204] Creates an [ApiResponse] with [HttpStatusCode.NoContent]
         */
        inline fun <T> noContent(): ApiResponse<T> = ApiResponse(HttpStatusCode.NoContent, null)

        /**
         * [205] Creates an [ApiResponse] with [HttpStatusCode.ResetContent]
         */
        inline fun <T> resetContent(): ApiResponse<T> = ApiResponse(HttpStatusCode.ResetContent, null)

        /**
         * [206] Creates an [ApiResponse] with [HttpStatusCode.PartialContent]
         */
        inline fun <T> partialContent(data: T): ApiResponse<T> = ApiResponse(HttpStatusCode.PartialContent, data)

        /**
         * [207] Creates an [ApiResponse] with [HttpStatusCode.MultiStatus]
         */
        inline fun <T> multiStatus(data: T): ApiResponse<T> = ApiResponse(HttpStatusCode.MultiStatus, data)

        /**
         * [208] Creates an [ApiResponse] with [HttpStatusCode.AlreadyReported]
         */
        inline fun <T> alreadyReported(data: T): ApiResponse<T> = ApiResponse(HttpStatusCode.AlreadyReported, data)

        /**
         * [226] Creates an [ApiResponse] with [HttpStatusCode.IMUsed]
         */
        inline fun <T> imUsed(data: T): ApiResponse<T> = ApiResponse(HttpStatusCode.IMUsed, data)

        // 3XX /////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * [300] Creates an [ApiResponse] with [HttpStatusCode.MultipleChoices]
         */
        inline fun <T> multipleChoices(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.MultipleChoices, data)

        /**
         * [301] Creates an [ApiResponse] with [HttpStatusCode.MovedPermanently]
         */
        inline fun <T> movedPermanently(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.MovedPermanently, data)

        /**
         * [302] Creates an [ApiResponse] with [HttpStatusCode.Found]
         */
        inline fun <T> found(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Found, data)

        /**
         * [303] Creates an [ApiResponse] with [HttpStatusCode.SeeOther]
         */
        inline fun <T> seeOther(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.SeeOther, data)

        /**
         * [304] Creates an [ApiResponse] with [HttpStatusCode.NotModified]
         */
        inline fun <T> notModified(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.NotModified, data)

        /**
         * [305] Creates an [ApiResponse] with [HttpStatusCode.UseProxy]
         */
        inline fun <T> useProxy(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.UseProxy, data)

        /**
         * [305] Creates an [ApiResponse] with [HttpStatusCode.UseProxy]
         */
        inline fun <T> switchProxy(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.SwitchProxy, data)

        /**
         * [307] Creates an [ApiResponse] with [HttpStatusCode.TemporaryRedirect]
         */
        inline fun <T> temporaryRedirect(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.TemporaryRedirect, data)

        /**
         * [308] Creates an [ApiResponse] with [HttpStatusCode.PermanentRedirect]
         */
        inline fun <T> permanentRedirect(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.PermanentRedirect, data)

        // 4XX /////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * [400] Creates an [ApiResponse] with [HttpStatusCode.BadRequest]
         */
        inline fun <T> badRequest(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.BadRequest, data)

        /**
         * [401] Creates an [ApiResponse] with [HttpStatusCode.Unauthorized]
         */
        inline fun <T> unauthorized(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Unauthorized, data)

        /**
         * [402] Creates an [ApiResponse] with [HttpStatusCode.PaymentRequired]
         */
        inline fun <T> paymentRequired(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.PaymentRequired, data)

        /**
         * [403] Creates an [ApiResponse] with [HttpStatusCode.Forbidden]
         */
        inline fun <T> forbidden(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Forbidden, data)

        /**
         * [404] Creates an [ApiResponse] with [HttpStatusCode.NotFound]
         */
        inline fun <T> notFound(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.NotFound, data)

        /**
         * [405] Creates an [ApiResponse] with [HttpStatusCode.MethodNotAllowed]
         */
        inline fun <T> methodNotAllowed(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.MethodNotAllowed, data)

        /**
         * [406] Creates an [ApiResponse] with [HttpStatusCode.NotAcceptable]
         */
        inline fun <T> notAcceptable(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.NotAcceptable, data)

        /**
         * [407] Creates an [ApiResponse] with [HttpStatusCode.ProxyAuthenticationRequired]
         */
        inline fun <T> proxyAuthenticationRequired(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.ProxyAuthenticationRequired, data)

        /**
         * [408] Creates an [ApiResponse] with [HttpStatusCode.RequestTimeout]
         */
        inline fun <T> requestTimeout(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.RequestTimeout, data)

        /**
         * [409] Creates an [ApiResponse] with [HttpStatusCode.Conflict]
         */
        inline fun <T> conflict(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Conflict, data)

        /**
         * [410] Creates an [ApiResponse] with [HttpStatusCode.Gone]
         */
        inline fun <T> gone(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Gone, data)

        /**
         * [411] Creates an [ApiResponse] with [HttpStatusCode.LengthRequired]
         */
        inline fun <T> lengthRequired(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.LengthRequired, data)

        /**
         * [412] Creates an [ApiResponse] with [HttpStatusCode.PreconditionFailed]
         */
        inline fun <T> preconditionFailed(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.PreconditionFailed, data)

        /**
         * [413] Creates an [ApiResponse] with [HttpStatusCode.ContentTooLarge]
         */
        inline fun <T> contentTooLarge(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.ContentTooLarge, data)

        /**
         * [414] Creates an [ApiResponse] with [HttpStatusCode.URITooLong]
         */
        inline fun <T> uriTooLong(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.URITooLong, data)

        /**
         * [415] Creates an [ApiResponse] with [HttpStatusCode.UnsupportedMediaType]
         */
        inline fun <T> unsupportedMediaType(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.UnsupportedMediaType, data)

        /**
         * [416] Creates an [ApiResponse] with [HttpStatusCode.RangeNotSatisfiable]
         */
        inline fun <T> rangeNotSatisfiable(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.RangeNotSatisfiable, data)

        /**
         * [417] Creates an [ApiResponse] with [HttpStatusCode.ExpectationFailed]
         */
        inline fun <T> expectationFailed(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.ExpectationFailed, data)

        /**
         * [418] Creates an [ApiResponse] with [HttpStatusCode.ImATeapot]
         */
        inline fun <T> imATeapot(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.ImATeapot, data)

        /**
         * [421] Creates an [ApiResponse] with [HttpStatusCode.MisdirectedRequest]
         */
        inline fun <T> misdirectedRequest(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.MisdirectedRequest, data)

        /**
         * [422] Creates an [ApiResponse] with [HttpStatusCode.UnprocessableEntity]
         */
        inline fun <T> unprocessableEntity(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.UnprocessableEntity, data)

        /**
         * [423] Creates an [ApiResponse] with [HttpStatusCode.Locked]
         */
        inline fun <T> locked(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Locked, data)

        /**
         * [424] Creates an [ApiResponse] with [HttpStatusCode.FailedDependency]
         */
        inline fun <T> failedDependency(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.FailedDependency, data)

        /**
         * [425] Creates an [ApiResponse] with [HttpStatusCode.TooEarly]
         */
        inline fun <T> tooEarly(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.TooEarly, data)

        /**
         * [426] Creates an [ApiResponse] with [HttpStatusCode.UpgradeRequired]
         */
        inline fun <T> upgradeRequired(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.UpgradeRequired, data)

        /**
         * [428] Creates an [ApiResponse] with [HttpStatusCode.PreconditionRequired]
         */
        inline fun <T> preconditionRequired(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.PreconditionRequired, data)

        /**
         * [429] Creates an [ApiResponse] with [HttpStatusCode.TooManyRequests]
         */
        inline fun <T> tooManyRequests(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.TooManyRequests, data)

        /**
         * [431] Creates an [ApiResponse] with [HttpStatusCode.RequestHeaderFieldsTooLarge]
         */
        inline fun <T> requestHeaderFieldsTooLarge(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.RequestHeaderFieldsTooLarge, data)

        /**
         * [451] Creates an [ApiResponse] with [HttpStatusCode.UnavailableForLegalReasons]
         */
        inline fun <T> unavailableForLegalReasons(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.UnavailableForLegalReasons, data)

        // 5XX /////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * [500] Creates an [ApiResponse] with [HttpStatusCode.InternalServerError]
         */
        inline fun <T> internalServerError(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.InternalServerError, data)

        /**
         * [500] Creates an [ApiResponse] with [HttpStatusCode.InternalServerError]
         */
        inline fun <T> notImplemented(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.NotImplemented, data)

        /**
         * [502] Creates an [ApiResponse] with [HttpStatusCode.BadGateway]
         */
        inline fun <T> badGateway(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.BadGateway, data)

        /**
         * [503] Creates an [ApiResponse] with [HttpStatusCode.ServiceUnavailable]
         */
        inline fun <T> serviceUnavailable(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.ServiceUnavailable, data)

        /**
         * [504] Creates an [ApiResponse] with [HttpStatusCode.GatewayTimeout]
         */
        inline fun <T> gatewayTimeout(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.GatewayTimeout, data)

        /**
         * [505] Creates an [ApiResponse] with [HttpStatusCode.HttpVersionNotSupported]
         */
        inline fun <T> httpVersionNotSupported(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.HttpVersionNotSupported, data)

        /**
         * [506] Creates an [ApiResponse] with [HttpStatusCode.VariantAlsoNegotiates]
         */
        inline fun <T> variantAlsoNegotiates(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.VariantAlsoNegotiates, data)

        /**
         * [507] Creates an [ApiResponse] with [HttpStatusCode.InsufficientStorage]
         */
        inline fun <T> insufficientStorage(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.InsufficientStorage, data)

        /**
         * [508] Creates an [ApiResponse] with [HttpStatusCode.LoopDetected]
         */
        inline fun <T> loopDetected(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.LoopDetected, data)

        /**
         * [510] Creates an [ApiResponse] with [HttpStatusCode.NotExtended]
         */
        inline fun <T> notExtended(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.NotExtended, data)

        /**
         * [511] Creates an [ApiResponse] with [HttpStatusCode.NetworkAuthenticationRequired]
         */
        inline fun <T> networkAuthenticationRequired(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.NetworkAuthenticationRequired, data)
    }

    /** Returns a new [ApiResponse] with the given [insights] set */
    fun withInsights(insights: Insights) = copy(insights = insights)

    /** Returns a new [ApiResponse] while adding messages of type "info" */
    fun withInfo(vararg text: String): ApiResponse<T> = withMessages(Message.Type.info, text)

    /** Returns a new [ApiResponse] while adding messages of type "warning" */
    fun withWarning(vararg text: String): ApiResponse<T> = withMessages(Message.Type.warning, text)

    /** Returns a new [ApiResponse] while adding messages of type "error" */
    fun withError(vararg text: String): ApiResponse<T> = withMessages(Message.Type.error, text)

    /**
     * Returns 'true' when the request was successful, meaning 200 <= status code <= 299
     */
    fun isSuccess(): Boolean {
        return status.isSuccess()
    }

    /**
     * Returns 'true' when the request was not successful,
     */
    fun isNotSuccess(): Boolean {
        return !isSuccess()
    }

    /**
     * Creates a new ApiResponse, while mapping the [data].
     */
    fun <R> map(mapper: (T?) -> R): ApiResponse<R> {
        return ApiResponse(
            status = status,
            data = mapper(data),
            messages = messages,
            insights = insights,
        )
    }

    /**
     * Internal helper for adding messages
     */
    private fun withMessages(type: Message.Type, texts: Array<out String>): ApiResponse<T> = copy(
        messages = (messages ?: emptyList()).plus(
            texts.map { Message(type, it) }
        )
    )
}
