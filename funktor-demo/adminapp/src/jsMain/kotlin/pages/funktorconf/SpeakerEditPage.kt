package io.peekandpoke.funktor.demo.adminapp.pages.funktorconf

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.common.funktorconf.SaveSpeakerRequest
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.forms.validation.strings.notBlank
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.semanticui.forms.UiTextArea
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.utils.launch
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
fun Tag.SpeakerEditPage(
    id: String?,
) = comp(
    SpeakerEditPage.Props(id = id)
) {
    SpeakerEditPage(it)
}

class SpeakerEditPage(ctx: Ctx<Props>) : Component<SpeakerEditPage.Props>(ctx) {

    //  PROPS  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val id: String?,
    )

    //  STATE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val speakerId: String get() = props.id ?: "_new_"
    private val isNew: Boolean get() = speakerId == "_new_"

    data class Draft(
        val name: String = "",
        val bio: String = "",
        val photoUrl: String = "",
        val talkTitle: String = "",
        val talkAbstract: String = "",
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
            Apis.funktorConf.getSpeaker(speakerId).map { it.data!! }.map {
                State(original = Draft(it.name, it.bio, it.photoUrl, it.talkTitle, it.talkAbstract))
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
                icon.microphone()
                noui.content {
                    +(if (isNew) "New Speaker" else "Edit Speaker")
                }
            }

            ui.button {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.speakers()) }
                icon.arrow_left()
                +"Back to Speakers"
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

                UiTextArea(draft.bio, { modifyDraft { copy(bio = it) } }) {
                    label("Bio")
                    accepts(notBlank())
                }

                UiInputField(draft.photoUrl, { modifyDraft { copy(photoUrl = it) } }) {
                    label("Photo URL")
                }

                UiInputField(draft.talkTitle, { modifyDraft { copy(talkTitle = it) } }) {
                    label("Talk Title")
                    accepts(notBlank())
                }

                UiTextArea(draft.talkAbstract, { modifyDraft { copy(talkAbstract = it) } }) {
                    label("Talk Abstract")
                    accepts(notBlank())
                }
            }

            ui.divider {}

            val canSave = formCtrl.isValid && noDblClick.canRun && state.isChanged

            ui.primary.givenNot(canSave) { disabled }
                .givenNot(noDblClick.canRun) { loading }
                .button {
                    onClick {
                        if (formCtrl.validate()) {
                            launch { saveSpeaker(draft) }
                        }
                    }

                    +"Save"
                }
        }
    }

    private suspend fun saveSpeaker(draft: Draft) = noDblClick.runBlocking {
        val request = SaveSpeakerRequest(
            name = draft.name,
            bio = draft.bio,
            photoUrl = draft.photoUrl,
            talkTitle = draft.talkTitle,
            talkAbstract = draft.talkAbstract,
        )

        try {
            val response = if (isNew) {
                Apis.funktorConf.createSpeaker(request).first()
            } else {
                Apis.funktorConf.updateSpeaker(speakerId, request).first()
            }

            if (response.isSuccess()) {
                toasts.info("Speaker saved successfully")
                router.navToUri(Nav.funktorConf.speakers())
            } else {
                toasts.error("Failed to save speaker")
            }
        } catch (e: Exception) {
            toasts.error("Failed to save speaker: ${e.message}")
        }
    }
}
