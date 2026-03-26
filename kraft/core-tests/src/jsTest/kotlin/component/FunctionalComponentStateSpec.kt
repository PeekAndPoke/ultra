package io.peekandpoke.kraft.coretests.component

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.component
import io.peekandpoke.kraft.components.value
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.html.div

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

    div("label first") { +first.toString() }
    div("label second") { +second.toString() }
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
