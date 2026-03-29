package io.peekandpoke.funktor.cluster.storage

import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.formatDdMmmYyyyHhMmSs
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
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
