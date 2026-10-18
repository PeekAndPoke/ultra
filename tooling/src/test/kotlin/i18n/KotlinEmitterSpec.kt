package io.peekandpoke.ultra.tooling.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.i18n.model.LocaleCatalog

class KotlinEmitterSpec : StringSpec({

    val config = I18nGenConfig(packageName = "io.peekandpoke.kraft.core.i18n", moduleName = "KraftCore")

    val enYaml = """
        forms:
          invalidValue: "Invalid value"
          greeting: "Hello {{name}}"
          minLength_one: "At least {{count}} char"
          minLength_other: "At least {{count}} chars"
          address:
            street: "Street {{n}}"
    """.trimIndent()

    val en = YamlCatalogParser.parse("en", enYaml)
    val de = YamlCatalogParser.parse("de", """forms:${'\n'}  invalidValue: "Ungültiger Wert"""")

    val files = KotlinEmitter.emit(config, fallback = en, allLocales = listOf(en, de))
    val catalog = files.single { it.fileName == "KraftCoreCatalog.kt" }.content
    val accessors = files.single { it.fileName == "KraftCoreI18n.kt" }.content

    "emits two files named from the module" {
        files.map { it.fileName }.toSet() shouldBe setOf("KraftCoreCatalog.kt", "KraftCoreI18n.kt")
    }

    "bakes every locale, keeping RAW plural keys for resolvePlural lookup" {
        catalog shouldContain "object KraftCoreCatalog : I18nCatalog {"
        catalog shouldContain "\"en\" to mapOf("
        catalog shouldContain "\"forms.minLength_one\" to \"At least {{count}} char\","
        catalog shouldContain "\"forms.minLength_other\" to \"At least {{count}} chars\","
        catalog shouldContain "\"de\" to mapOf("
        catalog shouldContain "\"forms.invalidValue\" to \"Ungültiger Wert\","
        catalog shouldContain "override fun template(key: String, locale: Locale): String? ="
        catalog shouldContain "data[locale.tag]?.get(key)"
    }

    "emits a receiver class + I18nTranslate extension property per namespace, nested" {
        accessors shouldContain "class FormsI18n(val i18n: I18n)"
        accessors shouldContain "val I18nTranslate.forms: FormsI18n get() = FormsI18n(i18n)"
        accessors shouldContain "class FormsAddressI18n(val i18n: I18n)"
        accessors shouldContain "val FormsI18n.address: FormsAddressI18n get() = FormsAddressI18n(i18n)"
    }

    "emits a param-less accessor for a placeholder-free message" {
        accessors shouldContain "fun FormsI18n.invalidValue(): String =\n    i18n.resolve(\"forms.invalidValue\")"
    }

    "forces named params via a leading vararg Nothing for placeholder messages" {
        accessors shouldContain
            "fun FormsI18n.greeting(vararg forcedNamed: Unit, name: Any?): String =\n" +
            "    i18n.resolve(\"forms.greeting\", mapOf(\"name\" to name))"
    }

    "emits a plural accessor taking count: Int and calling resolvePlural" {
        accessors shouldContain
            "fun FormsI18n.minLength(vararg forcedNamed: Unit, count: Int): String =\n" +
            "    i18n.resolvePlural(\"forms.minLength\", count, emptyMap())"
    }

    "nested accessor uses the fully qualified key" {
        accessors shouldContain
            "fun FormsAddressI18n.street(vararg forcedNamed: Unit, n: Any?): String =\n" +
            "    i18n.resolve(\"forms.address.street\", mapOf(\"n\" to n))"
    }

    "escapes dollar, quote and backslash in baked templates" {
        val cat = LocaleCatalog("en", mapOf("k" to "cost ${'$'}5 \"x\" \\y"))
        val out = KotlinEmitter.emit(config, cat, listOf(cat)).first { it.fileName.endsWith("Catalog.kt") }.content

        out shouldContain "\"k\" to \"cost \\\$5 \\\"x\\\" \\\\y\","
    }
})
