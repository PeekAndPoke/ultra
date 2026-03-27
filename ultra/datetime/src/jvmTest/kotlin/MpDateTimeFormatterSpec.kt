package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MpDateTimeFormatterSpec : StringSpec({

    // =========================================================================================================
    // Format tests for all known consumer patterns (verified against korlibs before removal)
    // =========================================================================================================

    "MpLocalDate.format yyyy-MM-dd" {
        MpLocalDate.of(2022, 4, 11).format("yyyy-MM-dd") shouldBe "2022-04-11"
    }

    "MpLocalDate.format dd MMM yyyy" {
        MpLocalDate.of(2022, 4, 11).format("dd MMM yyyy") shouldBe "11 Apr 2022"
    }

    "MpLocalTime.format HH:mm:ss" {
        MpLocalTime.of(14, 30, 45).format("HH:mm:ss") shouldBe "14:30:45"
    }

    "MpLocalTime.format HH mm ss SSS" {
        MpLocalTime.of(12, 13, 14, 123).format("HH mm ss SSS") shouldBe "12 13 14 123"
    }

    "MpZonedDateTime.format yyyy-MM-dd HH:mm" {
        MpZonedDateTime.parse("2022-04-05T12:00:00Z").format("yyyy-MM-dd HH:mm") shouldBe "2022-04-05 12:00"
    }

    "MpZonedDateTime.format yyyy-MM-dd HH:mm:ss.SSS" {
        MpZonedDateTime.parse("2022-04-05T14:30:45.123Z")
            .format("yyyy-MM-dd HH:mm:ss.SSS") shouldBe "2022-04-05 14:30:45.123"
    }

    "MpZonedDateTime.format with quoted T" {
        MpZonedDateTime.parse("2022-04-05T12:00:00Z").format("yyyy-MM-dd'T'HH:mm:ss") shouldBe "2022-04-05T12:00:00"
    }

    "MpZonedDateTime.format with quoted T and Z" {
        MpZonedDateTime.parse("2022-04-05T00:00:00Z")
            .format("yyyy-MM-dd'T'HH:mm:ss.SSSZ") shouldBe "2022-04-05T00:00:00.000Z"
    }

    // =========================================================================================================
    // Edge cases for the new formatter
    // =========================================================================================================

    "MpLocalDate.format midnight values" {
        val date = MpLocalDate.of(2022, 1, 1)
        date.format("yyyy-MM-dd") shouldBe "2022-01-01"
    }

    "MpLocalDate.format end of year" {
        val date = MpLocalDate.of(2022, 12, 31)
        date.format("yyyy-MM-dd") shouldBe "2022-12-31"
    }

    "MpLocalDate.format single-digit month and day get padded" {
        val date = MpLocalDate.of(2022, 3, 5)
        date.format("yyyy-MM-dd") shouldBe "2022-03-05"
    }

    "MpLocalDate.format abbreviated month names" {
        MpLocalDate.of(2022, 1, 1).format("MMM") shouldBe "Jan"
        MpLocalDate.of(2022, 6, 1).format("MMM") shouldBe "Jun"
        MpLocalDate.of(2022, 12, 1).format("MMM") shouldBe "Dec"
    }

    "MpLocalTime.format midnight" {
        val time = MpLocalTime.of(0, 0, 0, 0)
        time.format("HH:mm:ss.SSS") shouldBe "00:00:00.000"
    }

    "MpLocalTime.format noon" {
        val time = MpLocalTime.of(12, 0, 0, 0)
        time.format("HH:mm:ss") shouldBe "12:00:00"
    }

    "MpLocalTime.format millisecond precision" {
        MpLocalTime.of(0, 0, 0, 0).format("SSS") shouldBe "000"
        MpLocalTime.of(0, 0, 0, 1).format("SSS") shouldBe "001"
        MpLocalTime.of(0, 0, 0, 999).format("SSS") shouldBe "999"
    }

    "formatter handles empty pattern" {
        MpDateTimeFormatter.format("") shouldBe ""
    }

    "formatter handles pattern with only literals" {
        MpDateTimeFormatter.format("hello world!") shouldBe "hello world!"
    }

    "formatter handles quoted literals" {
        MpDateTimeFormatter.format("'T'", year = 2022) shouldBe "T"
        MpDateTimeFormatter.format("yyyy'T'MM") shouldBe "0000T00"
    }

    "formatter handles unclosed quote gracefully" {
        MpDateTimeFormatter.format("yyyy'broken") shouldBe "0000broken"
    }

    "formatter handles unrecognized single tokens as literals" {
        // Single 'y', 'd', 'H', 'm', 's' are passed through
        MpDateTimeFormatter.format("y") shouldBe "y"
        MpDateTimeFormatter.format("d") shouldBe "d"
        MpDateTimeFormatter.format("H") shouldBe "H"
        MpDateTimeFormatter.format("m") shouldBe "m"
        MpDateTimeFormatter.format("s") shouldBe "s"
        MpDateTimeFormatter.format("M") shouldBe "M"
    }

    "MpZonedDateTime.format UTC offset produces Z" {
        val zdt = MpZonedDateTime.parse("2022-04-05T00:00:00Z")
        zdt.format("Z") shouldBe "Z"
    }

    "formatter two-digit year" {
        MpDateTimeFormatter.format("yy", year = 2022) shouldBe "22"
        MpDateTimeFormatter.format("yy", year = 2000) shouldBe "00"
        MpDateTimeFormatter.format("yy", year = 1999) shouldBe "99"
    }
})
