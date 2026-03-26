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

/** Retrieves the [ResponsiveController] from the component's attribute hierarchy. */
val Component<*>.responsiveCtrl: ResponsiveController get() = getAttributeRecursive(ResponsiveController.key)

/**
 * Tracks the browser window size and determines the current [DisplayType] based on configurable [Breakpoints].
 *
 * Emits a new [State] whenever the window is resized.
 */
class ResponsiveController(
    private val breakpoints: Breakpoints = Breakpoints.default,
) : Stream<ResponsiveController.State> {

    companion object {
        val key = TypedKey<ResponsiveController>("ResponsiveCtrl")
    }

    /** Categorizes the viewport width into desktop, tablet, or mobile. */
    enum class DisplayType {
        Desktop,
        Tablet,
        Mobile;

        /** True when the display type is [Desktop]. */
        val isDesktop get() = this == Desktop
        val isNotDesktop = !isDesktop

        /** True when the display type is [Mobile]. */
        val isMobile get() = this == Mobile
        val isNotMobile = !isMobile
    }

    /**
     * Width breakpoints for determining the [DisplayType].
     *
     * @param tablet minimum width in pixels for the tablet breakpoint
     * @param desktop minimum width in pixels for the desktop breakpoint
     */
    data class Breakpoints(
        val tablet: Int,
        val desktop: Int,
    ) {
        companion object {
            val default = Breakpoints(tablet = 768, desktop = 1200)
        }
    }

    /**
     * Snapshot of the current window dimensions and resolved [DisplayType].
     */
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

    /** Returns the current [State]. */
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
