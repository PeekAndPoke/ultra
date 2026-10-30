package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.MediaQueryList
import org.w3c.dom.events.Event

/** Retrieves the [ResponsiveController] from the component's attribute hierarchy. */
val Component<*>.responsiveCtrl: ResponsiveController get() = getAttributeRecursive(ResponsiveController.key)

/**
 * Tracks the viewport's size and [Orientation], and determines the current [DisplayType] with the [Breakpoints] of
 * that orientation (ULTRA-00276).
 *
 * Emits a new [State] when the viewport is resized or rotated and the state changed. One per app; [dispose] (called by
 * [KraftApp.dispose]) removes its listeners.
 */
class ResponsiveController(
    /** The breakpoints while the viewport is at least as high as it is wide. */
    private val portrait: Breakpoints = Breakpoints.portrait,
    /** The breakpoints while the viewport is wider than it is high: a phone held sideways stays [DisplayType.Mobile]. */
    private val landscape: Breakpoints = Breakpoints.landscape,
    /** Measures the viewport's width and height; a test passes its own. */
    private val measure: () -> Vector2D = {
        val root = document.documentElement
        Vector2D((root?.clientWidth ?: 1200).toDouble(), (root?.clientHeight ?: 768).toDouble())
    },
) : Stream<ResponsiveController.State>, KraftApp.Disposable {

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

    /** The viewport's orientation, as the CSS media query `orientation` has it: portrait when height ≥ width. */
    enum class Orientation {
        Portrait,
        Landscape;

        val isPortrait get() = this == Portrait
        val isLandscape get() = this == Landscape
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
            val portrait = Breakpoints(tablet = 768, desktop = 1200)
            val landscape = Breakpoints(tablet = 960, desktop = 1200)
        }
    }

    /**
     * Snapshot of the current viewport dimensions, its [Orientation] and the resolved [DisplayType].
     */
    data class State(
        val windowSize: Vector2D,
        val orientation: Orientation,
        val displayType: DisplayType,
    ) {
        val isDesktop = displayType.isDesktop
        val isNotDesktop = !isDesktop

        val isMobile = displayType.isMobile
        val isNotMobile = !isMobile

        val isPortrait = orientation.isPortrait
        val isLandscape = orientation.isLandscape
    }

    private val stream = StreamSource(createState())

    /** Returns the current [State]. */
    override fun invoke(): State = stream()

    override fun subscribeToStream(sub: (State) -> Unit): Unsubscribe = stream.subscribeToStream(sub)

    // One reference each: removal needs the function object that was added
    private val onResize: (Event) -> Unit = { update() }

    private val onRotate: (Event) -> Unit = {
        update()
        // iOS reports the old size inside the rotation event: measure again on the next frame
        window.cancelAnimationFrame(pendingFrame)
        pendingFrame = window.requestAnimationFrame { update() }
    }

    private var pendingFrame = 0

    private val portraitQuery: MediaQueryList = window.matchMedia("(orientation: portrait)")

    private val screenOrientation: dynamic = window.screen.asDynamic().orientation

    init {
        window.addEventListener("resize", onResize)
        portraitQuery.addEventListener("change", onRotate)

        if (screenOrientation != null) {
            screenOrientation.addEventListener("change", onRotate)
        } else {
            window.addEventListener("orientationchange", onRotate)
        }
    }

    override fun dispose() {
        window.removeEventListener("resize", onResize)
        portraitQuery.removeEventListener("change", onRotate)

        if (screenOrientation != null) {
            screenOrientation.removeEventListener("change", onRotate)
        } else {
            window.removeEventListener("orientationchange", onRotate)
        }

        window.cancelAnimationFrame(pendingFrame)
    }

    private fun createState(): State {
        val size = measure()
        val orientation = if (size.y >= size.x) Orientation.Portrait else Orientation.Landscape
        val breakpoints = if (orientation.isPortrait) portrait else landscape

        return State(
            windowSize = size,
            orientation = orientation,
            displayType = when {
                size.x >= breakpoints.desktop -> DisplayType.Desktop
                size.x >= breakpoints.tablet -> DisplayType.Tablet
                else -> DisplayType.Mobile
            }
        )
    }

    private fun update() {
        val next = createState()

        if (next != stream()) stream(next)
    }
}
