package de.peekandpoke.kraft.components

import de.peekandpoke.kraft.components.state.ComponentStateProperty
import de.peekandpoke.kraft.components.state.ComponentStreamProperty
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.kraft.vdom.VDomEngine
import de.peekandpoke.ultra.common.MutableTypedAttributes
import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import de.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLElement
import kotlin.properties.ReadOnlyProperty

/**
 * Base class of all Components
 */
@Suppress("FunctionName", "Detekt.TooManyFunctions")
abstract class Component<PROPS>(val ctx: Ctx<PROPS>) {

    companion object {
        private var nextDomKey = 1

        fun getNextDomKey() = "--component-$nextDomKey--".also { nextDomKey += 1 }
    }

    /**
     * Lifecycle hooks for components
     */
    class LifeCycle<PROPS> {
        /**
         * Hook with no callback parameters
         */
        class Hook {
            private val listeners = mutableListOf<() -> Unit>()

            /**
             * Registers a listener.
             */
            operator fun invoke(block: () -> Unit) {
                listeners.add(block)
            }

            /**
             * Notifies all listeners.
             */
            fun notify() {
                listeners.forEach { it() }
            }
        }

        /**
         * Hook for props update
         */
        class NextPropsHook<PROPS> {
            private val listeners = mutableListOf<(newProps: PROPS, previousProps: PROPS) -> Unit>()

            /**
             * Registers a listener.
             */
            operator fun invoke(block: (new: PROPS, old: PROPS) -> Unit) {
                listeners.add(block)
            }

            /**
             * Notifies all listeners.
             */
            fun notify(new: PROPS, old: PROPS) {
                listeners.forEach { it(new, old) }
            }
        }

        /**
         * Brings the [LifeCycle] object into scope.
         */
        operator fun invoke(block: LifeCycle<PROPS>.() -> Unit) {
            this.block()
        }

        /** Hook called when the component was mounted */
        val onMount = Hook()

        /** Hook called when the DOM of the component was updated */
        val onUpdate = Hook()

        /** Hook called when the component was unmounted */
        val onUnmount = Hook()

        /** Hook called when the component receives new props */
        val onNextProps = NextPropsHook<PROPS>()
    }

    /** The attributes of the component */
    val attributes: MutableTypedAttributes = MutableTypedAttributes.empty()

    /** Accessor for the parent component */
    val parent: Component<*>? get() = _parent

    /** Public getter for the Props */
    val props: PROPS get() = _props

    /** The Dom node to which the component is rendered */
    val dom: HTMLElement? get() = _dom

    /** The life-cycle [LifeCycle] that the component exposes */
    val lifecycle: LifeCycle<PROPS> = LifeCycle()

    /** An automatically generated unique dom key for the component instance. */
    val autoDomKey: String = getNextDomKey()

    /** Pointer to the low level bridge Component for example for Preact */
    internal var lowLevelBridgeComponent: Any? = null

    /** Backing field for the [parent] */
    private var _parent: Component<*>? = ctx.parent

    /** Backing field for the [props] */
    private var _props: PROPS = ctx.props

    /** Backing field for the [dom] */
    private var _dom: HTMLElement? = null

    /** Flag indicating if the component needs to be redrawn */
    private var needsRedraw = true

    /** Render cache with the last render result */
    private var renderCache: dynamic = null

    /** A list of stream unsubscribe functions. Will be called when the component is removed */
    private val unSubscribers = mutableListOf<Unsubscribe>()

    /** Map of state values of any kind */
    internal val internalData = mutableMapOf<String, Any?>()

    /**
     * Every component needs to implement this method
     */
    abstract fun VDom.render()

    //  Life-cycle hooks  /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Called when the component is mounted
     */
    internal fun onMount(dom: HTMLElement?) {
        _setDom(dom)

        lifecycle.onMount.notify()
    }

    /**
     * Called when the dom was updated. This is called after the render cycle.
     */
    internal fun onUpdate(dom: HTMLElement?) {
        _setDom(dom)

        lifecycle.onUpdate.notify()
    }

    /**
     * Called when the component was removed from the dom
     *
     * Unsubscribes all async / stream listeners.
     * Calls [onUnmount] on the component.
     * Clears the [dom] reference.
     */
    internal fun onUnmount() {
        // unsubscribe from all stream subscriptions
        unSubscribers.forEach { it() }

        lifecycle.onUnmount.notify()

        _setDom(null)
    }

    /**
     * Called when the component receives new props.
     *
     * This function is only called when shouldRedraw(nextProps) returns true.
     *
     * @param new The new props the component just received
     * @param old The previous props the component had
     */
    private fun onNextProps(new: PROPS, old: PROPS) {
        if (new != old) {
            lifecycle.onNextProps.notify(new = new, old = old)
        }
    }

    /**
     * Returns 'true' when the component should redraw.
     *
     * By default, returns [props] != [nextProps]
     */
    open fun shouldRedraw(nextProps: PROPS): Boolean {
//        console.log("nextProps", this, nextProps, props, nextProps != props)
        return props != nextProps
    }

    /**
     * Triggers a redraw
     */
    fun triggerRedraw() {
        if (needsRedraw) {
            return
        }

        needsRedraw = true

        ctx.engine.triggerRedraw(this)
    }

    //  Ref Helpers  ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a reference to another component.
     */
    fun <C : Component<*>> createRef() = ComponentRef.Tracker<C>()

    //  Stream Helpers  ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Subscribes to a [Stream].
     *
     * When the component is destroyed, the subscription will be unsubscribed automatically.
     */
    operator fun <T> Stream<T>.invoke(handler: (T) -> Unit): () -> Unit {

        return this.subscribeToStream(handler).apply {
            unSubscribers.add(this)
        }
    }

    /**
     * Subscribes to a [Stream].
     *
     * When the component is destroyed, the subscription will be unsubscribed automatically.
     */
    fun <T> Stream<T>.subscribe(handler: (T) -> Unit): () -> Unit = this(handler)

    //  State Helpers  ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a read write property for the components state.
     *
     * When the value of the property is changed the component will redraw itself.
     *
     * @param initial  The initial value of the property
     * @param onChange Callback to be called when the value has changed
     */
    fun <T> value(initial: T, onChange: ((T) -> Unit)? = null): ComponentStateProperty<T> {

        return ComponentStateProperty(
            component = this,
            initialValue = initial,
            onChange = onChange
        )
    }

    /**
     * Creates a property that is subscribed to a stream.
     *
     * The property will always contain the current value of the stream.
     * When a new value is received from the stream the component redraws itself.
     *
     * @param stream The stream to subscribe to.
     */
    fun <T> subscribingTo(stream: Stream<T>, onNext: ((T) -> Unit)? = null): ReadOnlyProperty<Any?, T> {

        var first = true

        stream {

            // The first value will be received right away.
            // This can lead to situations where the [onNext] callback tries to use the property right away.
            // But the property is not yet initialized and a very cryptic null pointer errors will arise.
            // To work around this, we hold the first value back, because the property will be initialized properly.
            if (first) {
                first = false

                launch {
                    delay(1)
                    // notify about the change and trigger redraw
                    onNext?.invoke(it)
                    // redraw the component
                    triggerRedraw()
                }
            } else {
                // notify about the change and trigger redraw
                onNext?.invoke(it)
                // redraw the component
                triggerRedraw()
            }
        }

        return ReadOnlyProperty { _, _ -> stream() }
    }

    /**
     * Creates a property that is backed by a [StreamSource]
     *
     * When ever a value is written to the property it will also by passed into the stream.
     * The stream will be configured using [config] and will be then be subscribed to.
     *
     * This way we can implement e.g. debouncing of input values, for example:
     *
     * ```
     * private var search by stream("", { debounce(300) }) { reload() }
     * ```
     *
     * @param initial The initial value
     * @param config  Configures the stream before subscribing to it
     * @param handler Handler for values published by the stream
     */
    fun <T> stream(
        initial: T,
        config: (Stream<T>.() -> Stream<T>)? = null,
        handler: (T) -> Unit = {},
    ): ComponentStreamProperty<T> {

        val stream = StreamSource(initial)

        // Configure the stream, subscribe and invoke the handler with all new values
        val configured = when (config) {
            null -> stream
            else -> stream.config()
        }

        // Subscribe to the configured stream.
        // NOTICE: little hack here with "launch", to prevent the value being published twice.
        launch {
            delay(1)
            configured { handler(it) }
        }

        return ComponentStreamProperty(component = this, stream = stream)
    }

    //  Private functions  //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Internal function for setting the [dom] element of the component
     */
    private fun _setDom(dom: HTMLElement?) {
        this._dom = dom
    }

    /**
     * Internal function called by the [VDomEngine] when the component is about to be rendered.
     *
     * Return 'true' when the component needs a re-draw.
     *
     * The [newCtx] contains the next set of [PROPS]. These are passed to [shouldRedraw].
     */
    internal fun _internalShouldComponentUpdate(newCtx: Ctx<PROPS>): Boolean {

        if (shouldRedraw(newCtx.props)) {
            needsRedraw = true
        }

        val previousProps = _props

        _props = newCtx.props
        _parent = newCtx.parent

        onNextProps(_props, previousProps)

        return needsRedraw
    }

    /**
     * Internal function called by the [VDomEngine] to render the component.
     *
     * This method checks for [needsRedraw].
     * When 'true' then [render] method of the component is called and the result is put into the [renderCache].
     * When 'false' the [render] method is not called and the [renderCache] is returned.
     */
    internal fun _internalRender(): dynamic {
        if (!needsRedraw) {
            return renderCache
        }

        needsRedraw = false

//        console.log("rendering component", this)

        renderCache = ctx.engine.render(this)

        return renderCache
    }
}
