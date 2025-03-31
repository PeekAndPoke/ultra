package de.peekandpoke.kraft.component

import de.peekandpoke.kraft.components.component
import de.peekandpoke.kraft.components.value
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.testbed.TestBed
import de.peekandpoke.kraft.testbed.textContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

private val FuncComponent = component {

    var first: Int by value { 0 }

    val second: Int by value { 0 }
        .initLazy {
            flow {
                delay(10)
                emit(123)
            }
        }.onChange {
            first = it + 1
        }

    ui.label.with("first") { +first.toString() }
    ui.label.with("second") { +second.toString() }
}

class FunctionalComponentStateSpec : StringSpec({

    "Lazy init must work" {
        TestBed.preact({
            FuncComponent()
        }) { root ->
            delay(50)

            root.selectCss(".label.first").textContent() shouldBe "124"
            root.selectCss(".label.second").textContent() shouldBe "123"
        }
    }
})
