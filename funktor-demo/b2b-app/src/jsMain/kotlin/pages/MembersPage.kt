package io.peekandpoke.funktor.demo.b2bapp.pages

import io.peekandpoke.funktor.demo.b2bapp.Apis
import io.peekandpoke.funktor.demo.b2bapp.State
import io.peekandpoke.funktor.demo.common.b2b.AddMemberRequest
import io.peekandpoke.funktor.demo.common.b2b.ChangeMemberRolesRequest
import io.peekandpoke.funktor.demo.common.b2b.OrgMemberModel
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.modals.ModalsManager.Companion.modals
import io.peekandpoke.kraft.semanticui.modals.OkCancelModal.Companion.small
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.HttpStatusCode
import io.peekandpoke.ultra.security.user.OrgRole
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.canManageOrgMembers
import io.peekandpoke.ultra.security.user.isOrgOwner
import io.peekandpoke.ultra.security.user.wouldRemoveLastOwner
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.TBODY
import kotlinx.html.Tag
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

/** Human-readable label for a role id — shared by the list and the role-editor modal (same package). */
internal fun roleDisplayName(role: String): String = when (role) {
    OrgRole.OWNER -> "Owner"
    OrgRole.ADMIN -> "Admin"
    "member" -> "Member"
    else -> role
}

@Suppress("FunctionName")
fun Tag.MembersPage() = comp {
    MembersPage(it)
}

/**
 * The b2b tenant's own member-management page: lists the selected org's members and lets owners/admins
 * change roles and remove members via [Apis.members] (the `/api/b2b/orgs/{org}/members` endpoints).
 *
 * The org is the selected session org ([UserPermissions.org]); the caller's role in that org
 * ([UserPermissions.roles]) drives which controls are shown. All of this is a convenience layer — the
 * server independently enforces the floor, org isolation, owner-only ownership, and the last-owner
 * invariant, and any UI slip is rejected there (surfaced here as a toast).
 */
class MembersPage(ctx: NoProps) : PureComponent(ctx) {

    private val auth by subscribingTo(State.auth)

    private val orgKey: String? get() = auth.permissions.org
    private val callerRoles: Set<String> get() = auth.permissions.roles
    private val callerCanManage: Boolean get() = callerRoles.canManageOrgMembers
    private val callerIsOwner: Boolean get() = callerRoles.isOrgOwner
    private val myUserId: UserId? get() = auth.user?.id

    /** The roles this page lets managers toggle; other (app-defined) roles a member holds are preserved. */
    private val editableRoles = listOf(OrgRole.OWNER, OrgRole.ADMIN, "member")

    private val loader = dataLoader {
        when (val org = orgKey) {
            null -> flowOf(emptyList())
            else -> Apis.members.list(org).map { it.data ?: emptyList() }
        }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.users()
                noui.content { +"Members" }
            }

            when (val org = orgKey) {
                null -> ui.warning.message { +"No organisation is selected for this session." }
                else -> ui.info.message { +"Organisation: $org" }
            }

            if (callerCanManage && orgKey != null) {
                ui.green.button {
                    onClick { openAddModal() }
                    icon.user_plus()
                    +"Add member"
                }
            }
        }

        // Nothing to load without a selected org (an org-less session cannot manage members).
        orgKey ?: return

        loader.renderDefault(this) { members ->
            renderTable(members)
        }
    }

    private fun FlowContent.renderTable(members: List<OrgMemberModel>) {
        // The org's owner-id set from the LOADED list — drives a client-side last-owner guard that
        // mirrors the server's `wouldRemoveLastOwner`. NOTE: the b2b list is realm-scoped, so in the
        // rare anomaly where the only VISIBLE owner is backed by a cross-realm (b2b2c) co-owner this
        // guard over-restricts (fail-safe); the server stays authoritative either way.
        val ownerIds = members.filter { it.roles.isOrgOwner }.map { it.userId }.toSet()

        ui.segment {
            if (members.isEmpty()) {
                ui.message { +"This organisation has no members yet." }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Name" }
                            th { +"Email" }
                            th { +"Roles" }
                            if (callerCanManage) th { +"Actions" }
                        }
                    }
                    tbody {
                        members.forEach { member -> renderRow(member, ownerIds) }
                    }
                }
            }
        }
    }

    private fun TBODY.renderRow(member: OrgMemberModel, ownerIds: Set<UserId>) {
        val isSelf = member.userId == myUserId
        val targetIsOwner = member.roles.isOrgOwner
        // The server only lets an owner touch an owner (grant/revoke ownership). Mirror that gate here.
        val canManageThis = callerCanManage && (callerIsOwner || !targetIsOwner)
        // True only when this member is the org's SOLE (visible) owner — removing/demoting them would
        // orphan the org, which the server rejects (400). Prevent it up front; keep the server as the
        // authority for the two-owners-race (both clients see 2 owners → both proceed → first wins).
        val isLastOwner = wouldRemoveLastOwner(ownerIds, member.userId)

        tr {
            td {
                +member.name
                if (isSelf) {
                    +" "
                    ui.mini.basic.label { +"You" }
                }
            }
            td { +member.email }
            td { renderRoleLabels(member.roles) }

            if (callerCanManage) {
                td {
                    ui.small.givenNot(canManageThis) { disabled }.blue.button {
                        onClick { if (canManageThis) openRolesModal(member, isLastOwner) }
                        icon.user_cog()
                        +"Roles"
                    }
                    ui.small.givenNot(canManageThis) { disabled }.red.button {
                        onClick { if (canManageThis) confirmRemove(member, isLastOwner) }
                        icon.trash()
                        +"Remove"
                    }
                }
            }
        }
    }

    private fun FlowContent.renderRoleLabels(roles: Set<String>) {
        if (roles.isEmpty()) {
            ui.mini.label { +"(no roles)" }
            return
        }

        // Structural roles first (owner, admin, member), then any remaining app-defined roles.
        val ordered = editableRoles.filter { it in roles } + roles.filterNot { it in editableRoles }.sorted()

        ordered.forEach { role ->
            when (role) {
                OrgRole.OWNER -> ui.mini.yellow.label { icon.star(); +"Owner" }
                OrgRole.ADMIN -> ui.mini.blue.label { +"Admin" }
                else -> ui.mini.label { +roleDisplayName(role) }
            }
        }
    }

    //  ACTIONS  //////////////////////////////////////////////////////////////////////////////////////////////////

    private fun openAddModal() {
        modals.show { handle ->
            AddMemberModal(
                handle = handle,
                editableRoles = editableRoles,
                callerIsOwner = callerIsOwner,
                onAdd = { email, roles -> addMember(email, roles) },
            )
        }
    }

    private fun addMember(email: String, roles: Set<String>) {
        val org = orgKey ?: return
        launch {
            val response = Apis.members.add(org, AddMemberRequest(email = email, roles = roles)).first()

            handleMutationResult(response, "Added $email")
        }
    }

    private fun openRolesModal(member: OrgMemberModel, isLastOwner: Boolean) {
        modals.show { handle ->
            MemberRolesModal(
                handle = handle,
                member = member,
                editableRoles = editableRoles,
                callerIsOwner = callerIsOwner,
                isLastOwner = isLastOwner,
                onSave = { newRoles -> saveRoles(member, newRoles) },
            )
        }
    }

    private fun confirmRemove(member: OrgMemberModel, isLastOwner: Boolean) {
        if (isLastOwner) {
            // The sole owner can't be removed (an org must keep an owner). Explain up front; the server
            // enforces this too (400) — this just avoids the round-trip and a confusing server error.
            toasts.error("An organisation must keep at least one owner")
            return
        }
        modals.show { handle ->
            small(
                handle = handle,
                header = { ui.header { +"Remove member" } },
                content = { ui.content { +"Remove ${member.name} (${member.email}) from this organisation?" } },
                okText = { +"Remove" },
                cancelText = { +"Cancel" },
            ) { result ->
                result.ifOk { removeMember(member) }
            }
        }
    }

    private fun saveRoles(member: OrgMemberModel, newRoles: Set<String>) {
        val org = orgKey ?: return
        launch {
            val response = Apis.members
                .changeRoles(org, member.id, ChangeMemberRolesRequest(roles = newRoles))
                .first()

            handleMutationResult(response, "Roles updated for ${member.name}")
        }
    }

    private fun removeMember(member: OrgMemberModel) {
        val org = orgKey ?: return
        launch {
            val response = Apis.members.remove(org, member.id).first()

            handleMutationResult(response, "Removed ${member.name}")
        }
    }

    private fun handleMutationResult(response: ApiResponse<OrgMemberModel>, successText: String) {
        if (response.isSuccess()) {
            toasts.info(successText)
            // Silent reload keeps the current list visible instead of flashing the whole table to a spinner.
            loader.reloadSilently()
        } else {
            toasts.error(response.messages?.firstOrNull()?.text ?: "The action could not be completed")
            // A 404 means the list is stale (the member was removed elsewhere) — refresh to server truth.
            if (response.status == HttpStatusCode.NotFound) {
                loader.reloadSilently()
            }
        }
    }
}
