package io.peekandpoke.funktor.demo.b2bapp.pages

import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.semanticui.modals.FadingModal
import io.peekandpoke.ultra.html.debugId
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.security.user.OrgRole
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

/**
 * A modal for adding an EXISTING b2b user to the organisation by email. Only the [editableRoles] this
 * demo knows about are offered; the `owner` role is disabled unless [callerIsOwner] (mirrors the
 * server's owner-only ownership rule). The server resolves the email to a b2b user — an unknown email
 * is rejected there (404, surfaced as a toast) — and reactivates a previously-removed member.
 *
 * [onAdd] is invoked with the entered email + chosen roles AFTER the modal closes (the parent page
 * performs the API call, toasts the outcome, and reloads).
 */
@Suppress("FunctionName")
fun Tag.AddMemberModal(
    handle: ModalsManager.Handle,
    editableRoles: List<String>,
    callerIsOwner: Boolean,
    onAdd: (email: String, roles: Set<String>) -> Unit,
) = comp(
    AddMemberModal.Props(
        handle = handle,
        editableRoles = editableRoles,
        callerIsOwner = callerIsOwner,
        onAdd = onAdd,
    )
) {
    AddMemberModal(it)
}

class AddMemberModal(ctx: Ctx<Props>) : FadingModal<AddMemberModal.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    class Props(
        override val handle: ModalsManager.Handle,
        val editableRoles: List<String>,
        val callerIsOwner: Boolean,
        val onAdd: (email: String, roles: Set<String>) -> Unit,
    ) : FadingModal.Props()

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var email: String by value("")
    private var roles: Set<String> by value(emptySet())

    private val canAdd: Boolean get() = email.isNotBlank()

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun FlowContent.renderContent() {
        ui.small.modal.transition.visible.active.front {
            debugId("add-member-modal")

            ui.header {
                icon.user_plus()
                +"Add member"
            }

            ui.content {
                ui.form {
                    UiInputField(email, { email = it }) {
                        label("Email of an existing b2b user")
                        placeholder("user@example.com")
                    }

                    props.editableRoles.forEach { role ->
                        val ownerLocked = role == OrgRole.OWNER && !props.callerIsOwner
                        roleField(
                            role = role,
                            checked = role in roles,
                            locked = ownerLocked,
                            lockedHint = "owner-only",
                            onToggle = { roles = if (role in roles) roles - role else roles + role },
                        )
                    }
                }

                if (!props.callerIsOwner) {
                    ui.info.message { +"Only an owner can grant the owner role." }
                }
            }

            noui.actions {
                ui.button {
                    debugId("cancel-button")
                    onClick { close() }
                    +"Cancel"
                }
                ui.primary.givenNot(canAdd) { disabled }.button {
                    debugId("add-button")
                    onClick { if (canAdd) close { props.onAdd(email.trim(), roles) } }
                    icon.plus()
                    +"Add"
                }
            }
        }
    }
}
