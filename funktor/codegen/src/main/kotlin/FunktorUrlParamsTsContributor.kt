package io.peekandpoke.funktor.codegen

import io.peekandpoke.ultra.codegen.model.TsUrlParamClaims
import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import io.peekandpoke.ultra.datetime.MpAbsoluteDateTime
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.vault.Storable
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import io.peekandpoke.ultra.vault.Stored
import kotlin.reflect.KClass

/**
 * Claims the URL-parameter types funktor's own converters can round-trip.
 *
 * **This list mirrors funktor's converter registry, and must not outgrow it.** A claim here says "the
 * server can parse this back", so claiming a type with no `IncomingParamConverter` produces an SDK
 * that compiles, sends a plausible string, and gets a 400. Each entry below cites the converter that
 * makes it true.
 *
 * `MpDateTimeFieldParitySpec`'s sibling — `FunktorUrlParamsParitySpec` — fails when the two drift.
 */
class FunktorUrlParamsTsContributor : TsSdkContributor {

    companion object {
        const val NAME: String = "funktor:url-params"

        /**
         * The claimed types, paired with their TypeScript type and wire format.
         *
         * Exposed so the parity test can compare it against the converters rather than against a
         * second hand-written list — a drift guard that restates its subject guards nothing.
         */
        val CLAIMED: Map<KClass<*>, Pair<String, String>> = mapOf(
            // OutgoingMpDateTimeConverter writes toIsoString(); IncomingMpDateTimeConverter reads it
            // back with MpInstant.parse (funktor/core/src/jvmMain/kotlin/broker/vault/mpdatetime.kt).
            MpInstant::class to ("string" to "ISO-8601 instant, e.g. 2026-07-30T10:15:30Z"),
            MpAbsoluteDateTime::class to ("string" to "ISO-8601 instant, e.g. 2026-07-30T10:15:30Z"),
            // Same file: formatted and parsed as yyyy-MM-dd.
            MpLocalDate::class to ("string" to "ISO-8601 date, yyyy-MM-dd"),

            // java.time, via Incoming/OutgoingJavaTimeConverter (broker/vault/javatime.kt:19-26).
            // Without these a perfectly legal route — `val from: LocalDate = LocalDate.MIN` — aborts
            // the WHOLE run with "change the parameter type", for a route that is not broken.
            ZoneId::class to ("string" to "IANA zone id, e.g. Europe/Berlin"),
            LocalTime::class to ("string" to "ISO-8601 time, HH:mm[:ss]"),
            Instant::class to ("string" to "ISO-8601 instant, e.g. 2026-07-30T10:15:30Z"),
            LocalDate::class to ("string" to "ISO-8601 date, yyyy-MM-dd"),
            LocalDateTime::class to ("string" to "ISO-8601 local date-time, yyyy-MM-ddTHH:mm[:ss]"),
            ZonedDateTime::class to ("string" to "ISO-8601 zoned date-time"),

            // BigDecimal / BigInteger, via Incoming/OutgoingPrimitiveConverter (primitive.kt:19-23).
            // `string`, NOT `number`: a JS number cannot hold them, which is the reason they exist.
            BigDecimal::class to ("string" to "a decimal number as text, e.g. 12.34"),
            BigInteger::class to ("string" to "an integer as text, of any magnitude"),
        )
    }

    override val name: String = NAME

    override fun claimUrlParams(claims: TsUrlParamClaims.Scope) {
        CLAIMED.forEach { (cls, spec) ->
            claims.map(cls = cls, tsType = spec.first, format = spec.second)
        }

        // Vault entity binding: OutgoingVaultConverter writes the entity's id and
        // IncomingVaultConverter resolves it through the repository
        // (funktor/core/src/jvmMain/kotlin/broker/vault/vault.kt:50). Claimed by CLASS, so one claim
        // covers every `Stored<Whatever>`.
        //
        // NOTE the asymmetry with the datetime entries: the incoming side 404s when no repository
        // stores the entity, so a claim here promises the SHAPE is right, not that the id resolves.
        claims.map(cls = Stored::class, tsType = "string", format = "the entity's _key or _id")
        claims.map(cls = Storable::class, tsType = "string", format = "the entity's _key or _id")
    }
}
