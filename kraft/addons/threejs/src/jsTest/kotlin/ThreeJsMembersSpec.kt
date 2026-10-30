package io.peekandpoke.kraft.addons.threejs

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.addons.threejs.js.Light
import io.peekandpoke.kraft.addons.threejs.js.Mesh
import io.peekandpoke.kraft.addons.threejs.js.Object3D
import io.peekandpoke.kraft.addons.threejs.js.TextureFilter
import io.peekandpoke.kraft.addons.threejs.js.TextureWrapping
import io.peekandpoke.kraft.addons.threejs.js.Vector3
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.document
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.PI
import kotlin.time.Duration.Companion.seconds

private var loaded: ThreeJsAddon? = null

@Suppress("TestFunctionName")
private fun Tag.ThreeJsCatcher() = comp { ThreeJsCatcher(it) }

private class ThreeJsCatcher(ctx: NoProps) : PureComponent(ctx) {
    private val three by subscribingTo(addons.threeJs)

    override fun VDom.render() {
        loaded = three
        div { }
    }
}

/** Runs [block] with the loaded addon. No WebGL: these rows touch only what three.js builds without a renderer. */
private suspend fun withThree(block: suspend (ThreeJsAddon) -> Unit) {
    TestBed.preact(appSetup = { addons { threeJs() } }, view = { ThreeJsCatcher() }) {
        block(eventually(timeout = 5.seconds) { loaded.shouldNotBeNull() })
    }
}

private fun isInstance(obj: Any?, ctor: dynamic): Boolean = js("obj instanceof ctor").unsafeCast<Boolean>()

/** ULTRA-00246 (from Spross and Klang): the constructors and members the projects filled in themselves. */
class ThreeJsMembersSpec : StringSpec({

    "each new constructor gives an instance of the three.js class" {
        withThree { three ->
            val raw = three.raw

            isInstance(three.createObject3D(), raw.Object3D) shouldBe true
            isInstance(three.createGroup(), raw.Group) shouldBe true
            isInstance(three.createSpotLight(), raw.SpotLight) shouldBe true
            isInstance(three.createRaycaster(), raw.Raycaster) shouldBe true
            isInstance(three.createVector2(1.0, 2.0), raw.Vector2) shouldBe true

            val canvas = document.createElement("canvas") as HTMLCanvasElement
            isInstance(three.createCanvasTexture(canvas), raw.CanvasTexture) shouldBe true
        }
    }

    "create(className) builds any class, and names a missing one" {
        withThree { three ->
            val v = three.create<Vector3>("Vector3", 1.0, 2.0, 3.0)
            v.z shouldBe 3.0

            shouldThrow<IllegalArgumentException> { three.create<Any>("NoSuchThing") }
                .message shouldContain "NoSuchThing"
        }
    }

    "the scene graph members: parent, removeFromParent, renderOrder, lookAt(Vector3), localToWorld" {
        withThree { three ->
            val group = three.createGroup()
            val child: Object3D = three.createObject3D()

            group.add(child)
            child.parent shouldBeSameInstanceAs group

            child.removeFromParent()
            child.parent shouldBe null
            group.children.size shouldBe 0

            // The three.js default first: a misspelt binding would read undefined
            child.renderOrder shouldBe 0
            child.renderOrder = 7
            child.renderOrder shouldBe 7

            // A child of a moved group maps its origin to the group's position
            group.position.set(1.0, 2.0, 3.0)
            group.add(child)
            group.updateWorldMatrix(updateParents = true, updateChildren = true)
            val world = child.localToWorld(three.createVector3(0.0, 0.0, 0.0))
            listOf(world.x, world.y, world.z) shouldBe listOf(1.0, 2.0, 3.0)

            val mesh: Mesh = three.createMesh(three.createBoxGeometry(), three.createMeshBasicMaterial(js("({})")))
            // Facing straight up turns the mesh by -PI/2 around x; a no-op lookAt would leave 0
            mesh.lookAt(three.createVector3(0.0, 10.0, 0.0))
            // Looking along the up axis, three.js nudges the view direction off it by 1e-4: not exactly -PI/2
            mesh.rotation.x shouldBe (-PI / 2 plusOrMinus 1e-3)
        }
    }

    "lights, materials and textures carry the new members" {
        withThree { three ->
            val spot = three.createSpotLight(angle = 0.5, penumbra = 0.2)
            spot.angle shouldBe 0.5
            spot.penumbra shouldBe 0.2
            spot.shadow.mapSize.set(1024.0, 512.0)
            spot.shadow.mapSize.x shouldBe 1024.0
            spot.dispose()

            three.createDirectionalLight().shadow.mapSize.x shouldBe 512.0
            three.createPointLight().shadow.mapSize.y shouldBe 512.0

            // Through the base type: the shadow of a light that has one, none for one that has none
            val asLight: Light = three.createSpotLight()
            asLight.shadow?.mapSize?.x shouldBe 512.0
            val ambient: Light = three.createAmbientLight()
            (ambient.shadow == null) shouldBe true

            three.createHemisphereLight(groundColor = 0x00ff00).groundColor.g shouldBe 1.0

            val material = three.createMeshBasicMaterial(js("({})"))
            val versionBefore: Int = material.asDynamic().version.unsafeCast<Int>()
            material.needsUpdate = true
            (material.asDynamic().version.unsafeCast<Int>() > versionBefore) shouldBe true

            val texture = three.createCanvasTexture(document.createElement("canvas") as HTMLCanvasElement)
            texture.wrapS shouldBe TextureWrapping.ClampToEdgeWrapping
            texture.wrapS = TextureWrapping.RepeatWrapping
            texture.minFilter = TextureFilter.NearestFilter
            texture.wrapS shouldBe 1000
            // Write-only: it counts the texture's version up, which makes the renderer upload the image again
            val textureVersion: Int = texture.asDynamic().version.unsafeCast<Int>()
            texture.needsUpdate = true
            (texture.asDynamic().version.unsafeCast<Int>() > textureVersion) shouldBe true
            texture.dispose()
        }
    }

    "the constants equal three.js's own" {
        withThree { three ->
            val raw = three.raw

            TextureWrapping.RepeatWrapping shouldBe raw.RepeatWrapping
            TextureWrapping.ClampToEdgeWrapping shouldBe raw.ClampToEdgeWrapping
            TextureWrapping.MirroredRepeatWrapping shouldBe raw.MirroredRepeatWrapping
            TextureFilter.NearestFilter shouldBe raw.NearestFilter
            TextureFilter.NearestMipmapNearestFilter shouldBe raw.NearestMipmapNearestFilter
            TextureFilter.NearestMipmapLinearFilter shouldBe raw.NearestMipmapLinearFilter
            TextureFilter.LinearFilter shouldBe raw.LinearFilter
            TextureFilter.LinearMipmapNearestFilter shouldBe raw.LinearMipmapNearestFilter
            TextureFilter.LinearMipmapLinearFilter shouldBe raw.LinearMipmapLinearFilter
        }
    }
})
