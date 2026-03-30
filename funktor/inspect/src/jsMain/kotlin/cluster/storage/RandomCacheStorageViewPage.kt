package io.peekandpoke.funktor.inspect.cluster.storage

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
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
import kotlinx.html.pre

class RandomCacheStorageViewPage(ctx: Ctx<Props>) : Component<RandomCacheStorageViewPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorInspectClusterUi,
        val id: String,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var data: RawCacheDataModel? by value(null)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        launch {
            props.ui.api.storage.randomCache
                .get(props.id)
                .collect {
                    data = it.data!!
                }
        }
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Random Cache", props.id) }

        ui.padded.segment {

            ui.dividing.header H2 { +"Random Cache '${props.id}'" }

            data?.let {

                ui.cards {
                    ui.card {
                        noui.content {
                            noui.header { +"Category" }
                            noui.description { +it.category }
                        }
                    }

                    ui.card {
                        noui.content {
                            noui.header { +"Data Id" }
                            noui.description { +it.dataId }
                        }
                    }

                    ui.card {
                        noui.content {
                            noui.header { +"Policy" }
                            noui.description {
                                it.asHead.renderPolicyAsList(this)
                            }
                        }
                    }

                    ui.card {
                        noui.content {
                            it.renderTimestampsAsList(this)
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
