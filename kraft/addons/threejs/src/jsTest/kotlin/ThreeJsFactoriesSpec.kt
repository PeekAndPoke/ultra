package io.peekandpoke.kraft.addons.threejs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.addons
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

private suspend fun eventually(
    timeout: Duration = 5.seconds,
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
private fun Tag.ThreeJsFactoriesConsumer() = comp { ThreeJsFactoriesConsumer(it) }

private class ThreeJsFactoriesConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val three by subscribingTo(addons.threeJs)
    private var result: String by value("waiting")

    init {
        lifecycle {
            onMount { check() }
            onUpdate { check() }
        }
    }

    private fun check() {
        if (result != "waiting") return
        val addon = three ?: return

        try {
            // Math
            val v = addon.createVector3(1.0, 2.0, 3.0)

            // Cameras (construction only — no GL)
            addon.createPerspectiveCamera(60.0, 1.5, 0.1, 500.0)
            addon.createOrthographicCamera(-1.0, 1.0, 1.0, -1.0, 0.1, 100.0)

            // Geometries
            val sphere = addon.createSphereGeometry(2.0, 16, 8)
            val plane = addon.createPlaneGeometry(2.0, 3.0)
            addon.createCylinderGeometry(1.0, 1.0, 2.0, 16)
            addon.createTorusGeometry(2.0, 0.5, 8, 24)

            // Materials
            val basic = addon.createMeshBasicMaterial(jsObject { this.color = 0x00ff00 })
            val lambert = addon.createMeshLambertMaterial(jsObject { this.color = 0xff0000 })

            // Meshes + lights into the scene
            val scene = addon.createScene()
            scene.add(addon.createMesh(sphere, basic))
            scene.add(addon.createMesh(plane, lambert))
            scene.add(addon.createDirectionalLight(0xffffff, 0.7))
            scene.add(addon.createPointLight(0xffffff, 1.0, 10.0, 2.0))
            scene.add(addon.createHemisphereLight(0xffffff, 0x444444, 0.5))

            val ok = v.x == 1.0 && v.y == 2.0 && v.z == 3.0 && scene.children.size == 5
            result = if (ok) "ok" else "mismatch v=(${v.x},${v.y},${v.z}) children=${scene.children.size}"
        } catch (e: Throwable) {
            result = "error: ${e.message}"
        }
        triggerRedraw()
    }

    override fun VDom.render() {
        div(classes = "result") { +result }
    }
}

/** Top-up coverage for the remaining three.js factories — all pure construction, no WebGL. */
class ThreeJsFactoriesSpec : StringSpec() {

    init {
        "creates cameras, geometries, materials, lights and Vector3 (no GL)" {
            TestBed.preact(
                appSetup = { addons { threeJs() } },
                view = { ThreeJsFactoriesConsumer() },
            ) { root ->
                eventually {
                    root.selectCss(".result").textContent() shouldBe "ok"
                }
            }
        }
    }
}
