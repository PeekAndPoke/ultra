package io.peekandpoke.kraft.coretests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.kraft.addons.styling.CssRule
import io.peekandpoke.kraft.addons.styling.RawStyleSheet
import io.peekandpoke.kraft.addons.styling.StyleSheet
import io.peekandpoke.kraft.addons.styling.StyleSheets
import kotlinx.browser.document
import kotlinx.css.Color
import kotlinx.css.Display
import kotlinx.css.color
import kotlinx.css.display

// =====================================================================================================================
// Test stylesheets (autoMount = false to avoid side effects between tests)
// =====================================================================================================================

private object TestStyles : StyleSheet(autoMount = false) {
    val container by rule {
        display = Display.flex
    }

    val header by rule {
        color = Color.red
    }
}

private object NestedStyles : StyleSheet(autoMount = false) {
    val card by rule {
        display = Display.flex

        "h2" {
            color = Color.red
        }

        "> .content" {
            color = Color.blue
        }
    }
}

private object ContextStyles : StyleSheet(autoMount = false) {
    val parent by rule {
        display = Display.block
    }

    val child by rule(parent) {
        color = Color.blue
    }
}

// =====================================================================================================================
// Tests
// =====================================================================================================================

class StyleSheetSpec : StringSpec({

    "CssRule.name returns the mangled class name" {
        val rule = CssRule("myClass_abc12345")
        rule.name shouldBe "myClass_abc12345"
    }

    "CssRule.selector returns dot-prefixed mangled name" {
        val rule = CssRule("myClass_abc12345")
        rule.selector shouldBe ".myClass_abc12345"
    }

    "CssRule.toString returns the name" {
        val rule = CssRule("myClass_abc12345")
        rule.toString() shouldBe "myClass_abc12345"
    }

    "CssRule.invoke returns the name" {
        val rule = CssRule("myClass_abc12345")
        rule() shouldBe "myClass_abc12345"
    }

    "StyleSheet rule() delegate creates rules with mangled names" {
        // Mangled names should start with the property name followed by underscore
        TestStyles.container.name shouldStartWith "container_"
        TestStyles.header.name shouldStartWith "header_"

        // The two rules should have different mangled names
        TestStyles.container.name shouldNotBe TestStyles.header.name
    }

    "StyleSheet rule() delegate selector is dot-prefixed mangled name" {
        TestStyles.container.selector shouldBe ".${TestStyles.container.name}"
        TestStyles.header.selector shouldBe ".${TestStyles.header.name}"
    }

    "StyleSheet.css generates valid CSS content" {
        val css = TestStyles.css

        // CSS should contain the mangled selectors
        css shouldContain ".${TestStyles.container.name}"
        css shouldContain ".${TestStyles.header.name}"

        // CSS should contain the property values
        css shouldContain "display"
        css shouldContain "flex"
        css shouldContain "color"
    }

    "StyleSheet rule with nested string selectors generates scoped CSS" {
        val css = NestedStyles.css

        // The card rule itself
        css shouldContain ".${NestedStyles.card.name}"
        css shouldContain "display"
        css shouldContain "flex"

        // Nested h2 selector
        css shouldContain "h2"
        css shouldContain "color"

        // Nested > .content selector
        css shouldContain "> .content"
    }

    "StyleSheet rule with context rule scopes the selector" {
        val css = ContextStyles.css

        // The child rule should be scoped under the parent selector
        css shouldContain "${ContextStyles.parent.selector}.${ContextStyles.child.name}"
    }

    "RawStyleSheet stores raw CSS" {
        val rawCss = "body { margin: 0; }"
        val sheet = RawStyleSheet(rawCss, autoMount = false)
        sheet.css shouldBe rawCss
    }

    "StyleSheets.mount() injects a style element into the document head" {
        val rawCss = ".test-mount-class { font-size: 14px; }"
        val sheet = RawStyleSheet(rawCss, autoMount = false)

        val headBefore = document.head!!.innerHTML

        StyleSheets.mount(sheet)

        val headAfter = document.head!!.innerHTML

        // A new style element should have been added
        headAfter.length shouldNotBe headBefore.length

        // The style element should contain the CSS
        val styleEl = document.head!!.querySelector("style[id='injected-${rawCss.hashCode()}']")
        styleEl shouldNotBe null
        styleEl!!.textContent shouldBe rawCss

        // Cleanup
        StyleSheets.unmount(sheet)
    }

    "StyleSheets.unmount() removes the style element" {
        val rawCss = ".test-unmount-class { font-weight: bold; }"
        val sheet = RawStyleSheet(rawCss, autoMount = false)

        StyleSheets.mount(sheet)

        // Verify it was mounted
        val styleEl = document.head!!.querySelector("style[id='injected-${rawCss.hashCode()}']")
        styleEl shouldNotBe null

        // Unmount
        StyleSheets.unmount(sheet)

        // Verify it was removed
        val styleElAfter = document.head!!.querySelector("style[id='injected-${rawCss.hashCode()}']")
        styleElAfter shouldBe null
    }

    "RawStyleSheet with autoMount=true mounts automatically" {
        val rawCss = ".auto-mount-test { opacity: 0.5; }"
        val sheet = RawStyleSheet(rawCss, autoMount = true)

        // With autoMount=true, RawStyleSheet calls StyleSheets.mount() in init,
        // so the style element should already be present
        val styleEl = document.head!!.querySelector("style[id='injected-${rawCss.hashCode()}']")
        styleEl shouldNotBe null
        styleEl!!.textContent shouldBe rawCss

        // Cleanup
        StyleSheets.unmount(sheet)
    }

    "RawStyleSheet with autoMount=false does NOT mount automatically" {
        val rawCss = ".no-auto-mount-test { visibility: hidden; }"
        val sheet = RawStyleSheet(rawCss, autoMount = false)

        // With autoMount=false, no style element should be present
        val styleEl = document.head!!.querySelector("style[id='injected-${rawCss.hashCode()}']")
        styleEl shouldBe null
    }

    "Mounting the same sheet twice does not duplicate the style element" {
        val rawCss = ".duplicate-mount-test { border: 1px solid; }"
        val sheet = RawStyleSheet(rawCss, autoMount = false)

        StyleSheets.mount(sheet)
        StyleSheets.mount(sheet)

        val styleEls = document.head!!.querySelectorAll("style[id='injected-${rawCss.hashCode()}']")
        styleEls.length shouldBe 1

        // Cleanup
        StyleSheets.unmount(sheet)
    }
})
