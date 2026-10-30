package io.peekandpoke.kraft.addons.pixijs

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.kraft.addons.pixijs.js.Application
import io.peekandpoke.kraft.addons.pixijs.js.ApplicationOptions
import io.peekandpoke.kraft.addons.pixijs.js.Container
import io.peekandpoke.kraft.addons.pixijs.js.Ticker
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.utils.jsObject
import kotlinx.browser.document
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLDivElement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise
import kotlin.time.Duration.Companion.milliseconds

/**
 * The pixi.js fundamentals consumers rely on, run through the same Kotlin/JS webpack they bundle with — so a bundling
 * regression fails here. Ultra 0.111.0 shipped pixi 8.22 with webpack 5.108, which dropped pixi's side-effect import of
 * `app/init`: `init()` resolved, but `app.ticker` was undefined (Klang's playback highlight broke).
 */
@Suppress("unused")
class PixiJsContractSpec : StringSpec({

    suspend fun loadAddon(): PixiJsAddon = suspendCoroutine { cont ->
        var resumed = false
        AddonRegistryBuilder().pixiJs(lazy = true).subscribeToStream { addon ->
            if (addon != null && !resumed) {
                resumed = true
                cont.resume(addon)
            }
        }
    }

    suspend fun initializedApp(configure: ApplicationOptions.() -> Unit = {}): Application {
        val app = loadAddon().createApplication()
        app.init(jsObject<ApplicationOptions> {
            width = 120
            height = 80
            configure()
        }).await()
        return app
    }

    // Reads pixi's @internal `Application._plugins` on purpose: it is the root cause itself and needs no GPU. A pixi
    // rename turns this row red although the behaviour is fine — the ticker and resizeTo rows then say whether it is.
    "the Application plugins are registered (no GPU needed): TickerPlugin and ResizePlugin" {
        loadAddon()
        val pixi: dynamic = js("import('pixi.js')").unsafeCast<Promise<dynamic>>().await()
        val plugins: Array<dynamic> = pixi.Application._plugins.unsafeCast<Array<dynamic>>()

        plugins.contains(pixi.TickerPlugin) shouldBe true
        plugins.contains(pixi.ResizePlugin) shouldBe true
    }

    "createApplication, createGraphics and createText return instances" {
        val addon = loadAddon()

        addon.createApplication() shouldNotBe null
        addon.createGraphics() shouldNotBe null
        addon.createText(jsObject { text = "hi" }).text shouldBe "hi"
    }

    "after init the ticker exists and drives callbacks; start, remove and stop each work" {
        val app = initializedApp { asDynamic().autoStart = false }
        val ticker: Ticker? = app.asDynamic().ticker.unsafeCast<Ticker?>()

        ticker shouldNotBe null

        suspend fun callsWithin(callback: () -> Int): Int {
            var waited = 0
            val before = callback()
            while (callback() == before && waited < 2_000) {
                delay(20.milliseconds); waited += 20
            }
            return callback() - before
        }

        var calls = 0
        val callback: (Ticker) -> Unit = { calls++ }
        ticker!!.add(callback)

        // not started yet (autoStart = false): nothing is called
        delay(100.milliseconds)
        calls shouldBe 0

        ticker.start()
        (callsWithin { calls } > 0) shouldBe true

        // remove alone: the ticker keeps running, the callback is no longer called
        ticker.remove(callback)
        var other = 0
        ticker.add { other++ }
        val afterRemove = calls
        (callsWithin { other } > 0) shouldBe true
        calls shouldBe afterRemove

        // stop alone: nothing is called any more
        ticker.stop()
        val afterStop = other
        delay(100.milliseconds)
        other shouldBe afterStop

        app.destroy(rendererDestroy = true)
    }

    "renderer, canvas and stage exist; the stage takes a Graphics child" {
        val app = initializedApp()
        val g = loadAddon().createGraphics()

        (app.asDynamic().renderer.unsafeCast<Any?>()) shouldNotBe null
        app.canvas shouldNotBe null
        app.stage.addChild(g)
        app.stage.children.size shouldBe 1

        app.destroy(rendererDestroy = true)
    }

    "Graphics and Text actually render: their render pipes are registered" {
        val addon = loadAddon()
        val app = initializedApp()

        app.stage.addChild(addon.createGraphics().apply { rect(0.0, 0.0, 20.0, 10.0).fill(0xff00ff) })
        app.stage.addChild(addon.createText(jsObject { text = "hello" }))
        app.render()

        val pipes: dynamic = app.asDynamic().renderer.renderPipes
        (pipes.graphics.unsafeCast<Any?>()) shouldNotBe null
        (pipes.text.unsafeCast<Any?>()) shouldNotBe null

        app.destroy(rendererDestroy = true)
    }

    "resize() takes the resizeTo element's size (ResizePlugin adds it)" {
        val host = (document.createElement("div") as HTMLDivElement).apply {
            style.width = "100px"
            style.height = "60px"
            document.body!!.appendChild(this)
        }
        val app = initializedApp { resizeTo = host }

        host.style.width = "200px"
        host.style.height = "150px"
        app.resize()

        app.screen.width shouldBe 200.0
        app.screen.height shouldBe 150.0

        app.destroy(rendererDestroy = true)
        host.remove()
    }

    "followElementSize follows a resize by layout alone, with no window event, until stopped" {
        val host = (document.createElement("div") as HTMLDivElement).apply {
            style.width = "100px"
            style.height = "60px"
            document.body!!.appendChild(this)
        }
        val app = initializedApp()
        val stop = app.followElementSize(host)

        host.style.width = "220px"
        host.style.height = "140px"

        eventually {
            app.screen.width shouldBe 220.0
            app.screen.height shouldBe 140.0
        }

        stop()
        host.style.width = "300px"
        delay(100.milliseconds)
        app.screen.width shouldBe 220.0

        app.destroy(rendererDestroy = true)
        host.remove()
    }

    "followElementSize needs an initialized app, and a destroyed app stops it" {
        val pixi = loadAddon()
        val host = (document.createElement("div") as HTMLDivElement).apply {
            style.width = "100px"
            style.height = "60px"
            document.body!!.appendChild(this)
        }

        shouldThrow<IllegalArgumentException> { pixi.createApplication().followElementSize(host) }
            .message shouldContain "after init()"

        val app = initializedApp()
        app.followElementSize(host)
        app.destroy(rendererDestroy = true)

        // The observer's next callback finds no queueResize and disconnects: resizing the host must not throw
        host.style.width = "180px"
        delay(100.milliseconds)
        host.remove()
    }

    "createContainer and create(className) give pixi instances; create names a missing class" {
        val pixi = loadAddon()
        val isContainer: (Any) -> Boolean = { obj ->
            val ctor = pixi.raw.Container
            js("obj instanceof ctor").unsafeCast<Boolean>()
        }

        isContainer(pixi.createContainer()) shouldBe true
        isContainer(pixi.create<Container>("Container")) shouldBe true

        shouldThrow<IllegalArgumentException> { pixi.create<Any>("NoSuchThing") }.message shouldContain "NoSuchThing"
    }

    "Container: zIndex sorts with sortableChildren, addChildAt places, scale sets both" {
        val pixi = loadAddon()
        val parent = pixi.createContainer()
        val a = pixi.createContainer()
        val b = pixi.createContainer()
        val c = pixi.createContainer()

        parent.addChild(a)
        parent.addChild(b)
        parent.addChildAt(c, 0)
        parent.children.toList() shouldBe listOf(c, a, b)

        parent.sortableChildren = true
        a.zIndex = 3.0
        b.zIndex = 2.0
        c.zIndex = 1.0
        parent.sortChildren()
        parent.children.toList() shouldBe listOf(c, b, a)

        a.scale.set(2.0)
        a.scale.x shouldBe 2.0
        a.scale.y shouldBe 2.0
    }

    "the zIndex trap: a zIndex set before addChildAt does not turn on the parent's sortableChildren" {
        val pixi = loadAddon()

        // addChild and a zIndex set while in the parent turn it on
        val viaAddChild = pixi.createContainer().apply { addChild(pixi.createContainer().apply { zIndex = 5.0 }) }
        viaAddChild.sortableChildren shouldBe true

        val late = pixi.createContainer()
        val child = pixi.createContainer().also { late.addChild(it) }
        child.zIndex = 5.0
        late.sortableChildren shouldBe true

        // addChildAt does not
        val viaAddChildAt = pixi.createContainer()
        viaAddChildAt.addChildAt(pixi.createContainer().apply { zIndex = 5.0 }, 0)
        viaAddChildAt.sortableChildren shouldBe false
    }

    "the zIndex trap's remedy: sortableChildren before addChildAt sorts; turned on afterwards it does not" {
        val pixi = loadAddon()

        fun twoChildren(sortableFirst: Boolean): Pair<Container, List<Container>> {
            val parent = pixi.createContainer()
            val high = pixi.createContainer().apply { zIndex = 2.0 }
            val low = pixi.createContainer().apply { zIndex = 1.0 }

            if (sortableFirst) parent.sortableChildren = true
            parent.addChildAt(high, 0)
            parent.addChildAt(low, 0)
            parent.addChildAt(high, 0)
            if (!sortableFirst) parent.sortableChildren = true

            parent.sortChildren()
            return parent to listOf(low, high)
        }

        val (sorted, sortedOrder) = twoChildren(sortableFirst = true)
        sorted.children.toList() shouldBe sortedOrder

        val (unsorted, wouldBe) = twoChildren(sortableFirst = false)
        unsorted.children.toList() shouldBe wouldBe.reversed()
    }

    "Text.anchor and Graphics.poly" {
        val pixi = loadAddon()

        val text = pixi.createText(jsObject { this.text = "hi" })
        text.anchor.set(0.5)
        text.anchor.x shouldBe 0.5
        text.anchor.y shouldBe 0.5

        val graphics = pixi.createGraphics()
        graphics.poly(arrayOf(0.0, 0.0, 10.0, 0.0, 5.0, 8.0)) shouldBe graphics
    }

    "the init options Klang uses are accepted" {
        val app = initializedApp {
            backgroundAlpha = 0.0
            antialias = true
            preference = "webgl"
            autoDensity = true
            resolution = 2.0
        }

        app.screen.width shouldBe 120.0
        app.destroy(rendererDestroy = true)
    }

    "Graphics drawing calls run and change the bounds" {
        val g = loadAddon().createGraphics()

        g.rect(0.0, 0.0, 50.0, 30.0).fill(0xff00ff)
        g.roundRect(10.0, 10.0, 80.0, 40.0, 4.0).stroke(jsObject<dynamic> { width = 1.0; color = 0x00ff00 })
        g.alpha = 0.5
        g.visible = true
        g.x = 5.0
        g.y = 7.0

        val drawn = g.asDynamic().getBounds().width.unsafeCast<Double>()
        drawn shouldBeGreaterThan 0.0

        g.clear()
        g.asDynamic().getBounds().width.unsafeCast<Double>() shouldBe 0.0
    }

    "destroy, then a fresh Application initializes again (Klang re-creates the overlay per editor mount)" {
        initializedApp().destroy(rendererDestroy = true)

        val second = initializedApp()
        (second.asDynamic().ticker.unsafeCast<Any?>()) shouldNotBe null
        second.destroy(rendererDestroy = true)
    }

    "every API declared in pixi-definitions.kt exists on the real objects" {
        val addon = loadAddon()
        val app = initializedApp()
        val container: Container = addon.createGraphics()

        fun missing(obj: dynamic, names: List<String>): List<String> =
            names.filter { name -> js("!(name in obj)").unsafeCast<Boolean>() }

        missing(
            app,
            listOf("stage", "canvas", "ticker", "screen", "init", "render", "resize", "queueResize", "destroy"),
        ).shouldBeEmpty()
        missing(app.screen, listOf("x", "y", "width", "height")).shouldBeEmpty()
        missing(app.ticker, listOf("deltaTime", "deltaMS", "add", "remove", "stop", "start")).shouldBeEmpty()
        missing(
            container,
            listOf(
                "x", "y", "visible", "alpha", "rotation", "children", "scale", "zIndex", "sortableChildren",
                "addChild", "addChildAt", "sortChildren", "removeChild", "removeChildren", "removeFromParent", "destroy",
            ),
        ).shouldBeEmpty()
        missing(container, listOf("rect", "circle", "poly", "roundRect", "fill", "stroke", "clear")).shouldBeEmpty()
        missing(container.scale, listOf("x", "y", "set")).shouldBeEmpty()
        missing(addon.createText(jsObject { text = "x" }), listOf("text", "style", "anchor")).shouldBeEmpty()

        app.destroy(rendererDestroy = true)
    }
})
