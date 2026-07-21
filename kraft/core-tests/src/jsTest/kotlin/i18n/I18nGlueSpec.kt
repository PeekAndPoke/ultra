package io.peekandpoke.kraft.coretests.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.i18n.Formatting
import io.peekandpoke.kraft.i18n.I18nController
import io.peekandpoke.kraft.i18n.Translations
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.i18n.I18n
import io.peekandpoke.ultra.i18n.I18nTranslate
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.i18n.MapI18nCatalog
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.Element

private val catalog = MapI18nCatalog(
    "en" to mapOf("greeting" to "Hello"),
    "de" to mapOf("greeting" to "Hallo"),
)

private fun base() = I18n(Locale("en"), fallback = Locale("en")) { install(catalog) }

// Stands in for a generated accessor (the real ones are emitted per module by the S2/S3 codegen).
private fun I18nTranslate.greeting(): String = i18n.resolve("greeting")

@Suppress("TestFunctionName")
private fun Tag.LangLabel() = comp { LangLabel(it) }

private class LangLabel(ctx: NoProps) : PureComponent(ctx) {
    private val t by Translations
    override fun VDom.render() {
        div(classes = "label") { +t.greeting() }
    }
}

@Suppress("TestFunctionName")
private fun Tag.LocaleTag() = comp { LocaleTag(it) }

private class LocaleTag(ctx: NoProps) : PureComponent(ctx) {
    private val format by Formatting
    override fun VDom.render() {
        div(classes = "loc") { +format.locale.tag }
    }
}

/** Polls the DOM until [css]'s text equals [expected] (deterministic wait for the redraw). */
private suspend fun KQuery<Element>.awaitText(css: String, expected: String) {
    repeat(100) {
        if (selectCss(css).textContent() == expected) return
        delay(5)
    }
    selectCss(css).textContent() shouldBe expected // final assert gives a clear failure message
}

class I18nGlueSpec : StringSpec({

    "a component using `by Translations` re-renders in the new language after setLang" {
        val ctrl = I18nController.inMemory(base(), initialLang = "en")

        TestBed.preact(appSetup = { i18n(ctrl) }, view = { LangLabel() }) { root ->
            root.awaitText(".label", "Hello")
            ctrl.setLang(Locale("de"))
            root.awaitText(".label", "Hallo")
        }
    }

    "a component using `by Formatting` re-renders with the new locale after setLang" {
        val ctrl = I18nController.inMemory(base(), initialLang = "en")

        TestBed.preact(appSetup = { i18n(ctrl) }, view = { LocaleTag() }) { root ->
            root.awaitText(".loc", "en")
            ctrl.setLang(Locale("de"))
            root.awaitText(".loc", "de")
        }
    }

    "`by Translations` degrades to the key (does not crash) when the app registers no i18n" {
        // empty appSetup -> only the builder's default empty I18nController is present
        TestBed.preact(appSetup = { }, view = { LangLabel() }) { root ->
            root.awaitText(".label", "greeting")
        }
    }

    "create() boots from the stored locale over the initial language (stored > browser > fallback)" {
        val key = "test.i18n.boot.stored"
        window.localStorage.setItem(key, "\"de\"") // JSON-encoded string

        val ctrl = I18nController.create(base(), initialLang = "en", storageKey = key)

        ctrl.locale shouldBe Locale("de")
        window.localStorage.removeItem(key)
    }

    "create() uses the initial language when nothing is stored" {
        val key = "test.i18n.boot.empty"
        window.localStorage.removeItem(key)

        val ctrl = I18nController.create(base(), initialLang = "de", storageKey = key)

        ctrl.locale shouldBe Locale("de")
        window.localStorage.removeItem(key)
    }
})
