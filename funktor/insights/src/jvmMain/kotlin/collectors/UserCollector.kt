package de.peekandpoke.funktor.insights.collectors

import de.peekandpoke.funktor.insights.InsightsCollector
import de.peekandpoke.funktor.insights.InsightsCollectorData
import de.peekandpoke.funktor.insights.gui.InsightsBarTemplate
import de.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.security.user.UserProvider
import de.peekandpoke.ultra.security.user.UserRecord
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import io.ktor.server.application.*
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
