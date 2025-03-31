/*
Generated from konva.d.ts with ts2kt command line tool.
Afterwards, commented out @JsQualifier and added @JsModule annotation
to make it work.
 */

@file:Suppress(
    "INTERFACE_WITH_SUPERCLASS",
    "OVERRIDING_FINAL_MEMBER",
    "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
    "CONFLICTING_OVERLOADS",
    "EXTERNAL_DELEGATION",
    "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "PackageDirectoryMismatch",
    "unused",
    "FunctionName",
    "ClassName",
    "PropertyName",
    "Detekt:TooManyFunctions",
    "Detekt:LongParameterList",
    "Detekt:ParameterListWrapping",
)

@file:JsModule("konva")
@file:JsNonModule
@file:JsQualifier("default")

package konva

import org.w3c.dom.*
import org.w3c.dom.events.Event

external var pixelRatio: Number = definedExternally
external var dragDistance: Number = definedExternally
external var isDragging: () -> Boolean = definedExternally
external var isDragReady: () -> Boolean = definedExternally
external var DD: Any = definedExternally

open external class Util {
    companion object {
        fun getRandomColor(): String = definedExternally
        fun getRGB(color: String): String = definedExternally
    }
}

open external class Easings {
    companion object {
        fun BackEaseIn(): Any = definedExternally
        fun BackEaseInOut(): Any = definedExternally
        fun BackEaseOut(): Any = definedExternally
        fun BounceEaseIn(): Any = definedExternally
        fun BounceEaseInOut(): Any = definedExternally
        fun BounceEaseOut(): Any = definedExternally
        fun EaseIn(): Any = definedExternally
        fun EaseInOut(): Any = definedExternally
        fun EaseOut(): Any = definedExternally
        fun ElasticEaseIn(): Any = definedExternally
        fun ElasticEaseInOut(): Any = definedExternally
        fun ElasticEaseOut(): Any = definedExternally
        fun Linear(): Any = definedExternally
        fun StrongEaseIn(): Any = definedExternally
        fun StrongEaseInOut(): Any = definedExternally
        fun StrongEaseOut(): Any = definedExternally
    }
}

open external class Filter
open external class Filters {
    companion object {
        fun Blur(imageData: Any): Filter = definedExternally
        fun Brighten(imageData: Any): Filter = definedExternally
        fun Emboss(imageData: Any): Filter = definedExternally
        fun Enhance(imageData: Any): Filter = definedExternally
        fun Grayscale(imageData: Any): Filter = definedExternally
        fun HSV(imageData: Any): Filter = definedExternally
        fun Invert(imageData: Any): Filter = definedExternally
        fun Mask(imageData: Any): Filter = definedExternally
        fun Noise(imageData: Any): Filter = definedExternally
        fun Pixelate(imageData: Any): Filter = definedExternally
        fun Posterize(imageData: Any): Filter = definedExternally
        fun RGB(imageData: Any): Filter = definedExternally
        fun RGA(imageData: Any): Filter = definedExternally
        fun Sepia(imageData: Any): Filter = definedExternally
        fun Solarize(imageData: Any): Filter = definedExternally
        fun Threshold(imageData: Any): Filter = definedExternally
        fun Contrast(imageData: Any): Filter = definedExternally
    }
}

open external class Animation {
    constructor(func: Function<*>, layers: Array<Layer>? = definedExternally /* null */)
    constructor(func: Function<*>, layer: Layer? = definedExternally /* null */)

    open fun addLayer(layer: Layer): Boolean = definedExternally
    open fun getLayers(): Array<Layer> = definedExternally
    open fun isRunning(): Boolean = definedExternally
    open fun setLayers(layers: Array<Layer>): Animation = definedExternally
    open fun setLayers(layer: Layer): Animation = definedExternally
    open fun start(): Animation = definedExternally
    open fun stop(): Animation = definedExternally
}

external interface NodeConfig {
    var x: Number? get() = definedExternally; set(value) = definedExternally
    var y: Number? get() = definedExternally; set(value) = definedExternally
    var width: Number? get() = definedExternally; set(value) = definedExternally
    var height: Number? get() = definedExternally; set(value) = definedExternally
    var visible: Boolean? get() = definedExternally; set(value) = definedExternally
    var listening: Boolean? get() = definedExternally; set(value) = definedExternally
    var id: String? get() = definedExternally; set(value) = definedExternally
    var name: String? get() = definedExternally; set(value) = definedExternally
    var opacity: Number? get() = definedExternally; set(value) = definedExternally
    var scale: Vector2d? get() = definedExternally; set(value) = definedExternally
    var scaleX: Number? get() = definedExternally; set(value) = definedExternally
    var scaleY: Number? get() = definedExternally; set(value) = definedExternally
    var rotation: Number? get() = definedExternally; set(value) = definedExternally
    var rotationDeg: Number? get() = definedExternally; set(value) = definedExternally
    var offset: Vector2d? get() = definedExternally; set(value) = definedExternally
    var offsetX: Number? get() = definedExternally; set(value) = definedExternally
    var offsetY: Number? get() = definedExternally; set(value) = definedExternally
    var draggable: Boolean? get() = definedExternally; set(value) = definedExternally
    var dragDistance: Number? get() = definedExternally; set(value) = definedExternally
    var dragBoundFunc: ((pos: Vector2d) -> Vector2d)? get() = definedExternally; set(value) = definedExternally
    var preventDefault: Boolean? get() = definedExternally; set(value) = definedExternally
    var globalCompositeOperation: dynamic /* String /* "" */ | String /* "source-over" */ | String /* "source-in" */ | String /* "source-out" */ | String /* "source-atop" */ | String /* "destination-over" */ | String /* "destination-in" */ | String /* "destination-out" */ | String /* "destination-atop" */ | String /* "lighter" */ | String /* "copy" */ | String /* "xor" */ | String /* "multiply" */ | String /* "screen" */ | String /* "overlay" */ | String /* "darken" */ | String /* "lighten" */ | String /* "color-dodge" */ | String /* "color-burn" */ | String /* "hard-light" */ | String /* "soft-light" */ | String /* "difference" */ | String /* "exclusion" */ | String /* "hue" */ | String /* "saturation" */ | String /* "color" */ | String /* "luminosity" */ */ get() = definedExternally; set(value) = definedExternally
}

external interface SizeConfig {
    var x: Number? get() = definedExternally; set(value) = definedExternally
    var y: Number? get() = definedExternally; set(value) = definedExternally
    var width: Number? get() = definedExternally; set(value) = definedExternally
    var height: Number? get() = definedExternally; set(value) = definedExternally
}

external interface ToDataURLConfig : SizeConfig {
    var callback: Function<*>
    var mimeType: String? get() = definedExternally; set(value) = definedExternally
    var quality: Number? get() = definedExternally; set(value) = definedExternally
}

external interface CacheConfig : SizeConfig {
    var drawBorder: Boolean? get() = definedExternally; set(value) = definedExternally
    var pixelRatio: Number? get() = definedExternally; set(value) = definedExternally
}

external interface ClearConfig : SizeConfig
external interface `T$0` {
    var width: Number
    var height: Number
}

external interface `T$1` {
    var target: Shape
    var evt: Event
    var currentTarget: Node
}

open external class Node(config: NodeConfig) {
    open fun preventDefault(): Boolean = definedExternally
    open fun preventDefault(preventDefault: Boolean): Node /* this */ = definedExternally
    open fun addName(name: String): Node = definedExternally
    open fun blue(): Number = definedExternally
    open fun blue(blue: Number): Node /* this */ = definedExternally
    open fun brightness(): Number = definedExternally
    open fun brightness(brightness: Number): Node /* this */ = definedExternally
    open fun contrast(): Number = definedExternally
    open fun contrast(contrast: Number): Node /* this */ = definedExternally
    open fun blurRadius(): Number = definedExternally
    open fun blurRadius(radius: Number): Node /* this */ = definedExternally
    open fun cache(config: CacheConfig? = definedExternally /* null */): Node /* this */ = definedExternally
    open fun clearCache(): Node /* this */ = definedExternally
    open fun clear(bounds: ClearConfig? = definedExternally /* null */): Node /* this */ = definedExternally
    open fun clone(attrs: NodeConfig? = definedExternally /* null */): Node /* this */ = definedExternally
    open fun destroy(): Unit = definedExternally
    open fun dragBoundFunc(): (pos: Vector2d) -> Vector2d = definedExternally
    open fun dragBoundFunc(dragBoundFunc: (pos: Vector2d) -> Vector2d): Node /* this */ = definedExternally
    open fun draggable(): Boolean = definedExternally
    open fun draggable(draggable: Boolean): Node /* this */ = definedExternally
    open fun draw(): Node /* this */ = definedExternally
    open fun embossBlend(): Boolean = definedExternally
    open fun embossBlend(embossBlend: Boolean): Node /* this */ = definedExternally
    open fun embossDirection(): String = definedExternally
    open fun embossDirection(embossDirection: String): Node /* this */ = definedExternally
    open fun embossStrength(): Number = definedExternally
    open fun embossStrength(level: Number): Node /* this */ = definedExternally
    open fun embossWhiteLevel(): Number = definedExternally
    open fun embossWhiteLevel(embossWhiteLevel: Number): Node /* this */ = definedExternally
    open fun enhance(): Number = definedExternally
    open fun enhance(enhance: Number): Node /* this */ = definedExternally
    open fun filters(): Array<Filter> = definedExternally
    open fun filters(filters: Filter): Node /* this */ = definedExternally
    open fun findAncestor(
        selector: String? = definedExternally, /* null */
        includeSelf: Boolean? = definedExternally, /* null */
        stopNode: Node? = definedExternally, /* null */
    ): Node /* this */ = definedExternally

    open fun findAncestors(
        selector: String? = definedExternally, /* null */
        includeSelf: Boolean? = definedExternally, /* null */
        stopNode: Node? = definedExternally, /* null */
    ): Array<Node> = definedExternally

    open fun fire(
        eventType: String, evt: Any? = definedExternally, /* null */ bubble: Boolean? = definedExternally, /* null */
    ): Node /* this */ = definedExternally

    open fun getAbsoluteOpacity(): Number = definedExternally
    open fun getAbsolutePosition(): Vector2d = definedExternally
    open fun getAbsoluteTransform(): Transform = definedExternally
    open fun getAbsoluteZIndex(): Number = definedExternally
    open fun getAncestors(): Collection = definedExternally
    open fun getAttr(attr: String): Any = definedExternally
    open fun getAttrs(): NodeConfig = definedExternally
    open fun getCanvas(): Canvas = definedExternally
    open fun getClassName(): String = definedExternally
    open fun getClientRect(): SizeConfig = definedExternally
    open fun getContext(): Context = definedExternally
    open fun getDepth(): Number = definedExternally
    open fun getHeight(): Number = definedExternally
    open fun getHitCanvas(): Canvas = definedExternally
    open fun getLayer(): Layer = definedExternally
    open fun getParent(): Container = definedExternally
    open fun getPosition(): Vector2d = definedExternally
    open fun getSize(): `T$0` = definedExternally
    open fun getStage(): Stage = definedExternally
    open fun getTransform(): Transform = definedExternally
    open fun getType(): String = definedExternally
    open fun getWidth(): Number = definedExternally
    open fun getZIndex(): Number = definedExternally
    open fun green(): Number = definedExternally
    open fun green(green: Number): Node /* this */ = definedExternally
    open fun hasName(name: String): Boolean = definedExternally
    open fun height(): Number = definedExternally
    open fun height(height: Number): Node /* this */ = definedExternally
    open fun hide(): Unit = definedExternally
    open fun hue(): Number = definedExternally
    open fun hue(hue: Number): Node /* this */ = definedExternally
    open fun id(): String = definedExternally
    open fun id(id: String): Node /* this */ = definedExternally
    open fun isDragging(): Boolean = definedExternally
    open fun isListening(): Boolean = definedExternally
    open fun isVisible(): Boolean = definedExternally
    open fun kaleidoscopeAngle(): Number = definedExternally
    open fun kaleidoscopeAngle(kaleidoscopeAngle: Number): Node /* this */ = definedExternally
    open fun kaleidoscopePower(): Number = definedExternally
    open fun kaleidoscopePower(kaleidoscopePower: Number): Node /* this */ = definedExternally
    open fun levels(): Number = definedExternally
    open fun levels(levels: Number): Node /* this */ = definedExternally
    open fun listening(): Any = definedExternally
    open fun listening(listening: Boolean): Node /* this */ = definedExternally
    open fun listening(listening: String): Node /* this */ = definedExternally
    open fun move(move: Vector2d): Node /* this */ = definedExternally
    open fun moveDown(): Boolean = definedExternally
    open fun moveTo(newContainer: Container): Node /* this */ = definedExternally
    open fun moveToBottom(): Boolean = definedExternally
    open fun moveToTop(): Boolean = definedExternally
    open fun moveUp(): Boolean = definedExternally
    open fun name(): String = definedExternally
    open fun name(name: String): Node /* this */ = definedExternally
    open fun noise(): Number = definedExternally
    open fun noise(noise: Number): Node /* this */ = definedExternally
    open fun off(evtStr: String): Node /* this */ = definedExternally
    open fun offset(): Vector2d = definedExternally
    open fun offset(offset: Vector2d): Node /* this */ = definedExternally
    open fun offsetX(): Number = definedExternally
    open fun offsetX(offsetX: Number): Node /* this */ = definedExternally
    open fun offsetY(): Number = definedExternally
    open fun offsetY(offsetY: Number): Node /* this */ = definedExternally
    open fun on(evtStr: String, handler: (e: `T$1`) -> Unit): Node /* this */ = definedExternally
    open fun opacity(): Number = definedExternally
    open fun opacity(opacity: Number): Node /* this */ = definedExternally
    open fun pixelSize(): Number = definedExternally
    open fun pixelSize(pixelSize: Number): Node /* this */ = definedExternally
    open fun position(): Vector2d = definedExternally
    open fun position(position: Vector2d): Node /* this */ = definedExternally
    open fun red(): Number = definedExternally
    open fun red(red: Number): Node /* this */ = definedExternally
    open fun remove(): Node /* this */ = definedExternally
    open fun removeName(name: String): Node /* this */ = definedExternally
    open fun rotate(theta: Number): Node /* this */ = definedExternally
    open fun rotation(): Number = definedExternally
    open fun rotation(rotation: Number): Node /* this */ = definedExternally
    open fun saturation(): Number = definedExternally
    open fun saturation(saturation: Number): Node /* this */ = definedExternally
    open fun scale(): Vector2d = definedExternally
    open fun scale(scale: Vector2d): Node /* this */ = definedExternally
    open fun scaleX(): Number = definedExternally
    open fun scaleX(scaleX: Number): Node /* this */ = definedExternally
    open fun scaleY(): Number = definedExternally
    open fun scaleY(scaleY: Number): Node /* this */ = definedExternally
    open fun setAbsolutePosition(pos: Vector2d): Node /* this */ = definedExternally
    open fun setAttr(attr: String, value: Any): Node /* this */ = definedExternally
    open fun setAttrs(attrs: NodeConfig): Unit = definedExternally
    open fun setId(id: String): Node /* this */ = definedExternally
    open fun setSize(size: `T$0`): Node /* this */ = definedExternally
    open fun setZIndex(zIndex: Number): Unit = definedExternally
    open fun shouldDrawHit(): Boolean = definedExternally
    open fun show(): Node /* this */ = definedExternally
    open fun skew(): Vector2d = definedExternally
    open fun skew(skew: Vector2d): Node /* this */ = definedExternally
    open fun skewX(): Number = definedExternally
    open fun skewX(skewX: Number): Node /* this */ = definedExternally
    open fun skewY(): Number = definedExternally
    open fun skewY(skewY: Number): Node /* this */ = definedExternally
    open fun startDrag(): Unit = definedExternally
    open fun stopDrag(): Unit = definedExternally
    open fun threshold(): Number = definedExternally
    open fun threshold(threshold: Number): Node /* this */ = definedExternally
    open fun to(params: Any): Unit = definedExternally
    open fun toDataURL(config: ToDataURLConfig): String = definedExternally
    open fun toImage(config: ToDataURLConfig): HTMLImageElement = definedExternally
    open fun toJSON(): String = definedExternally
    open fun toObject(): Any = definedExternally
    open fun transformsEnabled(): String = definedExternally
    open fun transformsEnabled(transformsEnabled: String): Node /* this */ = definedExternally
    open fun value(): Number = definedExternally
    open fun value(value: Number): Node /* this */ = definedExternally
    open fun visible(): Any = definedExternally
    open fun visible(visible: Boolean): Node /* this */ = definedExternally
    open fun visible(visible: String): Node /* this */ = definedExternally
    open fun width(): Number = definedExternally
    open fun width(width: Number): Node /* this */ = definedExternally
    open fun x(): Number = definedExternally
    open fun x(x: Number): Node /* this */ = definedExternally
    open fun y(): Number = definedExternally
    open fun y(y: Number): Node /* this */ = definedExternally
    open fun globalCompositeOperation(): dynamic /* String /* "" */ | String /* "source-over" */ | String /* "source-in" */ | String /* "source-out" */ | String /* "source-atop" */ | String /* "destination-over" */ | String /* "destination-in" */ | String /* "destination-out" */ | String /* "destination-atop" */ | String /* "lighter" */ | String /* "copy" */ | String /* "xor" */ | String /* "multiply" */ | String /* "screen" */ | String /* "overlay" */ | String /* "darken" */ | String /* "lighten" */ | String /* "color-dodge" */ | String /* "color-burn" */ | String /* "hard-light" */ | String /* "soft-light" */ | String /* "difference" */ | String /* "exclusion" */ | String /* "hue" */ | String /* "saturation" */ | String /* "color" */ | String /* "luminosity" */ */ =
        definedExternally

    open fun globalCompositeOperation(type: dynamic /* String /* "" */ | String /* "source-over" */ | String /* "source-in" */ | String /* "source-out" */ | String /* "source-atop" */ | String /* "destination-over" */ | String /* "destination-in" */ | String /* "destination-out" */ | String /* "destination-atop" */ | String /* "lighter" */ | String /* "copy" */ | String /* "xor" */ | String /* "multiply" */ | String /* "screen" */ | String /* "overlay" */ | String /* "darken" */ | String /* "lighten" */ | String /* "color-dodge" */ | String /* "color-burn" */ | String /* "hard-light" */ | String /* "soft-light" */ | String /* "difference" */ | String /* "exclusion" */ | String /* "hue" */ | String /* "saturation" */ | String /* "color" */ | String /* "luminosity" */ */): Node /* this */ =
        definedExternally

    companion object {
        fun <T> create(data: Any, container: HTMLElement? = definedExternally /* null */): T = definedExternally
    }
}

external interface ContainerConfig : NodeConfig {
    var clearBeforeDraw: Boolean? get() = definedExternally; set(value) = definedExternally
    var clipFunc: ((ctx: CanvasRenderingContext2D) -> Unit)? get() = definedExternally; set(value) = definedExternally
    var clipX: Number? get() = definedExternally; set(value) = definedExternally
    var clipY: Number? get() = definedExternally; set(value) = definedExternally
    var clipWidth: Number? get() = definedExternally; set(value) = definedExternally
    var clipHeight: Number? get() = definedExternally; set(value) = definedExternally
}

open external class Container(params: ContainerConfig? = definedExternally /* null */) : Node {
    open fun add(vararg children: Node): Container /* this */ = definedExternally
    open fun getChildren(filterfunc: Function<*>? = definedExternally /* null */): Collection = definedExternally
    open fun clip(): SizeConfig = definedExternally
    open fun clip(clip: SizeConfig?): Container /* this */ = definedExternally
    open fun clipHeight(): Number = definedExternally
    open fun clipHeight(clipHeight: Number?): Container /* this */ = definedExternally
    open fun clipWidth(): Number = definedExternally
    open fun clipWidth(clipWidth: Number?): Container /* this */ = definedExternally
    open fun clipX(): Number = definedExternally
    open fun clipX(clipX: Number?): Container /* this */ = definedExternally
    open fun clipY(): Number = definedExternally
    open fun clipY(clipY: Number?): Container /* this */ = definedExternally
    open fun clipFunc(): (ctx: CanvasRenderingContext2D) -> Unit = definedExternally
    open fun clipFunc(ctx: CanvasRenderingContext2D?): Unit = definedExternally
    open fun destroyChildren(): Unit = definedExternally
    open fun find(selector: String? = definedExternally /* null */): Collection = definedExternally
    open fun find(selector: ((node: Node) -> Boolean)? = definedExternally /* null */): Collection = definedExternally
    open fun <T : Node> findOne(selector: String): T = definedExternally
    open fun <T : Node> findOne(selector: (node: Node) -> Boolean): T = definedExternally
    open fun getAllIntersections(pos: Vector2d): Array<Shape> = definedExternally
    open fun hasChildren(): Boolean = definedExternally
    open fun removeChildren(): Unit = definedExternally
    open fun find(): Collection = definedExternally
}

external interface ShapeConfig : NodeConfig {
    var fill: String? get() = definedExternally; set(value) = definedExternally
    var fillPatternImage: HTMLImageElement? get() = definedExternally; set(value) = definedExternally
    var fillPatternX: Number? get() = definedExternally; set(value) = definedExternally
    var fillPatternY: Number? get() = definedExternally; set(value) = definedExternally
    var fillPatternOffset: Vector2d? get() = definedExternally; set(value) = definedExternally
    var fillPatternOffsetX: Number? get() = definedExternally; set(value) = definedExternally
    var fillPatternOffsetY: Number? get() = definedExternally; set(value) = definedExternally
    var fillPatternScale: Vector2d? get() = definedExternally; set(value) = definedExternally
    var fillPatternScaleX: Number? get() = definedExternally; set(value) = definedExternally
    var fillPatternScaleY: Number? get() = definedExternally; set(value) = definedExternally
    var fillPatternRotation: Number? get() = definedExternally; set(value) = definedExternally
    var fillPatternRepeat: String? get() = definedExternally; set(value) = definedExternally
    var fillLinearGradientStartPoint: Vector2d? get() = definedExternally; set(value) = definedExternally
    var fillLinearGradientStartPointX: Number? get() = definedExternally; set(value) = definedExternally
    var fillLinearGradientStartPointY: Number? get() = definedExternally; set(value) = definedExternally
    var fillLinearGradientEndPoint: Vector2d? get() = definedExternally; set(value) = definedExternally
    var fillLinearGradientEndPointX: Number? get() = definedExternally; set(value) = definedExternally
    var fillLinearGradientEndPointY: Number? get() = definedExternally; set(value) = definedExternally
    var fillLinearGradientColorStops: Array<dynamic /* String | Number */>? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientStartPoint: Vector2d? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientStartPointX: Number? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientStartPointY: Number? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientEndPoint: Vector2d? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientEndPointX: Number? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientEndPointY: Number? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientStartRadius: Number? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientEndRadius: Number? get() = definedExternally; set(value) = definedExternally
    var fillRadialGradientColorStops: Array<dynamic /* String | Number */>? get() = definedExternally; set(value) = definedExternally
    var fillEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var fillPriority: String? get() = definedExternally; set(value) = definedExternally
    var stroke: String? get() = definedExternally; set(value) = definedExternally
    var strokeWidth: Number? get() = definedExternally; set(value) = definedExternally
    var strokeScaleEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var strokeHitEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var strokeEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var lineJoin: String? get() = definedExternally; set(value) = definedExternally
    var lineCap: String? get() = definedExternally; set(value) = definedExternally
    var sceneFunc: ((con: Context) -> Unit)? get() = definedExternally; set(value) = definedExternally
    var hitFunc: ((con: Context) -> Unit)? get() = definedExternally; set(value) = definedExternally
    var shadowColor: String? get() = definedExternally; set(value) = definedExternally
    var shadowBlur: Number? get() = definedExternally; set(value) = definedExternally
    var shadowOffset: Vector2d? get() = definedExternally; set(value) = definedExternally
    var shadowOffsetX: Number? get() = definedExternally; set(value) = definedExternally
    var shadowOffsetY: Number? get() = definedExternally; set(value) = definedExternally
    var shadowOpacity: Number? get() = definedExternally; set(value) = definedExternally
    var shadowEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var shadowForStrokeEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var dash: Array<Number>? get() = definedExternally; set(value) = definedExternally
    var dashEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var perfectDrawEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
}

open external class Shape(ShapeConfig: ShapeConfig) : Node {
    open fun dash(): Array<Number> = definedExternally
    open fun dash(dash: Array<Number>): Shape /* this */ = definedExternally
    open fun dashEnabled(): Boolean = definedExternally
    open fun dashEnabled(dashEnabled: Boolean): Shape /* this */ = definedExternally
    open fun drawHitFromCache(alphaThreshold: Number): Shape /* this */ = definedExternally
    open fun fill(): String = definedExternally
    open fun fill(fill: String): Shape /* this */ = definedExternally
    open fun fillEnabled(): Boolean = definedExternally
    open fun fillEnabled(fillEnabled: Boolean): Shape /* this */ = definedExternally
    open fun fillLinearGradientColorStops(): Array<dynamic /* String | Number */> = definedExternally
    open fun fillLinearGradientColorStops(colors: Array<dynamic /* String | Number */>): Shape /* this */ =
        definedExternally

    open fun fillLinearGradientStartPoint(): Vector2d = definedExternally
    open fun fillLinearGradientStartPoint(point: Vector2d): Vector2d = definedExternally
    open fun fillLinearGradientStartPointX(): Number = definedExternally
    open fun fillLinearGradientStartPointX(x: Number): Shape /* this */ = definedExternally
    open fun fillLinearGradientStartPointY(): Number = definedExternally
    open fun fillLinearGradientStartPointY(y: Number): Shape /* this */ = definedExternally
    open fun fillLinearGradientEndPoint(): Vector2d = definedExternally
    open fun fillLinearGradientEndPoint(point: Vector2d): Shape /* this */ = definedExternally
    open fun fillLinearGradientEndPointX(): Number = definedExternally
    open fun fillLinearGradientEndPointX(x: Number): Shape /* this */ = definedExternally
    open fun fillLinearGradientEndPointY(): Number = definedExternally
    open fun fillLinearGradientEndPointY(y: Number): Shape /* this */ = definedExternally
    open fun fillLinearRadialStartPoint(): Vector2d = definedExternally
    open fun fillLinearRadialStartPoint(point: Vector2d): Shape /* this */ = definedExternally
    open fun fillLinearRadialStartPointX(): Number = definedExternally
    open fun fillLinearRadialStartPointX(x: Number): Shape /* this */ = definedExternally
    open fun fillLinearRadialStartPointY(): Number = definedExternally
    open fun fillLinearRadialStartPointY(y: Number): Shape /* this */ = definedExternally
    open fun fillLinearRadialEndPoint(): Vector2d = definedExternally
    open fun fillLinearRadialEndPoint(point: Vector2d): Vector2d = definedExternally
    open fun fillLinearRadialEndPointX(): Number = definedExternally
    open fun fillLinearRadialEndPointX(x: Number): Shape /* this */ = definedExternally
    open fun fillLinearRadialEndPointY(): Number = definedExternally
    open fun fillLinearRadialEndPointY(y: Number): Shape /* this */ = definedExternally
    open fun fillPatternImage(): HTMLImageElement = definedExternally
    open fun fillPatternImage(image: HTMLImageElement): Shape /* this */ = definedExternally
    open fun fillRadialGradientStartRadius(): Number = definedExternally
    open fun fillRadialGradientStartRadius(radius: Number): Shape /* this */ = definedExternally
    open fun fillRadialGradientEndRadius(): Number = definedExternally
    open fun fillRadialGradientEndRadius(radius: Number): Shape /* this */ = definedExternally
    open fun fillRadialGradientColorStops(): Array<dynamic /* String | Number */> = definedExternally
    open fun fillRadialGradientColorStops(color: Array<dynamic /* String | Number */>): Shape /* this */ =
        definedExternally

    open fun fillPatternOffset(): Vector2d = definedExternally
    open fun fillPatternOffset(offset: Vector2d): Shape /* this */ = definedExternally
    open fun fillPatternOffsetX(): Number = definedExternally
    open fun fillPatternOffsetX(x: Number): Shape /* this */ = definedExternally
    open fun fillPatternOffsetY(): Number = definedExternally
    open fun fillPatternOffsetY(y: Number): Shape /* this */ = definedExternally
    open fun fillPatternRepeat(): String = definedExternally
    open fun fillPatternRepeat(repeat: String): Shape /* this */ = definedExternally
    open fun fillPatternRotation(): Number = definedExternally
    open fun fillPatternRotation(rotation: Number): Shape /* this */ = definedExternally
    open fun fillPatternScale(): Vector2d = definedExternally
    open fun fillPatternScale(scale: Vector2d): Shape /* this */ = definedExternally
    open fun fillPatternScaleX(): Number = definedExternally
    open fun fillPatternScaleX(x: Number): Shape /* this */ = definedExternally
    open fun fillPatternScaleY(): Number = definedExternally
    open fun fillPatternScaleY(y: Number): Shape /* this */ = definedExternally
    open fun fillPatternX(): Number = definedExternally
    open fun fillPatternX(x: Number): Number = definedExternally
    open fun fillPatternY(): Number = definedExternally
    open fun fillPatternY(y: Number): Shape /* this */ = definedExternally
    open fun fillPriority(): String = definedExternally
    open fun fillPriority(priority: String): Shape /* this */ = definedExternally
    open fun hasFill(): Boolean = definedExternally
    open fun hasShadow(): Boolean = definedExternally
    open fun hasStroke(): Boolean = definedExternally
    open fun hitFunc(): Function<*> = definedExternally
    open fun hitFunc(func: Function<*>): Shape /* this */ = definedExternally
    open fun intersects(point: Vector2d): Boolean = definedExternally
    open fun lineCap(): String = definedExternally
    open fun lineCap(lineCap: String): Shape /* this */ = definedExternally
    open fun lineJoin(): String = definedExternally
    open fun lineJoin(lineJoin: String): Shape /* this */ = definedExternally
    open fun perfectDrawEnabled(): Boolean = definedExternally
    open fun perfectDrawEnabled(perfectDrawEnabled: Boolean): Shape /* this */ = definedExternally
    open fun sceneFunc(): Function<*> = definedExternally
    open fun sceneFunc(func: (con: Context) -> Any): Shape /* this */ = definedExternally
    open fun shadowColor(): String = definedExternally
    open fun shadowColor(shadowColor: String): Shape /* this */ = definedExternally
    open fun shadowEnabled(): Boolean = definedExternally
    open fun shadowEnabled(shadowEnabled: Boolean): Shape /* this */ = definedExternally
    open fun shadowForStrokeEnabled(): Boolean = definedExternally
    open fun shadowForStrokeEnabled(shadowForStrokeEnabled: Boolean): Shape /* this */ = definedExternally
    open fun shadowOffset(): Vector2d = definedExternally
    open fun shadowOffset(shadowOffset: Vector2d): Shape /* this */ = definedExternally
    open fun shadowOffsetX(): Number = definedExternally
    open fun shadowOffsetX(shadowOffsetX: Number): Shape /* this */ = definedExternally
    open fun shadowOffsetY(): Number = definedExternally
    open fun shadowOffsetY(shadowOffsetY: Number): Shape /* this */ = definedExternally
    open fun shadowOpacity(): Number = definedExternally
    open fun shadowOpacity(shadowOpacity: Number): Shape /* this */ = definedExternally
    open fun shadowBlur(): Number = definedExternally
    open fun shadowBlur(blur: Number): Shape /* this */ = definedExternally
    open fun stroke(): String = definedExternally
    open fun stroke(stroke: String): Shape /* this */ = definedExternally
    open fun strokeEnabled(): Boolean = definedExternally
    open fun strokeEnabled(strokeEnabled: Boolean): Shape /* this */ = definedExternally
    open fun strokeScaleEnabled(): Boolean = definedExternally
    open fun strokeScaleEnabled(strokeScaleEnabled: Boolean): Shape /* this */ = definedExternally
    open fun strokeHitEnabled(): Boolean = definedExternally
    open fun strokeHitEnabled(strokeHitEnabled: Boolean): Shape /* this */ = definedExternally
    open fun strokeWidth(): Number = definedExternally
    open fun strokeWidth(strokeWidth: Number): Shape /* this */ = definedExternally
}

external interface StageConfig : ContainerConfig {
    var container: Any
}

open external class Stage(StageConfig: StageConfig) : Container {
    open fun add(vararg layers: Layer): Stage /* this */ = definedExternally
    open fun add(vararg layers: FastLayer): Stage /* this */ = definedExternally
    open fun batchDraw(): Unit = definedExternally
    open fun container(): HTMLElement = definedExternally
    override fun destroy(): Unit = definedExternally
    open fun drawHit(): Unit = definedExternally
    open fun getIntersection(pos: Vector2d, selector: String? = definedExternally /* null */): Shape = definedExternally
    open fun getLayers(): Array<Layer> = definedExternally
    open fun getPointerPosition(): Vector2d = definedExternally
    open fun setContainer(con: HTMLElement): Unit = definedExternally
    open fun setHeight(height: Number): Unit = definedExternally
    open fun setWidth(width: Number): Unit = definedExternally
}

external interface LayerConfig : ContainerConfig {
    override var clearBeforeDraw: Boolean? get() = definedExternally; set(value) = definedExternally
    var hitGraphEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
}

open external class FastLayer(config: LayerConfig? = definedExternally /* null */) : Container {
    open fun drawScene(): Unit = definedExternally
    open fun hitGraphEnabled(value: Boolean): FastLayer /* this */ = definedExternally
    open fun batchDraw(): Unit = definedExternally
}

open external class Layer(config: LayerConfig? = definedExternally /* null */) : Container {
    open fun getIntersection(pos: Vector2d, selector: String? = definedExternally /* null */): Shape = definedExternally
    open fun enableHitGraph(): Layer /* this */ = definedExternally
    open fun disableHitGraph(): Layer /* this */ = definedExternally
    open fun clearBeforeDraw(): Boolean = definedExternally
    open fun clearBeforeDraw(value: Boolean): Layer /* this */ = definedExternally
    open fun hitGraphEnabled(): Boolean = definedExternally
    open fun hitGraphEnabled(value: Boolean): Layer /* this */ = definedExternally
    open fun batchDraw(): Unit = definedExternally
    open fun drawScene(): Unit = definedExternally
}

external interface GroupConfig : ContainerConfig
open external class Group(options: GroupConfig) : Container
external interface CanvasConfig {
    var width: Number
    var height: Number
    var pixelRatio: Number
}

open external class Canvas(CanvasConfig: CanvasConfig) {
    open fun getContext(): CanvasRenderingContext2D = definedExternally
    open fun getHeight(): Number = definedExternally
    open fun getWidth(): Number = definedExternally
    open fun getPixelRatio(): Number = definedExternally
    open fun setHeight(value: Number): Unit = definedExternally
    open fun setWidth(value: Number): Unit = definedExternally
    open fun setPixelRatio(value: Number): Unit = definedExternally
    open fun setSize(size: `T$0`): Unit = definedExternally
    open fun toDataURL(mimeType: String, quality: Number): String = definedExternally
    open var _canvas: HTMLElement = definedExternally
}

open external class Context {
    open fun clear(bounds: ClearConfig? = definedExternally /* null */): Context = definedExternally
    open fun clearTrace(): Unit = definedExternally
    open fun fillShape(shape: Shape): Unit = definedExternally
    open fun fillStrokeShape(shape: Shape): Unit = definedExternally
    open fun getCanvas(): Canvas = definedExternally
    open fun getTrace(relaxed: Boolean): String = definedExternally
    open fun reset(): Unit = definedExternally
    open fun setAttr(attr: String, value: Any): Unit = definedExternally
    open fun strokeShape(shape: Shape): Unit = definedExternally
    open fun arc(
        x: Number,
        y: Number,
        radius: Number,
        startAngle: Number,
        endAngle: Number,
        anticlockwise: Boolean? = definedExternally, /* null */
    ): Unit = definedExternally

    open fun beginPath(): Unit = definedExternally
    open fun bezierCurveTo(cp1x: Number, cp1y: Number, cp2x: Number, cp2y: Number, x: Number, y: Number): Unit =
        definedExternally

    open fun clearRect(x: Number, y: Number, width: Number, height: Number): Unit = definedExternally
    open fun clip(): Unit = definedExternally
    open fun closePath(): Unit = definedExternally
    open fun createImageData(imageDataOrSw: Number, sh: Number? = definedExternally /* null */): ImageData =
        definedExternally

    open fun createImageData(imageDataOrSw: ImageData, sh: Number? = definedExternally /* null */): ImageData =
        definedExternally

    open fun createLinearGradient(x0: Number, y0: Number, x1: Number, y1: Number): CanvasGradient = definedExternally
    open fun createPattern(image: HTMLImageElement, repetition: String): CanvasPattern = definedExternally
    open fun createPattern(image: HTMLCanvasElement, repetition: String): CanvasPattern = definedExternally
    open fun createPattern(image: HTMLVideoElement, repetition: String): CanvasPattern = definedExternally
    open fun createRadialGradient(
        x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number,
    ): CanvasGradient = definedExternally

    open fun drawImage(image: HTMLImageElement, dstX: Number, dstY: Number): Unit = definedExternally
    open fun drawImage(image: HTMLCanvasElement, dstX: Number, dstY: Number): Unit = definedExternally
    open fun drawImage(image: HTMLVideoElement, dstX: Number, dstY: Number): Unit = definedExternally
    open fun drawImage(image: ImageBitmap, dstX: Number, dstY: Number): Unit = definedExternally
    open fun drawImage(image: HTMLImageElement, dstX: Number, dstY: Number, dstW: Number, dstH: Number): Unit =
        definedExternally

    open fun drawImage(image: HTMLCanvasElement, dstX: Number, dstY: Number, dstW: Number, dstH: Number): Unit =
        definedExternally

    open fun drawImage(image: HTMLVideoElement, dstX: Number, dstY: Number, dstW: Number, dstH: Number): Unit =
        definedExternally

    open fun drawImage(image: ImageBitmap, dstX: Number, dstY: Number, dstW: Number, dstH: Number): Unit =
        definedExternally

    open fun drawImage(
        image: HTMLImageElement,
        srcX: Number,
        srcY: Number,
        srcW: Number,
        srcH: Number,
        dstX: Number,
        dstY: Number,
        dstW: Number,
        dstH: Number,
    ): Unit = definedExternally

    open fun drawImage(
        image: HTMLCanvasElement,
        srcX: Number,
        srcY: Number,
        srcW: Number,
        srcH: Number,
        dstX: Number,
        dstY: Number,
        dstW: Number,
        dstH: Number,
    ): Unit = definedExternally

    open fun drawImage(
        image: HTMLVideoElement,
        srcX: Number,
        srcY: Number,
        srcW: Number,
        srcH: Number,
        dstX: Number,
        dstY: Number,
        dstW: Number,
        dstH: Number,
    ): Unit = definedExternally

    open fun drawImage(
        image: ImageBitmap,
        srcX: Number,
        srcY: Number,
        srcW: Number,
        srcH: Number,
        dstX: Number,
        dstY: Number,
        dstW: Number,
        dstH: Number,
    ): Unit = definedExternally

    open fun isPointInPath(x: Number, y: Number): Boolean = definedExternally
    open fun fill(): Unit = definedExternally
    open fun fillRect(x: Number, y: Number, width: Number, height: Number): Unit = definedExternally
    open fun strokeRect(x: Number, y: Number, w: Number, h: Number): Unit = definedExternally
    open fun fillText(text: String, x: Number, y: Number): Unit = definedExternally
    open fun measureText(text: String): TextMetrics = definedExternally
    open fun getImageData(sx: Number, sy: Number, sw: Number, sh: Number): ImageData = definedExternally
    open fun lineTo(x: Number, y: Number): Unit = definedExternally
    open fun moveTo(x: Number, y: Number): Unit = definedExternally
    open fun rect(x: Number, y: Number, w: Number, h: Number): Unit = definedExternally
    open fun putImageData(imagedata: ImageData, dx: Number, dy: Number): Unit = definedExternally
    open fun quadraticCurveTo(cpx: Number, cpy: Number, x: Number, y: Number): Unit = definedExternally
    open fun restore(): Unit = definedExternally
    open fun rotate(angle: Number): Unit = definedExternally
    open fun save(): Unit = definedExternally
    open fun scale(x: Number, y: Number): Unit = definedExternally
    open fun setLineDash(segments: Array<Number>): Unit = definedExternally
    open fun getLineDash(): Array<Number> = definedExternally
    open fun setTransform(m11: Number, m12: Number, m21: Number, m22: Number, dx: Number, dy: Number): Unit =
        definedExternally

    open fun stroke(path: Path2D? = definedExternally /* null */): Unit = definedExternally
    open fun strokeText(text: String, x: Number, y: Number): Unit = definedExternally
    open fun transform(m11: Number, m12: Number, m21: Number, m22: Number, dx: Number, dy: Number): Unit =
        definedExternally

    open fun translate(x: Number, y: Number): Unit = definedExternally
}

open external class Tween(params: Any) {
    open fun destroy(): Unit = definedExternally
    open fun finish(): Tween = definedExternally
    open fun pause(): Tween = definedExternally
    open fun play(): Tween = definedExternally
    open fun reset(): Tween = definedExternally
    open fun reverse(): Tween = definedExternally
    open fun seek(t: Number): Tween = definedExternally
}

external interface RingConfig : ShapeConfig {
    var innerRadius: Number
    var outerRadius: Number
    var clockwise: Boolean? get() = definedExternally; set(value) = definedExternally
}

open external class Ring(RingConfig: RingConfig) : Shape {
    open fun innerRadius(): Number = definedExternally
    open fun innerRadius(innerRadius: Number): Ring /* this */ = definedExternally
    open fun outerRadius(): Number = definedExternally
    open fun outerRadius(outerRadius: Number): Ring /* this */ = definedExternally
}

external interface ArcConfig : ShapeConfig {
    var angle: Number
    var innerRadius: Number
    var outerRadius: Number
    var clockwise: Boolean? get() = definedExternally; set(value) = definedExternally
}

open external class Arc(ArcConfig: ArcConfig) : Shape {
    open fun angle(): Number = definedExternally
    open fun angle(angle: Number): Arc /* this */ = definedExternally
    open fun clockwise(): Boolean = definedExternally
    open fun clockwise(clockwise: Boolean): Arc /* this */ = definedExternally
    open fun innerRadius(): Number = definedExternally
    open fun innerRadius(innerRadius: Number): Arc /* this */ = definedExternally
    open fun outerRadius(): Number = definedExternally
    open fun outerRadius(outerRadius: Number): Arc /* this */ = definedExternally
}

external interface CircleConfig : ShapeConfig {
    var radius: Number
}

open external class Circle(CircleConfig: CircleConfig) : Shape {
    open fun radius(): Number = definedExternally
    open fun radius(radius: Number): Circle /* this */ = definedExternally
}

external interface EllipseConfig : ShapeConfig {
    var radius: Any
}

open external class Ellipse(EllipseConfig: EllipseConfig) : Shape {
    open fun radius(): Any = definedExternally
    open fun radius(radius: Any): Ellipse /* this */ = definedExternally
    open fun radiusX(): Number = definedExternally
    open fun radiusX(radiusX: Number): Ellipse /* this */ = definedExternally
    open fun radiusY(): Number = definedExternally
    open fun radiusY(radiusY: Number): Ellipse /* this */ = definedExternally
}

external interface ImageConfig : ShapeConfig {
    var image: HTMLImageElement
    var crop: SizeConfig? get() = definedExternally; set(value) = definedExternally
}

open external class Image(ImageConfig: ImageConfig) : Shape {
    open fun image(): HTMLImageElement = definedExternally
    open fun image(image: HTMLImageElement): Image /* this */ = definedExternally
    open fun crop(): SizeConfig = definedExternally
    open fun crop(crop: SizeConfig): Image /* this */ = definedExternally
    open fun cropX(): Number = definedExternally
    open fun cropX(cropX: Number): Image /* this */ = definedExternally
    open fun cropY(): Number = definedExternally
    open fun cropY(cropY: Number): Image /* this */ = definedExternally
    open fun cropWidth(): Number = definedExternally
    open fun cropWidth(cropWidth: Number): Image /* this */ = definedExternally
    open fun cropHeight(): Number = definedExternally
    open fun cropHeight(cropHeight: Number): Image /* this */ = definedExternally

    companion object {
        fun fromURL(url: String, callback: (image: Image) -> Unit): Unit = definedExternally
    }
}

external interface LineConfig : ShapeConfig {
    var points: Array<Number>
    var tension: Number? get() = definedExternally; set(value) = definedExternally
    var closed: Boolean? get() = definedExternally; set(value) = definedExternally
    var bezier: Boolean? get() = definedExternally; set(value) = definedExternally
}

open external class Line(LineConfig: LineConfig) : Shape {
    open fun closed(): Boolean = definedExternally
    open fun closed(closed: Boolean): Line /* this */ = definedExternally
    open fun tension(): Number = definedExternally
    open fun tension(tension: Number): Line /* this */ = definedExternally
    open fun points(): Array<Number> = definedExternally
    open fun points(points: Array<Number>): Line /* this */ = definedExternally
}

external interface ArrowConfig : ShapeConfig {
    var points: Array<Number>
    var tension: Number? get() = definedExternally; set(value) = definedExternally
    var closed: Boolean? get() = definedExternally; set(value) = definedExternally
    var pointerLength: Number? get() = definedExternally; set(value) = definedExternally
    var pointerWidth: Number? get() = definedExternally; set(value) = definedExternally
}

open external class Arrow(ArrowConfig: ArrowConfig) : Shape {
    open fun closed(): Boolean = definedExternally
    open fun closed(closed: Boolean): Arrow /* this */ = definedExternally
    open fun tension(): Number = definedExternally
    open fun tension(tension: Number): Arrow /* this */ = definedExternally
    open fun points(): Array<Number> = definedExternally
    open fun points(points: Array<Number>): Arrow /* this */ = definedExternally
    open fun pointerLength(): Number = definedExternally
    open fun pointerLength(Length: Number): Arrow /* this */ = definedExternally
    open fun pointerWidth(): Number = definedExternally
    open fun pointerWidth(Width: Number): Arrow /* this */ = definedExternally
    open fun pointerAtBeginning(): Boolean = definedExternally
    open fun pointerAtBeginning(Should: Boolean): Arrow /* this */ = definedExternally
}

external interface RectConfig : ShapeConfig {
    var cornerRadius: Number? get() = definedExternally; set(value) = definedExternally
}

open external class Rect(RectConfig: RectConfig) : Shape {
    open fun cornerRadius(): Number = definedExternally
    open fun cornerRadius(cornerRadius: Number): Rect /* this */ = definedExternally
}

external interface SpriteConfig : ShapeConfig {
    var animation: String
    var animations: Any
    var frameIndex: Number? get() = definedExternally; set(value) = definedExternally
    var image: HTMLImageElement
}

open external class Sprite(SpriteConfig: SpriteConfig) : Shape {
    open fun start(): Unit = definedExternally
    open fun stop(): Unit = definedExternally
    open fun animation(): String = definedExternally
    open fun animation(value: String): Sprite /* this */ = definedExternally
    open fun animations(): Any = definedExternally
    open fun animations(value: Any): Sprite /* this */ = definedExternally
    open fun frameIndex(): Number = definedExternally
    open fun frameIndex(value: Number): Sprite /* this */ = definedExternally
    open fun image(): HTMLImageElement = definedExternally
    open fun image(image: HTMLImageElement): Sprite /* this */ = definedExternally
    open fun frameRate(): Number = definedExternally
    open fun frameRate(frameRate: Number): Sprite /* this */ = definedExternally
}

external interface TextConfig : ShapeConfig {
    var text: String
    var fontFamily: String? get() = definedExternally; set(value) = definedExternally
    var fontSize: Number? get() = definedExternally; set(value) = definedExternally
    var fontStyle: String? get() = definedExternally; set(value) = definedExternally
    var align: String? get() = definedExternally; set(value) = definedExternally
    var padding: Number? get() = definedExternally; set(value) = definedExternally
    var lineHeight: Number? get() = definedExternally; set(value) = definedExternally
    var wrap: String? get() = definedExternally; set(value) = definedExternally
}

open external class Text(TextConfig: TextConfig) : Shape {
    open fun getTextWidth(): Number = definedExternally
    open fun getTextHeight(): Number = definedExternally
    open fun text(): String = definedExternally
    open fun text(text: String): Text /* this */ = definedExternally
    open fun fontFamily(): String = definedExternally
    open fun fontFamily(fontFamily: String): Text /* this */ = definedExternally
    open fun fontSize(): Number = definedExternally
    open fun fontSize(fontSize: Number): Text /* this */ = definedExternally
    open fun fontStyle(): String = definedExternally
    open fun fontStyle(fontStyle: String): Text /* this */ = definedExternally
    open fun fontVariant(): String = definedExternally
    open fun fontVariant(fontVariant: String): Text /* this */ = definedExternally
    open fun align(): String = definedExternally
    open fun align(align: String): Text /* this */ = definedExternally
    open fun padding(): Number = definedExternally
    open fun padding(padding: Number): Text /* this */ = definedExternally
    open fun lineHeight(): Number = definedExternally
    open fun lineHeight(lineHeight: Number): Text /* this */ = definedExternally
    open fun wrap(): String = definedExternally
    open fun wrap(wrap: String): Text /* this */ = definedExternally
    open fun textDecoration(): String = definedExternally
    open fun textDecoration(textDecoration: String): Text /* this */ = definedExternally
}

external interface WedgeConfig : ShapeConfig {
    var angle: Number
    var radius: Number
    var clockwise: Boolean? get() = definedExternally; set(value) = definedExternally
}

open external class Wedge(WedgeConfig: WedgeConfig) : Shape {
    open fun angle(): Number = definedExternally
    open fun angle(angle: Number): Wedge /* this */ = definedExternally
    open fun radius(): Number = definedExternally
    open fun radius(radius: Number): Wedge /* this */ = definedExternally
    open fun clockwise(): Boolean = definedExternally
    open fun clockwise(clockwise: Boolean): Wedge /* this */ = definedExternally
}

external interface TagConfig : ShapeConfig {
    var pointerDirection: String? get() = definedExternally; set(value) = definedExternally
    var pointerWidth: Number? get() = definedExternally; set(value) = definedExternally
    var pointerHeight: Number? get() = definedExternally; set(value) = definedExternally
    var cornerRadius: Number? get() = definedExternally; set(value) = definedExternally
}

open external class Tag(config: TagConfig) : Shape {
    open fun pointerDirection(): String = definedExternally
    open fun pointerDirection(pointerDirection: String): Tag /* this */ = definedExternally
    open fun pointerWidth(): Number = definedExternally
    open fun pointerWidth(pointerWidth: Number): Tag /* this */ = definedExternally
    open fun pointerHeight(): Number = definedExternally
    open fun pointerHeight(pointerHeight: Number): Tag /* this */ = definedExternally
    open fun cornerRadius(): Number = definedExternally
    open fun cornerRadius(cornerRadius: Number): Tag /* this */ = definedExternally
}

external interface LabelInterface : ContainerConfig
open external class Label(LabelInterface: LabelInterface) : Group {
    open fun getText(): Text = definedExternally
    open fun getTag(): Rect = definedExternally
}

external interface PathConfig : ShapeConfig {
    var data: String
}

open external class Path(PathConfig: PathConfig) : Shape {
    open fun data(): String = definedExternally
    open fun data(data: String): Path /* this */ = definedExternally
}

external interface RegularPolygonConfig : ShapeConfig {
    var sides: Number
    var radius: Number
}

open external class RegularPolygon(RegularPolygonConfig: RegularPolygonConfig) : Shape {
    open fun sides(): Number = definedExternally
    open fun sides(sides: Number): RegularPolygon /* this */ = definedExternally
    open fun radius(): Number = definedExternally
    open fun radius(radius: Number): RegularPolygon /* this */ = definedExternally
}

external interface StarConfig : ShapeConfig {
    var numPoints: Number
    var innerRadius: Number
    var outerRadius: Number
}

open external class Star(StarConfig: StarConfig) : Shape {
    open fun numPoints(): Number = definedExternally
    open fun numPoints(numPoints: Number): Star /* this */ = definedExternally
    open fun innerRadius(): Number = definedExternally
    open fun innerRadius(innerRadius: Number): Star /* this */ = definedExternally
    open fun outerRadius(): Number = definedExternally
    open fun outerRadius(outerRadius: Number): Star /* this */ = definedExternally
}

external interface TextPathConfig : ShapeConfig {
    var text: String
    var data: String
    var fontFamily: String? get() = definedExternally; set(value) = definedExternally
    var fontSize: Number? get() = definedExternally; set(value) = definedExternally
    var fontStyle: String? get() = definedExternally; set(value) = definedExternally
}

open external class TextPath(TextPathConfig: TextPathConfig) : Shape {
    open fun getTextWidth(): Number = definedExternally
    open fun getTextHeight(): Number = definedExternally
    open fun setText(text: String): Unit = definedExternally
    open fun text(): String = definedExternally
    open fun text(text: String): TextPath /* this */ = definedExternally
    open fun fontFamily(): String = definedExternally
    open fun fontFamily(fontFamily: String): TextPath /* this */ = definedExternally
    open fun fontSize(): Number = definedExternally
    open fun fontSize(fontSize: Number): TextPath /* this */ = definedExternally
    open fun fontStyle(): String = definedExternally
    open fun fontStyle(fontStyle: String): TextPath /* this */ = definedExternally
}

open external class Collection {
    @nativeGetter
    open operator fun get(i: Number): Any? = definedExternally

    @nativeSetter
    open operator fun set(i: Number, value: Any): Unit = definedExternally
    open fun each(f: (el: Node) -> Unit): Unit = definedExternally
    open fun toArray(): Array<Any> = definedExternally
    open var length: Number = definedExternally

    companion object {
        fun toCollection(arr: Array<Any>): Collection = definedExternally
    }
}

open external class Transform {
    open fun copy(): Transform = definedExternally
    open fun getMatrix(): Array<Number> = definedExternally
    open fun getTranslation(): Vector2d = definedExternally
    open fun invert(): Transform = definedExternally
    open fun multiply(matrix: Array<Number>): Transform = definedExternally
    open fun point(point: Vector2d): Vector2d = definedExternally
    open fun rotate(deg: Number): Transform = definedExternally
    open fun scale(x: Number, y: Number): Transform = definedExternally
    open fun setAbsolutePosition(): Transform = definedExternally
    open fun skew(x: Number, y: Number): Transform = definedExternally
    open fun translate(x: Number, y: Number): Transform = definedExternally
}

external interface TransformerConfig : ContainerConfig {
    var resizeEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var rotateEnabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var rotationSnaps: Array<Number>? get() = definedExternally; set(value) = definedExternally
    var rotateHandlerOffset: Number? get() = definedExternally; set(value) = definedExternally
    var lineEnabled: Number? get() = definedExternally; set(value) = definedExternally
    var keepRatio: Boolean? get() = definedExternally; set(value) = definedExternally
    var enabledHandlers: Array<String>? get() = definedExternally; set(value) = definedExternally
    var node: Rect? get() = definedExternally; set(value) = definedExternally
}

open external class Transformer(config: TransformerConfig? = definedExternally /* null */) : Container {
    open fun attachTo(node: Node): Unit = definedExternally
    open fun setNode(node: Node): Unit = definedExternally
    open fun getNode(): Node = definedExternally
    open fun detach(): Unit = definedExternally
    open fun forceUpdate(): Unit = definedExternally
    open fun update(): Unit = definedExternally
}

external interface Vector2d {
    var x: Number
    var y: Number
}
