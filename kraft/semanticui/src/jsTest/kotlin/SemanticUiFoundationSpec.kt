package de.peekandpoke.kraft.semanticui

import de.peekandpoke.kraft.testing.TestBed
import de.peekandpoke.ultra.semanticui.ui
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

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
