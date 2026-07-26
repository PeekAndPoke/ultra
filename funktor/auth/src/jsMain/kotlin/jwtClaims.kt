package io.peekandpoke.funktor.auth

import io.peekandpoke.kraft.utils.jsObjectToMap
import kotlinx.browser.window

/**
 * Decodes the CLAIMS of a JWT — its middle segment — into a plain map.
 *
 * ### This is a READ, never a verification
 *
 * No signature is checked, no expiry is enforced, nothing here establishes trust. The claims are a
 * CONVENIENCE MIRROR for the UI: which org is selected, which roles to render, when to refresh. A
 * user can edit their own localStorage and hand this function anything they like, so a decoded claim
 * must only ever decide what the browser *shows* — never what it is *allowed to do*. Every real
 * decision is made server-side, where the token is signature-verified
 * ([io.peekandpoke.ultra.security.jwt.JwtGenerator.verify]) before its claims are read.
 *
 * ### Never throws
 *
 * A malformed, truncated, or non-JWT string yields an empty map — the same result as the old no-op
 * default. A bad token must not be able to break sign-in; the caller then simply has no claims, and
 * `UserId.parseOrNull` / `OrgId.parseOrNull` degrade from there.
 *
 * ### Why not `kraft:addons:jwtdecode`
 *
 * That addon loads the `jwt-decode` npm package through a dynamic `import()` and is therefore
 * asynchronous and reached via an `AddonRegistry`. Reading claims is needed synchronously inside
 * `AuthState.readJwt`, so using it would make that function `suspend` and thread a registry through
 * the auth state — a large change for what is a base64 decode and a `JSON.parse`.
 */
fun decodeJwtClaims(token: String): Map<String, Any?> {
    return try {
        val payload = token.split('.').getOrNull(1)?.takeIf { it.isNotEmpty() }
            ?: return emptyMap()

        val json = base64UrlDecodeToString(payload)
        val parsed = JSON.parse<Any?>(json)

        @Suppress("UnsafeCastFromDynamic")
        jsObjectToMap(parsed)
    } catch (t: Throwable) {
        // Deliberately broad: this runs on attacker-controllable input on the hot sign-in path, and
        // "no claims" is always a safe answer. But it must not be SILENT — an empty claim map is
        // precisely how this whole area failed unnoticed before (every claim-derived feature quietly
        // no-opped), so leave a breadcrumb in the browser console. Not a server log, so there is no
        // log-spam vector.
        console.warn("[AuthState] failed to decode JWT claims", t)
        emptyMap()
    }
}

/**
 * base64url → UTF-8 string.
 *
 * `atob` yields one char per BYTE (latin1), so a multi-byte UTF-8 sequence — an umlaut in a display
 * name, say — would come out mangled. Percent-encoding those bytes and running
 * `decodeURIComponent` re-assembles them correctly.
 */
private fun base64UrlDecodeToString(input: String): String {
    val base64 = input.replace('-', '+').replace('_', '/')
    // base64url drops the padding; atob wants it back.
    val padded = base64.padEnd((base64.length + 3) / 4 * 4, '=')

    val binary = window.atob(padded)

    val percentEncoded = binary.asSequence()
        .joinToString("") { "%" + it.code.toString(16).padStart(2, '0') }

    return js("decodeURIComponent")(percentEncoded) as String
}
