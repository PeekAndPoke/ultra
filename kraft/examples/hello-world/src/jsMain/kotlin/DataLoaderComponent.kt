package de.peekandpoke.kraft.examples.helloworld

import de.peekandpoke.kraft.addons.pagination.pagedSearchFilter
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.DataLoaderComponent(start: Int) = comp(
    DataLoaderComponent.Props(start = start)
) {
    DataLoaderComponent(it)
}

class DataLoaderComponent(ctx: Ctx<Props>) : Component<DataLoaderComponent.Props>(ctx) {

    data class Props(
        val start: Int,
    )

    private var filter by pagedSearchFilter(router) {
        console.log(it)
        reload()
    }

    private var reloads by value(0)

    private val loader = dataLoader {
        flow<Int> {
            delay(1_000)
            emit(props.start + reloads)
        }
    }

    private fun reload() {
        loader.reloadSilently()
    }

    override fun VDom.render() {
        ui.segment {
            div {
                loader(this) {
                    loading {
                        +"Loading ..."
                    }
                    error {
                        +"Error... ${it.message}"
                    }
                    loaded { data ->
                        +"Loaded: $data | Reloads: $reloads"
                    }
                }
            }

            div {
                ui.blue.button {
                    onClick {
                        loader.reload()
                    }
                    +"Reload"
                }

                ui.orange.button {
                    onClick {
                        filter = filter.copy(epp = filter.epp + 1)
                    }
                    +"Change uri"
                }
            }
        }
    }
}
