package io.peekandpoke.kraft.addons.threejs

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import io.peekandpoke.kraft.addons.threejs.js.AmbientLight
import io.peekandpoke.kraft.addons.threejs.js.BoxGeometry
import io.peekandpoke.kraft.addons.threejs.js.CylinderGeometry
import io.peekandpoke.kraft.addons.threejs.js.DirectionalLight
import io.peekandpoke.kraft.addons.threejs.js.HemisphereLight
import io.peekandpoke.kraft.addons.threejs.js.MaterialParameters
import io.peekandpoke.kraft.addons.threejs.js.Mesh
import io.peekandpoke.kraft.addons.threejs.js.MeshBasicMaterial
import io.peekandpoke.kraft.addons.threejs.js.MeshLambertMaterial
import io.peekandpoke.kraft.addons.threejs.js.MeshStandardMaterial
import io.peekandpoke.kraft.addons.threejs.js.OrthographicCamera
import io.peekandpoke.kraft.addons.threejs.js.PerspectiveCamera
import io.peekandpoke.kraft.addons.threejs.js.PlaneGeometry
import io.peekandpoke.kraft.addons.threejs.js.PointLight
import io.peekandpoke.kraft.addons.threejs.js.Scene
import io.peekandpoke.kraft.addons.threejs.js.SphereGeometry
import io.peekandpoke.kraft.addons.threejs.js.TorusGeometry
import io.peekandpoke.kraft.addons.threejs.js.Vector3
import io.peekandpoke.kraft.addons.threejs.js.WebGLRenderer
import io.peekandpoke.kraft.addons.threejs.js.WebGLRendererParameters
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded three.js addon.
 *
 * Provides type-safe access to three.js for 3D WebGL rendering.
 */
class ThreeJsAddon internal constructor(
    private val threeModule: dynamic,
) {
    /** Raw module handle — escape hatch for APIs not covered by the curated externals. */
    val raw: dynamic get() = threeModule

    //  Scene / Camera / Renderer  /////////////////////////////////////////////////////////////////////

    fun createScene(): Scene {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.Scene
        return js("new ctor()").unsafeCast<Scene>()
    }

    fun createPerspectiveCamera(
        fov: Double = 50.0,
        aspect: Double = 1.0,
        near: Double = 0.1,
        far: Double = 2000.0,
    ): PerspectiveCamera {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.PerspectiveCamera
        return js("new ctor(fov, aspect, near, far)").unsafeCast<PerspectiveCamera>()
    }

    fun createOrthographicCamera(
        left: Double, right: Double, top: Double, bottom: Double,
        near: Double = 0.1, far: Double = 2000.0,
    ): OrthographicCamera {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.OrthographicCamera
        return js("new ctor(left, right, top, bottom, near, far)").unsafeCast<OrthographicCamera>()
    }

    fun createWebGLRenderer(parameters: WebGLRendererParameters): WebGLRenderer {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.WebGLRenderer
        return js("new ctor(parameters)").unsafeCast<WebGLRenderer>()
    }

    //  Geometry  //////////////////////////////////////////////////////////////////////////////////////

    fun createBoxGeometry(width: Double = 1.0, height: Double = 1.0, depth: Double = 1.0): BoxGeometry {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.BoxGeometry
        return js("new ctor(width, height, depth)").unsafeCast<BoxGeometry>()
    }

    fun createSphereGeometry(radius: Double = 1.0, widthSegments: Int = 32, heightSegments: Int = 16): SphereGeometry {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.SphereGeometry
        return js("new ctor(radius, widthSegments, heightSegments)").unsafeCast<SphereGeometry>()
    }

    fun createPlaneGeometry(width: Double = 1.0, height: Double = 1.0): PlaneGeometry {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.PlaneGeometry
        return js("new ctor(width, height)").unsafeCast<PlaneGeometry>()
    }

    fun createCylinderGeometry(
        radiusTop: Double = 1.0, radiusBottom: Double = 1.0, height: Double = 1.0, radialSegments: Int = 32,
    ): CylinderGeometry {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.CylinderGeometry
        return js("new ctor(radiusTop, radiusBottom, height, radialSegments)").unsafeCast<CylinderGeometry>()
    }

    fun createTorusGeometry(
        radius: Double = 1.0, tube: Double = 0.4, radialSegments: Int = 12, tubularSegments: Int = 48,
    ): TorusGeometry {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.TorusGeometry
        return js("new ctor(radius, tube, radialSegments, tubularSegments)").unsafeCast<TorusGeometry>()
    }

    //  Materials  /////////////////////////////////////////////////////////////////////////////////////

    fun createMeshBasicMaterial(parameters: MaterialParameters): MeshBasicMaterial {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.MeshBasicMaterial
        return js("new ctor(parameters)").unsafeCast<MeshBasicMaterial>()
    }

    fun createMeshStandardMaterial(parameters: MaterialParameters): MeshStandardMaterial {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.MeshStandardMaterial
        return js("new ctor(parameters)").unsafeCast<MeshStandardMaterial>()
    }

    fun createMeshLambertMaterial(parameters: MaterialParameters): MeshLambertMaterial {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.MeshLambertMaterial
        return js("new ctor(parameters)").unsafeCast<MeshLambertMaterial>()
    }

    //  Mesh  //////////////////////////////////////////////////////////////////////////////////////////

    fun createMesh(geometry: dynamic, material: dynamic): Mesh {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.Mesh
        return js("new ctor(geometry, material)").unsafeCast<Mesh>()
    }

    //  Lights  ////////////////////////////////////////////////////////////////////////////////////////

    fun createAmbientLight(color: dynamic = 0xffffff, intensity: Double = 1.0): AmbientLight {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.AmbientLight
        return js("new ctor(color, intensity)").unsafeCast<AmbientLight>()
    }

    fun createDirectionalLight(color: dynamic = 0xffffff, intensity: Double = 1.0): DirectionalLight {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.DirectionalLight
        return js("new ctor(color, intensity)").unsafeCast<DirectionalLight>()
    }

    fun createPointLight(
        color: dynamic = 0xffffff, intensity: Double = 1.0, distance: Double = 0.0, decay: Double = 2.0,
    ): PointLight {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.PointLight
        return js("new ctor(color, intensity, distance, decay)").unsafeCast<PointLight>()
    }

    fun createHemisphereLight(
        skyColor: dynamic = 0xffffff, groundColor: dynamic = 0x444444, intensity: Double = 1.0,
    ): HemisphereLight {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.HemisphereLight
        return js("new ctor(skyColor, groundColor, intensity)").unsafeCast<HemisphereLight>()
    }

    //  Math  //////////////////////////////////////////////////////////////////////////////////////////

    fun createVector3(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vector3 {
        @Suppress("UnusedVariable", "unused")
        val ctor = threeModule.Vector3
        return js("new ctor(x, y, z)").unsafeCast<Vector3>()
    }
}

/** Key for the three.js addon. */
val threeJsAddonKey = AddonKey<ThreeJsAddon>("threejs")

/** Registers the three.js addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.threeJs(lazy: Boolean = false): Addon<ThreeJsAddon> = register(
    key = threeJsAddonKey,
    name = "threejs",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val module: dynamic = (js("import('three')") as Promise<dynamic>).await()

    ThreeJsAddon(
        threeModule = module.default ?: module,
    )
}

/** Accessor for the three.js addon on the registry. */
val AddonRegistry.threeJs: Addon<ThreeJsAddon>
    get() = this[threeJsAddonKey]
