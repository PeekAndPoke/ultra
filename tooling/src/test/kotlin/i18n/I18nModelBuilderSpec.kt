package io.peekandpoke.ultra.tooling.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.i18n.model.I18nMessage
import io.peekandpoke.ultra.i18n.model.I18nModelBuilder
import io.peekandpoke.ultra.i18n.model.I18nNamespace

class I18nModelBuilderSpec : StringSpec({

    "parses nested, plural and placeholder messages from the fallback" {
        val yaml = """
            forms:
              invalidValue: "Invalid value"
              greeting: "Hello {{name}}"
              minLength_one: "At least {{count}} char"
              minLength_other: "At least {{count}} chars"
              address:
                street: "Street {{n}}"
        """.trimIndent()

        val tree = I18nModelBuilder.build(YamlCatalogParser.parse("en", yaml))

        tree.map { it.name } shouldBe listOf("forms")
        val forms = tree.single() as I18nNamespace
        forms.children.map { it.name } shouldBe listOf("invalidValue", "greeting", "minLength", "address")

        val invalidValue = forms.children[0] as I18nMessage
        invalidValue.key shouldBe "forms.invalidValue"
        invalidValue.placeholders shouldBe emptyList()
        invalidValue.plural shouldBe false

        val greeting = forms.children[1] as I18nMessage
        greeting.placeholders shouldBe listOf("name")
        greeting.plural shouldBe false

        val minLength = forms.children[2] as I18nMessage
        minLength.key shouldBe "forms.minLength"
        minLength.plural shouldBe true
        minLength.placeholders shouldBe emptyList() // count is implicit, excluded

        val address = forms.children[3] as I18nNamespace
        val street = address.children.single() as I18nMessage
        street.key shouldBe "forms.address.street"
        street.placeholders shouldBe listOf("n")
    }

    "the parsed locale tag is normalized, so baked keys match runtime lookups" {
        YamlCatalogParser.parse("de_ch", "x: y").localeTag shouldBe "de-CH"
        YamlCatalogParser.parse("DE", "x: y").localeTag shouldBe "de"
    }
})
