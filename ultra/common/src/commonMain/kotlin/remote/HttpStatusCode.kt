package de.peekandpoke.ultra.common.remote

import kotlinx.serialization.Serializable

@Serializable
data class HttpStatusCode(val value: Int, val description: String) {

    @Suppress("Detekt.TooManyFunctions")
    companion object {
        inline val Continue get() = HttpStatusCode(100, "Continue")
        inline val SwitchingProtocols get() = HttpStatusCode(101, "Switching Protocols")
        inline val Processing get() = HttpStatusCode(102, "Processing")
        inline val EarlyHints get() = HttpStatusCode(103, "Early Hints")

        inline val OK get() = HttpStatusCode(200, "OK")
        inline val Created get() = HttpStatusCode(201, "Created")
        inline val Accepted get() = HttpStatusCode(202, "Accepted")
        inline val NonAuthoritativeInformation get() = HttpStatusCode(203, "Non-Authoritative Information")
        inline val NoContent get() = HttpStatusCode(204, "No Content")
        inline val ResetContent get() = HttpStatusCode(205, "Reset Content")
        inline val PartialContent get() = HttpStatusCode(206, "Partial Content")
        inline val MultiStatus get() = HttpStatusCode(207, "Multi-Status")
        inline val AlreadyReported get() = HttpStatusCode(208, "Already Reported")
        inline val IMUsed get() = HttpStatusCode(226, "IM Used")

        inline val MultipleChoices get() = HttpStatusCode(300, "Multiple Choices")
        inline val MovedPermanently get() = HttpStatusCode(301, "Moved Permanently")
        inline val Found get() = HttpStatusCode(302, "Found")
        inline val SeeOther get() = HttpStatusCode(303, "See Other")
        inline val NotModified get() = HttpStatusCode(304, "Not Modified")
        inline val UseProxy get() = HttpStatusCode(305, "Use Proxy")
        inline val SwitchProxy get() = HttpStatusCode(306, "Unused")
        inline val TemporaryRedirect get() = HttpStatusCode(307, "Temporary Redirect")
        inline val PermanentRedirect get() = HttpStatusCode(308, "Permanent Redirect")

        inline val BadRequest get() = HttpStatusCode(400, "Bad Request")
        inline val Unauthorized get() = HttpStatusCode(401, "Unauthorized")
        inline val PaymentRequired get() = HttpStatusCode(402, "Payment Required")
        inline val Forbidden get() = HttpStatusCode(403, "Forbidden")
        inline val NotFound get() = HttpStatusCode(404, "Not Found")
        inline val MethodNotAllowed get() = HttpStatusCode(405, "Method Not Allowed")
        inline val NotAcceptable get() = HttpStatusCode(406, "Not Acceptable")
        inline val ProxyAuthenticationRequired get() = HttpStatusCode(407, "Proxy Authentication Required")
        inline val RequestTimeout get() = HttpStatusCode(408, "Request Timeout")
        inline val Conflict get() = HttpStatusCode(409, "Conflict")
        inline val Gone get() = HttpStatusCode(410, "Gone")
        inline val LengthRequired get() = HttpStatusCode(411, "Length Required")
        inline val PreconditionFailed get() = HttpStatusCode(412, "Precondition Failed")
        inline val ContentTooLarge get() = HttpStatusCode(413, "Content Too Large")
        inline val URITooLong get() = HttpStatusCode(414, "URI Too Long")
        inline val UnsupportedMediaType get() = HttpStatusCode(415, "Unsupported Media Type")
        inline val RangeNotSatisfiable get() = HttpStatusCode(416, "Requested Range Not Satisfiable")
        inline val ExpectationFailed get() = HttpStatusCode(417, "Expectation Failed")
        inline val ImATeapot get() = HttpStatusCode(418, "I'm a teapot")
        inline val MisdirectedRequest get() = HttpStatusCode(421, "Misdirected Request")
        inline val UnprocessableEntity get() = HttpStatusCode(422, "Unprocessable Entity")
        inline val Locked get() = HttpStatusCode(423, "Locked")
        inline val FailedDependency get() = HttpStatusCode(424, "Failed Dependency")
        inline val TooEarly get() = HttpStatusCode(425, "Failed Dependency")
        inline val UpgradeRequired get() = HttpStatusCode(426, "Upgrade Required")
        inline val PreconditionRequired get() = HttpStatusCode(428, "Precondition Required")
        inline val TooManyRequests get() = HttpStatusCode(429, "Too Many Requests")
        inline val RequestHeaderFieldsTooLarge get() = HttpStatusCode(431, "Request Header Fields Too Large")
        inline val UnavailableForLegalReasons get() = HttpStatusCode(451, "Unavailable For Legal Reasons")

        inline val InternalServerError get() = HttpStatusCode(500, "Internal Server Error")
        inline val NotImplemented get() = HttpStatusCode(501, "Not Implemented")
        inline val BadGateway get() = HttpStatusCode(502, "Bad Gateway")
        inline val ServiceUnavailable get() = HttpStatusCode(503, "Service Unavailable")
        inline val GatewayTimeout get() = HttpStatusCode(504, "Gateway Timeout")
        inline val HttpVersionNotSupported get() = HttpStatusCode(505, "HTTP Version Not Supported")
        inline val VariantAlsoNegotiates get() = HttpStatusCode(506, "Variant Also Negotiates")
        inline val InsufficientStorage get() = HttpStatusCode(507, "Insufficient Storage")
        inline val LoopDetected get() = HttpStatusCode(508, "Loop Detected")
        inline val NotExtended get() = HttpStatusCode(510, "Not Extended")
        inline val NetworkAuthenticationRequired get() = HttpStatusCode(511, "Network Authentication Required")

        private val allStatusCodes
            get() = setOf(
                Continue,
                SwitchingProtocols,
                Processing,
                EarlyHints,
                OK,
                Created,
                Accepted,
                NonAuthoritativeInformation,
                NoContent,
                ResetContent,
                PartialContent,
                MultiStatus,
                AlreadyReported,
                IMUsed,
                MultipleChoices,
                MovedPermanently,
                Found,
                SeeOther,
                NotModified,
                UseProxy,
                SwitchProxy,
                TemporaryRedirect,
                PermanentRedirect,
                BadRequest,
                Unauthorized,
                PaymentRequired,
                Forbidden,
                NotFound,
                MethodNotAllowed,
                NotAcceptable,
                ProxyAuthenticationRequired,
                RequestTimeout,
                Conflict,
                Gone,
                LengthRequired,
                PreconditionFailed,
                ContentTooLarge,
                URITooLong,
                UnsupportedMediaType,
                RangeNotSatisfiable,
                ExpectationFailed,
                ImATeapot,
                MisdirectedRequest,
                UnprocessableEntity,
                Locked,
                FailedDependency,
                TooEarly,
                UpgradeRequired,
                PreconditionRequired,
                TooManyRequests,
                RequestHeaderFieldsTooLarge,
                UnavailableForLegalReasons,
                InternalServerError,
                NotImplemented,
                BadGateway,
                ServiceUnavailable,
                GatewayTimeout,
                HttpVersionNotSupported,
                VariantAlsoNegotiates,
                InsufficientStorage,
                LoopDetected,
                NotExtended,
                NetworkAuthenticationRequired,
            )

        fun of(status: Int, fallback: HttpStatusCode): HttpStatusCode =
            allStatusCodes.firstOrNull { it.value == status } ?: fallback
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpStatusCode) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    fun isInformational(): Boolean = value in (100 until 200)

    fun isSuccess(): Boolean = value in (200 until 300)

    fun isRedirect(): Boolean = value in (300 until 400)

    fun isClientError(): Boolean = value in (400 until 500)

    fun isServerError(): Boolean = value in (500 until 600)
}
