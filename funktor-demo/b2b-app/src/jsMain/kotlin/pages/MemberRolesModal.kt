package io.peekandpoke.funktor.demo.b2bapp.pages

import io.peekandpoke.funktor.demo.common.b2b.OrgMemberModel
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.kraft.semanticui.modals.FadingModal
import io.peekandpoke.ultra.html.debugId
import io.peekandpoke.ultra.html.onChange
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.security.user.OrgRole
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.input
import kotlinx.html.label
import org.w3c.dom.HTMLInputElement

/**
 * A modal for editing an [OrgMemberModel]'s roles. Only the [editableRoles] this demo knows about are
 * shown as checkboxes; any other (app-defined) role the member already holds is preserved untouched.
 *
 * The `owner` checkbox is disabled when the caller is not an owner ([callerIsOwner] — mirroring the
 * server's owner-only ownership rule) OR when this member is the org's only owner ([isLastOwner] — it
 * can't be demoted, or the org would have no owner). Both gates are a convenience; the server is
 * authoritative and still rejects an illegal ownership change (403) or a last-owner demotion (400).
 *
 * [onSave] is invoked with the chosen role set AFTER the modal closes (the parent page performs the
 * API call, toasts the outcome, and reloads).
 */
@Suppress("FunctionName")
fun Tag.MemberRolesModal(
    handle: ModalsManager.Handle,
    member: OrgMemberModel,
    editableRoles: List<String>,
    callerIsOwner: Boolean,
    isLastOwner: Boolean,
    onSave: (Set<String>) -> Unit,
) = comp(
    MemberRolesModal.Props(
        handle = handle,
        member = member,
        editableRoles = editableRoles,
        callerIsOwner = callerIsOwner,
        isLastOwner = isLastOwner,
        onSave = onSave,
    )
) {
    MemberRolesModal(it)
}

class MemberRolesModal(ctx: Ctx<Props>) : FadingModal<MemberRolesModal.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    class Props(
        override val handle: ModalsManager.Handle,
        val member: OrgMemberModel,
        val editableRoles: List<String>,
        val callerIsOwner: Boolean,
        val isLastOwner: Boolean,
        val onSave: (Set<String>) -> Unit,
    ) : FadingModal.Props()

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    /** The working role set — starts from the member's current roles and is edited via the checkboxes. */
    private var draft: Set<String> by value(props.member.roles)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun FlowContent.renderContent() {
        ui.small.modal.transition.visible.active.front {
            debugId("member-roles-modal")

            ui.header {
                icon.user_cog()
                +"Edit roles — ${props.member.name}"
            }

            ui.content {
                ui.form {
                    props.editableRoles.forEach { role -> renderRoleField(role) }
                }

                when {
                    props.isLastOwner ->
                        ui.info.message { +"This is the organisation's only owner — the owner role can't be removed here." }

                    !props.callerIsOwner ->
                        ui.info.message { +"Only an owner can grant or revoke the owner role." }
                }
            }

            noui.actions {
                ui.button {
                    debugId("cancel-button")
                    onClick { close() }
                    +"Cancel"
                }
                ui.primary.button {
                    debugId("save-button")
                    onClick { close { props.onSave(draft) } }
                    icon.check()
                    +"Save"
                }
            }
        }
    }

    private fun FlowContent.renderRoleField(role: String) {
        val isOwnerRole = role == OrgRole.OWNER
        // The owner role is locked when the caller isn't an owner (can't grant/revoke it) or when this
        // member is the org's last owner (can't be demoted) — mirrors the server's owner gates.
        val ownerLocked = isOwnerRole && (!props.callerIsOwner || props.isLastOwner)

        ui.field {
            ui.given(ownerLocked) { disabled }.checkbox {
                input {
                    type = InputType.checkBox
                    checked = role in draft

                    if (ownerLocked) {
                        disabled = true
                    } else {
                        onChange { evt ->
                            val on = (evt.target as HTMLInputElement).checked
                            draft = if (on) draft + role else draft - role
                        }
                    }
                }
                label {
                    // Semantic UI's native input only overlays the ~17px box glyph, so make the text
                    // label toggle the role too (the app's checkbox idiom) — except when owner-locked.
                    if (!ownerLocked) {
                        onClick { draft = if (role in draft) draft - role else draft + role }
                    }
                    +roleDisplayName(role)
                    if (ownerLocked) {
                        +(if (props.isLastOwner) " (the only owner)" else " (owner-only)")
                    }
                }
            }
        }
    }
}
