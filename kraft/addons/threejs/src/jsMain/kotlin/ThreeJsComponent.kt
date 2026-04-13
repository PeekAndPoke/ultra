package io.peekandpoke.kraft.addons.threejs

import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.threejs.js.Camera
import io.peekandpoke.kraft.addons.threejs.js.Scene
import io.peekandpoke.kraft.addons.threejs.js.WebGLRenderer
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.style
import org.w3c.dom.HTMLCanvasElement

/** Callback data passed to [ThreeJsComponent.Props.onReady]. */
class ThreeJsContext(
    val scene: Scene,
    val camera: Camera,
    val renderer: WebGLRenderer,
)

/** Per-frame callback data. */
class ThreeJsFrame(
    val scene: Scene,
    val camera: Camera,
    val renderer: WebGLRenderer,
    /** Milliseconds since the previous frame. */
    val deltaMs: Double,
    /** Milliseconds since the first frame. */
    val elapsedMs: Double,
)

/**
 * Renders a Three.js scene into a canvas.
 *
 * Build the scene in [onReady]; advance animation state in [onFrame]; the component
 * calls `renderer.render(scene, camera)` automatically each frame.
 */
@Suppress("FunctionName")
fun Tag.ThreeJs(
    onReady: (ThreeJsContext) -> Unit,
    onFrame: (ThreeJsFrame) -> Unit = {},
    createCamera: ((aspect: Double) -> Camera)? = null,
    antialias: Boolean = true,
    alpha: Boolean = false,
    clearColor: Int? = null,
    width: Int? = null,
    height: Int? = null,
) = comp(
    ThreeJsComponent.Props(
        onReady = onReady,
        onFrame = onFrame,
        createCamera = createCamera,
        antialias = antialias,
        alpha = alpha,
        clearColor = clearColor,
        width = width,
        height = height,
    )
) {
    ThreeJsComponent(it)
}

/** Kraft component that wraps a Three.js canvas and manages the render loop. */
class ThreeJsComponent(ctx: Ctx<Props>) : Component<ThreeJsComponent.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val onReady: (ThreeJsContext) -> Unit,
        val onFrame: (ThreeJsFrame) -> Unit,
        val createCamera: ((aspect: Double) -> Camera)?,
        val antialias: Boolean,
        val alpha: Boolean,
        val clearColor: Int?,
        val width: Int?,
        val height: Int?,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val threeJsAddon: ThreeJsAddon? by subscribingTo(addons.threeJs)

    private var scene: Scene? = null
    private var camera: Camera? = null
    private var renderer: WebGLRenderer? = null
    private var rafHandle: Int? = null
    private var firstFrameTs: Double = 0.0
    private var lastFrameTs: Double = 0.0

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount { tryStart() }
            onUpdate { tryStart() }
            onUnmount { stop() }
        }
    }

    override fun VDom.render() {
        canvas {
            style = when {
                props.width != null && props.height != null ->
                    "display: block; width: ${props.width}px; height: ${props.height}px;"

                else -> "display: block; width: 100%; height: 100%;"
            }
        }
    }

    private fun tryStart() {
        val addon = threeJsAddon ?: return
        if (renderer != null) return

        val canvas = dom as? HTMLCanvasElement ?: return

        val resolvedWidth = props.width ?: canvas.clientWidth.takeIf { it > 0 } ?: 300
        val resolvedHeight = props.height ?: canvas.clientHeight.takeIf { it > 0 } ?: 150
        val aspect = resolvedWidth.toDouble() / resolvedHeight.toDouble()

        val newScene = addon.createScene()
        val newCamera = props.createCamera?.invoke(aspect)
            ?: addon.createPerspectiveCamera(fov = 75.0, aspect = aspect, near = 0.1, far = 1000.0)
                .also { it.position.z = 5.0 }

        val newRenderer = addon.createWebGLRenderer(jsObject {
            this.canvas = canvas
            this.antialias = props.antialias
            this.alpha = props.alpha
        })
        newRenderer.setPixelRatio(window.devicePixelRatio)
        newRenderer.setSize(resolvedWidth, resolvedHeight, updateStyle = false)
        props.clearColor?.let { newRenderer.setClearColor(it, alpha = if (props.alpha) 0.0 else 1.0) }

        scene = newScene
        camera = newCamera
        renderer = newRenderer

        props.onReady(ThreeJsContext(newScene, newCamera, newRenderer))

        startLoop()
    }

    private fun startLoop() {
        firstFrameTs = 0.0
        lastFrameTs = 0.0

        fun frame(ts: Double) {
            val s = scene
            val c = camera
            val r = renderer
            if (s == null || c == null || r == null) return

            if (firstFrameTs == 0.0) firstFrameTs = ts
            val delta = if (lastFrameTs == 0.0) 0.0 else ts - lastFrameTs
            lastFrameTs = ts

            props.onFrame(ThreeJsFrame(s, c, r, deltaMs = delta, elapsedMs = ts - firstFrameTs))
            r.render(s, c)

            rafHandle = window.requestAnimationFrame(::frame)
        }

        rafHandle = window.requestAnimationFrame(::frame)
    }

    private fun stop() {
        rafHandle?.let { window.cancelAnimationFrame(it) }
        rafHandle = null
        renderer?.dispose()
        renderer = null
        scene = null
        camera = null
    }
}
