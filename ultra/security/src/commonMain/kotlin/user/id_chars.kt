package io.peekandpoke.ultra.security.user

/**
 * Characters that no id value class ([UserId], [OrgId], …) may contain: C0 controls, DEL, C1
 * controls, and the Unicode line/paragraph separators.
 *
 * Shared deliberately — these ids are composed into NUL-delimited composite keys (the CSRF signing
 * string in `StatelessCsrfProtection`, the auth session cache key) and written into log lines, so
 * banning the whole set makes a forged key boundary or a forged log line impossible BY CONSTRUCTION
 * rather than by convention. One predicate, so a later id type cannot quietly ship a weaker rule.
 */
internal fun Char.isForbiddenInId(): Boolean =
    code < 0x20 || code == 0x7F || code in 0x80..0x9F || this == '\u2028' || this == '\u2029'
