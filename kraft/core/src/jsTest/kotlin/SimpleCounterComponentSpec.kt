package de.peekandpoke.kraft

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.testbed.TestBed
import de.peekandpoke.kraft.testbed.click
import de.peekandpoke.kraft.testbed.textContent
import de.peekandpoke.kraft.vdom.VDom
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h2

@Suppress("TestFunctionName")
private fun Tag.PureCounter() = comp {
    PureCounter(it)
}

private class PureCounter(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    var count by value(0)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        h2 {
            +"Counter"
        }

        div(classes = "value") {
            +"$count"
        }

        div {
            button(classes = "plus-button") {
                onClick { count++ }
                +"PLUS"
            }
        }

        div {
            button(classes = "minus-button") {
                onClick { count-- }
                +"MINUS"
            }
        }
    }
}

class SimpleCounterComponentSpec : StringSpec() {

    init {

        "Clicking the plus button must have an effect" {

            TestBed.preact({
                PureCounter()
            }) { root ->

                root.selectCss(".value").textContent() shouldBe "0"

                repeat(5) {
                    root.selectCss("button.plus-button").click()
                    delay(1)
                }

                delay(50)

                root.selectCss(".value").textContent() shouldBe "5"

                repeat(5) {
                    root.selectCss("button.minus-button").click()
                    delay(1)
                }

                delay(50)

                root.selectCss(".value").textContent() shouldBe "0"
            }
        }
    }
}
