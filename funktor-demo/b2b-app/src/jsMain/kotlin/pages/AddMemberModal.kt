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
 * The modal answers with the entered email + chosen roles ([AddMemberModal.Result]) after it closes, or
 * `null` when cancelled (the parent page performs the API call, toasts the outcome, and reloads).
 */
@Suppress("FunctionName")
fun Tag.AddMemberModal(
    handle: ModalsManager.Handle<AddMemberModal.Result>,
    editableRoles: List<String>,
    callerIsOwner: Boolean,
) = comp(
    AddMemberModal.Props(
        handle = handle,
        editableRoles = editableRoles,
        callerIsOwner = callerIsOwner,
    )
) {
    AddMemberModal(it)
}

class AddMemberModal(ctx: Ctx<Props>) : FadingModal<AddMemberModal.Props, AddMemberModal.Result>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    class Props(
        override val handle: ModalsManager.Handle<Result>,
        val editableRoles: List<String>,
        val callerIsOwner: Boolean,
    ) : FadingModal.Props<Result>()

    /** The answer: the member to add. */
    data class Result(val email: String, val roles: Set<String>)

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
                    onClick { if (canAdd) close(Result(email = email.trim(), roles = roles)) }
                    icon.plus()
                    +"Add"
                }
            }
        }
    }
}
