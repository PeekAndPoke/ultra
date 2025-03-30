package de.peekandpoke.kraft.addons.semanticui.modals

import de.peekandpoke.kraft.addons.modal.ModalsManager
import de.peekandpoke.kraft.addons.routing.BackNavigationTrap
import de.peekandpoke.kraft.addons.routing.BackNavigationTrap.Companion.trapBackNavigation
import de.peekandpoke.kraft.addons.styling.StyleSheet
import de.peekandpoke.kraft.addons.styling.StyleSheets
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.css.Overflow
import kotlinx.css.overflowX
import kotlinx.css.overflowY
import kotlinx.html.FlowContent
import kotlinx.html.style

abstract class FadingModal<P : FadingModal.Props>(ctx: Ctx<P>) : Component<P>(ctx) {

    companion object {

        object Style : StyleSheet() {
            val noScroll = rule {
                overflowX = Overflow.hidden
                overflowY = Overflow.hidden
            }
        }

        init {
            StyleSheets.mount(Style)
        }
    }

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    abstract class Props {
        abstract val handle: ModalsManager.Handle
        open val transition: Transition = Transition()
    }

    data class Transition(
        val fadeInTimeMs: Int = 500,
        val fadeOutTimeMs: Int = 500,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var fadingIn by value(true)
    private var fadingOut by value(false)

    private val navTrap = trapBackNavigation {
        close()

        BackNavigationTrap.TrapResult.Continue
    }


    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                window.document.body?.classList?.add(Style.noScroll)

                launch {
                    delay(props.transition.fadeInTimeMs.toLong())
                    fadingIn = false
                }
            }
        }
    }

    abstract fun FlowContent.renderContent()

    final override fun VDom.render() {

        ui.dimmer.modals.page.transition.visible.active
            .given(fadingIn) { animating.fade._in }
            .given(fadingOut) { animating.fade.out }.then {
                style = "display: flex !important;"
                onClick {
                    if (it.target == it.currentTarget) {
                        close()
                    } else {
                        it.stopPropagation()
                    }
                }

                renderContent()
            }
    }

    open fun close(onClose: (suspend () -> Unit)? = null) {
        doClose(onClose)
    }

    private fun doClose(onClose: (suspend () -> Unit)? = null) {
        navTrap.deactivate()

        fadeOut()

        onClose?.let {
            launch {
                delay(150)
                onClose()
            }
        }
    }

    private fun fadeOut() {
        if (!fadingOut) {
            // Make the body no longer scrollable as long as the popup is shown
            window.document.body?.classList?.remove(Style.noScroll)

            fadingOut = true

            launch {
                delay(props.transition.fadeOutTimeMs.toLong())
                props.handle.close()
            }
        }
    }
}
