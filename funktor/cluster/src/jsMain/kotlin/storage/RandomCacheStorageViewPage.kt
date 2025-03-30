package de.peekandpoke.ktorfx.cluster.storage

import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ktorfx.cluster.KtorFxClusterUi
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
        val ui: KtorFxClusterUi,
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
        JoinedPageTitle { listOf("KtorFx", "Random Cache", props.id) }

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
