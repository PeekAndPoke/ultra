package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.addons.pagination.pagedSearchFilter
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
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

    private var filter by pagedSearchFilter {
        console.log(it)
        reload()
    }

    private var reloads by value(0)

    private val loader = dataLoader {
        flow {
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
