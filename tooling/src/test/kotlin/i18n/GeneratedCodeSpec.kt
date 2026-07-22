package io.peekandpoke.ultra.tooling.i18n

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

/**
 * Emitter identifier-safety checks. That the emitted Kotlin actually COMPILES (and is callable) is
 * proven for real in the `:tooling:i18n-fixture` module, which runs the plugin and compiles the
 * generated accessors on both jvm and js — including keyword/hyphen keys.
 */
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
})
