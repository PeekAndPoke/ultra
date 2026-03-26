package io.peekandpoke.kraft.semanticui

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.ultra.semanticui.ui

class SemanticUiFoundationSpec : StringSpec({

    "Render Component with Semantic UI content" {

        TestBed.preact(
            {
                ui.header { +"Hello Semantic!" }
            }
        ) {
            val found = it.selectCss("div.ui.header")
            found shouldHaveSize 1
            found.first().textContent shouldBe "Hello Semantic!"
        }
    }
})
