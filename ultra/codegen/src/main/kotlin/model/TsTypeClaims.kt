package io.peekandpoke.ultra.codegen.model

import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * A contributor's declaration of how a Kotlin type appears in TypeScript.
 *
 * Claims exist for types whose JSON shape cannot be derived from the type graph — anything handled
 * by a custom Slumber codec. `MpInstant` is the canonical case: it slumbers to
 * `{ts, timezone, human}`, which nothing in its `KType` reveals.
 */
data class TsTypeClaim(
    /** Qualified name of the claimed Kotlin class. */
    val qualifiedName: String,
    /** The TypeScript type name to reference. */
    val tsName: String,
    /** Module to import [tsName] from, or `null` when it needs no import. */
    val importFrom: String?,
    /** Zod schema expression for this type, or `null` for [opaque] claims. */
    val schema: String?,
    /** True when the type is deliberately emitted as `unknown`. */
    val opaque: Boolean,
    /** Why the type is opaque — surfaced in the run summary. */
    val reason: String,
    /** Name of the contributor that made this claim, for conflict reporting. */
    val claimedBy: String,
)

/**
 * Registry of type claims, keyed by qualified name.
 *
 * A second claim for the same type is a hard error naming both contributors — never last-wins.
 * With kontainer auto-discovery, adding a module could otherwise silently shadow a mapping and
 * change the emitted types without any diff in the generator itself.
 *
 * To replace a built-in claim, do not register the contributor that makes it (contributors are
 * opt-in kontainer singletons) and register your own instead.
 */
class TsTypeClaims {

    private val claims = LinkedHashMap<String, TsTypeClaim>()

    /** Returns a per-contributor view that stamps [contributor] onto every claim it makes. */
    fun scopeFor(contributor: String): Scope = Scope(contributor)

    /** The claim for [cls], or `null` when unclaimed. */
    fun find(cls: KClass<*>): TsTypeClaim? = claims[cls.qualifiedName ?: cls.jvmName]

    /** All claims, in registration order. */
    fun all(): List<TsTypeClaim> = claims.values.toList()

    /** Adds [claim], failing when the type is already claimed. */
    private fun add(claim: TsTypeClaim) {
        val existing = claims[claim.qualifiedName]

        check(existing == null) {
            "Type '${claim.qualifiedName}' is claimed twice: by '${existing!!.claimedBy}' and by " +
                    "'${claim.claimedBy}'. A type may only be claimed once — otherwise the emitted " +
                    "TypeScript depends on contributor order. Fix: register only one of the two " +
                    "contributors, or have one of them stop claiming this type."
        }

        claims[claim.qualifiedName] = claim
    }

    /** The API handed to a contributor during the claim phase. */
    inner class Scope internal constructor(private val contributor: String) {

        /** Claims [cls], mapping it to the TypeScript type [tsName]. */
        fun map(cls: KClass<*>, tsName: String, importFrom: String? = null, schema: String? = null) {
            add(
                TsTypeClaim(
                    qualifiedName = cls.qualifiedName ?: cls.jvmName,
                    tsName = tsName,
                    importFrom = importFrom,
                    schema = schema,
                    opaque = false,
                    reason = "",
                    claimedBy = contributor,
                )
            )
        }

        /** Claims [T], mapping it to the TypeScript type [tsName]. */
        inline fun <reified T : Any> map(tsName: String, importFrom: String? = null, schema: String? = null) {
            map(cls = T::class, tsName = tsName, importFrom = importFrom, schema = schema)
        }

        /** Claims [cls] as deliberately opaque — emitted as `unknown` and listed in the run summary. */
        fun opaque(cls: KClass<*>, reason: String) {
            add(
                TsTypeClaim(
                    qualifiedName = cls.qualifiedName ?: cls.jvmName,
                    tsName = "unknown",
                    importFrom = null,
                    schema = "z.unknown()",
                    opaque = true,
                    reason = reason,
                    claimedBy = contributor,
                )
            )
        }

        /** Claims [T] as deliberately opaque — emitted as `unknown` and listed in the run summary. */
        inline fun <reified T : Any> opaque(reason: String) {
            opaque(cls = T::class, reason = reason)
        }
    }
}
