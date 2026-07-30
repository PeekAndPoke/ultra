package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.core.broker.vault.OutgoingMpDateTimeConverter
import io.peekandpoke.funktor.core.broker.vault.OutgoingJavaTimeConverter
import io.peekandpoke.funktor.core.broker.vault.OutgoingPrimitiveConverter
import io.peekandpoke.funktor.core.broker.vault.OutgoingValueClassConverter
import io.peekandpoke.funktor.core.broker.vault.OutgoingVaultConverter
import io.peekandpoke.ultra.datetime.MpAbsoluteDateTime
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Guards [FunktorUrlParamsTsContributor]'s claim list against the converters it mirrors.
 *
 * A claim says "the server can parse this back". Claiming a type funktor has no converter for
 * produces an SDK that compiles, sends a plausible string, and gets a 400 — so the list must not
 * outgrow the registry, and must not fall behind it either.
 *
 * The expectation is derived from the CONVERTERS, never from a second hand-written list: a drift
 * guard that restates its subject guards nothing.
 */
class FunktorUrlParamsParitySpec : FreeSpec() {

    /** Every ultra/datetime type, so the check covers what is NOT claimed as well as what is. */
    private val allDateTimeTypes: Map<KClass<*>, KType> = mapOf(
        MpInstant::class to typeOf<MpInstant>(),
        MpAbsoluteDateTime::class to typeOf<MpAbsoluteDateTime>(),
        MpLocalDate::class to typeOf<MpLocalDate>(),
        MpLocalDateTime::class to typeOf<MpLocalDateTime>(),
        MpZonedDateTime::class to typeOf<MpZonedDateTime>(),
        MpLocalTime::class to typeOf<MpLocalTime>(),
        MpTimezone::class to typeOf<MpTimezone>(),
    )

    init {
        "the claimed datetime types are exactly those the converter can handle" {
            val converter = OutgoingMpDateTimeConverter()

            val handled = allDateTimeTypes.filterValues { converter.canHandle(it) }.keys

            withClue("the enumeration must find something, or this passes by scanning nothing") {
                handled.isNotEmpty() shouldBe true
            }

            // Driven from the claims the contributor ACTUALLY makes, not from its CLAIMED constant:
            // `Stored` and `Storable` are claimed outside that map, so a third claim added the same
            // way would have bypassed this guard entirely (found in review, 2026-07-30).
            val claimedDateTime = claimsOf(FunktorUrlParamsTsContributor())
                .all()
                .mapNotNull { claim -> allDateTimeTypes.keys.firstOrNull { it.qualifiedName == claim.qualifiedName } }

            // Both directions at once. Claiming more than the converter handles ships an SDK that
            // 400s; claiming less silently refuses a parameter the server would have accepted.
            claimedDateTime shouldContainExactlyInAnyOrder handled
        }

        "the types NOT claimed are genuinely unsupported, not merely forgotten" {
            val converter = OutgoingMpDateTimeConverter()

            val unclaimed = allDateTimeTypes.keys - FunktorUrlParamsTsContributor.CLAIMED.keys

            withClue("this spec is worthless if every datetime type happens to be claimed") {
                unclaimed.isNotEmpty() shouldBe true
            }

            unclaimed.forEach { cls ->
                withClue("${cls.simpleName} is unclaimed, so no converter may handle it") {
                    converter.canHandle(allDateTimeTypes.getValue(cls)) shouldBe false
                }
            }
        }

        "Stored is claimed, and the vault converter really handles it" {
            OutgoingVaultConverter().canHandle(typeOf<Stored<String>>()) shouldBe true

            val claims = claimsOf(FunktorUrlParamsTsContributor())

            claims.find(Stored::class)?.tsType shouldBe "string"
        }

        "every claimed type is handled by SOME registered outgoing converter" {
            // The contributor's promise is "the server can parse this back". Checking only the Mp
            // converter left the java.time and BigDecimal claims unguarded.
            val converters = listOf(
                OutgoingMpDateTimeConverter(),
                OutgoingJavaTimeConverter(),
                OutgoingPrimitiveConverter(),
                OutgoingValueClassConverter(),
                OutgoingVaultConverter(),
            )

            val vaultClaims = setOf(Stored::class.qualifiedName, Storable::class.qualifiedName)

            claimsOf(FunktorUrlParamsTsContributor()).all()
                // Vault types are generic; canHandle needs a concrete argument, covered separately.
                .filter { it.qualifiedName !in vaultClaims }
                .forEach { claim ->
                    val cls = Class.forName(claim.qualifiedName).kotlin
                    val type = cls.createType()

                    withClue("${claim.qualifiedName} is claimed but no converter handles it") {
                        converters.any { it.canHandle(type) } shouldBe true
                    }
                }
        }

        "every claim declares a format, because the TypeScript type alone says nothing" {
            val claims = claimsOf(FunktorUrlParamsTsContributor())

            claims.all().forEach { claim ->
                withClue("${claim.qualifiedName} claims '${claim.tsType}' with no format note") {
                    claim.format.isNullOrBlank() shouldBe false
                }
            }
        }
    }
}
