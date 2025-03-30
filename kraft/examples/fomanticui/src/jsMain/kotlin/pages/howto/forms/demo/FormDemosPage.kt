@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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
