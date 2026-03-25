@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormDemosPage() = comp {
    FormDemosPage(it)
}

class FormDemosPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("How To | Forms | Demo")

        ui.basic.padded.segment {
            ui.dividing.header H1 { +"Form demos" }

            ui.segment {
                ui.header H2 { +"A form with primitive values" }
                FormWithPrimitives()
            }

            ui.segment {
                ui.header H2 { +"A form with nullable primitive values" }
                FormWithNullablePrimitives()
            }

            ui.segment {
                ui.header H2 { +"A form with checkboxes" }
                FormWithCheckboxes()
            }

            ui.segment {
                ui.header H2 { +"A form with a text area" }
                FormWithTestArea()
            }

            ui.segment {
                ui.header H2 { +"A form with passwords" }
                FormWithPasswords()
            }

            ui.segment {
                ui.header H2 { +"A form with Dates " }
                FormWithDates()
            }

            ui.segment {
                ui.header H2 { +"A form with nullable Dates" }
                FormWithNullableDates()
            }

            ui.segment {
                ui.header H2 { +"A form with DateTimes" }
                FormWithDateTimes()
            }

            ui.segment {
                ui.header H2 { +"A form with nullable DateTimes" }
                FormWithNullableDateTimes()
            }

            ui.segment {
                ui.header H2 { +"A form with Times" }
                FormWithTimes()
            }

            ui.segment {
                ui.header H2 { +"A form with Colors" }
                FormWithColors()
            }

            ui.segment {
                ui.header H2 { +"A form with disabled fields" }
                FormWithDisabledFields()
            }
        }
    }
}
