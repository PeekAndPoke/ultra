package io.peekandpoke.kraft.coretests.component

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.utils.ResponsiveController
import io.peekandpoke.kraft.utils.ResponsiveController.DisplayType
import io.peekandpoke.kraft.utils.ResponsiveController.Orientation
import io.peekandpoke.kraft.utils.Vector2D
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.w3c.dom.events.Event

/** Rotates the device: the screen's orientation tells its listeners, or the window does where it has none. */
private fun rotate() {
    val orientation = window.screen.asDynamic().orientation

    if (orientation != null) {
        orientation.dispatchEvent(Event("change"))
    } else {
        window.dispatchEvent(Event("orientationchange"))
    }
}

/** `ResponsiveController`: breakpoints per orientation, rotation, and no emission without a change (ULTRA-00276). */
class ResponsiveControllerSpec : StringSpec({

    "portrait and landscape each resolve the display type with their own breakpoints" {
        var size = Vector2D(400.0, 800.0)
        val ctrl = ResponsiveController(measure = { size })

        fun at(width: Int, height: Int): Pair<Orientation, DisplayType> {
            size = Vector2D(width.toDouble(), height.toDouble())
            window.dispatchEvent(Event("resize"))
            return ctrl().orientation to ctrl().displayType
        }

        try {
            ctrl().orientation shouldBe Orientation.Portrait
            ctrl().displayType shouldBe DisplayType.Mobile

            // A phone held sideways: 900 is a tablet's width in portrait, but not in landscape
            at(900, 400) shouldBe (Orientation.Landscape to DisplayType.Mobile)
            at(900, 1200) shouldBe (Orientation.Portrait to DisplayType.Tablet)
            at(1000, 700) shouldBe (Orientation.Landscape to DisplayType.Tablet)
            at(1300, 800) shouldBe (Orientation.Landscape to DisplayType.Desktop)
            // Square counts as portrait, as in the CSS media query
            at(800, 800) shouldBe (Orientation.Portrait to DisplayType.Tablet)
        } finally {
            ctrl.dispose()
        }
    }

    "custom breakpoints per orientation" {
        val ctrl = ResponsiveController(
            portrait = ResponsiveController.Breakpoints(tablet = 500, desktop = 2000),
            landscape = ResponsiveController.Breakpoints(tablet = 1100, desktop = 1500),
            measure = { Vector2D(1200.0, 600.0) },
        )

        try {
            ctrl().displayType shouldBe DisplayType.Tablet
            ctrl().isLandscape shouldBe true
        } finally {
            ctrl.dispose()
        }
    }

    "a rotation emits once, with the new size, orientation and display type" {
        var size = Vector2D(390.0, 844.0)
        val ctrl = ResponsiveController(measure = { size })
        val seen = mutableListOf<ResponsiveController.State>()
        val unsubscribe = ctrl.subscribeToStream { seen.add(it) }
        seen.clear()

        try {
            size = Vector2D(844.0, 390.0)
            rotate()
            // The second measuring, on the next frame, finds the same state
            delay(100)

            seen.size shouldBe 1
            seen[0].windowSize shouldBe Vector2D(844.0, 390.0)
            seen[0].orientation shouldBe Orientation.Landscape
            seen[0].displayType shouldBe DisplayType.Mobile
        } finally {
            unsubscribe()
            ctrl.dispose()
        }
    }

    "a size that only arrives after the rotation event is measured on the next frame" {
        var size = Vector2D(390.0, 844.0)
        val ctrl = ResponsiveController(measure = { size })

        try {
            // iOS: the old size inside the rotation event, the new one a moment later, without a resize
            rotate()
            size = Vector2D(844.0, 390.0)
            delay(100)

            ctrl().orientation shouldBe Orientation.Landscape
        } finally {
            ctrl.dispose()
        }
    }

    "a resize that changes nothing emits nothing" {
        var size = Vector2D(1300.0, 800.0)
        val ctrl = ResponsiveController(measure = { size })
        var emissions = 0
        val unsubscribe = ctrl.subscribeToStream { emissions++ }
        emissions = 0

        try {
            window.dispatchEvent(Event("resize"))
            emissions shouldBe 0

            size = Vector2D(1301.0, 800.0)
            window.dispatchEvent(Event("resize"))
            emissions shouldBe 1
        } finally {
            unsubscribe()
            ctrl.dispose()
        }
    }

    "dispose removes every listener, the orientation query's too" {
        val proto = js("MediaQueryList.prototype")
        val add = proto.addEventListener
        val remove = proto.removeEventListener
        val counts = js("({ added: 0, removed: 0 })")
        // A plain function: the patched method needs its own `this`, the query it is called on
        val counting = js(
            "(function (orig, counts, field) { return function (type, h, o) { " +
                "if (type === 'change') counts[field]++; return orig.call(this, type, h, o); }; })"
        )
        proto.addEventListener = counting(add, counts, "added")
        proto.removeEventListener = counting(remove, counts, "removed")

        var size = Vector2D(390.0, 844.0)
        lateinit var ctrl: ResponsiveController

        try {
            ctrl = ResponsiveController(measure = { size })
            ctrl.dispose()
        } finally {
            proto.addEventListener = add
            proto.removeEventListener = remove
        }

        (counts.added as Int to counts.removed as Int) shouldBe (1 to 1)

        var emissions = 0
        val unsubscribe = ctrl.subscribeToStream { emissions++ }
        emissions = 0

        size = Vector2D(844.0, 390.0)
        window.dispatchEvent(Event("resize"))
        rotate()
        delay(100)
        unsubscribe()

        emissions shouldBe 0
    }

    "a browser whose MediaQueryList has no addEventListener (Safari before 14) uses addListener" {
        val proto = js("MediaQueryList.prototype")
        val add = proto.addEventListener
        val remove = proto.removeEventListener
        val counts = js("({ added: 0, removed: 0 })")
        val oldAdd = proto.addListener
        val oldRemove = proto.removeListener
        val counting = js("(function (orig, counts, field) { return function (l) { counts[field]++; return orig.call(this, l); }; })")

        proto.addEventListener = undefined
        proto.removeEventListener = undefined
        proto.addListener = counting(oldAdd, counts, "added")
        proto.removeListener = counting(oldRemove, counts, "removed")

        try {
            val ctrl = ResponsiveController(measure = { Vector2D(400.0, 800.0) })
            ctrl.dispose()
        } finally {
            proto.addEventListener = add
            proto.removeEventListener = remove
            proto.addListener = oldAdd
            proto.removeListener = oldRemove
        }

        (counts.added as Int to counts.removed as Int) shouldBe (1 to 1)
    }
})
