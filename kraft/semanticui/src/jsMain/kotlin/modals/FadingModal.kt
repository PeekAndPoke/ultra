package io.peekandpoke.kraft.semanticui.modals

import io.peekandpoke.kraft.addons.styling.StyleSheet
import io.peekandpoke.kraft.addons.styling.StyleSheets
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.getAttributeRecursiveOrNull
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.kraft.routing.BackNavigationTrap
import io.peekandpoke.kraft.routing.BackNavigationTrap.Companion.trapBackNavigation
import io.peekandpoke.kraft.routing.Router
import io.peekandpoke.kraft.semanticui.stopClick
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.css.Overflow
import kotlinx.css.overflowX
import kotlinx.css.overflowY
import kotlinx.html.FlowContent
import kotlinx.html.style

/**
 * Abstract base class for Semantic UI modals with fade-in/fade-out transitions, answering with a [RESULT].
 *
 * Subclasses implement [renderContent] and answer through [close] with a result; [close] without one (also the
 * backdrop and back navigation) is a dismissal. The answer reaches the handle's `onResult` after the fade-out.
 * The modal traps back-navigation and disables body scrolling while any FadingModal is open. A navigation to another
 * page (a link inside the modal, a logout) dismisses it; a `replaceUri` (a `urlParam`) does not. Under hash routing a
 * pushed move, also a query-only one, is a Back for the trap and dismisses it too.
 */
abstract class FadingModal<P : FadingModal.Props<RESULT>, RESULT : Any>(ctx: Ctx<P>) : Component<P>(ctx) {

    companion object {

        object Style : StyleSheet() {
            val noScroll by rule {
                overflowX = Overflow.hidden
                overflowY = Overflow.hidden
            }
        }

        /** Open FadingModals; the body class goes when the last one unmounts. */
        private var openCount = 0

        init {
            StyleSheets.mount(Style)
        }
    }

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    /** Base props for [FadingModal] subclasses. */
    abstract class Props<RESULT : Any> {
        abstract val handle: ModalsManager.Handle<RESULT>
        open val transition: Transition = Transition()
    }

    /** Configures fade-in and fade-out animation durations in milliseconds. */
    data class Transition(
        val fadeInTimeMs: Int = 500,
        val fadeOutTimeMs: Int = 500,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var fadingIn by value(true)
    private var fadingOut by value(false)
    private var countedOpen = false

    private val navTrap = trapBackNavigation {
        close()

        BackNavigationTrap.TrapResult.Continue
    }

    /** Ends the watch on the router's page, while mounted. */
    private var unwatchPage: Unsubscribe? = null

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                countedOpen = true
                if (openCount++ == 0) {
                    window.document.body?.classList?.add(Style.noScroll())
                }

                launch {
                    delay(props.transition.fadeInTimeMs.toLong())
                    fadingIn = false
                }

                // A navigation to another page dismisses the modal; a replaceUri (a urlParam) does not. The page is the
                // address's: under hash routing the router follows a written address a task later, and a modal shown
                // with that navigation belongs to the new page. Until the router arrives, the page it is still on is no
                // reason to close
                getAttributeRecursiveOrNull(Router.key)?.let { router ->
                    val page = Router.pageOf(router.strategy.getUriFromWindowLocation())
                    var stillOn = Router.pageOf(router.current().uri)

                    unwatchPage = router.current.subscribeToStream {
                        val now = Router.pageOf(it.uri)

                        when (now) {
                            page -> stillOn = page
                            stillOn -> Unit
                            else -> close()
                        }
                    }
                }
            }

            onUnmount {
                unwatchPage?.invoke()
                unwatchPage = null

                // Also covers an unmount without a fade: closeAll(), a cancelled showAndAwait, handle.close()
                if (countedOpen) {
                    countedOpen = false
                    if (--openCount == 0) {
                        window.document.body?.classList?.remove(Style.noScroll())
                    }
                }
            }
        }
    }

    /** Renders the modal content inside the dimmer overlay. Implemented by subclasses. */
    abstract fun FlowContent.renderContent()

    final override fun VDom.render() {

        ui.dimmer.modals.page.transition.visible.active
            .given(fadingIn) { animating.fade._in }
            .given(fadingOut) { animating.fade.out }.then {
                style = "display: flex !important;"
                onClick {
                    if (it.target == it.currentTarget) close() else stopClick(it)
                }

                renderContent()
            }
    }

    /** Answers [result] and closes with a fade-out. Ignored once the modal is fading out. */
    fun close(result: RESULT) = closeAnswering(result)

    /** Dismisses the modal (it answers `null`) and closes it with a fade-out. Ignored once fading out. */
    fun close() = closeAnswering(null)

    private fun closeAnswering(result: RESULT?) {
        if (fadingOut) return

        // Funnels through the deprecated overload, so subclasses overriding it keep running for one release
        @Suppress("DEPRECATION")
        close(onClose = null)

        // Decide only when the fade started: an override that vetoed the close leaves the answer open
        if (fadingOut) {
            props.handle.resolve(result)
        }
    }

    /** Closes the modal with a fade-out, optionally running [onClose] after a short delay. */
    @Deprecated("Answer through close(result) or close(), and register onResult on the handle.")
    open fun close(onClose: (suspend () -> Unit)? = null) {
        doClose(onClose)
    }

    private fun doClose(onClose: (suspend () -> Unit)? = null) {
        // Idempotent: once the modal is fading out, ignore repeat close() calls. The button stays
        // clickable for the whole fade-out window, so without this a fast double-click would schedule
        // [onClose] twice — double-firing whatever side effect (e.g. a mutation) it carries.
        if (fadingOut) return

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
            fadingOut = true

            launch {
                delay(props.transition.fadeOutTimeMs.toLong())
                props.handle.close()
            }
        }
    }
}
