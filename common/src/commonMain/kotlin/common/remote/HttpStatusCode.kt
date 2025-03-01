package de.peekandpoke.ktorfx.rest

import kotlinx.serialization.Serializable

@Serializable
data class HttpStatusCode(val value: Int, val description: String) {

    fun isSuccess(): Boolean = value in (200 until 300)

    fun isRedirect(): Boolean = value in (300 until 400)

    fun isError(): Boolean = value >= 400

    @Suppress("unused")
    companion object {
        val Continue: HttpStatusCode = HttpStatusCode(100, "Continue")
        val SwitchingProtocols: HttpStatusCode = HttpStatusCode(101, "Switching Protocols")
        val Processing: HttpStatusCode = HttpStatusCode(102, "Processing")

        val OK: HttpStatusCode = HttpStatusCode(200, "OK")
        val Created: HttpStatusCode = HttpStatusCode(201, "Created")
        val Accepted: HttpStatusCode = HttpStatusCode(202, "Accepted")

        val NonAuthoritativeInformation: HttpStatusCode = HttpStatusCode(203, "Non-Authoritative Information")

        val NoContent: HttpStatusCode = HttpStatusCode(204, "No Content")
        val ResetContent: HttpStatusCode = HttpStatusCode(205, "Reset Content")
        val PartialContent: HttpStatusCode = HttpStatusCode(206, "Partial Content")
        val MultiStatus: HttpStatusCode = HttpStatusCode(207, "Multi-Status")

        val MultipleChoices: HttpStatusCode = HttpStatusCode(300, "Multiple Choices")
        val MovedPermanently: HttpStatusCode = HttpStatusCode(301, "Moved Permanently")
        val Found: HttpStatusCode = HttpStatusCode(302, "Found")
        val SeeOther: HttpStatusCode = HttpStatusCode(303, "See Other")
        val NotModified: HttpStatusCode = HttpStatusCode(304, "Not Modified")
        val UseProxy: HttpStatusCode = HttpStatusCode(305, "Use Proxy")
        val SwitchProxy: HttpStatusCode = HttpStatusCode(306, "Switch Proxy")
        val TemporaryRedirect: HttpStatusCode = HttpStatusCode(307, "Temporary Redirect")
        val PermanentRedirect: HttpStatusCode = HttpStatusCode(308, "Permanent Redirect")

        val BadRequest: HttpStatusCode = HttpStatusCode(400, "Bad Request")
        val Unauthorized: HttpStatusCode = HttpStatusCode(401, "Unauthorized")
        val PaymentRequired: HttpStatusCode = HttpStatusCode(402, "Payment Required")
        val Forbidden: HttpStatusCode = HttpStatusCode(403, "Forbidden")
        val NotFound: HttpStatusCode = HttpStatusCode(404, "Not Found")
        val MethodNotAllowed: HttpStatusCode = HttpStatusCode(405, "Method Not Allowed")
        val NotAcceptable: HttpStatusCode = HttpStatusCode(406, "Not Acceptable")

        val ProxyAuthenticationRequired: HttpStatusCode = HttpStatusCode(407, "Proxy Authentication Required")

        val RequestTimeout: HttpStatusCode = HttpStatusCode(408, "Request Timeout")
        val Conflict: HttpStatusCode = HttpStatusCode(409, "Conflict")
        val Gone: HttpStatusCode = HttpStatusCode(410, "Gone")
        val LengthRequired: HttpStatusCode = HttpStatusCode(411, "Length Required")
        val PreconditionFailed: HttpStatusCode = HttpStatusCode(412, "Precondition Failed")
        val PayloadTooLarge: HttpStatusCode = HttpStatusCode(413, "Payload Too Large")
        val RequestURITooLong: HttpStatusCode = HttpStatusCode(414, "Request-URI Too Long")

        val UnsupportedMediaType: HttpStatusCode = HttpStatusCode(415, "Unsupported Media Type")

        val RequestedRangeNotSatisfiable: HttpStatusCode = HttpStatusCode(416, "Requested Range Not Satisfiable")

        val ExpectationFailed: HttpStatusCode = HttpStatusCode(417, "Expectation Failed")
        val UnprocessableEntity: HttpStatusCode = HttpStatusCode(422, "Unprocessable Entity")
        val Locked: HttpStatusCode = HttpStatusCode(423, "Locked")
        val FailedDependency: HttpStatusCode = HttpStatusCode(424, "Failed Dependency")
        val UpgradeRequired: HttpStatusCode = HttpStatusCode(426, "Upgrade Required")
        val TooManyRequests: HttpStatusCode = HttpStatusCode(429, "Too Many Requests")

        val RequestHeaderFieldTooLarge: HttpStatusCode = HttpStatusCode(431, "Request Header Fields Too Large")

        val InternalServerError: HttpStatusCode = HttpStatusCode(500, "Internal Server Error")
        val NotImplemented: HttpStatusCode = HttpStatusCode(501, "Not Implemented")
        val BadGateway: HttpStatusCode = HttpStatusCode(502, "Bad Gateway")
        val ServiceUnavailable: HttpStatusCode = HttpStatusCode(503, "Service Unavailable")
        val GatewayTimeout: HttpStatusCode = HttpStatusCode(504, "Gateway Timeout")

        val VersionNotSupported: HttpStatusCode = HttpStatusCode(505, "HTTP Version Not Supported")

        val VariantAlsoNegotiates: HttpStatusCode = HttpStatusCode(506, "Variant Also Negotiates")
        val InsufficientStorage: HttpStatusCode = HttpStatusCode(507, "Insufficient Storage")
    }
}
