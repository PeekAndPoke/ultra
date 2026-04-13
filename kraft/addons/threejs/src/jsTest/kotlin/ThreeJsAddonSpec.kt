package io.peekandpoke.kraft.addons.threejs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.addons.threejs.js.MaterialSide
import io.peekandpoke.kraft.addons.threejs.js.Mesh
import io.peekandpoke.kraft.addons.threejs.js.Scene
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Polls until [check] passes or [timeout] is reached. */
private suspend fun eventually(
    timeout: Duration = 2.seconds,
    poll: Duration = 25.milliseconds,
    check: suspend () -> Unit,
) {
    val deadline = kotlinx.datetime.Clock.System.now().plus(timeout)

    while (true) {
        try {
            check()
            return
        } catch (e: Throwable) {
            if (kotlinx.datetime.Clock.System.now() > deadline) throw e
            delay(poll)
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.ThreeJsLoadConsumer() = comp { ThreeJsLoadConsumer(it) }

private class ThreeJsLoadConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val threeAddon by subscribingTo(addons.threeJs)

    override fun VDom.render() {
        div(classes = "addon-status") {
            if (threeAddon != null) +"ready" else +"loading"
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.ThreeJsObjectsConsumer() = comp { ThreeJsObjectsConsumer(it) }

private class ThreeJsObjectsConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val threeAddon by subscribingTo(addons.threeJs)
    private var result: String by value("waiting")

    init {
        lifecycle {
            onMount { check() }
            onUpdate { check() }
        }
    }

    private fun check() {
        if (result != "waiting") return
        val addon = threeAddon ?: return

        try {
            val scene: Scene = addon.createScene()
            val geometry = addon.createBoxGeometry(1.0, 2.0, 3.0)
            val material = addon.createMeshStandardMaterial(jsObject {
                this.color = 0xff00ff
                this.metalness = 0.5
                this.roughness = 0.4
                this.side = MaterialSide.DoubleSide
            })
            val mesh: Mesh = addon.createMesh(geometry, material)
            mesh.position.set(1.0, 2.0, 3.0)
            mesh.rotation.set(0.1, 0.2, 0.3)
            scene.add(mesh)

            val ok = mesh.position.x == 1.0 &&
                    mesh.position.y == 2.0 &&
                    mesh.position.z == 3.0 &&
                    scene.children.size == 1
            result = if (ok) "ok" else "prop-mismatch"
        } catch (e: Throwable) {
            result = "error: ${e.message}"
        }
        triggerRedraw()
    }

    override fun VDom.render() {
        div(classes = "objects-status") { +result }
    }
}

@Suppress("TestFunctionName")
private fun Tag.ThreeJsRenderConsumer() = comp { ThreeJsRenderConsumer(it) }

private class ThreeJsRenderConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val threeAddon by subscribingTo(addons.threeJs)
    private var result: String by value("waiting")

    init {
        lifecycle {
            onMount { check() }
            onUpdate { check() }
        }
    }

    private fun check() {
        if (result != "waiting") return
        val addon = threeAddon ?: return

        try {
            val scene = addon.createScene()
            val camera = addon.createPerspectiveCamera(75.0, 1.0, 0.1, 1000.0)
            camera.position.z = 5.0

            scene.add(addon.createAmbientLight(0xffffff, 0.8))

            val mesh = addon.createMesh(
                addon.createBoxGeometry(1.0, 1.0, 1.0),
                addon.createMeshBasicMaterial(jsObject {
                    this.color = 0x00ff00
                }),
            )
            scene.add(mesh)

            val canvas = kotlinx.browser.document.createElement("canvas") as org.w3c.dom.HTMLCanvasElement
            canvas.width = 64
            canvas.height = 64

            val renderer = addon.createWebGLRenderer(jsObject {
                this.canvas = canvas
                this.antialias = false
            })
            renderer.setSize(64, 64, updateStyle = false)
            renderer.render(scene, camera)
            renderer.dispose()

            result = "ok"
        } catch (e: Throwable) {
            result = "error: ${e.message}"
        }
        triggerRedraw()
    }

    override fun VDom.render() {
        div(classes = "render-status") { +result }
    }
}

class ThreeJsAddonSpec : StringSpec() {

    init {
        "threejs addon loads and provides ThreeJsAddon" {
            TestBed.preact(
                appSetup = {
                    addons {
                        threeJs()
                    }
                },
                view = {
                    ThreeJsLoadConsumer()
                },
            ) { root ->
                eventually(timeout = 5.seconds) {
                    root.selectCss(".addon-status").textContent() shouldBe "ready"
                }
            }
        }

        "ThreeJsAddon creates Scene/Mesh with working properties" {
            TestBed.preact(
                appSetup = {
                    addons {
                        threeJs()
                    }
                },
                view = {
                    ThreeJsObjectsConsumer()
                },
            ) { root ->
                eventually(timeout = 5.seconds) {
                    root.selectCss(".objects-status").textContent() shouldBe "ok"
                }
            }
        }

        "ThreeJsAddon renders a scene through a WebGLRenderer" {
            TestBed.preact(
                appSetup = {
                    addons {
                        threeJs()
                    }
                },
                view = {
                    ThreeJsRenderConsumer()
                },
            ) { root ->
                eventually(timeout = 10.seconds) {
                    root.selectCss(".render-status").textContent() shouldBe "ok"
                }
            }
        }
    }
}
