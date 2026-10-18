package io.peekandpoke.ultra.codegen.model

import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * How a Kotlin type appears as a URL parameter.
 *
 * A URL parameter is not JSON. The server converts it to and from TEXT, so its TypeScript type
 * describes what a caller may pass in a path segment or query string — not the JSON shape of the same
 * Kotlin type.
 */
data class TsUrlParamClaim(
    /** Qualified name of the claimed Kotlin class. */
    val qualifiedName: String,
    /** The TypeScript type, e.g. `string`. */
    val tsType: String,
    /**
     * How the value must be formatted, for the caller's TSDoc — e.g. `ISO-8601 instant`.
     *
     * Worth carrying because [tsType] is almost always `string`, which says nothing about what the
     * server can parse back. A caller passing a wrong-format string gets a 400 at run time, and the
     * only place to warn them is here.
     */
    val format: String?,
    /** Name of the contributor that made this claim, for conflict reporting. */
    val claimedBy: String,
)

/**
 * Registry of URL-parameter type claims, keyed by qualified name.
 *
 * **Deliberately separate from [TsTypeClaims], because the two memberships are independent** — not
 * merely different renderings of one set. Measured on `ultra/datetime` (2026-07-30):
 *
 * | Type | JSON claim | URL parameter |
 * |---|---|---|
 * | `MpInstant`, `MpLocalDate` | yes | yes |
 * | `MpLocalDateTime`, `MpZonedDateTime`, `MpLocalTime`, `MpTimezone` | yes | **no** |
 * | `MpAbsoluteDateTime` | **no** | yes |
 *
 * Deriving one from the other would therefore be wrong in both directions. Treating a JSON claim as
 * URL-capable would let `MpTimezone` through as a path parameter and fail at the server, which has no
 * incoming converter to parse it back; and `MpAbsoluteDateTime` would need a JSON claim whose
 * `tsName` and `schema` are meaningless, a lie the moment that type is reached in a body.
 *
 * A second claim for the same type is a hard error naming both contributors — never last-wins, for
 * the same reason as [TsTypeClaims].
 */
class TsUrlParamClaims {

    private val claims = LinkedHashMap<String, TsUrlParamClaim>()

    /** Returns a per-contributor view that stamps [contributor] onto every claim it makes. */
    fun scopeFor(contributor: String): Scope = Scope(contributor)

    /** The claim for [cls], or `null` when unclaimed. */
    fun find(cls: KClass<*>): TsUrlParamClaim? = claims[cls.qualifiedName ?: cls.jvmName]

    /** All claims, in registration order. */
    fun all(): List<TsUrlParamClaim> = claims.values.toList()

    private fun add(claim: TsUrlParamClaim) {
        val existing = claims[claim.qualifiedName]

        check(existing == null) {
            "Type '${claim.qualifiedName}' is claimed twice as a URL parameter: by " +
                    "'${existing!!.claimedBy}' and by '${claim.claimedBy}'. A type may only be claimed " +
                    "once — otherwise the emitted signature depends on contributor order. Fix: " +
                    "register only one of the two contributors."
        }

        claims[claim.qualifiedName] = claim
    }

    /** The API handed to a contributor during the claim phase. */
    inner class Scope internal constructor(private val contributor: String) {

        /** Claims [cls] as a URL parameter of TypeScript type [tsType]. */
        fun map(cls: KClass<*>, tsType: String, format: String? = null) {
            add(
                TsUrlParamClaim(
                    qualifiedName = cls.qualifiedName ?: cls.jvmName,
                    tsType = tsType,
                    format = format,
                    claimedBy = contributor,
                )
            )
        }

        /** Claims [T] as a URL parameter of TypeScript type [tsType]. */
        inline fun <reified T : Any> map(tsType: String, format: String? = null) {
            map(cls = T::class, tsType = tsType, format = format)
        }
    }
}
