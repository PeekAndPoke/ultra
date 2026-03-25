package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File

class ClassesSpec : StringSpec({

    // String.getRelativePackagePath ///////////////////////////////////////////////////////////////////////////////////

    println(
        File("/opt/dev", File("").path)
    )

    listOf(
        tuple("", "", File("")),
        tuple("de", "", File("..")),
        tuple("de", "de", File("")),
        tuple("", "de", File("de")),
        tuple("io.peekandpoke", "", File("../..")),
        tuple("io.peekandpoke", "io", File("..")),
        tuple("io.peekandpoke", "io.peekandpoke", File("")),
        tuple("io", "io.peekandpoke", File("peekandpoke")),
        tuple("", "io.peekandpoke", File("io/peekandpoke")),
        tuple("io.peekandpoke", "io.other", File("../other")),
        tuple("io.peekandpoke", "io.other.package", File("../other/package")),
        tuple("io.peekandpoke.ultra", "com", File("../../../com")),
        tuple("io.peekandpoke.ultra", "com.thebase", File("../../../com/thebase"))
    ).forEachIndexed { idx, (src, target, expected) ->

        "String.getRelativePackagePath #$idx: \"$src\".getRelativePackagePath(\"$target\") should be '$expected'" {

            val result = src.getRelativePackagePath(target)

            result shouldBe expected
        }
    }
})
