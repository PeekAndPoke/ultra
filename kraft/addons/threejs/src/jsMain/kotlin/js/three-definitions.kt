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

    fun add(obj: Object3D): Object3D
    fun remove(obj: Object3D): Object3D
    fun clear(): Object3D
    fun lookAt(x: Double, y: Double, z: Double)
    fun traverse(callback: (Object3D) -> Unit)
}

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
    fun render(scene: Scene, camera: Camera)
    fun dispose()
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
}

/** Hemisphere light — gradient from sky to ground. */
external class HemisphereLight(
    skyColor: dynamic = definedExternally,
    groundColor: dynamic = definedExternally,
    intensity: Double = definedExternally,
) : Light

/** Material side constants (values taken from three.js). */
object MaterialSide {
    const val FrontSide: Int = 0
    const val BackSide: Int = 1
    const val DoubleSide: Int = 2
}
