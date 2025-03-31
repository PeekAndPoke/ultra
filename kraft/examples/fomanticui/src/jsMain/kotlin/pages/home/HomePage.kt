@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.home

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.kotlinToHtml
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.HomePage() = comp {
    HomePage(it)
}

class HomePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("FomanticUI")

        ui.basic.segment {
            ui.header H1 {
                +"FomanticUI + kotlinx/html + KRAFT = "
                icon.red.music()
            }

            renderFomanticUiExamplesLink()

            renderDslIntro()
        }
    }

    private fun FlowContent.renderFomanticUiExamplesLink() = example {

        ui.two.column.grid {
            ui.column {
                ui.header H2 { +"FomanticUI Docs" }

                ui.big.green.basic.button A {
                    href = "https://fomantic-ui.com/"
                    target = "_blank"
                    icon.linkify()
                    +"Version 2.9.0"
                }
            }

            ui.column {
                ui.header H2 { +"FomanticUI + Kraft code examples" }

                ui.big.green.basic.button A {
                    href = "https://github.com/PeekAndPoke/kraft/tree/master/examples/fomanticui"
                    target = "_blank"
                    icon.github()
                    +"Show me the code!"
                }
            }
        }
    }

    private fun FlowContent.renderDslIntro() = example {
        ui.header H2 { +"Intro to the DSL" }

        ui.info.message {
            +"Fomantic-UI often times needs the css classes to be prefixed with "
            ui.basic.label { +"ui" }
        }

        kotlinToHtml(
            kotlin = """
                ui.basic.segment {
                    +"Hello World"
                }
            """.trimIndent(),
            html = """
                <div class="ui basic segment">
                    Hello World
                </div>
            """.trimIndent()
        )

        ui.info.message {
            +"But there are also cases where you want to omit this prefix. Then you can use "
            ui.basic.label { +"noui" }
        }

        kotlinToHtml(
            kotlin = """
                noui.basic.item {
                    +"Hello World"
                }
            """.trimIndent(),
            html = """
                <div class="basic item">
                    Hello World
                </div>
            """.trimIndent()
        )

        ui.info.message {
            +"Sometimes you want to change the html tag from div to something else"
        }

        kotlinToHtml(
            kotlin = """
                no.basic.item A {
                    href = "https://github.com/PeekAndPoke/kraft"
                    +"KRAFT"
                }
            """.trimIndent(),
            html = """
                <a class="basic item" href="https://github.com/PeekAndPoke/kraft">
                    KRAFT
                </a>
            """.trimIndent()
        )

        ui.info.message {
            +"Sometimes you might want to add custom css class. You can use "
            ui.basic.label { +"with(...)" }
            +" to achieve this"
        }

        kotlinToHtml(
            kotlin = """
                no.basic.with("my-custom-class").item A {
                    href = "https://github.com/PeekAndPoke/kraft"
                    +"KRAFT"
                }
            """.trimIndent(),
            html = """
                <a class="basic my-custom-class item" href="https://github.com/PeekAndPoke/kraft">
                    KRAFT
                </a>
            """.trimIndent()
        )

        ui.info.message {
            +"Sometimes you only want to set a css class when a condition is met. You can use "
            ui.basic.label { +"given(...) { ... }" }
            +" to achieve this."
        }

        kotlinToHtml(
            kotlin = """
                val myCondition = false
                
                no.basic.given(myCondition) { disabled }.item A {
                    href = "https://github.com/PeekAndPoke/kraft"
                    +"KRAFT"
                }
            """.trimIndent(),
            html = """
                <a class="basic item" href="https://github.com/PeekAndPoke/kraft">
                    KRAFT
                </a>
            """.trimIndent()
        )

        ui.info.message {
            +"Sometimes you only want to set a css class when a condition is NOT met. You can use "
            ui.basic.label { +"givenNot(...) { ... }" }
            +" to achieve this."
        }

        kotlinToHtml(
            kotlin = """
                val myCondition = false
                
                no.basic.givenNot(myCondition) { disabled }.item A {
                    href = "https://github.com/PeekAndPoke/kraft"
                    +"KRAFT"
                }
            """.trimIndent(),
            html = """
                <a class="basic disabled item" href="https://github.com/PeekAndPoke/kraft">
                    KRAFT
                </a>
            """.trimIndent()
        )
    }
}
