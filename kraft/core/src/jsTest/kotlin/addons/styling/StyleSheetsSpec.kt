package de.peekandpoke.kraft.addons.styling

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.browser.document
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.color
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLStyleElement

class StyleSheetsSpec : StringSpec({

    "CssRule should format its selector properly" {
        val rule = CssRule("my-class")
        rule.name shouldBe "my-class"
        rule.selector shouldBe ".my-class"
        rule.toString() shouldBe "my-class"
    }

    "StyleSheets.mangleClassName should return a unique mangled name" {
        val original = "my-class"
        val mangled1 = StyleSheets.mangleClassName(original)
        val mangled2 = StyleSheets.mangleClassName(original)

        mangled1 shouldContain original
        mangled2 shouldContain original
        mangled1 shouldNotBe mangled2
    }

    "StyleSheet should build CSS rules correctly using the Property Delegate API" {
        val sheet = object : StyleSheet() {
            val myRule by rule {
                color = Color.red
            }

            val contextualRule by rule("div.test") {
                color = Color.blue
            }

            val nestedRule by rule(myRule) {
                backgroundColor = Color.black
            }
        }

        val css = sheet.css

        css shouldContain ".${sheet.myRule.name} {"
        css shouldContain "color: red;"

        css shouldContain "div.test.${sheet.contextualRule.name} {"
        css shouldContain "color: blue;"

        css shouldContain "${sheet.myRule.selector}.${sheet.nestedRule.name} {"
        css shouldContain "background-color: black;"
    }

    "RawStyleSheet should mount and unmount CSS text into the DOM" {
        val rawCss = ".test-class { color: black; }"
        val sheet = RawStyleSheet(rawCss)

        StyleSheets.mount(sheet)

        val styles = document.head?.getElementsByTagName("style")
        var found = false
        var mountedStyleTag: HTMLStyleElement? = null

        if (styles != null) {
            for (i in 0 until styles.length) {
                if (styles.item(i)?.textContent?.contains(rawCss) == true) {
                    found = true
                    mountedStyleTag = styles.item(i) as HTMLStyleElement
                    break
                }
            }
        }

        found shouldBe true
        mountedStyleTag?.id shouldContain "injected-"

        StyleSheets.unmount(sheet)

        var foundAfterUnmount = false
        val stylesAfter = document.head?.getElementsByTagName("style")
        if (stylesAfter != null) {
            for (i in 0 until stylesAfter.length) {
                if (stylesAfter.item(i)?.textContent?.contains(rawCss) == true) {
                    foundAfterUnmount = true
                    break
                }
            }
        }

        foundAfterUnmount shouldBe false
    }

    "StyleSheet should mount and unmount compiled CSS into the DOM" {
        val sheet = object : StyleSheet() {
            @Suppress("unused")
            val rule1 by rule {
                color = Color.pink
            }
        }

        sheet.mount(document.head!!)

        val styles = document.head?.getElementsByTagName("style")
        var found = false

        if (styles != null) {
            for (i in 0 until styles.length) {
                if (styles.item(i)?.textContent?.contains("color: pink;") == true) {
                    found = true
                    break
                }
            }
        }

        found shouldBe true

        sheet.unmount()

        // Let's ensure it's removed
        var foundAfterUnmount = false
        val stylesAfter = document.head?.getElementsByTagName("style")
        if (stylesAfter != null) {
            for (i in 0 until stylesAfter.length) {
                if (stylesAfter.item(i)?.textContent?.contains("color: pink;") == true) {
                    foundAfterUnmount = true
                    break
                }
            }
        }

        foundAfterUnmount shouldBe false
    }

    "StyleSheetTag should mount and unmount a link tag into the DOM" {
        val sheetTag = StyleSheetTag {
            href = "https://example.com/styles.css"
        }

        StyleSheets.mount(sheetTag)

        val links = document.head?.getElementsByTagName("link")
        var found = false

        if (links != null) {
            for (i in 0 until links.length) {
                val item = links.item(i) as? HTMLLinkElement
                if (item?.href == "https://example.com/styles.css" && item.rel == "stylesheet") {
                    found = true
                    break
                }
            }
        }

        found shouldBe true

        StyleSheets.unmount(sheetTag)

        var foundAfterUnmount = false
        val linksAfter = document.head?.getElementsByTagName("link")
        if (linksAfter != null) {
            for (i in 0 until linksAfter.length) {
                val item = linksAfter.item(i) as? HTMLLinkElement
                if (item?.href == "https://example.com/styles.css" && item.rel == "stylesheet") {
                    foundAfterUnmount = true
                    break
                }
            }
        }

        foundAfterUnmount shouldBe false
    }
})
