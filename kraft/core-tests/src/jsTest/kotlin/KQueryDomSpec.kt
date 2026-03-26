package io.peekandpoke.kraft.coretests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.allChecked
import io.peekandpoke.kraft.testing.allDisabled
import io.peekandpoke.kraft.testing.allEnabled
import io.peekandpoke.kraft.testing.allHaveAttr
import io.peekandpoke.kraft.testing.allHaveClass
import io.peekandpoke.kraft.testing.allVisible
import io.peekandpoke.kraft.testing.anyChecked
import io.peekandpoke.kraft.testing.anyDisabled
import io.peekandpoke.kraft.testing.anyEnabled
import io.peekandpoke.kraft.testing.anyHasAttr
import io.peekandpoke.kraft.testing.anyHasClass
import io.peekandpoke.kraft.testing.anyVisible
import io.peekandpoke.kraft.testing.attr
import io.peekandpoke.kraft.testing.awaitCss
import io.peekandpoke.kraft.testing.check
import io.peekandpoke.kraft.testing.checkedStates
import io.peekandpoke.kraft.testing.children
import io.peekandpoke.kraft.testing.classes
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.containsText
import io.peekandpoke.kraft.testing.disabledStates
import io.peekandpoke.kraft.testing.filterElements
import io.peekandpoke.kraft.testing.first
import io.peekandpoke.kraft.testing.innerHTML
import io.peekandpoke.kraft.testing.last
import io.peekandpoke.kraft.testing.nth
import io.peekandpoke.kraft.testing.outerHTML
import io.peekandpoke.kraft.testing.parents
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.selectDebugId
import io.peekandpoke.kraft.testing.setValue
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.testing.typeText
import io.peekandpoke.kraft.testing.uncheck
import io.peekandpoke.kraft.testing.values
import io.peekandpoke.kraft.testing.visibilityStates
import io.peekandpoke.ultra.html.onInput
import kotlinx.coroutines.delay
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.b
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.li
import kotlinx.html.span
import kotlinx.html.textArea
import kotlinx.html.ul
import org.w3c.dom.HTMLInputElement

class KQueryDomSpec : StringSpec({

    // =========================================================================================================
    // Attributes
    // =========================================================================================================

    "attr() returns attribute values for all matched elements" {
        TestBed.preact({
            a { href = "https://a.com"; +"A" }
            a { href = "https://b.com"; +"B" }
            a { +"No href" }
        }) { root ->
            root.selectCss("a").attr("href") shouldContainExactly listOf(
                "https://a.com",
                "https://b.com",
                null,
            )
        }
    }

    "allHaveAttr() checks all elements" {
        TestBed.preact({
            input { id = "a"; disabled = true }
            input { id = "b"; disabled = true }
        }) { root ->
            root.selectCss("input").allHaveAttr("disabled") shouldBe true
        }
    }

    "anyHasAttr() checks any element" {
        TestBed.preact({
            input { id = "a" }
            input { id = "b"; disabled = true }
        }) { root ->
            root.selectCss("input").anyHasAttr("disabled") shouldBe true
            root.selectCss("input").allHaveAttr("disabled") shouldBe false
        }
    }

    // =========================================================================================================
    // CSS classes
    // =========================================================================================================

    "classes() returns class sets for all matched elements" {
        TestBed.preact({
            span(classes = "foo bar") {}
            span(classes = "baz") {}
            span {}
        }) { root ->
            root.selectCss("span").classes() shouldContainExactly listOf(
                setOf("foo", "bar"),
                setOf("baz"),
                emptySet(),
            )
        }
    }

    "allHaveClass() checks all elements" {
        TestBed.preact({
            span(classes = "active primary") {}
            span(classes = "active secondary") {}
        }) { root ->
            root.selectCss("span").allHaveClass("active") shouldBe true
            root.selectCss("span").allHaveClass("primary") shouldBe false
        }
    }

    "anyHasClass() checks any element" {
        TestBed.preact({
            span(classes = "active") {}
            span(classes = "inactive") {}
        }) { root ->
            root.selectCss("span").anyHasClass("active") shouldBe true
            root.selectCss("span").anyHasClass("missing") shouldBe false
        }
    }

    // =========================================================================================================
    // Form values
    // =========================================================================================================

    "values() returns values for all matched inputs" {
        TestBed.preact({
            input { id = "a"; value = "hello" }
            input { id = "b"; value = "world" }
        }) { root ->
            root.selectCss("input").values() shouldContainExactly listOf("hello", "world")
        }
    }

    "values() works with textarea" {
        TestBed.preact({
            textArea { +"content here" }
        }) { root ->
            // textarea initial value comes from the DOM
            val vals = root.selectCss("textarea").values()
            vals.size shouldBe 1
        }
    }

    "setValue() sets value on all matched inputs" {
        TestBed.preact({
            input { id = "a" }
            input { id = "b" }
        }) { root ->
            root.selectCss("input").setValue("test")
            root.selectCss("input").values() shouldContainExactly listOf("test", "test")
        }
    }

    "typeText() sets value and fires events" {
        TestBed.preact({
            div(classes = "result") { +"empty" }
            input {
                id = "target"
                onInput { evt ->
                    val inp = evt.currentTarget as HTMLInputElement
                    inp.parentElement!!.querySelector(".result")!!.textContent = inp.value
                }
            }
        }) { root ->
            root.selectCss("#target").typeText("typed!")
            delay(10)
            root.selectCss(".result").textContent() shouldBe "typed!"
        }
    }

    // =========================================================================================================
    // Checked state
    // =========================================================================================================

    "checkedStates() returns checked state for all matched inputs" {
        TestBed.preact({
            input(type = InputType.checkBox) { id = "a"; checked = true }
            input(type = InputType.checkBox) { id = "b" }
        }) { root ->
            root.selectCss("input").checkedStates() shouldContainExactly listOf(true, false)
        }
    }

    "allChecked() and anyChecked()" {
        TestBed.preact({
            input(type = InputType.checkBox) { id = "a"; checked = true }
            input(type = InputType.checkBox) { id = "b" }
        }) { root ->
            root.selectCss("input").allChecked() shouldBe false
            root.selectCss("input").anyChecked() shouldBe true
        }
    }

    "check() sets checked on all inputs" {
        TestBed.preact({
            input(type = InputType.checkBox) { id = "a" }
            input(type = InputType.checkBox) { id = "b" }
        }) { root ->
            root.selectCss("input").check()
            root.selectCss("input").allChecked() shouldBe true
        }
    }

    "uncheck() clears checked on all inputs" {
        TestBed.preact({
            input(type = InputType.checkBox) { id = "a"; checked = true }
            input(type = InputType.checkBox) { id = "b"; checked = true }
        }) { root ->
            root.selectCss("input").uncheck()
            root.selectCss("input").allChecked() shouldBe false
        }
    }

    // =========================================================================================================
    // Enabled / Disabled
    // =========================================================================================================

    "disabledStates() returns state for all matched elements" {
        TestBed.preact({
            input { id = "a"; disabled = true }
            input { id = "b" }
        }) { root ->
            root.selectCss("input").disabledStates() shouldContainExactly listOf(true, false)
        }
    }

    "allDisabled() and anyDisabled()" {
        TestBed.preact({
            input { id = "a"; disabled = true }
            input { id = "b" }
        }) { root ->
            root.selectCss("input").allDisabled() shouldBe false
            root.selectCss("input").anyDisabled() shouldBe true
        }
    }

    "allEnabled() and anyEnabled()" {
        TestBed.preact({
            input { id = "a"; disabled = true }
            input { id = "b" }
        }) { root ->
            root.selectCss("input").allEnabled() shouldBe false
            root.selectCss("input").anyEnabled() shouldBe true
        }
    }

    // =========================================================================================================
    // HTML content
    // =========================================================================================================

    "innerHTML() returns inner HTML for all matched elements" {
        TestBed.preact({
            div(classes = "item") { span { +"Hello" } }
            div(classes = "item") { b { +"Bold" } }
        }) { root ->
            val html = root.selectCss(".item").innerHTML()
            html.size shouldBe 2
            html[0] shouldBe "<span>Hello</span>"
            html[1] shouldBe "<b>Bold</b>"
        }
    }

    "outerHTML() returns outer HTML for all matched elements" {
        TestBed.preact({
            span(classes = "tag") { +"X" }
        }) { root ->
            val html = root.selectCss(".tag").outerHTML()
            html.size shouldBe 1
            html[0] shouldBe """<span class="tag">X</span>"""
        }
    }

    // =========================================================================================================
    // DOM traversal
    // =========================================================================================================

    "parents() returns parent elements (deduplicated)" {
        TestBed.preact({
            div(classes = "parent") {
                span(classes = "child") { +"A" }
                span(classes = "child") { +"B" }
            }
        }) { root ->
            val parents = root.selectCss(".child").parents()
            parents.size shouldBe 1
            parents.allHaveClass("parent") shouldBe true
        }
    }

    "children() returns all direct children" {
        TestBed.preact({
            ul {
                id = "list"
                li { +"A" }
                li { +"B" }
                li { +"C" }
            }
        }) { root ->
            root.selectCss("#list").children().size shouldBe 3
        }
    }

    "first() returns only the first match" {
        TestBed.preact({
            div(classes = "item") { +"First" }
            div(classes = "item") { +"Second" }
            div(classes = "item") { +"Third" }
        }) { root ->
            val first = root.selectCss(".item").first()
            first.size shouldBe 1
            first.textContent() shouldBe "First"
        }
    }

    "last() returns only the last match" {
        TestBed.preact({
            div(classes = "item") { +"First" }
            div(classes = "item") { +"Second" }
            div(classes = "item") { +"Third" }
        }) { root ->
            val last = root.selectCss(".item").last()
            last.size shouldBe 1
            last.textContent() shouldBe "Third"
        }
    }

    "nth() returns element at index" {
        TestBed.preact({
            div(classes = "item") { +"Zero" }
            div(classes = "item") { +"One" }
            div(classes = "item") { +"Two" }
        }) { root ->
            root.selectCss(".item").nth(0).textContent() shouldBe "Zero"
            root.selectCss(".item").nth(1).textContent() shouldBe "One"
            root.selectCss(".item").nth(2).textContent() shouldBe "Two"
            root.selectCss(".item").nth(99).size shouldBe 0
        }
    }

    "filterElements() filters by predicate" {
        TestBed.preact({
            div(classes = "item active") { +"A" }
            div(classes = "item") { +"B" }
            div(classes = "item active") { +"C" }
        }) { root ->
            val active = root.selectCss(".item").filterElements { it.classList.contains("active") }
            active.size shouldBe 2
            active.textContent(" ") shouldBe "A C"
        }
    }

    // =========================================================================================================
    // Empty query safety
    // =========================================================================================================

    "all methods are safe on empty queries" {
        TestBed.preact({
            div { +"nothing here" }
        }) { root ->
            val empty = root.selectCss(".nonexistent")
            empty.size shouldBe 0

            // Query methods return empty lists
            empty.attr("id") shouldBe emptyList()
            empty.classes() shouldBe emptyList()
            empty.values() shouldBe emptyList()
            empty.checkedStates() shouldBe emptyList()
            empty.disabledStates() shouldBe emptyList()
            empty.visibilityStates() shouldBe emptyList()
            empty.innerHTML() shouldBe emptyList()
            empty.outerHTML() shouldBe emptyList()

            // Boolean methods return false on empty
            empty.allHaveAttr("x") shouldBe false
            empty.anyHasAttr("x") shouldBe false
            empty.allHaveClass("x") shouldBe false
            empty.anyHasClass("x") shouldBe false
            empty.allChecked() shouldBe false
            empty.anyChecked() shouldBe false
            empty.allDisabled() shouldBe false
            empty.anyDisabled() shouldBe false
            empty.allEnabled() shouldBe false
            empty.anyEnabled() shouldBe false
            empty.allVisible() shouldBe false
            empty.anyVisible() shouldBe false

            // Traversal returns empty
            empty.parents().size shouldBe 0
            empty.children().size shouldBe 0
            empty.first().size shouldBe 0
            empty.last().size shouldBe 0
            empty.nth(0).size shouldBe 0

            // Mutations are no-ops (don't crash)
            empty.setValue("x")
            empty.typeText("x")
            empty.check()
            empty.uncheck()
            empty.click()
        }
    }

    // =========================================================================================================
    // awaitCss
    // =========================================================================================================

    "awaitCss() returns empty on timeout" {
        TestBed.preact({
            div { +"static" }
        }) { root ->
            val result = root.awaitCss(".never-appears", timeoutMs = 100, intervalMs = 20)
            result.size shouldBe 0
        }
    }

    "awaitCss() finds existing elements immediately" {
        TestBed.preact({
            div(classes = "exists") { +"here" }
        }) { root ->
            val result = root.awaitCss(".exists", timeoutMs = 100)
            result.size shouldBe 1
            result.textContent() shouldBe "here"
        }
    }

    // =========================================================================================================
    // selectCss - typed variant
    // =========================================================================================================

    "selectCss<T>() filters by element type" {
        TestBed.preact({
            span {
                id = "wrapper"
                input { type = InputType.text }
                input { type = InputType.password }
            }
        }) { root ->
            val wrapper = root.selectCss("#wrapper")
            val inputs = wrapper.selectCss<HTMLInputElement>("input")
            inputs.size shouldBe 2
            inputs.forEach { it.shouldBeInstanceOf<HTMLInputElement>() }
        }
    }

    "selectCss() returns empty when no elements match" {
        TestBed.preact({
            div { +"only a div" }
        }) { root ->
            val result = root.selectCss(".nonexistent")
            result.size shouldBe 0
        }
    }

    "selectCss() with wildcard returns all descendants" {
        TestBed.preact({
            div { span { +"a" }; span { +"b" } }
        }) { root ->
            val all = root.selectCss("*")
            all.isNotEmpty() shouldBe true
        }
    }

    "select() with custom Selector works" {
        TestBed.preact({
            div(classes = "target") { +"found" }
            div { +"not found" }
        }) { root ->
            val result = root.selectCss(".target")
            result.size shouldBe 1
            result.textContent() shouldBe "found"
        }
    }

    // =========================================================================================================
    // selectDebugId
    // =========================================================================================================

    "selectDebugId() finds element by debug-id attribute" {
        TestBed.preact({
            div { attributes["debug-id"] = "header"; +"Header" }
            div { attributes["debug-id"] = "footer"; +"Footer" }
            div { +"No debug id" }
        }) { root ->
            val header = root.selectDebugId("header")
            header.size shouldBe 1
            header.textContent() shouldBe "Header"

            val footer = root.selectDebugId("footer")
            footer.size shouldBe 1
            footer.textContent() shouldBe "Footer"
        }
    }

    "selectDebugId<T>() finds and filters by element type" {
        TestBed.preact({
            input { attributes["debug-id"] = "email"; type = InputType.email }
            div { attributes["debug-id"] = "label"; +"Email:" }
        }) { root ->
            val emailInput = root.selectDebugId<HTMLInputElement>("email")
            emailInput.size shouldBe 1
            emailInput[0].shouldBeInstanceOf<HTMLInputElement>()
            emailInput[0].type shouldBe "email"
        }
    }

    "selectDebugId() returns empty when no match" {
        TestBed.preact({
            div { attributes["debug-id"] = "exists"; +"Here" }
        }) { root ->
            val result = root.selectDebugId("nonexistent")
            result.size shouldBe 0
        }
    }

    // =========================================================================================================
    // textContent and containsText (moved from testbed_helpers)
    // =========================================================================================================

    "textContent() concatenates text from all matched elements" {
        TestBed.preact({
            span { +"Hello" }
            span { +" " }
            span { +"World" }
        }) { root ->
            root.selectCss("span").textContent() shouldBe "Hello World"
            root.selectCss("span").textContent(", ") shouldBe "Hello,  , World"
        }
    }

    "containsText() checks for text in any matched element" {
        TestBed.preact({
            div { +"first item" }
            div { +"second item" }
        }) { root ->
            val divs = root.selectCss("div")
            divs.containsText("first") shouldBe true
            divs.containsText("second") shouldBe true
            divs.containsText("third") shouldBe false
        }
    }
})
