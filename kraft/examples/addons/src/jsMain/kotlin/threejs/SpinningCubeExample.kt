package io.peekandpoke.kraft.examples.jsaddons.threejs

import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.threejs.ThreeJs
import io.peekandpoke.kraft.addons.threejs.ThreeJsAddon
import io.peekandpoke.kraft.addons.threejs.js.Mesh
import io.peekandpoke.kraft.addons.threejs.threeJs
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.SpinningCubeExample() = comp {
    SpinningCubeExample(it)
}

class SpinningCubeExample(ctx: NoProps) : PureComponent(ctx) {

    private val threeAddon: ThreeJsAddon? by subscribingTo(addons.threeJs)

    private var cube: Mesh? = null

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"Spinning Cube (via Three.js addon)" }

            p {
                +"A textbook rotating cube rendered with Three.js, loaded on-demand through the "
                +"Kraft AddonRegistry. The component manages the scene, camera, renderer, and "
                +"requestAnimationFrame loop — you just build the scene and advance state."
            }

            if (threeAddon == null) {
                ui.placeholder.segment {
                    ui.icon.header {
                        icon.spinner_loading()
                        +"Loading three.js addon..."
                    }
                }
                return@segment
            }

            div {
                attributes["style"] = "width: 100%; max-width: 720px; height: 480px; " +
                        "border: 2px solid #333; border-radius: 4px; overflow: hidden;"

                ThreeJs(
                    antialias = true,
                    clearColor = 0x1a1a2e,
                    onReady = { ctx ->
                        val addon = threeAddon ?: return@ThreeJs

                        ctx.scene.add(addon.createAmbientLight(0xffffff, 0.6))
                        val dirLight = addon.createDirectionalLight(0xffffff, 0.8)
                        dirLight.position.set(5.0, 10.0, 7.5)
                        ctx.scene.add(dirLight)

                        val mesh = addon.createMesh(
                            addon.createBoxGeometry(1.5, 1.5, 1.5),
                            addon.createMeshStandardMaterial(jsObject {
                                this.color = 0xe94560
                                this.metalness = 0.3
                                this.roughness = 0.4
                            }),
                        )
                        ctx.scene.add(mesh)
                        cube = mesh
                    },
                    onFrame = { f ->
                        val c = cube ?: return@ThreeJs
                        val r = f.deltaMs / 1000.0
                        c.rotation.x += 0.6 * r
                        c.rotation.y += 0.9 * r
                    },
                )
            }
        }
    }
}
