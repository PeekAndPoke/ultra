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
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.ops.FrameClock
import io.peekandpoke.ultra.streams.ops.animationFrames
import io.peekandpoke.ultra.streams.steady
import kotlinx.browser.window
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.style
import org.w3c.dom.HTMLCanvasElement

/** The default of `paused`: one instance, so props stay equal across renders. */
private val NotPaused: Stream<Boolean> = steady(false)

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
    /** Milliseconds since the previous frame; 0 on the first frame and on the first after a pause. */
    val deltaMs: Double,
    /** Milliseconds of running time since the first frame: a pause does not count. */
    val elapsedMs: Double,
)

/**
 * Renders a Three.js scene into a canvas.
 *
 * Build the scene in [onReady]; advance animation state in [onFrame]; the component
 * calls `renderer.render(scene, camera)` automatically each frame.
 *
 * While [paused] holds true no frame is asked for at all (the scene, the camera and the WebGL context stay), e.g.
 * `paused = windowCtrl.hasFocus.map { !it }`. The scene is drawn once after [onReady] also when mounted paused.
 * Nothing else draws while paused: a `renderer.setSize` then leaves the canvas cleared until the next frame, so render
 * once by hand after resizing a paused scene. The frames come from [clock], by default the browser's animation frames,
 * shared with every other user of [FrameClock.animationFrames]. A new clock restarts the loop: keep a clock of your own
 * in a field, as one created inside `render` is a new one on every redraw.
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
    paused: Stream<Boolean> = NotPaused,
    clock: FrameClock = FrameClock.animationFrames,
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
        paused = paused,
        clock = clock,
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
        val paused: Stream<Boolean> = NotPaused,
        val clock: FrameClock = FrameClock.animationFrames,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val threeJsAddon: ThreeJsAddon? by subscribingTo(addons.threeJs)

    private var scene: Scene? = null
    private var camera: Camera? = null
    private var renderer: WebGLRenderer? = null
    private var loop: RenderLoop? = null
    private var unsubscribePaused: Unsubscribe? = null

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount { tryStart() }
            onUpdate { tryStart() }
            onNextProps { new, old ->
                when {
                    // A new clock: a new loop on it (the props are the new ones here)
                    new.clock !== old.clock -> loop?.let {
                        it.stop()
                        startLoop()
                    }

                    new.paused !== old.paused -> loop?.let { followPaused(it, new.paused) }
                }
            }
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

        // Drawn once at once, so a scene mounted paused is not left a blank canvas; the loop runs also after a
        // throwing first draw, as one bad frame does not stop it either
        drawOnceThenStart(draw = { newRenderer.render(newScene, newCamera) }, start = { startLoop() })
    }

    private fun startLoop() {
        val newLoop = RenderLoop(clock = props.clock) { delta, elapsed ->
            val s = scene
            val c = camera
            val r = renderer

            if (s != null && c != null && r != null) {
                props.onFrame(ThreeJsFrame(s, c, r, deltaMs = delta, elapsedMs = elapsed))
                r.render(s, c)
            }
        }

        loop = newLoop
        // Before the start, so a scene mounted paused asks for no frame at all
        followPaused(newLoop, props.paused)
        newLoop.start()
    }

    private fun followPaused(loop: RenderLoop, paused: Stream<Boolean>) {
        unsubscribePaused?.invoke()
        unsubscribePaused = paused.subscribeToStream { if (it) loop.pause() else loop.resume() }
    }

    private fun stop() {
        unsubscribePaused?.invoke()
        unsubscribePaused = null
        loop?.stop()
        loop = null
        renderer?.dispose()
        renderer = null
        scene = null
        camera = null
    }
}
