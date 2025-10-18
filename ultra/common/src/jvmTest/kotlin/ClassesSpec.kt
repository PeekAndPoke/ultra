package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.model.tuple
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
        tuple("de.peekandpoke", "", File("../..")),
        tuple("de.peekandpoke", "de", File("..")),
        tuple("de.peekandpoke", "de.peekandpoke", File("")),
        tuple("de", "de.peekandpoke", File("peekandpoke")),
        tuple("", "de.peekandpoke", File("de/peekandpoke")),
        tuple("de.peekandpoke", "de.other", File("../other")),
        tuple("de.peekandpoke", "de.other.package", File("../other/package")),
        tuple("de.peekandpoke.ultra", "com", File("../../../com")),
        tuple("de.peekandpoke.ultra", "com.thebase", File("../../../com/thebase"))
    ).forEachIndexed { idx, (src, target, expected) ->

        "String.getRelativePackagePath #$idx: \"$src\".getRelativePackagePath(\"$target\") should be '$expected'" {

            val result = src.getRelativePackagePath(target)

            result shouldBe expected
        }
    }
})
