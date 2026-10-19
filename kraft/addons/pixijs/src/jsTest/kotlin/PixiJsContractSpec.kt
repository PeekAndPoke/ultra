package io.peekandpoke.kraft.addons.pixijs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.kraft.addons.pixijs.js.Application
import io.peekandpoke.kraft.addons.pixijs.js.ApplicationOptions
import io.peekandpoke.kraft.addons.pixijs.js.Container
import io.peekandpoke.kraft.addons.pixijs.js.Ticker
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
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

    "after init the ticker exists and drives callbacks; remove and stop work" {
        val app = initializedApp()
        val ticker: Ticker? = app.asDynamic().ticker.unsafeCast<Ticker?>()

        ticker shouldNotBe null

        var calls = 0
        val callback: (Ticker) -> Unit = { calls++ }

        ticker!!.add(callback)
        ticker.start()

        var waited = 0
        while (calls == 0 && waited < 2_000) {
            delay(20.milliseconds); waited += 20
        }
        (calls > 0) shouldBe true

        ticker.remove(callback)
        ticker.stop()
        val afterStop = calls
        delay(100.milliseconds)
        calls shouldBe afterStop

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

    "resizeTo follows the element (ResizePlugin)" {
        val host = (document.createElement("div") as HTMLDivElement).apply {
            style.width = "100px"
            style.height = "60px"
            document.body!!.appendChild(this)
        }
        val app = initializedApp { resizeTo = host }

        host.style.width = "200px"
        host.style.height = "150px"
        // the ResizePlugin adds resize() to the application
        app.asDynamic().resize()

        app.screen.width shouldBe 200.0
        app.screen.height shouldBe 150.0

        app.destroy(rendererDestroy = true)
        host.remove()
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

        missing(app, listOf("stage", "canvas", "ticker", "screen", "init", "render", "destroy")).shouldBeEmpty()
        missing(app.screen, listOf("x", "y", "width", "height")).shouldBeEmpty()
        missing(app.ticker, listOf("deltaTime", "deltaMS", "add", "remove", "stop", "start")).shouldBeEmpty()
        missing(
            container,
            listOf(
                "x", "y", "visible", "alpha", "rotation", "children",
                "addChild", "removeChild", "removeChildren", "removeFromParent", "destroy",
            ),
        ).shouldBeEmpty()
        missing(container, listOf("rect", "circle", "roundRect", "fill", "stroke", "clear")).shouldBeEmpty()
        missing(addon.createText(jsObject { text = "x" }), listOf("text", "style")).shouldBeEmpty()

        app.destroy(rendererDestroy = true)
    }
})
