@file:OptIn(ExperimentalCompilerApi::class)

package io.peekandpoke.ultra.tooling.i18n

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.assertions.throwables.shouldThrow
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class GeneratedCodeSpec : StringSpec({

    val config = I18nGenConfig(packageName = "io.peekandpoke.demo.i18n", moduleName = "Demo")

    "keyword, hyphen and leading-digit names are backtick-quoted" {
        val en = YamlCatalogParser.parse(
            "en",
            """
                forms:
                  is: "reserved word"
                  my-key: "hyphen {{first-name}}"
                  2fa: "leading digit"
            """.trimIndent(),
        )
        val accessors = KotlinEmitter.emit(config, en, listOf(en)).first { it.fileName.endsWith("I18n.kt") }.content

        accessors shouldContain "fun FormsI18n.`is`(): String ="
        accessors shouldContain "fun FormsI18n.`my-key`(vararg forcedNamed: Unit, `first-name`: Any?): String ="
        accessors shouldContain "mapOf(\"first-name\" to `first-name`)"
        accessors shouldContain "fun FormsI18n.`2fa`(): String ="
    }

    "a name outside the [A-Za-z0-9_-] whitelist is rejected (injection guard)" {
        val en = YamlCatalogParser.parse("en", "\"x(): Unit; fun y\": \"hi\"")

        shouldThrow<IllegalArgumentException> {
            KotlinEmitter.emit(config, en, listOf(en))
        }
    }

    "generated accessors + catalog compile against ultra:i18n" {
        val en = YamlCatalogParser.parse(
            "en",
            """
                forms:
                  invalidValue: "Invalid"
                  is: "reserved word key"
                  my-key: "hyphen {{first-name}}"
                  minLength_one: "at least {{count}}"
                  minLength_other: "at least {{count}} chars in {{folder}}"
                  address:
                    street: "street {{n}}"
            """.trimIndent(),
        )
        val de = YamlCatalogParser.parse("de", """forms:${'\n'}  invalidValue: "Ungültig"""")

        val files = KotlinEmitter.emit(config, en, listOf(en, de))
        val result = KotlinCompilation().apply {
            sources = files.map { SourceFile.kotlin(it.fileName, it.content) }
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }
})
