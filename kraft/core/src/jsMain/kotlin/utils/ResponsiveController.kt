package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

val Component<*>.responsiveCtrl: ResponsiveController get() = getAttributeRecursive(ResponsiveController.key)

class ResponsiveController(
    private val breakpoints: Breakpoints = Breakpoints.default,
) : Stream<ResponsiveController.State> {

    companion object {
        val key = TypedKey<ResponsiveController>("ResponsiveCtrl")
    }

    enum class DisplayType {
        Desktop,
        Tablet,
        Mobile;

        val isDesktop get() = this == Desktop
        val isNotDesktop = !isDesktop

        val isMobile get() = this == Mobile
        val isNotMobile = !isMobile
    }

    data class Breakpoints(
        val tablet: Int,
        val desktop: Int,
    ) {
        companion object {
            val default = Breakpoints(tablet = 768, desktop = 1200)
        }
    }

    data class State(
        val windowSize: Vector2D,
        val displayType: DisplayType,
    ) {
        val isDesktop = displayType.isDesktop
        val isNotDesktop = !isDesktop

        val isMobile = displayType.isMobile
        val isNotMobile = !isMobile
    }

    private val stream = StreamSource(createState())

    override fun invoke(): State = stream()

    override fun subscribeToStream(sub: (State) -> Unit): Unsubscribe = stream.subscribeToStream(sub)

    init {
        window.addEventListener("resize", ::onWindowResize)
    }

    private fun createState(): State {
        val w = document.body?.clientWidth ?: 1200
        val h = document.body?.clientHeight ?: 768

        return State(
            windowSize = Vector2D(w.toDouble(), h.toDouble()),
            displayType = when {
                w >= breakpoints.desktop -> DisplayType.Desktop
                w >= breakpoints.tablet -> DisplayType.Tablet
                else -> DisplayType.Mobile
            }
        )
    }

    private fun onWindowResize(@Suppress("UNUSED_PARAMETER") evt: Event) {
        stream.modify {
            createState()
        }
    }
}
