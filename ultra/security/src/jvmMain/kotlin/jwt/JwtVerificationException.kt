package io.peekandpoke.ultra.security.jwt

/**
 * A token failed verification — structure, signature, or claim validation.
 *
 * The one exception type for every rejection on the verify path: [JwtSignatureGate.Rejected] for MAC
 * failures, plain instances for the rest. [JwtGenerator.tryVerify] catches exactly this type, so a
 * rejection of any kind degrades to null — an anonymous request — never a 500.
 *
 * Replaces `com.auth0.jwt.exceptions.JWTVerificationException`; with it gone, no vendor type appears
 * anywhere in this module's API.
 */
open class JwtVerificationException(message: String) : RuntimeException(message)
