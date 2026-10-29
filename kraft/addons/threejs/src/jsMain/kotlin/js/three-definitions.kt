@file:Suppress("FunctionName", "unused")

package io.peekandpoke.kraft.addons.threejs.js

import org.w3c.dom.HTMLCanvasElement

/**
 * Minimal type-only declarations for three.js.
 *
 * Instances are created via [io.peekandpoke.kraft.addons.threejs.ThreeJsAddon].
 * The JS library is loaded dynamically — no static @JsModule import.
 *
 * Based on: https://threejs.org/docs/
 */

/** 3D vector with x, y, z. */
external class Vector3 {
    var x: Double
    var y: Double
    var z: Double

    fun set(x: Double, y: Double, z: Double): Vector3
    fun copy(v: Vector3): Vector3
    fun add(v: Vector3): Vector3
    fun sub(v: Vector3): Vector3
    fun multiplyScalar(s: Double): Vector3
    fun length(): Double
    fun normalize(): Vector3
    fun clone(): Vector3
}

/** 2D vector with x, y: a shadow's map size, a pointer position for a [Raycaster]. */
external class Vector2(x: Double = definedExternally, y: Double = definedExternally) {
    var x: Double
    var y: Double

    fun set(x: Double, y: Double): Vector2
    fun copy(v: Vector2): Vector2
}

/** Euler rotation (x, y, z) in radians. */
external class Euler {
    var x: Double
    var y: Double
    var z: Double
    var order: String

    fun set(x: Double, y: Double, z: Double, order: String = definedExternally): Euler
}

/** RGB color; accepts Number, string (hex/css), or r,g,b doubles. */
external class Color {
    var r: Double
    var g: Double
    var b: Double

    fun set(value: dynamic): Color
    fun setHex(hex: Int): Color
    fun setRGB(r: Double, g: Double, b: Double): Color
}

/** Base scene graph node. */
open external class Object3D {
    val position: Vector3
    val rotation: Euler
    val scale: Vector3
    var name: String
    var visible: Boolean
    val children: Array<Object3D>
    var castShadow: Boolean
    var receiveShadow: Boolean

    /**
     * The draw order: a higher one draws later. On a mesh it orders the mesh among the others; on a [Group] it orders
     * the group's whole subtree as one, and a [Group] inside it starts an order of its own.
     */
    var renderOrder: Int

    /** The node it sits under, or null. */
    val parent: Object3D?

    fun add(obj: Object3D): Object3D
    fun remove(obj: Object3D): Object3D
    fun clear(): Object3D
    fun lookAt(x: Double, y: Double, z: Double)

    /** Turns the node to face [target], a point in world coordinates (a camera or a light looks along it). */
    fun lookAt(target: Vector3)

    fun traverse(callback: (Object3D) -> Unit)

    /** Takes the node out of its parent; nothing without one. */
    fun removeFromParent(): Object3D

    /** Updates the world matrix: the ancestors' first if [updateParents], the subtree's after if [updateChildren]. */
    fun updateWorldMatrix(updateParents: Boolean, updateChildren: Boolean)

    /** Turns [vector], a point in this node's coordinates, into world coordinates, in place, by the world matrix. */
    fun localToWorld(vector: Vector3): Vector3
}

/** A node that only groups others: moving, turning or hiding it does so to its subtree. */
external class Group : Object3D

/** Root scene container. */
external class Scene : Object3D {
    var background: dynamic // Color | Texture | null
}

/** Base camera. */
open external class Camera : Object3D

/** Perspective camera — most common 3D camera. */
external class PerspectiveCamera(
    fov: Double = definedExternally,
    aspect: Double = definedExternally,
    near: Double = definedExternally,
    far: Double = definedExternally,
) : Camera {
    var fov: Double
    var aspect: Double
    var near: Double
    var far: Double

    fun updateProjectionMatrix()
}

/** Orthographic camera for flat projections. */
external class OrthographicCamera(
    left: Double = definedExternally,
    right: Double = definedExternally,
    top: Double = definedExternally,
    bottom: Double = definedExternally,
    near: Double = definedExternally,
    far: Double = definedExternally,
) : Camera {
    var left: Double
    var right: Double
    var top: Double
    var bottom: Double
    var near: Double
    var far: Double

    fun updateProjectionMatrix()
}

/** Parameters for [WebGLRenderer] construction. */
external interface WebGLRendererParameters {
    var canvas: HTMLCanvasElement
    var antialias: Boolean
    var alpha: Boolean
    var precision: String
    var premultipliedAlpha: Boolean
    var preserveDrawingBuffer: Boolean
    var powerPreference: String
    var logarithmicDepthBuffer: Boolean
}

/** The WebGL renderer. */
external class WebGLRenderer(parameters: WebGLRendererParameters = definedExternally) {
    val domElement: HTMLCanvasElement

    fun setSize(width: Int, height: Int, updateStyle: Boolean = definedExternally)
    fun setPixelRatio(value: Double)
    fun setClearColor(color: dynamic, alpha: Double = definedExternally)
    /** The renderer's shadow maps: off unless [ShadowMap.enabled]. */
    val shadowMap: ShadowMap

    fun render(scene: Scene, camera: Camera)
    fun dispose()
}

/** A renderer's shadow maps. */
external interface ShadowMap {
    /** Whether the renderer draws shadows at all. */
    var enabled: Boolean

    /** The filtering: three.js's `BasicShadowMap` (0), `PCFShadowMap` (1), `PCFSoftShadowMap` (2), `VSMShadowMap` (3). */
    var type: Int
}

/** Base geometry. */
open external class BufferGeometry {
    fun dispose()
}

/** Box geometry. */
external class BoxGeometry(
    width: Double = definedExternally,
    height: Double = definedExternally,
    depth: Double = definedExternally,
    widthSegments: Int = definedExternally,
    heightSegments: Int = definedExternally,
    depthSegments: Int = definedExternally,
) : BufferGeometry

/** Sphere geometry. */
external class SphereGeometry(
    radius: Double = definedExternally,
    widthSegments: Int = definedExternally,
    heightSegments: Int = definedExternally,
) : BufferGeometry

/** Plane geometry. */
external class PlaneGeometry(
    width: Double = definedExternally,
    height: Double = definedExternally,
    widthSegments: Int = definedExternally,
    heightSegments: Int = definedExternally,
) : BufferGeometry

/** Cylinder geometry. */
external class CylinderGeometry(
    radiusTop: Double = definedExternally,
    radiusBottom: Double = definedExternally,
    height: Double = definedExternally,
    radialSegments: Int = definedExternally,
) : BufferGeometry

/** Torus geometry. */
external class TorusGeometry(
    radius: Double = definedExternally,
    tube: Double = definedExternally,
    radialSegments: Int = definedExternally,
    tubularSegments: Int = definedExternally,
) : BufferGeometry

/** Base material. */
open external class Material {
    var transparent: Boolean
    var opacity: Double
    var visible: Boolean
    var side: Int

    /** Whether the material is rebuilt before it draws next: needed after a change of `transparent` or `side`. */
    var needsUpdate: Boolean

    fun dispose()
}

/** Parameters shared by most materials. Extra fields exist for material-specific options (three.js silently ignores unknown keys). */
external interface MaterialParameters {
    var color: dynamic // Color | Int | String
    var transparent: Boolean
    var opacity: Double
    var wireframe: Boolean
    var side: Int

    // MeshStandardMaterial-specific (safe to set on other materials — ignored)
    var metalness: Double
    var roughness: Double

    // MeshBasicMaterial — line width
    var linewidth: Double
}

/** Unlit material. */
external class MeshBasicMaterial(parameters: MaterialParameters = definedExternally) : Material {
    val color: Color
    var wireframe: Boolean
}

/** Lit material using physically based shading. */
external class MeshStandardMaterial(parameters: MaterialParameters = definedExternally) : Material {
    val color: Color
    var metalness: Double
    var roughness: Double
    var wireframe: Boolean
}

/** Lit material using Lambertian reflectance (cheaper than Standard). */
external class MeshLambertMaterial(parameters: MaterialParameters = definedExternally) : Material {
    val color: Color
}

/** Line material. */
external class LineBasicMaterial(parameters: MaterialParameters = definedExternally) : Material {
    val color: Color
}

/** A mesh — geometry + material. */
external class Mesh(
    geometry: BufferGeometry = definedExternally,
    material: dynamic = definedExternally, // Material | Array<Material>
) : Object3D {
    var geometry: BufferGeometry
    var material: dynamic
}

/** Base light. */
open external class Light : Object3D {
    val color: Color
    var intensity: Double

    /** Frees the light's shadow map, if it has one. */
    fun dispose()
}

/** A light's shadow: the size of its map in texels, and the map itself once a frame rendered it. */
external interface LightShadow {
    val mapSize: Vector2

    /** The render target three.js made at the map's size: disposed and set to null, it is made anew at a new size. */
    var map: dynamic
}

/** Ambient light — equal in all directions, no shadows. */
external class AmbientLight(
    color: dynamic = definedExternally,
    intensity: Double = definedExternally,
) : Light

/** Directional light — parallel rays like the sun. */
external class DirectionalLight(
    color: dynamic = definedExternally,
    intensity: Double = definedExternally,
) : Light {
    val target: Object3D
    val shadow: LightShadow
}

/** Point light — emits in all directions from a point. */
external class PointLight(
    color: dynamic = definedExternally,
    intensity: Double = definedExternally,
    distance: Double = definedExternally,
    decay: Double = definedExternally,
) : Light {
    var distance: Double
    var decay: Double
    val shadow: LightShadow
}

/** Spot light — a cone from its position towards its [target]. */
external class SpotLight(
    color: dynamic = definedExternally,
    intensity: Double = definedExternally,
    distance: Double = definedExternally,
    angle: Double = definedExternally,
    penumbra: Double = definedExternally,
    decay: Double = definedExternally,
) : Light {
    var distance: Double
    var decay: Double
    var angle: Double
    var penumbra: Double
    val target: Object3D
    val shadow: LightShadow
}

/** Hemisphere light — gradient from sky to ground. */
external class HemisphereLight(
    skyColor: dynamic = definedExternally,
    groundColor: dynamic = definedExternally,
    intensity: Double = definedExternally,
) : Light {
    /** The color towards the ground. */
    val groundColor: Color
}

/** An image on a material. */
open external class Texture {
    /** How the texture repeats along u and v: a [TextureWrapping] value. */
    var wrapS: Int
    var wrapT: Int

    /** How a texel is magnified and minified: a [TextureFilter] value. */
    var magFilter: Int
    var minFilter: Int

    /** Whether the image is uploaded again before the next draw: set it after drawing into a canvas texture's canvas. */
    var needsUpdate: Boolean

    var anisotropy: Int

    fun dispose()
}

/** A texture drawn from a canvas. */
external class CanvasTexture(canvas: HTMLCanvasElement) : Texture

/** One hit of a [Raycaster]. */
external interface Intersection {
    val distance: Double
    val point: Vector3
    val `object`: Object3D
}

/** Casts a ray, e.g. from the camera through the pointer, and finds what it hits. */
external class Raycaster {
    /** The ray from [camera] through [coords], the pointer in normalized device coordinates (-1 to 1 each). */
    fun setFromCamera(coords: Vector2, camera: Camera)

    /** The hits on [objects] (and their subtrees if [recursive]), nearest first. */
    fun intersectObjects(objects: Array<Object3D>, recursive: Boolean = definedExternally): Array<Intersection>

    fun intersectObject(obj: Object3D, recursive: Boolean = definedExternally): Array<Intersection>
}

/** Texture wrapping constants (values taken from three.js). */
object TextureWrapping {
    const val RepeatWrapping: Int = 1000
    const val ClampToEdgeWrapping: Int = 1001
    const val MirroredRepeatWrapping: Int = 1002
}

/** Texture filter constants (values taken from three.js). */
object TextureFilter {
    const val NearestFilter: Int = 1003
    const val NearestMipmapNearestFilter: Int = 1004
    const val NearestMipmapLinearFilter: Int = 1005
    const val LinearFilter: Int = 1006
    const val LinearMipmapNearestFilter: Int = 1007
    const val LinearMipmapLinearFilter: Int = 1008
}

/** Material side constants (values taken from three.js). */
object MaterialSide {
    const val FrontSide: Int = 0
    const val BackSide: Int = 1
    const val DoubleSide: Int = 2
}
