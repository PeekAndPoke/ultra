package de.peekandpoke.kraft.addon.semanticui

import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.testbed.TestBed
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
