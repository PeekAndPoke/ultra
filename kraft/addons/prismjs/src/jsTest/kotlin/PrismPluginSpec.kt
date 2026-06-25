package io.peekandpoke.kraft.addons.prismjs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.browser.document
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.get

private fun newPre(): HTMLPreElement = document.createElement("pre") as HTMLPreElement

/**
 * Pure DOM tests for [PrismPlugin.applyTo] — exercises the class/dataset/style mutation each plugin
 * applies to a `<pre>`. No PrismJS CDN load or highlighting involved.
 */
class PrismPluginSpec : StringSpec({

    "CopyToClipboard.applyTo sets class and copy dataset" {
        val pre = newPre()
        PrismPlugin.CopyToClipboard(copy = "Copy").applyTo(pre)
        pre.className shouldContain "copy-to-clipboard"
        pre.dataset["prismjsCopy"] shouldBe "Copy"
    }

    "InlineColor.applyTo sets the inline-color class" {
        val pre = newPre()
        PrismPlugin.InlineColor().applyTo(pre)
        pre.className shouldContain "inline-color"
    }

    "LineNumbers.applyTo sets class, start and soft-wrap" {
        val pre = newPre()
        PrismPlugin.LineNumbers(start = 5, softWrap = true).applyTo(pre)
        pre.className shouldContain "line-numbers"
        pre.dataset["start"] shouldBe "5"
        pre.style.whiteSpace shouldBe "pre-wrap"
    }

    "LineNumbers without soft-wrap leaves whiteSpace untouched" {
        val pre = newPre()
        PrismPlugin.LineNumbers(start = 1, softWrap = false).applyTo(pre)
        pre.className shouldContain "line-numbers"
        pre.style.whiteSpace shouldBe ""
    }

    "ShowLanguage.applyTo sets class and language dataset" {
        val pre = newPre()
        PrismPlugin.ShowLanguage(language = "kotlin").applyTo(pre)
        pre.className shouldContain "show-language"
        pre.dataset["language"] shouldBe "kotlin"
    }

    "ShowLanguage with null language sets class but no language dataset" {
        val pre = newPre()
        PrismPlugin.ShowLanguage(language = null).applyTo(pre)
        pre.className shouldContain "show-language"
        pre.dataset["language"] shouldBe null
    }
})
