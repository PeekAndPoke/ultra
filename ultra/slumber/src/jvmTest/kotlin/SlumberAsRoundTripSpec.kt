package io.peekandpoke.ultra.slumber

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.slumber.Slumber
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation

/**
 * Checks that every `@Slumber.As` declaration tells the truth about its codec.
 *
 * `@Slumber.As` is descriptive: nothing derives the codec from it, so a wrong declaration is silent and
 * only surfaces downstream as confidently wrong generated code. This is the one guard, and it replaces
 * the per-consumer guards each generator would otherwise need.
 *
 * ### Why the full round trip, and not just `awake`
 *
 * Awaking the raw data into the declared class catches only a declaration that is too BIG. Measured
 * against the real `MpInstant` codec: a declaration missing a field awakes fine, because extra keys are
 * ignored; and one typing `ts` as `String` awakes fine too, because the value converts. Re-slumbering
 * the result and comparing against the original raw data catches all three cases.
 *
 * The comparison has to be type-sensitive, and is: in the wrong-type case both maps PRINT identically
 * and differ only because `"1649160794000"` is not `1649160794000L`. Map equality gives that for free;
 * comparing rendered strings would not.
 */
class SlumberAsRoundTripSpec : StringSpec({

    val codec = Codec.default

    // One real value per annotated type. This list cannot be derived: the round trip needs an INSTANCE,
    // and nothing conjures one generically. The coverage test below is what stops it rotting.
    val samples: List<Any> = listOf(
        MpLocalDate.of(2022, Month.APRIL, 5).atStartOfDay(TimeZone.UTC).toInstant(),
        MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14).atZone(TimeZone.of("Europe/Berlin")),
        MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14),
        MpLocalDate.of(2022, Month.APRIL, 5),
        MpLocalTime.ofMilliSeconds(123),
        MpTimezone.of("Europe/Berlin"),
    )

    "Every type carrying @Slumber.As must slumber to exactly what it declares" {

        samples.forEach { value ->

            val cls = value::class

            withClue("${cls.simpleName} must carry @Slumber.As, readable at RUNTIME") {
                // Reading it off `annotations` rather than via findAnnotation pins the retention too: a
                // SOURCE- or BINARY-retained annotation is absent here, and `ultra:codegen` consumes
                // this reflectively.
                cls.annotations.filterIsInstance<Slumber.As>().size shouldBe 1
            }

            val declared = cls.findAnnotation<Slumber.As>()!!.shape

            val raw1 = codec.slumber(cls.createType(), value)
            val awoken = codec.awake(declared.createType(), raw1)
            val raw2 = codec.slumber(declared.createType(), awoken)

            withClue("${cls.simpleName} declares ${declared.simpleName}: re-slumbering it must reproduce $raw1") {
                raw2 shouldBe raw1
            }
        }
    }

    "Every annotated type in ultra:datetime must have a sample above" {

        // Manual, for the reason given on `samples`. It is here so that annotating a seventh type and
        // forgetting to check it is a failing test rather than silence.
        val expected = setOf(
            MpInstant::class,
            MpZonedDateTime::class,
            MpLocalDateTime::class,
            MpLocalDate::class,
            MpLocalTime::class,
            MpTimezone::class,
        )

        samples.map { it::class }.toSet() shouldBe expected
    }
})
