package io.peekandpoke.funktor.inspect.introspection

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui

class IntrospectionOverviewPage(ctx: Ctx<Props>) : Component<IntrospectionOverviewPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: IntrospectionUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Introspection") }

        ui.padded.segment {
            ui.header H2 { +"Introspection" }

            ui.four.doubling.stackable.cards {
                ui.card A {
                    href(props.ui.routes.lifecycleHooks())
                    ui.center.aligned.content { icon.large.heartbeat() }
                    ui.center.aligned.content { noui.header { +"Lifecycle Hooks" } }
                }

                ui.card A {
                    href(props.ui.routes.configInfo())
                    ui.center.aligned.content { icon.large.cog() }
                    ui.center.aligned.content { noui.header { +"Config Info" } }
                }

                ui.card A {
                    href(props.ui.routes.cliCommands())
                    ui.center.aligned.content { icon.large.terminal() }
                    ui.center.aligned.content { noui.header { +"CLI Commands" } }
                }

                ui.card A {
                    href(props.ui.routes.fixtures())
                    ui.center.aligned.content { icon.large.puzzle_piece() }
                    ui.center.aligned.content { noui.header { +"Fixtures" } }
                }

                ui.card A {
                    href(props.ui.routes.repairs())
                    ui.center.aligned.content { icon.large.wrench() }
                    ui.center.aligned.content { noui.header { +"Repairs" } }
                }

                ui.card A {
                    href(props.ui.routes.endpoints())
                    ui.center.aligned.content { icon.large.sitemap() }
                    ui.center.aligned.content { noui.header { +"Endpoints" } }
                }

                ui.card A {
                    href(props.ui.routes.authRealms())
                    ui.center.aligned.content { icon.large.shield_alternate() }
                    ui.center.aligned.content { noui.header { +"Auth Realms" } }
                }

                ui.card A {
                    href(props.ui.routes.systemInfo())
                    ui.center.aligned.content { icon.large.server() }
                    ui.center.aligned.content { noui.header { +"System Info" } }
                }
            }
        }
    }
}
