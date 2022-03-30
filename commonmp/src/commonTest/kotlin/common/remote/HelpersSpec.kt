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

        withClue("Route param but no value given") {
            buildUri("/uri/{param}") shouldBe "/uri/{param}"
        }

        withClue("Route param and value given") {
            buildUri("/uri/{param}", "param" to "XYZ") shouldBe "/uri/XYZ"
        }

        withClue("Route param but another value given") {
            buildUri("/uri/{param}", "x" to "XYZ") shouldBe "/uri/{param}?x=XYZ"
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
    }
})
