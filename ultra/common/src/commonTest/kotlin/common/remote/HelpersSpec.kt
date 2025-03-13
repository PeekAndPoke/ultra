package de.peekandpoke.ultra.common.remote

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class HelpersSpec : StringSpec({

    "buildUri()" {

        withClue("Simple") {
            buildUri("/uri") shouldBe "/uri"
        }

        withClue("Route param and value given") {
            buildUri("/uri/{param}", "param" to "XYZ") shouldBe "/uri/XYZ"
        }

        withClue("Route param but another value given") {
            buildUri("/uri/{param}", "x" to "XYZ") shouldBe "/uri/?x=XYZ"
        }

        withClue("Route param is given but it is an empty string") {
            buildUri("/uri/{param}", "param" to "") shouldBe "/uri/"
        }

        withClue("Route param is empty and another one is not") {
            buildUri("/uri/{first}/and/{second}/end", "second" to "2") shouldBe "/uri//and/2/end"
        }

        withClue("Additional param is given as an empty string") {
            buildUri("/uri/{param}", "param" to "p", "add" to "") shouldBe "/uri/p"
        }

        withClue("Multiple route params and query params") {
            buildUri(
                "/uri/{p1}/and/{p2}/end",
                "p1" to "_1_",
                "p2" to "_2_",
                "p3" to "_3_",
                "p4" to "_4_",
            ) shouldBe "/uri/_1_/and/_2_/end?p3=_3_&p4=_4_"
        }

        withClue("Parameter Encoding") {
            buildUri(
                "/uri/{p1}",
                "p1" to "_&_",
                "p2" to "_/_",
            ) shouldBe "/uri/_%26_?p2=_%2F_"
        }

        withClue("With builder - number, string and raw parameters") {
            buildUri("/uri/{int}/{str}/{raw}") {
                set("int", 100)
                set("str", "abc")
                setRaw("raw", "2024-11-01T12:00:00.00Z")
                set("extra1", "e1")
                set("extra2", "e2")
            } shouldBe "/uri/100/abc/2024-11-01T12:00:00.00Z?extra1=e1&extra2=e2"
        }
    }
})
