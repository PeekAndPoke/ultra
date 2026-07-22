package io.peekandpoke.funktor.demo.opsapp.pages

import io.peekandpoke.funktor.demo.opsapp.Apis
import io.peekandpoke.funktor.demo.opsapp.Nav
import io.peekandpoke.funktor.saas.api.CreateOrgRequest
import io.peekandpoke.funktor.saas.api.UpdateOrgRequest
import io.peekandpoke.funktor.saas.domain.Slugs
import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.kraft.forms.validation.strings.validSlug
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.forms.validation.strings.notBlank
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.semanticui.forms.old.select.SelectField
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.OrgEditPage(
    id: String?,
) = comp(
    OrgEditPage.Props(id = id)
) {
    OrgEditPage(it)
}

class OrgEditPage(ctx: Ctx<Props>) : Component<OrgEditPage.Props>(ctx) {

    //  PROPS  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val id: String?,
    )

    //  STATE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val orgId: String get() = props.id ?: "_new_"
    private val isNew: Boolean get() = orgId == "_new_"

    data class Draft(
        val slug: String = "",
        val name: String = "",
        val status: OrgStatus = OrgStatus.Active,
    )

    data class State(
        val original: Draft = Draft(),
        val draft: Draft = original,
    ) {
        val isChanged: Boolean get() = draft != original
    }

    private val formCtrl = formController()
    private val noDblClick = doubleClickProtection()

    private val loader = dataLoader {
        if (!isNew) {
            Apis.orgs.get(orgId).map { it.data!! }.map {
                State(original = Draft(slug = it.slug, name = it.name, status = it.status))
            }
        } else {
            flowOf(State())
        }
    }

    private fun modifyDraft(block: Draft.() -> Draft) {
        loader.modifyValue { it.copy(draft = block(it.draft)) }
    }

    //  IMPL  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.sitemap()
                noui.content {
                    +(if (isNew) "New Organisation" else "Edit Organisation")
                }
            }

            ui.button {
                onClick { evt -> router.navToUri(evt, Nav.orgs()) }
                icon.arrow_left()
                +"Back to Organisations"
            }
        }

        loader.renderDefault(this) { data ->
            renderForm(data)
        }
    }

    private fun FlowContent.renderForm(state: State) {
        val draft = state.draft

        ui.segment {
            ui.form {
                UiInputField(draft.name, { modifyDraft { copy(name = it) } }) {
                    label("Name")
                    accepts(notBlank())
                }

                if (isNew) {
                    // Slug is subdomain-safe (validSlug) and shown as its canonical, normalized form.
                    UiInputField(draft.slug, { modifyDraft { copy(slug = Slugs.normalize(it)) } }) {
                        label("Slug")
                        placeholder("e.g. acme-hotels — used as the tenant subdomain")
                        accepts(validSlug())
                    }
                }

                SelectField(draft.status, { modifyDraft { copy(status = it) } }) {
                    label("Status")
                    OrgStatus.entries.forEach { s ->
                        option(s, s.name)
                    }
                }
            }

            ui.divider {}

            val canSave = formCtrl.isValid && noDblClick.canRun && state.isChanged

            ui.primary.givenNot(canSave) { disabled }
                .givenNot(noDblClick.canRun) { loading }
                .button {
                    onClick {
                        formCtrl.validate {
                            saveOrg(draft)
                        }
                    }

                    +"Save"
                }
        }
    }

    private suspend fun saveOrg(draft: Draft) = noDblClick.runBlocking {
        try {
            val response = if (isNew) {
                Apis.orgs.create(
                    CreateOrgRequest(
                        slug = draft.slug,
                        name = draft.name,
                        status = draft.status,
                    )
                ).first()
            } else {
                Apis.orgs.update(
                    orgId,
                    UpdateOrgRequest(
                        name = draft.name,
                        status = draft.status,
                    )
                ).first()
            }

            if (response.isSuccess()) {
                toasts.info("Organisation saved successfully")
                router.navToUri(Nav.orgs())
            } else {
                toasts.error("Failed to save organisation")
            }
        } catch (e: Exception) {
            toasts.error("Failed to save organisation: ${e.message}")
        }
    }
}
