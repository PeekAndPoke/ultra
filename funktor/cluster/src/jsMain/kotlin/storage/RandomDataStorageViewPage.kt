package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import kotlinx.css.Color
import kotlinx.css.Overflow
import kotlinx.css.Padding
import kotlinx.css.backgroundColor
import kotlinx.css.overflow
import kotlinx.css.padding
import kotlinx.css.px
import kotlinx.html.InputType
import kotlinx.html.pre

class RandomDataStorageViewPage(ctx: Ctx<Props>) : Component<RandomDataStorageViewPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
        val id: String,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var data: RawRandomDataModel? by value(null)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        launch {
            props.ui.api.storage.randomData
                .get(props.id)
                .collect {
                    data = it.data!!
                }
        }
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Random Storage", props.id) }

        ui.padded.segment {

            ui.dividing.header H2 { +"Random Data '${props.id}'" }

            data?.let {

                ui.cards {
                    ui.card {
                        noui.content {
                            noui.header { +"Category" }
                        }
                        noui.content {
                            +it.category
                        }
                    }

                    ui.card {
                        noui.content {
                            noui.header { +"Data Id" }
                        }
                        noui.content {
                            +it.dataId
                        }
                    }

                    ui.card {
                        noui.content {
                            noui.header { +"Created At" }
                        }
                        noui.content {
                            +it.createdAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                        }
                    }

                    ui.card {
                        noui.content {
                            noui.header { +"Updated At" }
                        }
                        noui.content {
                            +"${InputType.date}"
                        }
                    }
                }

                ui.divider {}

                pre {
                    css {
                        padding = Padding(5.px)
                        backgroundColor = Color("#F0F0F0")
                        overflow = Overflow.auto
                    }

                    +it.data
                }
            }
        }
    }
}
