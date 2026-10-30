package io.peekandpoke.kraft.coretests.component

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.utils.WakeLockComponent
import io.peekandpoke.kraft.utils.WakeLockController
import io.peekandpoke.kraft.utils.WakeLockSentinel
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.events.Event
import kotlin.js.Promise

/** A screen lock the fake grants: released by the app, or by the "browser" when the page is hidden. */
private class FakeSentinel : WakeLockSentinel {
    var released = false
    private val listeners = mutableListOf<() -> Unit>()

    override fun addEventListener(type: String, callback: () -> Unit) {
        if (type == "release") listeners.add(callback)
    }

    override fun release(): Promise<Unit> {
        if (!released) {
            released = true
            listeners.toList().forEach { it() }
        }
        return Promise.resolve(Unit)
    }
}

/** The Screen Wake Lock API, by hand: each request waits until the spec grants or refuses it. */
private class FakeLockApi(var supported: Boolean = true) {
    var visible = true

    class Request(val grant: (WakeLockSentinel) -> Unit, val refuse: (Throwable) -> Unit)

    val requests = mutableListOf<Request>()
    val granted = mutableListOf<FakeSentinel>()

    val controller = WakeLockController(
        requestLock = {
            if (!supported) {
                null
            } else {
                Promise { resolve, reject -> requests.add(Request(resolve, reject)) }
            }
        },
        isPageVisible = { visible },
    )

    /** Grants the oldest open request. */
    suspend fun grant(): FakeSentinel {
        val sentinel = FakeSentinel()
        granted.add(sentinel)
        requests.removeAt(0).grant(sentinel)
        // The promise resolves on a microtask
        delay(5)
        return sentinel
    }
}

@Suppress("TestFunctionName")
private fun Tag.Locks(count: StreamSource<Int>) = comp(Locks.Props(count)) { Locks(it) }

private class Locks(ctx: Ctx<Props>) : Component<Locks.Props>(ctx) {
    data class Props(val count: StreamSource<Int>)

    private val count by subscribingTo(props.count)

    override fun VDom.render() {
        div { repeat(count) { WakeLockComponent() } }
    }
}

/** `WakeLockComponent` and its shared counter (ULTRA-00275). */
class WakeLockSpec : StringSpec({

    "stacked components share one lock, released when the last one unmounts" {
        val api = FakeLockApi()
        val count = StreamSource(2)

        TestBed.preact(
            appSetup = { wakeLockCtrl(api.controller) },
            view = { Locks(count) },
        ) {
            eventually { api.controller.holderCount shouldBe 2 }
            api.requests.size shouldBe 1
            val lock = api.grant()
            api.controller.isActive() shouldBe true

            count(1)
            eventually { api.controller.holderCount shouldBe 1 }
            lock.released shouldBe false

            count(0)
            eventually { api.controller.holderCount shouldBe 0 }
            lock.released shouldBe true
            api.controller.isActive() shouldBe false
        }
    }

    "every app has the default controller; components reach it as wakeLockCtrl" {
        TestBed.preact(view = { div { } }) {
            (app.appAttributes[WakeLockController.key] === WakeLockController.default) shouldBe true
        }
    }

    "a lock granted after everyone let go is released at once" {
        val api = FakeLockApi()

        val release = api.controller.acquire()
        api.requests.size shouldBe 1
        release()

        val lock = api.grant()

        lock.released shouldBe true
        api.controller.isActive() shouldBe false
    }

    "one request for holders that come while it is pending; a second release call does nothing" {
        val api = FakeLockApi()

        val first = api.controller.acquire()
        val second = api.controller.acquire()
        api.requests.size shouldBe 1

        val lock = api.grant()
        first()
        first()
        api.controller.holderCount shouldBe 1
        lock.released shouldBe false

        second()
        lock.released shouldBe true
    }

    "the browser drops the lock when the page is hidden; visible again and held, it is asked for again" {
        val api = FakeLockApi()

        val release = api.controller.acquire()
        val lock = api.grant()

        api.visible = false
        lock.release()
        api.controller.isActive() shouldBe false
        // Hidden: no request (it would be refused)
        api.requests.size shouldBe 0

        api.visible = true
        document.dispatchEvent(Event("visibilitychange"))
        api.requests.size shouldBe 1

        api.grant()
        api.controller.isActive() shouldBe true

        // Let go: its visibility listener must not stay on the document for the rest of the run
        release()
    }

    "a refused request is retried while held and visible" {
        val api = FakeLockApi()

        val release = api.controller.acquire()
        api.requests.removeAt(0).refuse(Throwable("NotAllowedError"))

        eventually { api.requests.size shouldBe 1 }
        api.grant()
        api.controller.isActive() shouldBe true

        release()
    }

    "the visibility listener is on the document only while someone holds the lock" {
        val doc = document.asDynamic()
        val add = doc.addEventListener
        val remove = doc.removeEventListener
        // The handlers on now, by identity: a removal of another function object (a fresh `::x`) removes nothing
        val live = js("([])")
        val tracking = js(
            "(function (add, remove, live, type) { return [" +
                "function (t, h, o) { if (t === type) live.push(h); return add.call(this, t, h, o); }, " +
                "function (t, h, o) { var i = live.indexOf(h); if (t === type && i >= 0) live.splice(i, 1); " +
                "return remove.call(this, t, h, o); }]; })"
        )
        val patched = tracking(add, remove, live, "visibilitychange")
        doc.addEventListener = patched[0]
        doc.removeEventListener = patched[1]

        try {
            val api = FakeLockApi()
            (live.length as Int) shouldBe 0

            val first = api.controller.acquire()
            val second = api.controller.acquire()
            (live.length as Int) shouldBe 1

            first()
            second()
            (live.length as Int) shouldBe 0
        } finally {
            doc.addEventListener = add
            doc.removeEventListener = remove
        }
    }

    "without the API nothing is asked and nothing fails" {
        val api = FakeLockApi(supported = false)

        val release = api.controller.acquire()

        api.controller.isActive() shouldBe false
        release()
        api.controller.holderCount shouldBe 0
    }
})
