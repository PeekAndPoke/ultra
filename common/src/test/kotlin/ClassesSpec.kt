package de.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import java.io.File

class ClassesSpec : StringSpec({

    // String.getRelativePackagePath ///////////////////////////////////////////////////////////////////////////////////

    println(
        File("/opt/dev", File("").path)
    )

    listOf(
        row("", "", File("")),
        row("de", "", File("..")),
        row("de", "de", File("")),
        row("", "de", File("de")),
        row("de.peekandpoke", "", File("../..")),
        row("de.peekandpoke", "de", File("..")),
        row("de.peekandpoke", "de.peekandpoke", File("")),
        row("de", "de.peekandpoke", File("peekandpoke")),
        row("", "de.peekandpoke", File("de/peekandpoke")),
        row("de.peekandpoke", "de.other", File("../other")),
        row("de.peekandpoke", "de.other.package", File("../other/package")),
        row("de.peekandpoke.ultra", "com", File("../../../com")),
        row("de.peekandpoke.ultra", "com.thebase", File("../../../com/thebase"))
    ).forEachIndexed { idx, (src, target, expected) ->

        "String.getRelativePackagePath #$idx: \"$src\".getRelativePath(\"$target\") should be $expected" {

            val result = src.getRelativePackagePath(target)

            result shouldBe expected
        }
    }
})
