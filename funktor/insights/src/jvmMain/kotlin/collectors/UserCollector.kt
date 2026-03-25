package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.*
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.gui.InsightsBarTemplate
import io.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.security.user.UserRecord
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.title

class UserCollector(
    private val user: UserProvider,
) : InsightsCollector {

    data class Data(
        val user: UserRecord,
        val permissions: UserPermissions,
    ) : InsightsCollectorData {

        override fun renderBar(template: InsightsBarTemplate) = with(template) {

            left {
                ui.item {
                    title = "Current user"
                    icon.user()
                    +user.userId
                }
            }
        }

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            menu {
                icon.user()
                +"User"
            }

            content {
                ui.header H4 { +"User" }
                json(user)

                ui.header H4 { +"Permissions" }
                json(permissions)
            }
        }
    }

    override fun finish(call: ApplicationCall): Data = Data(
        user = user().record,
        permissions = user().permissions,
    )
}
