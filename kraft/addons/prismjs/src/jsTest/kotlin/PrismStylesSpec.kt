package io.peekandpoke.kraft.addons.prismjs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.kraft.testing.eventually
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.asList
import org.w3c.dom.css.CSSStyleSheet

/** Prism's stylesheets come from the app's own build output, not from a CDN (ULTRA-00306). */
class PrismStylesSpec : StringSpec({

    fun prismLinks(): List<HTMLLinkElement> = document.querySelectorAll("link[rel=stylesheet]").asList()
        .filterIsInstance<HTMLLinkElement>()
        // Webpack names the emitted files by their hash
        .filter { it.href.substringBefore('?').endsWith(".css") }

    "the default theme is served from the app's origin, and its rules apply" {
        prismJsInternals.styles.loadDefaultTheme()

        val links = prismLinks()
        links.size shouldBeGreaterThan 0
        links.forEach { it.href shouldStartWith window.location.origin }

        eventually {
            val sheets = document.styleSheets.asList().filterIsInstance<CSSStyleSheet>()
            val ours = sheets.filter { sheet -> links.any { it.href == sheet.href } }

            // Same origin: the rules are readable, so the files were really served and parsed
            ours.size shouldBe links.size
            ours.forEach { (it.cssRules.length) shouldBeGreaterThan 0 }
        }
    }

    "loading the theme twice adds it once" {
        prismJsInternals.styles.loadDefaultTheme()
        val before = prismLinks().size

        prismJsInternals.styles.loadDefaultTheme()

        prismLinks().size shouldBe before
    }
})
